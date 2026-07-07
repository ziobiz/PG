-- ElementPay 결제대행사 시드 (THB·URL결제·노티)
INSERT INTO tb_pg_agency (
    pg_cd, pg_nm, api_endpoint, endpoint_noti, endpoint_url_pay, endpoint_api,
    integ_noti_yn, integ_url_pay_yn, integ_web_chatbot_yn, integ_api_yn,
    integ_api_subscription_yn, integ_url_pay_repay_yn,
    use_yn, operational_yn, sandbox_yn, ext_settle_mode, created_at
)
SELECT
    'ELEMENTPAY',
    'ElementPay (THB·카드·PromptPay)',
    'https://api-sbox.elementpay.io',
    'https://api-sbox.elementpay.io',
    'https://api-sbox.elementpay.io',
    'https://api-sbox.elementpay.io',
    'Y', 'Y', 'N', 'N',
    'N', 'N',
    'Y', 'N', 'Y', 'OFF', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_pg_agency WHERE pg_cd = 'ELEMENTPAY');

COMMENT ON COLUMN tb_pg_agency.credentials_extra_json IS
    'PG별 추가 JSON. ElementPay: cardServiceAlias, promptPayServiceAlias, webhookSecretKey(선택)';
