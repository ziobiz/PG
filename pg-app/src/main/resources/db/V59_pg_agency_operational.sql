-- 결제대행사(tb_pg_agency) 운영 여부: 체크된 대행사만 가맹점 PG 선택·연동에 노출
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS operational_yn VARCHAR(1) DEFAULT 'N';
UPDATE tb_pg_agency SET operational_yn = 'N' WHERE operational_yn IS NULL;
-- 기존 데이터: 사용 중이던 PG는 운영으로 간주(업그레이드 직후 연동·드롭다운 유지). 이후 본사설정에서 [운영 저장]으로 조정.
UPDATE tb_pg_agency SET operational_yn = 'Y' WHERE UPPER(TRIM(COALESCE(use_yn, ''))) = 'Y';
