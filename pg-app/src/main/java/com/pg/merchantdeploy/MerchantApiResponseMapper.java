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
            return ResponseEntity.ok(failFromResultMap(result, "request failed", "ERROR"));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** {@code success=false} flat map (sale·prepare guard 등) → fail 응답 */
    @SuppressWarnings("unchecked")
    public static ApiResponse<Map<String, Object>> failFromResultMap(Map<String, Object> result,
                                                                     String defaultMsg,
                                                                     String defaultCode) {
        String msg = result.get("message") != null ? result.get("message").toString() : defaultMsg;
        String code = result.get("errorCode") != null ? result.get("errorCode").toString().trim() : defaultCode;
        if (code.isEmpty()) {
            code = defaultCode;
        }
        Object messageKey = result.get("messageKey");
        Object messagesObj = result.get("messages");
        if (messageKey != null && messagesObj instanceof Map<?, ?> messages) {
            return ApiResponse.failI18n(msg, code, messageKey.toString(), (Map<String, String>) messages);
        }
        return ApiResponse.fail(msg, code);
    }
}
