package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgLevel;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로그인 페이지용 브랜딩 조회 (인증 불필요)
 * GET /api/public/org/branding?compId=XXX
 */
@RestController
@RequestMapping(value = "/api/public/org/branding", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPublicOrgBrandingController {

    private final OrgBrandingRepository brandingRepository;
    private final OrgUnitRepository orgUnitRepository;

    public ApiPublicOrgBrandingController(OrgBrandingRepository brandingRepository, OrgUnitRepository orgUnitRepository) {
        this.brandingRepository = brandingRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@RequestParam(required = false) String compId) {
        if (compId == null || compId.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "mainImageUrl", "",
                    "logoImageUrl", "",
                    "theme", "DEFAULT"
            )));
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                        || ou.getOrgLevel() == OrgLevel.REGIONAL
                        || ou.getOrgLevel() == OrgLevel.MASTER_DIST)
                .flatMap(ou -> brandingRepository.findByOrgUnitId(ou.getId())
                        .map(b -> Map.<String, Object>of(
                                "compId", compId,
                                "mainImageUrl", b.getMainImageUrl() != null ? b.getMainImageUrl() : "",
                                "logoImageUrl", b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "",
                                "theme", b.getTheme() != null ? b.getTheme() : "DEFAULT"
                        )))
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.ok(Map.of(
                        "mainImageUrl", "",
                        "logoImageUrl", "",
                        "theme", "DEFAULT"
                ))));
    }
}
