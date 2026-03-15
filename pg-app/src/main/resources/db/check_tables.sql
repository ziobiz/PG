-- ============================================================
-- pgdev DB 테이블/컬럼 상태 확인
-- pgAdmin4 Query Tool에서 pgdev 선택 후 실행 (F5)
-- ============================================================

-- 1) public 스키마의 모든 테이블 목록
SELECT '=== 테이블 목록 ===' AS info;
SELECT table_name 
  FROM information_schema.tables 
 WHERE table_schema = 'public' 
   AND table_type = 'BASE TABLE'
 ORDER BY table_name;

-- 2) tb_org_unit 컬럼 확인
SELECT '=== tb_org_unit 컬럼 ===' AS info;
SELECT column_name, data_type, character_maximum_length, is_nullable
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_org_unit'
 ORDER BY ordinal_position;

-- 3) tb_merchant_profile 컬럼 확인 (terminal_count 포함 여부)
SELECT '=== tb_merchant_profile 컬럼 (terminal_count 확인) ===' AS info;
SELECT column_name, data_type
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_merchant_profile'
   AND column_name IN ('terminal_count_terminal', 'terminal_count_web', 'org_unit_id', 'created_at')
 ORDER BY column_name;

-- 4) tb_settlement_setting 컬럼 확인 (엑셀용 새 컬럼)
SELECT '=== tb_settlement_setting 컬럼 (엑셀용 새 컬럼) ===' AS info;
SELECT column_name, data_type
  FROM information_schema.columns 
 WHERE table_schema = 'public' AND table_name = 'tb_settlement_setting'
   AND column_name IN ('pay_limit_month', 'pay_limit_year', 'pay_limit_daily', 
       'same_card_limit_day_web', 'terminal_count_terminal')
 ORDER BY column_name;

-- 5) tb_org_unit 데이터 건수
SELECT '=== tb_org_unit 데이터 건수 ===' AS info;
SELECT COUNT(*) AS cnt FROM tb_org_unit;
