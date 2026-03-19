package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
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

    public ApiCalcController(PayListService payListService, PayListActionService payListActionService) {
        this.payListService = payListService;
        this.payListActionService = payListActionService;
    }

    @GetMapping("/payList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payList(
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchTmnId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) String searchPayProcCd,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String payListVariant,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String merchantId = searchKeyword != null && !searchKeyword.isEmpty() ? searchKeyword : searchCompNm;
        PageResult<Map<String, Object>> result = payListService.search(merchantId, searchFromDate, searchToDate, page, size, payListVariant);
        return ResponseEntity.ok(ApiResponse.ok(result));
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
