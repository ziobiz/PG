package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.PgNotiOrderLookupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * NOTI 전 PG 공통 주문→Comp-Id 조회.
 * <ul>
 *   <li>{@code GET /api/middleware/notify/v1/order-lookup?order={order}}</li>
 *   <li>{@code GET /api/pay/order-lookup?order={order}} (동일)</li>
 * </ul>
 * 응답 헤더 {@code X-Icopay-Comp-Id} · JSON {@code compId}/{@code merchantName}.
 */
@RestController
public class ApiPgNotiOrderLookupController {

    private final PgNotiOrderLookupService pgNotiOrderLookupService;

    public ApiPgNotiOrderLookupController(PgNotiOrderLookupService pgNotiOrderLookupService) {
        this.pgNotiOrderLookupService = pgNotiOrderLookupService;
    }

    @GetMapping(value = {
            "/api/middleware/notify/v1/order-lookup",
            "/api/pay/order-lookup"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderLookup(
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String orderNo) {
        String q = order != null && !order.isBlank() ? order.trim()
                : (orderNo != null ? orderNo.trim() : "");
        Map<String, Object> m = pgNotiOrderLookupService.lookup(q);
        HttpHeaders headers = new HttpHeaders();
        String compId = m.get("compId") != null ? String.valueOf(m.get("compId")).trim() : "";
        String on = m.get("orderNo") != null ? String.valueOf(m.get("orderNo")).trim() : "";
        if (!compId.isBlank()) {
            headers.add("X-Icopay-Comp-Id", compId);
        }
        if (!on.isBlank()) {
            headers.add("X-Icopay-Order-No", on);
        }
        if (!Boolean.TRUE.equals(m.get("success"))) {
            String code = m.get("errorCode") != null ? String.valueOf(m.get("errorCode")) : "NOT_FOUND";
            String msg = m.get("message") != null ? String.valueOf(m.get("message")) : "not found";
            return ResponseEntity.ok().headers(headers).body(ApiResponse.fail(msg, code));
        }
        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok(m));
    }
}
