package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.splitpay.SplitPayListService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/calc")
public class ApiCalcSplitPayController {

    private final SplitPayListService splitPayListService;

    public ApiCalcSplitPayController(SplitPayListService splitPayListService) {
        this.splitPayListService = splitPayListService;
    }

    @GetMapping("/splitPayList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> splitPayList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate) {
        return ResponseEntity.ok(ApiResponse.ok(
                splitPayListService.search(page, size, compId, contractNo, status, searchFromDate, searchToDate)));
    }
}
