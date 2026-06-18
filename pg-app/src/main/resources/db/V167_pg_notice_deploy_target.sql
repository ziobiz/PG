-- 공지사항 배포 대상 (조직 계층별 노출 범위)
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS deploy_target VARCHAR(30);
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS target_org_unit_ids_json TEXT;

-- 기존 데이터: 작성 조직 하위 전체(ALL)로 간주
UPDATE pg_notice SET deploy_target = 'ALL' WHERE deploy_target IS NULL OR deploy_target = '';
