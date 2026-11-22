package com.pg.urlpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.service.HqPayCopyTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 간편결제(/pay-simple) 로고·부제 해석 — FOLLOW_WEB / FOLLOW_API / 직접 / 본사따름.
 */
@Service
public class SimplePayBrandResolver {

    private static final Logger log = LoggerFactory.getLogger(SimplePayBrandResolver.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;
    private final HqPayCopyTranslationService hqPayCopyTranslationService;

    public SimplePayBrandResolver(MerchantProfileRepository merchantProfileRepository,
                                  HqApiConfigRepository hqApiConfigRepository,
                                  CheckoutHeaderLogoResolver checkoutHeaderLogoResolver,
                                  HqPayCopyTranslationService hqPayCopyTranslationService) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
        this.hqPayCopyTranslationService = hqPayCopyTranslationService;
    }

    private HqApiConfig hq() {
        return hqApiConfigRepository.findAll().stream().findFirst().orElseGet(HqApiConfig::new);
    }

    public void putIntoCheckoutMap(Map<String, Object> data, Long orgUnitId) {
        if (data == null || orgUnitId == null) {
            return;
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        HqApiConfig h = hq();
        putLogo(data, orgUnitId, mp, h);
        putBrandSub(data, orgUnitId, mp, h);
    }

    private void putLogo(Map<String, Object> data, Long orgUnitId, MerchantProfile mp, HqApiConfig h) {
        String merchantMode = mp != null ? mp.getSimplePayLogoMode() : SimplePayBrandModeUtil.FOLLOW_HQ;
        String hqMode = h.getSimplePayLogoModeDefault();
        String key = SimplePayBrandModeUtil.resolveLogoKey(merchantMode, hqMode);
        data.put("simplePayLogoMode", SimplePayBrandModeUtil.normalizeLogoMerchantStored(merchantMode));
        data.put("simplePayLogoEffective", key);

        if (SimplePayBrandModeUtil.FOLLOW_WEB.equals(key)) {
            checkoutHeaderLogoResolver.applyToCheckoutMap(data, orgUnitId, null);
            data.put("checkoutHeaderLogoChannel", "SIMPLE_FOLLOW_WEB");
            return;
        }
        if (SimplePayBrandModeUtil.FOLLOW_API.equals(key)) {
            checkoutHeaderLogoResolver.applyToCheckoutMap(data, orgUnitId, "merchant_api");
            data.put("checkoutHeaderLogoChannel", "SIMPLE_FOLLOW_API");
            return;
        }
        if (SimplePayBrandModeUtil.DISABLED.equals(key)) {
            data.put("checkoutHeaderLogoMode", WebPaymentHeaderLogoModeUtil.DISABLED);
            data.put("checkoutHeaderLogoChannel", "SIMPLE");
            data.remove("checkoutHeaderLogoUrl");
            return;
        }
        boolean merchantOwn = mp != null && !SimplePayBrandModeUtil.FOLLOW_HQ.equals(
                SimplePayBrandModeUtil.normalizeLogoMerchantStored(merchantMode));
        String htmlTitle;
        String logoUrl;
        if (merchantOwn) {
            htmlTitle = trim(mp.getSimplePayHtmlTitle());
            logoUrl = trim(mp.getSimplePayLogoUrl());
        } else {
            htmlTitle = trim(h.getSimplePayHtmlTitleDefault());
            logoUrl = trim(h.getSimplePayLogoUrlDefault());
        }
        if (htmlTitle.isBlank()) {
            htmlTitle = "ICOPAY";
        }
        data.put("checkoutHeaderHtmlTitle", htmlTitle);
        data.put("checkoutHeaderLogoChannel", "SIMPLE");
        if (SimplePayBrandModeUtil.HTML.equals(key)) {
            data.put("checkoutHeaderLogoMode", WebPaymentHeaderLogoModeUtil.HTML);
            data.remove("checkoutHeaderLogoUrl");
            return;
        }
        if (SimplePayBrandModeUtil.ACTIVE.equals(key)) {
            data.put("checkoutHeaderLogoMode", WebPaymentHeaderLogoModeUtil.ACTIVE);
            if (!logoUrl.isBlank()) {
                data.put("checkoutHeaderLogoUrl", logoUrl);
            } else {
                data.remove("checkoutHeaderLogoUrl");
            }
            return;
        }
        /* DEFAULT — 총판 로고 */
        CheckoutHeaderLogoResolver.Resolved dist = checkoutHeaderLogoResolver.resolve(orgUnitId);
        data.put("checkoutHeaderLogoMode", WebPaymentHeaderLogoModeUtil.DEFAULT);
        dist.url().ifPresentOrElse(
                u -> data.put("checkoutHeaderLogoUrl", u),
                () -> data.remove("checkoutHeaderLogoUrl"));
    }

    private void putBrandSub(Map<String, Object> data, Long orgUnitId, MerchantProfile mp, HqApiConfig h) {
        String merchantMode = mp != null ? mp.getSimplePayBrandSubMode() : SimplePayBrandModeUtil.FOLLOW_HQ;
        String hqMode = h.getSimplePayBrandSubModeDefault();
        String key = SimplePayBrandModeUtil.resolveSubKey(merchantMode, hqMode);
        data.put("simplePayBrandSubMode", SimplePayBrandModeUtil.normalizeSubMerchantStored(merchantMode));
        data.put("simplePayBrandSubEffective", key);

        if (SimplePayBrandModeUtil.DISABLED.equals(key)) {
            data.put("simplePayBrandSubShow", false);
            data.put("simplePayBrandSubI18n", Map.of());
            return;
        }
        if (SimplePayBrandModeUtil.FOLLOW_WEB.equals(key)) {
            Map<String, Object> tmp = new LinkedHashMap<>();
            checkoutHeaderLogoResolver.applyToCheckoutMap(tmp, orgUnitId, null);
            String subMode = str(tmp.get("checkoutHeaderSubtitleMode"));
            data.put("simplePayBrandSubShow",
                    !WebPaymentHeaderLogoModeUtil.DISABLED.equalsIgnoreCase(subMode)
                            && !"DISABLED".equalsIgnoreCase(subMode));
            Object i18n = tmp.get("checkoutHeaderSubtitleTextI18n");
            if (i18n instanceof Map<?, ?> m && !m.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, String> cast = (Map<String, String>) i18n;
                data.put("simplePayBrandSubI18n", new LinkedHashMap<>(cast));
            } else {
                String t = str(tmp.get("checkoutHeaderSubtitleText"));
                data.put("simplePayBrandSubI18n", t.isBlank()
                        ? SimplePayBrandModeUtil.builtInBrandSubI18n()
                        : fallbackAll(t));
            }
            return;
        }
        if (SimplePayBrandModeUtil.FOLLOW_API.equals(key)) {
            data.put("simplePayBrandSubShow", true);
            data.put("simplePayBrandSubI18n", SimplePayBrandModeUtil.builtInSecurePaymentI18n());
            return;
        }
        if (SimplePayBrandModeUtil.CUSTOM.equals(key)) {
            boolean merchantOwn = mp != null && !SimplePayBrandModeUtil.FOLLOW_HQ.equals(
                    SimplePayBrandModeUtil.normalizeSubMerchantStored(merchantMode));
            String text = merchantOwn ? trim(mp.getSimplePayBrandSubText()) : trim(h.getSimplePayBrandSubTextDefault());
            String i18nRaw = merchantOwn ? mp.getSimplePayBrandSubTextI18n() : h.getSimplePayBrandSubTextDefaultI18n();
            Map<String, String> i18n = parseI18n(i18nRaw);
            if (i18n.isEmpty() && !text.isBlank()) {
                i18n = fallbackAll(text);
            }
            if (i18n.isEmpty()) {
                i18n = SimplePayBrandModeUtil.builtInBrandSubI18n();
            }
            data.put("simplePayBrandSubShow", true);
            data.put("simplePayBrandSubI18n", i18n);
            return;
        }
        /* ACTIVE — 시스템 기본 「간편결제」 */
        data.put("simplePayBrandSubShow", true);
        data.put("simplePayBrandSubI18n", SimplePayBrandModeUtil.builtInBrandSubI18n());
    }

    public void applyTranslationOnSave(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        translateCustom(mp.getSimplePayBrandSubMode(), mp.getSimplePayBrandSubText(),
                mp.getSimplePayBrandSubTextI18n(),
                mp::setSimplePayBrandSubTextI18n,
                () -> mp.setSimplePayBrandSubTextI18n(null));
    }

    public void applyTranslationOnSave(HqApiConfig c) {
        if (c == null) {
            return;
        }
        translateCustom(c.getSimplePayBrandSubModeDefault(), c.getSimplePayBrandSubTextDefault(),
                c.getSimplePayBrandSubTextDefaultI18n(),
                c::setSimplePayBrandSubTextDefaultI18n,
                () -> c.setSimplePayBrandSubTextDefaultI18n(null));
    }

    private void translateCustom(String mode, String text, String prevI18n,
                                 java.util.function.Consumer<String> setter,
                                 Runnable clearer) {
        String u = mode != null ? mode.trim().toUpperCase(java.util.Locale.ROOT) : "";
        if (!SimplePayBrandModeUtil.CUSTOM.equals(u) && !"DIRECT".equals(u)) {
            return;
        }
        String t = text != null ? text.trim() : "";
        if (t.isEmpty()) {
            clearer.run();
            return;
        }
        Map<String, String> prev = parseI18n(prevI18n);
        if (!prev.isEmpty() && t.equals(trim(prev.get("KOR")))) {
            return;
        }
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(t);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(t);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", t);
            }
            setter.accept(OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("Simple pay brand sub i18n failed: {}", e.getMessage());
            try {
                setter.accept(OM.writeValueAsString(fallbackAll(t)));
            } catch (Exception e2) {
                clearer.run();
            }
        }
    }

    private static Map<String, String> parseI18n(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> m = OM.readValue(raw, new TypeReference<Map<String, String>>() {});
            return m != null ? new LinkedHashMap<>(m) : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
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

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
