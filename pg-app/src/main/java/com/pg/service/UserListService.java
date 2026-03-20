package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqNotifyEnvConfigRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserListService {

    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public UserListService(UserRepository userRepository, OrgUnitRepository orgUnitRepository,
                           HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyEnvConfigRepository = hqNotifyEnvConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Map<String, Object>> search(String searchUserId, String searchUserNm, String searchCompId, int page, int size) {
        Sort sort = Sort.by(
                Order.asc("permissionGroupNm").nullsLast(),
                Order.asc("username").nullsLast());
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), sort);
        String uid = (searchUserId != null && !searchUserId.isEmpty()) ? searchUserId.trim() : "";
        String nm = (searchUserNm != null && !searchUserNm.isEmpty()) ? searchUserNm.trim() : "";
        String cc = (searchCompId != null && !searchCompId.isEmpty()) ? searchCompId.trim() : "";
        Page<AppUser> result = userRepository.searchForList(uid, nm, cc, p);
        List<Map<String, Object>> list = result.getContent().stream().map(this::toRow).collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        return pr;
    }

    public PageResult<Map<String, Object>> searchScoped(String searchUserId, String searchUserNm, String searchCompId,
                                                        int page, int size, String scopeCompCode) {
        if (scopeCompCode == null || scopeCompCode.isBlank()) {
            return search(searchUserId, searchUserNm, searchCompId, page, size);
        }
        Set<String> allowed = collectSelfAndDescendantCodes(scopeCompCode.trim());
        if (allowed.isEmpty()) allowed.add(scopeCompCode.trim());
        String uid = (searchUserId != null && !searchUserId.isEmpty()) ? searchUserId.trim() : "";
        String nm = (searchUserNm != null && !searchUserNm.isEmpty()) ? searchUserNm.trim() : "";
        String cc = (searchCompId != null && !searchCompId.isEmpty()) ? searchCompId.trim() : "";
        List<Map<String, Object>> filtered = userRepository.searchForList(uid, nm, cc, Pageable.unpaged()).getContent().stream()
                .map(this::toRow)
                .filter(row -> allowed.contains(String.valueOf(row.getOrDefault("compId", ""))))
                .collect(Collectors.toList());
        int safeSize = Math.min(100, Math.max(1, size));
        int safePage = Math.max(1, page);
        int from = Math.min(filtered.size(), (safePage - 1) * safeSize);
        int to = Math.min(filtered.size(), from + safeSize);
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(filtered.subList(from, to));
        pr.setPage(safePage);
        pr.setSize(safeSize);
        pr.setTotalElements(filtered.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) filtered.size() / safeSize)));
        return pr;
    }

    private Set<String> collectSelfAndDescendantCodes(String rootCode) {
        Set<String> out = new HashSet<>();
        if (rootCode == null || rootCode.isBlank()) return out;
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, OrgUnit> byId = all.stream().collect(Collectors.toMap(OrgUnit::getId, o -> o));
        OrgUnit root = all.stream().filter(o -> rootCode.equals(o.getCode())).findFirst().orElse(null);
        if (root == null || root.getId() == null) return out;
        Set<Long> ids = new HashSet<>();
        ids.add(root.getId());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (OrgUnit o : all) {
                if (o.getParentId() != null && ids.contains(o.getParentId()) && ids.add(o.getId())) {
                    changed = true;
                }
            }
        }
        for (Long id : ids) {
            OrgUnit o = byId.get(id);
            if (o != null && o.getCode() != null && !o.getCode().isBlank()) out.add(o.getCode());
        }
        return out;
    }

    public Set<String> resolveAllowedCompCodes(String scopeCompCode) {
        if (scopeCompCode == null || scopeCompCode.isBlank()) return new HashSet<>();
        Set<String> allowed = collectSelfAndDescendantCodes(scopeCompCode.trim());
        if (allowed.isEmpty()) allowed.add(scopeCompCode.trim());
        return allowed;
    }

    public Map<String, Object> managementCapability(AppUser actor) {
        HqNotifyEnvConfig cfg = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean featureEnabled = cfg != null && "Y".equalsIgnoreCase(cfg.getManagerUserControlEnabledYn());
        boolean resetEnabled = cfg != null && "Y".equalsIgnoreCase(cfg.getManagerPasswordResetEnabledYn());
        boolean managerRole = isManagerRole(actor);
        boolean canManage = featureEnabled && managerRole;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("canManageUsers", canManage ? "Y" : "N");
        out.put("canResetPassword", (canManage && resetEnabled) ? "Y" : "N");
        return out;
    }

    public void createUserScoped(AppUser actor, Set<String> allowedCompCodes, String username, String name, String password,
                                 String compId, String role, String userType, String assistantRoleType, String parentUsername) {
        requireManage(actor, false);
        String uid = safeTrim(username);
        if (uid.isEmpty()) throw new IllegalArgumentException("사용자ID를 입력하세요.");
        if (userRepository.findByUsername(uid).isPresent()) throw new IllegalArgumentException("이미 존재하는 사용자ID입니다.");
        String pwd = safeTrim(password);
        if (pwd.length() < 8) throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        String code = safeTrim(compId);
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(code)) {
            throw new IllegalArgumentException("본인 또는 하위 조직 코드만 등록할 수 있습니다.");
        }
        AppUser u = new AppUser();
        u.setUsername(uid);
        u.setName(safeTrim(name));
        u.setPassword(passwordEncoder.encode(pwd));
        u.setOrgUnitCode(code);
        u.setRole(normalizeRole(role));
        u.setEnabled(true);
        u.setOtpRegisteredYn("N");
        u.setUserType(normalizeUserType(userType));
        u.setAssistantRoleType(normalizeAssistantRole(assistantRoleType));
        u.setParentUsername(safeTrim(parentUsername));
        u.setPermissionGroupNm(permissionGroupByAssistantRole(u.getAssistantRoleType()));
        userRepository.save(u);
    }

    public void deleteUserScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId) {
        requireManage(actor, false);
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 사용자를 찾을 수 없습니다."));
        if (actor.getUsername() != null && actor.getUsername().equalsIgnoreCase(target.getUsername())) {
            throw new IllegalArgumentException("본인 계정은 삭제할 수 없습니다.");
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole())) {
            throw new IllegalArgumentException("ADMIN 계정은 삭제할 수 없습니다.");
        }
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("본인 또는 하위 조직 사용자만 삭제할 수 있습니다.");
        }
        userRepository.delete(target);
    }

    public Map<String, Object> resetPasswordScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId) {
        requireManage(actor, true);
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("비밀번호를 초기화할 사용자를 찾을 수 없습니다."));
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("본인 또는 하위 조직 사용자만 초기화할 수 있습니다.");
        }
        String tempPassword = generateTempPassword();
        target.setPassword(passwordEncoder.encode(tempPassword));
        target.setOtpRegisteredYn("N");
        userRepository.save(target);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", target.getUsername());
        out.put("tempPassword", tempPassword);
        return out;
    }

    private void requireManage(AppUser actor, boolean needReset) {
        Map<String, Object> cap = managementCapability(actor);
        boolean canManage = "Y".equals(String.valueOf(cap.get("canManageUsers")));
        boolean canReset = "Y".equals(String.valueOf(cap.get("canResetPassword")));
        if (!canManage) throw new IllegalArgumentException("총본사 환경설정에서 사용자관리 권한이 비활성화되어 있습니다.");
        if (needReset && !canReset) throw new IllegalArgumentException("총본사 환경설정에서 비밀번호 초기화가 비활성화되어 있습니다.");
    }

    private boolean isManagerRole(AppUser actor) {
        if (actor == null) return false;
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) return true;
        return "MANAGER".equalsIgnoreCase(safeTrim(actor.getAssistantRoleType()));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String normalizeRole(String role) {
        String r = safeTrim(role).toUpperCase(Locale.ROOT);
        if (r.isEmpty()) return "USER";
        if ("ADMIN".equals(r)) return "ADMIN";
        return "USER";
    }

    private String normalizeUserType(String userType) {
        String t = safeTrim(userType).toUpperCase(Locale.ROOT);
        if ("ASSISTANT".equals(t)) return "ASSISTANT";
        return "REPRESENTATIVE";
    }

    private String normalizeAssistantRole(String assistantRoleType) {
        String t = safeTrim(assistantRoleType).toUpperCase(Locale.ROOT);
        if ("OPERATOR".equals(t) || "SETTLEMENT".equals(t) || "TECH".equals(t) || "MANAGER".equals(t)) return t;
        return "MANAGER";
    }

    private String permissionGroupByAssistantRole(String role) {
        if (role == null || role.isBlank()) return "대표";
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "MANAGER" -> "관리담당";
            case "OPERATOR" -> "운영담당";
            case "SETTLEMENT" -> "정산담당";
            case "TECH" -> "기술담당";
            default -> "대표";
        };
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private Map<String, Object> toRow(AppUser u) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", u.getId());
        row.put("userId", u.getUsername());
        row.put("userNm", u.getName());
        String ouCode = u.getOrgUnitCode() != null && !u.getOrgUnitCode().isBlank() ? u.getOrgUnitCode().trim() : "";
        row.put("compId", ouCode.isEmpty() ? "-" : ouCode);
        String compNm = "-";
        if (!ouCode.isEmpty()) {
            compNm = orgUnitRepository.findByCode(ouCode).map(OrgUnit::getName).orElse(ouCode);
        }
        row.put("compNm", compNm);
        row.put("roleNm", u.getRole() != null ? u.getRole() : "USER");
        row.put("permissionGroupNm", u.getPermissionGroupNm() != null ? u.getPermissionGroupNm() : "");
        row.put("otpRegisteredYn", "Y".equalsIgnoreCase(u.getOtpRegisteredYn()) ? "Y" : "N");
        row.put("useYn", u.isEnabled() ? "Y" : "N");
        return row;
    }
}
