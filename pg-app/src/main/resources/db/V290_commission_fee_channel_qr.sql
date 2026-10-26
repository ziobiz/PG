-- QR 수수료 채널 기본 템플릿. 카드 DEFAULT 정책을 복사해, 고치기 전까지 금액이 같게 둡니다.

INSERT INTO tb_commission_policy (
    scope, policy_name, deploy_yn, fee_channel, currency_code,
    per_tx_fee, cancel_rate, usage_rate, fail_fee, pay_rate, refund_rate,
    void_fee_per_tx, manual_void_fee_per_tx, fee_settlement_per_tx,
    remittance_transfer_fee, usdt_transfer_fee_usd, fee_usdt, fee_fx,
    rolling_pct, rolling_days, fee_3ds_rate, chargeback_fee_per_tx,
    split_pay_fee_pct, split_pay_fixed_fee_per_inst,
    void_settlement_mode, manual_void_settlement_mode, refund_settlement_mode, force_refund_settlement_mode,
    policy_remark, tier_commission_json,
    acquisition_ops_fee_pct, escrow_ops_fee_pct, auth_fee_per_tx,
    created_at, updated_at
)
SELECT
    'DEFAULT_QR', 'QR 기본', COALESCE(deploy_yn, 'Y'), 'QR', currency_code,
    per_tx_fee, cancel_rate, usage_rate, fail_fee, pay_rate, refund_rate,
    void_fee_per_tx, manual_void_fee_per_tx, fee_settlement_per_tx,
    remittance_transfer_fee, usdt_transfer_fee_usd, fee_usdt, fee_fx,
    rolling_pct, rolling_days, fee_3ds_rate, chargeback_fee_per_tx,
    split_pay_fee_pct, split_pay_fixed_fee_per_inst,
    void_settlement_mode, manual_void_settlement_mode, refund_settlement_mode, force_refund_settlement_mode,
    policy_remark, tier_commission_json,
    acquisition_ops_fee_pct, escrow_ops_fee_pct, auth_fee_per_tx,
    NOW(), NOW()
FROM tb_commission_policy
WHERE scope = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM tb_commission_policy WHERE scope = 'DEFAULT_QR');
