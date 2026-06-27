package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.merchantdeploy.MerchantApiDeploymentService;
import com.pg.service.AuthService;
import com.pg.util.MerchantNotifyUrlVisibility;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 가맹점 전용 — 본사 API 배포 결과(키·연동 정보) 조회만. 발급·수정 없음.
 */
@RestController
@RequestMapping(value = "/api/merchant/api-portal", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiMerchantApiPortalController {

    private final MerchantApiDeploymentService deploymentService;
    private final AuthService authService;

    public ApiMerchantApiPortalController(MerchantApiDeploymentService deploymentService,
                                          AuthService authService) {
        this.deploymentService = deploymentService;
        this.authService = authService;
    }

    @GetMapping("/self")
    public ResponseEntity<ApiResponse<Map<String, Object>>> self(HttpServletRequest req) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AppUser appUser = (auth != null && auth.getPrincipal() instanceof AppUser au) ? au : null;
            if (appUser == null) {
                return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "AUTH"));
            }
            Map<String, Object> org = authService.getOrgInfo(appUser.getUsername());
            if (org == null) {
                return ResponseEntity.ok(ApiResponse.fail("조직 정보를 찾을 수 없습니다.", "VALIDATION"));
            }
            String orgLevel = org.get("orgLevel") != null ? org.get("orgLevel").toString().trim() : "";
            String compId = org.get("compId") != null ? org.get("compId").toString().trim() : "";
            Map<String, Object> portal = deploymentService.buildMerchantSelfPortal(req, compId, orgLevel);
            MerchantNotifyUrlVisibility.redactSelfPortal(portal);
            return ResponseEntity.ok(ApiResponse.ok(portal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
