package com.pg.service;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * 업체 등록·수정 DataIntegrityViolationException → 사용자 안내(5개국어 키는 KO 원문).
 */
public final class CompDataIntegrityMessages {

    public static final String MSG_VARCHAR20 =
            "저장 값이 너무 깁니다. 은행명·우편번호·주소국가·웹결제 HTML표시명·결제대행사 코드를 줄이거나, 운영 DB에 V251_merchant_register_varchar20.sql 적용 여부를 확인하세요.";
    public static final String MSG_NOTIFY_URL =
            "저장 중 DB 제약 오류가 났습니다. 노티 URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.";
    public static final String MSG_TRADE_NM =
            "상호명 저장에 실패했습니다. 운영 DB에 db/V250_org_trade_nm.sql 적용 여부를 확인하세요.";
    public static final String MSG_DUPLICATE =
            "저장 중 중복 값이 있습니다. 로그인ID, 결제대행사(PG)·결제구분 중복, 노티 URL 구분을 확인하세요.";
    public static final String MSG_GENERIC =
            "저장 중 DB 제약 오류가 났습니다. 입력 길이·중복 여부를 확인하세요.";

    private CompDataIntegrityMessages() {
    }

    public static String forException(DataIntegrityViolationException e) {
        String cause = "";
        if (e != null && e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null) {
            cause = e.getMostSpecificCause().getMessage();
        } else if (e != null && e.getMessage() != null) {
            cause = e.getMessage();
        }
        String c = cause.toLowerCase();
        if (c.contains("trade_nm")) {
            return MSG_TRADE_NM;
        }
        if (c.contains("noti_url") || c.contains("tb_merchant_notify_url")) {
            return MSG_NOTIFY_URL;
        }
        if (c.contains("varying(20)") || c.contains("character varying(20)")) {
            return MSG_VARCHAR20;
        }
        if (c.contains("duplicate") || c.contains("unique") || c.contains("already exists")) {
            return MSG_DUPLICATE;
        }
        if (c.contains("value too long") || c.contains("22001")) {
            return MSG_VARCHAR20;
        }
        return MSG_GENERIC;
    }
}
