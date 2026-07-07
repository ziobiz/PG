package com.pg.middleware.notify;

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
 * <strong>노티 미들웨어(ICOPAY 수신층)</strong> 공개 URL.
 * ChillPay·ElementPay·NOTI 등에서 등록하는 콜백 주소로 이 베이스({@code /api/middleware/notify/v1/pg-notify})를 쓰면,
 * 레거시 경로({@code /api/open/pg-notify})와 <strong>동일한</strong> {@link PgNotifyReceiveService} 처리·가맹점 아웃바운드 분기가 적용됩니다.
 */
@RestController
@RequestMapping("/api/middleware/notify/v1/pg-notify")
public class NotifyMiddlewareController {

    private final PgNotifyIngressHandler ingressHandler;

    public NotifyMiddlewareController(PgNotifyIngressHandler ingressHandler) {
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
