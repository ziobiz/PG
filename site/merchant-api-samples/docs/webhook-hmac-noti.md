# ICOPAY server webhook security (HMAC) — for merchants (NOTI-first)

## Who sends

Payment **server notify** is usually **NOTI** `POST`ing to your merchant `callbackUrl`.  
ICOPAY may also POST directly (Background / inbound notify URLs). When signing is Active, **the same header and secret** apply.

This is not the browser Result redirect.

## Setup (ICOPAY admin)

1. **HQ Policy → Payment routing → Webhook signing (HQ default)** — default **OFF**  
2. **Company → merchant detail → Webhook signing** — Follow HQ / Active / Inactive (merchant overrides)  
3. Set **Active** → **Rotate secret** (plain text shown **once**) → store only on your server (`ICOPAY_WEBHOOK_SIGN_SECRET`)

ICOPAY syncs the setting to NOTI. Verify the same way for NOTI and ICOPAY-direct posts.

## Signature

| Item | Value |
|------|--------|
| Header | `X-Icopay-Signature: v1={hex}` |
| Algorithm | HMAC-SHA256 |
| Message | **raw HTTP body** bytes |
| Key | Webhook signing secret |

Return **401** on mismatch; **200** on success.

## Egress IP (informational)

| Source | IPv4 | IPv6 |
|--------|------|------|
| NOTI (typical) | `172.235.192.86` | `2400:8905::2000:6dff:fe4b:8079` |
| ICOPAY direct | `1.234.65.25` | none |

Prefer **HMAC**; use IP allowlists only as a supplement.

## Samples

- PHP: `../php/notify_webhook.php`  
- JSP: `../jsp/notify-webhook.jsp`  
- KO: [webhook-hmac-noti.ko.md](./webhook-hmac-noti.ko.md) · JA: [webhook-hmac-noti.ja.md](./webhook-hmac-noti.ja.md) · CH: [webhook-hmac-noti.ch.md](./webhook-hmac-noti.ch.md) · TH: [webhook-hmac-noti.th.md](./webhook-hmac-noti.th.md)
