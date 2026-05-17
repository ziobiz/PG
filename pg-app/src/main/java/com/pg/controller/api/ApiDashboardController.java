package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.DashboardHomeService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiDashboardController {

    private final DashboardHomeService dashboardHomeService;

    public ApiDashboardController(DashboardHomeService dashboardHomeService) {
        this.dashboardHomeService = dashboardHomeService;
    }

    /** 메인(/main) 카드·정산 달력·서버 요약 */
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<Map<String, Object>>> home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> body = dashboardHomeService.buildHome(auth);
        if (Boolean.FALSE.equals(body.get("ok"))) {
            return ResponseEntity.ok(ApiResponse.fail(
                    body.get("message") != null ? body.get("message").toString() : "오류", "UNAUTHORIZED"));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(ApiResponse.ok(body));
    }

    /**
     * 메인 영업일 3개월 달력 — anchorMonth(YYYY-MM) 기준 지난달·당월·다음달.
     * 이전/다음 이동은 응답의 prevAnchorMonth / nextAnchorMonth(±3개월) 사용.
     */
    @GetMapping("/businessDayCalendar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> businessDayCalendar(
            @RequestParam(name = "anchorMonth", required = false) String anchorMonth) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> body = dashboardHomeService.buildBusinessDayCalendar(auth, anchorMonth);
        if (Boolean.FALSE.equals(body.get("ok"))) {
            return ResponseEntity.ok(ApiResponse.fail(
                    body.get("message") != null ? body.get("message").toString() : "오류",
                    body.get("message") != null && String.valueOf(body.get("message")).contains("로그인")
                            ? "UNAUTHORIZED" : "FORBIDDEN"));
        }
        Object cal = body.get("businessDayCalendar");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(ApiResponse.ok(cal instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of()));
    }

    /** 메인 확장(insights·hqHub)만 — /home 본문이 잘리는 프록시·캐시 대비 */
    @GetMapping("/ext")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> body = dashboardHomeService.buildExtensionsOnly(auth);
        if (Boolean.FALSE.equals(body.get("ok"))) {
            return ResponseEntity.ok(ApiResponse.fail(
                    body.get("message") != null ? body.get("message").toString() : "오류", "UNAUTHORIZED"));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(ApiResponse.ok(body));
    }
}
