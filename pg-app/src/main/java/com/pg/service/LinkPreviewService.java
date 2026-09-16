package com.pg.service;

import com.pg.entity.OrgBranding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.LinkPreviewOgSupport;
import com.pg.util.LinkPreviewOgSupport.OgSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Host로 총본사·본사·총판을 찾고 FOLLOW_HQ 상속 후 크롤러용 제목·설명·이미지를 만든다.
 */
@Service
public class LinkPreviewService {

    public static final String HQ_CODE = "0000000000";

    public record Preview(String title, String description, String imageAbsUrl, String pageUrl, String lang) {
    }

    private final OrgUnitRepository orgUnitRepository;
    private final OrgBrandingRepository brandingRepository;
    private final OrgPortalHostService orgPortalHostService;

    @Value("${app.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public LinkPreviewService(OrgUnitRepository orgUnitRepository,
                              OrgBrandingRepository brandingRepository,
                              OrgPortalHostService orgPortalHostService) {
        this.orgUnitRepository = orgUnitRepository;
        this.brandingRepository = brandingRepository;
        this.orgPortalHostService = orgPortalHostService;
    }

    public Preview resolve(HttpServletRequest request) {
        String lang = LoginNoticePublicService.pickLangBucket(request.getHeader("Accept-Language"));
        String pageUrl = requestUrl(request);
        OrgUnit org = resolveOrg(clientHost(request)).orElse(null);
        OrgBranding branding = resolveEffectiveBranding(org);
        String title = resolveTitle(branding, org, lang);
        String desc = resolveDescription(branding, lang);
        String image = resolveImage(branding);
        return new Preview(title, desc, image, pageUrl, lang);
    }

    Optional<OrgUnit> resolveOrg(String host) {
        if (host != null && !host.isBlank()) {
            Optional<OrgUnit> portal = orgPortalHostService.findPortalOrgByAdminWebHost(host);
            if (portal.isPresent()) {
                return portal;
            }
        }
        return orgUnitRepository.findByCode(HQ_CODE)
                .filter(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS);
    }

    OrgBranding resolveEffectiveBranding(OrgUnit org) {
        if (org == null) {
            return hqBranding().orElse(null);
        }
        OrgBranding own = brandingOf(org).orElse(null);
        OrgUnit parent = org.getParentId() != null
                ? orgUnitRepository.findById(org.getParentId()).orElse(null)
                : null;
        String parentMode = parent == null ? null : brandingOf(parent).map(OrgBranding::getOgMode).orElse(null);
        OgSource src = LinkPreviewOgSupport.cascade(
                org.getOrgLevel(),
                own != null ? own.getOgMode() : null,
                parent != null ? parent.getOrgLevel() : null,
                parentMode);
        return switch (src) {
            case SELF -> own;
            case PARENT -> parent == null ? hqBranding().orElse(own) : brandingOf(parent).orElse(own);
            case HQ -> hqBranding().orElse(own);
            case DEFAULT -> hqBranding().orElse(own);
        };
    }

    private String resolveTitle(OrgBranding b, OrgUnit org, String lang) {
        if (b != null) {
            Map<String, String> titles = LinkPreviewOgSupport.parseLangMap(b.getOgTitleJson());
            String picked = LinkPreviewOgSupport.pick(titles, lang);
            if (!picked.isEmpty()) {
                return picked;
            }
            if (b.getSiteName() != null && !b.getSiteName().isBlank()) {
                return b.getSiteName().trim();
            }
        }
        if (org != null && org.getDomainPageTitle() != null && !org.getDomainPageTitle().isBlank()) {
            return org.getDomainPageTitle().trim();
        }
        return LinkPreviewOgSupport.DEFAULT_TITLE;
    }

    private String resolveDescription(OrgBranding b, String lang) {
        if (b != null) {
            Map<String, String> descs = LinkPreviewOgSupport.parseLangMap(b.getOgDescJson());
            String picked = LinkPreviewOgSupport.pick(descs, lang);
            if (!picked.isEmpty()) {
                return picked;
            }
        }
        return LinkPreviewOgSupport.defaultDescription(lang);
    }

    private String resolveImage(OrgBranding b) {
        if (b == null) {
            return "";
        }
        String rel = firstNonBlank(
                b.getOgImageUrl(),
                b.getFirstLogoImageUrl(),
                b.getLogoImageUrl(),
                b.getMainImageUrl(),
                b.getPopconImageUrl());
        return LinkPreviewOgSupport.toAbsoluteUrl(rel, publicBaseUrl);
    }

    private Optional<OrgBranding> brandingOf(OrgUnit org) {
        if (org == null || org.getId() == null) {
            return Optional.empty();
        }
        return brandingRepository.findByOrgUnitId(org.getId());
    }

    private Optional<OrgBranding> hqBranding() {
        return orgUnitRepository.findByCode(HQ_CODE)
                .filter(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .flatMap(this::brandingOf);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String clientHost(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-Host");
        if (xf != null && !xf.isBlank()) {
            String first = xf.split(",")[0].trim();
            int colon = first.indexOf(':');
            return colon > 0 ? first.substring(0, colon) : first;
        }
        String host = request.getHeader("Host");
        if (host != null && !host.isBlank()) {
            int colon = host.indexOf(':');
            return colon > 0 ? host.substring(0, colon) : host.trim();
        }
        return request.getServerName();
    }

    private static String requestUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.getScheme();
        } else {
            proto = proto.split(",")[0].trim();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        } else {
            host = host.split(",")[0].trim();
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            uri = "/";
        }
        return proto + "://" + host + uri;
    }
}
