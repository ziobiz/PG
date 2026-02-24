package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.PayListService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/calc")
public class ApiCalcController {

    private final PayListService payListService;

    public ApiCalcController(PayListService payListService) {
        this.payListService = payListService;
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String merchantId = searchKeyword != null && !searchKeyword.isEmpty() ? searchKeyword : searchCompNm;
        PageResult<Map<String, Object>> result = payListService.search(merchantId, searchFromDate, searchToDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
