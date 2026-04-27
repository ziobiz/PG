package com.pg.service;

import com.pg.entity.ServerUsageDaily;
import com.pg.entity.ServerUsageState;
import com.pg.repository.ServerUsageDailyRepository;
import com.pg.repository.ServerUsageStateRepository;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * NOTI 고급 시스템 모니터와 유사: 일간/주간/월간 트래픽(송수신 합)·일 메모리 피크(%) 시계열.
 * OSHI 네트워크 카운터 델타를 주기적으로 누적하고 DB에 일 단위로 저장합니다.
 */
@Service
public class ServerUsageService {

    private static final Logger log = LoggerFactory.getLogger(ServerUsageService.class);

    private final ServerUsageDailyRepository dailyRepo;
    private final ServerUsageStateRepository stateRepo;

    @Value("${app.serverUsage.enabled:true}")
    private boolean enabled;

    public ServerUsageService(ServerUsageDailyRepository dailyRepo, ServerUsageStateRepository stateRepo) {
        this.dailyRepo = dailyRepo;
        this.stateRepo = stateRepo;
    }

    /**
     * 스케줄 + 트랜잭션은 동일 public 메서드에 둡니다(동일 클래스 내부 호출 시 @Transactional 무력화 방지).
     */
    @Scheduled(fixedRateString = "${app.serverUsage.collectIntervalMs:600000}")
    @Transactional
    public void scheduledCollect() {
        if (!enabled) {
            return;
        }
        try {
            collectSampleCore();
        } catch (Exception e) {
            log.warn("server usage collect failed: {}", e.getMessage());
        }
    }

    private void collectSampleCore() {
        ZoneId z = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(z);
        Double memPct = readSystemMemoryUsedPct();
        Long netTotal = readNetworkTotalBytes();

        ServerUsageState st = stateRepo.findById(ServerUsageState.SINGLETON_ID).orElseGet(() -> {
            ServerUsageState n = new ServerUsageState();
            n.setId(ServerUsageState.SINGLETON_ID);
            return n;
        });

        long delta = 0;
        if (netTotal != null) {
            if (st.getLastNetTotalBytes() != null) {
                if (netTotal >= st.getLastNetTotalBytes()) {
                    delta = netTotal - st.getLastNetTotalBytes();
                } else {
                    delta = netTotal;
                }
            }
            st.setLastNetTotalBytes(netTotal);
        }
        st.setUpdatedAt(Instant.now());
        stateRepo.save(st);

        ServerUsageDaily day = dailyRepo.findById(today).orElseGet(() -> new ServerUsageDaily(today, 0L, 0.0));
        day.setTrafficBytes(day.getTrafficBytes() + Math.max(0, delta));
        if (memPct != null) {
            day.setMemoryPeakPct(Math.max(day.getMemoryPeakPct(), memPct));
        }
        dailyRepo.save(day);
    }

    private static Double readSystemMemoryUsedPct() {
        try {
            HardwareAbstractionLayer hal = new SystemInfo().getHardware();
            long total = hal.getMemory().getTotal();
            long avail = hal.getMemory().getAvailable();
            if (total <= 0) {
                return null;
            }
            return Math.round((total - avail) * 1000.0 / total) / 10.0;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Long readNetworkTotalBytes() {
        try {
            HardwareAbstractionLayer hal = new SystemInfo().getHardware();
            long sum = 0;
            for (NetworkIF nif : hal.getNetworkIFs()) {
                String name = nif.getName();
                if (name == null) {
                    continue;
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("lo") || lower.contains("loopback")) {
                    continue;
                }
                nif.updateAttributes();
                sum += nif.getBytesRecv() + nif.getBytesSent();
            }
            return sum;
        } catch (Throwable t) {
            return null;
        }
    }

    public Map<String, Object> buildUsageReport(String grain) {
        ZoneId z = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(z);
        String g = grain == null ? "daily" : grain.trim().toLowerCase(Locale.ROOT);
        if (!"daily".equals(g) && !"weekly".equals(g) && !"monthly".equals(g)) {
            g = "daily";
        }

        LocalDate loadFrom = today.minusDays(450);
        List<ServerUsageDaily> rows = dailyRepo.findByUsageDateBetweenOrderByUsageDateAsc(loadFrom, today);
        Map<LocalDate, ServerUsageDaily> byDay = rows.stream()
                .collect(Collectors.toMap(ServerUsageDaily::getUsageDate, x -> x, (a, b) -> a, TreeMap::new));

        List<String> labels = new ArrayList<>();
        List<Double> trafficGb = new ArrayList<>();
        List<Double> memPeak = new ArrayList<>();

        switch (g) {
            case "weekly" -> buildWeeklySeries(today, byDay, labels, trafficGb, memPeak);
            case "monthly" -> buildMonthlySeries(today, byDay, labels, trafficGb, memPeak);
            default -> buildDailySeries(today, byDay, labels, trafficGb, memPeak);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("grain", g);
        out.put("labels", labels);
        out.put("trafficSeriesGb", trafficGb);
        out.put("memoryPeakSeriesPct", memPeak);
        out.put("summary", buildSummary(g, today, byDay, labels, trafficGb, memPeak));
        return out;
    }

    private static void buildDailySeries(LocalDate today, Map<LocalDate, ServerUsageDaily> byDay,
                                         List<String> labels, List<Double> trafficGb, List<Double> memPeak) {
        for (int i = 30; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            labels.add(d.toString());
            ServerUsageDaily x = byDay.get(d);
            trafficGb.add(x == null ? 0.0 : bytesToGb(x.getTrafficBytes()));
            memPeak.add(x == null ? 0.0 : round1(x.getMemoryPeakPct()));
        }
    }

    private static void buildWeeklySeries(LocalDate today, Map<LocalDate, ServerUsageDaily> byDay,
                                          List<String> labels, List<Double> trafficGb, List<Double> memPeak) {
        for (int w = 11; w >= 0; w--) {
            LocalDate mon = today.with(DayOfWeek.MONDAY).minusWeeks(w);
            LocalDate sun = mon.plusDays(6);
            long sumB = 0;
            double maxM = 0;
            for (LocalDate d = mon; !d.isAfter(sun) && !d.isAfter(today); d = d.plusDays(1)) {
                ServerUsageDaily x = byDay.get(d);
                if (x != null) {
                    sumB += x.getTrafficBytes();
                    maxM = Math.max(maxM, x.getMemoryPeakPct());
                }
            }
            int y = mon.get(IsoFields.WEEK_BASED_YEAR);
            int wn = mon.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            labels.add(String.format(Locale.ROOT, "%d-W%02d", y, wn));
            trafficGb.add(bytesToGb(sumB));
            memPeak.add(round1(maxM));
        }
    }

    private static void buildMonthlySeries(LocalDate today, Map<LocalDate, ServerUsageDaily> byDay,
                                           List<String> labels, List<Double> trafficGb, List<Double> memPeak) {
        for (int m = 11; m >= 0; m--) {
            YearMonth ym = YearMonth.from(today).minusMonths(m);
            LocalDate first = ym.atDay(1);
            LocalDate last = ym.atEndOfMonth();
            if (last.isAfter(today)) {
                last = today;
            }
            long sumB = 0;
            double maxM = 0;
            for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
                ServerUsageDaily x = byDay.get(d);
                if (x != null) {
                    sumB += x.getTrafficBytes();
                    maxM = Math.max(maxM, x.getMemoryPeakPct());
                }
            }
            labels.add(ym.toString());
            trafficGb.add(bytesToGb(sumB));
            memPeak.add(round1(maxM));
        }
    }

    private static Map<String, Object> buildSummary(String grain, LocalDate today,
                                                    Map<LocalDate, ServerUsageDaily> byDay,
                                                    List<String> labels, List<Double> trafficGb, List<Double> memPeak) {
        Map<String, Object> s = new LinkedHashMap<>();
        String grainLabel = switch (grain) {
            case "weekly" -> "weekly";
            case "monthly" -> "monthly";
            default -> "daily";
        };
        s.put("grainLabel", grainLabel);
        s.put("daysInChart", labels.size());
        s.put("maxChartDays", "daily".equals(grain) ? 31 : labels.size());

        double sumGb = trafficGb.stream().mapToDouble(Double::doubleValue).sum();
        s.put("trafficTotalPeriodMb", round2(sumGb * 1024.0));

        long last7b = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(i);
            ServerUsageDaily x = byDay.get(d);
            if (x != null) {
                last7b += x.getTrafficBytes();
            }
        }
        s.put("trafficTotalLast7DaysMb", round2(last7b / (1024.0 * 1024.0)));

        LocalDate latestDay = today;
        ServerUsageDaily latest = byDay.get(latestDay);
        ServerUsageDaily prev = byDay.get(today.minusDays(1));
        double latestMb = latest == null ? 0 : latest.getTrafficBytes() / (1024.0 * 1024.0);
        double prevMb = prev == null ? 0 : prev.getTrafficBytes() / (1024.0 * 1024.0);
        s.put("latestDate", latestDay.toString());
        s.put("latestTrafficMb", round2(latestMb));
        s.put("prevTrafficMb", round2(prevMb));
        double deltaMb = latestMb - prevMb;
        s.put("trafficDeltaMb", round2(deltaMb));
        if (prevMb > 0.0001) {
            s.put("trafficDeltaPct", round1((deltaMb / prevMb) * 100.0));
        } else {
            s.put("trafficDeltaPct", null);
        }

        double maxDayMb = 0;
        LocalDate maxDay = null;
        for (int i = 0; i < 31; i++) {
            LocalDate d = today.minusDays(i);
            ServerUsageDaily x = byDay.get(d);
            if (x != null) {
                double mb = x.getTrafficBytes() / (1024.0 * 1024.0);
                if (mb >= maxDayMb) {
                    maxDayMb = mb;
                    maxDay = d;
                }
            }
        }
        s.put("maxDayTrafficMb", round2(maxDayMb));
        s.put("maxDayTrafficDate", maxDay != null ? maxDay.toString() : null);

        int active = 0;
        double sumMb = 0;
        for (int i = 0; i < 31; i++) {
            LocalDate d = today.minusDays(i);
            ServerUsageDaily x = byDay.get(d);
            if (x != null && x.getTrafficBytes() > 0) {
                active++;
                sumMb += x.getTrafficBytes() / (1024.0 * 1024.0);
            }
        }
        s.put("avgDailyTrafficMb", active > 0 ? round2(sumMb / active) : null);

        double memLatest = memPeak.isEmpty() ? 0 : memPeak.get(memPeak.size() - 1);
        double memMax = memPeak.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        s.put("memoryLatestPeakPct", round1(memLatest));
        s.put("memoryPeriodMaxPeakPct", round1(memMax));
        s.put("hasData", !rowsEmpty(trafficGb, memPeak));
        return s;
    }

    private static boolean rowsEmpty(List<Double> trafficGb, List<Double> memPeak) {
        boolean t = trafficGb.stream().allMatch(x -> x == null || x <= 0);
        boolean m = memPeak.stream().allMatch(x -> x == null || x <= 0);
        return t && m;
    }

    private static double bytesToGb(long bytes) {
        return Math.round(bytes * 1000.0 / (1024.0 * 1024.0 * 1024.0)) / 1000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
