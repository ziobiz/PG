-- 비활성카드 해지 사유 본문 (해지자는 released_by 별도)
ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS released_reason VARCHAR(500);

COMMENT ON COLUMN tb_hq_pay_card_blacklist.released_reason IS '해지 사유 본문(해지자는 released_by 별도)';
