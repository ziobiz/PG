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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * REDIRECT(RE) 결제 확인창 상·하단 안내 — 본사 기본 + 가맹 우선, CUSTOM 시 5국어 1회 번역.
 */
@Service
public class RedirectCheckoutCopyService {

    private static final Logger log = LoggerFactory.getLogger(RedirectCheckoutCopyService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqPayCopyTranslationService hqPayCopyTranslationService;

    public RedirectCheckoutCopyService(HqApiConfigRepository hqApiConfigRepository,
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
        putSlot(data, "redirectCheckoutLead",
                mp != null ? mp.getRedirectCheckoutLeadMode() : RedirectCheckoutCopyModeUtil.FOLLOW_HQ,
                mp != null ? mp.getRedirectCheckoutLeadText() : null,
                mp != null ? mp.getRedirectCheckoutLeadTextI18n() : null,
                h.getRedirectCheckoutLeadModeDefault(),
                h.getRedirectCheckoutLeadTextDefault(),
                h.getRedirectCheckoutLeadTextDefaultI18n(),
                RedirectCheckoutCopyModeUtil.builtInLeadI18n(),
                RedirectCheckoutCopyModeUtil.DEFAULT_LEAD_KO);
        putSlot(data, "redirectCheckoutHint",
                mp != null ? mp.getRedirectCheckoutHintMode() : RedirectCheckoutCopyModeUtil.FOLLOW_HQ,
                mp != null ? mp.getRedirectCheckoutHintText() : null,
                mp != null ? mp.getRedirectCheckoutHintTextI18n() : null,
                h.getRedirectCheckoutHintModeDefault(),
                h.getRedirectCheckoutHintTextDefault(),
                h.getRedirectCheckoutHintTextDefaultI18n(),
                RedirectCheckoutCopyModeUtil.builtInHintI18n(),
                RedirectCheckoutCopyModeUtil.DEFAULT_HINT_KO);
    }

    private void putSlot(Map<String, Object> data, String prefix,
                         String merchantMode, String merchantText, String merchantI18n,
                         String hqMode, String hqText, String hqI18n,
                         Map<String, String> builtIn, String defaultKo) {
        String effective = RedirectCheckoutCopyModeUtil.resolveEffective(merchantMode, hqMode);
        data.put(prefix + "Mode", effective);
        boolean show = !RedirectCheckoutCopyModeUtil.isDisabled(effective);
        data.put(prefix + "Show", show);
        if (!show) {
            data.put(prefix + "I18n", Map.of());
            return;
        }
        boolean merchantOverrides = merchantMode != null
                && !RedirectCheckoutCopyModeUtil.FOLLOW_HQ.equals(
                RedirectCheckoutCopyModeUtil.normalizeMerchantStored(merchantMode));
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
        if (RedirectCheckoutCopyModeUtil.isCustom(modeForText)) {
            i18n = parseI18n(i18nRaw);
            String t = text != null ? text.trim() : "";
            if (i18n.isEmpty() && !t.isEmpty()) {
                i18n = fallbackAll(t);
            }
            if (i18n.isEmpty()) {
                i18n = new LinkedHashMap<>(builtIn);
            }
        } else {
            /* ACTIVE — 시스템 기본 5국어 */
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
        translateIfCustom(mp.getRedirectCheckoutLeadMode(), mp.getRedirectCheckoutLeadText(),
                mp.getRedirectCheckoutLeadTextI18n(),
                i18n -> mp.setRedirectCheckoutLeadTextI18n(i18n),
                () -> mp.setRedirectCheckoutLeadTextI18n(null));
        translateIfCustom(mp.getRedirectCheckoutHintMode(), mp.getRedirectCheckoutHintText(),
                mp.getRedirectCheckoutHintTextI18n(),
                i18n -> mp.setRedirectCheckoutHintTextI18n(i18n),
                () -> mp.setRedirectCheckoutHintTextI18n(null));
    }

    public void applyToHqConfig(HqApiConfig c) {
        if (c == null) {
            return;
        }
        translateIfCustom(c.getRedirectCheckoutLeadModeDefault(), c.getRedirectCheckoutLeadTextDefault(),
                c.getRedirectCheckoutLeadTextDefaultI18n(),
                i18n -> c.setRedirectCheckoutLeadTextDefaultI18n(i18n),
                () -> c.setRedirectCheckoutLeadTextDefaultI18n(null));
        translateIfCustom(c.getRedirectCheckoutHintModeDefault(), c.getRedirectCheckoutHintTextDefault(),
                c.getRedirectCheckoutHintTextDefaultI18n(),
                i18n -> c.setRedirectCheckoutHintTextDefaultI18n(i18n),
                () -> c.setRedirectCheckoutHintTextDefaultI18n(null));
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
        if (!RedirectCheckoutCopyModeUtil.isCustom(mode)) {
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
            log.warn("Redirect checkout copy i18n failed: {}", e.getMessage());
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

    public Optional<MerchantProfile> findMerchant(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId);
    }
}
