=== ICOPAY WooCommerce ===
Contributors: icopay
Tags: woocommerce, payment, icopay, chillpay, jpay, credit card
Requires at least: 6.0
Tested up to: 6.7
Requires PHP: 7.4
Stable tag: 1.1.0
License: GPLv2 or later
License URI: https://www.gnu.org/licenses/gpl-2.0.html

ICOPAY URL 인라인·리다이렉트 결제(ChillPay/JPAY)를 WooCommerce 결제 수단으로 연동합니다.

== Description ==

* WooCommerce 체크아웃에서 ICOPAY inline-checkout 또는 redirect-checkout API 사용
* compId + Broker Secret + API Base URL 만 설정 (ChillPay MID/ApiKey는 ICOPAY HQ에 등록)
* Inline: 결제 iframe embed + postMessage + REST 웹훅으로 주문 완료
* Redirect: ICOPAY pay 페이지로 이동 후 복귀 URL(`wc-api=icopay_return`)에서 상태 확인

== Installation ==

1. `icopay-woocommerce` 폴더를 `/wp-content/plugins/` 에 업로드하거나 ZIP으로 설치
2. WordPress 관리자 → 플러그인 → ICOPAY WooCommerce 활성화
3. WooCommerce → 설정 → 결제 → ICOPAY (URL Inline) 설정
4. ICOPAY HQ 「가맹점 API 생성」에서 받은 compId, broker secret, API URL 입력
5. HQ 가맹 통보 URL에 플러그인에 표시된 Webhook URL 등록

== HQ 선행 설정 ==

* 가맹 웹결제 Y, ChillPay/JPAY URL결제 Y
* Inline: URL INLINE Y (ChillPay) 또는 API 중계형 INLINE Y (JPAY)
* Redirect: API 중계형 REDIRECT Y 또는 URL 결제형 REDIRECT Y

== Frequently Asked Questions ==

= ChillPay 키를 WooCommerce에 넣나요? =

아니오. compId와 broker secret만 입력합니다. PG 자격은 ICOPAY 본사/가맹 PG 바인딩에 있습니다.

= 웹훅은 필수인가요? =

주문 자동 완료를 위해 HQ merchantNotifyUrls 등록을 권장합니다. postMessage + status API 폴링도 보조로 동작합니다.

= Inline과 Redirect 중 무엇을 써야 하나요? =

기본값은 Inline(v1.0과 동일)입니다. Redirect는 고객이 ICOPAY 결제 페이지로 이동하는 방식입니다.

== Changelog ==

= 1.1.0 =
* icopay-core 공유 라이브러리 연동 (inline + redirect prepare/status)
* Checkout flow 설정 추가 (기본 inline — v1.0 동작 유지)
* Redirect 복귀 핸들러 `wc-api=icopay_return`
* JPAY/ChillPay redirect-checkout API 경로 지원

= 1.0.0 =
* Initial release: ChillPay/JPAY inline checkout gateway
