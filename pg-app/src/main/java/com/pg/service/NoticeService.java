package com.pg.service;

import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.Notice;
import com.pg.entity.OrgUnit;
import com.pg.repository.NoticeRepository;
import com.pg.repository.OrgUnitRepository;
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

    private final NoticeRepository noticeRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final OrgPagePermissionService orgPagePermissionService;

    public NoticeService(NoticeRepository noticeRepository,
                         OrgUnitRepository orgUnitRepository,
                         AuthService authService,
                         OrgPagePermissionService orgPagePermissionService) {
        this.noticeRepository = noticeRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
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

    @Transactional
    public NoticeListDto create(AppUser user, String title, String content) {
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
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setContent(body);
        n.setHitCnt(0);
        authService.resolveOrgUnitForLoginId(user.getUsername()).ifPresent(ou -> n.setOrgUnitId(ou.getId()));
        noticeRepository.save(n);
        OrgUnit ou = n.getOrgUnitId() != null ? orgUnitRepository.findById(n.getOrgUnitId()).orElse(null) : null;
        return NoticeListDto.from(n, ou);
    }
}
