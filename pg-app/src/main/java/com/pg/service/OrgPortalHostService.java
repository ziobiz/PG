package com.pg.service;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 본사(REGIONAL)·총판(MASTER_DIST) 도메인구성의 관리자(웹) URL 호스트와 동일 호스트에서의 로그인 허용 범위 판별.
 * 실제 허용 규칙은 {@link AuthService#loginHostAllowedForUser} 에서 적용한다.
 */
@Service
public class OrgPortalHostService {

    private static final Set<OrgLevel> PORTAL_ORG_LEVELS = EnumSet.of(OrgLevel.REGIONAL, OrgLevel.MASTER_DIST);

    /** 동일 호스트에 여러 행이 있으면 총판을 본사보다 우선(코드 순 보조). */
    private static final Comparator<OrgUnit> PORTAL_TIE_BREAK = Comparator
            .comparing((OrgUnit o) -> o.getOrgLevel() == OrgLevel.MASTER_DIST ? 0 : 1)
            .thenComparing(OrgUnit::getCode, Comparator.nullsFirst(String::compareTo));

    private final OrgUnitRepository orgUnitRepository;

    public OrgPortalHostService(OrgUnitRepository orgUnitRepository) {
        this.orgUnitRepository = orgUnitRepository;
    }

    /**
     * 브라우저 호스트와 일치하는 관리자(웹) URL을 가진 본사 또는 총판(1건).
     */
    public Optional<OrgUnit> findPortalOrgByAdminWebHost(String clientHost) {
        if (clientHost == null || clientHost.isBlank()) {
            return Optional.empty();
        }
        String h = hostOnlyFromClient(clientHost.trim());
        if (h.isEmpty()) {
            return Optional.empty();
        }
        return orgUnitRepository.findAll().stream()
                .filter(o -> o.getOrgLevel() != null && PORTAL_ORG_LEVELS.contains(o.getOrgLevel()))
                .filter(o -> o.getOrgDomainAdminUrl() != null && !o.getOrgDomainAdminUrl().isBlank())
                .filter(o -> {
                    String eh = hostFromConfiguredUrl(o.getOrgDomainAdminUrl().trim());
                    return eh != null && eh.equalsIgnoreCase(h);
                })
                .min(PORTAL_TIE_BREAK);
    }

    /**
     * 사용자 소속 조직이 포털 루트 본인이거나, 상위 체인에 포털 루트가 포함되는지.
     */
    public boolean userOrgBelongsToPortalSubtree(OrgUnit userOrg, OrgUnit portalRoot) {
        if (userOrg == null || portalRoot == null || portalRoot.getId() == null) {
            return false;
        }
        if (portalRoot.getOrgLevel() == null || !PORTAL_ORG_LEVELS.contains(portalRoot.getOrgLevel())) {
            return false;
        }
        Long curId = userOrg.getId();
        if (curId == null) {
            return false;
        }
        Set<Long> seen = new HashSet<>();
        while (curId != null && seen.add(curId)) {
            if (curId.equals(portalRoot.getId())) {
                return true;
            }
            OrgUnit cur = orgUnitRepository.findById(curId).orElse(null);
            if (cur == null) {
                break;
            }
            curId = cur.getParentId();
        }
        return false;
    }

    /**
     * {@code descendantOrgUnitId} 가 {@code ancestorOrgUnitId} 이거나, 상위로 거슬러 올라가며 그 조상 중 하나가 ancestor 인지.
     * (총판 포털에 로그인하는 본사 계정: 본사 조직이 해당 총판 트리 상단에 있는지 판별할 때 사용)
     */
    public boolean orgIsSelfOrUnderAncestor(Long ancestorOrgUnitId, Long descendantOrgUnitId) {
        if (ancestorOrgUnitId == null || descendantOrgUnitId == null) {
            return false;
        }
        Long cur = descendantOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if (ancestorOrgUnitId.equals(cur)) {
                return true;
            }
            OrgUnit o = orgUnitRepository.findById(cur).orElse(null);
            if (o == null) {
                break;
            }
            cur = o.getParentId();
        }
        return false;
    }

    private static String hostFromConfiguredUrl(String urlStr) {
        try {
            String u = urlStr.contains("://") ? urlStr : "https://" + urlStr;
            URI uri = URI.create(u);
            return uri.getHost() != null ? uri.getHost().trim().toLowerCase(Locale.ROOT) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String hostOnlyFromClient(String clientHost) {
        String t = clientHost.trim().toLowerCase(Locale.ROOT);
        int colon = t.indexOf(':');
        if (colon > 0) {
            return t.substring(0, colon);
        }
        return t;
    }
}
