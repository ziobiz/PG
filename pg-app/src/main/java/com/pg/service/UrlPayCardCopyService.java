package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 본사 결제구문설정: URL 결제 폼 카드 섹션 제목·안내 문구(PG별·다국어).
 */
@Service
public class UrlPayCardCopyService {

    private static final ObjectMapper OM = new ObjectMapper();

    public static final String KEY_CARD_SECTION = "cardSection";
    public static final String KEY_CARD_NOTE = "cardNote";
    public static final String KEY_CCD_HINT = "ccdBrandAdaptiveHint";
    /** 결제구문 내용 3 (다국어 맵) — 공개 결제 폼 세 번째 안내 문단 */
    public static final String KEY_CARD_BODY3 = "cardBody3";
    /** 브라우저 탭 제목 (다국어 맵) — 공개 URL 결제 페이지 {@code document.title} */
    public static final String KEY_BROWSER_TAB_TITLE = "browserTabTitle";

    private final HqApiConfigRepository hqApiConfigRepository;

    public UrlPayCardCopyService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public String getConfigJson() {
        return hqApiConfigRepository.findById(1L)
                .map(HqApiConfig::getUrlPayCardCopyConfigJson)
                .orElse(null);
    }

    /**
     * 운영 PG와 일치하는 <strong>활성</strong> 항목 1건의 다국어 맵을 반환.
     * 키: {@link #KEY_CARD_SECTION}, {@link #KEY_CARD_NOTE}, {@link #KEY_CCD_HINT}, {@link #KEY_CARD_BODY3}, {@link #KEY_BROWSER_TAB_TITLE} — 값은 언어코드→문자열.
     */
    public Optional<Map<String, Map<String, String>>> resolveActiveCopyByPg(String operationalPgCd) {
        String pg = operationalPgCd != null ? operationalPgCd.trim().toUpperCase(Locale.ROOT) : "";
        if (pg.isEmpty()) {
            return Optional.empty();
        }
        String raw = getConfigJson();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = OM.readTree(raw);
            JsonNode entries = root.path("entries");
            if (!entries.isArray()) {
                return Optional.empty();
            }
            for (JsonNode e : entries) {
                if (e == null || !e.isObject()) {
                    continue;
                }
                if (!"Y".equalsIgnoreCase(text(e, "activeYn"))) {
                    continue;
                }
                String rowPg = text(e, "pgCd").toUpperCase(Locale.ROOT);
                if (rowPg.isEmpty() || !pgMatches(rowPg, pg)) {
                    continue;
                }
                Map<String, Map<String, String>> out = new LinkedHashMap<>();
                out.put(KEY_CARD_SECTION, langMap(e.get("title")));
                out.put(KEY_CARD_NOTE, langMap(e.get("body1")));
                out.put(KEY_CCD_HINT, langMap(e.get("body2")));
                out.put(KEY_CARD_BODY3, langMap(e.get("body3")));
                out.put(KEY_BROWSER_TAB_TITLE, langMap(e.get("tabTitle")));
                return Optional.of(out);
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** 행 pg_cd 와 운영 PG 코드 일치, 또는 행이 CHILLPAY 이고 운영이 ChillPay 계열인 경우 */
    private static boolean pgMatches(String rowPgUpper, String opPgUpper) {
        if (rowPgUpper.equals(opPgUpper)) {
            return true;
        }
        return "CHILLPAY".equals(rowPgUpper) && ChillPayService.isChillPayFamilyPgCd(opPgUpper);
    }

    private static String text(JsonNode obj, String field) {
        if (obj == null || field == null) {
            return "";
        }
        JsonNode n = obj.get(field);
        return n != null && n.isTextual() ? n.asText("").trim() : "";
    }

    private static Map<String, String> langMap(JsonNode node) {
        Map<String, String> m = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return m;
        }
        node.fields().forEachRemaining(en -> {
            String k = en.getKey() != null ? en.getKey().trim().toUpperCase(Locale.ROOT) : "";
            JsonNode v = en.getValue();
            if (!k.isEmpty() && v != null && v.isTextual()) {
                String s = v.asText("").trim();
                if (!s.isEmpty()) {
                    m.put(k, s);
                }
            }
        });
        return m;
    }
}
