-- V267: 대표수탁 primary_yn + 자산 표시 순서 USDC → USDT → EURC
SET client_min_messages TO WARNING;

ALTER TABLE tb_crypto_custody_address
    ADD COLUMN IF NOT EXISTS primary_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_crypto_custody_address.primary_yn IS
    'Y=해당 자산×네트워크의 입금 대표 수탁 주소. 동일 asset+network 중 하나만 Y';

CREATE INDEX IF NOT EXISTS ix_crypto_custody_primary
    ON tb_crypto_custody_address (asset_code, network_code, primary_yn, use_yn);

-- 자산 정렬: USDC(1) → USDT(2) → EURC(3)
UPDATE tb_crypto_asset SET sort_ord = 1 WHERE code = 'USDC';
UPDATE tb_crypto_asset SET sort_ord = 2 WHERE code = 'USDT';
UPDATE tb_crypto_asset SET sort_ord = 3 WHERE code = 'EURC';

-- 기존 활성 주소: 자산×네트워크별 가장 작은 id 를 대표로 승격(이미 Y 있으면 유지)
WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY COALESCE(asset_code, ''), network_code
           ORDER BY CASE WHEN primary_yn = 'Y' THEN 0 ELSE 1 END, id ASC
         ) AS rn
  FROM tb_crypto_custody_address
  WHERE use_yn = 'Y'
)
UPDATE tb_crypto_custody_address c
SET primary_yn = CASE WHEN r.rn = 1 THEN 'Y' ELSE 'N' END
FROM ranked r
WHERE c.id = r.id;
