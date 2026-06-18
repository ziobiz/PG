package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.AppUser;
import com.pg.entity.Notice;
import com.pg.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로그인 후 팝업·메인 대시보드 공지 — 배포 대상·조직 범위에 맞춰 노출.
 */
@Service
public class NoticeDisplayService {

    private final NoticeRepository noticeRepository;
    private final NoticeAudienceService noticeAudienceService;
    private final ObjectMapper objectMapper;

    public NoticeDisplayService(NoticeRepository noticeRepository,
                                NoticeAudienceService noticeAudienceService,
                                ObjectMapper objectMapper) {
        this.noticeRepository = noticeRepository;
        this.noticeAudienceService = noticeAudienceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolvePostLoginPopup(AppUser user, String acceptLanguage) {
        return resolveFirstVisible(user, noticeRepository.findByShowPostLoginPopupOrderByRegDtDescIdDesc("Y"),
                acceptLanguage);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolveMainNotice(AppUser user, String acceptLanguage) {
        return resolveFirstVisible(user, noticeRepository.findByShowOnMainOrderByRegDtDescIdDesc("Y"),
                acceptLanguage);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolveDetailForUser(AppUser user, Long id, String acceptLanguage) {
        if (user == null || id == null) {
            throw new IllegalArgumentException("공지를 찾을 수 없습니다.");
        }
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        if (!noticeAudienceService.isVisibleToUser(user, n)) {
            throw new IllegalArgumentException("공지를 찾을 수 없습니다.");
        }
        String lang = LoginNoticePublicService.pickLangBucket(acceptLanguage);
        return toDisplayMap(n, lang);
    }

    private Map<String, Object> resolveFirstVisible(AppUser user, List<Notice> candidates, String acceptLanguage) {
        if (user == null || candidates == null || candidates.isEmpty()) {
            return Map.of("hasNotice", false);
        }
        String lang = LoginNoticePublicService.pickLangBucket(acceptLanguage);
        for (Notice n : candidates) {
            if (noticeAudienceService.isVisibleToUser(user, n)) {
                return toDisplayMap(n, lang);
            }
        }
        return Map.of("hasNotice", false);
    }

    Map<String, Object> toDisplayMap(Notice n, String lang) {
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
        map.put("id", n.getId());
        map.put("title", title);
        map.put("body", body);
        map.put("content", body);
        if (n.getRegDt() != null) {
            map.put("regDate", n.getRegDt().toLocalDate().toString());
        }
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
}
