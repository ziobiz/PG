package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.service.NoticeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/api/system", produces = "application/json")
public class ApiNoticeController {

    private final NoticeService noticeService;

    public ApiNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notice")
    public ResponseEntity<ApiResponse<PageResult<NoticeListDto>>> list(
            @RequestParam(required = false) String searchTitle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<NoticeListDto> result = noticeService.search(searchTitle, searchFromDate, searchToDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
