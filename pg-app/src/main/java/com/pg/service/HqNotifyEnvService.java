package com.pg.service;

import com.pg.entity.HqNotifyEnvConfig;
import com.pg.repository.HqNotifyEnvConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class HqNotifyEnvService {

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

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v) ? "Y" : "N";
    }

    @Transactional
    public HqNotifyEnvConfig saveFromBody(Map<String, Object> body) {
        HqNotifyEnvConfig c = getOrCreate();
        if (body.get("publicBaseUrl") != null) {
            String u = body.get("publicBaseUrl").toString().trim();
            c.setPublicBaseUrl(u.isEmpty() ? null : u);
        }
        if (body.containsKey("autoVoidYn")) c.setAutoVoidYn(yn(String.valueOf(body.get("autoVoidYn"))));
        if (body.containsKey("emailVoidYn")) c.setEmailVoidYn(yn(String.valueOf(body.get("emailVoidYn"))));
        if (body.containsKey("autoRefundYn")) c.setAutoRefundYn(yn(String.valueOf(body.get("autoRefundYn"))));
        if (body.containsKey("forceRefundYn")) c.setForceRefundYn(yn(String.valueOf(body.get("forceRefundYn"))));
        if (body.containsKey("autoVoidAfterHours")) {
            Object h = body.get("autoVoidAfterHours");
            if (h == null || h.toString().isBlank()) {
                c.setAutoVoidAfterHours(null);
            } else {
                try {
                    c.setAutoVoidAfterHours(Integer.parseInt(h.toString().trim()));
                } catch (NumberFormatException ignored) {
                    c.setAutoVoidAfterHours(null);
                }
            }
        }
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
        return repository.save(c);
    }

    @Transactional
    public Map<String, Object> regenerateToken(HttpServletRequest req) {
        HqNotifyEnvConfig c = getOrCreate();
        c.setIngressToken(UUID.randomUUID().toString().replace("-", ""));
        repository.save(c);
        return toMap(c, req);
    }
}
