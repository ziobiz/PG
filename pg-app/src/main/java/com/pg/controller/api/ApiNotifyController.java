package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.MerchantNotifyOutboundLogService;
import com.pg.service.NotifyUrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/notify", produces = "application/json")
public class ApiNotifyController {

    private final NotifyUrlService notifyUrlService;
    private final MerchantNotifyOutboundLogService merchantNotifyOutboundLogService;

    public ApiNotifyController(NotifyUrlService notifyUrlService,
                               MerchantNotifyOutboundLogService merchantNotifyOutboundLogService) {
        this.notifyUrlService = notifyUrlService;
        this.merchantNotifyOutboundLogService = merchantNotifyOutboundLogService;
    }

    private static PageResult<Map<String, Object>> emptyPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }

    @GetMapping("/payUrlMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payUrlMng(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchUrlType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> result = notifyUrlService.searchPayUrl(searchCompId, searchUrlType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/paySendMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> paySendMng(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate from = searchFromDate != null ? searchFromDate : LocalDate.now();
        LocalDate to = searchToDate != null ? searchToDate : LocalDate.now();
        PageResult<Map<String, Object>> result = merchantNotifyOutboundLogService.search(
                page, size, from, to, searchCompNm);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/cashReceiptUrlMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> cashReceiptUrlMng(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
    }

    @GetMapping("/cashReceiptSendMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> cashReceiptSendMng(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
    }
}
