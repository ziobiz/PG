package com.pg.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompDataIntegrityMessagesTest {

    @Test
    void mapsVarchar20WithoutBlamingNotifyUrl() {
        DataIntegrityViolationException e = wrap("ERROR: value too long for type character varying(20)");
        String msg = CompDataIntegrityMessages.forException(e);
        assertEquals(CompDataIntegrityMessages.MSG_VARCHAR20, msg);
        assertTrue(!msg.contains("노티 URL"));
    }

    @Test
    void mapsTradeNm() {
        DataIntegrityViolationException e = wrap("ERROR: column \"trade_nm\" of relation \"tb_merchant_profile\" does not exist");
        assertEquals(CompDataIntegrityMessages.MSG_TRADE_NM, CompDataIntegrityMessages.forException(e));
    }

    @Test
    void mapsNotifyUrl() {
        DataIntegrityViolationException e = wrap("ERROR: value too long for type character varying(500) Detail: column noti_url");
        assertEquals(CompDataIntegrityMessages.MSG_NOTIFY_URL, CompDataIntegrityMessages.forException(e));
    }

    private static DataIntegrityViolationException wrap(String sqlMsg) {
        return new DataIntegrityViolationException("could not execute statement", new SQLException(sqlMsg));
    }
}
