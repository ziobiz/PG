package com.pg.merchantdeploy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 샌드박스 브로커 시크릿이면 실PG(EP 등) 서비스를 타지 않도록 가맹 checkout API에서 공통 분기.
 */
@Component
public class MerchantSandboxCheckoutFacade {

    private final MerchantSandboxCheckoutService sandboxCheckoutService;

    public MerchantSandboxCheckoutFacade(MerchantSandboxCheckoutService sandboxCheckoutService) {
        this.sandboxCheckoutService = sandboxCheckoutService;
    }

    public boolean isSandbox(HttpServletRequest request) {
        return MerchantBrokerAccessVerifier.isSandboxRequest(request);
    }

    public boolean isSandboxSessionToken(String token) {
        return token != null && token.trim().startsWith("sb_");
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body) {
        return sandboxCheckoutService.prepare(orgUnitId, body != null ? body : Map.of());
    }

    public Map<String, Object> complete(Long orgUnitId, Map<String, Object> body) {
        return sandboxCheckoutService.complete(orgUnitId, body != null ? body : Map.of());
    }

    public Map<String, Object> status(Long orgUnitId, String orderNo) {
        return sandboxCheckoutService.orderStatus(orgUnitId, orderNo);
    }

    public Map<String, Object> session(String token) {
        return sandboxCheckoutService.readSession(token);
    }

    public Map<String, Object> refuseNonSandbox(String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", "SANDBOX_ONLY");
        out.put("code", "SANDBOX_ONLY");
        return out;
    }
}
