-- V19 가 CHAR(1) 로 추가된 DB: Hibernate 가 varchar(1) 을 기대하여 validate 실패할 때 정렬
ALTER TABLE pg_trnsctn
    ALTER COLUMN settled_yn TYPE VARCHAR(1) USING trim(settled_yn)::varchar;
