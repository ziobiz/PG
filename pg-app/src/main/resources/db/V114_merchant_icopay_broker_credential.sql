-- 가맹점 ICOPAY PG 브로커(/api/middleware/v1/pg/...) 호출용 시크릿 — 전산 「가맹점 API 연동키트」에서 발급·폐기
CREATE TABLE IF NOT EXISTS tb_merchant_icopay_broker_credential (
    id                  BIGSERIAL PRIMARY KEY,
    org_unit_id         BIGINT NOT NULL,
    vendor_scope        VARCHAR(20) NOT NULL DEFAULT 'ALL',
    broker_secret       VARCHAR(128) NOT NULL,
    secret_prefix       VARCHAR(12),
    use_yn              VARCHAR(1) NOT NULL DEFAULT 'Y',
    enforce_yn          VARCHAR(1) NOT NULL DEFAULT 'Y',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotated_at          TIMESTAMP,
    remark              VARCHAR(500),
    CONSTRAINT uq_merchant_icopay_broker_vendor UNIQUE (org_unit_id, vendor_scope)
);

CREATE INDEX IF NOT EXISTS idx_merchant_icopay_broker_secret
    ON tb_merchant_icopay_broker_credential (broker_secret)
    WHERE use_yn = 'Y';

COMMENT ON TABLE tb_merchant_icopay_broker_credential IS
    '가맹점별 ICOPAY 브로커 API 시크릿. vendor_scope: ALL(전 PG 브로커), CHILLPAY, JPAY 등';
COMMENT ON COLUMN tb_merchant_icopay_broker_credential.vendor_scope IS
    '브로커 벤더 범위 — ALL 또는 PgVendor 계열 코드';
COMMENT ON COLUMN tb_merchant_icopay_broker_credential.broker_secret IS
    '가맹점 서버가 브로커 호출 시 X-Icopay-Merchant-Broker-Secret 헤더로 전송';
COMMENT ON COLUMN tb_merchant_icopay_broker_credential.enforce_yn IS
    'Y: 시크릿 없으면 403. N: 행이 있어도 시크릿 미제출 시 허용(이행용)';
