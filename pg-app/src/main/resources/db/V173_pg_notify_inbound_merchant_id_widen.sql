-- tb_org_unit.code 는 VARCHAR(50) 인데 노티 수신 merchant_id 가 VARCHAR(20) 이면
-- 가맹점 해석(PARSED) 후 INSERT 가 실패해 NOTI 재전송이 HTTP 500 NOTIFY_ERROR 로 떨어집니다.
ALTER TABLE tb_pg_notify_inbound
    ALTER COLUMN merchant_id TYPE VARCHAR(50);
