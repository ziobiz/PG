-- V299: 통합결제 수단선택에 QR 문구·DEALMAI DEMO(6000000067) QR(QBANG) 구성
SET client_min_messages TO WARNING;

-- 본사 통합결제 안내·푸터에 QR 포함(카드·QR·크립토)
UPDATE tb_hq_api_config SET
  unified_payment_header_subtitle_mode_default = 'ACTIVE',
  unified_payment_header_subtitle_text_default =
    '카드, QR 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.',
  unified_payment_header_subtitle_text_default_i18n =
    '{"KOR":"카드, QR 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.","ENG":"You can pay by card, QR, or crypto. Please choose a payment method.","JPN":"カード、QR、または暗号資産でお支払いできます。お支払い方法を選択してください。","CHN":"可使用银行卡、QR 或加密货币支付。请选择支付方式。","THA":"ชำระด้วยบัตร QR หรือคริปโตได้ กรุณาเลือกวิธีชำระเงิน"}',
  unified_payment_notice_mode_default = 'ACTIVE',
  unified_payment_notice_text_default =
    '주의: 선택한 수단으로 결제가 진행됩니다. 카드, QR, 암호화폐는 별도 결제창에서 처리되며, 결제 완료 전까지 창을 닫지 마세요.',
  unified_payment_notice_text_default_i18n =
    '{"KOR":"주의: 선택한 수단으로 결제가 진행됩니다. 카드, QR, 암호화폐는 별도 결제창에서 처리되며, 결제 완료 전까지 창을 닫지 마세요.","ENG":"Note: Payment continues with the method you select. Card, QR, and crypto open separate checkout pages. Do not close the window until payment is complete.","JPN":"注意: 選択した方法で決済が続きます。カード、QR、暗号資産は別画面です。完了まで閉じないでください。","CHN":"注意：将按所选方式继续支付。银行卡、QR 与加密货币为不同页面。完成前请勿关闭窗口。","THA":"หมายเหตุ: จะชำระตามวิธีที่เลือก บัตร QR และคริปโตเปิดหน้าต่างคนละหน้า อย่าปิดจนกว่าจะชำระเสร็จ"}'
WHERE TRUE;

-- DEALMAI DEMO: QR 사용 ON + QBANG SANDBOX QR 전용 운영 행(카드 없음)
UPDATE tb_merchant_profile mp
SET qr_payment_use_yn = 'Y'
FROM tb_org_unit ou
WHERE ou.id = mp.org_unit_id
  AND ou.code = '6000000067';

INSERT INTO tb_merchant_pg_binding (
  org_unit_id, pg_cd, activation_yn, operational_yn, pay_method,
  mid, api_key, iv_key, installment_yn, sort_order,
  url_pay_pricing_mode, card_brand_scope, currency_scope, qr_wallet_scope, created_at
)
SELECT
  ou.id,
  'QBANG SANDBOX',
  'Y',
  'Y',
  'WEB',
  COALESCE(NULLIF(TRIM(ag.merchant_mid), ''), 'QBANG-DEMO-POS'),
  '',
  '',
  'N',
  10,
  'CHECKOUT_CURRENCY',
  'NONE',
  'ALL',
  'QBANG',
  NOW()
FROM tb_org_unit ou
LEFT JOIN tb_pg_agency ag ON UPPER(TRIM(ag.pg_cd)) = 'QBANG SANDBOX'
WHERE ou.code = '6000000067'
  AND NOT EXISTS (
    SELECT 1 FROM tb_merchant_pg_binding b
    WHERE b.org_unit_id = ou.id
      AND UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX'
      AND UPPER(TRIM(b.pay_method)) = 'WEB'
  );
