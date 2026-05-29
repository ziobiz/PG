# URL 결제 공통 플랫폼 가이드

## 목적

특정 PG사(ChillPay, JPAY 등)에만 묶이지 않고, **앞으로 추가되는 모든 결제대행사**가 동일한 본사 설정·고객 UX를 갖도록 URL 결제 기능을 **플랫폼 레이어**와 **PG 어댑터**로 분리합니다.

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  본사 URL결제설정 (HQ)                                       │
│  · urlPayFlow / urlPayFormMode                               │
│  · url_pay_display_fx_json (표시통화→실결제, MULTI/FIXED)     │
│  · 결제통화 로직 (PaymentCurrencyScale)                       │
│  · 결제문구 (UrlPayCardCopy)                                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ 모든 PG 동일 적용
┌───────────────────────────▼─────────────────────────────────┐
│  com.pg.urlpay (플랫폼)                                      │
│  · UrlPayPublicCheckoutService — checkout-context              │
│  · UrlPayChargeResolutionService — 결제 금액·통화            │
│  · UrlPayVendorCapabilityRegistry — PG별 가능 기능           │
│  · UrlPaySaleDispatcher — 승인 API 라우팅                    │
│  · site/js/url-pay-public-shell.js — 다국어·FX UI             │
└───────────────────────────┬─────────────────────────────────┘
                            │ PG별
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   ChillPay CCD        JPAY pay_index      (신규 PG)
   pay.html            jpay-pay.html       registry 등록
   direct-credit       jpay/sale
```

## 플랫폼 공통 (PG 수정 없이 자동)

| 기능 | 설명 |
|------|------|
| 다국어 | KOR/ENG/JPN/CHN/THA, `url-pay-public-shell.js` |
| 표시통화 FX | `UrlPayDisplayFxService`, `display-fx-quote` API |
| 결제통화 | `UrlPayCheckoutCurrencyService` + 스케일 |
| 결제문구 | `UrlPayCardCopyService` |
| URL 폼 모드 | FULL / SIMPLE (본사 결제로직설정) |

## PG 어댑터 (`UrlPayVendorCapability`)

| 필드 | 의미 |
|------|------|
| `inlineWidgetKind` | `CHILLPAY_CCD`, `JPAY_INLINE`, `UNSUPPORTED_INLINE`, … |
| `checkoutPagePath` | `/pay/`, `/jpay-pay/` |
| `saleChannel` | 승인 API (`CHILLPAY_DIRECT_CREDIT`, `JPAY_INLINE_SALE`, …) |
| `repayUrlEnabled` | **PG 재결제 API +** `tb_pg_agency.integ_url_pay_repay_yn=Y` |

**재결제 URL**: PG사 API에 저장 카드/재결제가 없으면 `repayUrlEnabled=false` (관리자에서 URL 비활성). ChillPay는 CCD 재결제 지원, JPAY는 현재 미지원.

## 공개 API (통합 + 하위 호환)

| 통합 (권장) | 하위 호환 |
|-------------|-----------|
| `GET /api/pay/url/checkout-context` | `/chillpay/checkout-context`, `/jpay/checkout-context` |
| `GET /api/pay/url/display-fx-quote` | `/chillpay/display-fx-quote` |
| `POST /api/pay/url/sale` | `/jpay/sale` (JPAY), ChillPay는 `direct-credit` |

`checkout-context` 응답에 **`urlPayCapabilities`** 객체가 포함됩니다. 관리자·프론트는 `repayUrlEnabled`, `checkoutPagePath` 등으로 UI를 분기합니다.

**운영 배포:** `SecurityConfig` 에 `/api/pay/url/**` 가 `permitAll` 이어야 공개 결제 페이지(`jpay-pay.html` 등)에서 `fetch` 시 **HTTP 401** 이 나지 않습니다. (ChillPay·JPAY 하위 경로만 열려 있고 통합 경로가 빠지면 401 발생)

## 신규 PG 추가 절차 (요약)

1. `PgVendor` + `tb_pg_agency` 연동용도 플래그  
2. `UrlPayVendorCapabilityRegistry` 에 분기 추가  
3. 승인 서비스 + `UrlPaySaleDispatcher` case  
4. (선택) `UrlPayCheckoutContextEnricher`  
5. 결제 HTML 페이지 또는 공통 셸 재사용  
6. 문서·테스트: 본사 URL결제설정 DISPLAY/CHECKOUT 각 1건  

## 관련 코드

- Java: `pg-app/src/main/java/com/pg/urlpay/`
- 정적 셸: `site/js/url-pay-public-shell.js`
- ChillPay 페이지: `site/pay.html`
- JPAY 페이지: `site/jpay-pay.html`
