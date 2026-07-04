package com.pg.merchantdeploy;

import com.pg.api.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** 가맹점 API 서비스 결과 → {@link ApiResponse} (다국어 오류 포함). */
public final class MerchantApiResponseMapper {

    /** 가맹점에게 통일 노출할 브랜드. 실제 PG(ChillPay/JPAY/Eximbay)는 절대 노출하지 않는다. */
    public static final String MERCHANT_FACING_BRAND = "ICOPAY";

    private MerchantApiResponseMapper() {
    }

    public static ResponseEntity<ApiResponse<Map<String, Object>>> mapServiceResult(Map<String, Object> result) {
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ResponseEntity.ok(failFromResultMap(result, "request failed", "ERROR"));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        neutralizePgIdentity(data);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * 가맹점 응답에서 실제 결제 대행사 식별 정보를 제거·중립화한다.
     * 어떤 통합 API 경로로 들어와도(통합·레거시 PG별) 가맹점은 항상 ICOPAY 만 보게 된다.
     */
    @SuppressWarnings("unchecked")
    public static void neutralizePgIdentity(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        // PG 벤더는 항상 ICOPAY 로 통일 노출
        if (data.containsKey("pgVendor")) {
            data.put("pgVendor", MERCHANT_FACING_BRAND);
        }
        // 내부 PG 코드·PG 전용 상태값은 가맹점에 노출하지 않는다
        data.remove("operationalPgCd");
        data.remove("jpayStatus");
        data.remove("van");
        // 중첩 data(있을 경우)도 동일 처리
        Object nested = data.get("data");
        if (nested instanceof Map<?, ?>) {
            neutralizePgIdentity((Map<String, Object>) nested);
        }
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
