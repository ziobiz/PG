package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import com.pg.service.MerchantChatbotOrderService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.util.ChatbotMerchantAdminConstants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 챗봇 주문 목록(주문자·예약·결제 연동).
 */
@RestController
@RequestMapping(value = "/api/chatbot/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiChatbotOrderController {

    private final MerchantChatbotOrderService orderService;
    private final MerchantChatbotProductService productService;
    private final AuthService authService;
    private final CompService compService;

    public ApiChatbotOrderController(MerchantChatbotOrderService orderService,
                                     MerchantChatbotProductService productService,
                                     AuthService authService,
                                     CompService compService) {
        this.orderService = orderService;
        this.productService = productService;
        this.authService = authService;
        this.compService = compService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@RequestParam String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        String cid = compId != null ? compId.trim() : "";
        if (cid.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("compId가 필요합니다.", "INVALID"));
        }
        if (!canAccessComp(user, cid)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        if (!merchantMayUseChatbotOrders(user, cid)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "챗봇 주문관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(cid);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail("챗봇 결제가 비활성입니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(orderService.listOrderRowsForOrg(ou.get().getId())));
    }

    private boolean merchantMayUseChatbotOrders(AppUser user, String targetCompId) {
        if (user == null || targetCompId == null || targetCompId.isBlank()) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String ol = org != null && org.get("orgLevel") != null
                ? String.valueOf(org.get("orgLevel")).trim().toUpperCase(Locale.ROOT) : "";
        if (!"MERCHANT".equals(ol)) {
            return true;
        }
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        if (!mine.equalsIgnoreCase(targetCompId.trim())) {
            return false;
        }
        return ChatbotMerchantAdminConstants.merchantAdminWebMayUseChatbotFeatures(user);
    }

    private boolean canAccessComp(AppUser u, String targetCompId) {
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        if (mine.isEmpty() || targetCompId == null || targetCompId.isBlank()) {
            return false;
        }
        String target = targetCompId.trim();
        String ol = org != null && org.get("orgLevel") != null
                ? String.valueOf(org.get("orgLevel")).trim().toUpperCase(Locale.ROOT) : "";
        if ("MERCHANT".equals(ol)) {
            return mine.equalsIgnoreCase(target);
        }
        return compService.isTargetUnderViewerOrg(mine, target);
    }
}
