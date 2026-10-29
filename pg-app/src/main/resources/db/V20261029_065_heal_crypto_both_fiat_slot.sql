-- 크립토 사용 저장이 checkout_means=CRYPTO 로 강제되며 FIAT 슬롯이 꺼진 가맹 복구.
-- 카드/URL PG 와 병행 가능한 BOTH + fiatCommissionSlotYn=Y.

UPDATE tb_merchant_crypto_config
SET checkout_means = 'BOTH'
WHERE UPPER(TRIM(COALESCE(use_yn, 'N'))) = 'Y'
  AND UPPER(TRIM(COALESCE(checkout_means, ''))) = 'CRYPTO';

UPDATE tb_merchant_profile
SET regional_settings = jsonb_set(
        COALESCE(NULLIF(TRIM(regional_settings), ''), '{}')::jsonb,
        '{fiatCommissionSlotYn}',
        '"Y"',
        true
    )::text
WHERE org_unit_id IN (
    SELECT org_unit_id
    FROM tb_merchant_crypto_config
    WHERE UPPER(TRIM(COALESCE(use_yn, 'N'))) = 'Y'
)
  AND (
        regional_settings IS NULL
        OR TRIM(regional_settings) = ''
        OR regional_settings::jsonb ->> 'fiatCommissionSlotYn' IS DISTINCT FROM 'Y'
      );
