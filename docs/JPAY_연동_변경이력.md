# JPAY WordPress 연동 변경 이력

| 항목 | 내용 |
|------|------|
| **범위** | `icopay-core`, `icopay-jpay`, `icopay-woocommerce` |
| **저장소 경로** | `wordpress/`, `woocommerce/` |

---

## icopay-core (공유)

| 버전 | 날짜 | 변경 |
|------|------|------|
| 1.0.0 | 2026-06 | 최초 추가 — `ICOPAY_Flow` (INLINE/REDIRECT), `ICOPAY_Core_Api_Client` (JPAY/ChillPay/unified prepare·status) |

### API 경로

| Vendor | Inline prepare | Redirect prepare |
|--------|----------------|------------------|
| JPAY | `/merchant/jpay/inline-checkout/prepare` | `/merchant/jpay/redirect-checkout/prepare` |
| ChillPay | `/merchant/chillpay/inline-checkout/prepare` | `/merchant/chillpay/redirect-checkout/prepare` |
| Unified | `/merchant/checkout/prepare` | `/merchant/checkout/redirect/prepare` |

Status 경로는 prepare와 동일 prefix + `/status`.

---

## icopay-jpay v1.0.0

| 항목 | 내용 |
|------|------|
| **용도** | 일반 WordPress (WooCommerce 불필요) |
| **기본 flow** | `inline` |
| **숏코드** | `[icopay_jpay]` |
| **웹훅** | `POST /wp-json/icopay-jpay/v1/webhook` |
| **라우트** | `/icopay-jpay/start|pay|return/` |

### 파일

- `icopay-jpay.php` — bootstrap, core 로드
- `includes/class-icopay-jpay-settings.php` — 설정 페이지
- `includes/class-icopay-jpay-payment.php` — inline/redirect 결제
- `includes/class-icopay-jpay-webhook.php` — REST 웹훅
- `includes/class-icopay-jpay-shortcode.php` — 숏코드

---

## icopay-woocommerce v1.1.0

| 항목 | v1.0.0 | v1.1.0 |
|------|--------|--------|
| flow_mode | inline 고정 | 설정 추가 (기본 **inline**) |
| icopay-core | 내장 Api_Client | core 상속 + 번들 |
| Redirect | — | prepare redirect + `wc-api=icopay_return` |
| 주문 메타 | orderNo, session | + `_icopay_flow_mode` |

### v1.0 호환

`flow_mode=inline`(기본) 선택 시 v1.0과 동일:

- `inline-checkout/prepare` → sessionToken
- `wc-api=icopay_pay` iframe 페이지
- postMessage + AJAX status + 웹훅

### v1.1.0 추가 파일/변경

- `icopay-woocommerce.php` — v1.1.0, `icopay_wc_load_core()`
- `includes/class-icopay-api-client.php` — `extends ICOPAY_Core_Api_Client`
- `includes/class-wc-gateway-icopay.php` — `flow_mode` 설정·redirect 분기
- `includes/class-icopay-payment-page.php` — `icopay_return` 핸들러
- `includes/class-icopay-order-helper.php` — `META_FLOW`

---

## 빌드

| 스크립트 | 산출물 |
|----------|--------|
| `tools/build-wp-plugin-zips.ps1` | `icopay-woocommerce-1.1.0.zip`, `icopay-jpay-1.0.0.zip` |

각 ZIP에 `includes/icopay-core/` 번들 포함.

---

## 문서

| 파일 | 설명 |
|------|------|
| `WordPress_JPAY_플러그인_배포가이드.md` | icopay-jpay 설치·설정 |
| `JPAY_리다이렉트_배포가이드.md` | Redirect flow·API·HQ 설정 |
| `JPAY_연동_변경이력.md` | 본 문서 |

---

## 향후

- 통합 checkout(unified) WordPress UI
- WooCommerce buyer prefill (billing → prepare)
- icopay-core Composer 패키지 분리 (선택)
