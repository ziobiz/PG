-- JPAY 통합내역(포털 Export 자동 동기화) · 전산설정
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_portal_username VARCHAR(255);
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_portal_password VARCHAR(512);
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_tr_init_sync_months INTEGER NOT NULL DEFAULT 3;
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_tr_recent_sync_days INTEGER NOT NULL DEFAULT 2;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_portal_username IS 'JPAY 가맹 포털(merchant.j-pay.net) 로그인 ID';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_portal_password IS 'JPAY 가맹 포털 비밀번호(평문 저장 — SMTP와 동일 패턴)';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_tr_init_sync_months IS 'JPAY 통합내역: 검색 초기화 시 과거 몇 개월';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_tr_recent_sync_days IS 'JPAY 통합내역: 날짜 미지정 조회·동기화 시 최근 며칠';
