package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Cloudflare Turnstile siteverify — 관리자 {@code POST /api/auth/login} 봇 방지.
 * 시크릿은 서버만 사용한다.
 */
@Service
public class TurnstileVerificationService {

    public static final String CODE_REQUIRED = "TURNSTILE_REQUIRED";
    public static final String CODE_FAIL = "TURNSTILE_FAIL";
    public static final String CODE_ERROR = "TURNSTILE_ERROR";

    public static final String MSG_REQUIRED = "보안 확인을 완료해 주세요.";
    public static final String MSG_FAIL = "보안 확인에 실패했습니다. 다시 시도해 주세요.";
    public static final String MSG_ERROR = "보안 확인을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    private static final Logger log = LoggerFactory.getLogger(TurnstileVerificationService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.turnstile.enabled:true}")
    private boolean enabled;

    @Value("${app.turnstile.secret:}")
    private String secret;

    @Value("${app.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    public TurnstileVerificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled && secret != null && !secret.isBlank();
    }

    public Optional<ApiResponse<?>> rejectIfNeeded(String token, HttpServletRequest request) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String t = token != null ? token.trim() : "";
        if (t.isEmpty()) {
            return Optional.of(ApiResponse.fail(MSG_REQUIRED, CODE_REQUIRED));
        }
        try {
            String body = "secret=" + URLEncoder.encode(secret.trim(), StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(t, StandardCharsets.UTF_8);
            String ip = clientIp(request);
            if (ip != null && !ip.isBlank()) {
                body += "&remoteip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8);
            }
            HttpRequest httpReq = HttpRequest.newBuilder(URI.create(verifyUrl.trim()))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("Turnstile siteverify HTTP {}", res.statusCode());
                return Optional.of(ApiResponse.fail(MSG_ERROR, CODE_ERROR));
            }
            JsonNode json = objectMapper.readTree(res.body() != null ? res.body() : "{}");
            if (json.path("success").asBoolean(false)) {
                return Optional.empty();
            }
            log.debug("Turnstile siteverify failed error-codes={}", json.path("error-codes"));
            return Optional.of(ApiResponse.fail(MSG_FAIL, CODE_FAIL));
        } catch (Exception e) {
            log.warn("Turnstile siteverify error: {}", e.toString());
            return Optional.of(ApiResponse.fail(MSG_ERROR, CODE_ERROR));
        }
    }

    static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
