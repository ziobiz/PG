package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.CommissionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/commission")
public class ApiCommissionController {

    private final CommissionService commissionService;

    public ApiCommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> pr = commissionService.search(searchCompId, searchCompNm, page, size);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam String compId) {
        return commissionService.getDetail(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.")));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(
            @RequestParam String compId,
            @RequestParam(required = false) String perTxFee,
            @RequestParam(required = false) String cancelRate,
            @RequestParam(required = false) String usageRate,
            @RequestParam(required = false) String failFee,
            @RequestParam(required = false) String payRate,
            @RequestParam(required = false) String refundRate,
            @RequestParam(required = false) String rollingPct,
            @RequestParam(required = false) String rollingDays,
            @RequestParam(required = false) String feeAccountActivation,
            @RequestParam(required = false) String feeAnnual,
            @RequestParam(required = false) String feeTechService,
            @RequestParam(required = false) String feeSettlementPerTx,
            @RequestParam(required = false) String feeRefund) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("perTxFee", perTxFee != null ? perTxFee : "");
        body.put("cancelRate", cancelRate != null ? cancelRate : "");
        body.put("usageRate", usageRate != null ? usageRate : "");
        body.put("failFee", failFee != null ? failFee : "");
        body.put("payRate", payRate != null ? payRate : "");
        body.put("refundRate", refundRate != null ? refundRate : "");
        body.put("rollingPct", rollingPct != null ? rollingPct : "");
        body.put("rollingDays", rollingDays != null ? rollingDays : "");
        body.put("feeAccountActivation", feeAccountActivation != null ? feeAccountActivation : "");
        body.put("feeAnnual", feeAnnual != null ? feeAnnual : "");
        body.put("feeTechService", feeTechService != null ? feeTechService : "");
        body.put("feeSettlementPerTx", feeSettlementPerTx != null ? feeSettlementPerTx : "");
        body.put("feeRefund", feeRefund != null ? feeRefund : "");
        boolean ok = commissionService.save(compId, body);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다."));
    }
}
