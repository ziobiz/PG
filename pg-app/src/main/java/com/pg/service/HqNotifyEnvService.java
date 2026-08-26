package com.pg.service;

import com.pg.entity.HqNotifyEnvConfig;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.noti.NotiInternalTargetCatalogService;
import com.pg.repository.HqNotifyEnvConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class HqNotifyEnvService {

    private static final int DEFAULT_AUTO_VOID_START_MIN = 0;
    private static final int DEFAULT_AUTO_VOID_END_MIN = 21 * 60;
    private static final int EMAIL_VOID_END_MIN_FIXED = 23 * 60 + 59;

    private final HqNotifyEnvConfigRepository repository;
    private final OrgPagePermissionService orgPagePermissionService;
    private final OrgTabletMenuService orgTabletMenuService;
    private final NotiInternalTargetCatalogService notiInternalTargetCatalogService;

    public HqNotifyEnvService(HqNotifyEnvConfigRepository repository,
                              @Lazy OrgPagePermissionService orgPagePermissionService,
                              @Lazy OrgTabletMenuService orgTabletMenuService,
                              NotiInternalTargetCatalogService notiInternalTargetCatalogService) {
        this.repository = repository;
        this.orgPagePermissionService = orgPagePermissionService;
        this.orgTabletMenuService = orgTabletMenuService;
        this.notiInternalTargetCatalogService = notiInternalTargetCatalogService;
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

    /**
     * NOTI·ChillPay·JPAY 등에 등록할 <strong>권장</strong> 전사 노티 수신 URL (미들웨어 베이스, 토큰까지 포함).
     * 레거시 open 경로는 {@link #buildNotifyIngressUrlOpen}.
     */
    public String buildNotifyIngressUrl(HqNotifyEnvConfig cfg, HttpServletRequest req) {
        return PgNotifyIngressPaths.buildIngressBase(resolveNotifyPublicBase(cfg, req), cfg.getIngressToken());
    }

    /** 레거시 {@code /api/open/pg-notify/…} — 기존 등록 URL과 병행 안내용 */
    public String buildNotifyIngressUrlOpen(HqNotifyEnvConfig cfg, HttpServletRequest req) {
        return PgNotifyIngressPaths.buildIngressBaseOpen(resolveNotifyPublicBase(cfg, req), cfg.getIngressToken());
    }

    private static String resolveNotifyPublicBase(HqNotifyEnvConfig cfg, HttpServletRequest req) {
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
        return base.trim().replaceAll("/+$", "");
    }

    public Map<String, Object> toMap(HqNotifyEnvConfig c, HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingressToken", c.getIngressToken());
        m.put("publicBaseUrl", c.getPublicBaseUrl() != null ? c.getPublicBaseUrl() : "");
        m.put("notifyIngressUrl", buildNotifyIngressUrl(c, req));
        m.put("notifyIngressUrlOpen", buildNotifyIngressUrlOpen(c, req));
        m.put("autoVoidYn", yn(c.getAutoVoidYn()));
        m.put("emailVoidYn", yn(c.getEmailVoidYn()));
        m.put("autoRefundYn", yn(c.getAutoRefundYn()));
        m.put("forceRefundYn", yn(c.getForceRefundYn()));
        m.put("manualVoidYn", yn(c.getManualVoidYn()));
        m.put("manualRefundYn", yn(c.getManualRefundYn()));
        m.put("epSameDayRefundYn", yn(c.getEpSameDayRefundYn()));
        m.put("autoVoidAfterHours", c.getAutoVoidAfterHours() != null ? c.getAutoVoidAfterHours() : "");
        m.put("emailVoidAfterHours", c.getEmailVoidAfterHours() != null ? c.getEmailVoidAfterHours() : "");
        m.put("autoVoidStartTime", formatMinutesToHm(c.getAutoVoidStartMin()));
        m.put("autoVoidEndTime", formatMinutesToHm(c.getAutoVoidEndMin()));
        m.put("emailVoidStartTime", formatMinutesToHm(c.getEmailVoidStartMin()));
        m.put("emailVoidEndTime", formatMinutesToHm(c.getEmailVoidEndMin() != null ? c.getEmailVoidEndMin() : EMAIL_VOID_END_MIN_FIXED));
        m.put("payFollowRefZone", nz(c.getPayFollowRefZone()));
        m.put("autoRefundAfterDays", c.getAutoRefundAfterDays() != null ? c.getAutoRefundAfterDays() : 7);
        m.put("autoRefundWindowStartTime", formatMinutesToHm(c.getAutoRefundWindowStartMin()));
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
        m.put("notiProvisionEnabledYn", yn(c.getNotiProvisionEnabledYn()));
        m.put("notiProvisionBaseUrl", c.getNotiProvisionBaseUrl() != null ? c.getNotiProvisionBaseUrl() : "");
        m.put("notiProvisionDefaultInternalTargetId",
                c.getNotiProvisionDefaultInternalTargetId() != null ? c.getNotiProvisionDefaultInternalTargetId() : "");
        m.put("notiProvisionInternalTargetJpy",
                c.getNotiProvisionInternalTargetJpy() != null ? c.getNotiProvisionInternalTargetJpy() : "");
        m.put("notiProvisionInternalTargetUsd",
                c.getNotiProvisionInternalTargetUsd() != null ? c.getNotiProvisionInternalTargetUsd() : "");
        m.put("notiProvisionInternalTargetThb",
                c.getNotiProvisionInternalTargetThb() != null ? c.getNotiProvisionInternalTargetThb() : "");
        m.put("notiProvisionDefaultDealmaiPartner",
                c.getNotiProvisionDefaultDealmaiPartner() != null ? c.getNotiProvisionDefaultDealmaiPartner() : "");
        String npKey = c.getNotiProvisionApiKey();
        m.put("notiProvisionApiKeyConfigured", (npKey != null && !npKey.isBlank()) ? "Y" : "N");
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        try {
            m.put("assistantOrgLevels", orgPagePermissionService.getAssistantOrgLevelsForApi());
            m.put("assistantRoleDefaultMatrixByLevel", orgPagePermissionService.getHqAssistantDefaultMatrixByLevelResolvedForApi());
            m.put("assistantMatrixCatalog", orgPagePermissionService.getAssistantMatrixCatalogForApi());
            m.put("assistantTabletMatrixCatalog", orgPagePermissionService.getAssistantTabletMatrixCatalogForApi());
            m.put("tabletMenuExposureByLevel", orgTabletMenuService.buildTabletExposureByLevelForApi());
        } catch (Exception ignored) {
            m.put("assistantOrgLevels", List.of());
            m.put("assistantRoleDefaultMatrixByLevel", Map.of());
            m.put("assistantMatrixCatalog", List.of());
            m.put("assistantTabletMatrixCatalog", List.of());
            m.put("tabletMenuExposureByLevel", Map.of());
        }
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
        m.put("manualVoidYn", yn(c.getManualVoidYn()));
        m.put("manualRefundYn", yn(c.getManualRefundYn()));
        m.put("epSameDayRefundYn", yn(c.getEpSameDayRefundYn()));
        m.put("autoVoidAfterHours", "");
        m.put("emailVoidAfterHours", "");
        m.put("autoVoidStartTime", formatMinutesToHm(c.getAutoVoidStartMin()));
        m.put("autoVoidEndTime", formatMinutesToHm(c.getAutoVoidEndMin()));
        m.put("emailVoidStartTime", formatMinutesToHm(c.getEmailVoidStartMin()));
        m.put("emailVoidEndTime", formatMinutesToHm(c.getEmailVoidEndMin() != null ? c.getEmailVoidEndMin() : EMAIL_VOID_END_MIN_FIXED));
        m.put("payFollowRefZone", nz(c.getPayFollowRefZone()));
        m.put("autoRefundAfterDays", c.getAutoRefundAfterDays() != null ? c.getAutoRefundAfterDays() : 7);
        m.put("autoRefundWindowStartTime", formatMinutesToHm(c.getAutoRefundWindowStartMin()));
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
        if (body.containsKey("manualVoidYn")) {
            c.setManualVoidYn(yn(String.valueOf(body.get("manualVoidYn"))));
        }
        if (body.containsKey("manualRefundYn")) {
            c.setManualRefundYn(yn(String.valueOf(body.get("manualRefundYn"))));
        }
        if (body.containsKey("epSameDayRefundYn")) {
            c.setEpSameDayRefundYn(yn(String.valueOf(body.get("epSameDayRefundYn"))));
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
        if (body.containsKey("emailVoidEndTime")) {
            c.setEmailVoidEndMin(parseHmToMinutes(body.get("emailVoidEndTime")));
        }
        if (body.containsKey("autoRefundWindowStartTime")) {
            c.setAutoRefundWindowStartMin(parseHmToMinutes(body.get("autoRefundWindowStartTime")));
        }
        if (body.containsKey("autoVoidStartTime") || body.containsKey("autoVoidEndTime")
                || body.containsKey("emailVoidStartTime") || body.containsKey("emailVoidEndTime")) {
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
            int ee = c.getEmailVoidEndMin() != null ? c.getEmailVoidEndMin() : EMAIL_VOID_END_MIN_FIXED;
            if (ee < 0 || ee > EMAIL_VOID_END_MIN_FIXED) {
                throw new IllegalArgumentException("이메일무효 마감 시각이 올바르지 않습니다(0:00~23:59).");
            }
            int minStart = 0;
            if (autoY) {
                int autoEndEff = ae != null ? ae : DEFAULT_AUTO_VOID_END_MIN;
                if (autoEndEff < EMAIL_VOID_END_MIN_FIXED) {
                    minStart = autoEndEff + 1;
                }
            }
            if (ee < minStart) {
                throw new IllegalArgumentException("이메일무효 마감은 자동무효 마감 다음 분 이후(또는 동일)여야 합니다.");
            }
            if (es != null) {
                if (es < 0 || es > EMAIL_VOID_END_MIN_FIXED) {
                    throw new IllegalArgumentException("이메일무효 시작 시각이 올바르지 않습니다(0:00~23:59).");
                }
                if (es > ee) {
                    throw new IllegalArgumentException("이메일무효 시작은 마감 시각과 같거나 이전이어야 합니다.");
                }
                if (autoY) {
                    int autoEndEff = ae != null ? ae : DEFAULT_AUTO_VOID_END_MIN;
                    if (autoEndEff < EMAIL_VOID_END_MIN_FIXED && es < autoEndEff + 1) {
                        throw new IllegalArgumentException(
                                "이메일무효 시작은 자동무효 마감 다음 분 이후여야 합니다. 자동무효·이메일무효를 함께 쓰는 경우 시작 입력은 비우면 자동무효 직후부터 적용됩니다.");
                    }
                }
            }
        }
        boolean refundY = "Y".equalsIgnoreCase(safeTrim(c.getAutoRefundYn()));
        if (refundY) {
            Integer wr = c.getAutoRefundWindowStartMin();
            if (wr != null && (wr < 0 || wr > EMAIL_VOID_END_MIN_FIXED)) {
                throw new IllegalArgumentException("환불 익일 구간 시작 시각이 올바르지 않습니다(0:00~23:59).");
            }
        }
    }

    /**
     * 자동무효·이메일무효 동시 사용 시 DB에 남은 이메일 시작이 자동무효 마감보다 이르면 NULL 로 정리(실제 판단은 마감 직후부터).
     */
    static void clampEmailVoidStartWhenAutoVoidEnabled(HqNotifyEnvConfig c) {
        if (c == null) {
            return;
        }
        boolean autoY = "Y".equalsIgnoreCase(safeTrim(c.getAutoVoidYn()));
        boolean emailY = "Y".equalsIgnoreCase(safeTrim(c.getEmailVoidYn()));
        if (!autoY || !emailY) {
            return;
        }
        Integer ae = c.getAutoVoidEndMin();
        int autoEndEff = ae != null ? ae : DEFAULT_AUTO_VOID_END_MIN;
        if (autoEndEff >= EMAIL_VOID_END_MIN_FIXED) {
            return;
        }
        int floor = autoEndEff + 1;
        Integer es = c.getEmailVoidStartMin();
        if (es != null && es < floor) {
            c.setEmailVoidStartMin(null);
        }
    }

    /** 이메일무효 사용 시 마감 미입력이면 23:59 기본값만 채움(저장된 시각은 덮어쓰지 않음). */
    static void normalizeEmailVoidEndDefault(HqNotifyEnvConfig c) {
        if (c == null) {
            return;
        }
        if ("Y".equalsIgnoreCase(safeTrim(c.getEmailVoidYn())) && c.getEmailVoidEndMin() == null) {
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
                && !body.containsKey("forceRefundYn") && !body.containsKey("manualVoidYn") && !body.containsKey("manualRefundYn")
                && !body.containsKey("epSameDayRefundYn") && !body.containsKey("autoVoidAfterHours")
                && !body.containsKey("emailVoidAfterHours") && !body.containsKey("payFollowRefZone")
                && !body.containsKey("autoRefundAfterDays") && !body.containsKey("forceRefundAfterDays")
                && !body.containsKey("autoVoidReflectSettlementYn") && !body.containsKey("emailVoidReflectSettlementYn")
                && !body.containsKey("autoRefundReflectSettlementYn") && !body.containsKey("forceRefundReflectSettlementYn")
                && !body.containsKey("autoVoidStartTime") && !body.containsKey("autoVoidEndTime")
                && !body.containsKey("emailVoidStartTime") && !body.containsKey("emailVoidEndTime")
                && !body.containsKey("autoRefundWindowStartTime")) {
            return;
        }
        HqNotifyEnvConfig c = getOrCreate();
        applyPayFollowActionFields(c, body);
        normalizeEmailVoidEndDefault(c);
        clampEmailVoidStartWhenAutoVoidEnabled(c);
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
        normalizeEmailVoidEndDefault(c);
        clampEmailVoidStartWhenAutoVoidEnabled(c);
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
        applyNotiProvisionFields(c, body);
        validateNotiProvisionInternalTargets(c);
        if (body.containsKey("assistantRoleDefaultMatrix")) {
            try {
                c.setAssistantRoleDefaultMatrixJson(
                        orgPagePermissionService.normalizeAssistantRoleDefaultMatrixToJson(body.get("assistantRoleDefaultMatrix")));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("담당자 기본 권한 JSON 저장 실패: " + e.getMessage());
            }
        }
        if (payFollowKeysPresentInBody(body)) {
            validatePayFollowTimeWindows(c);
        }
        return repository.save(c);
    }

    @Transactional(readOnly = true)
    public HqNotifyEnvConfig requireProvisionConfigReady() {
        HqNotifyEnvConfig c = getOrCreate();
        if (!"Y".equalsIgnoreCase(yn(c.getNotiProvisionEnabledYn()))) {
            throw new IllegalArgumentException("본사설정 → 노티구성설정에서 NOTI Provision API 사용을 켜 주세요.");
        }
        String base = c.getNotiProvisionBaseUrl();
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("NOTI Provision 베이스 URL을 본사설정에 입력하세요.");
        }
        String key = c.getNotiProvisionApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("NOTI Provision API 키를 본사설정에 저장하세요.");
        }
        return c;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listNotiInternalTargets() {
        return notiInternalTargetCatalogService.listFromNotiDetailed(getOrCreate(), "ko");
    }

    private void validateNotiProvisionInternalTargets(HqNotifyEnvConfig c) {
        if (!notiInternalTargetCatalogService.isProvisionConfigured(c)) {
            return;
        }
        List<Map<String, Object>> targets = notiInternalTargetCatalogService.listFromNoti(c, "ko");
        if (targets.isEmpty()) {
            return;
        }
        notiInternalTargetCatalogService.assertRegistered(nz(c.getNotiProvisionInternalTargetJpy()), targets);
        notiInternalTargetCatalogService.assertRegistered(nz(c.getNotiProvisionInternalTargetUsd()), targets);
        notiInternalTargetCatalogService.assertRegistered(nz(c.getNotiProvisionInternalTargetThb()), targets);
        notiInternalTargetCatalogService.assertRegistered(nz(c.getNotiProvisionDefaultInternalTargetId()), targets);
    }

    /**
     * 노티웹훅 Partner 삭제 시, 동일 코드가 Provision 기본값·전산 대상 매핑에 남지 않도록 정리합니다.
     */
    @Transactional
    public Map<String, Object> clearProvisionRefsForWebhookPartnerCode(String partnerCode) {
        String code = partnerCode != null ? partnerCode.trim() : "";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("partnerCode", code);
        out.put("cleared", false);
        if (code.isEmpty()) {
            return out;
        }
        HqNotifyEnvConfig c = getOrCreate();
        boolean changed = false;
        if (code.equalsIgnoreCase(nz(c.getNotiProvisionDefaultDealmaiPartner()))) {
            c.setNotiProvisionDefaultDealmaiPartner(null);
            changed = true;
            out.put("clearedDefaultDealmaiPartner", true);
        }
        if (code.equalsIgnoreCase(nz(c.getNotiProvisionDefaultInternalTargetId()))) {
            c.setNotiProvisionDefaultInternalTargetId(null);
            changed = true;
            out.put("clearedDefaultInternalTargetId", true);
        }
        if (code.equalsIgnoreCase(nz(c.getNotiProvisionInternalTargetJpy()))) {
            c.setNotiProvisionInternalTargetJpy(null);
            changed = true;
            out.put("clearedInternalTargetJpy", true);
        }
        if (code.equalsIgnoreCase(nz(c.getNotiProvisionInternalTargetUsd()))) {
            c.setNotiProvisionInternalTargetUsd(null);
            changed = true;
            out.put("clearedInternalTargetUsd", true);
        }
        if (code.equalsIgnoreCase(nz(c.getNotiProvisionInternalTargetThb()))) {
            c.setNotiProvisionInternalTargetThb(null);
            changed = true;
            out.put("clearedInternalTargetThb", true);
        }
        if (changed) {
            repository.save(c);
            out.put("cleared", true);
        }
        return out;
    }

    private void applyNotiProvisionFields(HqNotifyEnvConfig c, Map<String, Object> body) {
        if (body == null) {
            return;
        }
        if (body.containsKey("notiProvisionEnabledYn")) {
            c.setNotiProvisionEnabledYn(yn(String.valueOf(body.get("notiProvisionEnabledYn"))));
        }
        if (body.containsKey("notiProvisionBaseUrl")) {
            String u = String.valueOf(body.get("notiProvisionBaseUrl")).trim();
            c.setNotiProvisionBaseUrl(u.isEmpty() ? null : u.replaceAll("/+$", ""));
        }
        if (body.containsKey("notiProvisionDefaultInternalTargetId")) {
            String tid = String.valueOf(body.get("notiProvisionDefaultInternalTargetId")).trim();
            c.setNotiProvisionDefaultInternalTargetId(tid.isEmpty() ? null : tid);
        }
        if (body.containsKey("notiProvisionInternalTargetJpy")) {
            String tid = String.valueOf(body.get("notiProvisionInternalTargetJpy")).trim();
            c.setNotiProvisionInternalTargetJpy(tid.isEmpty() ? null : tid);
        }
        if (body.containsKey("notiProvisionInternalTargetUsd")) {
            String tid = String.valueOf(body.get("notiProvisionInternalTargetUsd")).trim();
            c.setNotiProvisionInternalTargetUsd(tid.isEmpty() ? null : tid);
        }
        if (body.containsKey("notiProvisionInternalTargetThb")) {
            String tid = String.valueOf(body.get("notiProvisionInternalTargetThb")).trim();
            c.setNotiProvisionInternalTargetThb(tid.isEmpty() ? null : tid);
        }
        if (body.containsKey("notiProvisionDefaultDealmaiPartner")) {
            String p = String.valueOf(body.get("notiProvisionDefaultDealmaiPartner")).trim();
            boolean forceClear = "Y".equalsIgnoreCase(String.valueOf(
                    body.getOrDefault("clearDefaultDealmaiPartnerYn", "")).trim());
            if (p.isEmpty()) {
                /* 빈 값으로 덮어쓰지 않음 — 실수로 기본 Partner가 사라지는 것 방지.
                   의도적 삭제는 clearDefaultDealmaiPartnerYn=Y 또는 Partner 삭제 시만. */
                if (forceClear) {
                    c.setNotiProvisionDefaultDealmaiPartner(null);
                }
            } else {
                c.setNotiProvisionDefaultDealmaiPartner(p);
            }
        }
        if (body.containsKey("notiProvisionApiKey")) {
            String k = String.valueOf(body.get("notiProvisionApiKey")).trim();
            if (!k.isEmpty() && !isProvisionApiKeyPlaceholder(k)) {
                c.setNotiProvisionApiKey(k);
            }
        }
    }

    private static boolean isProvisionApiKeyPlaceholder(String k) {
        if (k == null || k.isBlank()) {
            return true;
        }
        String t = k.trim();
        return "********".equals(t) || t.matches("^\\*+$");
    }

    private static boolean payFollowKeysPresentInBody(Map<String, Object> body) {
        if (body == null) {
            return false;
        }
        return body.containsKey("autoVoidYn") || body.containsKey("emailVoidYn") || body.containsKey("autoRefundYn")
                || body.containsKey("forceRefundYn") || body.containsKey("manualVoidYn") || body.containsKey("manualRefundYn")
                || body.containsKey("epSameDayRefundYn") || body.containsKey("autoVoidAfterHours")
                || body.containsKey("emailVoidAfterHours") || body.containsKey("payFollowRefZone")
                || body.containsKey("autoRefundAfterDays") || body.containsKey("forceRefundAfterDays")
                || body.containsKey("autoVoidReflectSettlementYn") || body.containsKey("emailVoidReflectSettlementYn")
                || body.containsKey("autoRefundReflectSettlementYn") || body.containsKey("forceRefundReflectSettlementYn")
                || body.containsKey("autoVoidStartTime") || body.containsKey("autoVoidEndTime")
                || body.containsKey("emailVoidStartTime") || body.containsKey("emailVoidEndTime")
                || body.containsKey("autoRefundWindowStartTime");
    }

    @Transactional
    public Map<String, Object> regenerateToken(HttpServletRequest req) {
        HqNotifyEnvConfig c = getOrCreate();
        c.setIngressToken(UUID.randomUUID().toString().replace("-", ""));
        repository.save(c);
        return toMap(c, req);
    }
}
