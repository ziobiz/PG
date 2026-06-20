package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.HqNotifyInboundQueryService;
import com.pg.service.PgNotifyReceiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
     * 노티 수신 본문({@code raw_body}) 수정. {@code icopayCompId} 추가 등 수동 보정용.
     */
    @PutMapping("/{id}/rawBody")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRawBody(
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {
        try {
            String rawBody = body != null && body.get("rawBody") != null
                    ? String.valueOf(body.get("rawBody")) : "";
            return ResponseEntity.ok(ApiResponse.ok(pgNotifyReceiveService.updateInboundRawBody(id, rawBody)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "BAD_REQUEST"));
        }
    }

    /**
     * 수신 로그를 원문 기준으로 다시 파싱·가맹 분기한 뒤 결제내역(pg_trnsctn) 적재 파이프라인을 실행합니다.
     * 바인딩·노티대상·총판 통화 등을 수정한 뒤 과거 건을 반영할 때 사용합니다.
     * 요청 본문에 {@code rawBody} 가 있으면 저장 후 재처리합니다.
     */
    @PostMapping("/{id}/replay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replay(
            @PathVariable long id,
            @RequestParam(required = false) String icopayCompId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String comp = icopayCompId;
            String rawOverride = null;
            String customerNm = null;
            String customerEmail = null;
            String cardPanDisplay = null;
            if (body != null) {
                if ((comp == null || comp.isBlank()) && body.get("icopayCompId") != null) {
                    comp = String.valueOf(body.get("icopayCompId")).trim();
                }
                if (body.get("rawBody") != null) {
                    rawOverride = String.valueOf(body.get("rawBody"));
                }
                if (body.get("customerNm") != null) {
                    customerNm = String.valueOf(body.get("customerNm")).trim();
                }
                if (body.get("customerEmail") != null) {
                    customerEmail = String.valueOf(body.get("customerEmail")).trim();
                }
                if (body.get("cardPanDisplay") != null) {
                    cardPanDisplay = String.valueOf(body.get("cardPanDisplay")).trim();
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(
                    pgNotifyReceiveService.replayInboundProcessing(
                            id, comp, rawOverride, customerNm, customerEmail, cardPanDisplay)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "BAD_REQUEST"));
        }
    }

    /**
     * 지정 일자·주문번호별 노티수령 원문을 {@code icopayCompId} 와 함께 재반영해 결제내역을 복구합니다.
     */
    @PostMapping("/replay-orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replayOrders(@RequestBody Map<String, Object> body) {
        try {
            String compId = body != null && body.get("icopayCompId") != null
                    ? String.valueOf(body.get("icopayCompId")).trim() : "";
            String dateStr = body != null && body.get("date") != null
                    ? String.valueOf(body.get("date")).trim() : "";
            LocalDate date = dateStr.isBlank() ? null : LocalDate.parse(dateStr);
            List<String> orderNos = new ArrayList<>();
            if (body != null && body.get("orderNos") instanceof List<?> raw) {
                for (Object o : raw) {
                    if (o != null) {
                        String s = String.valueOf(o).trim();
                        if (!s.isEmpty()) {
                            orderNos.add(s);
                        }
                    }
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(
                    pgNotifyReceiveService.replayOrdersWithCompId(compId, date, orderNos)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "replay failed", "ERROR"));
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
