package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.OrgUnit;
import com.pg.entity.UserCompAccess;
import com.pg.repository.HqNotifyEnvConfigRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserCompAccessRepository;
import com.pg.repository.UserRepository;
import com.pg.util.ChatbotMerchantAdminConstants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserListService {

    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository;
    private final UserCompAccessRepository userCompAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public UserListService(UserRepository userRepository, OrgUnitRepository orgUnitRepository,
                           HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository,
                           UserCompAccessRepository userCompAccessRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyEnvConfigRepository = hqNotifyEnvConfigRepository;
        this.userCompAccessRepository = userCompAccessRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Map<String, Object>> search(String searchUserId, String searchUserNm, String searchCompId,
                                                    String searchUseStatus, int page, int size) {
        Sort sort = Sort.by(
                Order.asc("permissionGroupNm").nullsLast(),
                Order.asc("username").nullsLast());
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(1000, Math.max(1, size)), sort);
        String uid = (searchUserId != null && !searchUserId.isEmpty()) ? searchUserId.trim() : "";
        String nm = (searchUserNm != null && !searchUserNm.isEmpty()) ? searchUserNm.trim() : "";
        String cc = (searchCompId != null && !searchCompId.isEmpty()) ? searchCompId.trim() : "";
        String st = normalizeSearchUseStatus(searchUseStatus);
        Page<AppUser> result = userRepository.searchForList(uid, nm, cc, st, p);
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
                                                        String searchUseStatus, int page, int size, String scopeCompCode,
                                                        String accessUsername, boolean actorIsAdmin) {
        if (scopeCompCode == null || scopeCompCode.isBlank()) {
            if (actorIsAdmin) {
                return search(searchUserId, searchUserNm, searchCompId, searchUseStatus, page, size);
            }
            // 상위·타 조직 사용자는 볼 수 없음: 소속 업체코드가 없으면 목록 비움(ADMIN 만 전체)
            return emptyUserListPage(page, size);
        }
        Set<String> allowed0 = collectSelfAndDescendantCodes(scopeCompCode.trim());
        if (allowed0.isEmpty()) {
            allowed0.add(scopeCompCode.trim());
        }
        final Set<String> allowed = intersectWithAccountCompanyAccess(allowed0, accessUsername, actorIsAdmin);
        String uid = (searchUserId != null && !searchUserId.isEmpty()) ? searchUserId.trim() : "";
        String nm = (searchUserNm != null && !searchUserNm.isEmpty()) ? searchUserNm.trim() : "";
        String cc = (searchCompId != null && !searchCompId.isEmpty()) ? searchCompId.trim() : "";
        String st = normalizeSearchUseStatus(searchUseStatus);
        List<Map<String, Object>> filtered = userRepository.searchForList(uid, nm, cc, st, Pageable.unpaged()).getContent().stream()
                .map(this::toRow)
                .filter(row -> allowed.contains(String.valueOf(row.getOrDefault("compId", ""))))
                .collect(Collectors.toList());
        int safeSize = Math.min(1000, Math.max(1, size));
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

    private static PageResult<Map<String, Object>> emptyUserListPage(int page, int size) {
        int safeSize = Math.min(1000, Math.max(1, size));
        int safePage = Math.max(1, page);
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(Collections.emptyList());
        pr.setPage(safePage);
        pr.setSize(safeSize);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }

    private String normalizeSearchUseStatus(String searchUseStatus) {
        if (searchUseStatus == null || searchUseStatus.isBlank()) return "";
        String t = searchUseStatus.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(t) || "INACTIVE".equals(t) || "SUSPENDED".equals(t)) return t;
        return "";
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

    /**
     * [계정·업체접근]에 행이 있으면, (로그인자 기준) 하위 조직 허용 집합과
     * 지정한 업체코드(정확히 일치)의 교집합만 사용자관리에 노출합니다. 지정 코드의 하위 조직은 자동으로 붙지 않습니다.
     */
    public Set<String> resolveAllowedCompCodesWithAccess(String scopeCompCode, String accessUsername, boolean actorIsAdmin) {
        Set<String> allowed = resolveAllowedCompCodes(scopeCompCode);
        return intersectWithAccountCompanyAccess(allowed, accessUsername, actorIsAdmin);
    }

    /** {@code allowed} 내 코드 중, 계정에 명시된 업체코드와 정확히 일치하는 것만 남깁니다(하위 확장 없음). */
    private Set<String> intersectWithAccountCompanyAccess(Set<String> allowed, String accessUsername, boolean actorIsAdmin) {
        if (actorIsAdmin || accessUsername == null || accessUsername.isBlank() || allowed.isEmpty()) {
            return allowed;
        }
        List<UserCompAccess> rows = userCompAccessRepository.findByUsernameIgnoreCaseOrderByCompCodeAsc(accessUsername.trim());
        if (rows.isEmpty()) {
            return allowed;
        }
        Set<String> whitelist = new HashSet<>();
        for (UserCompAccess r : rows) {
            if (r.getCompCode() != null && !r.getCompCode().isBlank()) {
                whitelist.add(r.getCompCode().trim());
            }
        }
        if (whitelist.isEmpty()) {
            return allowed;
        }
        Set<String> out = new HashSet<>();
        for (String c : allowed) {
            if (c == null) {
                continue;
            }
            String ct = c.trim();
            boolean ok = whitelist.stream().anyMatch(w -> w.equalsIgnoreCase(ct));
            if (ok) {
                out.add(ct);
            }
        }
        return out;
    }

    public Map<String, Object> managementCapability(AppUser actor) {
        HqNotifyEnvConfig cfg = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean featureEnabled = cfg != null && "Y".equalsIgnoreCase(cfg.getManagerUserControlEnabledYn());
        boolean resetEnabled = cfg != null && "Y".equalsIgnoreCase(cfg.getManagerPasswordResetEnabledYn());
        boolean managerRole = isManagerRole(actor);
        boolean canManage = featureEnabled && managerRole;
        boolean resetActor = isStrictManagerForPasswordOtpReset(actor);
        String canReset;
        if ("ADMIN".equalsIgnoreCase(safeTrim(actor.getRole()))) {
            canReset = "Y";
        } else {
            canReset = (resetActor && resetEnabled) ? "Y" : "N";
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("canManageUsers", canManage ? "Y" : "N");
        out.put("canResetPassword", canReset);
        return out;
    }

    /**
     * 신규 사용자는 로그인한 사용자와 동일한 조직(업체코드)에만 등록 가능. 하위 가맹점 전용 계정은 이 화면에서 생성할 수 없음.
     * (ADMIN 등 scopeCompCode 가 비어 있으면 기존처럼 본문 compId 검증은 저장소 정책에 따름)
     */
    public void createUserScoped(AppUser actor, Set<String> allowedCompCodes, String scopeCompCode,
                                 String username, String name, String mobile,
                                 String compId, String role, String userType, String assistantRoleType, String parentUsername) {
        requireManage(actor);
        String uid = safeTrim(username);
        if (uid.isEmpty()) throw new IllegalArgumentException("사용자ID를 입력하세요.");
        if (userRepository.findByUsername(uid).isPresent()) throw new IllegalArgumentException("이미 존재하는 사용자ID입니다.");
        /* 신규: 클라이언트에서 비밀번호를 받지 않음. 항상 로그인ID + "1!" 로 초기화(비밀번호 초기화와 동일 규칙). 첫 로그인 시 변경. */
        String pwd = uid + "1!";
        String code = safeTrim(compId);
        if (scopeCompCode != null && !scopeCompCode.isBlank()) {
            if (!code.equalsIgnoreCase(scopeCompCode.trim())) {
                throw new IllegalArgumentException("본인 조직의 사용자만 등록할 수 있습니다. 하위 가맹점 사용자는 이 화면에서 등록할 수 없습니다.");
            }
        } else if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(code)) {
            throw new IllegalArgumentException("유효한 업체코드가 아닙니다.");
        }
        AppUser u = new AppUser();
        u.setUsername(uid);
        u.setName(safeTrim(name));
        u.setPassword(passwordEncoder.encode(pwd));
        u.setPasswordMustChangeYn("Y");
        u.setOrgUnitCode(code);
        u.setMobile(safeTrim(mobile));
        u.setRole(normalizeRole(role));
        applyUserStatus(u, "ACTIVE");
        u.setOtpRegisteredYn("N");
        u.setUserType(normalizeUserType(userType));
        u.setAssistantRoleType(normalizeAssistantRole(assistantRoleType));
        u.setParentUsername(safeTrim(parentUsername));
        u.setPermissionGroupNm(permissionGroupByAssistantRole(u.getAssistantRoleType()));
        userRepository.save(u);
    }

    /** 저장된 사용자 삭제는 시스템 ADMIN만 가능. 일반 운영자는 미사용 처리로만 변경. */
    public void deleteUserScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId) {
        if (actor == null || !"ADMIN".equalsIgnoreCase(safeTrim(actor.getRole()))) {
            throw new IllegalArgumentException("저장된 사용자는 삭제할 수 없습니다. 미사용·영구정지 처리로 변경하세요.");
        }
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 사용자를 찾을 수 없습니다."));
        if (actor.getUsername() != null && actor.getUsername().equalsIgnoreCase(target.getUsername())) {
            throw new IllegalArgumentException("본인 계정은 삭제할 수 없습니다.");
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole())) {
            throw new IllegalArgumentException("ADMIN 계정은 삭제할 수 없습니다.");
        }
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("권한 범위 내 사용자만 삭제할 수 있습니다.");
        }
        userRepository.delete(target);
    }

    public void updateUserScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId,
                                 String mobile, String userStatus, String inactiveReason, String assistantRoleType) {
        requireManage(actor);
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 사용자를 찾을 수 없습니다."));
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("권한 범위 내 사용자만 수정할 수 있습니다.");
        }
        target.setMobile(safeTrim(mobile));
        String newStatus = safeTrim(userStatus).toUpperCase(Locale.ROOT);
        if (newStatus.isEmpty()) {
            newStatus = "ACTIVE";
        }
        String ir = safeTrim(inactiveReason);
        if ("INACTIVE".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            if (ir.isEmpty()) {
                throw new IllegalArgumentException("미사용 또는 영구정지로 변경할 때는 전환사유를 입력하세요.");
            }
            target.setInactiveReason(ir);
        } else {
            target.setInactiveReason(null);
        }
        applyUserStatus(target, newStatus);
        /* 대표(REPRESENTATIVE)는 권한그룹을 담당유형으로 덮어쓰지 않음(가맹 챗봇관리자·업체 대표 등 유지) */
        if ("ASSISTANT".equalsIgnoreCase(safeTrim(target.getUserType()))) {
            target.setAssistantRoleType(normalizeAssistantRole(assistantRoleType));
            target.setPermissionGroupNm(permissionGroupByAssistantRole(target.getAssistantRoleType()));
        }
        userRepository.save(target);
    }

    public Map<String, Object> resetPasswordScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId) {
        requireResetPrivilege(actor);
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("비밀번호를 초기화할 사용자를 찾을 수 없습니다."));
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("본인·동일 조직·하위 조직 사용자만 초기화할 수 있습니다. 상위 조직 사용자는 대상이 아닙니다.");
        }
        String uid = target.getUsername() != null ? target.getUsername().trim() : "";
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("사용자 아이디가 없어 초기화할 수 없습니다.");
        }
        String tempPassword = uid + "1!";
        target.setPassword(passwordEncoder.encode(tempPassword));
        target.setPasswordMustChangeYn("Y");
        target.setOtpRegisteredYn("N");
        target.setOtpSecret(null);
        target.setOtpPendingSecret(null);
        target.setOtpSetupCodeHash(null);
        target.setOtpSetupExpiresAt(null);
        userRepository.save(target);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", target.getUsername());
        out.put("tempPassword", tempPassword);
        return out;
    }

    public void resetOtpScoped(AppUser actor, Set<String> allowedCompCodes, Long targetId) {
        requireResetPrivilege(actor);
        AppUser target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("OTP를 초기화할 사용자를 찾을 수 없습니다."));
        if (!allowedCompCodes.isEmpty() && !allowedCompCodes.contains(safeTrim(target.getOrgUnitCode()))) {
            throw new IllegalArgumentException("본인·동일 조직·하위 조직 사용자만 초기화할 수 있습니다. 상위 조직 사용자는 대상이 아닙니다.");
        }
        target.setOtpRegisteredYn("N");
        target.setOtpSecret(null);
        target.setOtpPendingSecret(null);
        target.setOtpSetupCodeHash(null);
        target.setOtpSetupExpiresAt(null);
        userRepository.save(target);
    }

    private void applyUserStatus(AppUser u, String status) {
        String s = safeTrim(status).toUpperCase(Locale.ROOT);
        if (s.isEmpty()) s = "ACTIVE";
        switch (s) {
            case "ACTIVE" -> {
                u.setUserStatus("ACTIVE");
                u.setEnabled(true);
            }
            case "INACTIVE" -> {
                u.setUserStatus("INACTIVE");
                u.setEnabled(false);
            }
            case "SUSPENDED" -> {
                u.setUserStatus("SUSPENDED");
                u.setEnabled(false);
            }
            default -> throw new IllegalArgumentException("알 수 없는 사용여부입니다.");
        }
    }

    private void requireManage(AppUser actor) {
        HqNotifyEnvConfig cfg = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean featureEnabled = cfg != null && "Y".equalsIgnoreCase(cfg.getManagerUserControlEnabledYn());
        if (!featureEnabled) {
            throw new IllegalArgumentException(
                    "총본사 환경설정에서 「관리담당 사용자관리 권한」이 꺼져 있습니다. 본사설정 → 사용자설정에서 사용으로 저장한 뒤 다시 시도하세요.");
        }
        if (!isManagerRole(actor)) {
            throw new IllegalArgumentException(
                    "현재 로그인 계정으로는 사용자를 추가·수정할 수 없습니다. 시스템 관리자(ADMIN), 조직 대표(대표) 계정, 또는 담당자(ASSISTANT) 중 관리담당(MANAGER)만 가능합니다.");
        }
    }

    /**
     * 비밀번호·OTP 초기화: 시스템 ADMIN 또는 보조아이디 역할이 {@code MANAGER}(관리담당)인 사용자만.
     * (운영/정산/기술 담당 등 다른 assistant_role_type 은 초기화 불가)
     */
    private void requireResetPrivilege(AppUser actor) {
        if (!isStrictManagerForPasswordOtpReset(actor)) {
            throw new IllegalArgumentException("비밀번호·OTP 초기화는 관리담당(MANAGER) 권한이 있는 사용자만 가능합니다.");
        }
        if ("ADMIN".equalsIgnoreCase(safeTrim(actor.getRole()))) {
            return;
        }
        HqNotifyEnvConfig cfg = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        if (cfg == null || !"Y".equalsIgnoreCase(cfg.getManagerPasswordResetEnabledYn())) {
            throw new IllegalArgumentException("총본사 환경설정에서 비밀번호 초기화가 비활성화되어 있습니다.");
        }
    }

    private boolean isStrictManagerForPasswordOtpReset(AppUser actor) {
        if (actor == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(safeTrim(actor.getRole()))) {
            return true;
        }
        return "MANAGER".equalsIgnoreCase(safeTrim(actor.getAssistantRoleType()));
    }

    /**
     * 사용자관리(추가·수정) 가능 주체: ADMIN, 담당자(ASSISTANT) 중 MANAGER,
     * 또는 조직 대표({@code REPRESENTATIVE}). {@code userType}이 비어 있으면 레거시 호환으로 대표로 간주합니다.
     */
    private boolean isManagerRole(AppUser actor) {
        if (actor == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(safeTrim(actor.getRole()))) {
            return true;
        }
        if ("MANAGER".equalsIgnoreCase(safeTrim(actor.getAssistantRoleType()))) {
            return true;
        }
        String ut = safeTrim(actor.getUserType());
        if ("ASSISTANT".equalsIgnoreCase(ut)) {
            return false;
        }
        return ut.isEmpty() || "REPRESENTATIVE".equalsIgnoreCase(ut);
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
        if ("OPERATOR".equals(t) || "SETTLEMENT".equals(t) || "TECH".equals(t)
                || "MANAGER".equals(t) || ChatbotMerchantAdminConstants.ASSISTANT_ROLE_TYPE.equals(t)) {
            return t;
        }
        return "MANAGER";
    }

    private String permissionGroupByAssistantRole(String role) {
        if (role == null || role.isBlank()) return "대표";
        if (ChatbotMerchantAdminConstants.ASSISTANT_ROLE_TYPE.equalsIgnoreCase(role.trim())) {
            return ChatbotMerchantAdminConstants.PERMISSION_GROUP_NM;
        }
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
        row.put("mobile", u.getMobile() != null ? u.getMobile() : "");
        String ust = u.getUserStatus() != null && !u.getUserStatus().isBlank() ? u.getUserStatus().trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if (!"ACTIVE".equals(ust) && !"INACTIVE".equals(ust) && !"SUSPENDED".equals(ust)) ust = u.isEnabled() ? "ACTIVE" : "INACTIVE";
        row.put("userStatus", ust);
        row.put("inactiveReason", u.getInactiveReason() != null ? u.getInactiveReason() : "");
        row.put("roleNm", u.getRole() != null ? u.getRole() : "USER");
        String ut = u.getUserType() != null && !u.getUserType().isBlank() ? u.getUserType().trim() : "REPRESENTATIVE";
        row.put("userType", ut);
        row.put("permissionGroupNm", u.getPermissionGroupNm() != null ? u.getPermissionGroupNm() : "");
        String art = u.getAssistantRoleType();
        if (art == null || art.isBlank()) {
            art = "REPRESENTATIVE".equalsIgnoreCase(ut) ? "" : "MANAGER";
        }
        row.put("assistantRoleType", art);
        row.put("otpRegisteredYn", "Y".equalsIgnoreCase(u.getOtpRegisteredYn()) ? "Y" : "N");
        row.put("passwordMustChangeYn", "Y".equalsIgnoreCase(u.getPasswordMustChangeYn()) ? "Y" : "N");
        row.put("useYn", u.isEnabled() ? "Y" : "N");
        return row;
    }
}
