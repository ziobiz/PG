package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgBrandingRepository;
import com.pg.service.OrgPortalHostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 로그인 페이지: 현재 브라우저 호스트에 대응하는 본사·총판 포털(도메인구성 관리자 URL) 및 브랜딩.
 * GET /api/public/org/portalByHost?host=jpjp.icopay.co.kr
 */
@RestController
@RequestMapping(value = "/api/public/org/portalByHost", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPublicOrgPortalController {

    private final OrgPortalHostService orgPortalHostService;
    private final OrgBrandingRepository brandingRepository;

    public ApiPublicOrgPortalController(OrgPortalHostService orgPortalHostService,
                                        OrgBrandingRepository brandingRepository) {
        this.orgPortalHostService = orgPortalHostService;
        this.brandingRepository = brandingRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@RequestParam(required = false) String host) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (host == null || host.isBlank()) {
            out.put("matched", false);
            return ResponseEntity.ok(ApiResponse.ok(out));
        }
        Optional<OrgUnit> portal = orgPortalHostService.findPortalOrgByAdminWebHost(host.trim());
        if (portal.isEmpty()) {
            out.put("matched", false);
            return ResponseEntity.ok(ApiResponse.ok(out));
        }
        OrgUnit ou = portal.get();
        out.put("matched", true);
        out.put("compId", ou.getCode() != null ? ou.getCode() : "");
        out.put("compNm", ou.getName() != null ? ou.getName() : "");
        out.put("orgLevel", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "");
        out.put("orgUnitId", ou.getId());
        out.put("adminWebUrl", ou.getOrgDomainAdminUrl() != null ? ou.getOrgDomainAdminUrl() : "");

        Map<String, Object> branding = new LinkedHashMap<>();
        branding.put("mainImageUrl", "");
        branding.put("logoImageUrl", "");
        branding.put("firstLogoImageUrl", "");
        branding.put("popconImageUrl", "");
        branding.put("theme", "DEFAULT");
        branding.put("brandHost", "");
        branding.put("siteName", "");
        brandingRepository.findByOrgUnitId(ou.getId()).ifPresent(b -> {
            branding.put("mainImageUrl", b.getMainImageUrl() != null ? b.getMainImageUrl() : "");
            branding.put("logoImageUrl", b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "");
            branding.put("firstLogoImageUrl", b.getFirstLogoImageUrl() != null ? b.getFirstLogoImageUrl() : "");
            branding.put("popconImageUrl", b.getPopconImageUrl() != null ? b.getPopconImageUrl() : "");
            branding.put("theme", b.getTheme() != null ? b.getTheme() : "DEFAULT");
            branding.put("brandHost", b.getBrandHost() != null ? b.getBrandHost() : "");
            branding.put("siteName", b.getSiteName() != null ? b.getSiteName() : "");
        });
        out.put("branding", branding);
        return ResponseEntity.ok(ApiResponse.ok(out));
    }
}
