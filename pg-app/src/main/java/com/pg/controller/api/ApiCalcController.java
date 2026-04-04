package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.service.ChillPayService;
import com.pg.service.PayListActionService;
import com.pg.service.PayListService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/calc", produces = "application/json")
public class ApiCalcController {

    private final PayListService payListService;
    private final PayListActionService payListActionService;
    private final ChillPayService chillPayService;

    public ApiCalcController(PayListService payListService, PayListActionService payListActionService,
                             ChillPayService chillPayService) {
        this.payListService = payListService;
        this.payListActionService = payListActionService;
        this.chillPayService = chillPayService;
    }

    @GetMapping("/payList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payList(
            @RequestParam Map<String, String> params) {
        PayListSearchRequest req = PayListSearchRequest.fromParams(params);
        PageResult<Map<String, Object>> result = payListService.search(req);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * ChillPay Transaction Services — Search Payment Transaction (실시간).
     * 문서: ChillPay-API-Transaction-Services-Document-EN_v1.0.6 Table 1.2~1.3
     */
    @GetMapping("/chillPayTrSearch")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> chillPayTrSearch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchOrderBy,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchMerchantCode,
            @RequestParam(required = false) String searchPaymentChannel,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchChillStatus,
            @RequestParam(required = false) String searchRouteNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate) {
        try {
            Integer routeNo = null;
            if (searchRouteNo != null && !searchRouteNo.isBlank()) {
                try {
                    routeNo = Integer.parseInt(searchRouteNo.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }
            PageResult<Map<String, Object>> r = chillPayService.searchChillPayPaymentTransactions(
                    null,
                    page,
                    size,
                    searchOrderBy,
                    searchOrderDir,
                    searchKeyword,
                    searchMerchantCode,
                    searchPaymentChannel,
                    routeNo,
                    searchOrderNo,
                    searchChillStatus,
                    searchFromDate,
                    searchToDate);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    /** 결제내역 후속조치: 자동무효·이메일무효·자동환불·강제환불 (본사 환경설정 Y 일 때만) */
    @PostMapping("/payAction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payAction(@RequestBody Map<String, String> body) {
        try {
            String trnId = body != null ? body.get("trnId") : null;
            String action = body != null ? body.get("action") : null;
            payListActionService.apply(trnId, action);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "처리되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }
}
