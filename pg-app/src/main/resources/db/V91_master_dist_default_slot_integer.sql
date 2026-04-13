-- V90이 SMALLINT로 적용된 경우 Hibernate(int → INTEGER) 검증 실패 수정
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_master_dist_settlement_cycle_config'
          AND column_name = 'default_slot'
          AND udt_name = 'int2'
    ) THEN
        ALTER TABLE tb_master_dist_settlement_cycle_config
            ALTER COLUMN default_slot TYPE INTEGER USING (default_slot::integer);
    END IF;
END $$;
