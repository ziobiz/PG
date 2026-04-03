-- PG 연동 용도(노티 / URL결제 / 웹챗봇 / API) 및 용도별 엔드포인트 (PostgreSQL)
-- 운영: validate 모드이므로 배포 후 반드시 적용. 로컬 dev(H2)는 application-dev.yml ddl-auto 로도 반영 가능.

ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_noti_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_url_pay_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_web_chatbot_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_api_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS endpoint_noti VARCHAR(512);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS endpoint_url_pay VARCHAR(512);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS endpoint_api VARCHAR(512);

-- 기존 행(네 플래그가 모두 N인 행만): 레거시 호환으로 전 용도 Y. 재실행 시 이미 일부만 Y인 행은 건드리지 않음.
UPDATE tb_pg_agency SET
  integ_noti_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_web_chatbot_yn = 'Y',
  integ_api_yn = 'Y'
WHERE integ_noti_yn = 'N' AND integ_url_pay_yn = 'N' AND integ_web_chatbot_yn = 'N' AND integ_api_yn = 'N';

-- 레거시 api_endpoint → API·URL 결제용 신규 컬럼으로 복사(비어 있을 때만)
UPDATE tb_pg_agency SET endpoint_api = api_endpoint
WHERE (endpoint_api IS NULL OR TRIM(endpoint_api) = '') AND api_endpoint IS NOT NULL AND TRIM(api_endpoint) <> '';

UPDATE tb_pg_agency SET endpoint_url_pay = api_endpoint
WHERE (endpoint_url_pay IS NULL OR TRIM(endpoint_url_pay) = '') AND api_endpoint IS NOT NULL AND TRIM(api_endpoint) <> '';
