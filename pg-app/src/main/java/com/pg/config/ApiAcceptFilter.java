package com.pg.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 406 Not Acceptable 방지
 * - /api/** : Accept 없거나 (all) 이면 application/json
 * - /, /login : Accept 없거나 (all) 이면 text/html
 * (SecurityConfig에서 직접 등록 - @Component 제거로 Filter order 오류 방지)
 */
public class ApiAcceptFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        boolean needsDefault = accept == null || accept.trim().isEmpty() || "*/*".equals(accept.trim());

        if (uri.startsWith("/api/")) {
            if (needsDefault) {
                filterChain.doFilter(new AcceptHeaderRequestWrapper(request, "application/json"), response);
            } else {
                filterChain.doFilter(request, response);
            }
            return;
        }
        if ((uri.equals("/") || uri.startsWith("/login")) && needsDefault) {
            filterChain.doFilter(new AcceptHeaderRequestWrapper(request, "text/html"), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static class AcceptHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final String acceptHeader;

        public AcceptHeaderRequestWrapper(HttpServletRequest request, String acceptHeader) {
            super(request);
            this.acceptHeader = acceptHeader;
        }

        @Override
        public String getHeader(String name) {
            if ("Accept".equalsIgnoreCase(name)) {
                return acceptHeader;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Accept".equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(acceptHeader));
            }
            return super.getHeaders(name);
        }
    }
}
