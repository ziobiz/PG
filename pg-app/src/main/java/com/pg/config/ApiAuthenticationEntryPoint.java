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
 * API auth failure: return 401 instead of 302 redirect
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final HttpStatusEntryPoint UNAUTHORIZED = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        UNAUTHORIZED.commence(request, response, authException);
    }
}
