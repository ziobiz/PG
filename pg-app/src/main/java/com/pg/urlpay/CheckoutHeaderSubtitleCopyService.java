package com.pg.urlpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
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
    private final HqApiConfigRepository hqApiConfigRepository;

    public CheckoutHeaderSubtitleCopyService(HqPayCopyTranslationService hqPayCopyTranslationService,
                                             HqApiConfigRepository hqApiConfigRepository) {
        this.hqPayCopyTranslationService = hqPayCopyTranslationService;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    private HqApiConfig hq() {
        return hqApiConfigRepository.findAll().stream().findFirst().orElseGet(HqApiConfig::new);
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

    /** 크립토 안내문구 ACTIVE 시 다국어 1회 번역(시스템 기본 KO면 검수 팩). */
    public void applyCryptoToMerchantProfile(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        String mode = mp.getCryptoPaymentHeaderSubtitleMode();
        String textRaw = mp.getCryptoPaymentHeaderSubtitleText();
        String prevI18n = mp.getCryptoPaymentHeaderSubtitleTextI18n();
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "";
        if (!"ACTIVE".equals(m)) {
            return;
        }
        String text = textRaw != null ? textRaw.trim() : "";
        if (text.isEmpty()) {
            mp.setCryptoPaymentHeaderSubtitleText(null);
            mp.setCryptoPaymentHeaderSubtitleTextI18n(null);
            return;
        }
        if (prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        if (text.equals(CryptoPaymentCopyDefaults.HEADER_KO)) {
            try {
                mp.setCryptoPaymentHeaderSubtitleText(text);
                mp.setCryptoPaymentHeaderSubtitleTextI18n(
                        OM.writeValueAsString(CryptoPaymentCopyDefaults.headerI18n()));
                return;
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        applyChannelActiveI18n(mp, mode, textRaw, prevI18n,
                (t, i18n) -> {
                    mp.setCryptoPaymentHeaderSubtitleText(t);
                    mp.setCryptoPaymentHeaderSubtitleTextI18n(i18n);
                });
    }

    /** 통합결제 안내문구 ACTIVE 시 다국어 1회 번역. */
    public void applyUnifiedToMerchantProfile(MerchantProfile mp) {
        applyChannelActiveI18n(mp,
                mp != null ? mp.getUnifiedPaymentHeaderSubtitleMode() : null,
                mp != null ? mp.getUnifiedPaymentHeaderSubtitleText() : null,
                mp != null ? mp.getUnifiedPaymentHeaderSubtitleTextI18n() : null,
                (text, i18n) -> {
                    mp.setUnifiedPaymentHeaderSubtitleText(text);
                    mp.setUnifiedPaymentHeaderSubtitleTextI18n(i18n);
                });
    }

    public void putCryptoCheckoutFields(Map<String, Object> data, MerchantProfile mp) {
        HqApiConfig hq = hq();
        String subMode = resolveUnifiedMode(
                mp != null ? mp.getCryptoPaymentHeaderSubtitleMode() : null,
                hq.getCryptoPaymentHeaderSubtitleModeDefault(),
                "ACTIVE");
        String subText = coalesceBlank(
                resolveUnifiedText(
                        mp != null ? mp.getCryptoPaymentHeaderSubtitleMode() : null,
                        mp != null ? mp.getCryptoPaymentHeaderSubtitleText() : null,
                        hq.getCryptoPaymentHeaderSubtitleTextDefault()),
                CryptoPaymentCopyDefaults.HEADER_KO);
        String subI18n = coalesceBlank(
                resolveUnifiedI18n(
                        mp != null ? mp.getCryptoPaymentHeaderSubtitleMode() : null,
                        mp != null ? mp.getCryptoPaymentHeaderSubtitleTextI18n() : null,
                        hq.getCryptoPaymentHeaderSubtitleTextDefaultI18n()),
                null);
        Map<String, String> subI18nMap = parseI18n(subI18n);
        if (subI18nMap.isEmpty() && ("ACTIVE".equals(subMode) || "DEFAULT".equals(subMode))) {
            subI18nMap = CryptoPaymentCopyDefaults.headerI18n();
            if ("DEFAULT".equals(subMode)) {
                subMode = "ACTIVE";
            }
        }
        putChannelCheckoutFields(data, mp, subMode, subText, writeI18n(subI18nMap));
    }

    public void putUnifiedCheckoutFields(Map<String, Object> data, MerchantProfile mp) {
        HqApiConfig hq = hq();
        String subMode = resolveUnifiedMode(
                mp != null ? mp.getUnifiedPaymentHeaderSubtitleMode() : null,
                hq.getUnifiedPaymentHeaderSubtitleModeDefault(),
                "ACTIVE");
        String subText = coalesceBlank(
                resolveUnifiedText(
                        mp != null ? mp.getUnifiedPaymentHeaderSubtitleMode() : null,
                        mp != null ? mp.getUnifiedPaymentHeaderSubtitleText() : null,
                        hq.getUnifiedPaymentHeaderSubtitleTextDefault()),
                UnifiedPaymentCopyDefaults.HEADER_KO);
        String subI18n = coalesceBlank(
                resolveUnifiedI18n(
                        mp != null ? mp.getUnifiedPaymentHeaderSubtitleMode() : null,
                        mp != null ? mp.getUnifiedPaymentHeaderSubtitleTextI18n() : null,
                        hq.getUnifiedPaymentHeaderSubtitleTextDefaultI18n()),
                null);
        Map<String, String> subI18nMap = parseI18n(subI18n);
        if (subI18nMap.isEmpty() && "ACTIVE".equals(subMode)) {
            subI18nMap = UnifiedPaymentCopyDefaults.headerI18n();
        }
        putChannelCheckoutFields(data, mp, subMode, subText, writeI18n(subI18nMap));

        putUnifiedSlot(data, "unifiedPaymentNotice",
                mp != null ? mp.getUnifiedPaymentNoticeMode() : null,
                mp != null ? mp.getUnifiedPaymentNoticeText() : null,
                mp != null ? mp.getUnifiedPaymentNoticeTextI18n() : null,
                hq.getUnifiedPaymentNoticeModeDefault(),
                coalesceBlank(hq.getUnifiedPaymentNoticeTextDefault(), UnifiedPaymentCopyDefaults.NOTICE_KO),
                hq.getUnifiedPaymentNoticeTextDefaultI18n(),
                "ACTIVE",
                UnifiedPaymentCopyDefaults.noticeI18n(),
                false);
        putUnifiedSlot(data, "unifiedPaymentExtraFooter",
                mp != null ? mp.getUnifiedPaymentExtraFooterMode() : null,
                mp != null ? mp.getUnifiedPaymentExtraFooterText() : null,
                null,
                hq.getUnifiedPaymentExtraFooterModeDefault(),
                coalesceBlank(hq.getUnifiedPaymentExtraFooterTextDefault(), UnifiedPaymentCopyDefaults.EXTRA_FOOTER_EN),
                null,
                "ACTIVE",
                null,
                true);
    }

    private void putUnifiedSlot(Map<String, Object> data, String prefix,
                                String merchantMode, String merchantText, String merchantI18n,
                                String hqMode, String hqText, String hqI18n, String hqDef,
                                Map<String, String> systemI18n, boolean englishFixed) {
        String mode = resolveUnifiedMode(merchantMode, hqMode, hqDef);
        String text = resolveUnifiedText(merchantMode, merchantText, hqText);
        if (text == null || text.isBlank()) {
            text = hqText;
        }
        String i18nJson = englishFixed ? null : resolveUnifiedI18n(merchantMode, merchantI18n, hqI18n);
        data.put(prefix + "Mode", mode);
        if (text != null && !text.isBlank()) {
            data.put(prefix + "Text", text.trim());
        }
        if (englishFixed) {
            /* 추가 푸터: 언어 전환 없이 영어 원문만 */
            return;
        }
        Map<String, String> i18n = parseI18n(i18nJson);
        if (i18n.isEmpty() && "ACTIVE".equals(mode)) {
            if (systemI18n != null && !systemI18n.isEmpty()) {
                i18n = systemI18n;
            } else if (text != null && !text.isBlank()) {
                i18n = fallbackAll(text.trim());
            }
        }
        if (!i18n.isEmpty()) {
            data.put(prefix + "TextI18n", i18n);
        }
    }

    private static String coalesceBlank(String v, String def) {
        return v != null && !v.isBlank() ? v.trim() : def;
    }

    private static String writeI18n(Map<String, String> m) {
        if (m == null || m.isEmpty()) {
            return null;
        }
        try {
            return OM.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveUnifiedMode(String merchantMode, String hqMode, String hqDef) {
        String m = merchantMode != null ? merchantMode.trim().toUpperCase(Locale.ROOT) : "FOLLOW_HQ";
        if ("FOLLOW_HQ".equals(m) || m.isEmpty() || "DEFAULT".equals(m) || "HQ".equals(m)) {
            String h = hqMode != null ? hqMode.trim().toUpperCase(Locale.ROOT) : "";
            if ("DISABLED".equals(h)) {
                return "DISABLED";
            }
            if ("ACTIVE".equals(h)) {
                return "ACTIVE";
            }
            /* HQ DEFAULT/빈값 → 시스템 기본 문구 노출 */
            return "DEFAULT".equals(hqDef) ? "DEFAULT" : ("DISABLED".equals(hqDef) ? "DISABLED" : "ACTIVE");
        }
        if ("DISABLED".equals(m)) {
            return "DISABLED";
        }
        if ("ACTIVE".equals(m)) {
            return "ACTIVE";
        }
        return "DEFAULT";
    }

    private static String resolveUnifiedText(String merchantMode, String merchantText, String hqText) {
        String m = merchantMode != null ? merchantMode.trim().toUpperCase(Locale.ROOT) : "FOLLOW_HQ";
        if ("FOLLOW_HQ".equals(m) || m.isEmpty() || "DEFAULT".equals(m) || "HQ".equals(m)) {
            return hqText;
        }
        return merchantText;
    }

    private static String resolveUnifiedI18n(String merchantMode, String merchantI18n, String hqI18n) {
        String m = merchantMode != null ? merchantMode.trim().toUpperCase(Locale.ROOT) : "FOLLOW_HQ";
        if ("FOLLOW_HQ".equals(m) || m.isEmpty() || "DEFAULT".equals(m) || "HQ".equals(m)) {
            return hqI18n;
        }
        return merchantI18n;
    }

    /** 통합결제 ACTIVE 문구 저장 시 다국어 1회 번역(노란주의·추가푸터). */
    public void applyUnifiedNoticeToMerchantProfile(MerchantProfile mp) {
        applyChannelActiveI18n(mp,
                mp != null ? mp.getUnifiedPaymentNoticeMode() : null,
                mp != null ? mp.getUnifiedPaymentNoticeText() : null,
                mp != null ? mp.getUnifiedPaymentNoticeTextI18n() : null,
                (text, i18n) -> {
                    mp.setUnifiedPaymentNoticeText(text);
                    mp.setUnifiedPaymentNoticeTextI18n(i18n);
                });
    }

    public void applyUnifiedExtraFooterToMerchantProfile(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        String m = mp.getUnifiedPaymentExtraFooterMode() != null
                ? mp.getUnifiedPaymentExtraFooterMode().trim().toUpperCase(Locale.ROOT) : "";
        if (!"ACTIVE".equals(m)) {
            return;
        }
        String text = mp.getUnifiedPaymentExtraFooterText() != null
                ? mp.getUnifiedPaymentExtraFooterText().trim() : "";
        /* 추가 푸터는 영어 고정 — 번역·i18n JSON 없음 */
        mp.setUnifiedPaymentExtraFooterText(text.isEmpty() ? null : text);
        mp.setUnifiedPaymentExtraFooterTextI18n(null);
    }

    /** 본사 크립토 결제 안내 기본 문구 — ACTIVE 시 5개국어. */
    public void applyCryptoDefaultsToHq(HqApiConfig c) {
        if (c == null) {
            return;
        }
        if (c.getCryptoPaymentHeaderSubtitleModeDefault() == null
                || c.getCryptoPaymentHeaderSubtitleModeDefault().isBlank()) {
            c.setCryptoPaymentHeaderSubtitleModeDefault("ACTIVE");
        }
        if (c.getCryptoPaymentHeaderSubtitleTextDefault() == null
                || c.getCryptoPaymentHeaderSubtitleTextDefault().isBlank()) {
            c.setCryptoPaymentHeaderSubtitleTextDefault(CryptoPaymentCopyDefaults.HEADER_KO);
        }
        applyHqActiveI18n(c.getCryptoPaymentHeaderSubtitleModeDefault(),
                c.getCryptoPaymentHeaderSubtitleTextDefault(),
                c.getCryptoPaymentHeaderSubtitleTextDefaultI18n(),
                (text, i18n) -> {
                    c.setCryptoPaymentHeaderSubtitleTextDefault(text);
                    c.setCryptoPaymentHeaderSubtitleTextDefaultI18n(i18n);
                },
                CryptoPaymentCopyDefaults.headerI18n());
    }

    /** 본사 통합결제 기본 문구: 상단·푸터메시지는 5개국어, 추가푸터는 영어 고정. */
    public void applyUnifiedDefaultsToHq(HqApiConfig c) {
        if (c == null) {
            return;
        }
        if (c.getUnifiedPaymentHeaderSubtitleTextDefault() == null
                || c.getUnifiedPaymentHeaderSubtitleTextDefault().isBlank()) {
            c.setUnifiedPaymentHeaderSubtitleTextDefault(UnifiedPaymentCopyDefaults.HEADER_KO);
        }
        if (c.getUnifiedPaymentNoticeTextDefault() == null
                || c.getUnifiedPaymentNoticeTextDefault().isBlank()) {
            c.setUnifiedPaymentNoticeTextDefault(UnifiedPaymentCopyDefaults.NOTICE_KO);
        }
        if (c.getUnifiedPaymentExtraFooterTextDefault() == null
                || c.getUnifiedPaymentExtraFooterTextDefault().isBlank()) {
            c.setUnifiedPaymentExtraFooterTextDefault(UnifiedPaymentCopyDefaults.EXTRA_FOOTER_EN);
        }
        applyHqActiveI18n(c.getUnifiedPaymentHeaderSubtitleModeDefault(),
                c.getUnifiedPaymentHeaderSubtitleTextDefault(),
                c.getUnifiedPaymentHeaderSubtitleTextDefaultI18n(),
                (text, i18n) -> {
                    c.setUnifiedPaymentHeaderSubtitleTextDefault(text);
                    c.setUnifiedPaymentHeaderSubtitleTextDefaultI18n(i18n);
                },
                UnifiedPaymentCopyDefaults.headerI18n());
        applyHqActiveI18n(c.getUnifiedPaymentNoticeModeDefault(),
                c.getUnifiedPaymentNoticeTextDefault(),
                c.getUnifiedPaymentNoticeTextDefaultI18n(),
                (text, i18n) -> {
                    c.setUnifiedPaymentNoticeTextDefault(text);
                    c.setUnifiedPaymentNoticeTextDefaultI18n(i18n);
                },
                UnifiedPaymentCopyDefaults.noticeI18n());
        /* 추가 푸터: 영어 고정, i18n 미생성 */
        if ("ACTIVE".equalsIgnoreCase(String.valueOf(c.getUnifiedPaymentExtraFooterModeDefault()))) {
            String t = c.getUnifiedPaymentExtraFooterTextDefault() != null
                    ? c.getUnifiedPaymentExtraFooterTextDefault().trim() : "";
            c.setUnifiedPaymentExtraFooterTextDefault(t.isEmpty()
                    ? UnifiedPaymentCopyDefaults.EXTRA_FOOTER_EN : t);
            c.setUnifiedPaymentExtraFooterTextDefaultI18n(null);
        }
    }

    private void applyHqActiveI18n(String mode, String textRaw, String prevI18n, ChannelI18nSetter setter,
                                   Map<String, String> systemPack) {
        if (setter == null) {
            return;
        }
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "";
        if (!"ACTIVE".equals(m)) {
            return;
        }
        String text = textRaw != null ? textRaw.trim() : "";
        if (text.isEmpty()) {
            setter.accept(null, null);
            return;
        }
        if (prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        /* 시스템 기본 한국어와 동일하면 검수된 5개국어 팩 사용 */
        if (systemPack != null && text.equals(systemPack.get("KOR"))) {
            try {
                setter.accept(text, OM.writeValueAsString(systemPack));
                return;
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(text);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(text);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", text);
            }
            setter.accept(text, OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("HQ unified copy i18n failed: {}", e.getMessage());
            try {
                setter.accept(text, OM.writeValueAsString(fallbackAll(text)));
            } catch (Exception e2) {
                setter.accept(text, null);
            }
        }
    }

    @FunctionalInterface
    private interface ChannelI18nSetter {
        void accept(String text, String i18nJson);
    }

    private void applyChannelActiveI18n(MerchantProfile mp, String mode, String textRaw, String prevI18n,
                                        ChannelI18nSetter setter) {
        if (mp == null || setter == null) {
            return;
        }
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "DEFAULT";
        if (!"ACTIVE".equals(m)) {
            return;
        }
        String text = textRaw != null ? textRaw.trim() : "";
        if (text.isEmpty()) {
            setter.accept(null, null);
            return;
        }
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
            setter.accept(text, OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("Channel subtitle i18n failed: {}", e.getMessage());
            try {
                setter.accept(text, OM.writeValueAsString(fallbackAll(text)));
            } catch (Exception e2) {
                setter.accept(text, null);
            }
        }
    }

    private void putChannelCheckoutFields(Map<String, Object> data, MerchantProfile mp,
                                          String mode, String textRaw, String i18nJson) {
        if (data == null) {
            return;
        }
        String m = mode != null && !mode.isBlank() ? mode.trim().toUpperCase(Locale.ROOT) : "DEFAULT";
        if (!"ACTIVE".equals(m) && !"DISABLED".equals(m)) {
            m = "DEFAULT";
        }
        data.put("checkoutHeaderSubtitleMode", m);
        String text = textRaw != null ? textRaw.trim() : "";
        if (!text.isEmpty()) {
            data.put("checkoutHeaderSubtitleText", text);
        }
        Map<String, String> i18n = parseI18n(i18nJson);
        if (i18n.isEmpty() && !text.isEmpty() && "ACTIVE".equals(m)) {
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
