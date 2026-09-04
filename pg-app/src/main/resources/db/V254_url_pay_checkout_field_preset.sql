-- 결제창 구매자 입력 필드 프리셋 (기본형·1형·2형…)
CREATE TABLE IF NOT EXISTS tb_url_pay_checkout_field_preset (
    id                      BIGSERIAL PRIMARY KEY,
    preset_name             VARCHAR(40) NOT NULL,
    sort_no                 INT NOT NULL DEFAULT 0,
    is_default_yn           VARCHAR(1) NOT NULL DEFAULT 'N',
    buyer_email_use_yn      VARCHAR(1) NOT NULL DEFAULT 'Y',
    buyer_country_use_yn    VARCHAR(1) NOT NULL DEFAULT 'Y',
    buyer_phone_use_yn      VARCHAR(1) NOT NULL DEFAULT 'Y',
    shipping_address_use_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_url_pay_checkout_field_preset_name UNIQUE (preset_name)
);

COMMENT ON TABLE tb_url_pay_checkout_field_preset IS '결제창 구매자 입력(이메일·국가·전화·배송) 프리셋 — 기본형·N형';
COMMENT ON COLUMN tb_url_pay_checkout_field_preset.preset_name IS '기본형, 1형, 2형…';
COMMENT ON COLUMN tb_url_pay_checkout_field_preset.is_default_yn IS 'Y=기본형(삭제 불가). 가맹 본사설정따름 시 적용';

INSERT INTO tb_url_pay_checkout_field_preset (
    preset_name, sort_no, is_default_yn,
    buyer_email_use_yn, buyer_country_use_yn, buyer_phone_use_yn, shipping_address_use_yn, updated_at
)
SELECT '기본형', 0, 'Y', 'Y', 'Y', 'Y', 'N', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_url_pay_checkout_field_preset WHERE is_default_yn = 'Y' OR preset_name = '기본형'
);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_checkout_field_preset_id BIGINT NULL;

COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_field_preset_id IS '결제창 필드 프리셋 FK. NULL=본사설정따름(기본형)';

CREATE INDEX IF NOT EXISTS idx_mp_url_pay_chk_field_preset
    ON tb_merchant_profile (url_pay_checkout_field_preset_id);
