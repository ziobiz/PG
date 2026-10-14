-- V277: 가맹 크립토 보증금·보류일 — 본사 CRYPTO 기본 보류율(따름) 플래그
SET client_min_messages TO WARNING;

ALTER TABLE tb_merchant_crypto_config
    ADD COLUMN IF NOT EXISTS reserve_follow_hq VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_merchant_crypto_config.reserve_follow_hq IS
    'Y=본사 CRYPTO 수수료정책 롤링(담보)%·보류일수 따름. N=가맹 reserve_pct·hold_days 직접입력.';

-- 이미 보증금(%)을 직접 넣은 가맹은 직접입력으로 유지(기존 출금 산정 보존)
UPDATE tb_merchant_crypto_config
SET reserve_follow_hq = 'N'
WHERE COALESCE(reserve_pct, 0) <> 0;
