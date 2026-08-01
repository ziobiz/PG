-- 가맹점 운영기록 (총본사·본사·총판 전용 메모, 저장 시 업체변경이력에 작성자 기록)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS operation_record TEXT;

COMMENT ON COLUMN tb_merchant_profile.operation_record IS
    '가맹점 운영기록(총본사·본사·총판 전용). 변경 시 tb_org_unit_change_log 에 작성자(로그인ID)와 함께 이력.';
