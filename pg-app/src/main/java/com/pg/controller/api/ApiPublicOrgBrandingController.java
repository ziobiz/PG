package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
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
 * 로그인 페이지용 브랜딩 조회 (인증 불필요)
 * GET /api/public/org/branding?compId=XXX
 */
@RestController
@RequestMapping(value = "/api/public/org/branding", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPublicOrgBrandingController {

    private final OrgBrandingRepository brandingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgPortalHostService orgPortalHostService;

    public ApiPublicOrgBrandingController(OrgBrandingRepository brandingRepository, OrgUnitRepository orgUnitRepository,
                                          OrgPortalHostService orgPortalHostService) {
        this.brandingRepository = brandingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgPortalHostService = orgPortalHostService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@RequestParam(required = false) String compId,
                                                                @RequestParam(required = false) String host) {
        if (host != null && !host.trim().isEmpty()) {
            String hostNorm = normalizeHost(host);
            Optional<OrgUnit> byHost = orgPortalHostService.findPortalOrgByAdminWebHost(hostNorm);
            if (byHost.isPresent()) {
                return mapBranding(byHost.get(), byHost.get().getCode());
            }
            // 기본 운영 로그인 도메인(api.icopay.co.kr)에서는 총본사 브랜딩을 기본 적용
            if (isDefaultHqLoginHost(hostNorm)) {
                Optional<OrgUnit> hq = orgUnitRepository.findByCode("0000000000").filter(this::isBrandingOrg);
                if (hq.isPresent()) {
                    return mapBranding(hq.get(), "0000000000");
                }
            }
            // 도메인 브랜딩 분리 원칙:
            // host 파라미터가 왔는데 매핑이 없으면 compId로 폴백하지 않고 기본값을 반환한다.
            return ResponseEntity.ok(ApiResponse.ok(emptyBranding()));
        }
        if (compId == null || compId.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyBranding()));
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(this::isBrandingOrg)
                .flatMap(ou -> mapBrandingData(ou, compId.trim()))
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(emptyBranding())));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> mapBranding(OrgUnit ou, String codeForResponse) {
        if (ou == null || !isBrandingOrg(ou)) {
            return ResponseEntity.ok(ApiResponse.ok(emptyBranding()));
        }
        return mapBrandingData(ou, codeForResponse)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(emptyBranding())));
    }

    private Optional<Map<String, Object>> mapBrandingData(OrgUnit ou, String codeForResponse) {
        return brandingRepository.findByOrgUnitId(ou.getId()).map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("compId", codeForResponse != null ? codeForResponse : "");
            m.put("mainImageUrl", b.getMainImageUrl() != null ? b.getMainImageUrl() : "");
            m.put("logoImageUrl", b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "");
            m.put("popconImageUrl", b.getPopconImageUrl() != null ? b.getPopconImageUrl() : "");
            m.put("theme", b.getTheme() != null ? b.getTheme() : "DEFAULT");
            m.put("brandHost", b.getBrandHost() != null ? b.getBrandHost() : "");
            return m;
        });
    }

    private boolean isBrandingOrg(OrgUnit ou) {
        return ou != null && (ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                || ou.getOrgLevel() == OrgLevel.REGIONAL
                || ou.getOrgLevel() == OrgLevel.MASTER_DIST);
    }

    private String normalizeHost(String host) {
        if (host == null) return "";
        String h = host.trim().toLowerCase();
        int colon = h.indexOf(':');
        return colon > 0 ? h.substring(0, colon) : h;
    }

    private boolean isDefaultHqLoginHost(String host) {
        if (host == null || host.isBlank()) return false;
        return "api.icopay.co.kr".equals(host) || "www.api.icopay.co.kr".equals(host);
    }

    private Map<String, Object> emptyBranding() {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("mainImageUrl", "");
        empty.put("logoImageUrl", "");
        empty.put("popconImageUrl", "");
        empty.put("theme", "DEFAULT");
        empty.put("brandHost", "");
        return empty;
    }
}
