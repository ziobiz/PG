-- 본사: 결제창 표시 7항목 기본값 / 가맹: 본사설정 따름(FOLLOW_HQ) 기본

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_company_name_show_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS url_pay_lang_menu_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS checkout_contact_remember_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS web_payment_header_logo_mode_default VARCHAR(16) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN IF NOT EXISTS web_payment_header_subtitle_mode_default VARCHAR(16) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN IF NOT EXISTS url_pay_shipping_address_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS url_pay_product_name_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

-- 리스크설정의 자동기억 기본값이 있으면 본사 API 구성으로 이관(1회)
UPDATE tb_hq_api_config c
SET checkout_contact_remember_default_yn = COALESCE(
        (SELECT NULLIF(TRIM(r.checkout_contact_remember_default_yn), '')
         FROM tb_hq_risk_card_policy r
         ORDER BY r.id
         LIMIT 1),
        c.checkout_contact_remember_default_yn
    )
WHERE c.checkout_contact_remember_default_yn IS NULL
   OR TRIM(c.checkout_contact_remember_default_yn) = '';

ALTER TABLE tb_merchant_profile
    ALTER COLUMN url_pay_product_name_use_yn TYPE VARCHAR(16),
    ALTER COLUMN url_pay_company_name_show_yn TYPE VARCHAR(16),
    ALTER COLUMN url_pay_lang_menu_use_yn TYPE VARCHAR(16),
    ALTER COLUMN url_pay_shipping_address_use_yn TYPE VARCHAR(16);

ALTER TABLE tb_merchant_profile
    ALTER COLUMN url_pay_product_name_use_yn SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN url_pay_company_name_show_yn SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN url_pay_lang_menu_use_yn SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN url_pay_shipping_address_use_yn SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN checkout_contact_remember_mode SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN web_payment_header_logo_mode SET DEFAULT 'FOLLOW_HQ',
    ALTER COLUMN web_payment_header_subtitle_mode SET DEFAULT 'FOLLOW_HQ';

UPDATE tb_merchant_profile
SET url_pay_product_name_use_yn = 'FOLLOW_HQ',
    url_pay_company_name_show_yn = 'FOLLOW_HQ',
    url_pay_lang_menu_use_yn = 'FOLLOW_HQ',
    url_pay_shipping_address_use_yn = 'FOLLOW_HQ',
    checkout_contact_remember_mode = 'FOLLOW_HQ',
    web_payment_header_logo_mode = 'FOLLOW_HQ',
    web_payment_header_subtitle_mode = 'FOLLOW_HQ';
