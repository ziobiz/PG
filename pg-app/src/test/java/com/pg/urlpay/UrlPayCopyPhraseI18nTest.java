package com.pg.urlpay;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlPayCopyPhraseI18nTest {

    @Test
    void fillsMissingLangsFromKoreanTitle() {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("KOR", "결제안내");
        Map<String, String> out = UrlPayCopyPhraseI18n.fillMissing(src);
        assertEquals("決済案内", out.get("JPN"));
        assertEquals("Payment information", out.get("ENG"));
        assertEquals("支付说明", out.get("CHN"));
    }

    @Test
    void translatesLinewiseBody() {
        String ko = "이름 입력은 카드에 표시된 이름형식과 동일하게 입력해야 합니다.\n사용카드: VISA, MASTER, JCB, UNIONPAY";
        String jp = UrlPayCopyPhraseI18n.lookup(ko, "JPN");
        assertTrue(jp.contains("カードに記載"));
        assertTrue(jp.contains("ご利用カード"));
    }

    @Test
    void tabTitleOnTheLine() {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("KOR", "온더라인 간편결제 시스템");
        Map<String, String> out = UrlPayCopyPhraseI18n.fillMissing(src);
        assertEquals("オンザラインかんたん決済システム", out.get("JPN"));
    }

    @Test
    void doesNotOverwriteExistingTranslation() {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("KOR", "결제안내");
        src.put("JPN", "カスタム");
        Map<String, String> out = UrlPayCopyPhraseI18n.fillMissing(src);
        assertEquals("カスタム", out.get("JPN"));
    }
}
