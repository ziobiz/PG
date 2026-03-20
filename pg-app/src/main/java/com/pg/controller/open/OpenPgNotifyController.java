package com.pg.controller.open;

import com.pg.service.PgNotifyReceiveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 외부 시스템(NOTI, ChillPay 등)에서 호출하는 공개 노티 수신 URL.
 * 경로 토큰은 본사설정 > 전산노티·결제환경에서 확인합니다.
 */
@RestController
@RequestMapping("/api/open/pg-notify")
public class OpenPgNotifyController {

    private final PgNotifyReceiveService receiveService;

    public OpenPgNotifyController(PgNotifyReceiveService receiveService) {
        this.receiveService = receiveService;
    }

    @PostMapping("/{token}")
    public ResponseEntity<String> receive(@PathVariable String token, HttpServletRequest req) throws IOException {
        return receiveByTarget(token, null, req);
    }

    @PostMapping("/{token}/{targetCode}")
    public ResponseEntity<String> receiveByTarget(@PathVariable String token, @PathVariable(required = false) String targetCode, HttpServletRequest req) throws IOException {
        byte[] buf = req.getInputStream().readAllBytes();
        String body = new String(buf, StandardCharsets.UTF_8);
        try {
            String resp = receiveService.receiveAndRespond(token, body, req.getContentType(), clientIp(req));
            MediaType mt = MediaType.TEXT_PLAIN;
            if (resp != null && resp.trim().startsWith("{")) {
                mt = MediaType.APPLICATION_JSON;
            }
            return ResponseEntity.ok().contentType(mt).body(resp);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("FORBIDDEN");
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String x = req.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            return x.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
