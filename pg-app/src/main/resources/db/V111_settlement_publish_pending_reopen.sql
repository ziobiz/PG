-- V101 백필(tb_settlement_run.settlement_publish_sts NULL → payout_hold 가 아니면 DISTRIBUTED) 때문에
-- 정산배포 API(PENDING 만)에 행이 나오지 않던 문제를 보정합니다.
-- 지급보류(Y) 행은 HOLD 유지. 운영 DB에 적용 후 가맹점정산내역은 「정산배포」로 다시 DISTRIBUTED 될 때까지 비어 있을 수 있습니다.
UPDATE tb_settlement_run
SET settlement_publish_sts = 'PENDING'
WHERE UPPER(TRIM(settlement_publish_sts)) = 'DISTRIBUTED'
  AND UPPER(COALESCE(TRIM(payout_hold_yn), 'N')) <> 'Y';
