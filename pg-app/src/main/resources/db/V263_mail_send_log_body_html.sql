-- 메일관리: 고객에게 보낸 HTML 본문 보관(미리보기·PDF)
ALTER TABLE tb_mail_send_log ADD COLUMN IF NOT EXISTS body_html TEXT;
COMMENT ON COLUMN tb_mail_send_log.body_html IS '발송 HTML 본문(고객 수신과 동일). 없으면 body_preview(평문)로 대체 표시';
