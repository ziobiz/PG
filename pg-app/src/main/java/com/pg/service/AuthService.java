package com.pg.service;

import com.pg.api.dto.LoginAttempt;
import com.pg.api.dto.LoginResponse;
import com.pg.entity.AppUser;
import com.pg.entity.AuthToken;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.AuthTokenRepository;
import com.pg.merchantdeploy.MerchantApiDeploymentService;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserRepository;
import com.pg.util.ChatbotMerchantAdminConstants;
import com.pg.util.TotpRfc6238;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
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
    private final MerchantApiDeploymentService merchantApiDeploymentService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgPagePermissionService orgPagePermissionService;
    private final OrgPortalHostService orgPortalHostService;
    private final OrgTabletMenuService orgTabletMenuService;

    public AuthService(UserRepository userRepository, AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder, MerchantProfileRepository merchantProfileRepository,
                       @Lazy MerchantApiDeploymentService merchantApiDeploymentService,
                       OrgUnitRepository orgUnitRepository,
                       @Lazy OrgPagePermissionService orgPagePermissionService,
                       OrgPortalHostService orgPortalHostService,
                       @Lazy OrgTabletMenuService orgTabletMenuService) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantApiDeploymentService = merchantApiDeploymentService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgPagePermissionService = orgPagePermissionService;
        this.orgPortalHostService = orgPortalHostService;
        this.orgTabletMenuService = orgTabletMenuService;
    }

    @Transactional
    public Optional<LoginResponse> login(String username, String password) {
        return login(username, password, null);
    }

    @Transactional
    public Optional<LoginResponse> login(String username, String password, String clientHost) {
        LoginAttempt a = loginAttempt(username, password, clientHost, null);
        return a.isSuccess() ? Optional.of(a.getResponse()) : Optional.empty();
    }

    /**
     * 웹 로그인 — 총본사·본사·총판·가맹 CHATBOT 권한그룹·ADMIN(OTP 등록 완료)은 비밀번호 후 TOTP 6자리 필수.
     */
    @Transactional
    public LoginAttempt loginAttempt(String username, String password, String clientHost, String totpCode) {
        if (username == null || username.isBlank() || password == null) {
            return LoginAttempt.badCredentials();
        }
        Optional<AppUser> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return LoginAttempt.badCredentials();
        }
        AppUser user = userOpt.get();
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPassword())) {
            return LoginAttempt.badCredentials();
        }
        String ust = user.getUserStatus();
        if (ust != null && !ust.isBlank() && !"ACTIVE".equalsIgnoreCase(ust.trim())) {
            return LoginAttempt.badCredentials();
        }
        if (!loginHostAllowedForUser(user, clientHost)) {
            return LoginAttempt.badCredentials();
        }
        if (requiresTotpSecondFactorAtLogin(user)) {
            String code = totpCode != null ? totpCode.trim() : "";
            if (code.isEmpty()) {
                return LoginAttempt.otpRequired();
            }
            if (!TotpRfc6238.verify(user.getOtpSecret(), code, 2)) {
                return LoginAttempt.otpInvalid();
            }
        }
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
        String ut = user.getUserType();
        res.setUserType(ut != null && !ut.isBlank() ? ut.trim() : "REPRESENTATIVE");
        String pgn = user.getPermissionGroupNm();
        res.setPermissionGroupNm(pgn != null && !pgn.isBlank() ? pgn.trim() : "");
        resolveOrgUnitForLoginId(username.trim()).ifPresent(ou -> {
            res.setOrgUnitId(ou.getId());
            res.setCompId(ou.getCode());
            res.setOrgLevel(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
            res.setTabletFeatureUseYn(ynTabletFeature(ou.getTabletFeatureUseYn()));
        });
        if (res.getOrgLevel() != null && "MERCHANT".equalsIgnoreCase(res.getOrgLevel())) {
            res.setChatbotPaymentUseYn(resolveChatbotPaymentUseYnForMerchant(res.getOrgUnitId()));
            res.setWebPaymentUseYn(resolveWebPaymentUseYnForMerchant(res.getOrgUnitId()));
            res.setMerchantApiDeployedYn(resolveMerchantApiDeployedYnForMerchant(res.getOrgUnitId()));
        }
        boolean mustChange = "Y".equalsIgnoreCase(user.getPasswordMustChangeYn())
                || isInitialTempPassword(user.getUsername(), password);
        res.setMustChangePassword(mustChange);
        res.setMustSetupOtp(requiresOtpEnrollment(user));
        res.setOtpRegisteredYn(isOtpFullyEnrolled(user) ? "Y" : "N");
        res.setPagePermissions(orgPagePermissionService.resolvePagePermissionsForUser(user));
        res.setTabletMenuUrls(orgTabletMenuService.resolveTabletMenuUrlsForUser(user, res.getPagePermissions()));
        res.setCanWriteNotice(orgPagePermissionService.canWriteNotice(user));
        if (clientHost != null && !clientHost.isBlank()) {
            orgPortalHostService.findPortalOrgByAdminWebHost(clientHost.trim()).ifPresent(portal -> {
                if (portal.getCode() != null && !portal.getCode().isBlank()) {
                    res.setBrandingCompId(portal.getCode().trim());
                }
            });
        }
        return LoginAttempt.success(res);
    }

    /** 가맹점(org_unit 기준) 챗봇결제 스위치 — 세션·/me 에서 챗봇 메뉴 표시에 사용 */
    public String resolveChatbotPaymentUseYnForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return "N";
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> yn(mp.getChatbotPaymentUseYn()))
                .orElse("N");
    }

    /** 가맹점(org_unit 기준) URL(웹)결제 스위치 — 태블릿 런처·세션에 사용 */
    public String resolveWebPaymentUseYnForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return "N";
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> yn(mp.getWebPaymentUseYn()))
                .orElse("N");
    }

    /**
     * 가맹점API 메뉴 노출 — 활성 브로커 시크릿 + 운영(Y) PG 바인딩 중 integ_api_yn=Y 일 때만 Y.
     */
    public String resolveMerchantApiDeployedYnForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return "N";
        }
        return merchantApiDeploymentService.isMerchantApiIntegrationEligible(orgUnitId) ? "Y" : "N";
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v != null ? v.trim() : "") ? "Y" : "N";
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
     * <ul>
     *   <li>{@code org_unit_code}가 가맹점(MERCHANT)이면 그 조직.</li>
     *   <li>그 외(본사·총판 등)로 잡혀 있어도, 동일 로그인ID에 연결된 {@code tb_merchant_profile} 이 있으면
     *       가맹점 조직을 반환(가맹 포털·챗봇 기본설정에서 compId 누락 방지).</li>
     *   <li>그다음 가맹 프로필만 있는 경우.</li>
     *   <li>ADMIN이면 총본사 1건.</li>
     * </ul>
     */
    public Optional<OrgUnit> resolveOrgUnitForLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return Optional.empty();
        String id = loginId.trim();
        Optional<OrgUnit> fromMp = merchantProfileRepository.findByLoginId(id)
                .flatMap(mp -> orgUnitRepository.findById(mp.getOrgUnitId()));
        Optional<AppUser> userOpt = userRepository.findByUsername(id);
        if (userOpt.isPresent()) {
            AppUser u = userOpt.get();
            if (u.getOrgUnitCode() != null && !u.getOrgUnitCode().isBlank()) {
                Optional<OrgUnit> fromUserCode = orgUnitRepository.findByCode(u.getOrgUnitCode().trim());
                if (fromUserCode.isPresent()) {
                    OrgUnit ou = fromUserCode.get();
                    if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                        return fromUserCode;
                    }
                    if (fromMp.isPresent() && fromMp.get().getOrgLevel() == OrgLevel.MERCHANT) {
                        return fromMp;
                    }
                    return fromUserCode;
                }
            }
        }
        if (fromMp.isPresent()) {
            return fromMp;
        }
        if (userOpt.isPresent() && "ADMIN".equalsIgnoreCase(userOpt.get().getRole())) {
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
        m.put("tabletFeatureUseYn", ynTabletFeature(ou.getTabletFeatureUseYn()));
        return m;
    }

    private static String ynTabletFeature(String v) {
        if (v == null || v.isBlank()) {
            return "Y";
        }
        return "Y".equalsIgnoreCase(v.trim()) ? "Y" : "N";
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

    /**
     * 호스트별 로그인 허용(도메인구성 — 관리자 웹 URL 호스트 기준):
     * <ul>
     *   <li><strong>본사(REGIONAL) 포털</strong>: 총본사(HEADQUARTERS) 또는 <strong>그 본사 조직에 직접 소속</strong>된 계정만.
     *       총판·가맹 등 하위 조직 계정은 불가.</li>
     *   <li><strong>총판(MASTER_DIST) 포털</strong>: 총본사·<strong>이 총판을 트리 안에 두는 본사(REGIONAL)</strong>·해당 총판 및 그 하위(지사·대리점·영업점·가맹점)만.
     *       다른 총판 소속·다른 본사 트리는 불가.</li>
     *   <li>포털 미매칭: 사용자 소속 조직의 관리자 URL 호스트와 비교(미설정이면 통과).</li>
     * </ul>
     * ADMIN·clientHost 미전달은 검사 생략.
     */
    private boolean loginHostAllowedForUser(AppUser user, String clientHost) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (clientHost == null || clientHost.isBlank()) {
            return true;
        }
        Optional<OrgUnit> portalOpt = orgPortalHostService.findPortalOrgByAdminWebHost(clientHost.trim());
        Optional<OrgUnit> userOuOpt = resolveOrgUnitForLoginId(user.getUsername());
        if (portalOpt.isPresent()) {
            OrgUnit portal = portalOpt.get();
            if (userOuOpt.isEmpty()) {
                return false;
            }
            OrgUnit userOu = userOuOpt.get();
            if (portal.getOrgLevel() == OrgLevel.REGIONAL) {
                if (userOu.getOrgLevel() == OrgLevel.HEADQUARTERS) {
                    return true;
                }
                return userOu.getId() != null && portal.getId() != null && userOu.getId().equals(portal.getId());
            }
            if (portal.getOrgLevel() == OrgLevel.MASTER_DIST) {
                if (userOu.getOrgLevel() == OrgLevel.HEADQUARTERS) {
                    return true;
                }
                if (userOu.getOrgLevel() == OrgLevel.REGIONAL
                        && userOu.getId() != null
                        && portal.getId() != null
                        && orgPortalHostService.orgIsSelfOrUnderAncestor(userOu.getId(), portal.getId())) {
                    return true;
                }
                return orgPortalHostService.userOrgBelongsToPortalSubtree(userOu, portal);
            }
            return orgPortalHostService.userOrgBelongsToPortalSubtree(userOu, portal);
        }
        if (userOuOpt.isEmpty()) {
            return true;
        }
        String adminUrl = userOuOpt.get().getOrgDomainAdminUrl();
        if (adminUrl == null || adminUrl.isBlank()) {
            return true;
        }
        String expected = hostFromConfiguredUrl(adminUrl.trim());
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return hostsMatch(expected, clientHost.trim());
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

    private static boolean hostsMatch(String expectedHost, String clientHost) {
        return expectedHost.equalsIgnoreCase(hostOnlyFromClient(clientHost));
    }

    /** 시드·운영에서 총본사가 여러 건이면 코드 순 첫 건 */
    private Optional<OrgUnit> firstHeadquartersOrg() {
        return orgUnitRepository.findAll().stream()
                .filter(unit -> unit.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .min(Comparator.comparing(OrgUnit::getCode, Comparator.nullsFirst(String::compareTo)));
    }

    /**
     * 로그인 시 입력한 비밀번호가 초기 패턴(로그인ID + "1!")과 동일한지(평문 기준).
     * DB의 password_must_change_yn 과 무관하게 최초 임시 비번이면 변경 절차를 태웁니다.
     */
    public boolean isInitialTempPassword(String username, String plainPassword) {
        if (username == null || plainPassword == null) {
            return false;
        }
        String u = username.trim();
        String p = plainPassword.trim();
        if (u.isEmpty() || p.isEmpty()) {
            return false;
        }
        return (u + "1!").equals(p);
    }

    /**
     * {@code otp_registered_yn=Y} 이고 {@code otp_secret} 이 비어 있지 않을 때만 등록 완료.
     * 과거 시드처럼 Y 만 있고 시크릿이 없으면 미등록으로 본다.
     */
    public boolean isOtpFullyEnrolled(AppUser user) {
        if (user == null) {
            return false;
        }
        return "Y".equalsIgnoreCase(user.getOtpRegisteredYn())
                && user.getOtpSecret() != null
                && !user.getOtpSecret().isBlank();
    }

    /** 민감 운영(비활성카드 해지 등) — Google OTP 6자리 검증 */
    public void verifyTotpOrThrow(AppUser user, String totpCode) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        if (!isOtpFullyEnrolled(user)) {
            throw new IllegalStateException("Google OTP 등록 후 진행할 수 있습니다.");
        }
        String code = totpCode != null ? totpCode.trim() : "";
        if (code.isEmpty()) {
            throw new IllegalStateException("OTP 코드를 입력하세요.");
        }
        if (!TotpRfc6238.verify(user.getOtpSecret(), code, 2)) {
            throw new IllegalStateException("OTP 코드가 올바르지 않습니다.");
        }
    }

    /**
     * 가맹(MERCHANT) 소속이고 {@code tb_user.permission_group_nm} 이 CHATBOT(구 챗봇관리자)인 계정.
     * 챗봇 상품 관리 등에 Google OTP 등록·로그인 2단계를 적용한다.
     */
    public boolean isMerchantChatbotPermissionGroupUser(AppUser user) {
        if (user == null || !ChatbotMerchantAdminConstants.isChatbotPermissionGroupNm(user.getPermissionGroupNm())) {
            return false;
        }
        Optional<OrgUnit> ouOpt = resolveOrgUnitForLoginId(user.getUsername());
        if (ouOpt.isEmpty()) {
            return false;
        }
        OrgLevel lvl = ouOpt.get().getOrgLevel();
        return lvl == OrgLevel.MERCHANT;
    }

    /**
     * Google OTP 미등록 시 로그인 후 등록을 유도하는 경우 true.
     * <ul>
     *   <li>{@code ADMIN} 역할: 소속 조직이 없어도 등록·테스트 가능(시드 {@code admin} 포함).</li>
     *   <li>가맹(MERCHANT) 소속이면서 권한그룹 CHATBOT(구 챗봇관리자).</li>
     *   <li>그 외: 총본사·본사·총판(HEADQUARTERS / REGIONAL / MASTER_DIST) 소속만 해당.</li>
     * </ul>
     * {@link #isOtpFullyEnrolled(AppUser)} 이면 false.
     */
    public boolean requiresOtpEnrollment(AppUser user) {
        if (user == null) {
            return false;
        }
        if (isOtpFullyEnrolled(user)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (isMerchantChatbotPermissionGroupUser(user)) {
            return true;
        }
        Optional<OrgUnit> ouOpt = resolveOrgUnitForLoginId(user.getUsername());
        if (ouOpt.isEmpty()) {
            return false;
        }
        OrgLevel lvl = ouOpt.get().getOrgLevel();
        if (lvl == null) {
            return false;
        }
        return lvl == OrgLevel.HEADQUARTERS || lvl == OrgLevel.REGIONAL || lvl == OrgLevel.MASTER_DIST;
    }

    /**
     * 로그인 시점 Google OTP(TOTP) 2단계 인증 대상 여부.
     * OTP 앱 등록이 완료된 총본사·본사·총판 소속, 가맹 CHATBOT 권한그룹, 및 ADMIN 만 해당.
     */
    public boolean requiresTotpSecondFactorAtLogin(AppUser user) {
        if (user == null || !isOtpFullyEnrolled(user)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (isMerchantChatbotPermissionGroupUser(user)) {
            return true;
        }
        Optional<OrgUnit> ouOpt = resolveOrgUnitForLoginId(user.getUsername());
        if (ouOpt.isEmpty()) {
            return false;
        }
        OrgLevel lvl = ouOpt.get().getOrgLevel();
        if (lvl == null) {
            return false;
        }
        return lvl == OrgLevel.HEADQUARTERS || lvl == OrgLevel.REGIONAL || lvl == OrgLevel.MASTER_DIST;
    }
}
