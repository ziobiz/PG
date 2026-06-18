package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.Notice;
import com.pg.entity.OrgLevel;
import com.pg.repository.NoticeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 비로그인 로그인 페이지 — {@code Accept-Language} 에 맞춰 공지 제목·본문 한 벌 반환.
 * 접속팝업·첫화면은 <b>총본사(HEADQUARTERS) 작성</b> 공지만 노출합니다.
 */
@Service
public class LoginNoticePublicService {

    private final NoticeRepository noticeRepository;
    private final ObjectMapper objectMapper;

    public LoginNoticePublicService(NoticeRepository noticeRepository, ObjectMapper objectMapper) {
        this.noticeRepository = noticeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolveForAcceptLanguage(String acceptLanguage) {
        String lang = pickLangBucket(acceptLanguage);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("home", resolveHeadquartersLoginSite(
                noticeRepository.findLoginSiteHomeByWriterLevelOrderByRegDtDescIdDesc(
                        "Y", OrgLevel.HEADQUARTERS, PageRequest.of(0, 1)),
                lang));
        map.put("popup", resolveHeadquartersLoginSite(
                noticeRepository.findLoginSitePopupByWriterLevelOrderByRegDtDescIdDesc(
                        "Y", OrgLevel.HEADQUARTERS, PageRequest.of(0, 1)),
                lang));
        map.put("resolvedLang", lang);
        return map;
    }

    private Map<String, Object> resolveHeadquartersLoginSite(List<Notice> candidates, String lang) {
        if (candidates == null || candidates.isEmpty()) {
            return Map.of("hasNotice", false);
        }
        return resolvePinned(candidates.get(0), lang);
    }

    private Map<String, Object> resolvePinned(Notice n, String lang) {
        if (n == null) {
            return Map.of("hasNotice", false);
        }
        String title = n.getTitle();
        String body = n.getContent() != null ? n.getContent() : "";
        String json = n.getLoginI18nJson();
        if (json != null && !json.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode titles = root.get("titles");
                JsonNode bodies = root.get("bodies");
                if (titles != null && bodies != null) {
                    String pickTitle = textAt(titles, lang);
                    String pickBody = textAt(bodies, lang);
                    if (pickTitle != null) {
                        title = pickTitle;
                    }
                    if (pickBody != null) {
                        body = pickBody;
                    }
                }
            } catch (Exception ignored) {
                // 원문 유지
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hasNotice", true);
        map.put("title", title);
        map.put("body", body);
        return map;
    }

    private static String textAt(JsonNode group, String lang) {
        if (group == null || !group.isObject()) {
            return null;
        }
        JsonNode direct = group.get(lang);
        if (direct != null && direct.isTextual()) {
            return direct.asText();
        }
        if ("CH".equals(lang) && group.has("ZH") && group.get("ZH").isTextual()) {
            return group.get("ZH").asText();
        }
        return null;
    }

    public static String pickLangBucket(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "KO";
        }
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
            for (Locale.LanguageRange r : ranges) {
                String lang = r.getRange().toLowerCase(Locale.ROOT);
                if (lang.startsWith("ko")) {
                    return "KO";
                }
                if (lang.startsWith("en")) {
                    return "EN";
                }
                if (lang.startsWith("ja")) {
                    return "JP";
                }
                if (lang.startsWith("zh-hant") || lang.startsWith("zh-tw") || lang.startsWith("zh-hk")) {
                    return "CH";
                }
                if (lang.startsWith("zh")) {
                    return "CH";
                }
                if (lang.startsWith("th")) {
                    return "TH";
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "KO";
    }
}
