-- 비활성카드 — 출처 가맹(업체코드·업체명) 스냅샷 (전역 차단, 집계용 org_unit_id 와 별도 표시)
ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS registered_comp_id VARCHAR(32);

ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS registered_comp_nm VARCHAR(200);

COMMENT ON COLUMN tb_hq_pay_card_blacklist.registered_comp_id IS '등록 출처 업체코드(표시·집계용 스냅샷)';
COMMENT ON COLUMN tb_hq_pay_card_blacklist.registered_comp_nm IS '등록 출처 업체명(표시용 스냅샷)';
