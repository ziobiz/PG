package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.NoticeDetailDto;
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

    @GetMapping("/notice/{id}")
    public ResponseEntity<ApiResponse<NoticeDetailDto>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.getDetail(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        }
    }

    @PostMapping("/notice")
    public ResponseEntity<ApiResponse<NoticeListDto>> create(@RequestBody(required = false) Map<String, Object> body) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        String title = body != null && body.get("title") != null ? String.valueOf(body.get("title")) : "";
        String content = body != null && body.get("content") != null ? String.valueOf(body.get("content")) : "";
        boolean showOnLogin = parseYn(body, "showOnLogin");
        boolean showAsPopup = parseYn(body, "showAsPopup");
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.create(u, title, content, showOnLogin, showAsPopup)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PutMapping("/notice/{id}")
    public ResponseEntity<ApiResponse<NoticeListDto>> update(@PathVariable Long id,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        String title = body != null && body.containsKey("title") && body.get("title") != null
                ? String.valueOf(body.get("title")) : null;
        String content = body != null && body.containsKey("content") && body.get("content") != null
                ? String.valueOf(body.get("content")) : null;
        Boolean showOnLogin = body != null && body.containsKey("showOnLogin") ? parseYnBox(body.get("showOnLogin")) : null;
        Boolean showAsPopup = body != null && body.containsKey("showAsPopup") ? parseYnBox(body.get("showAsPopup")) : null;
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.update(u, id, title, content, showOnLogin, showAsPopup)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @DeleteMapping("/notice/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            noticeService.delete(u, id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/notice/{id}/login-home")
    public ResponseEntity<ApiResponse<NoticeListDto>> pinLoginHome(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.setLoginHome(u, id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/notice/{id}/login-popup")
    public ResponseEntity<ApiResponse<NoticeListDto>> pinLoginPopup(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.setLoginPopup(u, id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return null;
        }
        return u;
    }

    private static boolean parseYn(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return false;
        }
        Boolean b = parseYnBox(body.get(key));
        return Boolean.TRUE.equals(b);
    }

    private static Boolean parseYnBox(Object so) {
        if (so == null) {
            return null;
        }
        if (so instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(so).trim();
        return "Y".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "1".equals(s);
    }
}
