-- 전산설정: 수수료·정산 금액 표시 — 통화별 소수 자릿수·반올림(JSON, 전역 fee_list_* 폴백)
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS fee_currency_format_json TEXT;
