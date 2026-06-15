-- 가맹 웹결제(URL·JPAY) 결제창 로고 아래 경고/안내 문구 — DEFAULT(3DS 안전 결제)·DISABLED·ACTIVE(가맹 입력)

ALTER TABLE tb_merchant_profile

    ADD COLUMN IF NOT EXISTS web_payment_header_subtitle_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT';



ALTER TABLE tb_merchant_profile

    ADD COLUMN IF NOT EXISTS web_payment_header_subtitle_text VARCHAR(200);



COMMENT ON COLUMN tb_merchant_profile.web_payment_header_subtitle_mode IS '웹결제 상단 경고문구: DEFAULT|DISABLED|ACTIVE';

COMMENT ON COLUMN tb_merchant_profile.web_payment_header_subtitle_text IS '웹결제 상단 경고문구(ACTIVE 시 가맹 입력)';


