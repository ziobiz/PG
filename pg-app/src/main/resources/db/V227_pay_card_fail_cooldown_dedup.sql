-- V227: tb_pay_card_fail_cooldown 중복 행 정리 + org 단위 유니크 인덱스 (패치 실패 복구용)
-- ERROR ux_pay_card_fail_cooldown_pg_hash 중복 시 이 파일만 실행해도 됨.

ALTER TABLE tb_pay_card_fail_cooldown
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT;

DROP INDEX IF EXISTS ux_pay_card_fail_cooldown_pg_hash;

DELETE FROM tb_pay_card_fail_cooldown d
WHERE d.id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY pg_vendor, pan_hash, COALESCE(org_unit_id, 0)
                   ORDER BY COALESCE(updated_at, last_fail_at, created_at) DESC NULLS LAST, id DESC
               ) AS rn
        FROM tb_pay_card_fail_cooldown
    ) t
    WHERE t.rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_pay_card_fail_cooldown_pg_hash_org
    ON tb_pay_card_fail_cooldown (pg_vendor, pan_hash, COALESCE(org_unit_id, 0));
