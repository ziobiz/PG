package com.pg.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pg.api.dto.PageResult;
import com.pg.entity.MailSendLog;
import com.pg.receipt.PdfUnicodeFontSupport;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.MailSendLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MailSendLogService {

    public static final String KIND_VOID_TEST = "VOID_TEST";
    public static final String KIND_VOID_TXN = "VOID_TXN";
    public static final String KIND_SPLIT_PAY_D_MINUS1 = "SPLIT_PAY_D_MINUS1";
    public static final String KIND_SPLIT_PAY_D0 = "SPLIT_PAY_D0";
    public static final String KIND_SPLIT_PAY_D1 = "SPLIT_PAY_D1";
    public static final String KIND_SPLIT_PAY_D2 = "SPLIT_PAY_D2";
    public static final String KIND_SPLIT_PAY_D3 = "SPLIT_PAY_D3";
    public static final String KIND_SPLIT_PAY_CREATE = "SPLIT_PAY_CREATE";
    public static final String KIND_SPLIT_PAY_TEST = "SPLIT_PAY_TEST";
    public static final String KIND_RECEIPT_TXN = "RECEIPT_TXN";
    public static final String KIND_RECEIPT_FAIL = "RECEIPT_FAIL";
    public static final String KIND_RECEIPT_CANCEL = "RECEIPT_CANCEL";
    public static final String KIND_RECEIPT_REFUND = "RECEIPT_REFUND";
    public static final String KIND_RECEIPT_VOID = "RECEIPT_VOID";
    public static final String KIND_RECEIPT_TEST = "RECEIPT_TEST";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    private static final int PREVIEW_MAX = 800;
    private static final int HTML_MAX = 512_000;

    private final MailSendLogRepository mailSendLogRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public MailSendLogService(MailSendLogRepository mailSendLogRepository,
                              HqLedgerSysSettingsService hqLedgerSysSettingsService,
                              LedgerSmtpMailService ledgerSmtpMailService,
                              @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.mailSendLogRepository = mailSendLogRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    /** 평문만 있는 발송(무효메일 등). HTML 없음. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String mailKind, String status, String toAddress, String subject, String body,
                       String errorMessage, String pgTrnId, String actorUsername) {
        append(mailKind, status, toAddress, subject, body, null, errorMessage, pgTrnId, actorUsername);
    }

    /**
     * @param bodyPlain 평문(로그 요약용)
     * @param bodyHtml  고객에게 보낸 HTML(미리보기·PDF). null이면 평문만 보관
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String mailKind, String status, String toAddress, String subject,
                       String bodyPlain, String bodyHtml,
                       String errorMessage, String pgTrnId, String actorUsername) {
        MailSendLog row = new MailSendLog();
        row.setMailKind(mailKind != null ? mailKind.trim() : "");
        row.setStatus(status != null ? status.trim() : STATUS_FAIL);
        row.setToAddress(toAddress != null ? trimTo(toAddress, 500) : "");
        row.setSubject(subject != null ? trimTo(subject, 500) : "");
        row.setBodyPreview(preview(bodyPlain != null ? bodyPlain : stripTags(bodyHtml)));
        row.setBodyHtml(truncateHtml(bodyHtml));
        row.setErrorMessage(errorMessage != null ? trimTo(errorMessage, 4000) : null);
        row.setPgTrnId(pgTrnId != null ? trimTo(pgTrnId, 32) : null);
        row.setActorUsername(actorUsername != null ? trimTo(actorUsername, 128) : null);
        mailSendLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page1Based, int size,
                                                  String mailKind, String status,
                                                  LocalDate fromDate, LocalDate toDate) {
        int pageIdx = Math.max(page1Based, 1) - 1;
        int sz = size <= 0 ? 20 : Math.min(size, 200);
        Specification<MailSendLog> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (mailKind != null && !mailKind.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("mailKind")), mailKind.trim().toUpperCase(Locale.ROOT)));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
            }
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(fromDate, LocalTime.MIN)));
            }
            if (toDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(toDate, LocalTime.MAX)));
            }
            if (ps.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<MailSendLog> p = mailSendLogRepository.findAll(spec,
                PageRequest.of(pageIdx, sz, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(p, this::toRow);
    }

    @Transactional(readOnly = true)
    public Optional<MailSendLog> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return mailSendLogRepository.findById(id);
    }

    /** 미리보기용 HTML(고객 수신과 동일. HTML 없으면 평문을 pre로 감쌈). */
    @Transactional(readOnly = true)
    public String resolveDisplayHtml(Long id) {
        MailSendLog e = findById(id).orElseThrow(() -> new IllegalArgumentException("메일 로그를 찾을 수 없습니다."));
        return resolveDisplayHtml(e);
    }

    /**
     * 미리보기·PDF 공통 HTML.
     * 영수증(RECEIPT*)은 거래번호로 현재 PG·총판·가맹 설정을 반영해 재생성한다.
     * (발송 당시 bodyHtml만 쓰면 Acquirer 등을 나중에 고쳐도 미리보기에 안 나옴 — 쿠키 문제 아님)
     * 미리보기와 PDF는 이 메서드만 사용하므로 내용이 동일하다.
     */
    public String resolveDisplayHtml(MailSendLog e) {
        if (e == null) {
            return wrapPlain("(empty)");
        }
        if ("RECEIPT".equals(natureOf(e.getMailKind()))
                && e.getPgTrnId() != null && !e.getPgTrnId().isBlank()) {
            Optional<String> rebuilt = transactionReceiptEmailService.renderHtmlForMailLog(
                    e.getPgTrnId(), e.getMailKind());
            if (rebuilt.isPresent() && !rebuilt.get().isBlank()) {
                return rebuilt.get();
            }
        }
        String html = e.getBodyHtml();
        if (html != null && !html.isBlank()) {
            return html;
        }
        String plain = e.getBodyPreview();
        if (plain == null || plain.isBlank()) {
            return wrapPlain("(no body)");
        }
        return wrapPlain(plain);
    }

    @Transactional(readOnly = true)
    public byte[] toPdfBytes(Long id) {
        MailSendLog e = findById(id).orElseThrow(() -> new IllegalArgumentException("메일 로그를 찾을 수 없습니다."));
        String html = resolveDisplayHtml(e);
        String xhtml = PdfUnicodeFontSupport.prepareHtmlForPdf(toXhtml(html));
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            PdfUnicodeFontSupport.register(builder);
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            throw new IllegalStateException("PDF 변환 실패: " + msg, ex);
        }
    }

    /**
     * PDF 파일명: {@code yyyyMMdd_구분.pdf} (거래번호 있으면 {@code yyyyMMdd_구분_거래번호.pdf}).
     * 구분 = RECEIPT / VOID / SPLIT_PAY / OTHER
     */
    public static String suggestPdfFileName(MailSendLog e) {
        LocalDateTime at = e != null && e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now();
        String date = at.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nature = natureOf(e != null ? e.getMailKind() : null);
        String trn = e != null && e.getPgTrnId() != null ? e.getPgTrnId().trim() : "";
        if (!trn.isEmpty()) {
            trn = trn.replaceAll("[^A-Za-z0-9._-]+", "_");
            if (trn.length() > 64) {
                trn = trn.substring(0, 64);
            }
            return date + "_" + nature + "_" + trn + ".pdf";
        }
        Long id = e != null ? e.getId() : null;
        if (id != null) {
            return date + "_" + nature + "_" + id + ".pdf";
        }
        return date + "_" + nature + ".pdf";
    }

    @Transactional
    public void deleteById(Long id) {
        MailSendLog e = findById(id).orElseThrow(() -> new IllegalArgumentException("메일 로그를 찾을 수 없습니다."));
        mailSendLogRepository.delete(e);
    }

    /**
     * 저장된 본문(HTML 우선)을 동일 수신처로 재전송. 새 로그 행을 남깁니다.
     */
    @Transactional
    public Map<String, Object> resendStored(Long id, String actorUsername) {
        MailSendLog src = findById(id).orElseThrow(() -> new IllegalArgumentException("메일 로그를 찾을 수 없습니다."));
        String to = src.getToAddress();
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("수신 이메일이 없습니다.");
        }
        String subject = src.getSubject() != null ? src.getSubject() : "";
        if (!subject.startsWith("[RESEND]")) {
            subject = "[RESEND] " + subject;
        }
        String html = src.getBodyHtml();
        String plain = src.getBodyPreview() != null ? src.getBodyPreview() : stripTags(html);
        /* 영수증은 현재 설정(Acquirer 등)으로 재생성한 본문을 재전송 */
        if ("RECEIPT".equals(natureOf(src.getMailKind()))
                && src.getPgTrnId() != null && !src.getPgTrnId().isBlank()) {
            Optional<String> rebuilt = transactionReceiptEmailService.renderHtmlForMailLog(
                    src.getPgTrnId(), src.getMailKind());
            if (rebuilt.isPresent() && !rebuilt.get().isBlank()) {
                html = rebuilt.get();
                plain = stripTags(html);
            }
        }
        if ((html == null || html.isBlank()) && (plain == null || plain.isBlank())) {
            throw new IllegalArgumentException("재전송할 본문이 없습니다.");
        }
        String kind = src.getMailKind() != null ? src.getMailKind() : "";
        var smtp = hqLedgerSysSettingsService.getOrCreate();
        try {
            if (html != null && !html.isBlank()) {
                ledgerSmtpMailService.sendHtml(smtp, to, subject, html, plain != null ? plain : "");
            } else {
                ledgerSmtpMailService.sendPlainText(smtp, to, subject, plain);
            }
            append(kind, STATUS_SUCCESS, to, subject, plain, html, null, src.getPgTrnId(), actorUsername);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("resent", true);
            ok.put("toAddress", to);
            return ok;
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            append(kind, STATUS_FAIL, to, subject, plain, html, msg, src.getPgTrnId(), actorUsername);
            throw new IllegalStateException("재전송 실패: " + msg, ex);
        }
    }

    /** 영수증 / 메일취소 / 기타 */
    public static String natureOf(String mailKind) {
        String k = mailKind != null ? mailKind.trim().toUpperCase(Locale.ROOT) : "";
        if (k.startsWith("RECEIPT")) {
            return "RECEIPT";
        }
        if (k.startsWith("VOID")) {
            return "VOID";
        }
        if (k.startsWith("SPLIT_PAY")) {
            return "SPLIT_PAY";
        }
        return "OTHER";
    }

    private Map<String, Object> toRow(MailSendLog e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("mailKind", e.getMailKind());
        String nature = natureOf(e.getMailKind());
        m.put("mailNature", nature);
        m.put("status", e.getStatus());
        m.put("toAddress", e.getToAddress());
        m.put("subject", e.getSubject());
        m.put("bodyPreview", e.getBodyPreview());
        m.put("hasBody", (e.getBodyHtml() != null && !e.getBodyHtml().isBlank())
                || (e.getBodyPreview() != null && !e.getBodyPreview().isBlank()));
        m.put("hasBodyHtml", e.getBodyHtml() != null && !e.getBodyHtml().isBlank());
        m.put("errorMessage", e.getErrorMessage());
        m.put("pgTrnId", e.getPgTrnId());
        String actor = e.getActorUsername();
        if (actor == null || actor.isBlank()) {
            if ("RECEIPT".equals(nature)) {
                actor = "AUTO";
            } else {
                actor = "";
            }
        }
        m.put("actorUsername", actor);
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString().replace('T', ' ') : "");
        m.put("pdfFileName", suggestPdfFileName(e));
        return m;
    }

    private static String preview(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        String t = body.replace("\r\n", "\n").trim();
        if (t.length() <= PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, PREVIEW_MAX) + "…";
    }

    private static String truncateHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String t = html.trim();
        return t.length() <= HTML_MAX ? t : t.substring(0, HTML_MAX);
    }

    private static String stripTags(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String wrapPlain(String plain) {
        return "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/></head>"
                + "<body style=\"margin:0;background:#f0f4f8;\">"
                + "<pre style=\"white-space:pre-wrap;font-family:Segoe UI,Helvetica,Arial,sans-serif;font-size:13px;padding:16px;\">"
                + escapeHtml(plain) + "</pre></body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** openhtmltopdf 용 느슨한 XHTML 보정 */
    private static String toXhtml(String html) {
        if (html == null || html.isBlank()) {
            return wrapPlain("(empty)");
        }
        String h = html.trim();
        if (!h.toLowerCase(Locale.ROOT).contains("<html")) {
            h = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body>" + h + "</body></html>";
        }
        h = h.replaceAll("(?i)<meta([^>]*?)(?<!/)>", "<meta$1/>");
        h = h.replaceAll("(?i)<br\\s*>", "<br/>");
        h = h.replaceAll("(?i)<hr\\s*>", "<hr/>");
        h = h.replaceAll("(?i)<img([^>]*?)(?<!/)>", "<img$1/>");
        h = h.replace("&nbsp;", "&#160;");
        if (!h.contains("xmlns=")) {
            h = h.replaceFirst("(?i)<html\\b", "<html xmlns=\"http://www.w3.org/1999/xhtml\"");
        }
        return h;
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
