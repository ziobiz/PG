-- 결제창 1회 한도 표시: WARN_ONLY(경고만) / ALWAYS(항상 표시) / DISABLED(비활성·Pay 시만 서버 경고). 가맹은 FOLLOW(총판설정따름)도 허용.
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_ui_mode VARCHAR(16);
