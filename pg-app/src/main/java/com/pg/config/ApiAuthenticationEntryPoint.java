package com.pg.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API(/api/**): 401만 반환(302 금지). fetch 가 리다이렉트를 따라가며 ERR_TOO_MANY_REDIRECTS 나는 것을 방지.
 * 그 외 페이지: 상대 경로로 /login.html 만 사용(절대 URL 금지 — X-Forwarded-Proto 불일치 시 https↔http 무한 리다이렉트 방지).
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final HttpStatusEntryPoint UNAUTHORIZED = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath() != null ? request.getContextPath() : "";
        String apiPrefix = ctx + "/api/";
        if (uri != null && uri.startsWith(apiPrefix)) {
            UNAUTHORIZED.commence(request, response, authException);
            return;
        }
        String target = ctx.isEmpty() ? "/login.html" : ctx + "/login.html";
        response.sendRedirect(response.encodeRedirectURL(target));
    }
}
