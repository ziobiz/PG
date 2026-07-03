-- V218·V219 가 CHAR(1)(bpchar)로 추가된 Y/N 컬럼 → Hibernate validate(VARCHAR(1)) 와 맞춤
-- 이미 VARCHAR(1)이면 no-op. V219 미적용 DB는 존재하는 컬럼만 변환.

DO $$
DECLARE
  col text;
  cols text[] := ARRAY[
    'presale_filter_enabled_yn',
    'filter_buyer_contact_mismatch_yn',
    'filter_holder_name_yn',
    'filter_velocity_card_yn',
    'filter_velocity_email_yn',
    'filter_velocity_ip_yn',
    'checkout_contact_remember_default_yn',
    'filter_phone_invalid_yn',
    'filter_email_invalid_yn'
  ];
BEGIN
  FOREACH col IN ARRAY cols
  LOOP
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name = 'tb_hq_risk_card_policy'
        AND column_name = col
        AND data_type = 'character'
    ) THEN
      EXECUTE format(
        'ALTER TABLE tb_hq_risk_card_policy ALTER COLUMN %I TYPE VARCHAR(1) USING TRIM(%I::text)',
        col, col);
    END IF;
  END LOOP;
END $$;
