package com.pg.config;

import com.pg.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST API({@code com.pg.controller.api}) 미처리 예외를 HTML Whitelabel 대신 JSON(ApiResponse)으로 반환합니다.
 */
@RestControllerAdvice(basePackages = "com.pg.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(Exception ex) {
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : ex.getClass().getSimpleName();
        return ResponseEntity.ok(ApiResponse.fail(msg, "SERVER_ERROR"));
    }
}
