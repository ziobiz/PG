-- 본사 결제 라우팅: 엑심베이 결제방식 (가맹은 본사설정 따름)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS eximbay_methods_visible VARCHAR(200) NOT NULL DEFAULT 'CARD,PAYPAY,JPCONVBANK,UNIONPAY';

COMMENT ON COLUMN tb_hq_api_config.eximbay_methods_visible IS
    '엑심베이 결제창 노출 수단 CSV(CARD,PAYPAY,JPCONVBANK,UNIONPAY). 신용카드만이면 ICOPAY 카드입력 UI.';
