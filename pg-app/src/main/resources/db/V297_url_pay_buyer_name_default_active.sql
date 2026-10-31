-- 성명(리다이렉트용) 기본값 = 활성(Y)
ALTER TABLE tb_hq_api_config
    ALTER COLUMN url_pay_buyer_name_use_default_yn SET DEFAULT 'Y';

UPDATE tb_hq_api_config
SET url_pay_buyer_name_use_default_yn = 'Y'
WHERE url_pay_buyer_name_use_default_yn IS NULL
   OR UPPER(TRIM(url_pay_buyer_name_use_default_yn)) <> 'Y';

ALTER TABLE tb_merchant_profile
    ALTER COLUMN url_pay_buyer_name_use_yn SET DEFAULT 'Y';

-- 미설정·본사따름(FOLLOW_HQ) 가맹도 성명 활성으로 맞춤(명시 N만 유지)
UPDATE tb_merchant_profile
SET url_pay_buyer_name_use_yn = 'Y'
WHERE url_pay_buyer_name_use_yn IS NULL
   OR TRIM(url_pay_buyer_name_use_yn) = ''
   OR UPPER(TRIM(url_pay_buyer_name_use_yn)) = 'FOLLOW_HQ';
