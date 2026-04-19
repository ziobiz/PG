package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 결제구문설정용: 한국어 제목·내용1~3을 MyMemory 무료 API로 ENG·CHN·JPN·THA 초안 번역(본사 API에서 프록시 — 브라우저 CORS 회피).
 * <p>
 * MyMemory는 {@code langpair}를 2자 ISO(소문자 권장) 또는 RFC3066(예: zh-CN) 형태로 기대합니다.
 * {@code ko|…} 조합이 거절되는 환경에서는 {@code ko-KR|…} 또는 영어 경유({@code ko|en} → {@code en|ja})로 폴백합니다.
 * 응답 본문에 {@code INVALID LANGUAGE PAIR} 가 포함되면 번역 실패로 간주하고 다음 후보를 시도합니다.
 */
@Service
public class HqPayCopyTranslationService {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final String MM_BASE = "https://api.mymemory.translated.net/get";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> translatePayCopyFromKo(String titleKo, String body1Ko, String body2Ko, String body3Ko,
                                                        String resultOk1Ko, String resultOk2Ko, String resultFail1Ko, String resultFail2Ko,
                                                        String amountScaleNoticeKo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", buildLangMap(titleKo));
        out.put("body1", buildLangMap(body1Ko));
        out.put("body2", buildLangMap(body2Ko));
        out.put("body3", buildLangMap(body3Ko));
        out.put("resultSuccessMain", buildLangMap(resultOk1Ko != null ? resultOk1Ko : ""));
        out.put("resultSuccessFoot", buildLangMap(resultOk2Ko != null ? resultOk2Ko : ""));
        out.put("resultFailMain", buildLangMap(resultFail1Ko != null ? resultFail1Ko : ""));
        out.put("resultFailFoot", buildLangMap(resultFail2Ko != null ? resultFail2Ko : ""));
        out.put("amountScaleNotice", buildLangMap(amountScaleNoticeKo != null ? amountScaleNoticeKo : ""));
        return out;
    }

    /** URL 결제 폼 설정 — 브라우저 탭 제목 한 줄 다국어 맵 */
    public Map<String, Object> translateUrlPayTabTitleFromKo(String tabTitleKo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tabTitle", buildLangMap(tabTitleKo != null ? tabTitleKo : ""));
        return out;
    }

    private Map<String, String> buildLangMap(String koRaw) {
        Map<String, String> m = new LinkedHashMap<>();
        String ko = koRaw != null ? koRaw.trim() : "";
        if (!ko.isEmpty()) {
            m.put("KOR", ko);
        }
        if (ko.isEmpty()) {
            return m;
        }

        pause();
        String enPivot = translateKoToEn(ko);
        putIfGood(m, "ENG", enPivot);

        pause();
        putIfGood(m, "CHN", translateKoToZh(ko, enPivot));

        pause();
        putIfGood(m, "JPN", translateKoToJa(ko, enPivot));

        pause();
        putIfGood(m, "THA", translateKoToTh(ko, enPivot));

        return m;
    }

    private static void putIfGood(Map<String, String> m, String key, String val) {
        if (val != null && !val.isBlank() && !isMyMemoryErrorPayload(val)) {
            m.put(key, val.trim());
        }
    }

    /** 한→영: MyMemory가 허용하는 langpair 후보만 순차 시도 */
    private String translateKoToEn(String ko) {
        List<String> direct = List.of(
                "ko|en",
                "ko-KR|en",
                "ko|en-US",
                "KO|EN"
        );
        for (String pair : direct) {
            String r = myMemoryTranslate(ko, pair);
            if (acceptableTranslation(r)) {
                return r.trim();
            }
            pause();
        }
        return "";
    }

    private String translateKoToZh(String ko, String enPivot) {
        List<String> direct = List.of(
                "ko|zh-CN",
                "ko-KR|zh-CN",
                "ko|zh",
                "ko|zh-Hans",
                "KO|ZH-CN"
        );
        for (String pair : direct) {
            String r = myMemoryTranslate(ko, pair);
            if (acceptableTranslation(r)) {
                return r.trim();
            }
            pause();
        }
        if (enPivot != null && !enPivot.isBlank() && !isMyMemoryErrorPayload(enPivot)) {
            List<String> viaEn = List.of("en|zh-CN", "en|zh", "en|zh-Hans");
            for (String pair : viaEn) {
                String r = myMemoryTranslate(enPivot.trim(), pair);
                if (acceptableTranslation(r)) {
                    return r.trim();
                }
                pause();
            }
        }
        return "";
    }

    private String translateKoToJa(String ko, String enPivot) {
        List<String> direct = List.of(
                "ko|ja",
                "ko-KR|ja",
                "ko|ja-JP",
                "KO|JA"
        );
        for (String pair : direct) {
            String r = myMemoryTranslate(ko, pair);
            if (acceptableTranslation(r)) {
                return r.trim();
            }
            pause();
        }
        if (enPivot != null && !enPivot.isBlank() && !isMyMemoryErrorPayload(enPivot)) {
            List<String> viaEn = List.of("en|ja", "en|ja-JP");
            for (String pair : viaEn) {
                String r = myMemoryTranslate(enPivot.trim(), pair);
                if (acceptableTranslation(r)) {
                    return r.trim();
                }
                pause();
            }
        }
        return "";
    }

    private String translateKoToTh(String ko, String enPivot) {
        List<String> direct = List.of(
                "ko|th",
                "ko-KR|th",
                "KO|TH"
        );
        for (String pair : direct) {
            String r = myMemoryTranslate(ko, pair);
            if (acceptableTranslation(r)) {
                return r.trim();
            }
            pause();
        }
        if (enPivot != null && !enPivot.isBlank() && !isMyMemoryErrorPayload(enPivot)) {
            List<String> viaEn = List.of("en|th");
            for (String pair : viaEn) {
                String r = myMemoryTranslate(enPivot.trim(), pair);
                if (acceptableTranslation(r)) {
                    return r.trim();
                }
                pause();
            }
        }
        return "";
    }

    private static boolean acceptableTranslation(String r) {
        return r != null && !r.isBlank() && !isMyMemoryErrorPayload(r);
    }

    private static boolean isMyMemoryErrorPayload(String text) {
        String u = text.toUpperCase(Locale.ROOT);
        return u.contains("INVALID LANGUAGE PAIR")
                || u.contains("QUERY TOO LONG")
                || u.contains("MYMEMORY: ");
    }

    private static void pause() {
        try {
            Thread.sleep(420);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String myMemoryTranslate(String text, String langpair) {
        if (text == null || text.isBlank() || langpair == null || langpair.isBlank()) {
            return "";
        }
        String q = text.length() > 450 ? text.substring(0, 450) : text;
        try {
            URI uri = UriComponentsBuilder.fromUriString(MM_BASE)
                    .queryParam("q", q)
                    .queryParam("langpair", langpair)
                    .queryParam("mt", "1")
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "ICOPAY-PG/1.0 (pay-copy-translate)");
            headers.set(HttpHeaders.ACCEPT, "application/json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            String json = res.getBody();
            if (json == null || json.isBlank()) {
                return "";
            }
            JsonNode root = OM.readTree(json);
            String out = root.path("responseData").path("translatedText").asText("");
            return out;
        } catch (Exception e) {
            return "";
        }
    }
}
