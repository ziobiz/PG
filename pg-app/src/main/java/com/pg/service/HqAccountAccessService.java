package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.UserCompAccess;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserCompAccessRepository;
import com.pg.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 총본사·본사·총판 소속 로그인 ID에 대해, 사용자관리 등에서 접근 가능한 업체 코드를 한 건씩 명시한다.
 * 허용 코드는 조직 단계와 무관하게 지정할 수 있으며, 적용 시에는 해당 코드와만 교집합되어(하위 조직 자동 확장 없음) 그 업체만 보이게 제한된다.
 */
@Service
public class HqAccountAccessService {

    /** 업체접근설정에 노출·등록 가능한 조직 단계 */
    private static final EnumSet<OrgLevel> TOP_ACCESS_ORG_LEVELS = EnumSet.of(
            OrgLevel.HEADQUARTERS, OrgLevel.REGIONAL, OrgLevel.MASTER_DIST);

    private final UserCompAccessRepository userCompAccessRepository;
    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;

    public HqAccountAccessService(UserCompAccessRepository userCompAccessRepository,
                                  UserRepository userRepository,
                                  OrgUnitRepository orgUnitRepository) {
        this.userCompAccessRepository = userCompAccessRepository;
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    private Map<String, OrgLevel> codeToOrgLevelLower() {
        Map<String, OrgLevel> m = new HashMap<>();
        for (OrgUnit o : orgUnitRepository.findAll()) {
            if (o.getCode() == null || o.getCode().isBlank() || o.getOrgLevel() == null) {
                continue;
            }
            m.put(o.getCode().trim().toLowerCase(Locale.ROOT), o.getOrgLevel());
        }
        return m;
    }

    private static boolean isActiveOrg(OrgUnit o) {
        String s = o.getStatus();
        return s == null || s.isBlank() || "ACTIVE".equalsIgnoreCase(s);
    }

    private boolean isTopTierOrgCode(String code, Map<String, OrgLevel> codeToLevel) {
        if (code == null || code.isBlank() || codeToLevel == null) {
            return false;
        }
        OrgLevel lv = codeToLevel.get(code.trim().toLowerCase(Locale.ROOT));
        return lv != null && TOP_ACCESS_ORG_LEVELS.contains(lv);
    }

    public Map<String, Object> listAll() {
        Map<String, OrgLevel> codeToLevel = codeToOrgLevelLower();
        List<OrgUnit> allOrgs = orgUnitRepository.findAll();
        Map<String, String> codeToNameLower = new HashMap<>();
        for (OrgUnit o : allOrgs) {
            if (o.getCode() == null || o.getCode().isBlank()) {
                continue;
            }
            String nm = o.getName() != null ? o.getName() : "";
            codeToNameLower.put(o.getCode().trim().toLowerCase(Locale.ROOT), nm);
        }

        Map<String, AppUser> userByLower = new HashMap<>();
        for (AppUser u : userRepository.findAll()) {
            if (u.getUsername() != null && !u.getUsername().isBlank()) {
                userByLower.put(u.getUsername().trim().toLowerCase(Locale.ROOT), u);
            }
        }

        List<UserCompAccess> rows = userCompAccessRepository.findAllByOrderByUsernameAscCompCodeAsc();
        List<Map<String, Object>> list = new ArrayList<>();
        int n = 1;
        for (UserCompAccess r : rows) {
            if (r.getUsername() == null || r.getCompCode() == null) {
                continue;
            }
            AppUser au = userByLower.get(r.getUsername().trim().toLowerCase(Locale.ROOT));
            if (au == null || !isTopTierOrgCode(au.getOrgUnitCode(), codeToLevel)) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rowNo", n++);
            m.put("id", r.getId());
            m.put("username", r.getUsername());
            m.put("compCode", r.getCompCode());
            m.put("compNm", codeToNameLower.getOrDefault(r.getCompCode().trim().toLowerCase(Locale.ROOT), ""));
            m.put("regDt", r.getCreatedAt() != null ? r.getCreatedAt().toString().replace('T', ' ') : "");
            list.add(m);
        }
        List<Map<String, String>> users = userRepository.findAll().stream()
                .filter(u -> isTopTierOrgCode(u.getOrgUnitCode(), codeToLevel))
                .sorted(Comparator.comparing(AppUser::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(u -> {
                    Map<String, String> x = new LinkedHashMap<>();
                    x.put("username", u.getUsername());
                    x.put("name", u.getName() != null ? u.getName() : "");
                    x.put("orgUnitCode", u.getOrgUnitCode() != null ? u.getOrgUnitCode() : "");
                    return x;
                })
                .collect(Collectors.toList());
        List<Map<String, String>> comps = allOrgs.stream()
                .filter(HqAccountAccessService::isActiveOrg)
                .sorted(Comparator.comparing(OrgUnit::getCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(o -> {
                    Map<String, String> x = new LinkedHashMap<>();
                    x.put("code", o.getCode());
                    x.put("name", o.getName() != null ? o.getName() : "");
                    return x;
                })
                .collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("users", users);
        out.put("comps", comps);
        return out;
    }

    private void validateUserAndTargetOrg(String u, String c) {
        AppUser appUser = userRepository.findByUsername(u)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자ID입니다: " + u));

        String home = appUser.getOrgUnitCode();
        if (home == null || home.isBlank()) {
            throw new IllegalArgumentException("사용자의 소속 업체코드가 없습니다. 사용자관리에서 소속을 지정한 뒤 등록하세요.");
        }
        OrgUnit homeOrg = orgUnitRepository.findByCodeIgnoreCase(home.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자 소속 조직을 찾을 수 없습니다: " + home));
        if (!TOP_ACCESS_ORG_LEVELS.contains(homeOrg.getOrgLevel())) {
            throw new IllegalArgumentException("업체접근설정은 총본사·본사·총판 소속 계정만 등록할 수 있습니다.");
        }

        OrgUnit targetOrg = orgUnitRepository.findByCodeIgnoreCase(c)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 업체코드입니다: " + c));
        if (!isActiveOrg(targetOrg)) {
            throw new IllegalArgumentException("미사용 등 접근할 수 없는 업체코드입니다: " + c);
        }
    }

    private void assertNoDuplicate(Long excludeId, String u, String c) {
        for (UserCompAccess e : userCompAccessRepository.findAllByOrderByUsernameAscCompCodeAsc()) {
            if (excludeId != null && e.getId() != null && excludeId.longValue() == e.getId().longValue()) {
                continue;
            }
            if (u.equalsIgnoreCase(e.getUsername()) && c.equalsIgnoreCase(e.getCompCode())) {
                throw new IllegalArgumentException("이미 동일한 접근 규칙이 있습니다.");
            }
        }
    }

    @Transactional
    public void add(String username, String compCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("사용자ID를 입력하세요.");
        }
        if (compCode == null || compCode.isBlank()) {
            throw new IllegalArgumentException("업체코드를 입력하세요.");
        }
        String u = username.trim();
        String c = compCode.trim();
        validateUserAndTargetOrg(u, c);
        assertNoDuplicate(null, u, c);
        UserCompAccess row = new UserCompAccess();
        row.setUsername(u);
        row.setCompCode(c);
        userCompAccessRepository.save(row);
    }

    @Transactional
    public void update(long id, String username, String compCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("사용자ID를 입력하세요.");
        }
        if (compCode == null || compCode.isBlank()) {
            throw new IllegalArgumentException("업체코드를 입력하세요.");
        }
        String u = username.trim();
        String c = compCode.trim();
        UserCompAccess row = userCompAccessRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 규칙을 찾을 수 없습니다."));
        validateUserAndTargetOrg(u, c);
        assertNoDuplicate(id, u, c);
        row.setUsername(u);
        row.setCompCode(c);
        userCompAccessRepository.save(row);
    }

    @Transactional
    public void delete(long id) {
        userCompAccessRepository.deleteById(id);
    }
}
