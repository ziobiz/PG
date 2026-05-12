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
 * 가맹점 홈페이지·쇼핑몰에 삽입하는 챗봇 플로팅 위젯 부트스트랩.
 * {@code <script src=".../v1/embed-chatbot/{compId}">} 한 줄로 로드되며,
 * 동일 오리진의 {@code /js/icopay-embed-chatbot-widget.js} 를 이어서 로드합니다.
 */
@RestController
public class EmbedChatbotWidgetController {

    private static final Pattern COMP_ID_SAFE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/v1/embed-chatbot/{compId}", produces = "application/javascript;charset=UTF-8")
    public ResponseEntity<String> bootstrap(@PathVariable("compId") String compId) throws JsonProcessingException {
        if (compId == null || !COMP_ID_SAFE.matcher(compId).matches()) {
            String err = "console&&console.error&&console.error('[ICOPAY] invalid embed compId');";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.valueOf("application/javascript;charset=UTF-8"))
                    .cacheControl(CacheControl.noStore())
                    .body(err);
        }
        String jsonId = objectMapper.writeValueAsString(compId);
        String body = "(function(){"
                + "var cur=document.currentScript;"
                + "if(!cur||!cur.src){console.error('[ICOPAY] embed: no currentScript');return;}"
                + "var u=new URL(cur.src);"
                + "var origin=u.origin;"
                + "window.__ICOPAY_EMBED_CHATBOT__={compId:" + jsonId + ",origin:origin};"
                + "var s=document.createElement('script');"
                + "s.src=origin+'/js/icopay-embed-chatbot-widget.js?v=4';"
                + "s.async=true;"
                + "s.defer=true;"
                + "s.charset='utf-8';"
                + "document.head.appendChild(s);"
                + "})();";
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript;charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(body);
    }
}
