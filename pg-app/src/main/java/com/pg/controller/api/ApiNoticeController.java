package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.service.NoticeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

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

    @PostMapping("/notice")
    public ResponseEntity<ApiResponse<NoticeListDto>> create(@RequestBody(required = false) Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        String title = body != null && body.get("title") != null ? String.valueOf(body.get("title")) : "";
        String content = body != null && body.get("content") != null ? String.valueOf(body.get("content")) : "";
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.create(u, title, content)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
