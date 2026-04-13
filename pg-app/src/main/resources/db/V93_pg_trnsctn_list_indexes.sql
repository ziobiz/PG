-- 결제 목록·집계: 기간(created_at) + 가맹(merchant_id) + 상태(status) 조합 조회 부하 완화
-- 운영 PostgreSQL에서 수동 실행 또는 기존 마이그레이션 절차에 포함

CREATE INDEX IF NOT EXISTS idx_pg_trnsctn_merchant_created
    ON pg_trnsctn (merchant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_pg_trnsctn_status_created
    ON pg_trnsctn (status, created_at);
