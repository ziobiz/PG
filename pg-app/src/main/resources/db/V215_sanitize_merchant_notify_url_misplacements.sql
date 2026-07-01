-- 가맹 결제통보 URL 오등록 정리 (NOTI ingress → BACKGROUND/RESULT, WP 미사용 가맹의 WooCommerce 웹훅)
DELETE FROM tb_merchant_notify_url n
WHERE n.url_type IN ('BACKGROUND', 'RESULT')
  AND n.noti_url ILIKE '%noti.icopay.net/noti/%';

DELETE FROM tb_merchant_notify_url n
USING tb_merchant_profile mp
WHERE n.org_unit_id = mp.org_unit_id
  AND n.url_type = 'BACKGROUND'
  AND COALESCE(mp.api_wordpress_use_yn, 'N') <> 'Y'
  AND (
    n.noti_url ILIKE '%/wp-json/icopay%webhook%'
    OR n.noti_url ILIKE '%/wp-json/icopay-jpay%webhook%'
  );
