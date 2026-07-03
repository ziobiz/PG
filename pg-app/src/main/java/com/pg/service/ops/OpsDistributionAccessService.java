package com.pg.service.ops;

import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 운영관리 — 유통망내역·유통망정산 접근(가맹점 제외, 조직 로그인 + ADMIN).
 */
@Service
public class OpsDistributionAccessService {

    private final AuthService authService;

    public OpsDistributionAccessService(AuthService authService) {
        this.authService = authService;
    }

    public Optional<String> accessDeniedReason(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return Optional.of("로그인이 필요합니다.");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return Optional.empty();
        }
        OrgUnit ou = authService.resolveOrgUnitForLoginId(user.getUsername()).orElse(null);
        if (ou == null || ou.getOrgLevel() == null) {
            return Optional.of("조직 정보가 없어 유통망 화면을 열 수 없습니다.");
        }
        if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
            return Optional.of("가맹점 로그인은 이용할 수 없습니다.");
        }
        return Optional.empty();
    }

    public Map<String, Object> accessMeta(Authentication authentication, String screen) {
        Map<String, Object> m = new LinkedHashMap<>();
        Optional<String> deny = accessDeniedReason(authentication);
        m.put("allowed", deny.isEmpty());
        deny.ifPresent(s -> m.put("reason", s));
        m.put("screen", screen);
        if (!(authentication != null && authentication.getPrincipal() instanceof AppUser u)) {
            return m;
        }
        m.put("isAdmin", "ADMIN".equalsIgnoreCase(u.getRole()));
        authService.resolveOrgUnitForLoginId(u.getUsername()).ifPresent(ou -> {
            if (ou.getOrgLevel() != null) {
                m.put("viewerOrgLevel", ou.getOrgLevel().name());
                m.put("viewerOrgLevelNm", ou.getOrgLevel().getNameKo());
            }
            if (ou.getCode() != null) {
                m.put("viewerOrgCode", ou.getCode().trim());
            }
            if (ou.getName() != null) {
                m.put("viewerOrgNm", ou.getName().trim());
            }
        });
        return m;
    }

    public OrgLevel resolveViewerOrgLevel(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return OrgLevel.HEADQUARTERS;
        }
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .map(OrgUnit::getOrgLevel)
                .orElse(null);
    }
}
