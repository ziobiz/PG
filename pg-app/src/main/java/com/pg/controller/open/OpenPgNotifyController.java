package com.pg.controller.open;

import com.pg.dto.NotifyReceiveOutcome;
import com.pg.service.PgNotifyReceiveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 외부 시스템(NOTI, ChillPay 등)에서 호출하는 공개 노티 수신 URL.
 * 경로 토큰은 본사설정 노티구성설정에서 확인합니다.
 * <p>
 * CALLBACK({@code cb…})·RESULT({@code rs…}) 대상코드 모두, 브라우저 GET·폼 POST 시 {@code pay-result.html} 로 303 리다이렉트합니다.
 * JSON 본문 POST(서버·미들웨어 노티)는 기존과 같이 OK JSON 을 반환합니다.
 */
@RestController
@RequestMapping("/api/open/pg-notify")
public class OpenPgNotifyController {

    private final PgNotifyReceiveService receiveService;

    public OpenPgNotifyController(PgNotifyReceiveService receiveService) {
        this.receiveService = receiveService;
    }

    @GetMapping("/{token}/{targetCode}")
    public ResponseEntity<?> receiveGetByTarget(@PathVariable String token, @PathVariable String targetCode,
                                                  HttpServletRequest req) {
        return handle(token, targetCode, queryStringToFormBody(req), "application/x-www-form-urlencoded", req);
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> receiveGet(@PathVariable String token, HttpServletRequest req) {
        return handle(token, null, queryStringToFormBody(req), "application/x-www-form-urlencoded", req);
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> receive(@PathVariable String token, HttpServletRequest req) throws IOException {
        return receiveByTarget(token, null, req);
    }

    @PostMapping("/{token}/{targetCode}")
    public ResponseEntity<?> receiveByTarget(@PathVariable String token, @PathVariable(required = false) String targetCode,
                                            HttpServletRequest req) throws IOException {
        byte[] buf = req.getInputStream().readAllBytes();
        String body = new String(buf, StandardCharsets.UTF_8);
        String effectiveCt = req.getContentType();
        String effectiveBody = body;
        if ((effectiveBody == null || effectiveBody.isBlank()) && !req.getParameterMap().isEmpty()) {
            effectiveBody = queryStringToFormBody(req);
            effectiveCt = "application/x-www-form-urlencoded";
        }
        return handle(token, targetCode, effectiveBody != null ? effectiveBody : "", effectiveCt, req);
    }

    private ResponseEntity<?> handle(String token, String targetCode, String body, String contentType, HttpServletRequest req) {
        try {
            NotifyReceiveOutcome out = receiveService.receiveAndRespond(
                    token, targetCode, body, contentType, clientIp(req), req);
            if (out.isRedirect()) {
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .location(URI.create(out.redirectLocation()))
                        .build();
            }
            String resp = out.body();
            MediaType mt = MediaType.TEXT_PLAIN;
            if (resp != null && resp.trim().startsWith("{")) {
                mt = MediaType.APPLICATION_JSON;
            }
            return ResponseEntity.ok().contentType(mt).body(resp);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("FORBIDDEN");
        }
    }

    private static String queryStringToFormBody(HttpServletRequest req) {
        Map<String, String[]> pm = req.getParameterMap();
        if (pm.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : pm.entrySet()) {
            String k = e.getKey();
            if (k == null) {
                continue;
            }
            for (String v : e.getValue()) {
                if (v == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private static String clientIp(HttpServletRequest req) {
        String x = req.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            return x.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
