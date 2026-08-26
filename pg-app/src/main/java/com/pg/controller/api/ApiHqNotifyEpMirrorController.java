package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.ElementPayNotiMiddlewareMirrorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * 본사 — ElementPay NOTI pay 미러 재전송 (ICOPAY→NOTI {@code /noti/elementpay}).
 * 수령 로그(수신)와 별도: 이미 승인된 URL결제 건을 재결제 없이 NOTI로 다시 보냅니다.
 */
@RestController
@RequestMapping(value = "/api/hq/notifyEpMirror", produces = "application/json")
public class ApiHqNotifyEpMirrorController {

    private final ElementPayNotiMiddlewareMirrorService mirrorService;
    private final AuthService authService;

    public ApiHqNotifyEpMirrorController(ElementPayNotiMiddlewareMirrorService mirrorService,
                                         AuthService authService) {
        this.mirrorService = mirrorService;
        this.authService = authService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lookup(
            Authentication authentication,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String trnId) {
        if (!canUse(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "총본사(HEADQUARTERS) 또는 시스템 관리자만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        Map<String, Object> m = mirrorService.lookupTxn(orderNo, trnId);
        if (!Boolean.TRUE.equals(m.get("success"))) {
            String code = m.get("errorCode") != null ? String.valueOf(m.get("errorCode")) : "ERROR";
            String msg = m.get("message") != null ? String.valueOf(m.get("message")) : "lookup failed";
            return ResponseEntity.ok(ApiResponse.fail(msg, code));
        }
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resend(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        if (!canUse(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "총본사(HEADQUARTERS) 또는 시스템 관리자만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        String orderNo = body != null && body.get("orderNo") != null
                ? String.valueOf(body.get("orderNo")) : "";
        String trnId = body != null && body.get("trnId") != null
                ? String.valueOf(body.get("trnId")) : "";
        if (trnId.isBlank() && body != null && body.get("paymentId") != null) {
            trnId = String.valueOf(body.get("paymentId"));
        }
        boolean force = body != null && body.get("force") != null
                && Boolean.parseBoolean(String.valueOf(body.get("force")));
        if ((orderNo == null || orderNo.isBlank()) && (trnId == null || trnId.isBlank())) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "주문번호 또는 거래번호를 입력하세요.", "VALIDATION"));
        }
        Map<String, Object> m = mirrorService.remirrorByOrderOrTrnId(orderNo, trnId, force);
        if (!Boolean.TRUE.equals(m.get("success"))) {
            String code = m.get("errorCode") != null ? String.valueOf(m.get("errorCode")) : "ERROR";
            String msg = m.get("message") != null ? String.valueOf(m.get("message")) : "resend failed";
            ApiResponse<Map<String, Object>> fail = ApiResponse.fail(msg, code);
            fail.setData(m);
            return ResponseEntity.ok(fail);
        }
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    private boolean canUse(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        return org != null && "HEADQUARTERS".equals(
                String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }
}
