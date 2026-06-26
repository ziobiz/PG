package com.pg.service;

import com.pg.catalog.DataRetentionCatalog;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.OrgLevel;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.JpayTrSyncSchedule;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.ReceivableRecoveryModeUtil;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 본사설정 전산설정관리 — NOTI 시스템/환경설정(시간·동기화, 자동 메일, 결제 후속조치 스위치) 대응.
 */
@Service
public class HqLedgerSysSettingsService {

    public static final String SETTLEMENT_AUTO_BATCH_ACTIVE = "ACTIVE";
    public static final String SETTLEMENT_AUTO_BATCH_INACTIVE = "INACTIVE";
    public static final String SETTLEMENT_AUTO_BATCH_AUTO = "AUTO";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final HqLedgerSysSettingsRepository repository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final SettlementSettingRepository settlementSettingRepository;

    public HqLedgerSysSettingsService(HqLedgerSysSettingsRepository repository,
                                      HqNotifyEnvService hqNotifyEnvService,
                                      SettlementSettingRepository settlementSettingRepository) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    @Transactional
    public HqLedgerSysSettings getOrCreate() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            HqLedgerSysSettings x = new HqLedgerSysSettings();
            x.setId(1L);
            x.setDisplayTimezone("Asia/Bangkok");
            x.setSettlementAutoBatchMode(SETTLEMENT_AUTO_BATCH_INACTIVE);
            return repository.save(x);
        });
    }

    /**
     * 스케줄 tick 본문을 DB 만으로 허용할지(① JVM 은 호출 측에서 AND).
     *
     * @param peekDueThisTick {@link com.pg.service.settlement.SettlementAutoRunService#peekAnyDueAutoWorkThisTick()} 결과
     */
    public boolean isSettlementAutoBatchDbTickAllowed(boolean peekDueThisTick) {
        String mode = normalizeSettlementAutoBatchMode(
                repository.findFirstByOrderByIdAsc().map(HqLedgerSysSettings::getSettlementAutoBatchMode).orElse(null));
        if (SETTLEMENT_AUTO_BATCH_INACTIVE.equals(mode)) {
            return false;
        }
        if (SETTLEMENT_AUTO_BATCH_ACTIVE.equals(mode)) {
            return true;
        }
        if (SETTLEMENT_AUTO_BATCH_AUTO.equals(mode)) {
            return peekDueThisTick;
        }
        return false;
    }

    public static String normalizeSettlementAutoBatchMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return SETTLEMENT_AUTO_BATCH_INACTIVE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (SETTLEMENT_AUTO_BATCH_ACTIVE.equals(u) || SETTLEMENT_AUTO_BATCH_INACTIVE.equals(u) || SETTLEMENT_AUTO_BATCH_AUTO.equals(u)) {
            return u;
        }
        return SETTLEMENT_AUTO_BATCH_INACTIVE;
    }

    @Transactional
    public HqLedgerSysSettings updateSettlementAutoBatchMode(String mode) {
        HqLedgerSysSettings s = getOrCreate();
        s.setSettlementAutoBatchMode(normalizeSettlementAutoBatchMode(mode));
        return repository.save(s);
    }

    @Transactional
    public HqLedgerSysSettings updateVoidRefundSettlementModes(Map<String, Object> body) {
        HqLedgerSysSettings s = getOrCreate();
        if (body != null) {
            if (body.containsKey("voidSettlementMode")) {
                s.setVoidSettlementMode(VoidRefundSettlementModeUtil.normalize(String.valueOf(body.get("voidSettlementMode"))));
            }
            if (body.containsKey("manualVoidSettlementMode")) {
                s.setManualVoidSettlementMode(VoidRefundSettlementModeUtil.normalize(String.valueOf(body.get("manualVoidSettlementMode"))));
            }
            if (body.containsKey("refundSettlementMode")) {
                s.setRefundSettlementMode(VoidRefundSettlementModeUtil.normalize(String.valueOf(body.get("refundSettlementMode"))));
            }
            if (body.containsKey("forceRefundSettlementMode")) {
                s.setForceRefundSettlementMode(VoidRefundSettlementModeUtil.normalize(String.valueOf(body.get("forceRefundSettlementMode"))));
            }
            if (body.containsKey("receivableRecoveryDefaultMode")) {
                s.setReceivableRecoveryDefaultMode(
                        ReceivableRecoveryModeUtil.normalize(String.valueOf(body.get("receivableRecoveryDefaultMode"))));
            }
        }
        return repository.save(s);
    }

    /**
     * 본사 기본 미수금 환수 모드를 가맹에 반영합니다.
     * {@code receivable_recovery_override_yn = 'Y'} 인 가맹(개별 우선)은 제외합니다.
     */
    @Transactional
    public int applyReceivableRecoveryDefaultToAllMerchants(String normalizedMode) {
        return settlementSettingRepository.updateReceivableRecoveryModeForMerchantsInheriting(
                ReceivableRecoveryModeUtil.normalize(normalizedMode), OrgLevel.MERCHANT);
    }

    public Map<String, Object> toMap(HqLedgerSysSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        {
            String tz = s.getDisplayTimezone();
            if (tz == null || tz.isBlank()) {
                m.put("displayTimezone", "Asia/Bangkok");
            } else {
                m.put("displayTimezone", tz.trim());
            }
        }
        m.put("ntpSyncEnabledYn", yn(s.getNtpSyncEnabledYn()));
        m.put("ntpServerList", nz(s.getNtpServerList()));
        m.put("timeSyncIntervalMin", s.getTimeSyncIntervalMin());
        m.put("smtpHost", nz(s.getSmtpHost()));
        m.put("smtpPort", s.getSmtpPort());
        m.put("smtpTlsYn", yn(s.getSmtpTlsYn()));
        m.put("smtpAuthYn", yn(s.getSmtpAuthYn()));
        m.put("smtpUsername", nz(s.getSmtpUsername()));
        boolean hasPw = s.getSmtpPassword() != null && !s.getSmtpPassword().isBlank();
        m.put("smtpPasswordSet", hasPw);
        m.put("mailFromAddress", nz(s.getMailFromAddress()));
        m.put("mailFromName", nz(s.getMailFromName()));
        m.put("alertRecipientEmails", nz(s.getAlertRecipientEmails()));
        m.put("emailOnSyncFailureYn", yn(s.getEmailOnSyncFailureYn()));
        m.put("emailDailyDigestYn", yn(s.getEmailDailyDigestYn()));
        m.put("emailNotifyVoidBatchYn", yn(s.getEmailNotifyVoidBatchYn()));
        m.put("emailNotifyRefundBatchYn", yn(s.getEmailNotifyRefundBatchYn()));
        m.put("memo", nz(s.getMemo()));
        m.put("emailVoidTo", nz(s.getEmailVoidTo()));
        m.put("emailVoidSubject", nz(s.getEmailVoidSubject()));
        m.put("emailVoidBodyTemplate", nz(s.getEmailVoidBodyTemplate()));
        m.put("emailVoidCompanyName", nz(s.getEmailVoidCompanyName()));
        m.put("emailVoidContactName", nz(s.getEmailVoidContactName()));
        m.put("chillpayTrInitSyncMonths", ledgerIntOr(s.getChillpayTrInitSyncMonths(), 3));
        m.put("chillpayTrRecentSyncDays", ledgerIntOr(s.getChillpayTrRecentSyncDays(), 2));
        m.put("jpayPortalUsername", nz(s.getJpayPortalUsername()));
        m.put("jpayPortalPasswordSet", s.getJpayPortalPassword() != null && !s.getJpayPortalPassword().isBlank());
        m.put("jpayTrInitSyncMonths", ledgerIntOr(s.getJpayTrInitSyncMonths(), 3));
        m.put("jpayTrRecentSyncDays", ledgerIntOr(s.getJpayTrRecentSyncDays(), 7));
        m.put("jpayTrSyncScheduleMin", JpayTrSyncSchedule.clampMinutes(s.getJpayTrSyncScheduleMin()));
        m.put("jpayTrSyncScheduleOptions", JpayTrSyncSchedule.optionRows());
        m.put("appLogMemoryRetentionDays", ledgerIntOr(s.getAppLogMemoryRetentionDays(), 30));
        m.put("appLogFileRetentionDays", ledgerIntOr(s.getAppLogFileRetentionDays(), 90));
        m.put("feeListDecimalPlaces", ledgerIntOr(s.getFeeListDecimalPlaces(), 2));
        {
            String rm = s.getFeeListRoundMode();
            m.put("feeListRoundMode", rm != null && !rm.isBlank() ? rm.trim().toUpperCase() : "CEILING");
        }
        m.put("feeCurrencyFormats", FeeCurrencyRoundResolver.buildDisplayRows(s));
        {
            String num = PayDisplayCurrency.normalizeIsoNum(s.getPayDisplayCurrencyIsoNum());
            m.put("payDisplayCurrencyIsoNum", num);
            m.put("payDisplayCurrencyCode", PayDisplayCurrency.alphaFromIsoNum(num));
            m.put("payDisplayCurrencyCatalog", PayDisplayCurrency.catalogRows());
        }
        m.put("helloTimelineEnabledYn", yn(s.getHelloTimelineEnabledYn()));
        {
            String mode = normalizeSettlementAutoBatchMode(s.getSettlementAutoBatchMode());
            m.put("settlementAutoBatchMode", mode);
            /* 구 API·화면: 배치를 DB 에서 완전히 끈 것만 N */
            boolean legacyOn = SETTLEMENT_AUTO_BATCH_ACTIVE.equals(mode) || SETTLEMENT_AUTO_BATCH_AUTO.equals(mode);
            m.put("settlementAutoBatchEnabledYn", legacyOn ? "Y" : "N");
        }
        m.put("voidSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getVoidSettlementMode()));
        m.put("manualVoidSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getManualVoidSettlementMode()));
        m.put("refundSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getRefundSettlementMode()));
        m.put("forceRefundSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getForceRefundSettlementMode()));
        m.put("receivableRecoveryDefaultMode", ReceivableRecoveryModeUtil.normalize(s.getReceivableRecoveryDefaultMode()));
        m.put("cardFailCooldownEnabledYn", yn(s.getCardFailCooldownEnabledYn()));
        m.put("cardFailCooldownTier1Min", ledgerIntOr(s.getCardFailCooldownTier1Min(), 5));
        m.put("cardFailCooldownTier2Min", ledgerIntOr(s.getCardFailCooldownTier2Min(), 10));
        m.put("cardFailCooldownTier3Min", ledgerIntOr(s.getCardFailCooldownTier3Min(), 60));
        {
            int hm = s.getHelloTimelineDurationMin() != null && s.getHelloTimelineDurationMin() > 0
                    ? s.getHelloTimelineDurationMin() : 10;
            if (hm > 1440) {
                hm = 1440;
            }
            m.put("helloTimelineDurationMin", hm);
        }
        m.putAll(hqNotifyEnvService.payFollowActionsSlice());
        if (s.getUpdatedAt() != null) {
            m.put("updatedAt", s.getUpdatedAt().toString());
        } else {
            m.put("updatedAt", "");
        }
        m.put("dataRetentionRows", buildDataRetentionRows(s));
        ZoneId z = resolveDisplayZoneIdFromSettings(s);
        m.put("serverTimeIso", ZonedDateTime.now(z).format(ISO));
        m.put("serverZoneId", z.getId());
        return m;
    }

    @Transactional
    public HqLedgerSysSettings saveFromBody(Map<String, Object> body) {
        HqLedgerSysSettings s = getOrCreate();
        if (body == null) {
            return repository.save(s);
        }
        s.setDisplayTimezone(trimToNull(body.get("displayTimezone")));
        s.setNtpSyncEnabledYn(parseYn(body.get("ntpSyncEnabledYn"), s.getNtpSyncEnabledYn()));
        s.setNtpServerList(trimToNull(body.get("ntpServerList")));
        s.setTimeSyncIntervalMin(parsePositiveInt(body.get("timeSyncIntervalMin")));
        s.setSmtpHost(trimToNull(body.get("smtpHost")));
        s.setSmtpPort(parsePort(body.get("smtpPort")));
        s.setSmtpTlsYn(parseYn(body.get("smtpTlsYn"), s.getSmtpTlsYn()));
        s.setSmtpAuthYn(parseYn(body.get("smtpAuthYn"), s.getSmtpAuthYn()));
        s.setSmtpUsername(trimToNull(body.get("smtpUsername")));
        String newPw = trimToNull(body.get("smtpPassword"));
        if (newPw != null) {
            s.setSmtpPassword(newPw);
        }
        s.setMailFromAddress(trimToNull(body.get("mailFromAddress")));
        s.setMailFromName(trimToNull(body.get("mailFromName")));
        s.setAlertRecipientEmails(trimToNull(body.get("alertRecipientEmails")));
        s.setEmailOnSyncFailureYn(parseYn(body.get("emailOnSyncFailureYn"), s.getEmailOnSyncFailureYn()));
        s.setEmailDailyDigestYn(parseYn(body.get("emailDailyDigestYn"), s.getEmailDailyDigestYn()));
        s.setEmailNotifyVoidBatchYn(parseYn(body.get("emailNotifyVoidBatchYn"), s.getEmailNotifyVoidBatchYn()));
        s.setEmailNotifyRefundBatchYn(parseYn(body.get("emailNotifyRefundBatchYn"), s.getEmailNotifyRefundBatchYn()));
        s.setMemo(trimToNull(body.get("memo")));
        s.setEmailVoidTo(trimToNull(body.get("emailVoidTo")));
        s.setEmailVoidSubject(trimToNull(body.get("emailVoidSubject")));
        s.setEmailVoidBodyTemplate(trimToNull(body.get("emailVoidBodyTemplate")));
        s.setEmailVoidCompanyName(trimToNull(body.get("emailVoidCompanyName")));
        s.setEmailVoidContactName(trimToNull(body.get("emailVoidContactName")));
        if (body.containsKey("chillpayTrInitSyncMonths")) {
            s.setChillpayTrInitSyncMonths(clampInt(body.get("chillpayTrInitSyncMonths"), 3, 1, 120));
        }
        if (body.containsKey("chillpayTrRecentSyncDays")) {
            s.setChillpayTrRecentSyncDays(clampInt(body.get("chillpayTrRecentSyncDays"), 2, 1, 365));
        }
        if (body.containsKey("jpayPortalUsername")) {
            s.setJpayPortalUsername(trimToNull(body.get("jpayPortalUsername")));
        }
        String jpayPw = trimToNull(body.get("jpayPortalPassword"));
        if (jpayPw != null) {
            s.setJpayPortalPassword(jpayPw);
        }
        if (body.containsKey("jpayTrInitSyncMonths")) {
            s.setJpayTrInitSyncMonths(clampInt(body.get("jpayTrInitSyncMonths"), 3, 1, 120));
        }
        if (body.containsKey("jpayTrRecentSyncDays")) {
            s.setJpayTrRecentSyncDays(clampInt(body.get("jpayTrRecentSyncDays"), 7, 7, 365));
        }
        if (body.containsKey("jpayTrSyncScheduleMin")) {
            s.setJpayTrSyncScheduleMin(JpayTrSyncSchedule.clampMinutes(parseScheduleMinutes(body.get("jpayTrSyncScheduleMin"))));
        }
        if (body.containsKey("appLogMemoryRetentionDays")) {
            s.setAppLogMemoryRetentionDays(clampInt(body.get("appLogMemoryRetentionDays"), 30, 1, 3650));
        }
        if (body.containsKey("appLogFileRetentionDays")) {
            s.setAppLogFileRetentionDays(clampInt(body.get("appLogFileRetentionDays"), 90, 1, 3650));
        }
        if (body.containsKey("feeListDecimalPlaces")) {
            s.setFeeListDecimalPlaces(clampInt(body.get("feeListDecimalPlaces"), 2, 0, 8));
        }
        if (body.containsKey("feeListRoundMode")) {
            Integer dpCur = s.getFeeListDecimalPlaces();
            if (dpCur == null || dpCur != 0) {
                String rm = trimToNull(body.get("feeListRoundMode"));
                if (rm != null) {
                    String u = rm.toUpperCase();
                    if ("CEILING".equals(u) || "HALF_UP".equals(u) || "DOWN".equals(u)) {
                        s.setFeeListRoundMode(u);
                    }
                }
            }
        }
        if (s.getFeeListDecimalPlaces() != null && s.getFeeListDecimalPlaces() == 0) {
            s.setFeeListRoundMode("DOWN");
        }
        /* payDisplayCurrencyIsoNum: 전역 표시 기준 통화 — 전산설정 UI·본 API 저장 본문으로는 변경하지 않음(DB·배포 스키마로만 관리). */
        if (body.containsKey("feeCurrencyFormatJson")) {
            Object fc = body.get("feeCurrencyFormatJson");
            if (fc == null || String.valueOf(fc).isBlank()) {
                s.setFeeCurrencyFormatJson(FeeCurrencyRoundResolver.normalizePolicyJson(null, s));
            } else {
                s.setFeeCurrencyFormatJson(FeeCurrencyRoundResolver.normalizePolicyJson(String.valueOf(fc), s));
            }
        }
        s.setHelloTimelineEnabledYn(parseYn(body.get("helloTimelineEnabledYn"), s.getHelloTimelineEnabledYn()));
        if (body.containsKey("helloTimelineDurationMin")) {
            s.setHelloTimelineDurationMin(clampInt(body.get("helloTimelineDurationMin"), 10, 1, 1440));
        }
        if (body.containsKey("dataRetentionPolicyJson")) {
            Object dr = body.get("dataRetentionPolicyJson");
            if (dr == null) {
                s.setDataRetentionPolicyJson(null);
            } else {
                String norm = DataRetentionCatalog.normalizePolicyJson(String.valueOf(dr));
                if (norm != null) {
                    s.setDataRetentionPolicyJson(norm);
                }
            }
        }
        if (body.containsKey("cardFailCooldownEnabledYn")) {
            s.setCardFailCooldownEnabledYn(parseYn(body.get("cardFailCooldownEnabledYn"), s.getCardFailCooldownEnabledYn()));
        }
        if (body.containsKey("cardFailCooldownTier1Min")) {
            s.setCardFailCooldownTier1Min(clampInt(body.get("cardFailCooldownTier1Min"), 5, 1, 24 * 60));
        }
        if (body.containsKey("cardFailCooldownTier2Min")) {
            s.setCardFailCooldownTier2Min(clampInt(body.get("cardFailCooldownTier2Min"), 10, 1, 24 * 60));
        }
        if (body.containsKey("cardFailCooldownTier3Min")) {
            s.setCardFailCooldownTier3Min(clampInt(body.get("cardFailCooldownTier3Min"), 60, 1, 24 * 60));
        }
        HqLedgerSysSettings saved = repository.save(s);
        hqNotifyEnvService.mergePayFollowActionsFromBody(body);
        return saved;
    }

    /**
     * 헬로 타임라인만 저장합니다. 요청 본문의 다른 키는 무시하며, 전산설정의 나머지 필드는 변경하지 않습니다.
     */
    @Transactional
    public HqLedgerSysSettings saveHelloTimelineFromBody(Map<String, Object> body) {
        HqLedgerSysSettings s = getOrCreate();
        if (body == null) {
            return repository.save(s);
        }
        if (body.containsKey("helloTimelineEnabledYn")) {
            s.setHelloTimelineEnabledYn(parseYn(body.get("helloTimelineEnabledYn"), s.getHelloTimelineEnabledYn()));
        }
        if (body.containsKey("helloTimelineDurationMin")) {
            s.setHelloTimelineDurationMin(clampInt(body.get("helloTimelineDurationMin"), 10, 1, 1440));
        }
        return repository.save(s);
    }

    private static List<Map<String, Object>> buildDataRetentionRows(HqLedgerSysSettings s) {
        Map<String, DataRetentionCatalog.RetentionPolicy> pol =
                DataRetentionCatalog.parseRetentionPolicies(s.getDataRetentionPolicyJson());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DataRetentionCatalog.Entry e : DataRetentionCatalog.ENTRIES) {
            DataRetentionCatalog.RetentionPolicy p = pol.getOrDefault(e.id(), DataRetentionCatalog.defaultPolicyForEntry(e));
            int purge = p.purgeDays() > 0 ? p.purgeDays() : p.retainDays();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.id());
            row.put("label", e.label());
            row.put("description", e.description());
            row.put("days", p.retainDays());
            row.put("retainDays", p.retainDays());
            row.put("purgeDays", purge);
            row.put("autoDeleteEnabled", p.autoDeleteEnabled());
            row.put("schedulerApplied", e.schedulerPurge());
            rows.add(row);
        }
        return rows;
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }

    private static String yn(String v) {
        return (v != null && "Y".equalsIgnoreCase(v.trim())) ? "Y" : "N";
    }

    private static String trimToNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String parseYn(Object o, String def) {
        if (o == null) {
            return def != null ? yn(def) : "N";
        }
        return "Y".equalsIgnoreCase(String.valueOf(o).trim()) ? "Y" : "N";
    }

    private static Integer parsePositiveInt(Object o) {
        if (o == null || String.valueOf(o).trim().isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(String.valueOf(o).trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parsePort(Object o) {
        if (o == null || String.valueOf(o).trim().isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(String.valueOf(o).trim());
            return (v > 0 && v <= 65535) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int ledgerIntOr(Integer v, int def) {
        return v != null && v > 0 ? v : def;
    }

    private static Integer parseScheduleMinutes(Object o) {
        if (o == null || String.valueOf(o).trim().isEmpty()) {
            return JpayTrSyncSchedule.OFF;
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return JpayTrSyncSchedule.OFF;
        }
    }

    private static int clampInt(Object o, int defaultVal, int min, int max) {
        if (o == null || String.valueOf(o).trim().isEmpty()) {
            return defaultVal;
        }
        try {
            int v = Integer.parseInt(String.valueOf(o).trim());
            if (v < min) {
                return min;
            }
            return Math.min(v, max);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 전산설정 표준시간대({@code display_timezone}) — naive 적재·그리드 표시의 기본 벽시계.
     * 미설정·파싱 실패 시 {@code Asia/Bangkok}.
     */
    public static ZoneId resolveDisplayZoneIdFromSettings(HqLedgerSysSettings s) {
        if (s == null) {
            return ZoneId.of("Asia/Bangkok");
        }
        try {
            String tz = s.getDisplayTimezone();
            if (tz == null || tz.isBlank()) {
                return ZoneId.of("Asia/Bangkok");
            }
            return ZoneId.of(tz.trim());
        } catch (Exception e) {
            return ZoneId.of("Asia/Bangkok");
        }
    }

    /** 단일 행 전산설정을 열어 표준 시간대를 반환합니다. */
    public ZoneId resolveLedgerDisplayZoneId() {
        return resolveDisplayZoneIdFromSettings(getOrCreate());
    }
}
