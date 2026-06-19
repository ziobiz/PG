package com.pg.config;

import com.pg.middleware.notify.PgNotifyIngressHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 가 {@code application/x-www-form-urlencoded} POST 를 폼 로그인·CSRF 등으로
 * {@code 302 /login.html} 로 보내는 환경에서도, NOTI·JPAY 서버 노티(form)가
 * {@link PgNotifyIngressHandler} 에 도달하도록 {@link ApiLoginBypassFilter} 와 동일하게 우회합니다.
 */
public class ApiPgNotifyBypassFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiPgNotifyBypassFilter.class);
    private static final String OPEN_PREFIX = "/api/open/pg-notify/";
    private static final String MIDDLEWARE_PREFIX = "/api/middleware/notify/v1/pg-notify/";
    private static final String RELAY_SUFFIX = "noti-middleware-relay";

    private final PgNotifyIngressHandler ingressHandler;

    public ApiPgNotifyBypassFilter(PgNotifyIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        ParsedPath parsed = parsePostPath(request.getRequestURI());
        if (parsed == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            ResponseEntity<?> entity;
            if (parsed.relay) {
                String rawJson = readBody(request);
                entity = ingressHandler.notiMiddlewareRelay(parsed.token, parsed.targetCode, rawJson, request);
            } else {
                entity = ingressHandler.receivePostByTarget(parsed.token, parsed.targetCode, request);
            }
            writeResponse(response, entity);
        } catch (Exception e) {
            log.warn("pg-notify bypass 처리 실패 uri={}: {}", request.getRequestURI(), e.toString());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"success\":false,\"processed\":false,\"retryable\":true,\"errorCode\":\"NOTIFY_ERROR\"}");
        }
    }

    private static ParsedPath parsePostPath(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        String path = uri.trim();
        String rest;
        if (path.startsWith(OPEN_PREFIX)) {
            rest = path.substring(OPEN_PREFIX.length());
        } else if (path.startsWith(MIDDLEWARE_PREFIX)) {
            rest = path.substring(MIDDLEWARE_PREFIX.length());
        } else {
            return null;
        }
        if (rest.isBlank()) {
            return null;
        }
        String[] parts = rest.split("/", -1);
        if (parts.length == 1) {
            return new ParsedPath(parts[0], null, false);
        }
        if (parts.length == 2) {
            if (RELAY_SUFFIX.equalsIgnoreCase(parts[1])) {
                return new ParsedPath(parts[0], null, true);
            }
            return new ParsedPath(parts[0], parts[1], false);
        }
        if (parts.length == 3 && RELAY_SUFFIX.equalsIgnoreCase(parts[2])) {
            return new ParsedPath(parts[0], parts[1], true);
        }
        return null;
    }

    private static String readBody(HttpServletRequest request) throws IOException {
        byte[] buf = request.getInputStream().readAllBytes();
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static void writeResponse(HttpServletResponse response, ResponseEntity<?> entity) throws IOException {
        if (entity == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if (entity.getHeaders().getLocation() != null) {
            response.setStatus(entity.getStatusCode().value());
            response.sendRedirect(entity.getHeaders().getLocation().toString());
            return;
        }
        response.setStatus(entity.getStatusCode().value());
        Object body = entity.getBody();
        MediaType mt = entity.getHeaders().getContentType();
        if (mt != null) {
            response.setContentType(mt.toString());
        } else if (body != null && body.toString().trim().startsWith("{")) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (body != null) {
            response.getWriter().write(body.toString());
        }
    }

    private record ParsedPath(String token, String targetCode, boolean relay) {
    }
}
