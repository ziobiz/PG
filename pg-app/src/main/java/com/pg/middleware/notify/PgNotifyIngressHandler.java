package com.pg.middleware.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotiMiddlewareRelayRequest;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.service.PgNotifyReceiveService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ChillPay·NOTI 등 PG에서 ICOPAY(노티 수신층)로 들어오는 공개 노티 본 처리.
 * {@link com.pg.controller.open.OpenPgNotifyController} 및 {@link NotifyMiddlewareController} 가 동일 구현을 공유합니다.
 */
@Component
public class PgNotifyIngressHandler {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyIngressHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PgNotifyReceiveService receiveService;

    public PgNotifyIngressHandler(PgNotifyReceiveService receiveService) {
        this.receiveService = receiveService;
    }

    public ResponseEntity<?> notiMiddlewareRelay(String token, String targetCode, String rawJson, HttpServletRequest req) {
        try {
            NotiMiddlewareRelayRequest relay = MAPPER.readValue(rawJson != null ? rawJson : "{}", NotiMiddlewareRelayRequest.class);
            NotifyReceiveOutcome out = receiveService.receiveNotiMiddlewareRelay(
                    token, targetCode, rawJson != null ? rawJson : "", relay, clientIp(req), req);
            return toResponse(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("FORBIDDEN");
        } catch (Exception e) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("BAD_REQUEST");
        }
    }

    public ResponseEntity<?> receiveGetByTarget(String token, String targetCode, HttpServletRequest req) {
        return handle(token, targetCode, queryStringToFormBody(req), "application/x-www-form-urlencoded", req);
    }

    public ResponseEntity<?> receiveGet(String token, HttpServletRequest req) {
        return handle(token, null, queryStringToFormBody(req), "application/x-www-form-urlencoded", req);
    }

    public ResponseEntity<?> receivePost(String token, HttpServletRequest req) throws IOException {
        return receiveByTarget(token, null, req);
    }

    public ResponseEntity<?> receivePostByTarget(String token, String targetCode, HttpServletRequest req) throws IOException {
        return receiveByTarget(token, targetCode, req);
    }

    private ResponseEntity<?> receiveByTarget(String token, String targetCode, HttpServletRequest req) throws IOException {
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
            return toResponse(out);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("FORBIDDEN");
        } catch (Exception e) {
            log.warn("pg-notify handle 실패 token={} target={}: {}", token, targetCode, e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"processed\":false,\"retryable\":true,\"errorCode\":\"NOTIFY_ERROR\"}");
        }
    }

    private static ResponseEntity<?> toResponse(NotifyReceiveOutcome out) {
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
        return ResponseEntity.status(out.responseStatus()).contentType(mt).body(resp);
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

    public static String clientIp(HttpServletRequest req) {
        String x = req.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            return x.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
