package com.pg.service;



import com.pg.api.dto.PageResult;

import com.pg.api.dto.PayListItemDto;

import com.pg.entity.JpayPortalAccount;

import com.pg.entity.JpayPortalExportCache;

import com.pg.entity.MerchantPgBinding;

import com.pg.entity.MerchantProfile;

import com.pg.entity.OrgLevel;

import com.pg.entity.OrgUnit;

import com.pg.entity.PgTrnsctn;

import com.pg.integration.pg.PgVendor;

import com.pg.repository.OrgUnitRepository;

import com.pg.repository.JpayPortalExportCacheRepository;

import com.pg.repository.MerchantPgBindingRepository;

import com.pg.repository.MerchantProfileRepository;

import com.pg.repository.PgTrnsctnRepository;

import com.pg.util.JpayNotifyStatusResolver;
import com.pg.util.JpayPortalDateParser;

import com.pg.util.JpayReconcileStatusPolicy;

import com.pg.util.JpayTradeStatusMapper;

import com.pg.util.MerchantDisplayCurrencyResolver;

import com.pg.util.NotifyToTxnStatusMerge;

import com.pg.util.TxnOutcomeReasonApplier;

import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.PayListStatusBarBuckets;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Lazy;

import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;

import org.springframework.transaction.annotation.Transactional;



import java.nio.file.Files;

import java.nio.file.Path;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.ZoneId;

import java.time.format.DateTimeFormatter;

import java.time.temporal.ChronoUnit;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.math.BigDecimal;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import java.util.List;

import java.util.Set;

import java.util.Locale;

import java.util.Map;

import java.util.Optional;

import java.util.stream.Collectors;



/**

 * JPAY 통합내역 — 포털 Export 자동 다운로드·파싱·ICOPAY 대조·목록 제공.

 * 총판(MASTER_DIST)별 포털 계정을 순회하며 Export를 병합합니다.

 */

@Service

public class JpayIntegratedListService {



    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    private final JpayPortalAccountService jpayPortalAccountService;

    private final JpayPortalExportRunner portalExportRunner;

    private final JpayOrderExcelParseService excelParseService;

    private final PgTrnsctnRepository pgTrnsctnRepository;

    private final OrgUnitRepository orgUnitRepository;

    private final OrgAccessService orgAccessService;

    private final OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator;

    private final JpayPortalExportCacheRepository exportCacheRepository;

    private final MerchantProfileRepository merchantProfileRepository;

    private final MerchantPgBindingRepository merchantPgBindingRepository;

    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;

    private final PayListService payListService;

    private final ObjectMapper objectMapper;



    private volatile LocalDateTime lastSyncAt;

    private volatile String lastSyncMessage = "";

    private volatile LocalDate syncCountDate;

    private volatile int syncCountToday;

    private volatile LocalDate scheduleSyncCountDate;

    private volatile int scheduleSyncCountToday;

    /** 매일 00:00 기본 동기화 마지막 수행 기준일(전산 타임존) */
    private volatile LocalDate lastBasicMidnightSyncDate;

    private volatile List<Map<String, Object>> cachedRows = List.of();

    /** @Transactional 프록시를 통해 백그라운드에서 동기화를 실행하기 위한 자기 참조 */
    @Autowired
    @Lazy
    private JpayIntegratedListService self;

    /** 포털 Export는 수 분 소요 — HTTP 요청과 분리된 단일 백그라운드 스레드에서 실행 */
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jpay-portal-sync");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean syncRunning = false;

    private volatile String syncJobStatus = "IDLE"; // IDLE, RUNNING, DONE, ERROR

    private volatile String syncJobMessage = "";

    private volatile LocalDateTime syncJobStartedAt;

    private volatile LocalDateTime syncJobFinishedAt;

    private volatile LocalDate syncJobFrom;

    private volatile LocalDate syncJobTo;

    private volatile Map<String, Object> syncJobResult;



    public JpayIntegratedListService(HqLedgerSysSettingsService hqLedgerSysSettingsService,

                                     JpayPortalAccountService jpayPortalAccountService,

                                     JpayPortalExportRunner portalExportRunner,

                                     JpayOrderExcelParseService excelParseService,

                                     PgTrnsctnRepository pgTrnsctnRepository,

                                     OrgUnitRepository orgUnitRepository,

                                     OrgAccessService orgAccessService,

                                     OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator,

                                     JpayPortalExportCacheRepository exportCacheRepository,

                                     MerchantProfileRepository merchantProfileRepository,

                                     MerchantPgBindingRepository merchantPgBindingRepository,

                                     MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,

                                     PayListService payListService,

                                     ObjectMapper objectMapper) {

        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;

        this.jpayPortalAccountService = jpayPortalAccountService;

        this.portalExportRunner = portalExportRunner;

        this.excelParseService = excelParseService;

        this.pgTrnsctnRepository = pgTrnsctnRepository;

        this.orgUnitRepository = orgUnitRepository;

        this.orgAccessService = orgAccessService;

        this.outcomeReasonWarmCoordinator = outcomeReasonWarmCoordinator;

        this.exportCacheRepository = exportCacheRepository;

        this.merchantProfileRepository = merchantProfileRepository;

        this.merchantPgBindingRepository = merchantPgBindingRepository;

        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;

        this.payListService = payListService;

        this.objectMapper = objectMapper;

    }



    /** 서버 기동 시 마지막 동기화 목록을 DB에서 메모리로 복원 */
    @PostConstruct
    void warmCacheFromDatabase() {
        loadCacheFromDatabaseIfEmpty();
    }



    private synchronized boolean loadCacheFromDatabaseIfEmpty() {

        if (!cachedRows.isEmpty()) {

            return true;

        }

        return exportCacheRepository.findById(JpayPortalExportCache.DEFAULT_KEY)

                .map(this::applyCacheEntity)

                .orElse(false);

    }

    /** 통합조회·조회통합·통합체크 조회 전 DB(tb_jpay_portal_export_cache) → 메모리 복원 */
    public synchronized void ensureExportCacheLoaded() {
        loadCacheFromDatabaseIfEmpty();
    }



    private boolean applyCacheEntity(JpayPortalExportCache ent) {

        try {

            String json = ent.getRowsJson() != null ? ent.getRowsJson() : "[]";

            List<Map<String, Object>> rows = objectMapper.readValue(json, new TypeReference<>() {});

            if (rows == null || rows.isEmpty()) {

                return false;

            }

            cachedRows = List.copyOf(dedupeJpayRows(repairEnrichedRows(rows)));

            lastSyncAt = ent.getSyncedAt();

            lastSyncMessage = ent.getLastSyncMessage() != null ? ent.getLastSyncMessage() : "";

            syncCountDate = ent.getSyncCountDate();

            syncCountToday = ent.getSyncCountToday();

            scheduleSyncCountDate = ent.getScheduleSyncCountDate();

            scheduleSyncCountToday = ent.getScheduleSyncCountToday();

            lastBasicMidnightSyncDate = ent.getLastBasicSyncDate();

            return true;

        } catch (Exception ignored) {

            return false;

        }

    }



    private void persistCacheToDatabase(LocalDate exportFrom, LocalDate exportTo) {

        try {

            JpayPortalExportCache ent = exportCacheRepository.findById(JpayPortalExportCache.DEFAULT_KEY)

                    .orElseGet(JpayPortalExportCache::new);

            ent.setCacheKey(JpayPortalExportCache.DEFAULT_KEY);

            ent.setSyncedAt(lastSyncAt);

            ent.setLastSyncMessage(lastSyncMessage);

            ent.setExportFrom(exportFrom);

            ent.setExportTo(exportTo);

            ent.setRowsJson(objectMapper.writeValueAsString(cachedRows));

            ent.setSyncCountDate(syncCountDate);

            ent.setSyncCountToday(syncCountToday);

            ent.setScheduleSyncCountDate(scheduleSyncCountDate);

            ent.setScheduleSyncCountToday(scheduleSyncCountToday);

            ent.setLastBasicSyncDate(lastBasicMidnightSyncDate);

            exportCacheRepository.save(ent);

        } catch (Exception ignored) {

            /* DB 저장 실패해도 동기화·메모리 목록은 유지 */

        }

    }



    private record JpayExportPlan(LocalDate exportFrom, LocalDate exportTo, JpaySyncTrigger trigger, boolean mergeIntoCache) {}

    private ZoneId ledgerDisplayZone() {
        return hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
    }

    private int scheduleSyncCountForToday(LocalDate today) {
        if (scheduleSyncCountDate != null && scheduleSyncCountDate.equals(today)) {
            return scheduleSyncCountToday;
        }
        return 0;
    }

    private JpayExportPlan resolveExportPlan(LocalDate reqFrom, LocalDate reqTo, JpaySyncTrigger trigger) {
        var settings = hqLedgerSysSettingsService.getOrCreate();
        LocalDate today = LocalDate.now(ledgerDisplayZone());
        ensureExportCacheLoaded();
        boolean cacheEmpty = cachedRows == null || cachedRows.isEmpty();

        if (cacheEmpty) {
            int months = settings.getJpayTrInitSyncMonths() != null ? settings.getJpayTrInitSyncMonths() : 3;
            months = Math.max(1, Math.min(120, months));
            return new JpayExportPlan(today.minusMonths(months), today, JpaySyncTrigger.INITIAL_BOOTSTRAP, false);
        }

        if (trigger == JpaySyncTrigger.BASIC_MIDNIGHT) {
            return new JpayExportPlan(today.minusDays(1), today, trigger, true);
        }

        if (trigger == JpaySyncTrigger.FULL_RESYNC) {
            int months = settings.getJpayTrInitSyncMonths() != null ? settings.getJpayTrInitSyncMonths() : 3;
            months = Math.max(1, Math.min(120, months));
            return new JpayExportPlan(today.minusMonths(months), today, trigger, true);
        }

        if (trigger == JpaySyncTrigger.SCHEDULED) {
            int countBefore = scheduleSyncCountForToday(today);
            if (countBefore <= 0) {
                return new JpayExportPlan(today.minusDays(1), today, trigger, true);
            }
            return new JpayExportPlan(today, today, trigger, true);
        }

        if (trigger == JpaySyncTrigger.EXPLICIT_RANGE && (reqFrom != null || reqTo != null)) {
            LocalDate ef = reqFrom != null ? reqFrom : reqTo;
            LocalDate et = reqTo != null ? reqTo : reqFrom;
            if (ef.isAfter(et)) {
                LocalDate swap = ef;
                ef = et;
                et = swap;
            }
            return new JpayExportPlan(ef, et, trigger, true);
        }

        if (trigger == JpaySyncTrigger.MANUAL) {
            return new JpayExportPlan(today, today, trigger, true);
        }

        return new JpayExportPlan(today, today, trigger, true);
    }

    private static String syncTriggerLabel(JpaySyncTrigger trigger) {
        if (trigger == null) {
            return "";
        }
        return switch (trigger) {
            case BASIC_MIDNIGHT -> "기본(00:00·2일)";
            case SCHEDULED -> "스케줄";
            case MANUAL -> "수동(당일)";
            case FULL_RESYNC -> "전체재동기화";
            case EXPLICIT_RANGE -> "기간지정";
            case INITIAL_BOOTSTRAP -> "초기적재";
        };
    }

    private List<Map<String, Object>> mergeCacheByTrnDateRange(List<Map<String, Object>> existing,
                                                                 List<Map<String, Object>> incoming,
                                                                 LocalDate from, LocalDate to) {
        List<Map<String, Object>> kept = new ArrayList<>();
        if (existing != null) {
            for (Map<String, Object> row : existing) {
                Optional<LocalDate> ld = JpayPortalDateParser.rowTrnDate(row);
                if (ld.isPresent() && !ld.get().isBefore(from) && !ld.get().isAfter(to)) {
                    continue;
                }
                kept.add(row);
            }
        }
        if (incoming != null && !incoming.isEmpty()) {
            kept.addAll(incoming);
        }
        return dedupeJpayRows(kept);
    }

    private synchronized void repairAndDedupeCachedRows() {
        if (cachedRows == null || cachedRows.isEmpty()) {
            return;
        }
        int before = cachedRows.size();
        cachedRows = List.copyOf(dedupeJpayRows(repairEnrichedRows(new ArrayList<>(cachedRows))));
        if (cachedRows.size() != before) {
            persistCurrentCacheRows();
        }
    }

    private void persistCurrentCacheRows() {
        try {
            JpayPortalExportCache ent = exportCacheRepository.findById(JpayPortalExportCache.DEFAULT_KEY)
                    .orElseGet(JpayPortalExportCache::new);
            ent.setCacheKey(JpayPortalExportCache.DEFAULT_KEY);
            ent.setRowsJson(objectMapper.writeValueAsString(cachedRows));
            if (lastSyncAt != null) {
                ent.setSyncedAt(lastSyncAt);
            }
            ent.setLastSyncMessage(lastSyncMessage);
            ent.setSyncCountDate(syncCountDate);
            ent.setSyncCountToday(syncCountToday);
            ent.setScheduleSyncCountDate(scheduleSyncCountDate);
            ent.setScheduleSyncCountToday(scheduleSyncCountToday);
            ent.setLastBasicSyncDate(lastBasicMidnightSyncDate);
            exportCacheRepository.save(ent);
        } catch (Exception ignored) {
            /* DB 저장 실패해도 메모리 목록은 유지 */
        }
    }

    /** 승인번호·주문번호 기준 중복 제거 — 성공·승인번호 있는 행 우선 */
    static List<Map<String, Object>> dedupeJpayRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<String, Map<String, Object>> byTxnId = new LinkedHashMap<>();
        Map<String, Map<String, Object>> byOrderOnly = new LinkedHashMap<>();
        List<Map<String, Object>> anon = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            String txnKey = jpayTxnDedupKey(row);
            if (!txnKey.isEmpty()) {
                byTxnId.merge(txnKey, row, JpayIntegratedListService::preferJpayDuplicateRow);
                continue;
            }
            String orderKey = jpayOrderDedupKey(row);
            if (orderKey.isEmpty()) {
                anon.add(row);
            } else {
                byOrderOnly.merge(orderKey, row, JpayIntegratedListService::preferJpayDuplicateRow);
            }
        }
        Set<String> successOrders = new LinkedHashSet<>();
        for (Map<String, Object> row : byTxnId.values()) {
            if (jpayRowQualityScore(row) >= 500) {
                String ok = jpayOrderDedupKey(row);
                if (!ok.isEmpty()) {
                    successOrders.add(ok);
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>(byTxnId.values());
        for (Map.Entry<String, Map<String, Object>> e : byOrderOnly.entrySet()) {
            if (successOrders.contains(e.getKey()) && jpayRowQualityScore(e.getValue()) < 500) {
                continue;
            }
            out.add(e.getValue());
        }
        out.addAll(anon);
        return out;
    }

    static String jpayRowDedupKey(Map<String, Object> row) {
        String txnKey = jpayTxnDedupKey(row);
        if (!txnKey.isEmpty()) {
            return txnKey;
        }
        return jpayOrderDedupKey(row);
    }

    private static String jpayTxnDedupKey(Map<String, Object> row) {
        String txnId = String.valueOf(row.getOrDefault("transactionId", "")).trim();
        if (txnId.isEmpty()) {
            return "";
        }
        return "T:" + txnId;
    }

    private static String jpayOrderDedupKey(Map<String, Object> row) {
        String orderNo = String.valueOf(row.getOrDefault("orderNo", "")).trim();
        if (orderNo.isEmpty()) {
            return "";
        }
        return "O:" + orderNo.toLowerCase(Locale.ROOT);
    }

    private static int jpayRowQualityScore(Map<String, Object> row) {
        if (row == null) {
            return 0;
        }
        int score = 0;
        if (!String.valueOf(row.getOrDefault("transactionId", "")).trim().isEmpty()) {
            score += 1000;
        }
        String code = resolveJpayRowStatusCode(row);
        if ("10".equals(code)) {
            score += 500;
        } else if (PayListStatusBarBuckets.SUCCESS.equals(PayListStatusBarBuckets.bucketForPgStatus(code))) {
            score += 500;
        } else if (PayListStatusBarBuckets.VOID.equals(PayListStatusBarBuckets.bucketForPgStatus(code))
                || PayListStatusBarBuckets.EMAIL_VOID.equals(PayListStatusBarBuckets.bucketForPgStatus(code))) {
            score -= 300;
        }
        String trading = portalTradingText(row);
        if (trading.toLowerCase(Locale.ROOT).contains("success")) {
            score += 200;
        }
        return score;
    }

    private static Map<String, Object> preferJpayDuplicateRow(Map<String, Object> a, Map<String, Object> b) {
        if (jpayRowQualityScore(b) > jpayRowQualityScore(a)) {
            return new LinkedHashMap<>(b);
        }
        if (jpayRowQualityScore(b) < jpayRowQualityScore(a)) {
            return new LinkedHashMap<>(a);
        }
        Map<String, Object> merged = new LinkedHashMap<>(a);
        for (Map.Entry<String, Object> e : b.entrySet()) {
            Object v = e.getValue();
            if (v == null || String.valueOf(v).trim().isEmpty()) {
                continue;
            }
            merged.put(e.getKey(), v);
        }
        String la = String.valueOf(a.getOrDefault("portalLabel", "")).trim();
        String lb = String.valueOf(b.getOrDefault("portalLabel", "")).trim();
        if (!la.isEmpty() && !lb.isEmpty() && !la.equals(lb)) {
            merged.put("portalLabel", la + " / " + lb);
        }
        return merged;
    }

    private static List<Map<String, String>> dedupeJpayRawRows(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<String, Map<String, String>> byKey = new LinkedHashMap<>();
        int anon = 0;
        for (Map<String, String> row : rows) {
            if (row == null) {
                continue;
            }
            String txnId = resolvePortalTransactionId(row);
            String key = txnId.isBlank()
                    ? (resolvePortalOrderNo(row).isBlank()
                    ? "@" + (anon++)
                    : "O:" + resolvePortalOrderNo(row).trim().toLowerCase(Locale.ROOT))
                    : "T:" + txnId.trim();
            Map<String, String> prev = byKey.get(key);
            if (prev == null || jpayRawRowQualityScore(row) > jpayRawRowQualityScore(prev)) {
                byKey.put(key, row);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static int jpayRawRowQualityScore(Map<String, String> row) {
        if (row == null) {
            return 0;
        }
        int score = 0;
        if (!resolvePortalTransactionId(row).isBlank()) {
            score += 1000;
        }
        String trading = JpayOrderExcelParseService.col(row, "Trading Status", "trading_status");
        if (trading.toLowerCase(Locale.ROOT).contains("success")) {
            score += 500;
        }
        return score;
    }

    @Transactional
    public Map<String, Object> syncFromPortal(LocalDate from, LocalDate to) throws Exception {
        return syncFromPortal(from, to, JpaySyncTrigger.SCHEDULED);
    }

    @Transactional
    public Map<String, Object> syncFromPortal(LocalDate from, LocalDate to, JpaySyncTrigger trigger) throws Exception {

        if (trigger == null) {
            trigger = JpaySyncTrigger.SCHEDULED;
        }
        ensureExportCacheLoaded();
        JpayExportPlan plan = resolveExportPlan(from, to, trigger);
        LocalDate exportFrom = plan.exportFrom();
        LocalDate exportTo = plan.exportTo();
        JpaySyncTrigger appliedTrigger = plan.trigger();
        boolean mergeIntoCache = plan.mergeIntoCache();

        List<JpayPortalAccount> accounts = jpayPortalAccountService.listActiveForSync();
        List<Map<String, String>> allRaw = new ArrayList<>();
        int matched = 0;
        int updated = 0;
        int unmatched = 0;
        int parseScanned = 0;
        List<String> accountMessages = new ArrayList<>();

        var settings = hqLedgerSysSettingsService.getOrCreate();

        if (accounts.isEmpty()) {

            String user = settings.getJpayPortalUsername();

            String pass = settings.getJpayPortalPassword();

            if (user == null || user.isBlank() || pass == null || pass.isBlank()) {

                throw new IllegalStateException(

                        "JPAY 포털 계정이 없습니다. 본사설정 > 결제대행사로직에서 총판별 계정을 등록하세요.");

            }

            ExportParseResult legacy = exportAndParse(exportFrom, exportTo, user, pass, null);

            allRaw.addAll(legacy.rows);

            parseScanned += legacy.report.dataRowsScanned();

            ReconcileSummary summary = reconcileRows(allRaw, null);

            matched = summary.matched();

            updated = summary.updated();

            unmatched = summary.unmatched();

            accountMessages.add("레거시 단일계정 " + allRaw.size() + "건");

        } else {

            for (JpayPortalAccount acc : accounts) {

                ExportParseResult parsed = exportAndParse(exportFrom, exportTo,

                        acc.getPortalUsername(), acc.getPortalPassword(), acc);

                List<Map<String, String>> raw = parsed.rows;

                parseScanned += parsed.report.dataRowsScanned();

                allRaw.addAll(raw);

                ReconcileSummary s = reconcileRows(raw, acc.getMasterCompCode());

                matched += s.matched();

                updated += s.updated();

                unmatched += s.unmatched();

                String label = acc.getLabel() != null && !acc.getLabel().isBlank()

                        ? acc.getLabel() : acc.getMasterCompCode();

                accountMessages.add(label + " " + raw.size() + "건");

            }

        }

        allRaw = dedupeJpayRawRows(allRaw);

        List<Map<String, Object>> fetchedRows = enrichRows(allRaw);
        fetchedRows = repairEnrichedRows(fetchedRows);

        if (mergeIntoCache) {
            cachedRows = mergeCacheByTrnDateRange(cachedRows, fetchedRows, exportFrom, exportTo);
        } else {
            cachedRows = fetchedRows;
        }

        cachedRows = List.copyOf(dedupeJpayRows(repairEnrichedRows(cachedRows)));

        lastSyncAt = LocalDateTime.now(ledgerDisplayZone());

        recordSyncCountForToday();

        if (appliedTrigger == JpaySyncTrigger.SCHEDULED) {
            recordScheduleSyncCountForToday();
        }

        if (appliedTrigger == JpaySyncTrigger.BASIC_MIDNIGHT) {
            lastBasicMidnightSyncDate = LocalDate.now(ledgerDisplayZone());
        }

        StringBuilder msg = new StringBuilder();

        msg.append('[').append(syncTriggerLabel(appliedTrigger)).append("] ");
        msg.append("JPAY 포털 Export ").append(allRaw.size()).append("건 (").append(String.join(", ", accountMessages))
                .append(") — 조회 목록 ").append(cachedRows.size()).append("건 — ICOPAY DB 반영 ").append(updated).append("건");
        msg.append(" [포털조회 ").append(exportFrom).append("~").append(exportTo).append(']');
        if (mergeIntoCache) {
            msg.append(" — 해당 구간만 캐시 교체·과거 데이터 유지");
        }

        if (allRaw.isEmpty()) {

            if (parseScanned > 0) {

                msg.append(" — 엑셀 ").append(parseScanned).append("행이나 승인번호·주문번호를 읽지 못했습니다.");

            } else {

                msg.append(" — 해당 기간 포털 거래가 없습니다.");

            }

        }

        lastSyncMessage = msg.toString();

        persistCacheToDatabase(exportFrom, exportTo);

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("fromDate", exportFrom.toString());

        out.put("toDate", exportTo.toString());

        out.put("exportFromDate", exportFrom.toString());

        out.put("exportToDate", exportTo.toString());

        out.put("syncTrigger", appliedTrigger.name());

        out.put("mergeIntoCache", mergeIntoCache);

        out.put("portalRows", allRaw.size());

        out.put("listRows", cachedRows.size());

        out.put("accountCount", accounts.isEmpty() ? 1 : accounts.size());

        out.put("matched", matched);

        out.put("updated", updated);

        out.put("unmatched", unmatched);

        out.put("syncedAt", lastSyncAt.toString());

        out.put("message", lastSyncMessage);

        return out;

    }



    private static final class ExportParseResult {

        final List<Map<String, String>> rows;

        final JpayOrderExcelParseService.ParseReport report;

        ExportParseResult(List<Map<String, String>> rows, JpayOrderExcelParseService.ParseReport report) {

            this.rows = rows;

            this.report = report;

        }

    }



    private ExportParseResult exportAndParse(LocalDate from, LocalDate to,

                                                     String user, String pass,

                                                     JpayPortalAccount acc) throws Exception {

        Path xlsx = portalExportRunner.runExport(user, pass, from.toString(), to.toString());

        try {

            JpayOrderExcelParseService.ParseReport report = excelParseService.parseFileReport(xlsx);

            List<Map<String, String>> raw = new ArrayList<>(report.rows());

            if (acc != null) {

                for (Map<String, String> row : raw) {

                    row.put("_portalAccountId", String.valueOf(acc.getId()));

                    row.put("_masterCompCode", acc.getMasterCompCode() != null ? acc.getMasterCompCode() : "");

                    row.put("_portalLabel", acc.getLabel() != null ? acc.getLabel() : "");

                    row.put("_portalPgCd", acc.getPgCd() != null ? acc.getPgCd() : "");

                }

            }

            return new ExportParseResult(raw, report);

        } finally {

            try {

                Files.deleteIfExists(xlsx);

            } catch (Exception ignored) {

                /* temp cleanup */

            }

        }

    }



    public PageResult<Map<String, Object>> search(int page, int size,

                                                  String searchKeyword,

                                                  String searchOrderNo,

                                                  String searchPayDivCd,

                                                  LocalDate from, LocalDate to,

                                                  boolean triggerSyncIfEmpty,

                                                  String searchFieldType,

                                                  Authentication authentication) throws Exception {

        if (cachedRows.isEmpty()) {

            loadCacheFromDatabaseIfEmpty();

        }

        if (!cachedRows.isEmpty()) {

            repairAndDedupeCachedRows();

        }

        if (cachedRows.isEmpty() && triggerSyncIfEmpty) {

            syncFromPortal(from, to, JpaySyncTrigger.EXPLICIT_RANGE);

        }

        List<Map<String, Object>> filtered = filterRows(cachedRows, searchKeyword, searchOrderNo, searchPayDivCd, from, to, searchFieldType);

        int p = Math.max(1, page);

        int sz = Math.min(500, Math.max(1, size));

        int start = (p - 1) * sz;

        int end = Math.min(start + sz, filtered.size());

        List<Map<String, Object>> slice = start < filtered.size() ? filtered.subList(start, end) : List.of();

        int rowNo = start + 1;

        for (Map<String, Object> row : slice) {

            row.put("rowNo", rowNo++);

        }

        PageResult<Map<String, Object>> pr = new PageResult<>();

        pr.setList(slice);

        pr.setPage(p);

        pr.setSize(sz);

        pr.setTotalElements(filtered.size());

        pr.setTotalPages(sz > 0 ? (int) Math.ceil(filtered.size() / (double) sz) : 0);

        Map<String, Object> meta = new LinkedHashMap<>();

        meta.put("jpayIntegrated", true);

        meta.putAll(jpaySyncMetaMap());

        meta.put("lastSyncMessage", lastSyncMessage);

        long missingTrnDate = cachedRows.stream()

                .filter(r -> JpayPortalDateParser.rowTrnDate(r).isEmpty())

                .count();

        meta.put("cachedMissingTrnDate", missingTrnDate);

        meta.put("feeCurrencyFormatByCur",
                FeeCurrencyRoundResolver.from(hqLedgerSysSettingsService.getOrCreate()).toClientByCurrencyMap());

        meta.put("payListFinancialSummary", payListService.buildJpayFinancialSummary(filtered, authentication));

        meta.put("payListStatusBar", aggregateStatusBar(filtered));

        pr.setMeta(meta);

        return pr;

    }



    public Map<String, Object> syncMeta() {

        loadCacheFromDatabaseIfEmpty();

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");

        m.put("lastSyncMessage", lastSyncMessage);

        m.put("cachedTotal", cachedRows.size());

        return m;

    }



    /**
     * 비동기 동기화 시작 — 이미 진행 중이면 현재 상태만 반환합니다.
     * HTTP 요청을 붙잡지 않으므로 프록시·게이트웨이 504(타임아웃)가 발생하지 않습니다.
     */
    public synchronized Map<String, Object> startSyncJob(LocalDate from, LocalDate to) {
        return startSyncJob(from, to, JpaySyncTrigger.SCHEDULED);
    }

    public synchronized Map<String, Object> startSyncJob(LocalDate from, LocalDate to, JpaySyncTrigger trigger) {

        if (syncRunning) {

            return syncJobStatusMap();

        }

        syncRunning = true;

        syncJobStatus = "RUNNING";

        syncJobStartedAt = LocalDateTime.now(ledgerDisplayZone());

        syncJobFinishedAt = null;

        syncJobFrom = from;

        syncJobTo = to;

        syncJobMessage = "JPAY 포털 Export 진행 중입니다. 계정·기간에 따라 수 분 걸릴 수 있습니다.";

        syncJobResult = null;

        final LocalDate f = from;

        final LocalDate t = to;

        final JpaySyncTrigger trig = trigger != null ? trigger : JpaySyncTrigger.SCHEDULED;

        syncExecutor.submit(() -> {

            try {

                Map<String, Object> res = self.syncFromPortal(f, t, trig);

                syncJobResult = res;

                syncJobMessage = String.valueOf(res.getOrDefault("message", "동기화 완료"));

                syncJobStatus = "DONE";

            } catch (Exception e) {

                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

                syncJobMessage = "JPAY 포털 동기화 실패: " + msg;

                syncJobStatus = "ERROR";

            } finally {

                syncJobFinishedAt = LocalDateTime.now(ledgerDisplayZone());

                syncRunning = false;

            }

        });

        return syncJobStatusMap();

    }

    /**
     * 매일 00:00 기본 동기화 — 스케줄과 별도, 어제·오늘 2일 구간만 포털에서 받아 캐시 해당 구간 교체.
     */
    public synchronized void runBasicMidnightSyncIfDue() {
        LocalDate today = LocalDate.now(ledgerDisplayZone());
        ensureExportCacheLoaded();
        if (lastBasicMidnightSyncDate != null && lastBasicMidnightSyncDate.equals(today)) {
            return;
        }
        if (syncRunning) {
            return;
        }
        startSyncJob(null, null, JpaySyncTrigger.BASIC_MIDNIGHT);
    }



    /** 비동기 동기화 진행 상태 — 프론트가 폴링합니다. */
    public Map<String, Object> syncJobStatusMap() {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("status", syncJobStatus);

        m.put("running", syncRunning);

        m.put("message", syncJobMessage);

        m.put("startedAt", syncJobStartedAt != null ? syncJobStartedAt.toString() : "");

        m.put("finishedAt", syncJobFinishedAt != null ? syncJobFinishedAt.toString() : "");

        m.put("fromDate", syncJobFrom != null ? syncJobFrom.toString() : "");

        m.put("toDate", syncJobTo != null ? syncJobTo.toString() : "");

        if (syncJobStartedAt != null) {

            long elapsed = ChronoUnit.SECONDS.between(syncJobStartedAt,

                    syncJobFinishedAt != null ? syncJobFinishedAt : LocalDateTime.now());

            m.put("elapsedSeconds", Math.max(0, elapsed));

        } else {

            m.put("elapsedSeconds", 0L);

        }

        m.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");

        m.put("lastSyncMessage", lastSyncMessage);

        m.put("syncCountToday", syncCountToday);

        m.put("cachedTotal", cachedRows.size());

        if (syncJobResult != null) {

            m.put("result", syncJobResult);

        }

        return m;

    }



    /** 통합조회·조회통합·통합체크 요약 — 최근 동기화 시각·당일 횟수 */
    public Map<String, Object> jpaySyncMetaMap() {
        ensureExportCacheLoaded();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");
        m.put("syncCountToday", syncCountToday);
        m.put("cachedTotal", cachedRows.size());
        return m;
    }



    private void recordSyncCountForToday() {
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        if (syncCountDate != null && syncCountDate.equals(today)) {
            syncCountToday++;
        } else {
            syncCountDate = today;
            syncCountToday = 1;
        }
    }

    private void recordScheduleSyncCountForToday() {
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        if (scheduleSyncCountDate != null && scheduleSyncCountDate.equals(today)) {
            scheduleSyncCountToday++;
        } else {
            scheduleSyncCountDate = today;
            scheduleSyncCountToday = 1;
        }
    }



    private ReconcileSummary reconcileRows(List<Map<String, String>> raw, String masterCompCode) {

        int matched = 0;

        int updated = 0;

        int unmatched = 0;

        for (Map<String, String> row : raw) {

            String orderNo = JpayOrderExcelParseService.col(row, "Merchant Order Number", "pay_orderid", "orderid");

            String txnId = JpayOrderExcelParseService.col(row, "Transaction ID", "transaction_id");

            if (orderNo.isBlank() && txnId.isBlank()) {

                unmatched++;

                continue;

            }

            Optional<PgTrnsctn> opt = findTxn(orderNo, txnId);

            if (opt.isEmpty()) {

                unmatched++;

                continue;

            }

            PgTrnsctn t = opt.get();

            if (masterCompCode != null && !masterCompCode.isBlank()) {

                String merchantId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";

                if (merchantId.isEmpty()

                        || !orgAccessService.isTargetUnderViewerOrg(masterCompCode, merchantId)) {

                    unmatched++;

                    continue;

                }

            }

            matched++;

            if (!PgVendor.isJpayFamily(t.getVan())) {

                continue;

            }

            String trading = JpayOrderExcelParseService.col(row, "Trading Status");

            String chargeback = JpayOrderExcelParseService.col(row, "Is it a chargeback?");

            String rdr = JpayOrderExcelParseService.col(row, "RDR");

            String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading, chargeback, rdr);

            if (mapped == null || !JpayReconcileStatusPolicy.mayApplyReconcileMapping(mapped)) {

                continue;

            }

            String old = t.getStatus() != null ? t.getStatus().trim() : "";

            String merged = NotifyToTxnStatusMerge.merge(old, mapped, "RESULT", t.getOutcomeReasonCode());

            if (merged != null && !merged.equals(old)) {

                t.setStatus(merged);

                String rc = JpayTradeStatusMapper.returnCodeForInternalStatus(merged);
                t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(merged,
                        rc.isBlank() ? "00" : rc));

                if (!"10".equals(merged)) {

                    t.setPaidAt(null);

                }

                if (!txnId.isBlank()) {

                    t.setChillTransactionId(txnId);

                }

                Optional<String> recordedReason = TxnOutcomeReasonApplier.applyJpayReconcileOutcome(
                        t, old, merged, trading);
                outcomeReasonWarmCoordinator.onRecorded(recordedReason);

                pgTrnsctnRepository.save(t);

                updated++;

            }

        }

        return new ReconcileSummary(matched, updated, unmatched);

    }



    private Optional<PgTrnsctn> findTxn(String orderNo, String txnId) {

        if (txnId != null && !txnId.isBlank()) {

            Optional<PgTrnsctn> byTxn = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(txnId.trim());

            if (byTxn.isPresent()) {

                return byTxn;

            }

        }

        if (orderNo != null && !orderNo.isBlank()) {

            return findPreferredTxnByOrderNo(orderNo.trim());

        }

        return Optional.empty();

    }

    /** 동일 주문번호에 무효·성공 등 복수 건일 때 성공(10)+승인번호 우선 */
    private Optional<PgTrnsctn> findPreferredTxnByOrderNo(String orderNo) {
        List<PgTrnsctn> list = pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        for (PgTrnsctn t : list) {
            if (isSuccessStatus(t.getStatus()) && hasChillTransactionId(t)) {
                return Optional.of(t);
            }
        }
        for (PgTrnsctn t : list) {
            if (isSuccessStatus(t.getStatus())) {
                return Optional.of(t);
            }
        }
        return Optional.of(list.get(0));
    }

    private Optional<PgTrnsctn> findSuccessTxnByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        List<PgTrnsctn> list = pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo.trim());
        for (PgTrnsctn t : list) {
            if (isSuccessStatus(t.getStatus()) && hasChillTransactionId(t)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    private static boolean isSuccessStatus(String status) {
        return status != null && "10".equals(status.trim());
    }

    private static boolean hasChillTransactionId(PgTrnsctn t) {
        return t != null && t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank();
    }

    /**
     * 포털 Export에 승인번호가 없고 ICOPAY에 성공 건이 있으면 승인번호·성공 상태를 보강합니다.
     */
    private void alignRowWithSuccessTxnIfMissingApproval(Map<String, Object> m) {
        if (m == null) {
            return;
        }
        String txnId = String.valueOf(m.getOrDefault("transactionId", "")).trim();
        if (!txnId.isEmpty()) {
            return;
        }
        String orderNo = String.valueOf(m.getOrDefault("orderNo", "")).trim();
        if (orderNo.isEmpty()) {
            return;
        }
        Optional<PgTrnsctn> success = findSuccessTxnByOrderNo(orderNo);
        if (success.isEmpty()) {
            return;
        }
        PgTrnsctn t = success.get();
        String cid = t.getChillTransactionId();
        if (cid == null || cid.isBlank()) {
            return;
        }
        m.put("transactionId", cid.trim());
        if (!portalTradingIndicatesSuccess(m)) {
            m.put("status", "Success, Notified");
            m.put("tradingStatus", "Success, Notified");
            m.put("icopayStatus", "10");
        }
        m.put("dbStatus", t.getStatus());
        m.put("icopay", "Y");
        if (!m.containsKey("compId") || String.valueOf(m.getOrDefault("compId", "")).isBlank()) {
            m.put("compId", t.getMerchantId());
        }
    }

    private static boolean portalTradingIndicatesSuccess(Map<String, Object> m) {
        String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(
                portalTradingText(m),
                String.valueOf(m.getOrDefault("chargeback", "")),
                String.valueOf(m.getOrDefault("rdr", "")));
        return "10".equals(mapped);
    }



    private List<Map<String, Object>> enrichRows(List<Map<String, String>> raw) {

        List<Map<String, Object>> out = new ArrayList<>();

        Map<Long, Optional<MerchantProfile>> profileCache = new HashMap<>();

        for (Map<String, String> row : raw) {

            Map<String, Object> m = new LinkedHashMap<>();

            String orderNo = resolvePortalOrderNo(row);

            String txnId = resolvePortalTransactionId(row);

            if (orderNo.isBlank() && txnId.isBlank()) {

                continue;

            }

            String trading = JpayOrderExcelParseService.col(row, "Trading Status", "trading_status");

            String amount = JpayOrderExcelParseService.col(row, "Transaction Amount", "transaction_amount");

            String currency = JpayOrderExcelParseService.col(row, "Transaction Currency", "Original Currency", "original_currency");

            String originalCurrency = JpayOrderExcelParseService.col(row, "Original Currency", "original_currency");

            if (currency.isBlank() && !originalCurrency.isBlank()) {

                currency = originalCurrency;

            }

            if (!currency.isBlank()) {

                currency = PayListStatusBarBuckets.normalizeCurrency(currency);

            }

            if (!originalCurrency.isBlank()) {

                m.put("originalCurrency", PayListStatusBarBuckets.normalizeCurrency(originalCurrency));

            }

            String mid = JpayOrderExcelParseService.col(row, "Gateway Access Number", "gateway_access_number");

            String txnDate = resolvePortalTransactionDate(row);

            String masterCode = row.getOrDefault("_masterCompCode", "");

            String portalLabel = row.getOrDefault("_portalLabel", "");

            String portalPgCd = row.getOrDefault("_portalPgCd", "");

            m.put("transactionId", txnId);

            m.put("orderNo", orderNo);

            m.put("merchant", mid);

            m.put("amount", amount);

            m.put("currency", currency);

            m.put("status", trading);

            m.put("tradingStatus", trading);

            m.put("transactionDate", txnDate);

            m.put("masterDistCompId", masterCode);

            m.put("portalLabel", portalLabel);

            m.put("portalPgCd", portalPgCd);

            if (!masterCode.isBlank()) {

                orgUnitRepository.findByCodeIgnoreCase(masterCode)

                        .map(OrgUnit::getName)

                        .ifPresent(nm -> m.put("masterDistNm", nm));

            }

            JpayPortalDateParser.applyDateTimeFields(txnDate, m);

            if (!m.containsKey("trnDate") || String.valueOf(m.get("trnDate")).isBlank()) {

                for (String alt : new String[]{"Transaction Date", "transaction_date", "交易日期"}) {

                    String dv = row.get(alt);

                    if (dv != null && !dv.isBlank()) {

                        JpayPortalDateParser.applyDateTimeFields(dv, m);

                        if (m.containsKey("trnDate")) {

                            break;

                        }

                    }

                }

            }

            String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading,

                    JpayOrderExcelParseService.col(row, "Is it a chargeback?"),

                    JpayOrderExcelParseService.col(row, "RDR"));

            m.put("icopayStatus", mapped != null ? mapped : "");

            Optional<PgTrnsctn> txn = findTxn(orderNo, txnId);

            if (txn.isPresent()) {

                PgTrnsctn t = txn.get();

                m.put("icopay", "Y");

                m.put("trnId", t.getTrnId());

                m.put("compId", t.getMerchantId());

                m.put("dbStatus", t.getStatus());

                orgUnitRepository.findByCodeIgnoreCase(t.getMerchantId() != null ? t.getMerchantId().trim() : "")

                        .map(OrgUnit::getName)

                        .ifPresent(nm -> m.put("compNm", nm));

            } else {

                m.put("icopay", "N");

            }

            m.put("fee", JpayOrderExcelParseService.col(row, "Fee", "fee"));

            String customerEmail = JpayOrderExcelParseService.col(row, "Customer Email", "customer_email");

            String customerName = JpayOrderExcelParseService.col(row, "Customer Name", "customer_name");

            m.put("customerEmail", customerEmail);

            m.put("customerName", customerName);

            m.put("refundStatus", JpayOrderExcelParseService.col(row, "Refund Status", "refund_status"));

            m.put("chargeback", JpayOrderExcelParseService.col(row, "Is it a chargeback?", "chargeback"));

            m.put("rdr", JpayOrderExcelParseService.col(row, "RDR", "rdr"));

            m.put("urlSource", JpayOrderExcelParseService.col(row, "URL Source", "url_source"));

            m.put("cardBin", JpayOrderExcelParseService.col(row, "Card BIN", "card_bin"));

            fillTxnFallbacks(m, txn);

            resolveCurrencyOnRow(m, txn, profileCache);

            alignRowWithSuccessTxnIfMissingApproval(m);

            applyStatusNm(m);

            out.add(m);

        }

        return out;

    }



    private static void splitDateTime(String raw, Map<String, Object> m) {

        JpayPortalDateParser.applyDateTimeFields(raw, m);

    }



    private static List<Map<String, Object>> filterRows(List<Map<String, Object>> rows,

                                                      String keyword, String orderNo, String payDivCd,

                                                      LocalDate from, LocalDate to, String fieldType) {

        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";

        String on = orderNo != null ? orderNo.trim().toLowerCase(Locale.ROOT) : "";

        String status = payDivCd != null ? payDivCd.trim() : "";

        String ft = fieldType != null ? fieldType.trim().toUpperCase(Locale.ROOT) : "";

        return rows.stream()

                .filter(r -> {

                    String txnId = String.valueOf(r.getOrDefault("transactionId", "")).trim();

                    String ord = String.valueOf(r.getOrDefault("orderNo", "")).trim();

                    if (txnId.isEmpty() && ord.isEmpty()) {

                        return false;

                    }

                    if (!on.isEmpty()) {

                        if (!ord.toLowerCase(Locale.ROOT).contains(on)) {

                            return false;

                        }

                    }

                    if (!status.isEmpty()) {

                        String rowCode = resolveJpayRowStatusCode(r);

                        if (!status.equals(rowCode)) {

                            return false;

                        }

                    }

                    if (from != null || to != null) {

                        Optional<LocalDate> ldOpt = JpayPortalDateParser.rowTrnDate(r);

                        if (ldOpt.isEmpty()) {

                            /* 거래일 미파싱 행은 날짜 필터에서 제외하지 않음(그리드·수동 확인 가능) */

                        } else {

                            LocalDate ld = ldOpt.get();

                                if (from != null && ld.isBefore(from)) {

                                    return false;

                                }

                                if (to != null && ld.isAfter(to)) {

                                    return false;

                                }

                        }

                    }

                    if (!kw.isEmpty()) {

                        if ("ORDER_NO".equals(ft)) {

                            if (!ord.toLowerCase(Locale.ROOT).contains(kw)) {

                                return false;

                            }

                        } else if ("APPROVAL_NO".equals(ft)) {

                            if (!txnId.toLowerCase(Locale.ROOT).contains(kw)) {

                                return false;

                            }

                        } else if ("MID".equals(ft)) {

                            String mid = String.valueOf(r.getOrDefault("merchant", "")).toLowerCase(Locale.ROOT);

                            if (!mid.contains(kw)) {

                                return false;

                            }

                        } else {

                            String blob = (txnId + " "

                                    + ord + " "

                                + r.getOrDefault("merchant", "") + " "

                                + r.getOrDefault("compId", "") + " "

                                + r.getOrDefault("compNm", "") + " "

                                + r.getOrDefault("masterDistCompId", "") + " "

                                + r.getOrDefault("masterDistNm", "") + " "

                                + r.getOrDefault("portalLabel", "") + " "

                                + r.getOrDefault("status", "")).toLowerCase(Locale.ROOT);

                        if (!blob.contains(kw)) {

                            return false;

                            }

                        }

                    }

                    return true;

                })

                .collect(Collectors.toList());

    }



    /**
     * 통합조회(JPAY Export 캐시) — 거래일 기준 일별 집계(조회통합 화면).
     */
    public Map<String, Object> buildDailyIntegratedSummary(LocalDate tFrom, LocalDate tTo, LocalDate effectiveTo,
                                                           String searchKeyword, String searchOrderNo,
                                                           String searchPayDivCd, String searchOrderDir) {
        ensureExportCacheLoaded();
        List<Map<String, Object>> filtered = filterRows(cachedRows, searchKeyword, searchOrderNo, searchPayDivCd,
                tFrom, effectiveTo, null);
        Map<LocalDate, JpayDayAgg> byDay = new LinkedHashMap<>();
        for (LocalDate d = tFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
            byDay.put(d, new JpayDayAgg());
        }
        for (Map<String, Object> row : filtered) {
            String dStr = String.valueOf(row.getOrDefault("trnDate", "")).trim();
            if (dStr.length() < 10) {
                continue;
            }
            LocalDate ld;
            try {
                ld = LocalDate.parse(dStr.substring(0, 10));
            } catch (Exception ignored) {
                continue;
            }
            JpayDayAgg agg = byDay.get(ld);
            if (agg == null) {
                continue;
            }
            agg.count++;
            String st = resolveJpayRowStatusCode(row);
            String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
            agg.bucketCount.merge(bucket, 1L, Long::sum);
            String cur = resolveJpayAggregateCurrency(row);
            BigDecimal amt = PayListStatusBarBuckets.parseMoney(row.get("amount"));
            agg.totalTxn.merge(cur, amt, BigDecimal::add);
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                agg.successCount++;
                agg.approve.merge(cur, amt, BigDecimal::add);
                agg.approveCountByCur.merge(cur, 1L, Long::sum);
            } else if (isJpayCancelFinancialBucket(bucket)) {
                agg.cancel.merge(cur, amt, BigDecimal::add);
                agg.cancelCountByCur.merge(cur, 1L, Long::sum);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(tFrom); d = d.minusDays(1)) {
            JpayDayAgg agg = byDay.getOrDefault(d, new JpayDayAgg());
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("day", d.toString());
            one.put("totalElements", agg.count);
            one.put("statusBucketCounts", new LinkedHashMap<>(agg.bucketCount));
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("payListFinancialSummary", agg.toFinancialSummary());
            one.put("meta", meta);
            rows.add(one);
        }
        PayListService.applyDailySummaryDayListOrder(rows, searchOrderDir);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", rows);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("jpayIntegrated", true);
        meta.putAll(jpaySyncMetaMap());
        meta.put("dailyJpayNote",
                "일자별 상세는 동일 조건으로 jpayTrSearch(통합조회)에 해당 일자만 지정해 조회합니다.");
        if (cachedRows.isEmpty()) {
            meta.put("note",
                    "저장된 JPAY Export 캐시가 없습니다. 전산설정관리 JPAY 통합조회 스케줄을 켜거나 [JPAY 동기화]를 실행하세요.");
        }
        payload.put("meta", meta);
        return payload;
    }

    private static boolean isJpayCancelFinancialBucket(String bucket) {
        return PayListStatusBarBuckets.CANCEL.equals(bucket)
                || PayListStatusBarBuckets.REFUND.equals(bucket)
                || PayListStatusBarBuckets.VOID.equals(bucket)
                || PayListStatusBarBuckets.EMAIL_VOID.equals(bucket)
                || PayListStatusBarBuckets.FORCE_REFUND.equals(bucket);
    }

    private static final class JpayDayAgg {
        long count;
        long successCount;
        final Map<String, Long> bucketCount = new HashMap<>();
        final Map<String, BigDecimal> totalTxn = new HashMap<>();
        final Map<String, BigDecimal> approve = new HashMap<>();
        final Map<String, BigDecimal> cancel = new HashMap<>();
        final Map<String, Long> approveCountByCur = new HashMap<>();
        final Map<String, Long> cancelCountByCur = new HashMap<>();

        Map<String, Object> toFinancialSummary() {
            Set<String> union = new HashSet<>();
            union.addAll(totalTxn.keySet());
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            List<String> currencyOrder = new ArrayList<>(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
            if (currencyOrder.isEmpty()) {
                currencyOrder.add("JPY");
            }
            Map<String, String> totalTxnPlain = new LinkedHashMap<>();
            Map<String, String> approvePlain = new LinkedHashMap<>();
            Map<String, String> cancelPlain = new LinkedHashMap<>();
            Map<String, String> paymentPlain = new LinkedHashMap<>();
            Map<String, Long> approveCountPlain = new LinkedHashMap<>();
            Map<String, Long> cancelCountPlain = new LinkedHashMap<>();
            for (String c : currencyOrder) {
                BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
                BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
                totalTxnPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(totalTxn.getOrDefault(c, BigDecimal.ZERO)));
                approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
                cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
                paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a.subtract(k)));
                approveCountPlain.put(c, approveCountByCur.getOrDefault(c, 0L));
                cancelCountPlain.put(c, cancelCountByCur.getOrDefault(c, 0L));
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("successCount", successCount);
            out.put("multiCurrency", currencyOrder.size() > 1);
            out.put("primaryCurrency", currencyOrder.get(0));
            out.put("currencyOrder", currencyOrder);
            out.put("totalTxnByCurrency", totalTxnPlain);
            out.put("approveByCurrency", approvePlain);
            out.put("cancelByCurrency", cancelPlain);
            out.put("approveCountByCurrency", approveCountPlain);
            out.put("cancelCountByCurrency", cancelCountPlain);
            out.put("paymentByCurrency", paymentPlain);
            return out;
        }
    }

    private record ReconcileSummary(int matched, int updated, int unmatched) {

    }

    private static String resolvePortalOrderNo(Map<String, String> row) {
        String v = JpayOrderExcelParseService.col(row,
                "Merchant Order Number", "pay_orderid", "orderid", "商户订单号");
        if (!v.isBlank()) {
            return v;
        }
        return scanRowValueByHeaderHint(row, "merchant", "order");
    }

    private static String resolvePortalTransactionId(Map<String, String> row) {
        String v = JpayOrderExcelParseService.col(row,
                "Transaction ID", "transaction_id", "交易流水号", "交易号");
        if (!v.isBlank()) {
            return v;
        }
        return scanRowValueByHeaderHint(row, "transaction", "id");
    }

    private static String resolvePortalTransactionDate(Map<String, String> row) {
        String v = JpayOrderExcelParseService.col(row,
                "Transaction Date", "transaction_date", "交易日期", "Payment Date", "payment_date");
        if (!v.isBlank()) {
            return v;
        }
        v = JpayOrderExcelParseService.col(row, "Refund time", "refund_time");
        if (!v.isBlank()) {
            return v;
        }
        return scanRowValueByHeaderHint(row, "transaction", "date");
    }

    private static String scanRowValueByHeaderHint(Map<String, String> row, String... tokens) {
        if (row == null || tokens == null || tokens.length == 0) {
            return "";
        }
        outer:
        for (Map.Entry<String, String> e : row.entrySet()) {
            String key = e.getKey();
            if (key == null || key.startsWith("_")) {
                continue;
            }
            String nk = key.toLowerCase(Locale.ROOT).replaceAll("@\\d+$", "");
            for (String t : tokens) {
                if (t == null || t.isBlank() || !nk.contains(t.toLowerCase(Locale.ROOT))) {
                    continue outer;
                }
            }
            String val = e.getValue();
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return "";
    }

    private void fillTxnFallbacks(Map<String, Object> m, Optional<PgTrnsctn> txn) {
        if (m == null) {
            return;
        }
        if (txn.isEmpty()) {
            return;
        }
        PgTrnsctn t = txn.get();
        String tid = String.valueOf(m.getOrDefault("transactionId", "")).trim();
        if (tid.isEmpty() && t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
            m.put("transactionId", t.getChillTransactionId().trim());
        }
        boolean trnDateBlank = fieldBlank(m, "trnDate");
        boolean trnTimeBlank = fieldBlank(m, "trnTime");
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId opTz = hqLedgerSysSettingsService.resolveOperationalDisplayZoneId();
        Map<String, Object> payRow = PayListItemDto.from(t, null, ledgerTz, opTz);
        if (trnDateBlank && payRow.get("trnDate") != null) {
            m.put("trnDate", payRow.get("trnDate"));
            trnDateBlank = false;
        }
        if (trnTimeBlank && payRow.get("trnTime") != null) {
            m.put("trnTime", payRow.get("trnTime"));
        }
        if (fieldBlank(m, "payCompletedAt") && payRow.get("payCompletedAt") != null) {
            m.put("payCompletedAt", payRow.get("payCompletedAt"));
        }
        if (fieldBlank(m, "customerEmail")) {
            String em = payerEmailFromTxn(t);
            if (!em.isBlank()) {
                m.put("customerEmail", em);
            }
        }
        if (fieldBlank(m, "customerName") && t.getCustomerNm() != null && !t.getCustomerNm().isBlank()) {
            m.put("customerName", t.getCustomerNm().trim());
        }
    }

    private static boolean fieldBlank(Map<String, Object> m, String key) {
        return m == null || key == null || !m.containsKey(key)
                || String.valueOf(m.getOrDefault(key, "")).trim().isEmpty();
    }

    private static String payerEmailFromTxn(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String id = t.getCustomerId();
        if (id == null || id.isBlank() || "guest".equalsIgnoreCase(id.trim())) {
            return "";
        }
        return id.trim();
    }

    /** 그리드 고객 열 — 이메일 | 성명 (결제내역 chillCustomer와 동일 구분자) */
    private static void applyCustomerDisplay(Map<String, Object> m) {
        if (m == null) {
            return;
        }
        String em = String.valueOf(m.getOrDefault("customerEmail", "")).trim();
        String nm = String.valueOf(m.getOrDefault("customerName", "")).trim();
        if (em.isEmpty() && nm.isEmpty()) {
            m.put("customer", "");
            return;
        }
        if (!em.isEmpty() && !nm.isEmpty()) {
            m.put("customer", em + " | " + nm);
        } else {
            m.put("customer", !em.isEmpty() ? em : nm);
        }
    }

    private static Map<String, Object> aggregateFinancialSummary(List<Map<String, Object>> rows) {
        JpayDayAgg agg = new JpayDayAgg();
        if (rows == null || rows.isEmpty()) {
            return agg.toFinancialSummary();
        }
        for (Map<String, Object> row : rows) {
            agg.count++;
            String st = resolveJpayRowStatusCode(row);
            String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
            if (PayListStatusBarBuckets.OTHER.equals(bucket) && !st.isEmpty()) {
                bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
            }
            agg.bucketCount.merge(bucket, 1L, Long::sum);
            String cur = resolveJpayAggregateCurrency(row);
            BigDecimal amt = PayListStatusBarBuckets.parseMoney(row.get("amount"));
            agg.totalTxn.merge(cur, amt, BigDecimal::add);
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                agg.successCount++;
                agg.approve.merge(cur, amt, BigDecimal::add);
                agg.approveCountByCur.merge(cur, 1L, Long::sum);
            } else if (isJpayCancelFinancialBucket(bucket)) {
                agg.cancel.merge(cur, amt, BigDecimal::add);
                agg.cancelCountByCur.merge(cur, 1L, Long::sum);
            }
        }
        return agg.toFinancialSummary();
    }

    private static Map<String, Object> aggregateStatusBar(List<Map<String, Object>> rows) {
        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        Set<String> currencies = new LinkedHashSet<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String bucket = resolveJpayAggregateBucket(row);
                String cur = resolveJpayAggregateCurrency(row);
                currencies.add(cur);
                BigDecimal amt = PayListStatusBarBuckets.parseMoney(row.get("amount"));
                roll.add(bucket, cur, amt, 1L);
            }
        }
        roll.mergeBucketInto(PayListStatusBarBuckets.FORCE_REFUND, PayListStatusBarBuckets.REFUND);
        List<String> currencyOrder = new ArrayList<>(currencies);
        PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
        boolean multi = currencyOrder.size() > 1;
        String primary = currencyOrder.isEmpty() ? "JPY" : currencyOrder.get(0);
        return roll.toPayload(multi, primary, false, true, currencyOrder);
    }

    private static String resolveJpayAggregateBucket(Map<String, Object> row) {
        String st = resolveJpayRowStatusCode(row);
        String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
        if (PayListStatusBarBuckets.OTHER.equals(bucket) && !st.isEmpty()) {
            bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
        }
        return bucket;
    }

    private static String resolveJpayAggregateCurrency(Map<String, Object> row) {
        String cur = MerchantDisplayCurrencyResolver.resolveJpayRowCurrencyFromMap(row);
        if (cur == null || cur.isBlank()) {
            return PayListStatusBarBuckets.normalizeCurrency(
                    String.valueOf(row.getOrDefault("currency", "")));
        }
        return PayListStatusBarBuckets.normalizeCurrency(cur);
    }

    private void resolveCurrencyOnRow(Map<String, Object> m, Optional<PgTrnsctn> txn,
                                      Map<Long, Optional<MerchantProfile>> profileCache) {
        if (m == null) {
            return;
        }
        applyResolvedCurrency(m, txn, profileCache);
        applyCustomerDisplay(m);
    }

    private void applyResolvedCurrency(Map<String, Object> m, Optional<PgTrnsctn> txn,
                                       Map<Long, Optional<MerchantProfile>> profileCache) {
        OrgCurrencyContext ctx = resolveOrgCurrencyContext(m);
        MerchantProfile merchantMp = loadMerchantProfile(ctx.merchantOrgId(), profileCache);
        MerchantProfile masterMp = loadMerchantProfile(ctx.masterDistOrgId(), profileCache);
        String resolved = MerchantDisplayCurrencyResolver.resolveJpayRowCurrency(
                String.valueOf(m.getOrDefault("currency", "")),
                String.valueOf(m.getOrDefault("originalCurrency", "")),
                txn.orElse(null),
                merchantMp,
                masterMp);
        if (!resolved.isBlank()) {
            m.put("currency", resolved);
        } else {
            String fromMap = MerchantDisplayCurrencyResolver.resolveJpayRowCurrencyFromMap(m);
            if (!fromMap.isBlank()) {
                m.put("currency", fromMap);
            }
        }
        String baseCur = masterMp != null
                ? MerchantDisplayCurrencyResolver.primaryFromMerchantProfile(masterMp)
                : MerchantDisplayCurrencyResolver.primaryFromMerchantProfile(merchantMp);
        if (!baseCur.isBlank()) {
            m.put("merchantBaseCur", baseCur);
        }
    }

    private record OrgCurrencyContext(Long merchantOrgId, Long masterDistOrgId) {
    }

    private OrgCurrencyContext resolveOrgCurrencyContext(Map<String, Object> m) {
        Long merchantOrgId = null;
        Long masterOrgId = null;
        String compId = String.valueOf(m.getOrDefault("compId", "")).trim();
        if (!compId.isEmpty()) {
            merchantOrgId = orgUnitRepository.findByCodeIgnoreCase(compId).map(OrgUnit::getId).orElse(null);
        }
        if (merchantOrgId == null) {
            String mid = String.valueOf(m.getOrDefault("merchant", "")).trim();
            if (!mid.isEmpty()) {
                for (MerchantPgBinding binding : merchantPgBindingRepository
                        .findByMidIgnoreCaseOrderByOperationalYnDescIdAsc(mid)) {
                    Optional<OrgUnit> ou = orgUnitRepository.findById(binding.getOrgUnitId());
                    if (ou.isPresent() && ou.get().getOrgLevel() == OrgLevel.MERCHANT) {
                        merchantOrgId = ou.get().getId();
                        break;
                    }
                }
            }
        }
        if (merchantOrgId != null) {
            masterOrgId = masterDistSettlementCronZoneService.findNearestMasterDistOrgId(merchantOrgId).orElse(null);
        }
        if (masterOrgId == null) {
            String masterCode = String.valueOf(m.getOrDefault("masterDistCompId", "")).trim();
            if (!masterCode.isEmpty()) {
                masterOrgId = orgUnitRepository.findByCodeIgnoreCase(masterCode).map(OrgUnit::getId).orElse(null);
            }
        }
        return new OrgCurrencyContext(merchantOrgId, masterOrgId);
    }

    private MerchantProfile loadMerchantProfile(Long orgUnitId, Map<Long, Optional<MerchantProfile>> profileCache) {
        if (orgUnitId == null) {
            return null;
        }
        Optional<MerchantProfile> cached = profileCache.get(orgUnitId);
        if (cached != null) {
            return cached.orElse(null);
        }
        Optional<MerchantProfile> loaded = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        profileCache.put(orgUnitId, loaded);
        return loaded.orElse(null);
    }

    /** JPAY 통합조회 — 포털 Export Trading Status 우선, ICOPAY DB는 폴백 */
    static String resolveJpayRowStatusCode(Map<String, Object> m) {
        if (m == null) {
            return "";
        }
        refreshPortalIcopayStatus(m);
        String ic = String.valueOf(m.getOrDefault("icopayStatus", "")).trim();
        if (!ic.isEmpty()) {
            return ic;
        }
        String trading = portalTradingText(m);
        if (!trading.isEmpty()) {
            String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading,
                    String.valueOf(m.getOrDefault("chargeback", "")),
                    String.valueOf(m.getOrDefault("rdr", "")));
            if (mapped != null) {
                m.put("icopayStatus", mapped);
                return mapped;
            }
        }
        return String.valueOf(m.getOrDefault("dbStatus", "")).trim();
    }

    private static void refreshPortalIcopayStatus(Map<String, Object> m) {
        String trading = portalTradingText(m);
        if (trading.isEmpty()) {
            return;
        }
        String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading,
                String.valueOf(m.getOrDefault("chargeback", "")),
                String.valueOf(m.getOrDefault("rdr", "")));
        if (mapped != null) {
            m.put("icopayStatus", mapped);
        }
    }

    private static String portalTradingText(Map<String, Object> m) {
        String t = String.valueOf(m.getOrDefault("status", "")).trim();
        if (t.isEmpty()) {
            t = String.valueOf(m.getOrDefault("tradingStatus", "")).trim();
        }
        return t;
    }

    private static void applyStatusNm(Map<String, Object> m) {
        if (m == null) {
            return;
        }
        String code = resolveJpayRowStatusCode(m);
        if (!code.isEmpty()) {
            m.put("statusNm", PayListStatusBarBuckets.pgStatusDisplayLabel(code));
            return;
        }
        String trading = portalTradingText(m);
        if (!trading.isEmpty()) {
            m.put("statusNm", portalTradingStatusLabel(trading));
        }
    }

    private static String portalTradingStatusLabel(String trading) {
        if (trading == null || trading.isBlank()) {
            return "";
        }
        return switch (PayListStatusBarBuckets.bucketForChillStatus(trading)) {
            case PayListStatusBarBuckets.SUCCESS -> "성공";
            case PayListStatusBarBuckets.FAIL -> "실패";
            case PayListStatusBarBuckets.CANCEL -> "취소";
            case PayListStatusBarBuckets.REFUND, PayListStatusBarBuckets.FORCE_REFUND -> "환불";
            case PayListStatusBarBuckets.VOID, PayListStatusBarBuckets.EMAIL_VOID -> "무효";
            default -> trading;
        };
    }

    private List<Map<String, Object>> repairEnrichedRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Optional<MerchantProfile>> profileCache = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(repairEnrichedRow(row, profileCache));
        }
        return out;
    }

    private Map<String, Object> repairEnrichedRow(Map<String, Object> src,
                                                Map<Long, Optional<MerchantProfile>> profileCache) {
        Map<String, Object> m = src instanceof LinkedHashMap ? src : new LinkedHashMap<>(src);
        String txnDate = String.valueOf(m.getOrDefault("trnDate", "")).trim();
        if (txnDate.isEmpty()) {
            String raw = String.valueOf(m.getOrDefault("transactionDate", "")).trim();
            if (!raw.isEmpty()) {
                JpayPortalDateParser.applyDateTimeFields(raw, m);
            }
        }
        String orderNo = String.valueOf(m.getOrDefault("orderNo", "")).trim();
        String txnId = String.valueOf(m.getOrDefault("transactionId", "")).trim();
        alignRowWithSuccessTxnIfMissingApproval(m);
        txnId = String.valueOf(m.getOrDefault("transactionId", "")).trim();
        Optional<PgTrnsctn> txn = findTxn(orderNo, txnId);
        if (txn.isPresent()) {
            PgTrnsctn t = txn.get();
            if (!m.containsKey("dbStatus") || String.valueOf(m.getOrDefault("dbStatus", "")).isBlank()) {
                m.put("dbStatus", t.getStatus());
            }
            if (!"Y".equals(String.valueOf(m.getOrDefault("icopay", "")))) {
                m.put("icopay", "Y");
            }
            if (!m.containsKey("compId") || String.valueOf(m.getOrDefault("compId", "")).isBlank()) {
                m.put("compId", t.getMerchantId());
            }
            if (!m.containsKey("compNm") || String.valueOf(m.getOrDefault("compNm", "")).isBlank()) {
                orgUnitRepository.findByCodeIgnoreCase(t.getMerchantId() != null ? t.getMerchantId().trim() : "")
                        .map(OrgUnit::getName)
                        .ifPresent(nm -> m.put("compNm", nm));
            }
        }
        fillTxnFallbacks(m, txn);
        resolveCurrencyOnRow(m, txn, profileCache);
        refreshPortalIcopayStatus(m);
        applyStatusNm(m);
        return m;
    }

}


