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
 * 웹결제 카드입력 비활성 안내 — 원문 저장 시 다국어 JSON 1회 생성(표시 시 재번역 금지).
 */
@Service
public class UrlPayCardInputCopyService {

    private static final Logger log = LoggerFactory.getLogger(UrlPayCardInputCopyService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final HqPayCopyTranslationService hqPayCopyTranslationService;

    public UrlPayCardInputCopyService(HqPayCopyTranslationService hqPayCopyTranslationService) {
        this.hqPayCopyTranslationService = hqPayCopyTranslationService;
    }

    public void applyToMerchantProfile(MerchantProfile mp, String modeRaw, String textRaw) {
        if (mp == null) {
            return;
        }
        String mode = UrlPayCardInputModeUtil.normalize(modeRaw);
        mp.setUrlPayCardInputMode(mode);
        if (!UrlPayCardInputModeUtil.DISABLED.equals(mode)) {
            mp.setUrlPayCardInputDisabledText(null);
            mp.setUrlPayCardInputDisabledTextI18n(null);
            return;
        }
        String text = textRaw != null ? textRaw.trim() : "";
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        if (text.isEmpty()) {
            mp.setUrlPayCardInputDisabledText(null);
            mp.setUrlPayCardInputDisabledTextI18n(null);
            return;
        }
        String prev = mp.getUrlPayCardInputDisabledText() != null ? mp.getUrlPayCardInputDisabledText().trim() : "";
        String prevI18n = mp.getUrlPayCardInputDisabledTextI18n();
        mp.setUrlPayCardInputDisabledText(text);
        if (text.equals(prev) && prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(text);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(text);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", text);
            }
            mp.setUrlPayCardInputDisabledTextI18n(OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("Card-input disabled text i18n failed: {}", e.getMessage());
            try {
                mp.setUrlPayCardInputDisabledTextI18n(OM.writeValueAsString(fallbackAll(text)));
            } catch (Exception e2) {
                mp.setUrlPayCardInputDisabledTextI18n(null);
            }
        }
    }

    public void putCheckoutFields(Map<String, Object> data, MerchantProfile mp) {
        if (data == null || mp == null) {
            return;
        }
        String mode = UrlPayCardInputModeUtil.normalize(mp.getUrlPayCardInputMode());
        data.put("checkoutCardInputMode", mode);
        if (!UrlPayCardInputModeUtil.DISABLED.equals(mode)) {
            return;
        }
        String text = mp.getUrlPayCardInputDisabledText() != null ? mp.getUrlPayCardInputDisabledText().trim() : "";
        if (!text.isEmpty()) {
            data.put("checkoutCardInputDisabledText", text);
        }
        Map<String, String> i18n = parseI18n(mp.getUrlPayCardInputDisabledTextI18n());
        if (i18n.isEmpty() && !text.isEmpty()) {
            i18n = fallbackAll(text);
        }
        if (!i18n.isEmpty()) {
            data.put("checkoutCardInputDisabledTextI18n", i18n);
        }
    }

    public static String resolveTextForLang(Map<String, Object> ctx, String langCode) {
        if (ctx == null) {
            return "";
        }
        Object i18nObj = ctx.get("checkoutCardInputDisabledTextI18n");
        Map<String, String> i18n = new LinkedHashMap<>();
        if (i18nObj instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    i18n.put(String.valueOf(e.getKey()).trim().toUpperCase(Locale.ROOT), String.valueOf(e.getValue()));
                }
            }
        }
        String lang = normalizeLang(langCode);
        if (!i18n.isEmpty()) {
            String hit = firstNonBlank(i18n.get(lang), i18n.get("KOR"), i18n.get("ENG"));
            if (hit != null) {
                return hit;
            }
        }
        Object plain = ctx.get("checkoutCardInputDisabledText");
        return plain != null ? String.valueOf(plain).trim() : "";
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
        m.put("KOR", text);
        m.put("ENG", text);
        m.put("JPN", text);
        m.put("CHN", text);
        m.put("THA", text);
        return m;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "KOR";
        }
        String u = lang.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("KO")) return "KOR";
        if (u.startsWith("EN")) return "ENG";
        if (u.startsWith("JP") || u.startsWith("JA")) return "JPN";
        if (u.startsWith("CH") || u.startsWith("ZH")) return "CHN";
        if (u.startsWith("TH")) return "THA";
        return "KOR";
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
