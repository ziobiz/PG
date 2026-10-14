package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.crypto.CryptoCatalogService;
import com.pg.crypto.CryptoCheckoutService;
import com.pg.entity.OrgUnit;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 공개 크립토 결제 API — 구매자 체크아웃. */
@RestController
@RequestMapping(value = "/api/pay/crypto", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiCryptoPayController {

    private final CryptoCheckoutService checkoutService;
    private final CryptoCatalogService catalogService;
    private final OrgUnitRepository orgUnitRepository;

    public ApiCryptoPayController(CryptoCheckoutService checkoutService,
                                  CryptoCatalogService catalogService,
                                  OrgUnitRepository orgUnitRepository) {
        this.checkoutService = checkoutService;
        this.catalogService = catalogService;
        this.orgUnitRepository = orgUnitRepository;
    }

    @GetMapping("/wallets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> wallets(
            @RequestParam(value = "network", required = false) String network) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "wallets", catalogService.wallets(network),
                "catalog", catalogService.publicCatalog(null, null)
        )));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalog() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.publicCatalog(null, null)));
    }

    /** 수단선택·세션 전 — 상단 로고·안내문구. channel=unified|crypto */
    @GetMapping("/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutContext(
            @RequestParam(value = "m", required = false) String m,
            @RequestParam(value = "compId", required = false) String compId,
            @RequestParam(value = "channel", required = false) String channel) {
        String mid = (m != null && !m.isBlank()) ? m.trim() : (compId != null ? compId.trim() : "");
        Long orgUnitId = mid.isEmpty()
                ? null
                : orgUnitRepository.findByCode(mid).map(OrgUnit::getId).orElse(null);
        if (orgUnitId == null && !mid.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "INVALID_MERCHANT"));
        }
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.checkoutContext(orgUnitId, channel));
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepare(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("orgUnitId 또는 compId가 필요합니다.", "INVALID_MERCHANT"));
        }
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.prepare(orgUnitId, body, request));
    }

    /** 표시금액 → 청구(USD) → 자산 송금액 실시간 견적 */
    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimate(
            @RequestBody(required = false) Map<String, Object> body) {
        Long orgUnitId = resolveOrgUnitId(body);
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.estimateFx(orgUnitId, body != null ? body : Map.of()));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> session(
            @RequestParam("session") String session) {
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.session(session));
    }

    /** 자산·네트워크 선택 확정 → 수탁 주소 갱신 */
    @PostMapping("/bind")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bind(
            @RequestBody(required = false) Map<String, Object> body) {
        String session = body != null && body.get("session") != null
                ? body.get("session").toString().trim()
                : (body != null && body.get("sessionToken") != null
                ? body.get("sessionToken").toString().trim() : "");
        if (session.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("session이 필요합니다.", "INVALID_SESSION"));
        }
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.bindSelection(session, body));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @RequestParam(value = "session", required = false) String session,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "orgUnitId", required = false) Long orgUnitId) {
        return MerchantApiResponseMapper.mapServiceResult(checkoutService.status(session, orderNo, orgUnitId));
    }

    private Long resolveOrgUnitId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object id = body.get("orgUnitId");
        if (id != null && !id.toString().isBlank()) {
            try {
                return Long.valueOf(id.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        Object comp = body.get("compId");
        if (comp == null) {
            comp = body.get("m");
        }
        if (comp != null && !comp.toString().isBlank()) {
            return orgUnitRepository.findByCode(comp.toString().trim()).map(OrgUnit::getId).orElse(null);
        }
        return null;
    }
}
