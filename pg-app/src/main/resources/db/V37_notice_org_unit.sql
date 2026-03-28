-- 공지사항: 작성 조직(업체) 추적 — 목록에 업체명·코드 표시
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS org_unit_id BIGINT;
