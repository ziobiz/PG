-- 고객 거래명세서 언어: 결제창 선택(checkout_lang) 다음 폴백용 단말 언어

ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS device_lang VARCHAR(8);

COMMENT ON COLUMN pg_trnsctn.device_lang IS '구매자 단말/브라우저 언어 KOR/ENG/JPN/CHN/THA (영수증 폴백)';
