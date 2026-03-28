-- ChillPay URL(호스티드) 결제: 본사 API 구성에서 Result 경로·Callback URL(노티 미들웨어/직접 PG) 오버라이드
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS chillpay_url_result_path VARCHAR(255);

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS chillpay_url_callback_url VARCHAR(1024);
