package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;

/**
 * 본사설정 서버관리: 호스트·JVM·DB·SSL(PEM)·Certbot·Nginx stub 요약
 */
@Service
public class HqServerManageService {

    private static final String PEM_BEGIN = "-----BEGIN CERTIFICATE-----";

    /** NOTI advanced_system_monitor 와 유사한 임계치 (단일화) */
    private static final double SYS_MEM_PCT_DANGER = 90;
    private static final double SYS_MEM_PCT_WARN = 70;
    private static final double DISK_PCT_DANGER = 90;
    private static final double DISK_PCT_WARN = 75;
    private static final double HEAP_PCT_WARN = 80;
    private static final double HEAP_PCT_DANGER = 92;
    private static final long HEAP_FRAC_MIN_TOTAL_BYTES = 64L * 1024 * 1024;
    private static final double LOAD_MULT = 2;
    private static final int SSL_DAYS_WARN = 30;
    private static final int SSL_DAYS_DANGER = 14;

    private final HqApiConfigRepository hqApiConfigRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.serverManage.uiAutoRefreshSeconds:120}")
    private int uiAutoRefreshSeconds;

    @Value("${app.serverManage.nginxStubStatusUrl:}")
    private String nginxStubStatusUrl;

    public HqServerManageService(HqApiConfigRepository hqApiConfigRepository, DataSource dataSource) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int getUiAutoRefreshSeconds() {
        return uiAutoRefreshSeconds;
    }

    public Map<String, Object> buildSummary() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", Instant.now().toString());
        root.put("uiAutoRefreshSeconds", uiAutoRefreshSeconds);
        root.put("nginxStubStatusUrlConfigured", nginxStubStatusUrl != null && !nginxStubStatusUrl.isBlank());

        Optional<HqApiConfig> cfgOpt = hqApiConfigRepository.findAll().stream().findFirst();
        String configuredPath = cfgOpt.map(HqApiConfig::getServerManageSslCertPath).orElse(null);
        String leDomain = cfgOpt.map(HqApiConfig::getServerManageSslLeDomain).orElse(null);

        root.put("serverManageSslCertPath", configuredPath != null ? configuredPath : "");
        root.put("serverManageSslLeDomain", leDomain != null ? leDomain : "");

        Path pemPath = resolvePemPath(configuredPath, leDomain);
        root.put("sslResolvedPath", pemPath != null ? pemPath.toString() : "");
        Map<String, Object> host = readHostInfo();
        Map<String, Object> jvm = readJvmInfo();
        Map<String, Object> disk = readDiskInfo();
        Map<String, Object> ssl = readSslInfo(pemPath);
        root.put("ssl", ssl);
        root.put("host", host);
        root.put("jvm", jvm);
        root.put("disk", disk);
        root.put("health", buildHealth(host, jvm, disk, ssl, cfgOpt));
        root.put("certbot", readCertbotInfo());
        root.put("db", readDbInfo());
        root.put("nginxStub", readNginxStub());
        putServerManageContractFields(root, cfgOpt);
        return root;
    }

    private void putServerManageContractFields(Map<String, Object> root, Optional<HqApiConfig> cfgOpt) {
        HqApiConfig c = cfgOpt.orElse(null);
        root.put("serverManageContractDiskMb", c != null ? c.getServerManageContractDiskMb() : null);
        root.put("serverManageContractTrafficMb", c != null ? c.getServerManageContractTrafficMb() : null);
        root.put("serverManageContractStart", c != null && c.getServerManageContractStart() != null
                ? c.getServerManageContractStart().toString() : "");
        root.put("serverManageContractEnd", c != null && c.getServerManageContractEnd() != null
                ? c.getServerManageContractEnd().toString() : "");
        root.put("serverManageTrafficUsedMb", c != null ? c.getServerManageTrafficUsedMb() : null);
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("diskMb", c != null ? c.getServerManageContractDiskMb() : null);
        contract.put("trafficMb", c != null ? c.getServerManageContractTrafficMb() : null);
        contract.put("trafficUsedMb", c != null ? c.getServerManageTrafficUsedMb() : null);
        contract.put("periodStart", c != null && c.getServerManageContractStart() != null
                ? c.getServerManageContractStart().toString() : "");
        contract.put("periodEnd", c != null && c.getServerManageContractEnd() != null
                ? c.getServerManageContractEnd().toString() : "");
        root.put("serverManageContract", contract);
    }

    private Path resolvePemPath(String configured, String leDomain) {
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured.trim());
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        String env = System.getenv("PG_SSL_CERT_PATH");
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env.trim());
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        if (leDomain != null && !leDomain.isBlank()) {
            Path p = Paths.get("/etc/letsencrypt/live", leDomain.trim(), "fullchain.pem");
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private Map<String, Object> readSslInfo(Path pemPath) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (pemPath == null || !Files.isRegularFile(pemPath)) {
            m.put("status", "N/A");
            m.put("detail", "인증서 파일을 찾을 수 없습니다. 경로 또는 LE 도메인을 저장하세요.");
            return m;
        }
        try {
            String pem = Files.readString(pemPath, StandardCharsets.UTF_8);
            int b = pem.indexOf(PEM_BEGIN);
            int e = pem.indexOf("-----END CERTIFICATE-----");
            if (b < 0 || e < 0) {
                m.put("status", "ERROR");
                m.put("detail", "PEM 형식이 아닙니다.");
                return m;
            }
            String b64 = pem.substring(b + PEM_BEGIN.length(), e).replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(b64);
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            m.put("status", "OK");
            m.put("subjectDn", cert.getSubjectX500Principal().getName());
            m.put("issuerDn", cert.getIssuerX500Principal().getName());
            m.put("notBefore", cert.getNotBefore().toInstant().toString());
            m.put("notAfter", cert.getNotAfter().toInstant().toString());
            m.put("daysRemaining", daysBetween(Instant.now(), cert.getNotAfter().toInstant()));
            m.put("fingerprintSha256", sha256Hex(cert.getEncoded()));
        } catch (Exception ex) {
            m.put("status", "ERROR");
            m.put("detail", ex.getMessage() != null ? ex.getMessage() : "parse failed");
        }
        return m;
    }

    private static long daysBetween(Instant from, Instant to) {
        long secs = to.getEpochSecond() - from.getEpochSecond();
        return secs / 86400;
    }

    private Map<String, Object> readHostInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            SystemInfo si = new SystemInfo();
            OperatingSystem os = si.getOperatingSystem();
            HardwareAbstractionLayer hal = si.getHardware();
            m.put("hostname", os.getNetworkParams().getHostName());
            m.put("osFamily", os.getFamily());
            m.put("osVersion", os.getVersionInfo().getVersion());
            m.put("arch", System.getProperty("os.arch"));
            long total = hal.getMemory().getTotal();
            long avail = hal.getMemory().getAvailable();
            m.put("memoryTotalMb", total / (1024 * 1024));
            m.put("memoryAvailableMb", avail / (1024 * 1024));
        } catch (Throwable t) {
            m.put("hostname", safeHostname());
            m.put("error", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
        return m;
    }

    private static String safeHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> readJvmInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        m.put("javaVersion", System.getProperty("java.version"));
        m.put("javaVendor", System.getProperty("java.vendor"));
        m.put("maxMemoryMb", rt.maxMemory() / (1024 * 1024));
        m.put("totalMemoryMb", rt.totalMemory() / (1024 * 1024));
        m.put("freeMemoryMb", rt.freeMemory() / (1024 * 1024));
        try {
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = mem.getHeapMemoryUsage();
            long heapUsed = heap.getUsed();
            long heapMax = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
            m.put("heapUsedMb", heapUsed / (1024 * 1024));
            m.put("heapMaxMb", heapMax > 0 ? heapMax / (1024 * 1024) : 0);
            double heapPct = heapMax > 0 ? Math.round((heapUsed * 1000.0 / heapMax)) / 10.0 : 0;
            m.put("heapUsedPct", heapPct);
        } catch (Exception ignored) {
            m.put("heapUsedMb", 0);
            m.put("heapMaxMb", 0);
            m.put("heapUsedPct", 0.0);
        }
        int cpuN = Math.max(1, rt.availableProcessors());
        m.put("cpuCount", cpuN);
        double loadAvg = -1;
        try {
            loadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception ignored) {
            /* ignore */
        }
        m.put("systemLoadAverage", loadAvg >= 0 ? Math.round(loadAvg * 100.0) / 100.0 : null);
        try {
            var rbx = ManagementFactory.getRuntimeMXBean();
            m.put("uptimeMs", rbx.getUptime());
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private Map<String, Object> readDiskInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            FileStore store = Files.getFileStore(base);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            double pct = total > 0 ? Math.round((used * 1000.0 / total)) / 10.0 : 0;
            m.put("ok", true);
            m.put("pathRoot", base.toString());
            m.put("fileSystem", store.name());
            m.put("totalBytes", total);
            m.put("usableBytes", usable);
            m.put("usedBytes", used);
            m.put("usedPct", pct);
        } catch (Exception ex) {
            m.put("ok", false);
            m.put("error", ex.getMessage() != null ? ex.getMessage() : "disk");
        }
        return m;
    }

    private Map<String, Object> buildHealth(Map<String, Object> host, Map<String, Object> jvm, Map<String, Object> disk, Map<String, Object> ssl,
                                            Optional<HqApiConfig> cfgOpt) {
        Map<String, Object> h = new LinkedHashMap<>();
        List<String> alerts = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        cfgOpt.flatMap(x -> Optional.ofNullable(x.getServerManageContractEnd())).ifPresent(end -> {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), end);
            if (days < 0) {
                alerts.add("호스팅 약정 종료일이 지났습니다. (" + end + ")");
            } else if (days <= 14) {
                alerts.add("호스팅 약정 종료 " + days + "일 전입니다.");
            }
        });

        long totalMb = toLong(host.get("memoryTotalMb"));
        long availMb = toLong(host.get("memoryAvailableMb"));
        double sysMemPct = totalMb > 0 ? Math.round(((totalMb - availMb) * 1000.0 / totalMb)) / 10.0 : 0;
        String sysStatus = sysMemPct >= SYS_MEM_PCT_DANGER ? "danger" : sysMemPct >= SYS_MEM_PCT_WARN ? "warn" : "ok";
        if (sysMemPct >= SYS_MEM_PCT_DANGER) {
            alerts.add("시스템 메모리 사용률이 " + SYS_MEM_PCT_DANGER + "% 이상입니다.");
        }
        rows.add(rowMetric("sys_mem", "시스템 메모리",
                String.format(Locale.ROOT, "주의: 사용률 %.0f%% 이상 · 위험: %.0f%% 이상 (그 미만은 양호, RAM)", SYS_MEM_PCT_WARN, SYS_MEM_PCT_DANGER),
                sysMemPct + "% (가용 " + availMb + " / 총 " + totalMb + " MB)", sysStatus));

        double heapPct = jvm.get("heapUsedPct") instanceof Number ? ((Number) jvm.get("heapUsedPct")).doubleValue() : 0;
        long heapMaxB = toLong(jvm.get("heapMaxMb")) * 1024L * 1024L;
        boolean heapDanger = heapMaxB >= HEAP_FRAC_MIN_TOTAL_BYTES && heapPct >= HEAP_PCT_DANGER;
        String heapStatus = heapDanger ? "danger" : heapPct >= HEAP_PCT_WARN ? "warn" : "ok";
        if (heapDanger) {
            alerts.add("JVM 힙 사용률이 " + (int) HEAP_PCT_DANGER + "% 이상입니다.");
        }
        rows.add(rowMetric("jvm_heap", "JVM 힙",
                String.format(Locale.ROOT, "주의: 사용률 %.0f%% 이상 · 위험: %.0f%% 이상 (최대힙 ≥%dMB일 때만 위험 판정)",
                        HEAP_PCT_WARN, HEAP_PCT_DANGER, (int) (HEAP_FRAC_MIN_TOTAL_BYTES / (1024 * 1024))),
                jvm.get("heapUsedMb") + " / " + jvm.get("heapMaxMb") + " MB (" + heapPct + "%)", heapStatus));

        Double load = jvm.get("systemLoadAverage") instanceof Number ? ((Number) jvm.get("systemLoadAverage")).doubleValue() : null;
        int cpuN = jvm.get("cpuCount") instanceof Number ? ((Number) jvm.get("cpuCount")).intValue() : 1;
        boolean loadDanger = load != null && load >= 0 && load > cpuN * LOAD_MULT;
        String loadStatus = load == null || load < 0 ? "ok" : loadDanger ? "danger" : load > cpuN ? "warn" : "ok";
        if (loadDanger) {
            alerts.add("시스템 부하(1분 평균)가 CPU 코어 수의 " + (int) LOAD_MULT + "배를 넘었습니다.");
        }
        String loadStr = load == null || load < 0 ? "—" : String.valueOf(load);
        rows.add(rowMetric("load_avg", "Load average (1m)",
                String.format(Locale.ROOT, "주의: 1분 평균 > CPU 코어(%d) · 위험: > 코어×%.0f (%.0f) · 없음/N/A는 양호", cpuN, LOAD_MULT, cpuN * LOAD_MULT),
                loadStr, loadStatus));

        String diskStatus = "ok";
        String diskVal = "—";
        if (Boolean.TRUE.equals(disk.get("ok"))) {
            double dPct = disk.get("usedPct") instanceof Number ? ((Number) disk.get("usedPct")).doubleValue() : 0;
            diskVal = dPct + "%";
            diskStatus = dPct >= DISK_PCT_DANGER ? "danger" : dPct >= DISK_PCT_WARN ? "warn" : "ok";
            if (dPct >= DISK_PCT_DANGER) {
                alerts.add("디스크 사용률이 " + DISK_PCT_DANGER + "% 이상입니다.");
            }
        } else {
            diskStatus = "warn";
        }
        rows.add(rowMetric("disk", "디스크 (앱 기준 경로)",
                String.format(Locale.ROOT, "주의: 사용률 %.0f%% 이상 · 위험: %.0f%% 이상 (그 미만은 양호)", DISK_PCT_WARN, DISK_PCT_DANGER),
                diskVal, diskStatus));

        String sslStatus = "ok";
        String sslVal = "—";
        if ("OK".equals(ssl.get("status"))) {
            long days = ssl.get("daysRemaining") instanceof Number ? ((Number) ssl.get("daysRemaining")).longValue() : 999;
            sslVal = "만료까지 약 " + days + "일";
            sslStatus = days < SSL_DAYS_DANGER ? "danger" : days < SSL_DAYS_WARN ? "warn" : "ok";
            if (days < SSL_DAYS_DANGER) {
                alerts.add("SSL 인증서 만료가 " + SSL_DAYS_DANGER + "일 이내입니다.");
            } else if (days < SSL_DAYS_WARN) {
                alerts.add("SSL 인증서 만료가 " + SSL_DAYS_WARN + "일 이내입니다.");
            }
        } else if ("N/A".equals(ssl.get("status"))) {
            sslStatus = "warn";
            sslVal = "인증서 없음/미설정";
        } else if ("ERROR".equals(ssl.get("status"))) {
            sslStatus = "danger";
            sslVal = String.valueOf(ssl.getOrDefault("detail", "오류"));
            alerts.add("SSL 인증서를 읽지 못했습니다.");
        }
        rows.add(rowMetric("ssl", "SSL 인증서",
                String.format(Locale.ROOT, "주의: 만료 잔여 %.0f일 미만 · 위험: %.0f일 미만", (double) SSL_DAYS_WARN, (double) SSL_DAYS_DANGER),
                sslVal, sslStatus));

        HqApiConfig cfg = cfgOpt.orElse(null);
        Integer contractDiskMb = cfg != null ? cfg.getServerManageContractDiskMb() : null;
        if (contractDiskMb != null && contractDiskMb > 0 && Boolean.TRUE.equals(disk.get("ok"))) {
            long usedBytes = disk.get("usedBytes") instanceof Number ? ((Number) disk.get("usedBytes")).longValue() : 0L;
            double usedMb = usedBytes / (1024.0 * 1024.0);
            double pctContract = Math.round((usedMb * 1000.0 / contractDiskMb)) / 10.0;
            String cDiskStatus = pctContract >= DISK_PCT_DANGER ? "danger" : pctContract >= DISK_PCT_WARN ? "warn" : "ok";
            String quotaDiskGb = formatGbFromMb(contractDiskMb);
            if (pctContract >= DISK_PCT_DANGER) {
                alerts.add("약정 디스크(" + quotaDiskGb + ") 대비 사용률이 " + (int) DISK_PCT_DANGER + "% 이상입니다.");
            }
            rows.add(rowMetric("contract_disk", "약정 디스크",
                    String.format(Locale.ROOT, "앱 경로 디스크 사용량 ÷ 약정 %s · 주의: %.0f%% 이상 · 위험: %.0f%% 이상%s",
                            quotaDiskGb, DISK_PCT_WARN, DISK_PCT_DANGER, contractPeriodNote(cfg)),
                    String.format(Locale.ROOT, "약정 %s 중 약 %s 사용 (%.1f%%)", quotaDiskGb, formatGbFromMb(usedMb), pctContract),
                    cDiskStatus));
        }

        Integer contractTrafficMb = cfg != null ? cfg.getServerManageContractTrafficMb() : null;
        Integer trafficUsedMb = cfg != null ? cfg.getServerManageTrafficUsedMb() : null;
        if (contractTrafficMb != null && contractTrafficMb > 0) {
            String quotaTrGb = formatGbFromMb(contractTrafficMb);
            if (trafficUsedMb == null) {
                rows.add(rowMetric("contract_traffic", "약정 트래픽",
                        String.format(Locale.ROOT, "기간당 약정 %s · 사용률 주의 %.0f%%·위험 %.0f%% 이상 (패널 누적을 GB로 저장 후 판정)%s",
                                quotaTrGb, DISK_PCT_WARN, DISK_PCT_DANGER, contractPeriodNote(cfg)),
                        "사용량 미입력 — 호스팅 패널 누적을 GB로 입력 후 저장",
                        "warn"));
            } else {
                double pctT = Math.round((trafficUsedMb * 1000.0 / contractTrafficMb)) / 10.0;
                String tStatus = pctT >= DISK_PCT_DANGER ? "danger" : pctT >= DISK_PCT_WARN ? "warn" : "ok";
                if (pctT >= DISK_PCT_DANGER) {
                    alerts.add("약정 트래픽(" + quotaTrGb + ") 대비 사용이 " + (int) DISK_PCT_DANGER + "% 이상입니다.");
                }
                rows.add(rowMetric("contract_traffic", "약정 트래픽",
                        String.format(Locale.ROOT, "누적 사용 ÷ 약정 %s · 주의: %.0f%% 이상 · 위험: %.0f%% 이상%s",
                                quotaTrGb, DISK_PCT_WARN, DISK_PCT_DANGER, contractPeriodNote(cfg)),
                        String.format(Locale.ROOT, "%s / %s (%.1f%%)", formatGbFromMb(trafficUsedMb), quotaTrGb, pctT),
                        tStatus));
            }
        }

        h.put("alerts", alerts);
        h.put("rows", rows);
        h.put("worstStatus", worstOf(rows));
        return h;
    }

    private static String contractPeriodNote(HqApiConfig cfg) {
        if (cfg == null) {
            return "";
        }
        LocalDate s = cfg.getServerManageContractStart();
        LocalDate e = cfg.getServerManageContractEnd();
        if (s == null && e == null) {
            return "";
        }
        String a = s != null ? s.toString() : "?";
        String b = e != null ? e.toString() : "?";
        return " · 약정기간 " + a + " ~ " + b;
    }

    /** 표시용 MB → GB (저장은 MB 유지) */
    private static String formatGbFromMb(double mbVal) {
        if (mbVal <= 0) {
            return "0 GB";
        }
        return String.format(Locale.ROOT, "%.2f GB", mbVal / 1024.0);
    }

    private static String worstOf(List<Map<String, Object>> rows) {
        int w = 0;
        for (Map<String, Object> r : rows) {
            String s = String.valueOf(r.getOrDefault("status", "ok"));
            if ("danger".equals(s)) {
                return "danger";
            }
            if ("warn".equals(s)) {
                w = 1;
            }
        }
        return w == 1 ? "warn" : "ok";
    }

    private static Map<String, Object> rowMetric(String id, String label, String criteria, String value, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("criteria", criteria);
        m.put("value", value);
        m.put("status", status);
        return m;
    }

    private static long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> readCertbotInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        Path renewalDir = Paths.get("/etc/letsencrypt/renewal");
        List<String> files = new ArrayList<>();
        try {
            if (Files.isDirectory(renewalDir)) {
                try (var stream = Files.list(renewalDir)) {
                    stream.filter(p -> p.toString().endsWith(".conf"))
                            .map(p -> p.getFileName().toString())
                            .sorted()
                            .forEach(files::add);
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        m.put("renewalConfFiles", files);

        Map<String, String> timer = new LinkedHashMap<>();
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            timer.put("active", execShort("systemctl", "is-active", "certbot.timer"));
            timer.put("next", execShort("systemctl", "show", "certbot.timer", "-p", "NextElapseUSecRealtime", "--value"));
        } else {
            timer.put("active", "N/A");
            timer.put("next", "N/A");
        }
        m.put("certbotTimer", timer);
        return m;
    }

    private static String execShort(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(line);
                }
            }
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> readDbInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = current_schema()",
                    Integer.class);
            m.put("ok", true);
            m.put("tableCountInSchema", n != null ? n : 0);
        } catch (Exception ex) {
            m.put("ok", false);
            m.put("error", ex.getMessage() != null ? ex.getMessage() : "db check failed");
        }
        return m;
    }

    private Map<String, Object> readNginxStub() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (nginxStubStatusUrl == null || nginxStubStatusUrl.isBlank()) {
            m.put("status", "SKIPPED");
            return m;
        }
        try {
            var client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(nginxStubStatusUrl.trim()))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            m.put("status", resp.statusCode() == 200 ? "OK" : "HTTP_" + resp.statusCode());
            String body = resp.body();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }
            m.put("bodyPreview", body != null ? body : "");
        } catch (Exception ex) {
            m.put("status", "ERROR");
            m.put("detail", ex.getMessage() != null ? ex.getMessage() : "request failed");
        }
        return m;
    }
}
