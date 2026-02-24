package com.pg.service;

import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.Notice;
import com.pg.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public PageResult<NoticeListDto> search(String title, LocalDate fromDt, LocalDate toDt, int page, int size) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "regDt"));
        Page<Notice> result = noticeRepository.search(title, from, to, p);
        return PageResult.of(result, NoticeListDto::from);
    }
}
