package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.OrgViewColumnAllowance;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.OrgViewColumnAllowanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrgViewColumnAllowanceService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgViewColumnAllowanceRepository allowanceRepository;

    public OrgViewColumnAllowanceService(AuthService authService,
                                         OrgUnitRepository orgUnitRepository,
                                         OrgViewColumnAllowanceRepository allowanceRepository) {
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.allowanceRepository = allowanceRepository;
    }

    public boolean canManageOrgViewAllowance(AppUser user) {
        if (user == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return true;
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .map(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .orElse(false);
    }

    /**
     * 로그인 사용자의 "본사(REGIONAL) 스코프"에 대해 컬럼 허용 정책이 있으면 허용 키 목록을 반환.
     * empty() = 정책 없음(제한 없음). of(empty list) = 정책은 있으나 선택 가능한 선택 컬럼 없음.
     */
    public Optional<List<String>> getRestrictedAllowedKeys(String loginId, String pageUrl) {
        Optional<String> regional = resolveRegionalAncestorOrgCode(loginId);
        if (regional.isEmpty()) return Optional.empty();
        String p = safe(pageUrl);
        Optional<OrgViewColumnAllowance> row = allowanceRepository.findByRegionalOrgCodeAndPageUrl(regional.get(), p);
        if (row.isEmpty()) return Optional.empty();
        return Optional.of(parseJsonArray(row.get().getAllowedKeysJson()));
    }

    /**
     * 소속 조직에서 위로 올라가며 첫 REGIONAL 의 업체코드. HEADQUARTERS 소속이면 empty (총본사 직원은 트리 제한 없음).
     */
    public Optional<String> resolveRegionalAncestorOrgCode(String loginId) {
        Optional<OrgUnit> ouOpt = authService.resolveOrgUnitForLoginId(loginId);
        if (ouOpt.isEmpty()) return Optional.empty();
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() == OrgLevel.HEADQUARTERS) return Optional.empty();
        OrgUnit walk = ou;
        while (walk != null) {
            if (walk.getOrgLevel() == OrgLevel.REGIONAL) {
                return Optional.of(walk.getCode());
            }
            Long pid = walk.getParentId();
            if (pid == null) break;
            walk = orgUnitRepository.findById(pid).orElse(null);
        }
        return Optional.empty();
    }

    public Map<String, Object> getAllowanceRow(String regionalOrgCode, String pageUrl) {
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("regionalOrgCode", r);
        m.put("pageUrl", p);
        var opt = allowanceRepository.findByRegionalOrgCodeAndPageUrl(r, p);
        m.put("hasPolicy", opt.isPresent());
        m.put("allowedKeysJson", opt.map(OrgViewColumnAllowance::getAllowedKeysJson).orElse("[]"));
        opt.ifPresent(row -> m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null));
        return m;
    }

    @Transactional
    public Map<String, Object> saveAllowance(String regionalOrgCode, String pageUrl, String allowedKeysJson, AppUser actor) {
        if (!canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("총본사(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.");
        }
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        if (r.isEmpty() || p.isEmpty()) {
            throw new IllegalArgumentException("본사 업체코드와 화면 경로(pageUrl)가 필요합니다.");
        }
        OrgUnit target = orgUnitRepository.findByCode(r)
                .orElseThrow(() -> new IllegalArgumentException("본사 업체코드를 찾을 수 없습니다: " + r));
        if (target.getOrgLevel() != OrgLevel.REGIONAL) {
            throw new IllegalArgumentException("대상은 본사(REGIONAL) 업체만 지정할 수 있습니다.");
        }
        List<String> keys = parseJsonArray(allowedKeysJson);
        OrgViewColumnAllowance row = allowanceRepository.findByRegionalOrgCodeAndPageUrl(r, p).orElseGet(OrgViewColumnAllowance::new);
        row.setRegionalOrgCode(r);
        row.setPageUrl(p);
        row.setAllowedKeysJson(writeJsonArray(keys));
        allowanceRepository.save(row);
        return getAllowanceRow(r, p);
    }

    @Transactional
    public Map<String, Object> deleteAllowance(String regionalOrgCode, String pageUrl, AppUser actor) {
        if (!canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("총본사(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.");
        }
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        allowanceRepository.deleteByRegionalOrgCodeAndPageUrl(r, p);
        return getAllowanceRow(r, p);
    }

    public List<Map<String, String>> listRegionalBranches() {
        return orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.REGIONAL).stream()
                .map(ou -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("code", ou.getCode());
                    m.put("name", ou.getName() != null ? ou.getName() : ou.getCode());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<String> list = MAPPER.readValue(json.trim(), new TypeReference<List<String>>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String writeJsonArray(List<String> keys) {
        try {
            return MAPPER.writeValueAsString(keys != null ? keys : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }
}
