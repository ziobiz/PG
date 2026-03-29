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
