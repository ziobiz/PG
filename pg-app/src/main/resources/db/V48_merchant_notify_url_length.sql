-- 총판·가맹 노티 URL: 전산노티 수신 URL(토큰 경로 포함)이 500자를 넘으면 저장 시 DB 오류 발생
-- PostgreSQL
ALTER TABLE tb_merchant_notify_url
  ALTER COLUMN noti_url TYPE VARCHAR(2048);
