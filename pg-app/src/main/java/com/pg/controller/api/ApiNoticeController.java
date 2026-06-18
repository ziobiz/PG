package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.NoticeDetailDto;
import com.pg.api.dto.NoticeListDto;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.service.NoticeDisplayService;
import com.pg.service.NoticeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/system", produces = "application/json")
public class ApiNoticeController {

    private final NoticeService noticeService;
    private final NoticeDisplayService noticeDisplayService;

    public ApiNoticeController(NoticeService noticeService, NoticeDisplayService noticeDisplayService) {
        this.noticeService = noticeService;
        this.noticeDisplayService = noticeDisplayService;
    }

    @GetMapping("/notice/deploy-targets")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> deployTargets() {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.deployTargetOptions(u)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/notice")
    public ResponseEntity<ApiResponse<PageResult<NoticeListDto>>> list(
            @RequestParam(required = false) String searchTitle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        PageResult<NoticeListDto> result = noticeService.search(u, searchTitle, searchFromDate, searchToDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/notice/{id}")
    public ResponseEntity<ApiResponse<NoticeDetailDto>> get(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.getDetail(u, id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        }
    }

    @GetMapping("/notice/{id}/display")
    public ResponseEntity<ApiResponse<Map<String, Object>>> display(
            @PathVariable Long id,
            @RequestHeader(value = org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeDisplayService.resolveDetailForUser(u, id, acceptLanguage)));
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
        boolean showPostLoginPopup = parseYn(body, "showPostLoginPopup");
        boolean showOnMain = parseYn(body, "showOnMain");
        String deployTarget = body != null && body.get("deployTarget") != null ? String.valueOf(body.get("deployTarget")) : null;
        List<Long> targetOrgUnitIds = parseLongList(body != null ? body.get("targetOrgUnitIds") : null);
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.create(u, title, content, showOnLogin, showAsPopup,
                    showPostLoginPopup, showOnMain, deployTarget, targetOrgUnitIds)));
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
        Boolean showPostLoginPopup = body != null && body.containsKey("showPostLoginPopup") ? parseYnBox(body.get("showPostLoginPopup")) : null;
        Boolean showOnMain = body != null && body.containsKey("showOnMain") ? parseYnBox(body.get("showOnMain")) : null;
        String deployTarget = body != null && body.containsKey("deployTarget") && body.get("deployTarget") != null
                ? String.valueOf(body.get("deployTarget")) : null;
        List<Long> targetOrgUnitIds = body != null && body.containsKey("targetOrgUnitIds")
                ? parseLongList(body.get("targetOrgUnitIds")) : null;
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.update(u, id, title, content, showOnLogin, showAsPopup,
                    showPostLoginPopup, showOnMain, deployTarget, targetOrgUnitIds)));
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

    @PostMapping("/notice/{id}/post-login-popup")
    public ResponseEntity<ApiResponse<NoticeListDto>> pinPostLoginPopup(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.setPostLoginPopup(u, id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/notice/{id}/main-notice")
    public ResponseEntity<ApiResponse<NoticeListDto>> pinMainNotice(@PathVariable Long id) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(noticeService.setMainNotice(u, id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/notice/display/post-login-popup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> postLoginPopupDisplay(
            @RequestHeader(value = org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        AppUser u = currentUser();
        if (u == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTH"));
        }
        return ResponseEntity.ok(ApiResponse.ok(noticeDisplayService.resolvePostLoginPopup(u, acceptLanguage)));
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

    @SuppressWarnings("unchecked")
    private static List<Long> parseLongList(Object raw) {
        if (raw == null) {
            return null;
        }
        List<Long> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                Long v = toLong(o);
                if (v != null) {
                    out.add(v);
                }
            }
            return out;
        }
        Long one = toLong(raw);
        if (one != null) {
            out.add(one);
        }
        return out;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
