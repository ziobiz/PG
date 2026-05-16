package com.pg.service;

import com.pg.api.dto.NoticeDetailDto;
import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.Notice;
import com.pg.entity.OrgUnit;
import com.pg.repository.NoticeRepository;
import com.pg.repository.OrgUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final NoticeRepository noticeRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final OrgPagePermissionService orgPagePermissionService;
    private final NoticeLoginI18nService noticeLoginI18nService;

    public NoticeService(NoticeRepository noticeRepository,
                         OrgUnitRepository orgUnitRepository,
                         AuthService authService,
                         OrgPagePermissionService orgPagePermissionService,
                         NoticeLoginI18nService noticeLoginI18nService) {
        this.noticeRepository = noticeRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.noticeLoginI18nService = noticeLoginI18nService;
    }

    public PageResult<NoticeListDto> search(String title, LocalDate fromDt, LocalDate toDt, int page, int size) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(1000, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "regDt"));
        Page<Notice> result = noticeRepository.search(title, from, to, p);
        List<Notice> content = result.getContent();
        var idSet = content.stream().map(Notice::getOrgUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OrgUnit> ouMap = new HashMap<>();
        if (!idSet.isEmpty()) {
            orgUnitRepository.findAllById(idSet).forEach(ou -> ouMap.put(ou.getId(), ou));
        }
        return PageResult.of(result, n -> NoticeListDto.from(n, n.getOrgUnitId() != null ? ouMap.get(n.getOrgUnitId()) : null));
    }

    @Transactional(readOnly = true)
    public NoticeDetailDto getDetail(Long id) {
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeDetailDto.from(n, ou);
    }

    @Transactional
    public NoticeListDto create(AppUser user, String title, String content, boolean showOnLogin, boolean showAsPopup) {
        if (user == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        if (!orgPagePermissionService.canWriteNotice(user)) {
            throw new IllegalArgumentException("공지사항 등록 권한이 없습니다. (총본사·본사·총판이면서 해당 화면이 수정 이상으로 설정된 경우만 가능)");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력하세요.");
        }
        String body = content != null ? content : "";
        if (body.length() > 200_000) {
            throw new IllegalArgumentException("내용이 너무 깁니다.");
        }
        if (showOnLogin) {
            noticeRepository.clearAllShowOnLogin();
        }
        if (showAsPopup) {
            noticeRepository.clearAllShowAsPopup();
        }
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setContent(body);
        n.setHitCnt(0);
        n.setShowOnLogin(showOnLogin ? "Y" : "N");
        n.setShowAsPopup(showAsPopup ? "Y" : "N");
        n.setWriterNm(resolveWriterNm(user));
        authService.resolveOrgUnitForLoginId(user.getUsername()).ifPresent(ou -> n.setOrgUnitId(ou.getId()));
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, showOnLogin || showAsPopup);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou);
    }

    @Transactional
    public NoticeListDto update(AppUser user, Long id, String title, String content, Boolean showOnLogin, Boolean showAsPopup) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        if (title != null && !title.isBlank()) {
            n.setTitle(title.trim());
        }
        if (content != null) {
            if (content.length() > 200_000) {
                throw new IllegalArgumentException("내용이 너무 깁니다.");
            }
            n.setContent(content);
        }
        boolean wasLogin = "Y".equalsIgnoreCase(n.getShowOnLogin());
        boolean wasPopup = "Y".equalsIgnoreCase(n.getShowAsPopup());
        if (showOnLogin != null) {
            if (showOnLogin) {
                noticeRepository.clearAllShowOnLogin();
                n.setShowOnLogin("Y");
            } else {
                n.setShowOnLogin("N");
            }
        }
        if (showAsPopup != null) {
            if (showAsPopup) {
                noticeRepository.clearAllShowAsPopup();
                n.setShowAsPopup("Y");
            } else {
                n.setShowAsPopup("N");
            }
        }
        noticeRepository.save(n);
        boolean needI18n = "Y".equalsIgnoreCase(n.getShowOnLogin()) || "Y".equalsIgnoreCase(n.getShowAsPopup());
        if (needI18n && (wasLogin || wasPopup || Boolean.TRUE.equals(showOnLogin) || Boolean.TRUE.equals(showAsPopup)
                || title != null || content != null)) {
            refreshLoginI18nIfNeeded(n, true);
        } else if (!needI18n) {
            n.setLoginI18nJson(null);
            noticeRepository.save(n);
        }
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        requireWrite(user);
        if (!noticeRepository.existsById(id)) {
            throw new IllegalArgumentException("공지를 찾을 수 없습니다.");
        }
        noticeRepository.deleteById(id);
    }

    @Transactional
    public NoticeListDto setLoginHome(AppUser user, Long id) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        noticeRepository.clearAllShowOnLogin();
        n.setShowOnLogin("Y");
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou);
    }

    @Transactional
    public NoticeListDto setLoginPopup(AppUser user, Long id) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        noticeRepository.clearAllShowAsPopup();
        n.setShowAsPopup("Y");
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou);
    }

    private void requireWrite(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        if (!orgPagePermissionService.canWriteNotice(user)) {
            throw new IllegalArgumentException("공지사항 등록 권한이 없습니다.");
        }
    }

    private static String resolveWriterNm(AppUser user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        return user.getUsername();
    }

    private void refreshLoginI18nIfNeeded(Notice n, boolean enabled) {
        if (!enabled) {
            return;
        }
        try {
            String json = noticeLoginI18nService.buildLoginI18nJson(n.getTitle(), n.getContent() != null ? n.getContent() : "");
            n.setLoginI18nJson(json);
            noticeRepository.save(n);
        } catch (Exception e) {
            log.warn("login notice i18n persist skipped: {}", e.getMessage());
        }
    }
}
