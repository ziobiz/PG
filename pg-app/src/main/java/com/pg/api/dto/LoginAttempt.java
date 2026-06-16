package com.pg.api.dto;

/**
 * /api/auth/login 결과 — 2단계(비밀번호 후 TOTP) 구분용.
 */
public final class LoginAttempt {

    public enum Kind {
        SUCCESS,
        BAD_CREDENTIALS,
        OTP_REQUIRED,
        OTP_INVALID,
        /** 업체 영구정지(S) — 로그인 불가 */
        ORG_SUSPENDED
    }

    private final Kind kind;
    private final LoginResponse response;

    private LoginAttempt(Kind kind, LoginResponse response) {
        this.kind = kind;
        this.response = response;
    }

    public static LoginAttempt success(LoginResponse response) {
        return new LoginAttempt(Kind.SUCCESS, response);
    }

    public static LoginAttempt badCredentials() {
        return new LoginAttempt(Kind.BAD_CREDENTIALS, null);
    }

    public static LoginAttempt otpRequired() {
        return new LoginAttempt(Kind.OTP_REQUIRED, null);
    }

    public static LoginAttempt otpInvalid() {
        return new LoginAttempt(Kind.OTP_INVALID, null);
    }

    public static LoginAttempt orgSuspended() {
        return new LoginAttempt(Kind.ORG_SUSPENDED, null);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isSuccess() {
        return kind == Kind.SUCCESS;
    }

    public LoginResponse getResponse() {
        return response;
    }
}
