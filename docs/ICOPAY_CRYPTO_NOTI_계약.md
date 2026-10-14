# CRYPTO — ICOPAY ↔ NOTI ↔ 가맹 통보 계약

가맹·구매자 노출은 항상 `ICOPAY`. 병칭 채널 코드 `crypto` / `CRYPTO`.

## 목표

카드(JPAY/ElementPay/ox)와 **동일하게** 가맹 Callback/Result는 **NOTI가 릴레이**한다.

```
구매자 입금 → ICOPAY confirmDeposit
           → ICOPAY POST https://noti.icopay.net/noti/crypto
           → NOTI가 Comp-Id로 가맹 매칭
           → 가맹 callbackUrl (기존 JPAY/EP/ox 등록분 그대로)
```

- **결제통보 URL(Background/Result) 직통은 사용하지 않음** (기존 NOTI 연동 가맹 호환).
- NOTI → ICOPAY 재전달 없음 (ICOPAY가 이미 확정 소스).

## NOTI (`ziobiz/NOTI`)

| 항목 | 값 |
|------|-----|
| Webhook | `POST /noti/crypto` (별칭 `/noti/webhook/crypto`) |
| Result | `GET\|POST /noti/result/crypto` |
| 가맹 매칭 | `X-Icopay-Comp-Id` 또는 body `compId` → `MERCHANTS` (pgKind 무관, callbackUrl 우선) |
| 가맹 스키마 | `returncode` / `orderid` / `transaction_id` / `amount` (JPAY 호환) + `assetCode`·`networkCode`·`depositTxHash` |
| 구현 | `lib/cryptoNoti.js`, `server.js` handlers, 운영매뉴얼 id=`crypto` |

## ICOPAY (본 저장소)

| 항목 | 값 |
|------|-----|
| 트리거 | `CryptoCheckoutService.confirmDeposit` 커밋 후 |
| 송신 | `CryptoNotiMiddlewareMirrorService` → JSON POST `/noti/crypto` |
| 헤더 | `X-Icopay-Comp-Id`, `X-Icopay-Crypto-Mirror`, 선택 `X-Api-Key`(NOTI provision key) |
| URL 상수 | `/noti/crypto`, `/noti/result/crypto` |

## 가맹 운영

1. 카드용으로 NOTI에 이미 등록된 가맹이면 **추가 노티생성 없이** Comp-Id만 맞으면 Callback 수신.
2. 크립토만 신규면 NOTI에 가맹 등록 후 `callbackUrl`/`resultUrl` 설정 (JPAY/EP와 동일 필드).
3. 수신통보 URL 카드의 Notify/Result는 **PG→NOTI 입구** 표시용; 크립토 고정 입구는 위 표.

## 검증

1. 테스트 가맹 Comp-Id로 입금 확정.
2. NOTI 로그 `pgProvider=crypto`, `relayStatus=ok`.
3. 가맹 Callback이 JPAY와 동일 스키마로 수신.
