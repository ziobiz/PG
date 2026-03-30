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
@RequestMapping(value = "/api/commission", produces = "application/json")
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
            @RequestParam(defaultValue = "500") int size) {
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

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> history(
            @RequestParam String compId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.ok(commissionService.history(compId, page, size)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(
            @RequestParam String compId,
            @RequestParam(required = false) String perTxFee,
            @RequestParam(required = false) String cancelRate,
            @RequestParam(required = false) String voidFeePerTx,
            @RequestParam(required = false) String manualVoidFeePerTx,
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
            @RequestParam(required = false) String feeRefund,
            @RequestParam(required = false) String hqRate,
            @RequestParam(required = false) String regionalRate,
            @RequestParam(required = false) String masterRate,
            @RequestParam(required = false) String branchRate,
            @RequestParam(required = false) String agencyRate,
            @RequestParam(required = false) String salesOfficeRate,
            @RequestParam(required = false) String hqPerTxFee,
            @RequestParam(required = false) String regionalPerTxFee,
            @RequestParam(required = false) String masterPerTxFee,
            @RequestParam(required = false) String branchPerTxFee,
            @RequestParam(required = false) String agencyPerTxFee,
            @RequestParam(required = false) String salesOfficePerTxFee,
            @RequestParam(required = false) String applyStartDate) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("perTxFee", perTxFee != null ? perTxFee : "");
        body.put("cancelRate", cancelRate != null ? cancelRate : "");
        body.put("voidFeePerTx", voidFeePerTx != null ? voidFeePerTx : "");
        body.put("manualVoidFeePerTx", manualVoidFeePerTx != null ? manualVoidFeePerTx : "");
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
        body.put("hqRate", hqRate != null ? hqRate : "");
        body.put("regionalRate", regionalRate != null ? regionalRate : "");
        body.put("masterRate", masterRate != null ? masterRate : "");
        body.put("branchRate", branchRate != null ? branchRate : "");
        body.put("agencyRate", agencyRate != null ? agencyRate : "");
        body.put("salesOfficeRate", salesOfficeRate != null ? salesOfficeRate : "");
        body.put("hqPerTxFee", hqPerTxFee != null ? hqPerTxFee : "");
        body.put("regionalPerTxFee", regionalPerTxFee != null ? regionalPerTxFee : "");
        body.put("masterPerTxFee", masterPerTxFee != null ? masterPerTxFee : "");
        body.put("branchPerTxFee", branchPerTxFee != null ? branchPerTxFee : "");
        body.put("agencyPerTxFee", agencyPerTxFee != null ? agencyPerTxFee : "");
        body.put("salesOfficePerTxFee", salesOfficePerTxFee != null ? salesOfficePerTxFee : "");
        body.put("applyStartDate", applyStartDate != null ? applyStartDate : "");
        boolean ok = commissionService.save(compId, body);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다."));
    }
}
