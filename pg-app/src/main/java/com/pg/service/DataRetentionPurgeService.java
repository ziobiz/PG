package com.pg.service;

import com.pg.catalog.DataRetentionCatalog;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.repository.ServerUsageDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link HqLedgerSysSettings#getDataRetentionPolicyJson()} 중 {@link DataRetentionCatalog.Entry#schedulerPurge()} 가 true 인 유형만
 * 주기적으로 오래된 행을 삭제합니다. {@link HqLedgerSysSettings#getAppLogFileRetentionDays()} 에 따라 로그 디렉터리의 오래된 파일도 삭제합니다.
 */
@Service
public class DataRetentionPurgeService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionPurgeService.class);

    @Value("${logging.file.name:}")
    private String loggingFileName;

    private final HqLedgerSysSettingsRepository ledgerSysSettingsRepository;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final ServerUsageDailyRepository serverUsageDailyRepository;

    public DataRetentionPurgeService(HqLedgerSysSettingsRepository ledgerSysSettingsRepository,
                                    PgNotifyInboundRepository pgNotifyInboundRepository,
                                    ServerUsageDailyRepository serverUsageDailyRepository) {
        this.ledgerSysSettingsRepository = ledgerSysSettingsRepository;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.serverUsageDailyRepository = serverUsageDailyRepository;
    }

    @Scheduled(cron = "${app.dataRetention.purgeCron:0 0 4 * * *}")
    public void scheduledPurge() {
        try {
            runPurges();
        } catch (Exception e) {
            log.warn("dataRetention scheduled purge failed: {}", e.getMessage());
        }
    }

    @Transactional
    public void runPurges() {
        HqLedgerSysSettings s = ledgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        String json = s != null ? s.getDataRetentionPolicyJson() : null;
        LocalDate today = LocalDate.now();
        LocalDateTime nowDt = LocalDateTime.now();

        Optional<Integer> niOpt = effectivePurge("PG_NOTIFY_INBOUND", json);
        if (niOpt.isPresent()) {
            int ni = niOpt.get();
            LocalDateTime niCut = nowDt.minusDays(Math.max(1, ni));
            long niDel = pgNotifyInboundRepository.deleteByCreatedAtBefore(niCut);
            if (niDel > 0) {
                log.info("dataRetention purge PG_NOTIFY_INBOUND: deleted {} rows (older than {} days)", niDel, ni);
            }
        }

        Optional<Integer> suOpt = effectivePurge("SERVER_USAGE_DAILY", json);
        if (suOpt.isPresent()) {
            int su = suOpt.get();
            LocalDate suCut = today.minusDays(Math.max(1, su));
            long suDel = serverUsageDailyRepository.deleteByUsageDateBefore(suCut);
            if (suDel > 0) {
                log.info("dataRetention purge SERVER_USAGE_DAILY: deleted {} rows (usage_date before {})", suDel, suCut);
            }
        }

        purgeOldAppLogFiles(s);
    }

    /**
     * 전산설정의 로그 파일 보관 일수보다 오래된 {@code .log} / {@code .gz} / {@code .zip} 파일을 {@code logs/} 및
     * {@code logging.file.name} 의 부모 디렉터리에서 삭제합니다(현재 쓰기 중인 파일은 OS가 수정 시각을 갱신).
     */
    private void purgeOldAppLogFiles(HqLedgerSysSettings s) {
        final int retentionDays = (s != null && s.getAppLogFileRetentionDays() != null)
                ? Math.max(1, s.getAppLogFileRetentionDays())
                : 90;
        long cutoffMillis = System.currentTimeMillis() - retentionDays * 86_400_000L;
        Set<Path> dirs = new LinkedHashSet<>();
        dirs.add(Paths.get("logs"));
        if (loggingFileName != null && !loggingFileName.isBlank()) {
            try {
                Path lp = Paths.get(loggingFileName.trim()).toAbsolutePath().normalize();
                Path parent = lp.getParent();
                if (parent != null) {
                    dirs.add(parent);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!name.endsWith(".log") && !name.endsWith(".gz") && !name.endsWith(".zip")) {
                        return;
                    }
                    try {
                        long lm = Files.getLastModifiedTime(path).toMillis();
                        if (lm < cutoffMillis) {
                            Files.deleteIfExists(path);
                            log.info("app log file retention ({} days): deleted {}", retentionDays, path);
                        }
                    } catch (IOException ex) {
                        log.debug("app log purge skip {}: {}", path, ex.getMessage());
                    }
                });
            } catch (IOException e) {
                log.warn("app log file retention scan failed for {}: {}", dir, e.getMessage());
            }
        }
    }

    private static Optional<Integer> effectivePurge(String id, String json) {
        for (DataRetentionCatalog.Entry e : DataRetentionCatalog.ENTRIES) {
            if (e.id().equals(id)) {
                return DataRetentionCatalog.effectivePurgeDays(e, json);
            }
        }
        return Optional.empty();
    }
}
