-- 수동 실행용: 노티수령정보(tb_pg_notify_inbound) 중복·루프 echo 일괄 삭제
-- 운영 DB에서 DBA가 직접 실행. Flyway 자동 적용 아님.
-- JAR 배포 후에는 본사설정 > 노티수령정보 화면 [중복 노티 정리] 사용을 권장합니다.

-- 1) 미리보기: 아웃바운드 echo (OUTBOUND_ECHO + pg.payment.status 재유입)
-- SELECT COUNT(*) FROM tb_pg_notify_inbound n
-- WHERE (UPPER(TRIM(COALESCE(n.process_status,''))) = 'OUTBOUND_ECHO'
--   OR (n.raw_body IS NOT NULL AND LEFT(TRIM(n.raw_body),1)=CHR(123)
--       AND n.raw_body ILIKE '%pg.payment.status%'
--       AND (n.raw_body ILIKE '%"trnId"%' OR n.raw_body ILIKE '%"trn_id"%')
--       AND (n.raw_body ILIKE '%"compId"%' OR n.raw_body ILIKE '%"comp_id"%')));

-- 2) 삭제: 아웃바운드 echo (기간 조건 필요 시 created_at 추가)
-- DELETE FROM tb_pg_notify_inbound n
-- WHERE (UPPER(TRIM(COALESCE(n.process_status,''))) = 'OUTBOUND_ECHO'
--   OR (n.raw_body IS NOT NULL AND LEFT(TRIM(n.raw_body),1)=CHR(123)
--       AND n.raw_body ILIKE '%pg.payment.status%'
--       AND (n.raw_body ILIKE '%"trnId"%' OR n.raw_body ILIKE '%"trn_id"%')
--       AND (n.raw_body ILIKE '%"compId"%' OR n.raw_body ILIKE '%"comp_id"%')));

-- 3) 미리보기: 동일 raw_body 중복(최초 id 1건 제외)
-- SELECT COALESCE(SUM(cnt-1),0) FROM (
--   SELECT COUNT(*) cnt FROM tb_pg_notify_inbound
--   WHERE raw_body IS NOT NULL AND LENGTH(TRIM(raw_body))>0
--   GROUP BY raw_body HAVING COUNT(*)>1) t;

-- 4) 삭제: 동일 raw_body 중복
-- DELETE FROM tb_pg_notify_inbound a
-- USING (
--   SELECT raw_body, MIN(id) keep_id FROM tb_pg_notify_inbound
--   WHERE raw_body IS NOT NULL AND LENGTH(TRIM(raw_body))>0
--   GROUP BY raw_body HAVING COUNT(*)>1
-- ) d WHERE a.raw_body=d.raw_body AND a.id>d.keep_id;
