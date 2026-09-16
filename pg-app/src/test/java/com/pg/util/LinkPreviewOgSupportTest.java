package com.pg.util;

import com.pg.entity.OrgLevel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkPreviewOgSupportTest {

    @Test
    void headquartersAlwaysCustom() {
        assertEquals(LinkPreviewOgSupport.MODE_CUSTOM,
                LinkPreviewOgSupport.normalizeMode(LinkPreviewOgSupport.MODE_FOLLOW_HQ, OrgLevel.HEADQUARTERS));
        assertEquals(LinkPreviewOgSupport.OgSource.SELF,
                LinkPreviewOgSupport.cascade(OrgLevel.HEADQUARTERS, LinkPreviewOgSupport.MODE_FOLLOW_HQ, null, null));
    }

    @Test
    void regionalFollowHqUsesHeadquarters() {
        assertEquals(LinkPreviewOgSupport.OgSource.HQ,
                LinkPreviewOgSupport.cascade(OrgLevel.REGIONAL, LinkPreviewOgSupport.MODE_FOLLOW_HQ, OrgLevel.HEADQUARTERS, LinkPreviewOgSupport.MODE_CUSTOM));
    }

    @Test
    void masterDistFollowsRegionalCustomThenHq() {
        assertEquals(LinkPreviewOgSupport.OgSource.PARENT,
                LinkPreviewOgSupport.cascade(OrgLevel.MASTER_DIST, LinkPreviewOgSupport.MODE_FOLLOW_HQ, OrgLevel.REGIONAL, LinkPreviewOgSupport.MODE_CUSTOM));
        assertEquals(LinkPreviewOgSupport.OgSource.HQ,
                LinkPreviewOgSupport.cascade(OrgLevel.MASTER_DIST, LinkPreviewOgSupport.MODE_FOLLOW_HQ, OrgLevel.REGIONAL, LinkPreviewOgSupport.MODE_FOLLOW_HQ));
    }

    @Test
    void customUsesSelf() {
        assertEquals(LinkPreviewOgSupport.OgSource.SELF,
                LinkPreviewOgSupport.cascade(OrgLevel.REGIONAL, LinkPreviewOgSupport.MODE_CUSTOM, OrgLevel.HEADQUARTERS, LinkPreviewOgSupport.MODE_CUSTOM));
    }

    @Test
    void pickFallsBackEnThenKo() {
        Map<String, String> m = LinkPreviewOgSupport.parseLangMap("{\"EN\":\"Hello\",\"KO\":\"안녕\"}");
        assertEquals("Hello", LinkPreviewOgSupport.pick(m, "TH"));
        assertEquals("안녕", LinkPreviewOgSupport.pick(LinkPreviewOgSupport.parseLangMap("{\"KO\":\"안녕\"}"), "EN"));
    }

    @Test
    void injectReplacesPlaceholderAndTitle() {
        String html = "<html><head><title>PG 통합관리자</title><!--PG_OG_START-->old<!--PG_OG_END--></head></html>";
        String block = LinkPreviewOgSupport.buildOgBlock("ICOPAY", "desc", "https://api.icopay.co.kr/uploads/x.png", "https://api.icopay.co.kr/");
        String out = LinkPreviewOgSupport.injectOgBlock(html, block, "ICOPAY");
        assertTrue(out.contains("og:title"));
        assertTrue(out.contains("ICOPAY"));
        assertFalse(out.contains("old"));
        assertTrue(out.contains("<title>ICOPAY</title>"));
        assertFalse(out.contains("등록된 키"));
    }

    @Test
    void hqHostsIncludeApex() {
        assertTrue(LinkPreviewOgSupport.isHqLoginHost("api.icopay.co.kr"));
        assertTrue(LinkPreviewOgSupport.isHqLoginHost("icopay.co.kr"));
        assertTrue(LinkPreviewOgSupport.isHqLoginHost("www.icopay.co.kr"));
        assertFalse(LinkPreviewOgSupport.isHqLoginHost("hqth.icopay.co.kr"));
    }
}
