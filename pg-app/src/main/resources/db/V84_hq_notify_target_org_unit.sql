-- 본사 노티 수신 대상(tb_hq_notify_target) ↔ 총판 조직: 총판 저장 시 NOTIFY URL과 target_url 일치 시 연결
ALTER TABLE tb_hq_notify_target ADD COLUMN IF NOT EXISTS org_unit_id BIGINT;
DO $v84$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_hq_notify_target_org_unit'
  ) THEN
    ALTER TABLE tb_hq_notify_target
      ADD CONSTRAINT fk_hq_notify_target_org_unit FOREIGN KEY (org_unit_id) REFERENCES tb_org_unit (id) ON DELETE SET NULL;
  END IF;
END
$v84$;
CREATE INDEX IF NOT EXISTS idx_hq_notify_target_org_unit_id ON tb_hq_notify_target (org_unit_id);
COMMENT ON COLUMN tb_hq_notify_target.org_unit_id IS '총판(MASTER_DIST) 등: 노티 URL 1~4에 본사 발급 URL을 넣어 저장한 조직. NULL이면 미연결';
