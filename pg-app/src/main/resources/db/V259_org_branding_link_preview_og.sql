-- 브랜드: LINE·WhatsApp 등 링크 미리보기(Open Graph)
ALTER TABLE tb_org_branding
    ADD COLUMN IF NOT EXISTS og_mode VARCHAR(20) DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS og_title_json VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS og_desc_json VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS og_image_url VARCHAR(500);

COMMENT ON COLUMN tb_org_branding.og_mode IS 'FOLLOW_HQ=총본사·상위 그대로, CUSTOM=직접 입력. 총본사는 CUSTOM으로 저장';
COMMENT ON COLUMN tb_org_branding.og_title_json IS 'og:title 언어별 JSON {KO,EN,JP,CH,TH}';
COMMENT ON COLUMN tb_org_branding.og_desc_json IS 'og:description 언어별 JSON {KO,EN,JP,CH,TH}';
COMMENT ON COLUMN tb_org_branding.og_image_url IS 'og:image URL (미입력 시 로고·메인 폴백)';

UPDATE tb_org_branding b
SET og_mode = 'CUSTOM'
FROM tb_org_unit u
WHERE b.org_unit_id = u.id
  AND u.org_level = 'HEADQUARTERS'
  AND (b.og_mode IS NULL OR b.og_mode = 'FOLLOW_HQ');
