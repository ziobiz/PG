-- Google OTP 등록용: 시크릿·이메일 인증 대기
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_secret VARCHAR(128);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_pending_secret VARCHAR(128);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_setup_code_hash VARCHAR(128);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_setup_expires_at TIMESTAMP;
