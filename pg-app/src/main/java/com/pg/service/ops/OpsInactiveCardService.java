package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.HqPayCardBlacklist;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.OrgPagePermissionService;
import com.pg.util.PagePermissionCodes;
import com.pg.service.PayCardPolicyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 운영관리 — 비활성카드(블랙리스트) 등록·해지.
 * 접근: 본사권한설정 페이지 권한 + 총본사·본사·총판·ADMIN.
 */
@Service
public class OpsInactiveCardService {

    public static final String PAGE_URL = "/ops/inactiveCard";

    private final PayCardPolicyService payCardPolicyService;
    private final HqPayCardBlacklistRepository blacklistRepository;
    private final AuthService authService;
    private final OrgPagePermissionService orgPagePermissionService;
    private final OrgUnitRepository orgUnitRepository;

    public OpsInactiveCardService(PayCardPolicyService payCardPolicyService,
                                  HqPayCardBlacklistRepository blacklistRepository,
                                  AuthService authService,
                                  OrgPagePermissionService orgPagePermissionService,
                                  OrgUnitRepository orgUnitRepository) {
        this.payCardPolicyService = payCardPolicyService;
        this.blacklistRepository = blacklistRepository;
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.orgUnitRepository = orgUnitRepository;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        Map<String, Object> m = new LinkedHashMap<>();
        Optional<String> deny = accessDeniedReason(authentication);
        m.put("allowed", deny.isEmpty());
        m.put("denyReason", deny.orElse(null));
        m.put("canWrite", deny.isEmpty() && canWrite(authentication));
        m.put("pageUrl", PAGE_URL);
        return m;
    }

    public Optional<String> accessDeniedReason(Authentication authentication) {
        AppUser user = resolveUser(authentication);
        if (user == null) {
            return Optional.of("로그인이 필요합니다.");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return Optional.empty();
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) {
            return Optional.of("조직 정보를 확인할 수 없습니다.");
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        if (!"HEADQUARTERS".equals(level) && !"REGIONAL".equals(level) && !"MASTER_DIST".equals(level)) {
            return Optional.of("총본사·본사·총판 운영자만 사용할 수 있습니다.");
        }
        Map<String, String> perms = orgPagePermissionService.resolvePagePermissionsForUser(user);
        if (perms != null) {
            String p = perms.get(PAGE_URL);
            if (p != null) {
                String b = PagePermissionCodes.base(p);
                if (PagePermissionCodes.P_NONE.equals(b) || PagePermissionCodes.P_OBSERVER.equals(b)) {
                    return Optional.of("본사권한설정에서 이 메뉴 권한이 없습니다.");
                }
            }
        }
        return Optional.empty();
    }

    public boolean canWrite(Authentication authentication) {
        AppUser user = resolveUser(authentication);
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (accessDeniedReason(authentication).isPresent()) {
            return false;
        }
        Map<String, String> perms = orgPagePermissionService.resolvePagePermissionsForUser(user);
        if (perms == null) {
            return true;
        }
        String p = perms.get(PAGE_URL);
        if (p == null) {
            return true;
        }
        return PagePermissionCodes.canWriteLike(p);
    }

    private void assertAccess(Authentication authentication) {
        accessDeniedReason(authentication).ifPresent(msg -> {
            throw new IllegalStateException(msg);
        });
    }

    private void assertWrite(Authentication authentication) {
        assertAccess(authentication);
        if (!canWrite(authentication)) {
            throw new IllegalStateException("등록·해지 권한이 없습니다(본사권한설정).");
        }
    }

    public PageResult<Map<String, Object>> list(Authentication authentication,
                                                String searchActiveYn,
                                                String searchKeyword,
                                                String searchFromDate,
                                                String searchToDate,
                                                String searchOrderDir,
                                                int page,
                                                int size) {
        assertAccess(authentication);
        int p = Math.max(1, page);
        int s = Math.min(Math.max(size, 1), 200);
        String active = normalizeActiveYn(searchActiveYn);
        String keyword = searchKeyword != null ? searchKeyword.trim() : "";
        String panDigits = extractPanDigits(keyword);
        String fromDate = normalizeDateParam(searchFromDate);
        String toDate = normalizeDateParam(searchToDate);
        boolean asc = searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim());
        Page<HqPayCardBlacklist> pg = asc
                ? blacklistRepository.searchFilteredAsc(active, keyword, panDigits, fromDate, toDate,
                PageRequest.of(p - 1, s))
                : blacklistRepository.searchFilteredDesc(active, keyword, panDigits, fromDate, toDate,
                PageRequest.of(p - 1, s));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HqPayCardBlacklist row : pg.getContent()) {
            rows.add(toRowMap(row));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(s);
        pr.setTotalElements(pg.getTotalElements());
        pr.setTotalPages(pg.getTotalPages());
        return pr;
    }

    @Transactional
    public Map<String, Object> register(Authentication authentication, Map<String, Object> body) {
        assertWrite(authentication);
        AppUser user = resolveUser(authentication);
        String pg = body.get("pgVendor") != null ? body.get("pgVendor").toString() : "";
        String pan = body.get("pan") != null ? body.get("pan").toString() : "";
        String reason = body.get("reason") != null ? body.get("reason").toString() : "";
        String cardBrand = body.get("cardBrand") != null ? body.get("cardBrand").toString() : "";
        String holderName = body.get("holderName") != null ? body.get("holderName").toString() : "";
        String compId = body.get("compId") != null ? body.get("compId").toString().trim() : "";
        String compNm = body.get("compNm") != null ? body.get("compNm").toString().trim() : "";
        Long orgUnitId = compId.isEmpty() ? null : resolveMerchantOrgUnitId(compId);
        HqPayCardBlacklist row = payCardPolicyService.addBlacklistManual(
                pg, pan, reason, user != null ? user.getUsername() : null, cardBrand, holderName,
                orgUnitId, compId, compNm);
        return toRowMap(row);
    }

    @Transactional
    public Map<String, Object> release(Authentication authentication, Map<String, Object> body) {
        assertWrite(authentication);
        AppUser user = resolveUser(authentication);
        String totp = body.get("totpCode") != null ? body.get("totpCode").toString() : "";
        if (totp.isBlank() && body.get("otp") != null) {
            totp = body.get("otp").toString();
        }
        authService.verifyTotpOrThrow(user, totp);
        Object idObj = body.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("id가 필요합니다.");
        }
        long id = Long.parseLong(idObj.toString().trim());
        String releaseReason = body.get("releaseReason") != null ? body.get("releaseReason").toString().trim() : "";
        if (releaseReason.isEmpty() && body.get("releaseReasonText") != null) {
            releaseReason = body.get("releaseReasonText").toString().trim();
        }
        HqPayCardBlacklist row = payCardPolicyService.releaseBlacklist(
                id, user != null ? user.getUsername() : null, releaseReason);
        return toRowMap(row);
    }

    @Transactional
    public Map<String, Object> releaseBulk(Authentication authentication, Map<String, Object> body) {
        assertWrite(authentication);
        AppUser user = resolveUser(authentication);
        String totp = body.get("totpCode") != null ? body.get("totpCode").toString() : "";
        if (totp.isBlank() && body.get("otp") != null) {
            totp = body.get("otp").toString();
        }
        authService.verifyTotpOrThrow(user, totp);
        String mode = body.get("mode") != null ? body.get("mode").toString().trim().toUpperCase(Locale.ROOT) : "";
        String releaseReason = body.get("releaseReason") != null ? body.get("releaseReason").toString().trim() : "";
        if (releaseReason.isEmpty() && body.get("releaseReasonText") != null) {
            releaseReason = body.get("releaseReasonText").toString().trim();
        }
        if (releaseReason.isEmpty()) {
            throw new IllegalArgumentException("해지 사유를 입력하세요.");
        }
        List<Long> ids = resolveBulkReleaseIds(body, mode);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("해지할 등록 카드가 없습니다.");
        }
        String username = user != null ? user.getUsername() : null;
        int released = 0;
        int skipped = 0;
        for (Long id : ids) {
            if (id == null || id <= 0) {
                skipped++;
                continue;
            }
            try {
                payCardPolicyService.releaseBlacklist(id, username, releaseReason);
                released++;
            } catch (IllegalStateException e) {
                skipped++;
            }
        }
        if (released == 0) {
            throw new IllegalStateException("해지할 등록 카드가 없습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("releasedCount", released);
        result.put("skippedCount", skipped);
        result.put("requestedCount", ids.size());
        return result;
    }

    @Transactional
    public Map<String, Object> update(Authentication authentication, Map<String, Object> body) {
        assertWrite(authentication);
        AppUser user = resolveUser(authentication);
        Object idObj = body.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("id가 필요합니다.");
        }
        long id = Long.parseLong(idObj.toString().trim());
        String pg = body.get("pgVendor") != null ? body.get("pgVendor").toString() : "";
        String reason = body.get("reason") != null ? body.get("reason").toString() : "";
        String holderName = body.get("holderName") != null ? body.get("holderName").toString() : "";
        String compId = body.get("compId") != null ? body.get("compId").toString().trim() : "";
        String compNm = body.get("compNm") != null ? body.get("compNm").toString().trim() : "";
        Long orgUnitId = compId.isEmpty() ? null : resolveMerchantOrgUnitId(compId);
        HqPayCardBlacklist row = payCardPolicyService.updateInactiveCardContent(
                id, pg, holderName, reason, orgUnitId, compId, compNm,
                user != null ? user.getUsername() : null);
        return toRowMap(row);
    }

    private Map<String, Object> toRowMap(HqPayCardBlacklist row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("pgVendor", row.getPgVendor());
        m.put("panDisplay", row.getPanDisplay());
        m.put("holderName", row.getHolderName());
        m.put("matchMode", row.getMatchMode());
        m.put("source", row.getSource());
        m.put("regTypeLabel", regTypeLabel(row.getSource()));
        m.put("compId", displayCompId(row));
        m.put("compNm", displayCompNm(row));
        m.put("reason", row.getReason());
        m.put("activeYn", row.getActiveYn());
        m.put("registeredBy", row.getRegisteredBy());
        m.put("registeredAt", row.getCreatedAt() != null ? row.getCreatedAt().toString() : null);
        m.put("lastModifiedAt", row.getContentUpdatedAt() != null ? row.getContentUpdatedAt().toString() : null);
        m.put("lastModifiedBy", row.getContentUpdatedBy());
        m.put("releasedBy", row.getReleasedBy());
        m.put("releasedReason", row.getReleasedReason());
        m.put("releasedAt", row.getReleasedAt() != null ? row.getReleasedAt().toString() : null);
        return m;
    }

    private Long resolveMerchantOrgUnitId(String compId) {
        if (compId == null || compId.isBlank()) {
            return null;
        }
        OrgUnit ou = orgUnitRepository.findByCodeIgnoreCase(compId.trim())
                .orElseThrow(() -> new IllegalArgumentException("유효한 가맹점 업체코드가 아닙니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("유효한 가맹점 업체코드가 아닙니다.");
        }
        return ou.getId();
    }

    private String displayCompId(HqPayCardBlacklist row) {
        if (row.getRegisteredCompId() != null && !row.getRegisteredCompId().isBlank()) {
            return row.getRegisteredCompId().trim();
        }
        if (row.getRegisteredOrgUnitId() == null) {
            return "";
        }
        return orgUnitRepository.findById(row.getRegisteredOrgUnitId())
                .map(OrgUnit::getCode)
                .orElse("");
    }

    private String displayCompNm(HqPayCardBlacklist row) {
        if (row.getRegisteredCompNm() != null && !row.getRegisteredCompNm().isBlank()) {
            return row.getRegisteredCompNm().trim();
        }
        if (row.getRegisteredOrgUnitId() == null) {
            return "";
        }
        return orgUnitRepository.findById(row.getRegisteredOrgUnitId())
                .map(OrgUnit::getName)
                .orElse("");
    }

    private static String regTypeLabel(String source) {
        if (source != null && "AUTO".equalsIgnoreCase(source.trim())) {
            return "자동";
        }
        return "수동";
    }

    private List<Long> resolveBulkReleaseIds(Map<String, Object> body, String mode) {
        if ("SELECTED".equals(mode)) {
            Object idsObj = body.get("ids");
            List<Long> ids = new ArrayList<>();
            if (idsObj instanceof List<?> raw) {
                for (Object o : raw) {
                    if (o == null) {
                        continue;
                    }
                    try {
                        long id = Long.parseLong(o.toString().trim());
                        if (id > 0) {
                            ids.add(id);
                        }
                    } catch (NumberFormatException ignored) {
                        /* skip */
                    }
                }
            }
            return ids;
        }
        if ("ALL_FILTERED".equals(mode)) {
            String active = normalizeActiveYn(body.get("searchActiveYn") != null
                    ? body.get("searchActiveYn").toString() : "ALL");
            String keyword = body.get("searchKeyword") != null ? body.get("searchKeyword").toString().trim() : "";
            String panDigits = extractPanDigits(keyword);
            String fromDate = normalizeDateParam(body.get("searchFromDate") != null
                    ? body.get("searchFromDate").toString() : "");
            String toDate = normalizeDateParam(body.get("searchToDate") != null
                    ? body.get("searchToDate").toString() : "");
            return blacklistRepository.findActiveIdsByFilter(active, keyword, panDigits, fromDate, toDate);
        }
        throw new IllegalArgumentException("mode가 올바르지 않습니다.");
    }

    private static String normalizeActiveYn(String searchActiveYn) {
        String active = searchActiveYn != null ? searchActiveYn.trim().toUpperCase(Locale.ROOT) : "ALL";
        if (!"Y".equals(active) && !"N".equals(active)) {
            return "ALL";
        }
        return active;
    }

    private static String normalizeDateParam(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    /** 카드번호 부분 검색: 키워드에서 숫자만 추출, 3자리 이상이면 pan_display 숫자열에 포함 여부로 매칭 */
    private static String extractPanDigits(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return digits.length() >= 3 ? digits.toString() : "";
    }

    private static AppUser resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser u)) {
            return null;
        }
        return u;
    }
}
