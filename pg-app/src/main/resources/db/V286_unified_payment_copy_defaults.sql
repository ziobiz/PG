-- V286: 통합결제 상단·푸터메시지·추가푸터 본사 기본값 시드
SET client_min_messages TO WARNING;

UPDATE tb_hq_api_config SET
  unified_payment_header_subtitle_mode_default = 'ACTIVE',
  unified_payment_header_subtitle_text_default =
    '카드 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.',
  unified_payment_header_subtitle_text_default_i18n =
    '{"KOR":"카드 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.","ENG":"You can pay by card or crypto. Please choose a payment method.","JPN":"カードまたは暗号資産でお支払いできます。お支払い方法を選択してください。","CHN":"可使用银行卡或加密货币支付。请选择支付方式。","THA":"ชำระด้วยบัตรหรือคริปโตได้ กรุณาเลือกวิธีชำระเงิน"}',
  unified_payment_notice_mode_default = 'ACTIVE',
  unified_payment_notice_text_default =
    '주의: 선택한 수단으로 결제가 진행됩니다. 카드와 암호화폐는 별도 결제창에서 처리되며, 결제 완료 전까지 창을 닫지 마세요.',
  unified_payment_notice_text_default_i18n =
    '{"KOR":"주의: 선택한 수단으로 결제가 진행됩니다. 카드와 암호화폐는 별도 결제창에서 처리되며, 결제 완료 전까지 창을 닫지 마세요.","ENG":"Note: Payment continues with the method you select. Card and crypto open separate checkout pages. Do not close the window until payment is complete.","JPN":"注意: 選択した方法で決済が続きます。カードと暗号資産は別画面です。完了まで閉じないでください。","CHN":"注意：将按所选方式继续支付。银行卡与加密货币为不同页面。完成前请勿关闭窗口。","THA":"หมายเหตุ: จะชำระตามวิธีที่เลือก บัตรและคริปโตเปิดหน้าต่างคนละหน้า อย่าปิดจนกว่าจะชำระเสร็จ"}',
  unified_payment_extra_footer_mode_default = 'ACTIVE',
  unified_payment_extra_footer_text_default = 'ICOPAY, a service operated by ONTHELINE.',
  unified_payment_extra_footer_text_default_i18n = NULL;

COMMENT ON COLUMN tb_hq_api_config.unified_payment_extra_footer_text_default IS
  '통합결제 추가푸터(영어 고정). 다국어 번역하지 않음.';
