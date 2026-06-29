package com.pg.urlpay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlPayInputModeUtilTest {

    @Test
    void formatCompListLabel_followHqAndLegacyBlank() {
        assertEquals("HQ", UrlPayInputModeUtil.formatCompListLabel(UrlPayInputModeUtil.FOLLOW_HQ));
        assertEquals("HQ", UrlPayInputModeUtil.formatCompListLabel("follow_hq"));
        assertEquals("HQ", UrlPayInputModeUtil.formatCompListLabel(null));
        assertEquals("HQ", UrlPayInputModeUtil.formatCompListLabel(""));
    }

    @Test
    void formatCompListLabel_generalAndTypes() {
        assertEquals("GN", UrlPayInputModeUtil.formatCompListLabel(UrlPayInputModeUtil.GENERAL));
        assertEquals("BA", UrlPayInputModeUtil.formatCompListLabel(UrlPayInputModeUtil.TYPE_BA));
        assertEquals("AA", UrlPayInputModeUtil.formatCompListLabel(UrlPayInputModeUtil.TYPE_AA));
        assertEquals("CN", UrlPayInputModeUtil.formatCompListLabel(UrlPayInputModeUtil.TYPE_CN));
    }
}
