package com.pg.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * REST API 공통 응답 포맷 (금융/결제 도메인 표준)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
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
