-- V273: 가맹 6000000064 — 이메일·국가·전화 활성 + 구매자입력 프리셋 해제
-- (프리셋 FK가 남아 있으면 개별 활성이 결제창에 반영되지 않음)

UPDATE tb_merchant_profile mp
SET url_pay_checkout_field_preset_id = NULL,
    url_pay_buyer_email_use_yn = 'Y',
    url_pay_buyer_country_use_yn = 'Y',
    url_pay_buyer_phone_use_yn = 'Y',
    jpay_checkout_field_mode = 'CARD_ONLY'
FROM tb_org_unit ou
WHERE mp.org_unit_id = ou.id
  AND ou.code = '6000000064';
