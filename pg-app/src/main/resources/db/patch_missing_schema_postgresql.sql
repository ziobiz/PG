-- PostgreSQL: JAR 배포 후 기동 실패(Schema-validation) 시 1회 실행
--  - missing column server_manage_ui_refresh_sec (tb_hq_api_config)
--  - missing table tb_org_unit_assistant_page_permission
-- 적용 후: ./restart-pg-app.sh
--
-- 주의: 명령 예시에 꺾쇠 괄호(예: <호스트>)를 넣지 마세요. bash가 리다이렉션으로 처리합니다.
-- 실제 호스트·DB명·유저·비밀번호로만 바꿔 실행하세요.
--
-- 연결 정보는 앱 환경변수/설정의 JDBC URL과 동일한 DB여야 합니다. (spring.datasource.*)
--
-- 예시 A — URI 한 줄 (비밀번호에 특수문자 있으면 URL 인코딩):
--   psql "postgresql://DB유저:비밀번호@127.0.0.1:5432/DB이름" -f patch_missing_schema_postgresql.sql
--
-- 예시 B — 옵션 분리:
--   PGPASSWORD='비밀번호' psql -h 127.0.0.1 -p 5432 -U DB유저 -d DB이름 -f patch_missing_schema_postgresql.sql
--
-- 예시 C — 배포된 JAR에서 SQL만 꺼내서 파이프 (파일 복사 없이):
--   unzip -p /home/ftpuser/pg-app/build/libs/pg-app-0.0.1-SNAPSHOT.jar \
--     BOOT-INF/classes/db/patch_missing_schema_postgresql.sql | \
--     PGPASSWORD='비밀번호' psql -h 127.0.0.1 -p 5432 -U DB유저 -d DB이름
--
-- 적용 확인:
--   psql ... -c "\d tb_org_unit_assistant_page_permission"
--   psql ... -c "SELECT column_name FROM information_schema.columns WHERE table_name='tb_hq_api_config' AND column_name='server_manage_ui_refresh_sec';"

-- V46: 서버관리 UI 자동 갱신(초)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS server_manage_ui_refresh_sec INTEGER;

-- V47: 담당자 권한그룹별 메뉴 권한
CREATE TABLE IF NOT EXISTS tb_org_unit_assistant_page_permission (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL,
    assistant_role_type VARCHAR(32) NOT NULL,
    page_url VARCHAR(256) NOT NULL,
    menu_id VARCHAR(32),
    permission VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_org_unit_assist_page UNIQUE (org_unit_id, assistant_role_type, page_url)
);

CREATE INDEX IF NOT EXISTS idx_org_unit_assist_page_org ON tb_org_unit_assistant_page_permission(org_unit_id);

-- V48: 총판 노티 URL 저장 — noti_url 500자 초과 시 등록/수정 실패 방지 (JPA validate는 컬럼이 2048과 맞아야 함)
ALTER TABLE tb_merchant_notify_url
  ALTER COLUMN noti_url TYPE VARCHAR(2048);

-- V50: 월간이용료 컬럼 — 고정 금액(정책 통화), NUMERIC(12,0)
ALTER TABLE tb_commission_policy
  ALTER COLUMN usage_rate TYPE NUMERIC(12, 0)
  USING round(COALESCE(usage_rate, 0))::NUMERIC(12, 0);

-- V51: 기타 수수료 4건 (이름·PCT|FIX·값)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_value NUMERIC(15, 4);

-- V52: 차지백 구간 정책 + 수수료정책 연결
CREATE TABLE IF NOT EXISTS tb_chargeback_fee_policy (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    remark TEXT,
    currency_code VARCHAR(8) NOT NULL DEFAULT 'KRW',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS tb_chargeback_fee_tier (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES tb_chargeback_fee_policy(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    count_min INT NOT NULL DEFAULT 0,
    count_max INT,
    fee_per_case NUMERIC(12, 0) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cb_fee_tier_policy ON tb_chargeback_fee_tier(policy_id);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS chargeback_policy_id BIGINT
    REFERENCES tb_chargeback_fee_policy(id) ON DELETE SET NULL;

-- V53: 차지백 정책 기준통화
ALTER TABLE tb_chargeback_fee_policy ADD COLUMN IF NOT EXISTS currency_code VARCHAR(8) NOT NULL DEFAULT 'KRW';

-- V54: 취소·환불 — 건당 고정액(통화 단위). 기존 % 저장값은 검토 후 재입력 권장.
ALTER TABLE tb_commission_policy
  ALTER COLUMN cancel_rate TYPE NUMERIC(12, 0)
  USING ROUND(COALESCE(cancel_rate, 0))::NUMERIC(12, 0);
ALTER TABLE tb_commission_policy
  ALTER COLUMN refund_rate TYPE NUMERIC(12, 0)
  USING ROUND(COALESCE(refund_rate, 0))::NUMERIC(12, 0);

-- V55: 무효·수동무효 건당 수수료 (거래 status 21·22)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS void_fee_per_tx NUMERIC(12, 0) NOT NULL DEFAULT 0;
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS manual_void_fee_per_tx NUMERIC(12, 0) NOT NULL DEFAULT 0;

-- V56: 건당·고정 수수료 소수 첫째 자리(USD·THB 등) — V56_commission_policy_amount_one_decimal.sql 과 동일
ALTER TABLE tb_commission_policy
  ALTER COLUMN per_tx_fee TYPE NUMERIC(12, 1) USING round(per_tx_fee::numeric, 1),
  ALTER COLUMN cancel_rate TYPE NUMERIC(12, 1) USING round(cancel_rate::numeric, 1),
  ALTER COLUMN usage_rate TYPE NUMERIC(12, 1) USING round(usage_rate::numeric, 1),
  ALTER COLUMN fail_fee TYPE NUMERIC(12, 1) USING round(fail_fee::numeric, 1),
  ALTER COLUMN refund_rate TYPE NUMERIC(12, 1) USING round(refund_rate::numeric, 1),
  ALTER COLUMN void_fee_per_tx TYPE NUMERIC(12, 1) USING round(void_fee_per_tx::numeric, 1),
  ALTER COLUMN manual_void_fee_per_tx TYPE NUMERIC(12, 1) USING round(manual_void_fee_per_tx::numeric, 1),
  ALTER COLUMN fee_settlement_per_tx TYPE NUMERIC(12, 1) USING round(fee_settlement_per_tx::numeric, 1),
  ALTER COLUMN chargeback_fee_per_tx TYPE NUMERIC(12, 1) USING round(chargeback_fee_per_tx::numeric, 1);

-- V57: 본사 기본정책 — 조직 단계별 수수료 격자(JSON)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS tier_commission_json TEXT;

-- V60: URL 공개 결제 폼 모드 (FULL/SIMPLE) — db/V60_url_pay_form_mode.sql 과 동일
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_form_mode VARCHAR(20) DEFAULT 'FULL';

-- V61: PG사 API 연동 자격 — db/V61_pg_agency_credentials.sql 과 동일
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS merchant_mid VARCHAR(100);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS api_key VARCHAR(512);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS md5_secret_key VARCHAR(255);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS route_no INTEGER;
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS sandbox_yn VARCHAR(1) DEFAULT 'Y';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS credentials_extra_json TEXT;

-- V63: 결제통화로직설정(JSON) — db/V63_pay_currency_scale_rules.sql 과 동일
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS pay_currency_scale_rules_json TEXT;

-- V64: 결제구문설정(JSON) — db/V64_url_pay_card_copy_config.sql 과 동일
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_card_copy_config_json TEXT;

-- V65: 전산설정관리 — db/V65_hq_ledger_sys_settings.sql 과 동일
CREATE TABLE IF NOT EXISTS tb_hq_ledger_sys_settings (
    id                      BIGINT PRIMARY KEY,
    display_timezone        VARCHAR(64),
    ntp_sync_enabled_yn     VARCHAR(1) NOT NULL DEFAULT 'N',
    ntp_server_list         VARCHAR(500),
    time_sync_interval_min  INTEGER,
    smtp_host               VARCHAR(255),
    smtp_port               INTEGER,
    smtp_tls_yn             VARCHAR(1) NOT NULL DEFAULT 'Y',
    smtp_auth_yn            VARCHAR(1) NOT NULL DEFAULT 'Y',
    smtp_username           VARCHAR(255),
    smtp_password           VARCHAR(512),
    mail_from_address       VARCHAR(255),
    mail_from_name          VARCHAR(200),
    alert_recipient_emails  TEXT,
    email_on_sync_failure_yn       VARCHAR(1) NOT NULL DEFAULT 'N',
    email_daily_digest_yn          VARCHAR(1) NOT NULL DEFAULT 'N',
    email_notify_void_batch_yn     VARCHAR(1) NOT NULL DEFAULT 'N',
    email_notify_refund_batch_yn   VARCHAR(1) NOT NULL DEFAULT 'N',
    memo                    TEXT,
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE
);
INSERT INTO tb_hq_ledger_sys_settings (
    id, ntp_sync_enabled_yn, smtp_tls_yn, smtp_auth_yn,
    email_on_sync_failure_yn, email_daily_digest_yn, email_notify_void_batch_yn, email_notify_refund_batch_yn,
    created_at, updated_at
)
SELECT 1, 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_ledger_sys_settings WHERE id = 1);

-- V66: 전산설정 Y/N 컬럼을 VARCHAR(1)로 (Hibernate·JPA String length=1 과 일치, CHAR(1) bpchar 오류 방지)
ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN ntp_sync_enabled_yn TYPE VARCHAR(1),
    ALTER COLUMN smtp_tls_yn TYPE VARCHAR(1),
    ALTER COLUMN smtp_auth_yn TYPE VARCHAR(1),
    ALTER COLUMN email_on_sync_failure_yn TYPE VARCHAR(1),
    ALTER COLUMN email_daily_digest_yn TYPE VARCHAR(1),
    ALTER COLUMN email_notify_void_batch_yn TYPE VARCHAR(1),
    ALTER COLUMN email_notify_refund_batch_yn TYPE VARCHAR(1);

-- V67: URL 결제 폼 — 브라우저 탭 제목(JSON)·파비콘 경로 (결제구문 PG별 설정과 분리)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_tab_title_json TEXT;
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_favicon_url VARCHAR(500);

-- V71: 조직항목설정 — 화면별 VIEW SETTING 추가 항목(표시명·내부 키)
CREATE TABLE IF NOT EXISTS tb_hq_view_custom_column (
    id              BIGSERIAL PRIMARY KEY,
    page_url        VARCHAR(256) NOT NULL,
    column_key      VARCHAR(80) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_hq_view_custom_col_url_key UNIQUE (page_url, column_key)
);
CREATE INDEX IF NOT EXISTS idx_hq_view_custom_col_page ON tb_hq_view_custom_column(page_url);

-- V72: 노티 수신 로그 — URL 대상코드·채널(CALLBACK/RESULT)
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS notify_target_code VARCHAR(64);
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS notify_channel_type VARCHAR(20);

-- V73: 거래 마스터 금액 — 노티 원문 소수 유지(USD 등), JPA precision=20 scale=8 과 일치 (NULL 행은 NULL 유지)
ALTER TABLE pg_trnsctn
  ALTER COLUMN amt_krw TYPE NUMERIC(20, 8) USING amt_krw::NUMERIC(20, 8),
  ALTER COLUMN icopay_amt TYPE NUMERIC(20, 8) USING icopay_amt::NUMERIC(20, 8),
  ALTER COLUMN chill_fee_amt TYPE NUMERIC(20, 8) USING chill_fee_amt::NUMERIC(20, 8),
  ALTER COLUMN total_amt TYPE NUMERIC(20, 8) USING total_amt::NUMERIC(20, 8);

-- V74: 거래 마스터 — 노티 수신 채널(CALLBACK/RESULT). 기존 NOTI 행은 NULL(필터에서 CALL·레거시로 간주)
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS notify_channel_type VARCHAR(20);
