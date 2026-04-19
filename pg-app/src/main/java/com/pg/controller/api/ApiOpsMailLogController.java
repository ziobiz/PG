package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.MailSendLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 운영관리 — 메일 발송 로그(이메일무효·테스트 등).
 */
@RestController
@RequestMapping(value = "/api/ops/mailLog", produces = "application/json")
public class ApiOpsMailLogController {

    private final MailSendLogService mailSendLogService;

    public ApiOpsMailLogController(MailSendLogService mailSendLogService) {
        this.mailSendLogService = mailSendLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String mailKind,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            LocalDate from = parseLocalDate(fromDate);
            LocalDate to = parseLocalDate(toDate);
            PageResult<Map<String, Object>> r = mailSendLogService.search(page, size, mailKind, status, from, to);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (DateTimeParseException e) {
            return ResponseEntity.ok(ApiResponse.fail("날짜 형식이 올바르지 않습니다. (YYYY-MM-DD)", "VALIDATION"));
        }
    }

    private static LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }
}
