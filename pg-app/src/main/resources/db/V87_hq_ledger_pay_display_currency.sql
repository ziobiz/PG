-- 전산설정: 결제 통화(ISO 4217 숫자) — 목록·집계 기본 표시 통화 폴백 등
ALTER TABLE tb_hq_ledger_sys_settings
  ADD COLUMN IF NOT EXISTS pay_display_currency_iso_num VARCHAR(3) NOT NULL DEFAULT '764';

COMMENT ON COLUMN tb_hq_ledger_sys_settings.pay_display_currency_iso_num IS '결제 통화(ISO 4217 숫자코드). 수수료 정책 등 미지정 시 집계·표시 폴백. 허용: 764,840,978,392,826,036,554,344,702,756,458,156';
