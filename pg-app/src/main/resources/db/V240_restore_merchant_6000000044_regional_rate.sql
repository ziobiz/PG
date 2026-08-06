-- V240: 가맹 6000000044 본사 요율% 복구 (업체정보 저장 시 HQ 템플릿 재적용으로 0 초기화된 건)
-- 직전 정상 이력(id=158, 2026-08-05): hq 6.8 + regional 1.7 = 8.5, 건당 100+50=150

UPDATE tb_distribution_fee_config
SET regional_rate = 1.70
WHERE comp_id = '6000000044'
  AND COALESCE(regional_rate, 0) = 0
  AND COALESCE(hq_rate, 0) = 6.80
  AND COALESCE(regional_per_tx_fee, 0) = 50;

UPDATE tb_commission_policy
SET pay_rate = 8.50,
    per_tx_fee = 150.0
WHERE scope = '6000000044'
  AND COALESCE(pay_rate, 0) = 6.80;

INSERT INTO tb_commission_history (comp_id, chg_type, chg_desc, changed_by, snapshot_json, created_at)
SELECT
  '6000000044',
  'COMMISSION',
  '본사 요율% 복구(V240): 업체정보 저장 시 템플릿 재적용으로 0이 된 regional_rate 1.7 복원',
  'system',
  '{"compId":"6000000044","compNm":"Crypto Shinjiro","hqRate":6.8,"regionalRate":1.7,"masterRate":0,"branchRate":0,"agencyRate":0,"salesOfficeRate":0,"hqPerTxFee":100,"regionalPerTxFee":50,"masterPerTxFee":0,"branchPerTxFee":0,"agencyPerTxFee":0,"salesOfficePerTxFee":0,"totalRate":8.5,"totalPerTxFee":150,"payRate":8.5,"policyCur":"JPY","chgNote":"V240 restore"}',
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM tb_commission_history
  WHERE comp_id = '6000000044'
    AND chg_desc LIKE '본사 요율% 복구(V240)%'
);
