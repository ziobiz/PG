# ILK(PayPay QR) — NOTI 경유 (ChillPay·EP·ox 동일 방식)

## 원칙

모든 결제대행사 Back Noti 는 **PG → NOTI → ICOPAY → 가맹 Callback** 을 유지합니다.  
ILK(카드·**QR/PayPay**)도 예외 없이 동일합니다.

## 가맹 시나리오

1. 가맹이 이미 **ElementPay(카드)** 사용 → NOTI에 Comp-Id·Callback 등록됨  
2. **PayPay QR** 추가 → ICOPAY 업체에 **ILK** 결제대행사 행 추가, QR(PayPay)만 수신  
3. 결제 결과 가맹 통보 = **기존 Callback URL·JPAY 호환 스키마** (카드와 동일)

## URL

| 구간 | URL |
|---|---|
| ILK → NOTI | `POST https://noti.icopay.net/noti/ilk` |
| NOTI → ICOPAY | `…/api/middleware/notify/v1/pg-notify/{token}/ILK` |
| 가맹 Callback | NOTI가 Comp-Id로 기존 EP/JPAY 슬롯 매칭 후 릴레이 |

## NOTI 설정

`config/ilk-ingress.json` (예: `config/ilk-ingress.example.json`)

- `icopayNotifyUrl` — ICOPAY `…/ILK` 수신 URL  
- 가맹 **별도 ILK 등록 불필요** — 동일 Comp-Id의 EP/JPAY 슬롯 Callback 재사용

## ICOPAY

- `IlkPaymentService` — `processingInformation.notifyUrl` = NOTI `/noti/ilk`  
- `IlkCallbackService` — ACK 헤더 `X-Icopay-Comp-Id` / `X-Icopay-Order-No`  
- 가맹 「결제통보 URL」 아웃바운드(`MerchantOutboundNotifyService`)는 EP와 동일하게 유지
