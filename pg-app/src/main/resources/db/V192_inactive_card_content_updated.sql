-- 비활성카드 — 내용 수정 이력(등록일시와 별도)
ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS content_updated_at TIMESTAMP;

ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS content_updated_by VARCHAR(64);

COMMENT ON COLUMN tb_hq_pay_card_blacklist.content_updated_at IS '비활성카드 내용 최근 수정 일시';
COMMENT ON COLUMN tb_hq_pay_card_blacklist.content_updated_by IS '비활성카드 내용 최근 수정자';
