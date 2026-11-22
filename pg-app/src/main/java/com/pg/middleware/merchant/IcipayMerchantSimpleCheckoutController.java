package com.pg.middleware.merchant;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgUnit;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.merchantdeploy.MerchantBrokerAccessVerifier;
import com.pg.merchantdeploy.MerchantPgBrokerVendor;
import com.pg.merchantdeploy.MerchantSandboxCheckoutFacade;
import com.pg.merchantdeploy.MerchantUnifiedSimpleCheckoutService;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 간편결제(QR URL) — REDIRECT 확장 API.
 * 기존 {@code /checkout/redirect/**} 는 그대로 유지.
 */
@RestController
@RequestMapping("/api/middleware/v1/merchant/checkout/simple")
public class IcipayMerchantSimpleCheckoutController {

    private final MerchantUnifiedSimpleCheckoutService simpleCheckoutService;
    private final MerchantSandboxCheckoutFacade sandboxCheckoutFacade;
    private final MerchantBrokerAccessVerifier brokerAccessVerifier;
    private final OrgUnitRepository orgUnitRepository;

    public IcipayMerchantSimpleCheckoutController(MerchantUnifiedSimpleCheckoutService simpleCheckoutService,
                                                  MerchantSandboxCheckoutFacade sandboxCheckoutFacade,
                                                  MerchantBrokerAccessVerifier brokerAccessVerifier,
                                                  OrgUnitRepository orgUnitRepository) {
        this.simpleCheckoutService = simpleCheckoutService;
        this.sandboxCheckoutFacade = sandboxCheckoutFacade;
        this.brokerAccessVerifier = brokerAccessVerifier;
        this.orgUnitRepository = orgUnitRepository;
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepare(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verifyMerchantApi(request, body != null ? body : Map.of(),
                    MerchantPgBrokerVendor.ALL);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result;
        if (sandboxCheckoutFacade.isSandbox(request)) {
            result = sandboxCheckoutFacade.prepare(orgUnitId, body != null ? body : Map.of());
            patchSandboxPayUrlHint(result);
        } else {
            result = simpleCheckoutService.prepare(orgUnitId, body != null ? body : Map.of(), request);
        }
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> session(@RequestParam("token") String token) {
        Map<String, Object> result = sandboxCheckoutFacade.isSandboxSessionToken(token)
                ? sandboxCheckoutFacade.session(token)
                : simpleCheckoutService.readSession(token);
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    /**
     * 간편결제 UI 미리보기(공개) — 세션 없이 저장된 로고·부제·노출 설정만 반환.
     * 실결제·prepare 아님. 관리자 「간편결제 UI URL」용.
     */
    @GetMapping("/ui-preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uiPreview(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String m) {
        String mid = (compId != null && !compId.isBlank()) ? compId : m;
        return MerchantApiResponseMapper.mapServiceResult(simpleCheckoutService.buildUiPreview(mid));
    }

    /** PC QR 대기 — 세션 토큰만으로 승인 여부(공개). */
    @GetMapping("/public-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicStatus(@RequestParam("token") String token) {
        if (sandboxCheckoutFacade.isSandboxSessionToken(token)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "READY");
            data.put("paid", false);
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("success", true);
            wrap.put("data", data);
            return MerchantApiResponseMapper.mapServiceResult(wrap);
        }
        return MerchantApiResponseMapper.mapServiceResult(simpleCheckoutService.publicStatusBySession(token));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam String orderNo,
            HttpServletRequest request) {
        Map<String, Object> idBody = new LinkedHashMap<>();
        if (compId != null) {
            idBody.put("compId", compId);
        }
        if (merchantId != null) {
            idBody.put("merchantId", merchantId);
        }
        try {
            brokerAccessVerifier.verifyMerchantApi(request, idBody, MerchantPgBrokerVendor.ALL);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(idBody);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = sandboxCheckoutFacade.isSandbox(request)
                ? sandboxCheckoutFacade.status(orgUnitId, orderNo)
                : simpleCheckoutService.orderStatus(orgUnitId, orderNo);
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    @SuppressWarnings("unchecked")
    private void patchSandboxPayUrlHint(Map<String, Object> result) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return;
        }
        data.put("integrationMode", "SIMPLE_QR_SANDBOX");
    }

    private Long resolveOrgUnitId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object mid = body.get("merchantId");
        if (mid instanceof Number) {
            return ((Number) mid).longValue();
        }
        if (mid != null) {
            try {
                return Long.parseLong(mid.toString().trim());
            } catch (NumberFormatException ignore) {
                /* fall through */
            }
        }
        Object cid = body.get("compId");
        if (cid == null) {
            cid = body.get("merchantCode");
        }
        if (cid == null) {
            return null;
        }
        String code = cid.toString().trim();
        if (code.isEmpty()) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(code);
        return ou.map(OrgUnit::getId).orElse(null);
    }
}
