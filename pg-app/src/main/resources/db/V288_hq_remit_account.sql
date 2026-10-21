-- 본사 송금 수취계좌. ElementPay 수취인 ID를 저장하고 송금관리 선택 목록으로 씁니다.
CREATE TABLE IF NOT EXISTS tb_hq_remit_account (
    id            BIGSERIAL PRIMARY KEY,
    recipient_id  VARCHAR(64)  NOT NULL,
    holder_name   VARCHAR(200),
    account_last  VARCHAR(8),
    country       VARCHAR(8),
    recipient_type VARCHAR(20),
    default_yn    VARCHAR(1)   NOT NULL DEFAULT 'N',
    source_cd     VARCHAR(20)  NOT NULL DEFAULT 'EP',
    active_yn     VARCHAR(1)   NOT NULL DEFAULT 'Y',
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_hq_remit_account_recipient UNIQUE (recipient_id)
);

COMMENT ON TABLE tb_hq_remit_account IS '본사 송금 수취계좌. ElementPay 등록 계좌를 가져와 기본 계좌로 두고, 계좌관리에서 추가합니다.';
