# ความปลอดภัย Webhook เซิร์ฟเวอร์ ICOPAY (HMAC) — สำหรับร้าน (ยึด NOTI)

การแจ้งชำระฝั่ง**เซิร์ฟเวอร์**มักเป็น **NOTI** POST ไป `callbackUrl` ของร้าน เมื่อเปิดลายเซ็น จะมีหัว `X-Icopay-Signature: v1=` + HMAC-SHA256(raw body) รับ secret ที่ ICOPAY จัดการบริษัท→ลายเซ็น Webhook→ออกใหม่ (เปิดเผยครั้งเดียว) ตรวจ HMAC อย่าพึ่งแค่ IP

IP ขาออก: NOTI `172.235.192.86` / IPv6 `2400:8905::2000:6dff:fe4b:8079` · ICOPAY ตรง `1.234.65.25`

ตัวอย่าง: `../php/notify_webhook.php` · อังกฤษ [webhook-hmac-noti.md](./webhook-hmac-noti.md)
