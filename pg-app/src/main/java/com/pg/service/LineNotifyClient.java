package com.pg.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * LINE Notify HTTP API — 가맹점 알림 토큰 또는 고객이 발급한 개인 Notify 토큰에 동일하게 사용합니다.
 */
@Service
public class LineNotifyClient {

    private static final String NOTIFY_URL = "https://notify-api.line.me/api/notify";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * @param bearerToken LINE Notify 발급 토큰(앞뒤 공백 제거 후 Bearer 로 전송)
     * @param message 본문(1000자 제한 — API 규격에 맞춰 약 950자로 절단)
     */
    public void postNotify(String bearerToken, String message) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("LINE Notify 토큰이 비어 있습니다.");
        }
        String msg = message != null ? message : "";
        if (msg.length() > 950) {
            msg = msg.substring(0, 947) + "...";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(bearerToken.trim());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("message", msg);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        restTemplate.postForEntity(NOTIFY_URL, entity, String.class);
    }
}
