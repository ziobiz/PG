package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgBranding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 운영매뉴얼 — 총본사(HEADQUARTERS) 기본정보·브랜딩 + 로그인 조직별 노출 audience.
 * 매뉴얼 HTML의 로고·회사명·주소 등은 이 API 값을 주입한다.
 */
@RestController
@RequestMapping(value = "/api/hq/platformManuals", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqPlatformManualsController {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final AuthService authService;

    public ApiHqPlatformManualsController(OrgUnitRepository orgUnitRepository,
                                          MerchantProfileRepository merchantProfileRepository,
                                          OrgBrandingRepository orgBrandingRepository,
                                          AuthService authService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.authService = authService;
    }

    @GetMapping("/brand")
    public ResponseEntity<ApiResponse<Map<String, Object>>> brand(Authentication authentication) {
        OrgUnit hq = resolveHeadquarters();
        Map<String, Object> m = new LinkedHashMap<>();
        if (hq == null) {
            m.put("compId", "");
            m.put("compNm", "ICOPAY");
            m.put("siteName", "ICOPAY");
            m.put("logoImageUrl", "");
            m.put("firstLogoImageUrl", "");
            m.put("addr", "");
            m.put("compTel", "");
            m.put("email", "");
            m.put("homepage", "");
            m.put("copyright", "");
        } else {
            m.put("compId", hq.getCode() != null ? hq.getCode() : "");
            m.put("compNm", hq.getName() != null ? hq.getName() : "ICOPAY");

            Optional<OrgBranding> brandOpt = orgBrandingRepository.findByOrgUnitId(hq.getId());
            if (brandOpt.isPresent()) {
                OrgBranding b = brandOpt.get();
                m.put("logoImageUrl", nz(b.getLogoImageUrl()));
                m.put("firstLogoImageUrl", nz(b.getFirstLogoImageUrl()));
                m.put("siteName", !nz(b.getSiteName()).isEmpty() ? b.getSiteName().trim() : m.get("compNm"));
            } else {
                m.put("logoImageUrl", "");
                m.put("firstLogoImageUrl", "");
                m.put("siteName", m.get("compNm"));
            }

            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(hq.getId());
            if (mpOpt.isPresent()) {
                MerchantProfile mp = mpOpt.get();
                String addr = joinAddr(mp.getZipCode(), mp.getAddr(), mp.getAddrDetail(), mp.getAddrEtc());
                m.put("addr", addr);
                m.put("compTel", nz(mp.getCompTel()));
                m.put("email", nz(mp.getEmail()));
                String home = nz(mp.getHomepage());
                if (home.isEmpty()) {
                    home = nz(mp.getSiteUrl());
                }
                m.put("homepage", home);
                m.put("copyright", "");
            } else {
                m.put("addr", "");
                m.put("compTel", "");
                m.put("email", "");
                m.put("homepage", "");
                m.put("copyright", "");
            }
        }

        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        if (viewerLevel != null) {
            m.put("viewerOrgLevel", viewerLevel.name());
            m.put("viewerOrgLevelNm", viewerLevel.getNameKo());
            m.put("viewerOrgLevelCode", viewerLevel.getCode());
        } else {
            m.put("viewerOrgLevel", "HEADQUARTERS");
            m.put("viewerOrgLevelNm", OrgLevel.HEADQUARTERS.getNameKo());
            m.put("viewerOrgLevelCode", OrgLevel.HEADQUARTERS.getCode());
            viewerLevel = OrgLevel.HEADQUARTERS;
        }
        m.put("allowedAudiences", allowedAudiences(viewerLevel));
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    /**
     * 총본사: 전체 / 본사·총판: hqdist+merchant / 지사·대리점·영업점·가맹: merchant 만.
     */
    static List<String> allowedAudiences(OrgLevel level) {
        List<String> out = new ArrayList<>();
        if (level == null || level == OrgLevel.HEADQUARTERS) {
            out.add("super");
            out.add("hqdist");
            out.add("merchant");
            return out;
        }
        int code = level.getCode();
        if (code <= OrgLevel.MASTER_DIST.getCode()) {
            out.add("hqdist");
            out.add("merchant");
            return out;
        }
        out.add("merchant");
        return out;
    }

    private OrgLevel resolveViewerOrgLevel(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return OrgLevel.HEADQUARTERS;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return OrgLevel.HEADQUARTERS;
        }
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .map(OrgUnit::getOrgLevel)
                .orElse(OrgLevel.HEADQUARTERS);
    }

    private OrgUnit resolveHeadquarters() {
        Optional<OrgUnit> byCode = orgUnitRepository.findByCode("0000000000");
        if (byCode.isPresent() && byCode.get().getOrgLevel() == OrgLevel.HEADQUARTERS) {
            return byCode.get();
        }
        List<OrgUnit> list = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.HEADQUARTERS);
        return list.isEmpty() ? null : list.get(0);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String joinAddr(String zip, String a1, String a2, String a3) {
        StringBuilder sb = new StringBuilder();
        if (zip != null && !zip.isBlank()) {
            sb.append('(').append(zip.trim()).append(") ");
        }
        if (a1 != null && !a1.isBlank()) {
            sb.append(a1.trim());
        }
        if (a2 != null && !a2.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a2.trim());
        }
        if (a3 != null && !a3.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a3.trim());
        }
        return sb.toString().trim();
    }
}
