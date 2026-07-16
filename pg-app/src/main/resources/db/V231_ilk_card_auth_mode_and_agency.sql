-- 일반결제 카드 인증방식(3DS / NONE3D·2DS): 본사 기본 + 가맹 FOLLOW_HQ(가맹 우선)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS card_auth_mode_default VARCHAR(16) NOT NULL DEFAULT 'THREE_DS';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_auth_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_hq_api_config.card_auth_mode_default IS
    '일반결제 카드 인증 본사 기본: THREE_DS | NONE3D (2DS). URL·API 인라인 공통';
COMMENT ON COLUMN tb_merchant_profile.card_auth_mode IS
    '일반결제 카드 인증: FOLLOW_HQ | THREE_DS | NONE3D. FOLLOW_HQ가 아니면 본사보다 우선';

-- ILK(아이엘케이) 결제대행사 시드
INSERT INTO tb_pg_agency (
    pg_cd, pg_nm, api_endpoint, endpoint_noti, endpoint_url_pay, endpoint_api,
    integ_noti_yn, integ_url_pay_yn, integ_web_chatbot_yn, integ_api_yn,
    integ_api_subscription_yn, integ_url_pay_repay_yn,
    use_yn, operational_yn, sandbox_yn, ext_settle_mode, created_at
)
SELECT
    'ILK',
    'ILK (아이엘케이·카드 3DS/NONE3D·구독 COF)',
    'https://testocp.ilkrhub.com',
    'https://testocp.ilkrhub.com',
    'https://testocp.ilkrhub.com',
    'https://testocp.ilkrhub.com',
    'Y', 'Y', 'N', 'Y',
    'Y', 'N',
    'Y', 'N', 'Y', 'OFF', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_pg_agency WHERE pg_cd = 'ILK');

COMMENT ON COLUMN tb_pg_agency.credentials_extra_json IS
    'PG별 추가 JSON. ILK: merchantSiteId, seedKey, seedIv (AES). ElementPay: cardServiceAlias, promptPayServiceAlias';

-- ILK 구독(COF) 등록·회차 청구
CREATE TABLE IF NOT EXISTS tb_merchant_ilk_subscription (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    comp_id         VARCHAR(64) NOT NULL,
    subscription_no VARCHAR(64) NOT NULL,
    plan_json       TEXT,
    status          VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    currency        VARCHAR(8),
    amount          NUMERIC(18, 4),
    first_order_no  VARCHAR(64),
    first_auth_id   VARCHAR(64),
    card_brand      VARCHAR(16),
    card_last4      VARCHAR(8),
    next_charge_at  TIMESTAMP,
    last_charge_at  TIMESTAMP,
    charge_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ilk_sub_comp_no UNIQUE (comp_id, subscription_no)
);

CREATE INDEX IF NOT EXISTS ix_ilk_sub_next ON tb_merchant_ilk_subscription (status, next_charge_at);
CREATE INDEX IF NOT EXISTS ix_ilk_sub_org ON tb_merchant_ilk_subscription (org_unit_id);
