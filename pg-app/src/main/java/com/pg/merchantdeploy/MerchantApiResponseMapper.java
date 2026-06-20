package com.pg.merchantdeploy;

import com.pg.api.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** 가맹점 API 서비스 결과 → {@link ApiResponse} (다국어 오류 포함). */
public final class MerchantApiResponseMapper {

    private MerchantApiResponseMapper() {
    }

    public static ResponseEntity<ApiResponse<Map<String, Object>>> mapServiceResult(Map<String, Object> result) {
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            String msg = result.get("message") != null ? result.get("message").toString() : "request failed";
            String code = result.get("errorCode") != null ? result.get("errorCode").toString() : "ERROR";
            Object messageKey = result.get("messageKey");
            Object messagesObj = result.get("messages");
            if (messageKey != null && messagesObj instanceof Map<?, ?> messages) {
                @SuppressWarnings("unchecked")
                Map<String, String> msgMap = (Map<String, String>) messages;
                return ResponseEntity.ok(ApiResponse.failI18n(
                        msg, code, messageKey.toString(), msgMap));
            }
            return ResponseEntity.ok(ApiResponse.fail(msg, code));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
