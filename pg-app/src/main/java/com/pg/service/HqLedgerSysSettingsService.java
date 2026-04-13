package com.pg.service;

import com.pg.catalog.DataRetentionCatalog;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본사설정 전산설정관리 — NOTI 시스템/환경설정(시간·동기화, 자동 메일, 결제 후속조치 스위치) 대응.
 */
@Service
public class HqLedgerSysSettingsService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final HqLedgerSysSettingsRepository repository;
    private final HqNotifyEnvService hqNotifyEnvService;

    public HqLedgerSysSettingsService(HqLedgerSysSettingsRepository repository, HqNotifyEnvService hqNotifyEnvService) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    @Transactional
    public HqLedgerSysSettings getOrCreate() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            HqLedgerSysSettings x = new HqLedgerSysSettings();
            x.setId(1L);
            x.setDisplayTimezone("Asia/Bangkok");
            return repository.save(x);
        });
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
        m.putAll(hqNotifyEnvService.payFollowActionsSlice());
        if (s.getUpdatedAt() != null) {
            m.put("updatedAt", s.getUpdatedAt().toString());
        } else {
            m.put("updatedAt", "");
        }
        m.put("dataRetentionRows", buildDataRetentionRows(s));
        ZoneId z;
        try {
            String tz = s.getDisplayTimezone();
            z = (tz != null && !tz.isBlank()) ? ZoneId.of(tz.trim()) : ZoneId.systemDefault();
        } catch (Exception e) {
            z = ZoneId.systemDefault();
        }
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
        HqLedgerSysSettings saved = repository.save(s);
        hqNotifyEnvService.mergePayFollowActionsFromBody(body);
        return saved;
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
}
