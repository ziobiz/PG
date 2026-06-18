package com.pg.service;

import com.pg.api.dto.NoticeDetailDto;
import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.Notice;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.notice.NoticeDeployTarget;
import com.pg.repository.NoticeRepository;
import com.pg.repository.OrgUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final NoticeAudienceService noticeAudienceService;

    public NoticeService(NoticeRepository noticeRepository,
                         OrgUnitRepository orgUnitRepository,
                         AuthService authService,
                         OrgPagePermissionService orgPagePermissionService,
                         NoticeLoginI18nService noticeLoginI18nService,
                         NoticeAudienceService noticeAudienceService) {
        this.noticeRepository = noticeRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.noticeLoginI18nService = noticeLoginI18nService;
        this.noticeAudienceService = noticeAudienceService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> deployTargetOptions(AppUser user) {
        requireWrite(user);
        OrgUnit writer = noticeAudienceService.requireWriterOrg(user);
        OrgLevel level = writer.getOrgLevel();
        return noticeAudienceService.deployTargetOptions(level);
    }

    @Transactional(readOnly = true)
    public PageResult<NoticeListDto> search(AppUser user, String title, LocalDate fromDt, LocalDate toDt, int page, int size) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(1000, Math.max(1, size));

        List<Notice> all = noticeRepository.searchList(title, null, null);
        List<Notice> allVisible = all.stream()
                .filter(n -> noticeAudienceService.isVisibleToUser(user, n))
                .collect(Collectors.toList());

        Notice latest = allVisible.isEmpty() ? null : allVisible.get(0);
        Long latestId = latest != null ? latest.getId() : null;

        List<Notice> inRange = allVisible.stream()
                .filter(n -> matchesRegDateRange(n, from, to))
                .collect(Collectors.toList());

        List<Notice> merged = new ArrayList<>();
        if (latest != null) {
            merged.add(latest);
        }
        for (Notice n : inRange) {
            if (latestId == null || !Objects.equals(n.getId(), latestId)) {
                merged.add(n);
            }
        }

        int total = merged.size();
        int fromIdx = (pageNo - 1) * pageSize;
        int toIdx = Math.min(fromIdx + pageSize, total);
        List<Notice> pageContent = fromIdx >= total ? List.of() : merged.subList(fromIdx, toIdx);

        var idSet = pageContent.stream().map(Notice::getOrgUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OrgUnit> ouMap = new HashMap<>();
        if (!idSet.isEmpty()) {
            orgUnitRepository.findAllById(idSet).forEach(ou -> ouMap.put(ou.getId(), ou));
        }

        List<NoticeListDto> list = pageContent.stream()
                .map(n -> NoticeListDto.from(
                        n,
                        n.getOrgUnitId() != null ? ouMap.get(n.getOrgUnitId()) : null,
                        noticeAudienceService.labelForTarget(n.getDeployTarget()),
                        latestId != null && Objects.equals(n.getId(), latestId)))
                .collect(Collectors.toList());

        PageResult<NoticeListDto> result = new PageResult<>();
        result.setList(list);
        result.setTotalElements(total);
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setTotalPages(pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        return result;
    }

    private static boolean matchesRegDateRange(Notice n, LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return true;
        }
        LocalDateTime reg = n.getRegDt();
        if (reg == null) {
            return true;
        }
        if (from != null && reg.isBefore(from)) {
            return false;
        }
        if (to != null && reg.isAfter(to)) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public NoticeDetailDto getDetail(AppUser user, Long id) {
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        if (!noticeAudienceService.isVisibleToUser(user, n)) {
            throw new IllegalArgumentException("공지를 찾을 수 없습니다.");
        }
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeDetailDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()), loadTargetOrgUnits(n));
    }

    @Transactional
    public NoticeListDto create(AppUser user, String title, String content, boolean showOnLogin, boolean showAsPopup,
                                boolean showPostLoginPopup, boolean showOnMain,
                                String deployTargetCode, List<Long> targetOrgUnitIds) {
        if (user == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        requireWrite(user);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력하세요.");
        }
        String body = content != null ? content : "";
        if (body.length() > 200_000) {
            throw new IllegalArgumentException("내용이 너무 깁니다.");
        }

        OrgUnit writer = noticeAudienceService.requireWriterOrg(user);
        OrgLevel writerLevel = writer.getOrgLevel();
        NoticeDeployTarget deployTarget = resolveDeployTargetForSave(writerLevel, deployTargetCode);
        noticeAudienceService.validateDeploy(user, deployTarget, targetOrgUnitIds);
        requireHeadquartersForLoginSiteFlags(user, showOnLogin, showAsPopup);
        if (!isHeadquartersWriter(user)) {
            showOnLogin = false;
            showAsPopup = false;
        }

        if (showOnLogin) {
            noticeRepository.clearAllShowOnLogin();
        }
        if (showAsPopup) {
            noticeRepository.clearAllShowAsPopup();
        }
        if (showPostLoginPopup) {
            noticeRepository.clearAllShowPostLoginPopup();
        }
        if (showOnMain) {
            noticeRepository.clearAllShowOnMain();
        }
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setContent(body);
        n.setHitCnt(0);
        n.setShowOnLogin(showOnLogin ? "Y" : "N");
        n.setShowAsPopup(showAsPopup ? "Y" : "N");
        n.setShowPostLoginPopup(showPostLoginPopup ? "Y" : "N");
        n.setShowOnMain(showOnMain ? "Y" : "N");
        n.setWriterNm(resolveWriterNm(user));
        n.setOrgUnitId(writer.getId());
        n.setDeployTarget(deployTarget.getCode());
        n.setTargetOrgUnitIdsJson(deployTarget == NoticeDeployTarget.NOTI
                ? noticeAudienceService.serializeTargetOrgUnitIds(targetOrgUnitIds)
                : null);
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, needsDisplayI18n(n));
        return NoticeListDto.from(n, writer, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    @Transactional
    public NoticeListDto update(AppUser user, Long id, String title, String content, Boolean showOnLogin, Boolean showAsPopup,
                                Boolean showPostLoginPopup, Boolean showOnMain,
                                String deployTargetCode, List<Long> targetOrgUnitIds) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        if (title != null && !title.isBlank()) {
            n.setTitle(title.trim());
        }
        if (content != null) {
            if (content.length() > 200_000) {
                throw new IllegalArgumentException("내용이 너무 깁니다.");
            }
            n.setContent(content);
        }
        if (deployTargetCode != null && !deployTargetCode.isBlank()) {
            OrgUnit writer = noticeAudienceService.resolveWriterOrg(n);
            OrgLevel writerLevel = writer != null ? writer.getOrgLevel() : null;
            NoticeDeployTarget deployTarget = resolveDeployTargetForSave(writerLevel, deployTargetCode);
            List<Long> notiIds = targetOrgUnitIds;
            if (deployTarget != NoticeDeployTarget.NOTI) {
                notiIds = List.of();
            } else if (notiIds == null) {
                notiIds = noticeAudienceService.parseTargetOrgUnitIds(n.getTargetOrgUnitIdsJson());
            }
            noticeAudienceService.validateDeploy(user, deployTarget, notiIds);
            n.setDeployTarget(deployTarget.getCode());
            n.setTargetOrgUnitIdsJson(deployTarget == NoticeDeployTarget.NOTI
                    ? noticeAudienceService.serializeTargetOrgUnitIds(notiIds)
                    : null);
        } else if (targetOrgUnitIds != null && NoticeDeployTarget.NOTI == NoticeDeployTarget.fromCode(n.getDeployTarget())) {
            noticeAudienceService.validateDeploy(user, NoticeDeployTarget.NOTI, targetOrgUnitIds);
            n.setTargetOrgUnitIdsJson(noticeAudienceService.serializeTargetOrgUnitIds(targetOrgUnitIds));
        }

        boolean wasLogin = "Y".equalsIgnoreCase(n.getShowOnLogin());
        boolean wasPopup = "Y".equalsIgnoreCase(n.getShowAsPopup());
        boolean wasPostLogin = "Y".equalsIgnoreCase(n.getShowPostLoginPopup());
        boolean wasMain = "Y".equalsIgnoreCase(n.getShowOnMain());
        if (showOnLogin != null || showAsPopup != null) {
            requireHeadquartersForLoginSiteFlags(user,
                    Boolean.TRUE.equals(showOnLogin),
                    Boolean.TRUE.equals(showAsPopup));
        }
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
        if (showPostLoginPopup != null) {
            if (showPostLoginPopup) {
                noticeRepository.clearAllShowPostLoginPopup();
                n.setShowPostLoginPopup("Y");
            } else {
                n.setShowPostLoginPopup("N");
            }
        }
        if (showOnMain != null) {
            if (showOnMain) {
                noticeRepository.clearAllShowOnMain();
                n.setShowOnMain("Y");
            } else {
                n.setShowOnMain("N");
            }
        }
        if (!isHeadquartersWriter(user)) {
            n.setShowOnLogin("N");
            n.setShowAsPopup("N");
        }
        noticeRepository.save(n);
        boolean needI18n = needsDisplayI18n(n);
        if (needI18n && (wasLogin || wasPopup || wasPostLogin || wasMain
                || Boolean.TRUE.equals(showOnLogin) || Boolean.TRUE.equals(showAsPopup)
                || Boolean.TRUE.equals(showPostLoginPopup) || Boolean.TRUE.equals(showOnMain)
                || title != null || content != null)) {
            refreshLoginI18nIfNeeded(n, true);
        } else if (!needI18n) {
            n.setLoginI18nJson(null);
            noticeRepository.save(n);
        }
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        noticeRepository.deleteById(id);
    }

    @Transactional
    public NoticeListDto setLoginHome(AppUser user, Long id) {
        requireWrite(user);
        requireHeadquartersWriter(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        noticeRepository.clearAllShowOnLogin();
        n.setShowOnLogin("Y");
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    @Transactional
    public NoticeListDto setLoginPopup(AppUser user, Long id) {
        requireWrite(user);
        requireHeadquartersWriter(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        noticeRepository.clearAllShowAsPopup();
        n.setShowAsPopup("Y");
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    @Transactional
    public NoticeListDto setPostLoginPopup(AppUser user, Long id) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        noticeRepository.clearAllShowPostLoginPopup();
        n.setShowPostLoginPopup("Y");
        if (!isHeadquartersWriter(user)) {
            n.setShowOnLogin("N");
            n.setShowAsPopup("N");
        }
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    @Transactional
    public NoticeListDto setMainNotice(AppUser user, Long id) {
        requireWrite(user);
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        requireSameWriterOrg(user, n);
        noticeRepository.clearAllShowOnMain();
        n.setShowOnMain("Y");
        if (!isHeadquartersWriter(user)) {
            n.setShowOnLogin("N");
            n.setShowAsPopup("N");
        }
        noticeRepository.save(n);
        refreshLoginI18nIfNeeded(n, true);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou, noticeAudienceService.labelForTarget(n.getDeployTarget()));
    }

    private NoticeDeployTarget resolveDeployTargetForSave(OrgLevel writerLevel, String deployTargetCode) {
        if (deployTargetCode == null || deployTargetCode.isBlank()) {
            NoticeDeployTarget def = noticeAudienceService.defaultTargetForWriter(writerLevel);
            if (def == null) {
                throw new IllegalArgumentException("배포 대상을 선택하세요.");
            }
            return def;
        }
        return NoticeDeployTarget.fromCode(deployTargetCode);
    }

    private List<Map<String, Object>> loadTargetOrgUnits(Notice n) {
        List<Long> ids = noticeAudienceService.parseTargetOrgUnitIds(n.getTargetOrgUnitIdsJson());
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findAllById(ids)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", ou.getId());
            row.put("compId", ou.getCode());
            row.put("compNm", ou.getName());
            row.put("compDiv", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
            out.add(row);
        }
        return out;
    }

    private void requireSameWriterOrg(AppUser user, Notice n) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        OrgUnit viewer = noticeAudienceService.requireWriterOrg(user);
        if (n.getOrgUnitId() == null || !Objects.equals(n.getOrgUnitId(), viewer.getId())) {
            throw new IllegalArgumentException("본인 조직이 작성한 공지만 수정·삭제할 수 있습니다.");
        }
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

    private static boolean needsDisplayI18n(Notice n) {
        return "Y".equalsIgnoreCase(n.getShowOnLogin())
                || "Y".equalsIgnoreCase(n.getShowAsPopup())
                || "Y".equalsIgnoreCase(n.getShowPostLoginPopup())
                || "Y".equalsIgnoreCase(n.getShowOnMain());
    }

    private void requireHeadquartersForLoginSiteFlags(AppUser user, boolean showOnLogin, boolean showAsPopup) {
        if (!showOnLogin && !showAsPopup) {
            return;
        }
        requireHeadquartersWriter(user);
    }

    private void requireHeadquartersWriter(AppUser user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        OrgUnit writer = noticeAudienceService.requireWriterOrg(user);
        if (writer.getOrgLevel() != OrgLevel.HEADQUARTERS) {
            throw new IllegalArgumentException("접속팝업·첫화면은 총본사만 사용할 수 있습니다.");
        }
    }

    private boolean isHeadquartersWriter(AppUser user) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        OrgUnit writer = noticeAudienceService.requireWriterOrg(user);
        return writer.getOrgLevel() == OrgLevel.HEADQUARTERS;
    }
}
