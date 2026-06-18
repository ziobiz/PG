# WordPress JPAY 플러그인 배포 가이드

| 항목 | 내용 |
|------|------|
| **대상** | 일반 WordPress 사이트 (WooCommerce 불필요) |
| **플러그인** | `icopay-jpay` v1.0.0 |
| **공유 라이브러리** | `icopay-core` (ZIP 빌드 시 플러그인에 번들) |
| **API** | `https://api.icopay.co.kr` (운영 기준) |

---

## 1. 패키지 구성

| 구성요소 | 설명 |
|----------|------|
| `wordpress/icopay-jpay/` | JPAY 전용 WP 플러그인 |
| `wordpress/icopay-core/` | inline/redirect API 클라이언트 (공유) |
| `tools/build-wp-plugin-zips.ps1` | icopay-core 번들 ZIP 생성 |

빌드:

```powershell
cd D:\Delopment\PG
.\tools\build-wp-plugin-zips.ps1
```

산출물: `wordpress/icopay-jpay-1.0.0.zip`

---

## 2. WordPress 설치

1. WordPress 관리자 → **플러그인 → 새로 추가 → 플러그인 업로드**
2. `icopay-jpay-1.0.0.zip` 업로드 후 **활성화**
3. 플러그인 활성화 시 rewrite rule 등록 — **설정 → 고유주소**에서 한 번 **저장** (Permalink flush)

---

## 3. ICOPAY HQ 선행 설정

관리자: `https://icopay.co.kr`

| 항목 | Inline | Redirect |
|------|--------|----------|
| 가맹 웹결제 | Y | Y |
| JPAY URL 결제 바인딩 | 운영 WEB PG | 운영 WEB PG |
| API 중계형 INLINE | **Y** | — |
| API 중계형 REDIRECT | — | **Y** |
| 가맹점 API 생성 | compId + broker secret | 동일 |

상세: `JPAY_URL결제_인라인API_배포가이드.md`, `JPAY_리다이렉트_배포가이드.md`

---

## 4. 플러그인 설정

**설정 → ICOPAY JPAY**

| 필드 | 설명 |
|------|------|
| compId | 본사 발급 업체코드 |
| API Base URL | `https://api.icopay.co.kr` |
| Broker secret | `X-Icopay-Merchant-Broker-Secret` (서버 전용) |
| Checkout flow | **Inline**(기본) 또는 Redirect |
| Return page | Redirect 복귀 후 이동할 WP 페이지 |
| Webhook sign secret | HQ HMAC 설정 시 동일 값 |
| Webhook URL | HQ merchantNotifyUrls 에 등록 |

---

## 5. 숏코드

```
[icopay_jpay amount="100" currency="USD" product="Sample product"]
```

| 속성 | 필수 | 설명 |
|------|------|------|
| amount | Y | 결제 금액 |
| currency | O | 기본 USD |
| product | O | 상품명 |
| order_no | O | 미입력 시 자동 생성 (≤20자) |
| button | O | 버튼 문구 |

---

## 6. 결제 흐름

### Inline (기본)

1. 숏코드 폼 POST → `/icopay-jpay/start/`
2. 서버 `jpay/inline-checkout/prepare` → sessionToken
3. `/icopay-jpay/pay/?order_no=…` 에 embed iframe
4. postMessage `ICOPAY_INLINE_CHECKOUT` 완료 → return URL
5. REST 웹훅 병행 (`/wp-json/icopay-jpay/v1/webhook`)

### Redirect

1. 숏코드 폼 POST → prepare `jpay/redirect-checkout/prepare` + returnUrl
2. 브라우저 ICOPAY `payUrl` 로 이동
3. 결제 후 returnUrl (`/icopay-jpay/return/`) → 설정한 Return page

---

## 7. 검수 체크리스트

- [ ] compId·broker secret 설정
- [ ] HQ 웹훅 URL 등록
- [ ] Inline: 테스트 결제 → return page 또는 쿼리 `icopay_status=paid`
- [ ] Redirect: pay 페이지 이동·복귀·status PAID
- [ ] Permalink flush 완료

---

## 8. 관련 문서

- `JPAY_리다이렉트_배포가이드.md`
- `JPAY_연동_변경이력.md`
- `가맹점_JPAY_API_연동가이드.md`
