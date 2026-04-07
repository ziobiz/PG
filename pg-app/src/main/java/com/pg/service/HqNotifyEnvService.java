package com.pg.service;

import com.pg.entity.HqNotifyEnvConfig;
import com.pg.repository.HqNotifyEnvConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class HqNotifyEnvService {

    private static final int DEFAULT_AUTO_VOID_START_MIN = 0;
    private static final int DEFAULT_AUTO_VOID_END_MIN = 21 * 60;
    private static final int EMAIL_VOID_END_MIN_FIXED = 23 * 60 + 59;

    private final HqNotifyEnvConfigRepository repository;

    public HqNotifyEnvService(HqNotifyEnvConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HqNotifyEnvConfig getOrCreate() {
        HqNotifyEnvConfig c = repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            HqNotifyEnvConfig x = new HqNotifyEnvConfig();
            x.setIngressToken(UUID.randomUUID().toString().replace("-", ""));
            return repository.save(x);
        });
        if (c.getOtpRequiredYn() == null || c.getOtpRequiredYn().isBlank()) {
            c.setOtpRequiredYn("Y");
            c = repository.save(c);
        }
        return c;
    }

    /** NOTI/칠페이 등에 등록할 전사 노티 수신 URL (토큰 경로 포함) */
    public String buildNotifyIngressUrl(HqNotifyEnvConfig cfg, HttpServletRequest req) {
        String base = cfg.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            String scheme = req.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) {
                scheme = req.getScheme();
            }
            String host = req.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = req.getServerName();
                int port = req.getServerPort();
                if (("http".equalsIgnoreCase(scheme) && port != 80) || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                    host = host + ":" + port;
                }
            }
            base = scheme + "://" + host;
        }
        base = base.trim().replaceAll("/+$", "");
        return base + "/api/open/pg-notify/" + cfg.getIngressToken();
    }

    public Map<String, Object> toMap(HqNotifyEnvConfig c, HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingressToken", c.getIngressToken());
        m.put("publicBaseUrl", c.getPublicBaseUrl() != null ? c.getPublicBaseUrl() : "");
        m.put("notifyIngressUrl", buildNotifyIngressUrl(c, req));
        m.put("autoVoidYn", yn(c.getAutoVoidYn()));
        m.put("emailVoidYn", yn(c.getEmailVoidYn()));
        m.put("autoRefundYn", yn(c.getAutoRefundYn()));
        m.put("forceRefundYn", yn(c.getForceRefundYn()));
        m.put("autoVoidAfterHours", c.getAutoVoidAfterHours() != null ? c.getAutoVoidAfterHours() : "");
        m.put("emailVoidAfterHours", c.getEmailVoidAfterHours() != null ? c.getEmailVoidAfterHours() : "");
        m.put("autoVoidStartTime", formatMinutesToHm(c.getAutoVoidStartMin()));
        m.put("autoVoidEndTime", formatMinutesToHm(c.getAutoVoidEndMin()));
        m.put("emailVoidStartTime", formatMinutesToHm(c.getEmailVoidStartMin()));
        m.put("emailVoidEndTime", "23:59");
        m.put("payFollowRefZone", nz(c.getPayFollowRefZone()));
        m.put("autoRefundAfterDays", c.getAutoRefundAfterDays() != null ? c.getAutoRefundAfterDays() : 7);
        m.put("forceRefundAfterDays", c.getForceRefundAfterDays() != null ? c.getForceRefundAfterDays() : 0);
        m.put("autoVoidReflectSettlementYn", yn(c.getAutoVoidReflectSettlementYn()));
        m.put("emailVoidReflectSettlementYn", yn(c.getEmailVoidReflectSettlementYn()));
        m.put("autoRefundReflectSettlementYn", yn(c.getAutoRefundReflectSettlementYn()));
        m.put("forceRefundReflectSettlementYn", yn(c.getForceRefundReflectSettlementYn()));
        m.put("notifyOkResponse", c.getNotifyOkResponse() != null ? c.getNotifyOkResponse() : "{\"result\":\"OK\"}");
        m.put("otpRequiredYn", yn(c.getOtpRequiredYn()));
        m.put("otpPolicyMode", (c.getOtpPolicyMode() == null || c.getOtpPolicyMode().isBlank()) ? "NOTI" : c.getOtpPolicyMode());
        m.put("passwordPolicyMode", (c.getPasswordPolicyMode() == null || c.getPasswordPolicyMode().isBlank()) ? "NOTI" : c.getPasswordPolicyMode());
        m.put("forgotPasswordEnabledYn", yn(c.getForgotPasswordEnabledYn()));
        m.put("managerUserControlEnabledYn", yn(c.getManagerUserControlEnabledYn()));
        m.put("managerPasswordResetEnabledYn", yn(c.getManagerPasswordResetEnabledYn()));
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        return m;
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v) ? "Y" : "N";
    }

    /**
     * 전산설정관리 화면용 — 결제 후속조치 플래그만 조회 (저장 위치는 tb_hq_notify_env_config).
     */
    public Map<String, Object> payFollowActionsSlice() {
        HqNotifyEnvConfig c = getOrCreate();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("autoVoidYn", yn(c.getAutoVoidYn()));
        m.put("emailVoidYn", yn(c.getEmailVoidYn()));
        m.put("autoRefundYn", yn(c.getAutoRefundYn()));
        m.put("forceRefundYn", yn(c.getForceRefundYn()));
        m.put("autoVoidAfterHours", "");
        m.put("emailVoidAfterHours", "");
        m.put("autoVoidStartTime", formatMinutesToHm(c.getAutoVoidStartMin()));
        m.put("autoVoidEndTime", formatMinutesToHm(c.getAutoVoidEndMin()));
        m.put("emailVoidStartTime", formatMinutesToHm(c.getEmailVoidStartMin()));
        m.put("emailVoidEndTime", "23:59");
        m.put("payFollowRefZone", nz(c.getPayFollowRefZone()));
        m.put("autoRefundAfterDays", c.getAutoRefundAfterDays() != null ? c.getAutoRefundAfterDays() : 7);
        m.put("forceRefundAfterDays", c.getForceRefundAfterDays() != null ? c.getForceRefundAfterDays() : 0);
        m.put("autoVoidReflectSettlementYn", yn(c.getAutoVoidReflectSettlementYn()));
        m.put("emailVoidReflectSettlementYn", yn(c.getEmailVoidReflectSettlementYn()));
        m.put("autoRefundReflectSettlementYn", yn(c.getAutoRefundReflectSettlementYn()));
        m.put("forceRefundReflectSettlementYn", yn(c.getForceRefundReflectSettlementYn()));
        return m;
    }

    private void applyPayFollowActionFields(HqNotifyEnvConfig c, Map<String, Object> body) {
        if (body == null) {
            return;
        }
        if (body.containsKey("autoVoidYn")) {
            c.setAutoVoidYn(yn(String.valueOf(body.get("autoVoidYn"))));
        }
        if (body.containsKey("emailVoidYn")) {
            c.setEmailVoidYn(yn(String.valueOf(body.get("emailVoidYn"))));
        }
        if (body.containsKey("autoRefundYn")) {
            c.setAutoRefundYn(yn(String.valueOf(body.get("autoRefundYn"))));
        }
        if (body.containsKey("forceRefundYn")) {
            c.setForceRefundYn(yn(String.valueOf(body.get("forceRefundYn"))));
        }
        if (body.containsKey("autoVoidAfterHours")) {
            c.setAutoVoidAfterHours(parseHoursOrNull(body.get("autoVoidAfterHours")));
        }
        if (body.containsKey("emailVoidAfterHours")) {
            c.setEmailVoidAfterHours(parseHoursOrNull(body.get("emailVoidAfterHours")));
        }
        if (body.containsKey("autoVoidStartTime")) {
            c.setAutoVoidStartMin(parseHmToMinutes(body.get("autoVoidStartTime")));
        }
        if (body.containsKey("autoVoidEndTime")) {
            c.setAutoVoidEndMin(parseHmToMinutes(body.get("autoVoidEndTime")));
        }
        if (body.containsKey("emailVoidStartTime")) {
            c.setEmailVoidStartMin(parseHmToMinutes(body.get("emailVoidStartTime")));
        }
        if (body.containsKey("autoVoidStartTime") || body.containsKey("autoVoidEndTime")
                || body.containsKey("emailVoidStartTime")) {
            c.setAutoVoidAfterHours(null);
            c.setEmailVoidAfterHours(null);
        }
        if (body.containsKey("payFollowRefZone")) {
            String z = String.valueOf(body.get("payFollowRefZone")).trim();
            c.setPayFollowRefZone(z.isEmpty() ? null : z);
        }
        if (body.containsKey("autoRefundAfterDays")) {
            Object ar = body.get("autoRefundAfterDays");
            String ars = ar == null ? "" : ar.toString().trim();
            if (ars.isEmpty()) {
                c.setAutoRefundAfterDays(7);
            } else {
                c.setAutoRefundAfterDays(parseDaysOrNull(ar));
            }
        }
        if (body.containsKey("forceRefundAfterDays")) {
            Object fr = body.get("forceRefundAfterDays");
            String frs = fr == null ? "" : fr.toString().trim();
            if (frs.isEmpty()) {
                c.setForceRefundAfterDays(0);
            } else {
                c.setForceRefundAfterDays(parseDaysOrNull(fr));
            }
        }
        if (body.containsKey("autoVoidReflectSettlementYn")) {
            c.setAutoVoidReflectSettlementYn(yn(String.valueOf(body.get("autoVoidReflectSettlementYn"))));
        }
        if (body.containsKey("emailVoidReflectSettlementYn")) {
            c.setEmailVoidReflectSettlementYn(yn(String.valueOf(body.get("emailVoidReflectSettlementYn"))));
        }
        if (body.containsKey("autoRefundReflectSettlementYn")) {
            c.setAutoRefundReflectSettlementYn(yn(String.valueOf(body.get("autoRefundReflectSettlementYn"))));
        }
        if (body.containsKey("forceRefundReflectSettlementYn")) {
            c.setForceRefundReflectSettlementYn(yn(String.valueOf(body.get("forceRefundReflectSettlementYn"))));
        }
    }

    static void validatePayFollowTimeWindows(HqNotifyEnvConfig c) {
        boolean autoY = "Y".equalsIgnoreCase(safeTrim(c.getAutoVoidYn()));
        boolean emailY = "Y".equalsIgnoreCase(safeTrim(c.getEmailVoidYn()));
        Integer as = c.getAutoVoidStartMin();
        Integer ae = c.getAutoVoidEndMin();
        Integer es = c.getEmailVoidStartMin();
        if (autoY) {
            int s = as != null ? as : DEFAULT_AUTO_VOID_START_MIN;
            int e = ae != null ? ae : DEFAULT_AUTO_VOID_END_MIN;
            if (s < 0 || s > 1439 || e < 0 || e > 1439) {
                throw new IllegalArgumentException("자동무효 시각이 올바르지 않습니다(0:00~23:59).");
            }
            if (s > e) {
                throw new IllegalArgumentException("자동무효: 시작 시각은 마감 시각과 같거나 이전이어야 합니다.");
            }
        }
        if (emailY) {
            if (es != null && (es < 0 || es > EMAIL_VOID_END_MIN_FIXED)) {
                throw new IllegalArgumentException("이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).");
            }
        }
    }

    /** 수동무효 마감은 항상 당일 23:59(분 단위 고정). */
    static void normalizeEmailVoidEndFixed(HqNotifyEnvConfig c) {
        if (c == null) {
            return;
        }
        if ("Y".equalsIgnoreCase(safeTrim(c.getEmailVoidYn()))) {
            c.setEmailVoidEndMin(EMAIL_VOID_END_MIN_FIXED);
        }
    }

    private static String safeTrim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String formatMinutesToHm(Integer min) {
        if (min == null || min < 0 || min > 1439) {
            return "";
        }
        return String.format(Locale.ROOT, "%02d:%02d", min / 60, min % 60);
    }

    private static Integer parseHmToMinutes(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        String[] p = s.split(":");
        if (p.length < 2) {
            return null;
        }
        try {
            int h = Integer.parseInt(p[0].trim());
            int mi = Integer.parseInt(p[1].trim());
            if (h < 0 || h > 23 || mi < 0 || mi > 59) {
                return null;
            }
            return h * 60 + mi;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseDaysOrNull(Object d) {
        if (d == null || d.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(d.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseHoursOrNull(Object h) {
        if (h == null || h.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(h.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 전산설정관리 저장 시 — 요청에 후속조치 키가 있을 때만 갱신.
     */
    @Transactional
    public void mergePayFollowActionsFromBody(Map<String, Object> body) {
        if (body == null) {
            return;
        }
        if (!body.containsKey("autoVoidYn") && !body.containsKey("emailVoidYn") && !body.containsKey("autoRefundYn")
                && !body.containsKey("forceRefundYn") && !body.containsKey("autoVoidAfterHours")
                && !body.containsKey("emailVoidAfterHours") && !body.containsKey("payFollowRefZone")
                && !body.containsKey("autoRefundAfterDays") && !body.containsKey("forceRefundAfterDays")
                && !body.containsKey("autoVoidReflectSettlementYn") && !body.containsKey("emailVoidReflectSettlementYn")
                && !body.containsKey("autoRefundReflectSettlementYn") && !body.containsKey("forceRefundReflectSettlementYn")
                && !body.containsKey("autoVoidStartTime") && !body.containsKey("autoVoidEndTime")
                && !body.containsKey("emailVoidStartTime")) {
            return;
        }
        HqNotifyEnvConfig c = getOrCreate();
        applyPayFollowActionFields(c, body);
        normalizeEmailVoidEndFixed(c);
        validatePayFollowTimeWindows(c);
        repository.save(c);
    }

    @Transactional
    public HqNotifyEnvConfig saveFromBody(Map<String, Object> body) {
        HqNotifyEnvConfig c = getOrCreate();
        if (body.get("publicBaseUrl") != null) {
            String u = body.get("publicBaseUrl").toString().trim();
            c.setPublicBaseUrl(u.isEmpty() ? null : u);
        }
        applyPayFollowActionFields(c, body);
        normalizeEmailVoidEndFixed(c);
        if (body.get("notifyOkResponse") != null) {
            String r = body.get("notifyOkResponse").toString().trim();
            if (!r.isEmpty()) {
                c.setNotifyOkResponse(r);
            }
        }
        if (body.containsKey("otpRequiredYn")) {
            c.setOtpRequiredYn(yn(String.valueOf(body.get("otpRequiredYn"))));
        }
        if (body.containsKey("otpPolicyMode")) {
            String v = String.valueOf(body.get("otpPolicyMode"));
            c.setOtpPolicyMode((v == null || v.isBlank()) ? "NOTI" : v.trim().toUpperCase());
        }
        if (body.containsKey("passwordPolicyMode")) {
            String v = String.valueOf(body.get("passwordPolicyMode"));
            c.setPasswordPolicyMode((v == null || v.isBlank()) ? "NOTI" : v.trim().toUpperCase());
        }
        if (body.containsKey("forgotPasswordEnabledYn")) {
            c.setForgotPasswordEnabledYn(yn(String.valueOf(body.get("forgotPasswordEnabledYn"))));
        }
        if (body.containsKey("managerUserControlEnabledYn")) {
            c.setManagerUserControlEnabledYn(yn(String.valueOf(body.get("managerUserControlEnabledYn"))));
        }
        if (body.containsKey("managerPasswordResetEnabledYn")) {
            c.setManagerPasswordResetEnabledYn(yn(String.valueOf(body.get("managerPasswordResetEnabledYn"))));
        }
        if (payFollowKeysPresentInBody(body)) {
            validatePayFollowTimeWindows(c);
        }
        return repository.save(c);
    }

    private static boolean payFollowKeysPresentInBody(Map<String, Object> body) {
        if (body == null) {
            return false;
        }
        return body.containsKey("autoVoidYn") || body.containsKey("emailVoidYn") || body.containsKey("autoRefundYn")
                || body.containsKey("forceRefundYn") || body.containsKey("autoVoidAfterHours")
                || body.containsKey("emailVoidAfterHours") || body.containsKey("payFollowRefZone")
                || body.containsKey("autoRefundAfterDays") || body.containsKey("forceRefundAfterDays")
                || body.containsKey("autoVoidReflectSettlementYn") || body.containsKey("emailVoidReflectSettlementYn")
                || body.containsKey("autoRefundReflectSettlementYn") || body.containsKey("forceRefundReflectSettlementYn")
                || body.containsKey("autoVoidStartTime") || body.containsKey("autoVoidEndTime")
                || body.containsKey("emailVoidStartTime");
    }

    @Transactional
    public Map<String, Object> regenerateToken(HttpServletRequest req) {
        HqNotifyEnvConfig c = getOrCreate();
        c.setIngressToken(UUID.randomUUID().toString().replace("-", ""));
        repository.save(c);
        return toMap(c, req);
    }
}
