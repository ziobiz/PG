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

/**
 * 간편결제 QR URL(/pay-simple) 상·하단·자동완료 안내.
 * REDIRECT(RE) {@link RedirectCheckoutCopyService} 와 분리 — 간편결제 ON 업체 전용.
 */
@Service
public class SimplePayCheckoutCopyService {

    private static final Logger log = LoggerFactory.getLogger(SimplePayCheckoutCopyService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqPayCopyTranslationService hqPayCopyTranslationService;

    public SimplePayCheckoutCopyService(HqApiConfigRepository hqApiConfigRepository,
                                        MerchantProfileRepository merchantProfileRepository,
                                        HqPayCopyTranslationService hqPayCopyTranslationService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
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
        putSlot(data, "simplePayCheckoutLead",
                mp != null ? mp.getSimplePayCheckoutLeadMode() : SimplePayCheckoutCopyModeUtil.FOLLOW_HQ,
                mp != null ? mp.getSimplePayCheckoutLeadText() : null,
                mp != null ? mp.getSimplePayCheckoutLeadTextI18n() : null,
                h.getSimplePayCheckoutLeadModeDefault(),
                h.getSimplePayCheckoutLeadTextDefault(),
                h.getSimplePayCheckoutLeadTextDefaultI18n(),
                SimplePayCheckoutCopyModeUtil.builtInLeadI18n(),
                SimplePayCheckoutCopyModeUtil.DEFAULT_LEAD_KO);
        putSlot(data, "simplePayCheckoutHint",
                mp != null ? mp.getSimplePayCheckoutHintMode() : SimplePayCheckoutCopyModeUtil.FOLLOW_HQ,
                mp != null ? mp.getSimplePayCheckoutHintText() : null,
                mp != null ? mp.getSimplePayCheckoutHintTextI18n() : null,
                h.getSimplePayCheckoutHintModeDefault(),
                h.getSimplePayCheckoutHintTextDefault(),
                h.getSimplePayCheckoutHintTextDefaultI18n(),
                SimplePayCheckoutCopyModeUtil.builtInHintI18n(),
                SimplePayCheckoutCopyModeUtil.DEFAULT_HINT_KO);
        putSlot(data, "simplePayCheckoutAuto",
                mp != null ? mp.getSimplePayCheckoutAutoMode() : SimplePayCheckoutCopyModeUtil.FOLLOW_HQ,
                mp != null ? mp.getSimplePayCheckoutAutoText() : null,
                mp != null ? mp.getSimplePayCheckoutAutoTextI18n() : null,
                h.getSimplePayCheckoutAutoModeDefault(),
                h.getSimplePayCheckoutAutoTextDefault(),
                h.getSimplePayCheckoutAutoTextDefaultI18n(),
                SimplePayCheckoutCopyModeUtil.builtInAutoI18n(),
                SimplePayCheckoutCopyModeUtil.DEFAULT_AUTO_KO);
    }

    private void putSlot(Map<String, Object> data, String prefix,
                         String merchantMode, String merchantText, String merchantI18n,
                         String hqMode, String hqText, String hqI18n,
                         Map<String, String> builtIn, String defaultKo) {
        String effective = SimplePayCheckoutCopyModeUtil.resolveEffective(merchantMode, hqMode);
        data.put(prefix + "Mode", effective);
        boolean show = !SimplePayCheckoutCopyModeUtil.isDisabled(effective);
        data.put(prefix + "Show", show);
        if (!show) {
            data.put(prefix + "I18n", Map.of());
            return;
        }
        boolean merchantOverrides = merchantMode != null
                && !SimplePayCheckoutCopyModeUtil.FOLLOW_HQ.equals(
                SimplePayCheckoutCopyModeUtil.normalizeMerchantStored(merchantMode));
        String modeForText = effective;
        String text;
        String i18nRaw;
        if (merchantOverrides) {
            text = merchantText;
            i18nRaw = merchantI18n;
        } else {
            text = hqText;
            i18nRaw = hqI18n;
        }
        Map<String, String> i18n;
        if (SimplePayCheckoutCopyModeUtil.isCustom(modeForText)) {
            i18n = parseI18n(i18nRaw);
            String t = text != null ? text.trim() : "";
            if (i18n.isEmpty() && !t.isEmpty()) {
                i18n = fallbackAll(t);
            }
            if (i18n.isEmpty()) {
                i18n = new LinkedHashMap<>(builtIn);
            }
        } else {
            i18n = new LinkedHashMap<>(builtIn);
        }
        if (!i18n.containsKey("KOR") || i18n.get("KOR") == null || i18n.get("KOR").isBlank()) {
            i18n.put("KOR", defaultKo);
        }
        data.put(prefix + "I18n", i18n);
    }

    public void applyToMerchantProfile(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        translateIfCustom(mp.getSimplePayCheckoutLeadMode(), mp.getSimplePayCheckoutLeadText(),
                mp.getSimplePayCheckoutLeadTextI18n(),
                i18n -> mp.setSimplePayCheckoutLeadTextI18n(i18n),
                () -> mp.setSimplePayCheckoutLeadTextI18n(null));
        translateIfCustom(mp.getSimplePayCheckoutHintMode(), mp.getSimplePayCheckoutHintText(),
                mp.getSimplePayCheckoutHintTextI18n(),
                i18n -> mp.setSimplePayCheckoutHintTextI18n(i18n),
                () -> mp.setSimplePayCheckoutHintTextI18n(null));
        translateIfCustom(mp.getSimplePayCheckoutAutoMode(), mp.getSimplePayCheckoutAutoText(),
                mp.getSimplePayCheckoutAutoTextI18n(),
                i18n -> mp.setSimplePayCheckoutAutoTextI18n(i18n),
                () -> mp.setSimplePayCheckoutAutoTextI18n(null));
    }

    public void applyToHqConfig(HqApiConfig c) {
        if (c == null) {
            return;
        }
        translateIfCustom(c.getSimplePayCheckoutLeadModeDefault(), c.getSimplePayCheckoutLeadTextDefault(),
                c.getSimplePayCheckoutLeadTextDefaultI18n(),
                i18n -> c.setSimplePayCheckoutLeadTextDefaultI18n(i18n),
                () -> c.setSimplePayCheckoutLeadTextDefaultI18n(null));
        translateIfCustom(c.getSimplePayCheckoutHintModeDefault(), c.getSimplePayCheckoutHintTextDefault(),
                c.getSimplePayCheckoutHintTextDefaultI18n(),
                i18n -> c.setSimplePayCheckoutHintTextDefaultI18n(i18n),
                () -> c.setSimplePayCheckoutHintTextDefaultI18n(null));
        translateIfCustom(c.getSimplePayCheckoutAutoModeDefault(), c.getSimplePayCheckoutAutoTextDefault(),
                c.getSimplePayCheckoutAutoTextDefaultI18n(),
                i18n -> c.setSimplePayCheckoutAutoTextDefaultI18n(i18n),
                () -> c.setSimplePayCheckoutAutoTextDefaultI18n(null));
    }

    @FunctionalInterface
    private interface I18nSetter {
        void set(String json);
    }

    @FunctionalInterface
    private interface ClearI18n {
        void clear();
    }

    private void translateIfCustom(String mode, String text, String prevI18n,
                                   I18nSetter setter, ClearI18n clearer) {
        if (!SimplePayCheckoutCopyModeUtil.isCustom(mode)) {
            return;
        }
        String t = text != null ? text.trim() : "";
        if (t.isEmpty()) {
            clearer.clear();
            return;
        }
        if (prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, t)) {
            return;
        }
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(t);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(t);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", t);
            }
            setter.set(OM.writeValueAsString(langs));
        } catch (Exception e) {
            log.warn("Simple pay checkout copy i18n failed: {}", e.getMessage());
            try {
                setter.set(OM.writeValueAsString(fallbackAll(t)));
            } catch (Exception e2) {
                clearer.clear();
            }
        }
    }

    private static boolean korMatches(String i18nJson, String text) {
        Map<String, String> m = parseI18n(i18nJson);
        String kor = m.get("KOR");
        return kor != null && kor.trim().equals(text.trim());
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
}
