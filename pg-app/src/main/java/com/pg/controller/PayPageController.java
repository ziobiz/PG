package com.pg.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 공개 결제 페이지: {@code /pay.html?m=업체코드}.
 * 정적 파일 대신 서버에서 HTML을 내려 {@code m}을 본문에 주입 — API/JS가 실패해도 가맹점 코드가 보이도록 함.
 * <p>쿼리 {@code m} 이 있으면 중립 {@code /checkout/{m}} 으로 리다이렉트하여 EP·JPAY 등이
 * ChillPay {@code pay.html} 셸에 잘못 붙지 않게 합니다(내부 forward 는 루프 없음).
 */
@Controller
public class PayPageController {

    private static final Logger log = LoggerFactory.getLogger(PayPageController.class);
    private static final String PLACEHOLDER = "___PAY_M_FALLBACK___";

    @GetMapping(value = "/pay.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> payHtml(@RequestParam(required = false) String m,
                                          @RequestParam(required = false) String compId,
                                          @RequestParam(required = false) String merchant,
                                          @RequestParam(required = false) String pgCheckoutFwd) {
        String mid = firstNonBlank(m, firstNonBlank(compId, merchant));
        /* 브라우저가 pay.html?m= 로 직접 들어오면 중립 경로로 보냄(EP 등 올바른 결제창 forward).
         * /checkout → forward 시 pgCheckoutFwd=1 이면 루프 방지. */
        boolean fromNeutralFwd = "1".equals(pgCheckoutFwd) || "Y".equalsIgnoreCase(pgCheckoutFwd);
        if (!fromNeutralFwd && mid != null && !mid.isBlank()) {
            String enc = URLEncoder.encode(mid.trim(), StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/checkout/" + enc)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        try {
            Resource resource = new ClassPathResource("static/pay.html");
            byte[] bytes = resource.getInputStream().readAllBytes();
            String html = new String(bytes, StandardCharsets.UTF_8);
            String fallback = (mid != null && !mid.isBlank()) ? HtmlUtils.htmlEscape(mid.trim()) : "—";
            if (!html.contains(PLACEHOLDER)) {
                log.warn("static/pay.html 에 {} 플레이스홀더가 없습니다.", PLACEHOLDER);
            }
            html = html.replace(PLACEHOLDER, fallback);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .cacheControl(CacheControl.noStore())
                    .body(html);
        } catch (IOException e) {
            log.error("pay.html 로드 실패", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Payment page unavailable.");
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    @GetMapping(value = "/jpay-pay.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> jpayPayHtml() {
        try {
            Resource resource = new ClassPathResource("static/jpay-pay.html");
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .cacheControl(CacheControl.noStore())
                    .body(html);
        } catch (IOException e) {
            log.error("jpay-pay.html 로드 실패", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("JPAY payment page unavailable.");
        }
    }

    @GetMapping(value = "/split-pay.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> splitPayHtml() {
        return serveStaticHtml("static/split-pay.html", "split-pay.html");
    }

    @GetMapping(value = "/split-pay-setup.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> splitPaySetupHtml() {
        return serveStaticHtml("static/split-pay-setup.html", "split-pay-setup.html");
    }

    @GetMapping(value = "/jpay-subscribe.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> jpaySubscribeHtml() {
        return serveStaticHtml("static/jpay-subscribe.html", "jpay-subscribe.html");
    }

    @GetMapping(value = "/eximbay-pay.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> eximbayPayHtml() {
        return serveStaticHtml("static/eximbay-pay.html", "eximbay-pay.html");
    }

    @GetMapping(value = "/elementpay-pay.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> elementpayPayHtml() {
        return serveStaticHtml("static/elementpay-pay.html", "elementpay-pay.html");
    }

    @GetMapping(value = "/eximbay-subscribe.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> eximbaySubscribeHtml() {
        return serveStaticHtml("static/eximbay-subscribe.html", "eximbay-subscribe.html");
    }

    private ResponseEntity<String> serveStaticHtml(String classpath, String logName) {
        try {
            Resource resource = new ClassPathResource(classpath);
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .cacheControl(CacheControl.noStore())
                    .body(html);
        } catch (IOException e) {
            log.error("{} 로드 실패", logName, e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Page unavailable.");
        }
    }
}
