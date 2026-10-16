-- V287: 크립토 결제 안내 — 본사 기본 + 가맹 FOLLOW_HQ (가맹 직접입력이 우선)
SET client_min_messages TO WARNING;

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_text_default VARCHAR(500),
  ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.crypto_payment_header_subtitle_mode_default IS
  '크립토 결제창 상단안내 본사기본 ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.crypto_payment_header_subtitle_text_default IS
  '크립토 결제창 상단안내 본사 기본 문구(KO). 저장 시 5개국어 i18n 생성';

UPDATE tb_hq_api_config SET
  crypto_payment_header_subtitle_mode_default = 'ACTIVE',
  crypto_payment_header_subtitle_text_default =
    '암호화폐로 결제합니다. 자산·네트워크·지갑을 선택한 뒤 안내 주소로 입금해 주세요.',
  crypto_payment_header_subtitle_text_default_i18n =
    '{"KOR":"암호화폐로 결제합니다. 자산·네트워크·지갑을 선택한 뒤 안내 주소로 입금해 주세요.","ENG":"Pay with cryptocurrency. Choose asset, network and wallet, then deposit to the address shown.","JPN":"暗号資産でお支払いください。資産・ネットワーク・ウォレットを選び、表示の住所へ入金してください。","CHN":"请使用加密货币支付。选择资产、网络与钱包后，转账至所示地址。","THA":"ชำระด้วยคริปโต เลือกสินทรัพย์ เครือข่าย และกระเป๋า แล้วโอนไปยังที่อยู่ที่แสดง"}'
WHERE crypto_payment_header_subtitle_text_default IS NULL
   OR TRIM(crypto_payment_header_subtitle_text_default) = '';

-- 가맹: DEFAULT(레거시 시스템고정) → FOLLOW_HQ(본사 기본 따름). ACTIVE|DISABLED 유지
UPDATE tb_merchant_profile
SET crypto_payment_header_subtitle_mode = 'FOLLOW_HQ'
WHERE crypto_payment_header_subtitle_mode IS NULL
   OR TRIM(crypto_payment_header_subtitle_mode) = ''
   OR UPPER(TRIM(crypto_payment_header_subtitle_mode)) = 'DEFAULT';

ALTER TABLE tb_merchant_profile
  ALTER COLUMN crypto_payment_header_subtitle_mode SET DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.crypto_payment_header_subtitle_mode IS
  '크립토 결제창 안내: FOLLOW_HQ|ACTIVE|DISABLED (ACTIVE=가맹 우선)';
