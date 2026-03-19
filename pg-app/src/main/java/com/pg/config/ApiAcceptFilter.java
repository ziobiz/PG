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
 * - /api/ 하위: Accept 없거나 와일드카드 전체면 application/json
 * - HTML 문서 경로(루트, /login, .html): 항상 Accept 를 text/html 로 고정
 *   (브라우저는 text/html 을 포함해도 Nginx 가 Accept: application/json 을 넣으면 406 발생 가능)
 * (SecurityConfig 에서 직접 등록 - Component 제거로 Filter order 오류 방지)
 */
public class ApiAcceptFilter extends OncePerRequestFilter {

    /** 정적 JS/CSS 등은 제외 — HTML 문서·로그인 관련 URL 만 */
    private static boolean isHtmlDocumentPath(String uri) {
        if (uri.equals("/")) {
            return true;
        }
        if (uri.startsWith("/login")) {
            return true;
        }
        return uri.endsWith(".html");
    }

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
        if (isHtmlDocumentPath(uri)) {
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
