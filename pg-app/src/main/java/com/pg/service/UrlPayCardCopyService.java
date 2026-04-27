package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.integration.pg.PgVendor;
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
    /** 탭 파비콘 URL — 본사 업로드 경로({@code /uploads/hq/url-pay/…})만 허용 */
    public static final String KEY_FAVICON_URL = "faviconUrl";
    /** URL 결제 결과(pay-result·인라인 완료 카드) 성공 시 큰 제목 */
    public static final String KEY_RESULT_SUCCESS_MAIN = "resultSuccessMain";
    /** URL 결제 결과 성공 시 하단 안내 */
    public static final String KEY_RESULT_SUCCESS_FOOT = "resultSuccessFoot";
    /** URL 결제 결과 실패·취소 시 큰 제목 */
    public static final String KEY_RESULT_FAIL_MAIN = "resultFailMain";
    /** URL 결제 결과 실패·취소 시 하단 안내 */
    public static final String KEY_RESULT_FAIL_FOOT = "resultFailFoot";
    /** 결제 금액 입력란 하단 통화 스케일 안내(×100/÷100) — 언어코드→문자열 맵, PG별 */
    public static final String KEY_AMOUNT_SCALE_NOTICE = "amountScaleNotice";
    /** 금액 하단 안내 노출 여부 — checkout JSON에 boolean {@code amountScaleNoticeShow} 로도 내려감 */
    public static final String KEY_AMOUNT_SCALE_NOTICE_SHOW_YN = "amountScaleNoticeShowYn";

    private static final String SAFE_FAVICON_PREFIX = "/uploads/hq/url-pay/";

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
     * 운영 PG와 일치하는 <strong>활성</strong> 항목 1건을 반환.
     * <p>운영 {@code pg_cd}가 {@code CHILLPAY BL JP THB} 등 확장 코드일 때, 목록 앞쪽의 범용 {@code CHILLPAY} 행이
     * 먼저 매칭되지 않도록 <strong>행 {@code pgCd} 와 운영 코드가 완전히 같을 때를 최우선</strong>하고,
     * 없을 때만 {@code CHILLPAY} 단독 행·ChillPay 계열 폴백({@link #pgMatchesLoose})을 쓴다.</p>
     * 키: {@link #KEY_CARD_SECTION}, {@link #KEY_CARD_NOTE}, {@link #KEY_CCD_HINT}, {@link #KEY_CARD_BODY3}, {@link #KEY_BROWSER_TAB_TITLE} — 값은 언어코드→문자열 맵.
     * {@link #KEY_FAVICON_URL} — 문자열(허용된 업로드 경로만).
     * {@link #KEY_AMOUNT_SCALE_NOTICE} — 금액 하단 안내(맵). 비어 있으면 키 생략, 페이지 기본 I18N.
     * {@code amountScaleNoticeShow} — {@code amountScaleNoticeShowYn} 이 N이 아니면 true(기본).
     */
    public Optional<Map<String, Object>> resolveActiveCopyByPg(String operationalPgCd) {
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
            HqApiConfig hqCfg = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
            Optional<Map<String, Object>> exact = pickActiveCopy(entries, pg, hqCfg, true);
            if (exact.isPresent()) {
                return exact;
            }
            return pickActiveCopy(entries, pg, hqCfg, false);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * @param exactOnly {@code true} 이면 행 {@code pgCd} 가 운영 PG와 <strong>문자열 동일</strong>할 때만 매칭.
     */
    private Optional<Map<String, Object>> pickActiveCopy(
            JsonNode entries, String opPgUpper, HqApiConfig hqCfg, boolean exactOnly) {
        for (JsonNode e : entries) {
            if (e == null || !e.isObject()) {
                continue;
            }
            if (!"Y".equalsIgnoreCase(text(e, "activeYn"))) {
                continue;
            }
            String rowPg = text(e, "pgCd").toUpperCase(Locale.ROOT);
            if (rowPg.isEmpty()) {
                continue;
            }
            if (exactOnly) {
                if (!rowPg.equals(opPgUpper)) {
                    continue;
                }
            } else if (!pgMatchesLoose(rowPg, opPgUpper)) {
                continue;
            }
            return Optional.of(buildActiveCopyMap(e, hqCfg));
        }
        return Optional.empty();
    }

    private Map<String, Object> buildActiveCopyMap(JsonNode e, HqApiConfig hqCfg) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(KEY_CARD_SECTION, langMap(e.get("title")));
        out.put(KEY_CARD_NOTE, langMap(e.get("body1")));
        out.put(KEY_CCD_HINT, langMap(e.get("body2")));
        out.put(KEY_CARD_BODY3, langMap(e.get("body3")));
        applyUrlPayChrome(out, hqCfg, e);
        putLangMapIfNonEmpty(out, KEY_RESULT_SUCCESS_MAIN, e.get("resultSuccessMain"));
        putLangMapIfNonEmpty(out, KEY_RESULT_SUCCESS_FOOT, e.get("resultSuccessFoot"));
        putLangMapIfNonEmpty(out, KEY_RESULT_FAIL_MAIN, e.get("resultFailMain"));
        putLangMapIfNonEmpty(out, KEY_RESULT_FAIL_FOOT, e.get("resultFailFoot"));
        boolean showAmt = resolveAmountScaleNoticeShow(e);
        out.put("amountScaleNoticeShow", showAmt);
        Map<String, String> amtMap = langMap(e.get(KEY_AMOUNT_SCALE_NOTICE));
        if (!amtMap.isEmpty()) {
            out.put(KEY_AMOUNT_SCALE_NOTICE, amtMap);
        }
        return out;
    }

    /** 정확 일치 또는 행이 CHILLPAY 단독이고 운영이 ChillPay 계열인 경우(레거시 폴백). */
    private static boolean pgMatchesLoose(String rowPgUpper, String opPgUpper) {
        if (rowPgUpper.equals(opPgUpper)) {
            return true;
        }
        return PgVendor.CHILLPAY.equals(rowPgUpper) && ChillPayService.isChillPayFamilyPgCd(opPgUpper);
    }

    private static String text(JsonNode obj, String field) {
        if (obj == null || field == null) {
            return "";
        }
        JsonNode n = obj.get(field);
        return n != null && n.isTextual() ? n.asText("").trim() : "";
    }

    /**
     * JSON에 문자열 Y/N·불리언·미설정(기본 노출)을 허용합니다.
     * {@code text()}는 비텍스트 노드를 빈 문자열로 두어, 과거에 boolean으로 저장된 값이 무시되던 문제를 막습니다.
     */
    private static boolean resolveAmountScaleNoticeShow(JsonNode entry) {
        if (entry == null || !entry.isObject()) {
            return true;
        }
        JsonNode n = entry.get(KEY_AMOUNT_SCALE_NOTICE_SHOW_YN);
        if (n == null || n.isNull()) {
            return true;
        }
        if (n.isBoolean()) {
            return n.asBoolean();
        }
        if (n.isTextual()) {
            String t = n.asText("").trim();
            if (t.isEmpty()) {
                return true;
            }
            return !"N".equalsIgnoreCase(t);
        }
        return true;
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

    private static String safeFaviconUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.length() > 500 || s.contains("..") || s.contains("\n") || s.contains("\r")) {
            return "";
        }
        if (!s.startsWith(SAFE_FAVICON_PREFIX)) {
            return "";
        }
        String rest = s.substring(SAFE_FAVICON_PREFIX.length()).trim();
        if (rest.isEmpty() || rest.contains("/") || rest.contains("\\")) {
            return "";
        }
        return s;
    }

    private static void putLangMapIfNonEmpty(Map<String, Object> out, String key, JsonNode node) {
        Map<String, String> m = langMap(node);
        if (!m.isEmpty()) {
            out.put(key, m);
        }
    }

    /**
     * 본사 {@link HqApiConfig}의 URL 결제 폼 탭·파비콘을 우선하고, 비어 있으면 결제구문 JSON 행(레거시)을 폴백.
     */
    private void applyUrlPayChrome(Map<String, Object> out, HqApiConfig hq, JsonNode entryNode) {
        Map<String, String> tab = new LinkedHashMap<>();
        if (hq != null && hq.getUrlPayTabTitleJson() != null && !hq.getUrlPayTabTitleJson().isBlank()) {
            try {
                JsonNode n = OM.readTree(hq.getUrlPayTabTitleJson());
                tab.putAll(langMap(n));
            } catch (Exception ignored) {
                // ignore invalid JSON
            }
        }
        if (tab.isEmpty() && entryNode != null) {
            tab.putAll(langMap(entryNode.get("tabTitle")));
        }
        if (!tab.isEmpty()) {
            out.put(KEY_BROWSER_TAB_TITLE, tab);
        }
        String fav = "";
        if (hq != null && hq.getUrlPayFaviconUrl() != null && !hq.getUrlPayFaviconUrl().isBlank()) {
            fav = safeFaviconUrl(hq.getUrlPayFaviconUrl().trim());
        }
        if (fav.isEmpty() && entryNode != null) {
            fav = safeFaviconUrl(text(entryNode, "faviconUrl"));
        }
        if (!fav.isEmpty()) {
            out.put(KEY_FAVICON_URL, fav);
        }
    }
}
