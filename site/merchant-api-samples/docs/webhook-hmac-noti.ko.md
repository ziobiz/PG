# ICOPAY 서버 웹훅 보안(HMAC) — 가맹점용 (NOTI 기준)

## 누가 보이나

결제 확정 **서버 통보**는 보통 **NOTI**가 가맹 `callbackUrl`로 `POST`합니다.  
(ICOPAY 업체관리의 결제통보·수신통보 직통도 가능하며, 서명 활성 시 **같은 헤더·시크릿**을 씁니다.)

브라우저 Result URL 리다이렉트와는 **별개**입니다.

## 설정 (ICOPAY 관리자)

1. **본사정책 → 결제 라우팅 → 웹훅 서명(본사 기본)** — 기본 **비활성(OFF)**  
2. **업체관리 → 가맹 상세 → 웹훅 서명** — `본사설정 따름` / `활성` / `비활성` (가맹 우선)  
3. **활성** 후 **시크릿 재발급** — 평문은 **한 번만** 표시 → 가맹 서버에만 보관 (`ICOPAY_WEBHOOK_SIGN_SECRET`)

ICOPAY가 NOTI에 설정을 동기화합니다. 가맹은 NOTI·ICOPAY 직통 모두 **동일 검증**하면 됩니다.

## 서명 규격

| 항목 | 내용 |
|------|------|
| 헤더 | `X-Icopay-Signature: v1={hex}` |
| 알고리즘 | HMAC-SHA256 |
| 메시지 | **raw HTTP body** (수신 바이트 그대로) |
| 키 | 웹훅 서명 시크릿 |

서명 불일치 시 **401** 권장. 성공 시 **HTTP 200**.

## 발신 IP (참고)

| 발신 | IPv4 | IPv6 |
|------|------|------|
| NOTI (일반) | `172.235.192.86` | `2400:8905::2000:6dff:fe4b:8079` |
| ICOPAY 직통 | `1.234.65.25` | 없음 |

**HMAC 검증을 주 수단**으로 두고, IP는 보조로 쓰세요.

## 샘플

- PHP: `../php/notify_webhook.php`  
- JSP: `../jsp/notify-webhook.jsp`  
- 영어: [webhook-hmac-noti.md](./webhook-hmac-noti.md)
