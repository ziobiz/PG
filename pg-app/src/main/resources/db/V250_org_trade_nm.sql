-- 본사(REGIONAL)·총판(MASTER_DIST) 거래명세서용 상호명
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS trade_nm VARCHAR(200);

COMMENT ON COLUMN tb_merchant_profile.trade_nm IS '거래명세서 결제대행사 표시 상호명(본사·총판). 업체명(그룹명)과 별도';
