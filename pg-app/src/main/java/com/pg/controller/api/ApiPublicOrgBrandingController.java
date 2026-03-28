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

import java.util.LinkedHashMap;
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
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("mainImageUrl", "");
            empty.put("logoImageUrl", "");
            empty.put("theme", "DEFAULT");
            empty.put("brandHost", "");
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                        || ou.getOrgLevel() == OrgLevel.REGIONAL
                        || ou.getOrgLevel() == OrgLevel.MASTER_DIST)
                .flatMap(ou -> brandingRepository.findByOrgUnitId(ou.getId())
                        .map(b -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("compId", compId);
                            m.put("mainImageUrl", b.getMainImageUrl() != null ? b.getMainImageUrl() : "");
                            m.put("logoImageUrl", b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "");
                            m.put("theme", b.getTheme() != null ? b.getTheme() : "DEFAULT");
                            m.put("brandHost", b.getBrandHost() != null ? b.getBrandHost() : "");
                            return m;
                        }))
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("mainImageUrl", "");
                    empty.put("logoImageUrl", "");
                    empty.put("theme", "DEFAULT");
                    empty.put("brandHost", "");
                    return ResponseEntity.ok(ApiResponse.ok(empty));
                });
    }
}
