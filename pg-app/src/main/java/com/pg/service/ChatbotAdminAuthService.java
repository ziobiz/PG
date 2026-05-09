package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.ChatbotAdminSession;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.ChatbotAdminSessionRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserRepository;
import com.pg.util.TotpRfc6238;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 공개 챗봇 페이지에서 가맹 챗봇 관리자(가맹당 1명, OTP 필수) 로그인·세션.
 */
@Service
public class ChatbotAdminAuthService {

    public static final String TOKEN_HEADER = "X-PG-Chatbot-Admin-Token";
    private static final int TOKEN_VALID_HOURS = 4;
    private static final SecureRandom RND = new SecureRandom();

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantChatbotProductService productService;
    private final ChatbotAdminSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public ChatbotAdminAuthService(UserRepository userRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   MerchantChatbotProductService productService,
                                   ChatbotAdminSessionRepository sessionRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.productService = productService;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record ValidSession(long userId, long orgUnitId, String compId) {}

    private static boolean otpFullyEnrolled(AppUser user) {
        if (user == null) {
            return false;
        }
        return "Y".equalsIgnoreCase(user.getOtpRegisteredYn())
                && user.getOtpSecret() != null
                && !user.getOtpSecret().isBlank();
    }

    /**
     * compId 가맹의 지정 챗봇 관리자만 로그인 가능. 비밀번호 + TOTP 6자리 필수.
     */
    @Transactional
    public Map<String, Object> login(String compId, String username, String password, String totpCode) {
        if (compId == null || compId.isBlank() || username == null || username.isBlank()
                || password == null || totpCode == null || totpCode.isBlank()) {
            throw new IllegalArgumentException("가맹코드·아이디·비밀번호·OTP를 입력하세요.");
        }
        String cid = compId.trim();
        OrgUnit ou = productService.requireMerchantOrgByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("가맹점 코드를 확인하세요."));
        if (!productService.isChatbotPaymentOpenForMerchant(ou.getId())) {
            throw new IllegalArgumentException("챗봇 결제가 비활성인 가맹점입니다.");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        Long adminId = mp.getChatbotAdminUserId();
        if (adminId == null) {
            throw new IllegalArgumentException("챗봇 관리자가 지정되지 않았습니다. 본사/상위에서 업체 정보에 등록하세요.");
        }
        AppUser user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!adminId.equals(user.getId())) {
            throw new IllegalArgumentException("이 가맹점의 챗봇 관리자 계정이 아닙니다.");
        }
        String occ = user.getOrgUnitCode() != null ? user.getOrgUnitCode().trim() : "";
        if (!occ.equalsIgnoreCase(cid)) {
            throw new IllegalArgumentException("소속 가맹점이 일치하지 않습니다.");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }
        String ust = user.getUserStatus();
        if (ust != null && !ust.isBlank() && !"ACTIVE".equalsIgnoreCase(ust.trim())) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!otpFullyEnrolled(user)) {
            throw new IllegalArgumentException("챗봇 관리자는 Google OTP 등록이 필요합니다. 관리자 웹에서 OTP를 먼저 등록하세요.");
        }
        if (!TotpRfc6238.verify(user.getOtpSecret(), totpCode.trim(), 2)) {
            throw new IllegalArgumentException("OTP 코드가 올바르지 않습니다.");
        }
        sessionRepository.deleteByUserId(user.getId());
        sessionRepository.deleteExpiredBefore(Instant.now());

        byte[] buf = new byte[32];
        RND.nextBytes(buf);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : buf) {
            hex.append(String.format("%02x", b));
        }
        String token = hex.toString();
        Instant exp = Instant.now().plusSeconds(TOKEN_VALID_HOURS * 3600L);
        ChatbotAdminSession row = new ChatbotAdminSession();
        row.setToken(token);
        row.setUserId(user.getId());
        row.setOrgUnitId(ou.getId());
        row.setExpiresAt(exp);
        row.setCreatedAt(Instant.now());
        sessionRepository.save(row);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("expiresAt", exp.toEpochMilli());
        out.put("username", user.getUsername());
        return out;
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessionRepository.findByToken(token.trim()).ifPresent(sessionRepository::delete);
    }

    @Transactional(readOnly = true)
    public Optional<ValidSession> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<ChatbotAdminSession> s = sessionRepository.findByToken(token.trim());
        if (s.isEmpty()) {
            return Optional.empty();
        }
        ChatbotAdminSession row = s.get();
        if (row.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        OrgUnit ou = orgUnitRepository.findById(row.getOrgUnitId()).orElse(null);
        if (ou == null || ou.getCode() == null) {
            return Optional.empty();
        }
        return Optional.of(new ValidSession(row.getUserId(), row.getOrgUnitId(), ou.getCode().trim()));
    }
}
