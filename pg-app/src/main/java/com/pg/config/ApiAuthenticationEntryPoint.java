package com.pg.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.io.IOException;

/**
 * /api/** 인증 실패 시 401 반환 (302 리다이렉트 방지)
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final HttpStatusEntryPoint UNAUTHORIZED = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        UNAUTHORIZED.commence(request, response, authException);
    }
}
