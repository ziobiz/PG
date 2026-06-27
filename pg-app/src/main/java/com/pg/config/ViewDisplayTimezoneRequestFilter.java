package com.pg.config;

import com.pg.util.ViewDisplayTimezoneResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 목록 API {@code viewDisplayTimezone} 쿼리 — 요청 스코프 표시 Zone 바인딩.
 */
public class ViewDisplayTimezoneRequestFilter extends OncePerRequestFilter {

    static final String PARAM = "viewDisplayTimezone";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !needsBind(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            ViewDisplayTimezoneResolver.bindRequestOverride(request.getParameter(PARAM));
            filterChain.doFilter(request, response);
        } finally {
            ViewDisplayTimezoneResolver.clearRequestOverride();
        }
    }

    private static boolean needsBind(String uri) {
        return uri.startsWith("/api/calc/")
                || uri.startsWith("/api/settlement/")
                || uri.startsWith("/api/chatbot/")
                || uri.startsWith("/api/pay/");
    }
}
