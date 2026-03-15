-- ============================================================
-- 업체관리 엑셀 컬럼 추가 (2026-02)
-- ============================================================
-- pgAdmin4: pgdev 우클릭 → Query Tool → 붙여넣기 → F5
--
-- * tb_settlement_setting, tb_merchant_profile 테이블이 없으면
--   먼저 생성한 뒤 컬럼을 추가합니다.
--
-- [확인] 실행 후 왼쪽 Tables에서 우클릭 → Refresh(F5) 해야 새 테이블 보임
-- ============================================================

BEGIN;

-- ----- 1. tb_org_unit (없으면 생성, tb_merchant_profile/tb_settlement_setting의 부모)
CREATE TABLE IF NOT EXISTS tb_org_unit (
    id          BIGSERIAL PRIMARY KEY,
    org_level   VARCHAR(20) NOT NULL,
    parent_id   BIGINT,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    status      VARCHAR(20) DEFAULT 'ACTIVE',
    created_at  TIMESTAMP
);

-- ----- 2. tb_merchant_profile (없으면 생성)
CREATE TABLE IF NOT EXISTS tb_merchant_profile (
    id                      BIGSERIAL PRIMARY KEY,
    org_unit_id              BIGINT NOT NULL,
    comp_div                 VARCHAR(20),
    tel                      VARCHAR(50),
    zip_code                 VARCHAR(20),
    addr                     VARCHAR(255),
    addr_detail              VARCHAR(255),
    ceo_nm                   VARCHAR(100),
    ceo_mobile               VARCHAR(50),
    use_yn                   VARCHAR(1),
    login_id                 VARCHAR(50),
    reg_no                   VARCHAR(50),
    biz_type                 VARCHAR(100),
    industry                 VARCHAR(100),
    biz_nature               VARCHAR(100),
    product                  VARCHAR(100),
    homepage                 VARCHAR(255),
    site_url                 VARCHAR(255),
    settle_name              VARCHAR(100),
    settle_tel_no             VARCHAR(50),
    settle_type              VARCHAR(5),
    commission_rate          NUMERIC(10,4),
    limit_amt                NUMERIC(18,0),
    fax                      VARCHAR(50),
    email                    VARCHAR(100),
    pwd                     VARCHAR(200),
    bank_cd                  VARCHAR(20),
    transfer_fee             VARCHAR(50),
    account_no               VARCHAR(50),
    account_holder           VARCHAR(100),
    country_cd               VARCHAR(10),
    swift                    VARCHAR(50),
    branch_name              VARCHAR(100),
    branch_addr              VARCHAR(255),
    contact_tel              VARCHAR(50),
    wallet_address           VARCHAR(255),
    network_name             VARCHAR(50),
    remark                   VARCHAR(500),
    commission_config_allowed VARCHAR(1) DEFAULT 'N',
    web_payment_use_yn       VARCHAR(1) DEFAULT 'Y',
    base_currency            VARCHAR(10),
    terminal_count_terminal  INTEGER,
    terminal_count_web       INTEGER,
    created_at               TIMESTAMP
);

-- ----- 3. tb_settlement_setting (없으면 생성)
CREATE TABLE IF NOT EXISTS tb_settlement_setting (
    id                          BIGSERIAL PRIMARY KEY,
    org_unit_id                  BIGINT NOT NULL UNIQUE,
    withdraw_limit_days          INTEGER,
    withdraw_start_time          TIME,
    withdraw_end_time            TIME,
    pay_limit_default            NUMERIC(18,0),
    pay_limit_extra              NUMERIC(18,0),
    pay_limit_alert_sms          VARCHAR(1) DEFAULT 'N',
    hold_rate_follow_hq          VARCHAR(1) DEFAULT 'Y',
    hold_rate                    NUMERIC(5,2),
    hold_days                    INTEGER,
    calc_cycle                   VARCHAR(20),
    calc_close_time              TIME,
    transfer_type                VARCHAR(20),
    transfer_cycle_days          INTEGER,
    auto_transfer_min            NUMERIC(18,0),
    pay_hold_yn                  VARCHAR(1) DEFAULT 'N',
    calc_exclude_dates           VARCHAR(200),
    calc_start_time              TIME,
    calc_exclude_target          VARCHAR(20),
    calc_exclude_yn              VARCHAR(1) DEFAULT 'N',
    pay_limit_month              NUMERIC(18,0),
    pay_limit_year               NUMERIC(18,0),
    withdraw_limit_hour          INTEGER,
    pay_amount_in_time           NUMERIC(18,0),
    same_card_limit_day_web      INTEGER,
    same_card_limit_cnt_web      INTEGER,
    same_card_limit_amt_web       NUMERIC(18,0),
    same_card_limit_day_terminal  INTEGER,
    same_card_limit_cnt_terminal  INTEGER,
    same_card_limit_amt_terminal  NUMERIC(18,0),
    pay_limit_daily              NUMERIC(18,0)
);

-- ----- 4. 테이블이 이미 있는 경우 새 컬럼만 추가
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_limit_month NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_limit_year NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS withdraw_limit_hour INTEGER;
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_amount_in_time NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_day_web INTEGER;
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_cnt_web INTEGER;
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_amt_web NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_day_terminal INTEGER;
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_cnt_terminal INTEGER;
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS same_card_limit_amt_terminal NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_limit_daily NUMERIC(18,0);

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS terminal_count_terminal INTEGER;
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS terminal_count_web INTEGER;

COMMIT;

-- 실행 완료 후 "Query returned successfully" 메시지 확인
