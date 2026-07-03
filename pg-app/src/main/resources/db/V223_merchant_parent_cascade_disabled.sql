-- 총판·본사 등 상위 조직 미사용/정지에 따라 함께 미사용된 하위 조직 표시.
-- Y = 상위연쇄로 미사용됨(상위가 다시 사용되면 자동 복원 대상)
-- N = 직접(개별) 설정 — 상위 복원과 무관하게 현재 상태 유지
-- 운영(application.yml ddl-auto: validate) 시 1회 실행. 로컬 H2(dev)는 ddl-auto: update 로 자동 반영.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS parent_cascade_disabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_merchant_profile.parent_cascade_disabled_yn IS 'Y=상위연쇄로 미사용됨(상위 복원 시 자동 복원), N=개별 설정';
