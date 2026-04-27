-- 가맹 결제대행사 행별 허용 카드 브랜드(ALL·V/M/J/U/A/D 조합 약어). URL·노티 라우팅 확장용.
ALTER TABLE tb_merchant_pg_binding ADD COLUMN IF NOT EXISTS card_brand_scope VARCHAR(16) NOT NULL DEFAULT 'ALL';
COMMENT ON COLUMN tb_merchant_pg_binding.card_brand_scope IS '허용 카드: ALL|VMJU|VMJ|VM|VJ|MJ|V|M|J|U|A|D (VISA·MASTER·JCB·UNION·AMX·DINERS 약어)';
