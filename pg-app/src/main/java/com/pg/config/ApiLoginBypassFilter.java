package com.pg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.api.dto.LoginAttempt;
import com.pg.api.dto.LoginRequest;
import com.pg.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 우회: POST /api/auth/login 을 직접 처리하여 JSON 반환 (302 방지)
 */
@Order(-1)
public class ApiLoginBypassFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiLoginBypassFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/api/auth/login".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            LoginRequest req = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            if (req == null || req.getUsername() == null || req.getPassword() == null) {
                writeJson(response, ApiResponse.fail("아이디와 비밀번호를 입력하세요.", "INVALID_INPUT"));
                return;
            }
            String ch = req.getClientHost() != null ? req.getClientHost().trim() : null;
            String totp = req.getTotpCode() != null ? String.valueOf(req.getTotpCode()).trim() : "";
            LoginAttempt attempt = authService.loginAttempt(req.getUsername().trim(), req.getPassword(), ch,
                    totp.isEmpty() ? null : totp);
            switch (attempt.getKind()) {
                case SUCCESS -> writeJson(response, ApiResponse.ok(attempt.getResponse()));
                case BAD_CREDENTIALS -> writeJson(response, ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.", "AUTH_FAIL"));
                case OTP_REQUIRED -> writeJson(response, ApiResponse.fail("Google Authenticator의 6자리 코드를 입력하세요.", "OTP_REQUIRED"));
                case OTP_INVALID -> writeJson(response, ApiResponse.fail("OTP 코드가 올바르지 않습니다.", "OTP_INVALID"));
            }
        } catch (Exception e) {
            writeJson(response, ApiResponse.fail("로그인 처리 중 오류가 발생했습니다.", "ERROR"));
        }
    }

    private void writeJson(HttpServletResponse response, ApiResponse<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
