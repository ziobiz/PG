package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.HqNotifyInboundQueryService;
import com.pg.service.PgNotifyReceiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 본사설정 — 노티 수신 로그(노티서버→PG 수신 원문) 조회
 */
@RestController
@RequestMapping(value = "/api/hq/notifyInbound", produces = "application/json")
public class ApiHqNotifyInboundController {

    private final HqNotifyInboundQueryService hqNotifyInboundQueryService;
    private final PgNotifyReceiveService pgNotifyReceiveService;

    public ApiHqNotifyInboundController(HqNotifyInboundQueryService hqNotifyInboundQueryService,
                                        PgNotifyReceiveService pgNotifyReceiveService) {
        this.hqNotifyInboundQueryService = hqNotifyInboundQueryService;
        this.pgNotifyReceiveService = pgNotifyReceiveService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        LocalDate from = parseLocalDate(fromDate);
        LocalDate to = parseLocalDate(toDate);
        PageResult<Map<String, Object>> pr = hqNotifyInboundQueryService.search(
                page, size, searchKey, searchValue, from, to);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable long id) {
        return hqNotifyInboundQueryService.findDetail(id)
                .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("not found", "NOT_FOUND")));
    }

    /**
     * 수신 로그를 원문 기준으로 다시 파싱·가맹 분기한 뒤 결제내역(pg_trnsctn) 적재 파이프라인을 실행합니다.
     * 바인딩·노티대상·총판 통화 등을 수정한 뒤 과거 건을 반영할 때 사용합니다.
     */
    @PostMapping("/{id}/replay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replay(@PathVariable long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(pgNotifyReceiveService.replayInboundProcessing(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "BAD_REQUEST"));
        }
    }

    private static LocalDate parseLocalDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
