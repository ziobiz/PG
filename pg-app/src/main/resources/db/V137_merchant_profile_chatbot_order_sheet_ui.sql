-- 챗봇 고객 주문·예약 시트 UI(가맹별 JSON). 비우면 업체성격 기본 + 서버 병합 규칙만 사용.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_order_sheet_ui_json TEXT;

COMMENT ON COLUMN tb_merchant_profile.chatbot_order_sheet_ui_json IS
    '챗봇 주문 시트 필드 표시·라벨·숨김 등(JSON). 키 fields 하위에 ordererName,ordererEmail,ordererPhone,ordererAddr,orderMemo,reservationLocal,reservationCheckout,guestCount,serviceMinutes';
