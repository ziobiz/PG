# ElementPay 웹훅 IP — NOTI · ICOPAY 역할

| 구간 | 역할 |
|---|---|
| **EP → NOTI** `/noti/elementpay` | **1차 화이트리스트**. 허용 IP만 수신. 거절 시 **403** + `data/elementpay-webhook-ip-reject.log` |
| **NOTI → ICOPAY** `…/pg-notify/…/ELEMENTPAY` | 원문 패스스루 + 헤더 **`X-ElementPay-Source-Ip`** (EP 출구 IP) |
| **ICOPAY** | 헤더를 **감사·로그·inbound client_ip** 에 기록. 알 수 없는 IP여도 결제 응답은 계속(이중 거절로 결제 끊김 방지) |

## 허용 IP (Cabinet 기준)

- Sandbox: `3.0.36.253`
- Live: `3.1.29.20`
- NOTI 설정: `config/elementpay-ingress.json` → `webhookIpWhitelist` (추가 IP·신뢰 프록시 가능)

## 점검

```text
# 비허용 공인 IP에서 POST → 403 이어야 함
POST https://noti.icopay.net/noti/elementpay
```

ICOPAY util: `ElementPayWebhookIpUtil` · 콜백: `ElementPayCallbackService`  
NOTI: `lib/elementpayWebhookIp.js` · `forwardElementPayToIcopay` 헤더 전달
