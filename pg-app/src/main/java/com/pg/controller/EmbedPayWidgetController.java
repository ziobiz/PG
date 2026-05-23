package com.pg.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 가맹점 쇼핑몰에 삽입하는 ChillPay 인라인 결제 iframe 위젯 부트스트랩.
 * {@code <script src=".../v1/embed-pay/{compId}" data-session-token="...">} 한 줄로 로드합니다.
 */
@RestController
public class EmbedPayWidgetController {

    private static final Pattern COMP_ID_SAFE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/v1/embed-pay/{compId}", produces = "application/javascript;charset=UTF-8")
    public ResponseEntity<String> bootstrap(@PathVariable("compId") String compId) throws JsonProcessingException {
        if (compId == null || !COMP_ID_SAFE.matcher(compId).matches()) {
            String err = "console&&console.error&&console.error('[ICOPAY] invalid embed-pay compId');";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.valueOf("application/javascript;charset=UTF-8"))
                    .cacheControl(CacheControl.noStore())
                    .body(err);
        }
        String jsonId = objectMapper.writeValueAsString(compId);
        String body = "(function(){"
                + "var cur=document.currentScript;"
                + "if(!cur||!cur.src){console.error('[ICOPAY] embed-pay: no currentScript');return;}"
                + "var u=new URL(cur.src);"
                + "var origin=u.origin;"
                + "window.__ICOPAY_EMBED_PAY__={compId:" + jsonId + ",origin:origin,script:cur};"
                + "var ls=document.createElement('script');"
                + "ls.src=origin+'/js/icopay-checkout-lang.js?v=2';"
                + "ls.charset='utf-8';"
                + "ls.onload=function(){"
                + "var s=document.createElement('script');"
                + "s.src=origin+'/js/icopay-embed-pay-widget.js?v=2';"
                + "s.async=true;s.defer=true;s.charset='utf-8';document.head.appendChild(s);"
                + "};"
                + "document.head.appendChild(ls);"
                + "})();";
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript;charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(body);
    }
}
