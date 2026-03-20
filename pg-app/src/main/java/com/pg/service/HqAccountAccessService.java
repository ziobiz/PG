package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.entity.UserCompAccess;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserCompAccessRepository;
import com.pg.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HqAccountAccessService {

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

    public Map<String, Object> listAll() {
        List<UserCompAccess> rows = userCompAccessRepository.findAllByOrderByUsernameAscCompCodeAsc();
        List<Map<String, Object>> list = new ArrayList<>();
        int n = 1;
        for (UserCompAccess r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rowNo", n++);
            m.put("id", r.getId());
            m.put("username", r.getUsername());
            m.put("compCode", r.getCompCode());
            m.put("regDt", r.getCreatedAt() != null ? r.getCreatedAt().toString().replace('T', ' ') : "");
            list.add(m);
        }
        List<Map<String, String>> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(u -> {
                    Map<String, String> x = new LinkedHashMap<>();
                    x.put("username", u.getUsername());
                    x.put("name", u.getName() != null ? u.getName() : "");
                    return x;
                })
                .collect(Collectors.toList());
        List<Map<String, String>> comps = orgUnitRepository.findAll().stream()
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
        if (userRepository.findByUsername(u).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자ID입니다: " + u);
        }
        if (orgUnitRepository.findByCode(c).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 업체코드입니다: " + c);
        }
        List<UserCompAccess> existing = userCompAccessRepository.findAllByOrderByUsernameAscCompCodeAsc();
        for (UserCompAccess e : existing) {
            if (u.equalsIgnoreCase(e.getUsername()) && c.equalsIgnoreCase(e.getCompCode())) {
                throw new IllegalArgumentException("이미 동일한 접근 규칙이 있습니다.");
            }
        }
        UserCompAccess row = new UserCompAccess();
        row.setUsername(u);
        row.setCompCode(c);
        userCompAccessRepository.save(row);
    }

    @Transactional
    public void delete(long id) {
        userCompAccessRepository.deleteById(id);
    }
}
