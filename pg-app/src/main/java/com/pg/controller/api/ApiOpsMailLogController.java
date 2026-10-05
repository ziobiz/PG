package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.MailSendLog;
import com.pg.service.MailSendLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 운영관리 — 메일 발송 로그(이메일무효·거래명세서 등).
 */
@RestController
@RequestMapping("/api/ops/mailLog")
public class ApiOpsMailLogController {

    private final MailSendLogService mailSendLogService;

    public ApiOpsMailLogController(MailSendLogService mailSendLogService) {
        this.mailSendLogService = mailSendLogService;
    }

    @GetMapping(produces = "application/json")
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

    /** 고객에게 보낸 본문 HTML 미리보기 */
    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> preview(@PathVariable long id) {
        try {
            String html = mailSendLogService.resolveDisplayHtml(id);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            String msg = "<!DOCTYPE html><html><body><p>" + escape(e.getMessage()) + "</p></body></html>";
            return ResponseEntity.status(404)
                    .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** 본문 PDF 다운로드 */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdf(@PathVariable long id) {
        try {
            MailSendLog row = mailSendLogService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("메일 로그를 찾을 수 없습니다."));
            byte[] pdf = mailSendLogService.toPdfBytes(id);
            String fileName = MailSendLogService.suggestPdfFileName(row);
            String asciiName = fileName.replaceAll("[^A-Za-z0-9._-]+", "_");
            String disposition = "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''"
                    + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage(), "PDF_FAILED"));
        }
    }

    /** 동일 수신처로 본문 재전송 */
    @PostMapping(value = "/{id}/resend", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resend(@PathVariable long id) {
        try {
            Map<String, Object> r = mailSendLogService.resendStored(id, currentUsername());
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "RESEND_FAILED"));
        }
    }

    /** 메일 로그 행 삭제 */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable long id) {
        try {
            mailSendLogService.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("deleted", true, "id", id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        }
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return null;
        }
        return auth.getName().trim();
    }

    private static LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
