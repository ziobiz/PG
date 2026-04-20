package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.UserRepository;
import com.pg.util.TotpRfc6238;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * 총본사·본사·총판 및 {@code ADMIN} 역할 계정의 Google OTP 등록.
 * 이메일로 발송하는 구성용 인증번호는 {@value #OTP_SETUP_EMAIL_TO} 로만 발송합니다.
 */
@Service
public class UserOtpEnrollmentService {

    public static final String OTP_SETUP_EMAIL_TO = "ziobizm@gmail.com";
    private static final int EMAIL_CODE_MINUTES = 15;
    private static final String OTP_ISSUER = "PG";
    private static final String CODE_PEPPER = "pg-otp-email-v1";

    private final UserRepository userRepository;
    private final AuthService authService;
    private final HqLedgerSysSettingsRepository ledgerSysSettingsRepository;
    private final LedgerSmtpMailService ledgerSmtpMailService;

    public UserOtpEnrollmentService(UserRepository userRepository,
                                    AuthService authService,
                                    HqLedgerSysSettingsRepository ledgerSysSettingsRepository,
                                    LedgerSmtpMailService ledgerSmtpMailService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.ledgerSysSettingsRepository = ledgerSysSettingsRepository;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
    }

    private AppUser reload(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private void requireEligible(AppUser user) {
        if (!authService.requiresOtpEnrollment(user)) {
            throw new IllegalArgumentException("이 계정은 Google OTP 등록 대상이 아닙니다.");
        }
    }

    @Transactional
    public void requestEmailVerificationCode(AppUser actor) {
        AppUser user = reload(actor.getId());
        requireEligible(user);
        clearPendingState(user);
        String code = String.format(Locale.US, "%06d", new SecureRandom().nextInt(1_000_000));
        user.setOtpSetupCodeHash(hashEmailCode(user.getId(), code));
        user.setOtpSetupExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_CODE_MINUTES));
        userRepository.save(user);

        HqLedgerSysSettings smtp = ledgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("전산설정(SMTP)이 없습니다. 메일 발송을 위해 전산설정관리에서 SMTP를 등록하세요."));
        String subject = "[PG] Google OTP 구성 인증번호 (" + user.getUsername() + ")";
        String body = "Google OTP 등록을 위한 인증번호입니다.\n\n"
                + "사용자: " + user.getUsername() + "\n"
                + "인증번호(6자리): " + code + "\n\n"
                + "유효 시간: " + EMAIL_CODE_MINUTES + "분\n"
                + "본 메일은 지정 운영 수신처로만 발송됩니다.";
        ledgerSmtpMailService.sendPlainText(smtp, OTP_SETUP_EMAIL_TO, subject, body);
    }

    @Transactional
    public Map<String, Object> verifyEmailCodeAndIssueSecret(AppUser actor, String emailCode) {
        requireEligible(actor);
        if (emailCode == null || !emailCode.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("이메일 인증번호 6자리를 입력하세요.");
        }
        if (actor.getOtpSetupCodeHash() == null || actor.getOtpSetupExpiresAt() == null) {
            throw new IllegalArgumentException("먼저 인증 메일을 발송하세요.");
        }
        if (actor.getOtpSetupExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 발송하세요.");
        }
        String trimmed = emailCode.trim();
        if (!actor.getOtpSetupCodeHash().equals(hashEmailCode(actor.getId(), trimmed))) {
            throw new IllegalArgumentException("이메일 인증번호가 올바르지 않습니다.");
        }
        String secret = TotpRfc6238.randomBase32Secret();
        actor.setOtpPendingSecret(secret);
        actor.setOtpSetupCodeHash(null);
        actor.setOtpSetupExpiresAt(null);
        userRepository.save(actor);

        String accountLabel = actor.getUsername();
        String otpAuthUri = buildOtpAuthUri(accountLabel, secret);
        return Map.of(
                "secretBase32", secret,
                "otpAuthUri", otpAuthUri,
                "issuer", OTP_ISSUER,
                "accountLabel", accountLabel
        );
    }

    @Transactional
    public void activatePendingSecret(AppUser actor, String totpCode) {
        AppUser user = reload(actor.getId());
        requireEligible(user);
        if (user.getOtpPendingSecret() == null || user.getOtpPendingSecret().isBlank()) {
            throw new IllegalArgumentException("먼저 이메일 인증을 완료하세요.");
        }
        if (!TotpRfc6238.verify(user.getOtpPendingSecret(), totpCode, 2)) {
            throw new IllegalArgumentException("Google OTP 앱의 6자리 코드가 올바르지 않습니다.");
        }
        user.setOtpSecret(user.getOtpPendingSecret());
        user.setOtpPendingSecret(null);
        user.setOtpRegisteredYn("Y");
        userRepository.save(user);
    }

    private void clearPendingState(AppUser u) {
        u.setOtpPendingSecret(null);
        u.setOtpSetupCodeHash(null);
        u.setOtpSetupExpiresAt(null);
    }

    private static String hashEmailCode(long userId, String code) {
        return DigestUtils.sha256Hex(code + "|" + userId + "|" + CODE_PEPPER);
    }

    private static String buildOtpAuthUri(String accountLabel, String secretBase32) {
        try {
            String label = URLEncoder.encode(OTP_ISSUER + ":" + accountLabel, StandardCharsets.UTF_8);
            String issuer = URLEncoder.encode(OTP_ISSUER, StandardCharsets.UTF_8);
            return "otpauth://totp/" + label + "?secret=" + secretBase32 + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
        } catch (Exception e) {
            throw new IllegalStateException("OTP URI 생성 실패", e);
        }
    }
}
