package com.pg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.CompService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * dev 프로파일에서 GET /api/comp/list 요청을 인증 없이 처리 (검색 결과 미표시 문제 우회)
 */
public class ApiCompListBypassFilter extends OncePerRequestFilter {

    private final CompService compService;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiCompListBypassFilter(CompService compService, Environment environment) {
        this.compService = compService;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || !"/api/comp/list".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean isDev = Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
        if (!isDev) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String searchCompId = request.getParameter("searchCompId");
            String searchCompNm = request.getParameter("searchCompNm");
            String searchCompDiv = request.getParameter("searchCompDiv");
            String searchUseYn = request.getParameter("searchUseYn");
            String searchPayHoldYn = request.getParameter("searchPayHoldYn");
            String searchCeoNm = request.getParameter("searchCeoNm");
            String searchTerminalId = request.getParameter("searchTerminalId");
            String searchCeoMobile = request.getParameter("searchCeoMobile");
            String searchRegNo = request.getParameter("searchRegNo");
            Boolean searchIncludeSub = parseBoolean(request.getParameter("searchIncludeSub"));
            int page = parseInt(request.getParameter("page"), 1);
            int size = parseInt(request.getParameter("size"), 20);

            PageResult<Map<String, Object>> result = compService.search(
                    searchCompId, searchCompNm, searchCompDiv, searchUseYn, searchPayHoldYn,
                    searchCeoNm, searchTerminalId, searchCeoMobile, searchRegNo, searchIncludeSub,
                    page, size, null, false);

            writeJson(response, 200, ApiResponse.ok(result));
        } catch (Exception e) {
            writeJson(response, 500, ApiResponse.fail("조회 중 오류: " + (e.getMessage() != null ? e.getMessage() : "알 수 없음"), "ERROR"));
        }
    }

    private static Boolean parseBoolean(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    private static int parseInt(String v, int def) {
        if (v == null || v.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
