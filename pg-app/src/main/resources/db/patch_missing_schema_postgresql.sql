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

-- V113: URL 결제 금액 하단 통화스케일 안내 — db/V113_url_pay_amount_scale_notice_json.sql 과 동일
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_amount_scale_notice_json TEXT;

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

-- 전산설정관리: 데이터 유형별 보관 기간 JSON — db/V73_hq_ledger_data_retention.sql 과 동일
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS data_retention_policy_json TEXT;

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

-- V83: 노티 수령 — LIVE/RETRY 구분 (db/V83_pg_notify_inbound_ingress_delivery_kind.sql 과 동일)
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS ingress_delivery_kind VARCHAR(16);

-- V173: 노티 수령 merchant_id — tb_org_unit.code(50) 과 맞춤
ALTER TABLE tb_pg_notify_inbound
    ALTER COLUMN merchant_id TYPE VARCHAR(50);

-- V174: process_status 20자 초과 코드 저장
ALTER TABLE tb_pg_notify_inbound
    ALTER COLUMN process_status TYPE VARCHAR(32);

-- V73: 거래 마스터 금액 — 노티 원문 소수 유지(USD 등), JPA precision=20 scale=8 과 일치 (NULL 행은 NULL 유지)
ALTER TABLE pg_trnsctn
  ALTER COLUMN amt_krw TYPE NUMERIC(20, 8) USING amt_krw::NUMERIC(20, 8),
  ALTER COLUMN icopay_amt TYPE NUMERIC(20, 8) USING icopay_amt::NUMERIC(20, 8),
  ALTER COLUMN chill_fee_amt TYPE NUMERIC(20, 8) USING chill_fee_amt::NUMERIC(20, 8),
  ALTER COLUMN total_amt TYPE NUMERIC(20, 8) USING total_amt::NUMERIC(20, 8);

-- V74: 거래 마스터 — 노티 수신 채널(CALLBACK/RESULT). 기존 NOTI 행은 NULL(필터에서 CALL·레거시로 간주)
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS notify_channel_type VARCHAR(20);

-- V78: 전산설정 — 이메일무효(수동무효) 메일 템플릿 (db/V78_hq_ledger_email_void_template.sql 과 동일)
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS email_void_to VARCHAR(255);
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS email_void_subject VARCHAR(500);
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS email_void_body_template TEXT;
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS email_void_company_name VARCHAR(200);
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS email_void_contact_name VARCHAR(200);

-- V79: 후속조치 무효·이메일무효 시각 구간(분) — db/V79_pay_follow_void_time_windows.sql 과 동일
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS auto_void_start_min INTEGER;
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS auto_void_end_min INTEGER;
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS email_void_start_min INTEGER;
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS email_void_end_min INTEGER;

-- V80: 전산설정 — 칠페이 통합내역 동기화·로그 보관 — db/V80_ledger_chillpay_sync_log_retention.sql 과 동일
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS chillpay_tr_init_sync_months INTEGER NOT NULL DEFAULT 3;
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS chillpay_tr_recent_sync_days INTEGER NOT NULL DEFAULT 2;
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS app_log_memory_retention_days INTEGER NOT NULL DEFAULT 30;
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS app_log_file_retention_days INTEGER NOT NULL DEFAULT 90;
UPDATE tb_hq_notify_env_config SET auto_refund_after_days = 7 WHERE auto_refund_after_days IS NULL;
UPDATE tb_hq_notify_env_config SET force_refund_after_days = 0 WHERE force_refund_after_days IS NULL;
UPDATE tb_hq_notify_env_config SET email_void_end_min = 1439 WHERE email_void_end_min IS NULL;

-- V81: 환불 익일 구간 시작 시각(분) — db/V81_pay_follow_email_void_end_refund_start.sql 과 동일
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS auto_refund_window_start_min INTEGER;

-- V84: 노티 수신 대상 ↔ 총판 조직 — db/V84_hq_notify_target_org_unit.sql (FK는 운영에서 DO 블록으로 적용 권장)
ALTER TABLE tb_hq_notify_target ADD COLUMN IF NOT EXISTS org_unit_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_hq_notify_target_org_unit_id ON tb_hq_notify_target (org_unit_id);

-- V87: 전산설정 — 결제 통화(ISO 4217 숫자) — db/V87_hq_ledger_pay_display_currency.sql 과 동일
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS pay_display_currency_iso_num VARCHAR(3) NOT NULL DEFAULT '764';

-- V89: 본사 정산주기 관리 — db/V89_hq_settlement_cycle_def.sql 과 동일
CREATE TABLE IF NOT EXISTS tb_hq_settlement_cycle_def (
    id              BIGSERIAL PRIMARY KEY,
    cycle_code      VARCHAR(64)  NOT NULL,
    display_label   VARCHAR(128),
    description     TEXT,
    sort_order      INT          NOT NULL DEFAULT 0,
    active_yn       VARCHAR(1)   NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_hq_settlement_cycle_code UNIQUE (cycle_code)
);
CREATE INDEX IF NOT EXISTS idx_hq_settlement_cycle_def_sort ON tb_hq_settlement_cycle_def (sort_order, cycle_code);

-- V89만 적용된 DB: 아래 V92~V94를 통합 파일로만 실행하려면 db/patch_after_V89_postgresql.sql 를 사용하세요.

-- V92: 정산 실행 행 — 집계 기간(가맹점정산내역·수수료내역과 동일 거래 창 재현용)
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_from DATE;
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_to DATE;
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_end_at TIMESTAMP WITHOUT TIME ZONE;

-- V93: 환수금(정산 후 환불 자동)·미수금(수동) — 다음 정산 지급액 FIFO 차감
CREATE TABLE IF NOT EXISTS tb_settlement_recovery (
    id                          BIGSERIAL PRIMARY KEY,
    merchant_id                 VARCHAR(50) NOT NULL,
    trn_id                      VARCHAR(20) NOT NULL,
    recall_amount               BIGINT NOT NULL,
    remaining_amount            BIGINT NOT NULL,
    applied_amount              BIGINT NOT NULL DEFAULT 0,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code                 VARCHAR(40) NOT NULL,
    fee_included_yn             VARCHAR(1),
    vat_applied_yn              VARCHAR(1),
    last_applied_settlement_run_id BIGINT,
    memo                        VARCHAR(500),
    created_at                  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_settlement_recovery_trn_reason UNIQUE (trn_id, reason_code)
);
CREATE INDEX IF NOT EXISTS idx_settlement_recovery_merchant_status ON tb_settlement_recovery (merchant_id, status);

CREATE TABLE IF NOT EXISTS tb_merchant_receivable (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         VARCHAR(50) NOT NULL,
    title               VARCHAR(200),
    total_amount        BIGINT NOT NULL,
    remaining_amount    BIGINT NOT NULL,
    applied_amount      BIGINT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code         VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    memo                TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_merchant_receivable_merchant_status ON tb_merchant_receivable (merchant_id, status);

-- V94: 지급보류(Y) 가맹점 — 정산 실행 행을 정산보류내역에 적치(가맹점정산내역·유통망 집계 제외), 해제 시 반영
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS payout_hold_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS payout_hold_remark VARCHAR(800);
CREATE INDEX IF NOT EXISTS idx_settlement_run_payout_hold ON tb_settlement_run (payout_hold_yn, calc_dt);

ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS calc_cycle_snapshot VARCHAR(64);

-- V95: 전산설정 — 통화별 수수료·정산 소수 처리(JSON) — db/V95_hq_ledger_fee_currency_format.sql 과 동일
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS fee_currency_format_json TEXT;

-- V96: 헬로 타임라인 — db/V96_hq_ledger_hello_timeline.sql 과 동일
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS hello_timeline_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS hello_timeline_duration_min INTEGER NOT NULL DEFAULT 10;

-- V97: 총판별 정산주기 슬롯 10개 — db/V97_master_dist_settlement_cycle_slots_10.sql 과 동일
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_6 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_7 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_8 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_9 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_10 VARCHAR(64);

-- V98: 미수금 수동 환수 — db/V98_receivable_recovery_manual.sql 과 동일
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS receivable_recovery_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO';
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS receivable_applied_amt NUMERIC(21, 8);
CREATE TABLE IF NOT EXISTS tb_merchant_receivable_recovery_req (
    id                          BIGSERIAL PRIMARY KEY,
    merchant_receivable_id      BIGINT NOT NULL,
    merchant_id                 VARCHAR(50) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at                TIMESTAMP WITHOUT TIME ZONE,
    requested_by                VARCHAR(100),
    applied_settlement_run_id   BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mrr_req_one_pending_per_recv
    ON tb_merchant_receivable_recovery_req (merchant_receivable_id)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_mrr_req_merchant_status
    ON tb_merchant_receivable_recovery_req (merchant_id, status);

-- V99: 통합정산 예정일(T/D) — db/V99_pg_ext_settlement_expected.sql 과 동일
ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS ext_settle_mode VARCHAR(8) NOT NULL DEFAULT 'OFF',
    ADD COLUMN IF NOT EXISTS ext_settle_lag INTEGER NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_batch_time TIME WITHOUT TIME ZONE NULL;
ALTER TABLE tb_merchant_pg_binding
    ADD COLUMN IF NOT EXISTS ext_settle_mode VARCHAR(8) NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_lag INTEGER NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_batch_time TIME WITHOUT TIME ZONE NULL;

-- V100: 정산주기 예약 + 변경 이력 — db/V100_settlement_calc_cycle_pending_audit.sql 과 동일
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pending_calc_cycle VARCHAR(64);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pending_calc_cycle_at TIMESTAMP;
CREATE TABLE IF NOT EXISTS tb_settlement_calc_cycle_audit (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    merchant_code   VARCHAR(64) NOT NULL,
    from_cycle      VARCHAR(64),
    to_cycle        VARCHAR(64) NOT NULL,
    transition_mode VARCHAR(32) NOT NULL,
    actor_username  VARCHAR(128),
    remark          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_scca_org_created ON tb_settlement_calc_cycle_audit (org_unit_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_scca_merchant_created ON tb_settlement_calc_cycle_audit (merchant_code, created_at DESC);

-- V118: 거래 마스터 — 고객금액·고객통화 (db/V118_pg_trnsctn_display_pay.sql 과 동일)
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS display_cur_type VARCHAR(10);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS display_amt NUMERIC(20, 8);
