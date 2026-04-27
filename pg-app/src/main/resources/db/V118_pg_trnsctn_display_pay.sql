-- URL·DISPLAY_FX·노티 등: 고객 화면 표시 금액·통화(PG 청구 cur_type/amt_krw 와 별도)
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS display_cur_type VARCHAR(10);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS display_amt NUMERIC(20, 8);

COMMENT ON COLUMN pg_trnsctn.display_cur_type IS '고객 표시 통화(ISO). PG 청구 통화(cur_type)와 다를 수 있음(URL DISPLAY_FX·노티 DisplayCurrency 등)';
COMMENT ON COLUMN pg_trnsctn.display_amt IS '고객 표시 금액(주단위·소수). PG 청구 금액(amt_krw)과 통화가 다를 때';
