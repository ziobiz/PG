package com.pg.service;

import com.pg.entity.OutcomeReasonTranslationCache;
import com.pg.repository.OutcomeReasonTranslationCacheRepository;
import com.pg.util.OutcomeReasonPhraseDictionary;
import com.pg.util.TxnOutcomeReasonApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 결제내역 처리사유 — 관리자 UI 언어로 번역.
 * <p>저장 시 {@link #scheduleWarmAllLocales(String)} 로 KO·EN·JP·CH·TH 를 한 번만 AI/사전으로 채우고
 * {@code tb_outcome_reason_translation_cache} 에 보관합니다. 목록 조회 시 캐시만 읽습니다.</p>
 */
@Service
public class OutcomeReasonTranslateService {

    private static final Logger log = LoggerFactory.getLogger(OutcomeReasonTranslateService.class);

    private static final List<String> ALL_LOCALES = List.of("KO", "EN", "JP", "CH", "TH");

    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotLlmCompletionService chatbotLlmCompletionService;
    private final OutcomeReasonTranslationCacheRepository cacheRepository;

    public OutcomeReasonTranslateService(HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                         ChatbotLlmCompletionService chatbotLlmCompletionService,
                                         OutcomeReasonTranslationCacheRepository cacheRepository) {
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
        this.cacheRepository = cacheRepository;
    }

    /**
     * 처리사유가 새로 기록될 때 호출 — 비동기로 5개 언어 캐시를 채웁니다.
     */
    @Async
    public void scheduleWarmAllLocales(String sourceText) {
        try {
            warmAllLocalesForSource(sourceText);
        } catch (Exception e) {
            log.warn("outcome reason warm-all-locales failed: {}", abbrev(e.getMessage(), 300));
        }
    }

    @Transactional
    public void warmAllLocalesForSource(String sourceText) {
        String text = stringVal(sourceText);
        if (text.isBlank()) {
            return;
        }
        for (String locale : ALL_LOCALES) {
            warmOneLocaleIfNeeded(text, locale);
        }
    }

    public void applyToPayListRows(List<Map<String, Object>> rows, String adminUiLocale) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String locale = normalizeLocale(adminUiLocale);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            String raw = stringVal(row.get("outcomeReason"));
            if (!raw.isBlank()) {
                unique.add(raw);
            }
        }
        if (unique.isEmpty()) {
            return;
        }
        Map<String, String> translated = translateBatchFromCacheOnly(unique, locale);
        for (Map<String, Object> row : rows) {
            String raw = stringVal(row.get("outcomeReason"));
            if (raw.isBlank()) {
                continue;
            }
            String display = translated.getOrDefault(raw, raw);
            row.put("outcomeReasonDisplay", display);
            row.put("outcomeReasonPreview", TxnOutcomeReasonApplier.preview(display));
        }
    }

    /**
     * 목록 조회용 — 캐시·사전·원문 동일 언어만 사용(AI 호출 없음).
     */
    public Map<String, String> translateBatchFromCacheOnly(Collection<String> sourceTexts, String adminUiLocale) {
        String locale = normalizeLocale(adminUiLocale);
        Map<String, String> out = new LinkedHashMap<>();
        if (sourceTexts == null || sourceTexts.isEmpty()) {
            return out;
        }
        LinkedHashSet<String> pending = new LinkedHashSet<>();
        for (String raw : sourceTexts) {
            String text = stringVal(raw);
            if (text.isBlank()) {
                continue;
            }
            if (likelyAlreadyInLocale(text, locale)) {
                out.put(text, text);
                continue;
            }
            String dict = OutcomeReasonPhraseDictionary.lookup(text, locale);
            if (dict != null) {
                out.put(text, dict);
                continue;
            }
            pending.add(text);
        }
        if (pending.isEmpty()) {
            return out;
        }
        Map<String, String> cacheHits = loadFromCache(pending, locale);
        out.putAll(cacheHits);
        for (String text : pending) {
            if (!out.containsKey(text)) {
                out.put(text, text);
            }
        }
        return out;
    }

    /** @deprecated 목록 조회는 {@link #translateBatchFromCacheOnly} 사용 */
    @Deprecated
    public Map<String, String> translateBatch(Collection<String> sourceTexts, String adminUiLocale) {
        return translateBatchFromCacheOnly(sourceTexts, adminUiLocale);
    }

    private void warmOneLocaleIfNeeded(String text, String locale) {
        if (isCached(text, locale)) {
            return;
        }
        if (likelyAlreadyInLocale(text, locale)) {
            persistCache(text, locale, text, "NATIVE");
            return;
        }
        String dict = OutcomeReasonPhraseDictionary.lookup(text, locale);
        if (dict != null) {
            persistCache(text, locale, dict, "DICT");
            return;
        }
        translateOneWithAi(text, locale);
    }

    private boolean isCached(String text, String locale) {
        try {
            return cacheRepository.findByCacheKey(cacheKey(text, locale))
                    .map(row -> row.getTranslatedText() != null && !row.getTranslatedText().isBlank())
                    .orElse(false);
        } catch (Exception e) {
            log.debug("outcome reason cache lookup skipped: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, String> loadFromCache(Collection<String> texts, String locale) {
        Map<String, String> hits = new LinkedHashMap<>();
        if (texts == null || texts.isEmpty()) {
            return hits;
        }
        Map<String, String> keyByText = new LinkedHashMap<>();
        for (String text : texts) {
            keyByText.put(text, cacheKey(text, locale));
        }
        try {
            List<OutcomeReasonTranslationCache> rows =
                    cacheRepository.findByCacheKeyIn(keyByText.values());
            Map<String, OutcomeReasonTranslationCache> byKey = new HashMap<>();
            for (OutcomeReasonTranslationCache row : rows) {
                byKey.put(row.getCacheKey(), row);
            }
            for (Map.Entry<String, String> e : keyByText.entrySet()) {
                OutcomeReasonTranslationCache hit = byKey.get(e.getValue());
                if (hit != null && hit.getTranslatedText() != null && !hit.getTranslatedText().isBlank()) {
                    hits.put(e.getKey(), hit.getTranslatedText().trim());
                }
            }
        } catch (Exception e) {
            log.warn("outcome reason translation cache read failed: {}", e.getMessage());
        }
        return hits;
    }

    private void translateOneWithAi(String text, String locale) {
        try {
            Map<String, Object> cfg = hqChatbotAiSettingsService.rawConfigForServerUse();
            String label = targetLanguageLabel(locale);
            String translated = chatbotLlmCompletionService.translateOutcomeReason(cfg, text, label);
            if (translated != null && !translated.isBlank()) {
                persistCache(text, locale, translated.trim(), "AI");
                return;
            }
        } catch (Exception e) {
            log.warn("outcome reason AI translate failed locale={}: {}", locale, abbrev(e.getMessage(), 300));
        }
        persistCache(text, locale, text, "FALLBACK");
    }

    @Transactional
    public void persistCache(String sourceText, String locale, String translated, String providerUsed) {
        try {
            String key = cacheKey(sourceText, locale);
            OutcomeReasonTranslationCache row = cacheRepository.findByCacheKey(key)
                    .orElseGet(OutcomeReasonTranslationCache::new);
            row.setCacheKey(key);
            row.setSourceText(sourceText.trim());
            row.setTargetLocale(normalizeLocale(locale));
            row.setTranslatedText(translated != null ? translated.trim() : "");
            row.setProviderUsed(providerUsed);
            cacheRepository.save(row);
        } catch (Exception e) {
            log.warn("outcome reason translation cache write failed: {}", e.getMessage());
        }
    }

    static String cacheKey(String sourceText, String locale) {
        String payload = sourceText.trim() + "|" + normalizeLocale(locale);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "KO";
        }
        String u = locale.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "EN", "JP", "CH", "TH" -> u;
            default -> "KO";
        };
    }

    static String targetLanguageLabel(String locale) {
        return switch (normalizeLocale(locale)) {
            case "EN" -> "English";
            case "JP" -> "Japanese";
            case "CH" -> "Simplified Chinese";
            case "TH" -> "Thai";
            default -> "Korean";
        };
    }

    static boolean likelyAlreadyInLocale(String text, String locale) {
        if (text == null || text.isBlank()) {
            return true;
        }
        int len = text.length();
        int hangul = 0;
        int cjk = 0;
        int latin = 0;
        int thai = 0;
        for (int i = 0; i < len; ) {
            int cp = text.codePointAt(i);
            if (cp >= 0xAC00 && cp <= 0xD7A3) {
                hangul++;
            } else if ((cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF)) {
                cjk++;
            } else if ((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')) {
                latin++;
            } else if (cp >= 0x0E00 && cp <= 0x0E7F) {
                thai++;
            }
            i += Character.charCount(cp);
        }
        return switch (normalizeLocale(locale)) {
            case "KO" -> hangul >= Math.max(2, len / 4);
            case "CH" -> cjk >= Math.max(2, len / 3) && hangul == 0;
            case "JP" -> cjk >= Math.max(2, len / 3);
            case "EN" -> latin >= Math.max(3, len / 2) && hangul == 0 && cjk == 0;
            case "TH" -> thai >= Math.max(2, len / 4);
            default -> false;
        };
    }

    private static String stringVal(Object o) {
        if (o == null) {
            return "";
        }
        return String.valueOf(o).trim();
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ');
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
