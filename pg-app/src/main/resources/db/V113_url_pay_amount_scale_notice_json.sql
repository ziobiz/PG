-- URL 결제(pay.html) 금액 필드 하단 ×100 / ÷100 안내 문구 — 본사 결제구문설정에서 노출·다국어 편집
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_amount_scale_notice_json TEXT;

COMMENT ON COLUMN tb_hq_api_config.url_pay_amount_scale_notice_json IS
    'URL 결제 금액 하단 통화스케일 안내 JSON: showMultiplyYn(Y/N), showDivideYn(Y/N), multiply·divide 언어코드(KOR,ENG,...)→문자열 맵';
