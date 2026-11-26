# ICOPAY サーバWebhookセキュリティ(HMAC) — 加盟店向け (NOTI基準)

決済確定の**サーバ通知**は通常 **NOTI** が加盟 `callbackUrl` へ POST します。署名が有効な場合ヘッダ `X-Icopay-Signature: v1=` + HMAC-SHA256(raw body)。シークレットは ICOPAY 事業者管理→Webhook署名→再発行(平文1回)。IPだけでなくHMACを検証してください。

送信IP参考: NOTI `172.235.192.86` / IPv6 `2400:8905::2000:6dff:fe4b:8079` · ICOPAY直送 `1.234.65.25`

サンプル: `../php/notify_webhook.php` · 詳細英文 [webhook-hmac-noti.md](./webhook-hmac-noti.md)
