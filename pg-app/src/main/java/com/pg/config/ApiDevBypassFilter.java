package com.pg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.controller.api.ApiDevController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 우회: GET /api/dev/** 요청을 직접 처리하여 JSON 반환 (302 리다이렉트 방지)
 */
public class ApiDevBypassFilter extends OncePerRequestFilter {

    private final ApiDevController apiDevController;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiDevBypassFilter(ApiDevController apiDevController) {
        this.apiDevController = apiDevController;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/api/dev/")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            ResponseEntity<ApiResponse<Object>> result;
            if (request.getRequestURI().endsWith("/ping")) {
                result = apiDevController.ping();
            } else {
                result = apiDevController.seedOrg();
            }
            writeJson(response, result.getStatusCode().value(), result.getBody());
        } catch (Exception e) {
            writeJson(response, 500, ApiResponse.fail("시드 생성 중 오류: " + (e.getMessage() != null ? e.getMessage() : "알 수 없음"), "ERROR"));
        }
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
