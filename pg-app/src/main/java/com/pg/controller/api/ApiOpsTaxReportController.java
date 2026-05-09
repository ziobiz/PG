package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.ops.TaxReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 운영관리 — 태국 신고용 TAX 리포트(확정 정산 실행 목록·월별 통합 엑셀).
 */
@RestController
@RequestMapping(value = "/api/ops/taxReport")
public class ApiOpsTaxReportController {

    private final TaxReportService taxReportService;

    public ApiOpsTaxReportController(TaxReportService taxReportService) {
        this.taxReportService = taxReportService;
    }

    @GetMapping(value = "/access", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(taxReportService.accessMeta(authentication)));
    }

    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResult<Map<String, Object>> pr =
                taxReportService.listRuns(authentication, searchFromDate, searchToDate, searchCompId, searchOrderDir, page, size);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /**
     * 엑셀(xlsx): 전체 행(페이지 무관)·TOTAL 행·가맹별 합계 블록.
     */
    @PostMapping(value = "/export",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> export(Authentication authentication,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : new LinkedHashMap<>();
        LocalDate from = parseLocalDate(b.get("searchFromDate"));
        LocalDate to = parseLocalDate(b.get("searchToDate"));
        String ym = b.get("searchYearMonth") != null ? String.valueOf(b.get("searchYearMonth")).trim() : "";
        String scope = b.get("searchTaxScope") != null ? String.valueOf(b.get("searchTaxScope")).trim() : "";
        if ("MONTHLY".equalsIgnoreCase(scope) && !ym.isBlank()) {
            Map<String, LocalDate> bounds = taxReportService.resolveMonthBounds(ym);
            from = bounds.get("from");
            to = bounds.get("to");
        }
        String searchCompId = b.get("searchCompId") != null ? String.valueOf(b.get("searchCompId")).trim() : "";
        String searchOrderDir = b.get("searchOrderDir") != null ? String.valueOf(b.get("searchOrderDir")).trim() : "DESC";
        String sheetLabel = "MONTHLY".equalsIgnoreCase(scope) && !ym.isBlank() ? "TH_TAX_" + ym : "TH_TAX_period";

        try {
            byte[] bytes = taxReportService.exportStyledXlsx(sheetLabel, authentication, from, to, searchCompId, searchOrderDir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fn = sheetLabel + "_" + stamp + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.status(403)
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "EXPORT_FAILED";
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static LocalDate parseLocalDate(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        return LocalDate.parse(s);
    }
}
