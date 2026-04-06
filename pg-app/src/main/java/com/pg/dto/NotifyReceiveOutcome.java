package com.pg.dto;

/**
 * PG 노티 수신 응답: JSON 본문 또는 브라우저용 리다이렉트(RESULT URL 등).
 */
public record NotifyReceiveOutcome(String body, String redirectLocation) {

    public boolean isRedirect() {
        return redirectLocation != null && !redirectLocation.isBlank();
    }

    public static NotifyReceiveOutcome json(String body) {
        return new NotifyReceiveOutcome(body != null ? body : "{\"result\":\"OK\"}", null);
    }

    public static NotifyReceiveOutcome redirect(String location) {
        return new NotifyReceiveOutcome(null, location);
    }
}
