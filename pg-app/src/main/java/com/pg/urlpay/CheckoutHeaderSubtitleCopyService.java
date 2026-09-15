package com.pg.urlpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantProfile;
import com.pg.service.HqPayCopyTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 웹결제 경고메세지(직접입력) — 원문 저장 시 다국어 JSON 1회 생성(표시마다 재번역 금지).
 * 결제창 다국어 메뉴는 저장된 언어 키만 고른다.
 */
@Service
public class CheckoutHeaderSubtitleCopyService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutHeaderSubtitleCopyService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final HqPayCopyTranslationService hqPayCopyTranslationService;

    public CheckoutHeaderSubtitleCopyService(HqPayCopyTranslationService hqPayCopyTranslationService) {
        this.hqPayCopyTranslationService = hqPayCopyTranslationService;
    }

    public void applyToMerchantProfile(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        if (!CheckoutHeaderSubtitleModeUtil.isDirectActive(mp.getWebPaymentHeaderSubtitleMode())) {
            return;
        }
        String text = mp.getWebPaymentHeaderSubtitleText() != null ? mp.getWebPaymentHeaderSubtitleText().trim() : "";
        if (text.isEmpty()) {
            mp.setWebPaymentHeaderSubtitleTextI18n(null);
            return;
        }
        String prevI18n = mp.getWebPaymentHeaderSubtitleTextI18n();
        if (prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(text);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(text);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", text);
            }
            mp.setWebPaymentHeaderSubtitleTextI18n(OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("Checkout subtitle i18n failed: {}", e.getMessage());
            try {
                mp.setWebPaymentHeaderSubtitleTextI18n(OM.writeValueAsString(fallbackAll(text)));
            } catch (Exception e2) {
                mp.setWebPaymentHeaderSubtitleTextI18n(null);
            }
        }
    }

    public void putCheckoutFields(Map<String, Object> data, MerchantProfile mp) {
        if (data == null || mp == null) {
            return;
        }
        Map<String, String> i18n = parseI18n(mp.getWebPaymentHeaderSubtitleTextI18n());
        String text = mp.getWebPaymentHeaderSubtitleText() != null ? mp.getWebPaymentHeaderSubtitleText().trim() : "";
        if (i18n.isEmpty() && !text.isEmpty()
                && CheckoutHeaderSubtitleModeUtil.isDirectActive(mp.getWebPaymentHeaderSubtitleMode())) {
            i18n = fallbackAll(text);
        }
        if (!i18n.isEmpty()) {
            data.put("checkoutHeaderSubtitleTextI18n", i18n);
        }
    }

    private static boolean korMatches(String i18nJson, String text) {
        Map<String, String> m = parseI18n(i18nJson);
        String kor = m.get("KOR");
        return kor != null && kor.trim().equals(text.trim());
    }

    private static Map<String, String> parseI18n(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> raw = OM.readValue(json, new TypeReference<>() {});
            Map<String, String> out = new LinkedHashMap<>();
            if (raw != null) {
                raw.forEach((k, v) -> {
                    if (k != null && v != null && !v.isBlank()) {
                        out.put(k.trim().toUpperCase(Locale.ROOT), v.trim());
                    }
                });
            }
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, String> fallbackAll(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        String t = text != null ? text : "";
        m.put("KOR", t);
        m.put("ENG", t);
        m.put("JPN", t);
        m.put("CHN", t);
        m.put("THA", t);
        return m;
    }
}
