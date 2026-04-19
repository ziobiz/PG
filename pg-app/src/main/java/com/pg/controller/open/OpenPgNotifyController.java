package com.pg.controller.open;

import com.pg.middleware.notify.PgNotifyIngressHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 외부 시스템(NOTI, ChillPay 등)에서 호출하는 공개 노티 수신 URL (레거시 베이스).
 * 신규 연동은 {@link com.pg.middleware.notify.NotifyMiddlewareController} 의
 * {@code /api/middleware/notify/v1/pg-notify} 베이스를 권장합니다. 처리 로직은 동일합니다.
 */
@RestController
@RequestMapping("/api/open/pg-notify")
public class OpenPgNotifyController {

    private final PgNotifyIngressHandler ingressHandler;

    public OpenPgNotifyController(PgNotifyIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @PostMapping(value = "/{token}/noti-middleware-relay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notiMiddlewareRelay(@PathVariable String token,
                                                 @RequestBody String rawJson,
                                                 HttpServletRequest req) {
        return ingressHandler.notiMiddlewareRelay(token, null, rawJson, req);
    }

    @PostMapping(value = "/{token}/{targetCode}/noti-middleware-relay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notiMiddlewareRelayWithTarget(@PathVariable String token,
                                                           @PathVariable String targetCode,
                                                           @RequestBody String rawJson,
                                                           HttpServletRequest req) {
        return ingressHandler.notiMiddlewareRelay(token, targetCode, rawJson, req);
    }

    @GetMapping("/{token}/{targetCode}")
    public ResponseEntity<?> receiveGetByTarget(@PathVariable String token, @PathVariable String targetCode,
                                                  HttpServletRequest req) {
        return ingressHandler.receiveGetByTarget(token, targetCode, req);
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> receiveGet(@PathVariable String token, HttpServletRequest req) {
        return ingressHandler.receiveGet(token, req);
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> receive(@PathVariable String token, HttpServletRequest req) throws IOException {
        return ingressHandler.receivePost(token, req);
    }

    @PostMapping("/{token}/{targetCode}")
    public ResponseEntity<?> receiveByTarget(@PathVariable String token, @PathVariable(required = false) String targetCode,
                                            HttpServletRequest req) throws IOException {
        return ingressHandler.receivePostByTarget(token, targetCode, req);
    }
}
