-- PG 중계: 가맹점 아웃바운드 콜백 URL·HMAC 시크릿 (tb_merchant_notify_url.url_type = MIDDLEWARE)
ALTER TABLE tb_merchant_notify_url
    ADD COLUMN IF NOT EXISTS sign_secret VARCHAR(256);

COMMENT ON COLUMN tb_merchant_notify_url.sign_secret IS 'PG중계→가맹점 POST JSON HMAC-SHA256 시크릿(비어 있으면 X-Icopay-Signature 생략)';

-- 동일 내부상태로 중복 전송 방지(상태 변경 시에만 재전송)
ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS mw_outbound_last_sent_status VARCHAR(8);

COMMENT ON COLUMN pg_trnsctn.mw_outbound_last_sent_status IS '미들웨어 아웃바운드 마지막 전송 시점의 내부 status(10/08/99 등)';
