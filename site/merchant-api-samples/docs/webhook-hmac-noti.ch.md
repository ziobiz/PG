# ICOPAY 服务器 Webhook 安全(HMAC) — 商户版（以 NOTI 为准）

支付确认的**服务器通知**通常由 **NOTI** POST 到商户 `callbackUrl`。启用签名时请求头 `X-Icopay-Signature: v1=` + HMAC-SHA256(raw body)。密钥在 ICOPAY 公司管理→Webhook 签名→重新签发（明文一次）。请校验 HMAC，勿只依赖 IP。

出站 IP：NOTI `172.235.192.86` / IPv6 `2400:8905::2000:6dff:fe4b:8079` · ICOPAY 直连 `1.234.65.25`

样例：`../php/notify_webhook.php` · 英文详版 [webhook-hmac-noti.md](./webhook-hmac-noti.md)
