package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.util.TurnstileHostPolicy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
    private final OrgPortalHostService orgPortalHostService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.turnstile.enabled:true}")
    private boolean enabled;

    @Value("${app.turnstile.secret:}")
    private String secret;

    @Value("${app.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    public TurnstileVerificationService(ObjectMapper objectMapper, OrgPortalHostService orgPortalHostService) {
        this.objectMapper = objectMapper;
        this.orgPortalHostService = orgPortalHostService;
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
            String idempotencyKey = UUID.randomUUID().toString();
            JsonNode json = siteverify(t, clientIp(request), idempotencyKey);
            if (json == null) {
                return Optional.of(ApiResponse.fail(MSG_ERROR, CODE_ERROR));
            }
            if (isSuccess(json)) {
                String host = json.path("hostname").asText("");
                if (!host.isBlank() && !isTrustedTurnstileHost(host)) {
                    log.warn("Turnstile hostname not an ICOPAY admin host: {}", host);
                    return Optional.of(ApiResponse.fail(MSG_FAIL, CODE_FAIL));
                }
                return Optional.empty();
            }
            List<String> codes = errorCodes(json);
            String host = json.path("hostname").asText("");
            String reqHost = requestHost(request);
            if (isHostnameMismatchOnly(codes) && (isTrustedTurnstileHost(host) || isTrustedTurnstileHost(reqHost))) {
                log.warn("Turnstile hostname-mismatch accepted for admin host={} reqHost={}", host, reqHost);
                return Optional.empty();
            }
            /* 위젯 성공 후 동일 토큰이 api 서브도메인+포털 호스트로 두 번 가면 timeout-or-duplicate.
               hostname 이 우리 포털이면 이미 유효했던 토큰으로 본다. */
            if (isTimeoutOrDuplicate(codes) && (isTrustedTurnstileHost(host) || isTrustedTurnstileHost(reqHost))) {
                log.warn("Turnstile timeout-or-duplicate accepted for admin host={} reqHost={}", host, reqHost);
                return Optional.empty();
            }
            log.warn("Turnstile siteverify failed error-codes={} hostname={} reqHost={}", codes, host, reqHost);
            return Optional.of(ApiResponse.fail(MSG_FAIL, CODE_FAIL));
        } catch (Exception e) {
            log.warn("Turnstile siteverify error: {}", e.toString());
            return Optional.of(ApiResponse.fail(MSG_ERROR, CODE_ERROR));
        }
    }

    private JsonNode siteverify(String token, String ip, String idempotencyKey) throws Exception {
        JsonNode json = postSiteverify(token, ip, idempotencyKey);
        if (json == null) {
            return null;
        }
        List<String> codes = errorCodes(json);
        if (!isSuccess(json) && codes.size() == 1 && "internal-error".equalsIgnoreCase(codes.get(0))) {
            log.warn("Turnstile siteverify internal-error — retry once");
            JsonNode retry = postSiteverify(token, ip, idempotencyKey);
            return retry != null ? retry : json;
        }
        return json;
    }

    private JsonNode postSiteverify(String token, String ip, String idempotencyKey) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("secret=").append(URLEncoder.encode(secret.trim(), StandardCharsets.UTF_8));
        body.append("&response=").append(URLEncoder.encode(token, StandardCharsets.UTF_8));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            body.append("&idempotency_key=").append(URLEncoder.encode(idempotencyKey.trim(), StandardCharsets.UTF_8));
        }
        if (ip != null && !ip.isBlank()) {
            body.append("&remoteip=").append(URLEncoder.encode(ip, StandardCharsets.UTF_8));
        }
        HttpRequest httpReq = HttpRequest.newBuilder(URI.create(verifyUrl.trim()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = http.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.warn("Turnstile siteverify HTTP {}", res.statusCode());
            return null;
        }
        return objectMapper.readTree(res.body() != null ? res.body() : "{}");
    }

    private boolean isTrustedTurnstileHost(String hostname) {
        if (TurnstileHostPolicy.isTrustedAdminHost(hostname)) {
            return true;
        }
        if (orgPortalHostService == null || hostname == null || hostname.isBlank()) {
            return false;
        }
        return orgPortalHostService.findPortalOrgByAdminWebHost(hostname.trim()).isPresent();
    }

    private static boolean isSuccess(JsonNode json) {
        return json != null && json.path("success").asBoolean(false);
    }

    private static boolean isHostnameMismatchOnly(List<String> codes) {
        if (codes == null || codes.size() != 1) {
            return false;
        }
        String c = codes.get(0).trim().toLowerCase(Locale.ROOT);
        return "hostname-mismatch".equals(c) || "hostname_mismatch".equals(c);
    }

    private static boolean isTimeoutOrDuplicate(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        for (String raw : codes) {
            if (raw == null) {
                continue;
            }
            String c = raw.trim().toLowerCase(Locale.ROOT);
            if ("timeout-or-duplicate".equals(c) || "timeout_or_duplicate".equals(c)) {
                return true;
            }
        }
        return false;
    }

    static String requestHost(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String origin = header(request, "Origin");
        String fromOrigin = hostFromUrl(origin);
        if (!fromOrigin.isBlank()) {
            return fromOrigin;
        }
        String xfHost = header(request, "X-Forwarded-Host");
        if (xfHost != null && !xfHost.isBlank()) {
            String first = xfHost.split(",")[0].trim();
            int colon = first.indexOf(':');
            return colon > 0 ? first.substring(0, colon) : first;
        }
        String host = header(request, "Host");
        if (host != null && !host.isBlank()) {
            int colon = host.indexOf(':');
            return colon > 0 ? host.substring(0, colon) : host;
        }
        return "";
    }

    private static String hostFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String h = URI.create(url.trim()).getHost();
            return h != null ? h : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> errorCodes(JsonNode json) {
        List<String> out = new ArrayList<>();
        if (json == null) {
            return out;
        }
        JsonNode arr = json.path("error-codes");
        if (!arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            String s = n.asText("");
            if (s != null && !s.isBlank()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String cf = firstPublic(header(request, "CF-Connecting-IP"));
        if (cf != null) {
            return cf;
        }
        String trueClient = firstPublic(header(request, "True-Client-IP"));
        if (trueClient != null) {
            return trueClient;
        }
        String xff = header(request, "X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            for (String part : xff.split(",")) {
                String pub = firstPublic(part);
                if (pub != null) {
                    return pub;
                }
            }
        }
        String realIp = firstPublic(header(request, "X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return firstPublic(request.getRemoteAddr());
    }

    private static String firstPublic(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || !TurnstileHostPolicy.isPublicIp(t)) {
            return null;
        }
        return t;
    }

    private static String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return v != null ? v.trim() : null;
    }
}
