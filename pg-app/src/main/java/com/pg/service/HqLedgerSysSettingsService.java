package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.HqLedgerSysSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 본사설정 전산설정관리 — NOTI 시스템/환경설정(시간·동기화, 자동 메일) 대응.
 */
@Service
public class HqLedgerSysSettingsService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final HqLedgerSysSettingsRepository repository;

    public HqLedgerSysSettingsService(HqLedgerSysSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HqLedgerSysSettings getOrCreate() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            HqLedgerSysSettings x = new HqLedgerSysSettings();
            x.setId(1L);
            return repository.save(x);
        });
    }

    public Map<String, Object> toMap(HqLedgerSysSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("displayTimezone", nz(s.getDisplayTimezone()));
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
        if (s.getUpdatedAt() != null) {
            m.put("updatedAt", s.getUpdatedAt().toString());
        } else {
            m.put("updatedAt", "");
        }
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
        return repository.save(s);
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
}
