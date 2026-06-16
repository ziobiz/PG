-- 가맹점 브로커 시크릿 발행자(로그인 ID) 기록
ALTER TABLE tb_merchant_icopay_broker_credential
    ADD COLUMN IF NOT EXISTS issued_by VARCHAR(100);

COMMENT ON COLUMN tb_merchant_icopay_broker_credential.issued_by IS
    '브로커 시크릿 최종 발행·재발행 수행 로그인 ID';
