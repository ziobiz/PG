# ICOPAY 가맹점 통합 Checkout API — 연동 흐름

| 항목 | 내용 |
|------|------|
| **문서 ID** | ICOPAY-CHECKOUT-FLOW-001 |
| **버전** | 2.0 |
| **연동 방식** | ICOPAY 통합 인라인 — 가맹에는 **ICOPAY만** 노출 |
| **관련** | [간단 연동 빠른 시작](./가맹점_ICOPAY_간단연동_빠른시작.md) · [파라미터 규격](./가맹점_통합Checkout_API_연동파라미터_규격.md) |

## 1. 개요

가맹점은 **하나의 prepare**만 호출합니다. 결제망 선택·처리는 ICOPAY가 담당하며, 응답·결제창·URL에는 결제대행사 이름이 나오지 않습니다.

| 항목 | 값 |
|------|-----|
| integrationMode | `INLINE_UNIFIED` |
| Prepare | `POST {BASE}/api/middleware/v1/merchant/checkout/prepare` |
| Embed | `{BASE}/v1/embed-checkout/{compId}` |
| payUrl | `{BASE}/checkout/{compId}?entry=merchant_api&embed=1&session=…` |
| pgVendor (응답) | 항상 `ICOPAY` |

## 2. 엔드포인트

| 엔드포인트 | Method | 호출 | 브로커 | 역할 |
|-----------|--------|------|--------|------|
| `…/merchant/checkout/prepare` | POST | 가맹 서버 | 필요 | 세션·sessionToken |
| `…/merchant/checkout/session?token=` | GET | 브라우저(embed) | 불필요 | 토큰 검증 |
| `…/merchant/checkout/status?compId=&orderNo=` | GET | 가맹 서버 | 필요 | 결제 결과 |
| `/v1/embed-checkout/{compId}` | GET(JS) | 브라우저 | 불필요 | iframe 부트 |

## 3. 권장 흐름

```
[가맹 서버]  주문 PENDING 저장
     → POST …/checkout/prepare (buyer 필수 + broker secret)
     → sessionToken
[브라우저]  embed 또는 iframe payUrl (/checkout/{compId})
     → postMessage ICOPAY_INLINE_CHECKOUT (참고)
[가맹 서버]  webhook 및/또는 GET …/status → PAID 확정 (멱등)
```

## 4. 보안

- 브로커 시크릿은 **서버만**
- `sessionToken`만 브라우저에 전달
- prepare에 가맹 return URL 금지 (복귀는 ICOPAY NOTI → 가맹 Result)

## 5. FAQ

**결제대행사 이름이 보이나요?**  
아니요. API·결제창·배포 문서는 ICOPAY만입니다.

**Session을 서버에서 호출하나요?**  
아니요. embed 위젯이 브라우저에서 호출합니다.

**postMessage만으로 확정해도 되나요?**  
UX 참고용. **최종 확정은 Status API 또는 Webhook.**
