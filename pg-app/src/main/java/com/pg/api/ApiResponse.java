package com.pg.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * REST API 공통 응답 포맷 (금융/결제 도메인 표준)
 * <p>클래스 전체 NON_NULL은 제거한다. {@code data}가 {@code Map}일 때 중첩 null 키 생략 등
 * 의도치 않은 직렬화 전파를 막는다. {@code data}에는 NON_NULL을 붙이지 않는다(Map 값 null 생략 방지).</p>
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(true);
        r.setData(data);
        return r;
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail(message, null);
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(false);
        r.setMessage(message);
        r.setErrorCode(errorCode);
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
