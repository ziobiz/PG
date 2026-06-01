ICOPAY 통합 checkout — JSON(REST) 연동 샘플
============================================

키트 JSON의 integrationModes.json 과 함께 사용하세요.

파일
----
unified-prepare-request.json     — POST /merchant/checkout/prepare 요청 본문 예시
unified-prepare-response.example.json — prepare 성공 응답 예시(필드 설명용)

호출
----
POST {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/prepare
Header: Content-Type: application/json
        Accept: application/json
        X-Icopay-Merchant-Broker-Secret: {brokerSecret}  (강제 시)

buyer.email · buyer.phone · buyer.countryIso2 필수.

응답 data.sessionToken → 브라우저에 /v1/embed-checkout/{compId} 스크립트만 전달.

상태 조회
---------
GET {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/status?compId=&orderNo=

PHP 연동은 ../php/checkout_unified.php 를 참고하세요.
