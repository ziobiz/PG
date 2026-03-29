-- =============================================================================
-- ALL: 마이그레이션 V2 ~ V36 + check_tables.sql (PostgreSQL / pgdev)
-- =============================================================================
-- 경로: pg-app/src/main/resources/db/ALL_migrate_V2_V36_plus_check_tables_postgresql.sql
-- 
-- 사용: pgAdmin에서 데이터베이스 pgdev 선택 → Query Tool → 전체 실행(F5)
-- 
-- 구성:
--   SECTION M — DDL: V2 ~ V36 (순서 고정)
--   SECTION C — 검증: check_tables.sql (SELECT, 마이그레이션 후 확인)
-- 
-- 주의: pg_trnsctn, tb_user, tb_commission_policy, tb_hq_api_config,
--   tb_merchant_pg_binding, tb_rolling_reserve 등 기본 테이블이 있어야 하는 구간이 있습니다.
--   V36은 settled_yn 컬럼이 있을 때만 성공합니다 (없으면 V19 먼저).
-- =============================================================================

-- #############################################################################
-- SECTION M: V2 ~ V36
-- #############################################################################

-- ---------- V2_add_comp_excel_columns.sql ----------
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

-- ---------- V3_add_site_summary.sql ----------
-- ============================================================
-- 사이트개요 컬럼 추가 (2026-02)
-- ============================================================
-- pgAdmin4: pgdev 우클릭 → Query Tool → 붙여넣기 → F5
-- ============================================================

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS site_summary VARCHAR(500);

-- ---------- V4_add_crypto_transfer_fee.sql ----------
-- ============================================================
-- 크립토 이체 수수료 컬럼 추가 (2026-02)
-- ============================================================

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS crypto_transfer_fee VARCHAR(50);

-- ---------- V5_add_org_branding.sql ----------
-- ============================================================
-- 본사/총판 브랜딩 설정 (2026-03)
-- 메인이미지, 로고이미지, 배경테마
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_org_branding (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL UNIQUE,
    main_image_url VARCHAR(500),
    logo_image_url VARCHAR(500),
    theme VARCHAR(20) DEFAULT 'DEFAULT',
    updated_at TIMESTAMP,
    CONSTRAINT fk_org_branding_org_unit FOREIGN KEY (org_unit_id) REFERENCES tb_org_unit(id)
);

CREATE INDEX IF NOT EXISTS idx_org_branding_org_unit_id ON tb_org_branding(org_unit_id);

-- ---------- V6_add_regional_settings.sql ----------
-- ============================================================
-- 본사(REGIONAL) 전용 설정 (2026-03)
-- 업체 상세 정보, 정산정보, 출금/결제 제한, 기본 수수료, 카드사별 한도, 터미널 등
-- ============================================================

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS regional_settings TEXT;

COMMENT ON COLUMN tb_merchant_profile.regional_settings IS '본사(REGIONAL) 전용 JSON 설정';

-- ---------- V7_add_addr_country.sql ----------
-- 주소 국가 (기본정보)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS addr_country_cd VARCHAR(20);

-- ---------- V8_add_addr_etc.sql ----------
-- 주소 기타 (상세주소 아래)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS addr_etc VARCHAR(255);

-- ---------- V9_add_base_currency_multi.sql ----------
-- 본사 멀티 기준화폐 지원 (최대 3종, comma-separated 예: KRW,USD,JPY)
ALTER TABLE tb_merchant_profile ALTER COLUMN base_currency TYPE VARCHAR(30);

-- ---------- V10_add_pg_trnsctn_origin.sql ----------
-- PostgreSQL 등 영구 DB용. H2 dev는 ddl-auto:create-drop 로 엔티티 반영.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS origin VARCHAR(20);

-- ---------- V11_notify_env_and_inbound.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
CREATE TABLE IF NOT EXISTS tb_hq_notify_env_config (
    id BIGSERIAL PRIMARY KEY,
    ingress_token VARCHAR(64) NOT NULL UNIQUE,
    public_base_url VARCHAR(500),
    auto_void_yn VARCHAR(1),
    email_void_yn VARCHAR(1),
    auto_refund_yn VARCHAR(1),
    force_refund_yn VARCHAR(1),
    auto_void_after_hours INTEGER,
    notify_ok_response VARCHAR(500),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_pg_notify_inbound (
    id BIGSERIAL PRIMARY KEY,
    mid VARCHAR(80),
    root_no VARCHAR(40),
    merchant_id VARCHAR(20),
    org_unit_id BIGINT,
    raw_body TEXT,
    content_type VARCHAR(120),
    client_ip VARCHAR(64),
    process_status VARCHAR(20),
    error_message VARCHAR(500),
    created_at TIMESTAMP
);

ALTER TABLE tb_merchant_pg_binding ADD COLUMN IF NOT EXISTS root_no VARCHAR(40);

-- ---------- V12_user_hq_otp_account_access.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)

ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS otp_required_yn VARCHAR(1) DEFAULT 'Y';

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS org_unit_code VARCHAR(32);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS permission_group_nm VARCHAR(100);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_registered_yn VARCHAR(1) DEFAULT 'N';

CREATE TABLE IF NOT EXISTS tb_user_comp_access (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    comp_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_user_comp_access UNIQUE (username, comp_code)
);

-- ---------- V13_user_view_setting.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)

CREATE TABLE IF NOT EXISTS tb_user_view_setting (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    page_url VARCHAR(200) NOT NULL,
    selected_keys_json TEXT NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_user_view_setting UNIQUE (username, page_url)
);

-- ---------- V14_commission_template_columns.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS policy_name VARCHAR(100);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS deploy_yn VARCHAR(1) DEFAULT 'N';

-- ---------- V15_user_policy_and_notify_policy.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS user_type VARCHAR(20);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS assistant_role_type VARCHAR(20);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS parent_username VARCHAR(50);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS menu_policy_code VARCHAR(50);

ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS otp_policy_mode VARCHAR(20) DEFAULT 'NOTI';
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS password_policy_mode VARCHAR(20) DEFAULT 'NOTI';
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS forgot_password_enabled_yn VARCHAR(1) DEFAULT 'N';

-- ---------- V16_user_manager_control.sql ----------
-- 사용자관리 권한 기능 토글 (총본사 환경설정)
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manager_user_control_enabled_yn VARCHAR(1) DEFAULT 'N';

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manager_password_reset_enabled_yn VARCHAR(1) DEFAULT 'N';

-- ---------- V17_hq_notify_targets.sql ----------
CREATE TABLE IF NOT EXISTS tb_hq_notify_target (
    id BIGSERIAL PRIMARY KEY,
    target_code VARCHAR(50) NOT NULL UNIQUE,
    target_name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    use_yn VARCHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP
);

-- ---------- V18_distribution_fee_config.sql ----------
CREATE TABLE IF NOT EXISTS tb_distribution_fee_config (
    id BIGSERIAL PRIMARY KEY,
    comp_id VARCHAR(32) NOT NULL UNIQUE,
    hq_rate NUMERIC(5,2) DEFAULT 0,
    regional_rate NUMERIC(5,2) DEFAULT 0,
    master_rate NUMERIC(5,2) DEFAULT 0,
    branch_rate NUMERIC(5,2) DEFAULT 0,
    agency_rate NUMERIC(5,2) DEFAULT 0,
    updated_at TIMESTAMP
);

-- ---------- V19_pg_trnsctn_chillpay_fields.sql ----------
-- ChillPay(칠페이) 거래내역·엑셀 필드 (PostgreSQL). H2 dev는 JPA ddl-auto:create-drop 로 동기화.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS order_no VARCHAR(64);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS customer_id VARCHAR(100);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS customer_nm VARCHAR(200);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS payment_channel VARCHAR(80);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS icopay_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_fee_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS total_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS route_no VARCHAR(32);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_payment_status VARCHAR(50);
-- Hibernate validate: String + length 1 → VARCHAR(1). CHAR(1)/bpchar 는 타입 불일치 오류 발생.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS settled_yn VARCHAR(1) DEFAULT 'N';

-- ---------- V20_pg_trnsctn_chill_transaction_id.sql ----------
-- 칠페이(ChillPay) 측 TransactionId — 우리 trn_id와 별도 보관
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_transaction_id VARCHAR(64);

-- ---------- V21_hq_notify_mapping.sql ----------
-- 본사설정: PG 노티(CALLBACK/RESULT 등) → 전산 화면·그리드 필드 매핑 정의 (JSON)
CREATE TABLE IF NOT EXISTS tb_hq_notify_mapping (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    mapping_json TEXT,
    updated_at TIMESTAMP
);

-- ---------- V22_org_view_column_allowance.sql ----------
-- 총본사 → 본사(REGIONAL) 단위 VIEW SETTING 허용 컬럼 상한
CREATE TABLE IF NOT EXISTS tb_org_view_column_allowance (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    regional_org_code VARCHAR(50) NOT NULL,
    page_url VARCHAR(200) NOT NULL,
    allowed_keys_json TEXT NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_org_view_col_allow UNIQUE (regional_org_code, page_url)
);

-- ---------- V23_commission_enhance.sql ----------
-- 유통 수수료: 조직별 건당수수료, 영업점 비율, 적용시작일
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS hq_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS regional_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS master_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS branch_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS agency_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS sales_office_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS sales_office_rate DECIMAL(5, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS apply_start_date DATE;

-- 수수료 변경 이력: 스냅샷 JSON, 변경자
-- PostgreSQL: CLOB 없음 → TEXT (JPA columnDefinition = TEXT 와 동일)
ALTER TABLE tb_commission_history ADD COLUMN IF NOT EXISTS snapshot_json TEXT;
ALTER TABLE tb_commission_history ADD COLUMN IF NOT EXISTS changed_by VARCHAR(100);

-- ---------- V24_settlement_calc_cycle_extend.sql ----------
-- 정산주기 코드 확장 (실시간·분·시간·D+N·W+N·Weekly2 등)
ALTER TABLE tb_settlement_setting ALTER COLUMN calc_cycle TYPE VARCHAR(64);

-- ---------- V25_merchant_settlement_method_extend.sql ----------
-- 가맹점 정산방법 확장: 정산구분(calc_proc_type), 정산최소금액(calc_min_amt), 이체시간(transfer_exec_time)
-- transfer_type 컬럼 의미 변경: 이체및송금구분(MANUAL / AUTO / NONE). 기존 FUMBANKING 값은 calc_proc_type 으로 이관.

ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS calc_proc_type VARCHAR(20);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS calc_min_amt NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS transfer_exec_time TIME;

-- 레거시 transfer_type(정산+이체 혼합) → 정산구분
UPDATE tb_settlement_setting SET calc_proc_type = CASE UPPER(COALESCE(transfer_type, ''))
    WHEN 'FUMBANKING' THEN 'FUMBANKING'
    WHEN 'AUTO' THEN 'AUTO'
    ELSE 'MANUAL'
END;

-- 이체및송금구분: 펌뱅킹 정산이면 이체는 자동 연동, 자동 정산이면 이체도 자동, 그 외 수동
UPDATE tb_settlement_setting SET transfer_type = CASE UPPER(COALESCE(calc_proc_type, 'MANUAL'))
    WHEN 'FUMBANKING' THEN 'AUTO'
    WHEN 'AUTO' THEN 'AUTO'
    ELSE 'MANUAL'
END;

-- ---------- V26_rolling_reserve_hold_meta.sql ----------
-- 담보금(롤링) 내역: 적용일·보류 영업일 수·해지 시각
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS hold_start_date DATE;
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS hold_business_days INT;
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS released_at TIMESTAMP;

-- ---------- V26_settlement_withdraw_restrict.sql ----------
-- 가맹점 출금제한 유형(tb_settlement_setting.withdraw_restrict_type)
-- 이체및송금구분 코드 확장(AUTO_NO_MANUAL, ARBITRARY 등)

ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS withdraw_restrict_type VARCHAR(32);

ALTER TABLE tb_settlement_setting ALTER COLUMN transfer_type TYPE VARCHAR(32);

-- ---------- V27_hq_fee_recall_vat_policy.sql ----------
-- 본사 환경설정: 환수금 수수료 포함 여부, 정산 VAT 적용 여부
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS recall_include_fee_yn VARCHAR(1) DEFAULT 'N';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS settlement_vat_apply_yn VARCHAR(1) DEFAULT 'Y';

-- ---------- V28_balance_deduction.sql ----------
-- 잔액/미수금관리 수동 차감 이력
CREATE TABLE IF NOT EXISTS tb_balance_deduction (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    memo VARCHAR(300),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT now()
);

-- ---------- V29_hq_business_day_settings.sql ----------
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS business_day_settings_json TEXT;

-- ---------- V30_user_password_must_change.sql ----------
-- PostgreSQL 등 영구 DB용 (H2 dev: JPA ddl-auto 로 엔티티 반영)

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS password_must_change_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- ---------- V31_hq_notify_target_channel.sql ----------
-- 총판 노티 대상: CALLBACK / RESULT 구분 (PostgreSQL 수동 적용용; 로컬 H2는 JPA ddl-auto로 컬럼 반영)
ALTER TABLE tb_hq_notify_target ADD COLUMN IF NOT EXISTS channel_type VARCHAR(16);
UPDATE tb_hq_notify_target SET channel_type = 'CALLBACK' WHERE channel_type IS NULL OR TRIM(channel_type) = '';
ALTER TABLE tb_hq_notify_target ALTER COLUMN channel_type SET DEFAULT 'CALLBACK';
ALTER TABLE tb_hq_notify_target ALTER COLUMN channel_type SET NOT NULL;

-- ---------- V32_org_page_permission.sql ----------
-- 조직(OrgLevel)별 화면(URL) 접근 권한: NONE / OBSERVER / MODIFY / DELETE
CREATE TABLE IF NOT EXISTS tb_org_page_permission (
    id              BIGSERIAL PRIMARY KEY,
    org_level       VARCHAR(32) NOT NULL,
    page_url        VARCHAR(256) NOT NULL,
    menu_id         VARCHAR(32),
    permission      VARCHAR(16) NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_page_perm UNIQUE (org_level, page_url)
);
CREATE INDEX IF NOT EXISTS idx_org_page_perm_org ON tb_org_page_permission(org_level);

-- ---------- V33_org_unit_page_permission.sql ----------
-- 조직 단위(tb_org_unit)별 화면 권한 오버라이드 + 단계 기본/개별 모드
-- PostgreSQL: 한 번에 NOT NULL + DEFAULT. (로컬 H2 dev는 application-dev ddl-auto로 반영; 엔티티는 H2 호환 위해 nullable 매핑)
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS page_permission_mode VARCHAR(20) NOT NULL DEFAULT 'LEVEL_DEFAULT';

CREATE TABLE IF NOT EXISTS tb_org_unit_page_permission (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    page_url        VARCHAR(256) NOT NULL,
    menu_id         VARCHAR(32),
    permission      VARCHAR(16) NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_unit_page_perm UNIQUE (org_unit_id, page_url),
    CONSTRAINT fk_org_unit_page_perm_ou FOREIGN KEY (org_unit_id) REFERENCES tb_org_unit(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_org_unit_page_perm_ou ON tb_org_unit_page_permission(org_unit_id);

-- ---------- V34_user_mobile_status.sql ----------
-- 사용자관리: 연락처, 계정상태(사용/미사용/영구정지), 미사용 전환 사유
-- PostgreSQL 등 영구 DB용 (H2 dev: JPA ddl-auto 로 엔티티 반영 가능)

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS mobile VARCHAR(64);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS user_status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS inactive_reason VARCHAR(500);

UPDATE tb_user SET user_status = CASE WHEN enabled = true THEN 'ACTIVE' ELSE 'INACTIVE' END
WHERE user_status IS NULL OR user_status = '';

-- ---------- V35_chillpay_url_payment_hq.sql ----------
-- ChillPay URL(호스티드) 결제: 본사 API 구성에서 Result 경로·Callback URL(노티 미들웨어/직접 PG) 오버라이드
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS chillpay_url_result_path VARCHAR(255);

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS chillpay_url_callback_url VARCHAR(1024);

-- ---------- V36_pg_trnsctn_settled_yn_varchar.sql ----------
-- V19 가 CHAR(1) 로 추가된 DB: Hibernate 가 varchar(1) 을 기대하여 validate 실패할 때 정렬
ALTER TABLE pg_trnsctn
    ALTER COLUMN settled_yn TYPE VARCHAR(1) USING trim(settled_yn)::varchar;

-- ---------- V42_hq_domain_ssl_commission_brand.sql ----------
-- 본사설정 확장: 기본정책(통화·3DS·차지백·비고), 도메인 URL, 서버관리 SSL 경로, 브랜딩 로그인 호스트
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS currency_code VARCHAR(16);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS policy_remark TEXT;
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS fee_3ds_rate NUMERIC(5, 2);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS chargeback_fee_per_tx NUMERIC(12, 0);
UPDATE tb_commission_policy SET currency_code = 'KRW' WHERE currency_code IS NULL;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS public_admin_site_url VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS public_api_base_url VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_ssl_cert_path VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_ssl_le_domain VARCHAR(255);
ALTER TABLE tb_org_branding ADD COLUMN IF NOT EXISTS brand_host VARCHAR(255);

-- V43: 본사·총판 조직별 도메인 URL (tb_org_unit)
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS domain_setting_name VARCHAR(200);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS org_domain_admin_url VARCHAR(500);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS org_domain_api_url VARCHAR(500);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS domain_urls_updated_at TIMESTAMP;

-- V44: 서버관리 호스팅 약정(디스크·트래픽 MB, 기간, 트래픽 사용량 수동)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_disk_mb INTEGER;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_traffic_mb INTEGER;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_start DATE;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_end DATE;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_traffic_used_mb INTEGER;

-- V45: 서버 일별 트래픽·메모리 피크 시계열
CREATE TABLE IF NOT EXISTS tb_server_usage_daily (
    usage_date DATE NOT NULL PRIMARY KEY,
    traffic_bytes BIGINT NOT NULL DEFAULT 0,
    memory_peak_pct DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS tb_server_usage_state (
    id SMALLINT NOT NULL PRIMARY KEY,
    last_net_total_bytes BIGINT,
    updated_at TIMESTAMP
);
INSERT INTO tb_server_usage_state (id, last_net_total_bytes, updated_at)
VALUES (1, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- V46: 서버관리 대시보드 자동 갱신 간격(초), NULL이면 yml 기본
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_ui_refresh_sec INTEGER;

-- #############################################################################
-- SECTION C: check_tables.sql (검증용 SELECT)
-- #############################################################################

-- ============================================================
-- pgdev DB 테이블/컬럼 상태 확인
-- pgAdmin4 Query Tool에서 pgdev 선택 후 실행 (F5)
-- ============================================================

-- 1) public 스키마의 모든 테이블 목록
SELECT '=== 테이블 목록 ===' AS info;
SELECT table_name 
  FROM information_schema.tables 
 WHERE table_schema = 'public' 
   AND table_type = 'BASE TABLE'
 ORDER BY table_name;

-- 2) tb_org_unit 컬럼 확인
SELECT '=== tb_org_unit 컬럼 ===' AS info;
SELECT column_name, data_type, character_maximum_length, is_nullable
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_org_unit'
 ORDER BY ordinal_position;

-- 3) tb_merchant_profile 컬럼 확인 (terminal_count 포함 여부)
SELECT '=== tb_merchant_profile 컬럼 (terminal_count 확인) ===' AS info;
SELECT column_name, data_type
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_merchant_profile'
   AND column_name IN ('terminal_count_terminal', 'terminal_count_web', 'org_unit_id', 'created_at')
 ORDER BY column_name;

-- 4) tb_settlement_setting 컬럼 확인 (엑셀용 새 컬럼)
SELECT '=== tb_settlement_setting 컬럼 (엑셀용 새 컬럼) ===' AS info;
SELECT column_name, data_type
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_settlement_setting'
   AND column_name IN ('pay_limit_month', 'pay_limit_year', 'pay_limit_daily', 
       'same_card_limit_day_web', 'terminal_count_terminal')
 ORDER BY column_name;

-- 5) tb_org_unit 데이터 건수
SELECT '=== tb_org_unit 데이터 건수 ===' AS info;
SELECT COUNT(*) AS cnt FROM tb_org_unit;

