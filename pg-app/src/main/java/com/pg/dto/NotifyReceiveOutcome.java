package com.pg.dto;

import org.springframework.http.HttpStatus;

/**
 * PG 노티 수신 응답: JSON 본문 또는 브라우저용 리다이렉트(RESULT URL 등).
 * 서버-투-서버 노티는 {@link #httpStatus} 로 성공(2xx)·재시도 유도(4xx/5xx)를 구분합니다.
 */
public record NotifyReceiveOutcome(String body, String redirectLocation, HttpStatus httpStatus) {

    public NotifyReceiveOutcome {
        if (redirectLocation != null && !redirectLocation.isBlank()) {
            body = null;
            httpStatus = null;
        }
    }

    public boolean isRedirect() {
        return redirectLocation != null && !redirectLocation.isBlank();
    }

    /** 컨트롤러용: 리다이렉트면 303, 아니면 본문 응답용 상태(기본 200). */
    public HttpStatus responseStatus() {
        if (isRedirect()) {
            return HttpStatus.SEE_OTHER;
        }
        return httpStatus != null ? httpStatus : HttpStatus.OK;
    }

    public static NotifyReceiveOutcome json(String body) {
        return new NotifyReceiveOutcome(body != null ? body : "{\"result\":\"OK\"}", null, HttpStatus.OK);
    }

    public static NotifyReceiveOutcome json(String body, HttpStatus status) {
        return new NotifyReceiveOutcome(body, null, status != null ? status : HttpStatus.OK);
    }

    public static NotifyReceiveOutcome redirect(String location) {
        return new NotifyReceiveOutcome(null, location, null);
    }
}
