package com.pg.service;

import com.pg.api.dto.LoginResponse;
import com.pg.entity.AppUser;
import com.pg.entity.AuthToken;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.AuthTokenRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int TOKEN_VALID_HOURS = 8;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgPagePermissionService orgPagePermissionService;

    public AuthService(UserRepository userRepository, AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder, MerchantProfileRepository merchantProfileRepository,
                       OrgUnitRepository orgUnitRepository,
                       @Lazy OrgPagePermissionService orgPagePermissionService) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgPagePermissionService = orgPagePermissionService;
    }

    @Transactional
    public Optional<LoginResponse> login(String username, String password) {
        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();
        AppUser user = userOpt.get();
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPassword()))
            return Optional.empty();
        String ust = user.getUserStatus();
        if (ust != null && !ust.isBlank() && !"ACTIVE".equalsIgnoreCase(ust.trim()))
            return Optional.empty();
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthToken at = new AuthToken();
        at.setToken(token);
        at.setUserId(user.getId());
        at.setExpiresAt(Instant.now().plusSeconds(TOKEN_VALID_HOURS * 3600L));
        authTokenRepository.save(at);
        LoginResponse res = new LoginResponse();
        res.setToken(token);
        res.setUserId(user.getUsername());
        res.setUserNm(user.getName() != null ? user.getName() : user.getUsername());
        res.setRole(user.getRole());
        merchantProfileRepository.findByLoginId(username)
                .map(mp -> orgUnitRepository.findById(mp.getOrgUnitId()))
                .filter(Optional::isPresent)
                .map(o -> o.get())
                .ifPresent(ou -> {
                    res.setOrgUnitId(ou.getId());
                    res.setCompId(ou.getCode());
                    res.setOrgLevel(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
                });
        if (res.getCompId() == null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            firstHeadquartersOrg().ifPresent(headquarters -> {
                res.setOrgUnitId(headquarters.getId());
                res.setCompId(headquarters.getCode());
                res.setOrgLevel(headquarters.getOrgLevel() != null ? headquarters.getOrgLevel().name() : null);
            });
        }
        res.setMustChangePassword("Y".equalsIgnoreCase(user.getPasswordMustChangeYn()));
        res.setPagePermissions(orgPagePermissionService.resolvePagePermissionsForUser(user));
        res.setCanWriteNotice(orgPagePermissionService.canWriteNotice(user));
        return Optional.of(res);
    }

    public Optional<AppUser> validateToken(String token) {
        if (token == null || token.isEmpty()) return Optional.empty();
        return authTokenRepository.findByTokenAndExpiresAtAfter(token, Instant.now())
                .flatMap(at -> userRepository.findById(at.getUserId()));
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("사용자 정보를 확인할 수 없습니다.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력하세요.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력하세요.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }
        String uid = username.trim();
        String forbidden = uid + "1!";
        if (forbidden.equals(newPassword)) {
            throw new IllegalArgumentException("아이디+1! 형태의 초기 비밀번호는 새 비밀번호로 사용할 수 없습니다.");
        }
        AppUser user = userRepository.findByUsername(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 입력하세요.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordMustChangeYn("N");
        userRepository.save(user);
    }

    /**
     * 로그인ID로 소속 {@link OrgUnit} 조회.
     * 우선순위: 가맹점 프로필(로그인ID) → 사용자(tb_user)의 org_unit_code → ADMIN이면 총본사 1건.
     */
    public Optional<OrgUnit> resolveOrgUnitForLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return Optional.empty();
        String id = loginId.trim();
        Optional<OrgUnit> fromMp = merchantProfileRepository.findByLoginId(id)
                .flatMap(mp -> orgUnitRepository.findById(mp.getOrgUnitId()));
        if (fromMp.isPresent()) return fromMp;
        Optional<AppUser> userOpt = userRepository.findByUsername(id);
        if (userOpt.isEmpty()) return Optional.empty();
        AppUser u = userOpt.get();
        if (u.getOrgUnitCode() != null && !u.getOrgUnitCode().isBlank()) {
            return orgUnitRepository.findByCode(u.getOrgUnitCode().trim());
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return firstHeadquartersOrg();
        }
        return Optional.empty();
    }

    /** 로그인ID( username )로 조직 정보 조회 - 업체정보조회 필터·권한 판단용 */
    public Map<String, Object> getOrgInfo(String loginId) {
        return resolveOrgUnitForLoginId(loginId).map(this::orgToMap).orElse(null);
    }

    private Map<String, Object> orgToMap(OrgUnit ou) {
        Map<String, Object> m = new HashMap<>();
        m.put("orgUnitId", ou.getId());
        m.put("compId", ou.getCode());
        m.put("compNm", ou.getName());
        m.put("orgLevel", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
        return m;
    }

    public void changeOwnName(String username, String newName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        String uid = username.trim();
        String nn = newName != null ? newName.trim() : "";
        if (nn.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        AppUser user = userRepository.findByUsername(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setName(nn);
        userRepository.save(user);
    }

    /** 시드·운영에서 총본사가 여러 건이면 코드 순 첫 건 */
    private Optional<OrgUnit> firstHeadquartersOrg() {
        return orgUnitRepository.findAll().stream()
                .filter(unit -> unit.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .min(Comparator.comparing(OrgUnit::getCode, Comparator.nullsFirst(String::compareTo)));
    }
}
