-- V291: QR 결제 사용유무(본사기본·가맹 FOLLOW_HQ|Y|N) + QR 안내문구(본사/가맹)
SET client_min_messages TO WARNING;

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS qr_payment_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_text_default VARCHAR(500),
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.qr_payment_use_default_yn IS
  '가맹 QR 사용이 FOLLOW_HQ 일 때 본사 기본 Y|N';
COMMENT ON COLUMN tb_hq_api_config.qr_payment_header_subtitle_mode_default IS
  'QR 결제창 상단안내 본사기본 ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.qr_payment_header_subtitle_text_default IS
  'QR 결제창 상단안내 본사 기본 문구(KO). 저장 시 5개국어 i18n 생성';

UPDATE tb_hq_api_config SET
  qr_payment_use_default_yn = COALESCE(NULLIF(TRIM(qr_payment_use_default_yn), ''), 'Y'),
  qr_payment_header_subtitle_mode_default = COALESCE(NULLIF(TRIM(qr_payment_header_subtitle_mode_default), ''), 'ACTIVE'),
  qr_payment_header_subtitle_text_default =
    'QR로 결제합니다. 앱에서 아래 QR을 스캔한 뒤 결제를 완료해 주세요.',
  qr_payment_header_subtitle_text_default_i18n =
    '{"KOR":"QR로 결제합니다. 앱에서 아래 QR을 스캔한 뒤 결제를 완료해 주세요.","ENG":"Pay with QR. Scan the QR below in your app and complete payment.","JPN":"QRでお支払いください。アプリで下のQRをスキャンし、決済を完了してください。","CHN":"请使用二维码支付。在应用中扫描下方二维码并完成支付。","THA":"ชำระด้วย QR สแกน QR ด้านล่างในแอปแล้วชำระให้เสร็จ"}'
WHERE qr_payment_header_subtitle_text_default IS NULL
   OR TRIM(qr_payment_header_subtitle_text_default) = '';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS qr_payment_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_text VARCHAR(500),
  ADD COLUMN IF NOT EXISTS qr_payment_header_subtitle_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.qr_payment_use_yn IS
  'QR 결제 사용: FOLLOW_HQ|Y|N (실효=본사기본 또는 Y, 그리고 운영 WEB QR 바인딩)';
COMMENT ON COLUMN tb_merchant_profile.qr_payment_header_subtitle_mode IS
  'QR 결제창 안내: FOLLOW_HQ|ACTIVE|DISABLED (ACTIVE=가맹 우선)';

UPDATE tb_merchant_profile
SET qr_payment_use_yn = 'FOLLOW_HQ'
WHERE qr_payment_use_yn IS NULL
   OR TRIM(qr_payment_use_yn) = '';

UPDATE tb_merchant_profile
SET qr_payment_header_subtitle_mode = 'FOLLOW_HQ'
WHERE qr_payment_header_subtitle_mode IS NULL
   OR TRIM(qr_payment_header_subtitle_mode) = ''
   OR UPPER(TRIM(qr_payment_header_subtitle_mode)) = 'DEFAULT';
