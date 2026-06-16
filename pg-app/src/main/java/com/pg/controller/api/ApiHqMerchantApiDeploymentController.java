package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.merchantdeploy.MerchantApiDeploymentService;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 배포설정 — 가맹점 API 연동 키트(브로커 URL·노티·MID 요약·시크릿 발급).
 */
@RestController
@RequestMapping(value = "/api/hq/merchant-api-deployment", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqMerchantApiDeploymentController {

    private final MerchantApiDeploymentService deploymentService;
    private final CompService compService;
    private final AuthService authService;

    public ApiHqMerchantApiDeploymentController(MerchantApiDeploymentService deploymentService,
                                                CompService compService,
                                                AuthService authService) {
        this.deploymentService = deploymentService;
        this.compService = compService;
        this.authService = authService;
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> vendors() {
        return ResponseEntity.ok(ApiResponse.ok(deploymentService.listVendors()));
    }

    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> merchants(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        String scopeCompId = null;
        boolean scopeSubtreeBelow = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppUser appUser = (auth != null && auth.getPrincipal() instanceof AppUser au) ? au : null;
        if (appUser != null && !"ADMIN".equalsIgnoreCase(appUser.getRole())) {
            Map<String, Object> org = authService.getOrgInfo(appUser.getUsername());
            if (org != null && org.get("compId") != null) {
                scopeCompId = org.get("compId").toString().trim();
            }
            scopeSubtreeBelow = true;
        }
        if (appUser != null && !"ADMIN".equalsIgnoreCase(appUser.getRole())
                && (scopeCompId == null || scopeCompId.isBlank())) {
            PageResult<Map<String, Object>> empty = new PageResult<>();
            empty.setList(List.of());
            empty.setPage(page);
            empty.setSize(size);
            empty.setTotalElements(0);
            empty.setTotalPages(1);
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }
        PageResult<Map<String, Object>> result = deploymentService.searchMerchants(
                searchCompId, searchCompNm, page, size, scopeCompId, scopeSubtreeBelow);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/kit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kit(
            @RequestParam String compId,
            @RequestParam(required = false) String vendorScope,
            HttpServletRequest req) {
        try {
            assertCanViewComp(compId);
            return ResponseEntity.ok(ApiResponse.ok(deploymentService.buildKit(compId, vendorScope, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 배포설정 — API배포문서: 가맹 다운로드·연동 파라미터(브로커 시크릿 평문 제외) */
    @GetMapping("/docs-portal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> docsPortal(
            @RequestParam String compId,
            HttpServletRequest req) {
        try {
            assertCanViewComp(compId);
            return ResponseEntity.ok(ApiResponse.ok(deploymentService.buildDocsPortal(compId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/credential/rotate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotate(@RequestBody Map<String, Object> body) {
        try {
            String compId = str(body.get("compId"));
            assertCanViewComp(compId);
            String vendorScope = str(body.get("vendorScope"));
            String issuedBy = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser au) {
                issuedBy = au.getUsername();
            }
            return ResponseEntity.ok(ApiResponse.ok(deploymentService.rotateBrokerSecret(compId, vendorScope, issuedBy)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/credential/enforce")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enforce(@RequestBody Map<String, Object> body) {
        try {
            String compId = str(body.get("compId"));
            assertCanViewComp(compId);
            String vendorScope = str(body.get("vendorScope"));
            boolean enforce = parseBool(body.get("enforceYn"));
            return ResponseEntity.ok(ApiResponse.ok(deploymentService.setEnforce(compId, vendorScope, enforce)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private void assertCanViewComp(String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
                String target = compId != null ? compId.trim() : "";
                if (mine.isEmpty() || target.isEmpty() || !compService.isTargetUnderViewerOrg(mine, target)) {
                    throw new IllegalArgumentException("소속 업체 및 하위 가맹점만 조회할 수 있습니다.");
                }
            }
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static boolean parseBool(Object o) {
        if (o == null) {
            return false;
        }
        String s = o.toString().trim();
        return "Y".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "1".equals(s);
    }
}
