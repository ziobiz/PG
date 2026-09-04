-- 가맹 url_pay_checkout_field_preset_id:
--   NULL = 구매자입력 프리셋 비활성(가맹이 이메일·국가·전화·배송을 개별 활성/비활성)
--   값 있음 = 해당 프리셋(기본형·N형) 따름
-- 기존 NULL(본사설정따름/기본형) → 기본형 id 로 이관

COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_field_preset_id IS
    '결제창 필드 프리셋 FK. NULL=프리셋 비활성(가맹 개별 Y/N). 값=기본형·N형 프리셋 적용';

UPDATE tb_merchant_profile mp
SET url_pay_checkout_field_preset_id = d.id
FROM (
    SELECT id FROM tb_url_pay_checkout_field_preset
    WHERE UPPER(COALESCE(is_default_yn, 'N')) = 'Y'
    ORDER BY id
    LIMIT 1
) d
WHERE mp.url_pay_checkout_field_preset_id IS NULL;
