package com.pg.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * GET / 가 static index(환영 페이지)로 가며 Accept 협상으로 406 이 나는 경우 방지.
 * DispatcherServlet·WelcomePage 앞에서 바로 /login.html 로 보냄.
 */
public class RootRedirectFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        if (ctx.isEmpty()) {
            return !"/".equals(uri);
        }
        return !(uri.equals(ctx) || uri.equals(ctx + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        response.sendRedirect(ctx + "/login.html");
    }
}
