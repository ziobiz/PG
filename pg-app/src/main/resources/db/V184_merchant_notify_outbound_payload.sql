-- 결제통보 전송관리: 웹훅 POST JSON 본문 저장
ALTER TABLE tb_merchant_notify_outbound_log
    ADD COLUMN IF NOT EXISTS payload_body TEXT;

COMMENT ON COLUMN tb_merchant_notify_outbound_log.payload_body IS '가맹점 웹훅으로 POST한 JSON 본문(결제통보 전송관리 조회용)';
