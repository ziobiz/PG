package com.pg.splitpay;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayEmailPhase;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayEmailPhaseRepository;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.LedgerSmtpMailService;
import com.pg.service.MailSendLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SplitPayEmailSettingsService {

    public static final List<String> PHASES = List.of("D_MINUS1", "D0", "D1", "D2", "D3");
    public static final List<String> LOCALES = List.of(
            SplitPayMailLocaleUtil.KOR,
            SplitPayMailLocaleUtil.ENG,
            SplitPayMailLocaleUtil.JPN,
            SplitPayMailLocaleUtil.CHN,
            SplitPayMailLocaleUtil.THA);

    private final SplitPayEmailPhaseRepository phaseRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final MailSendLogService mailSendLogService;

    public SplitPayEmailSettingsService(SplitPayEmailPhaseRepository phaseRepository,
                                        HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                        LedgerSmtpMailService ledgerSmtpMailService,
                                        MailSendLogService mailSendLogService) {
        this.phaseRepository = phaseRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.mailSendLogService = mailSendLogService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadAll() {
        ensurePhases();
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> phases = new LinkedHashMap<>();
        for (SplitPayEmailPhase p : phaseRepository.findAllByOrderByPhaseAsc()) {
            phases.put(p.getPhase(), toPhaseMap(p));
        }
        out.put("phases", phases);
        out.put("smtpHint", smtpHint());
        return out;
    }

    @Transactional
    public Map<String, Object> saveFromBody(Map<String, Object> body) {
        ensurePhases();
        Object raw = body != null ? body.get("phases") : null;
        if (!(raw instanceof Map<?, ?> phaseMap)) {
            throw new IllegalArgumentException("phases 항목이 필요합니다.");
        }
        for (String ph : PHASES) {
            Object node = phaseMap.get(ph);
            if (!(node instanceof Map<?, ?> m)) {
                continue;
            }
            SplitPayEmailPhase row = phaseRepository.findById(ph).orElseGet(() -> {
                SplitPayEmailPhase n = new SplitPayEmailPhase();
                n.setPhase(ph);
                return n;
            });
            applyPhaseFields(row, m);
            phaseRepository.save(row);
        }
        return loadAll();
    }

    @Transactional
    public void sendTestMail(String phase, String locale, String testRecipientEmail, String actorUsername) {
        if (testRecipientEmail == null || testRecipientEmail.isBlank()) {
            throw new IllegalArgumentException("테스트 수신 이메일을 입력하세요.");
        }
        String ph = normalizePhase(phase);
        String loc = SplitPayMailLocaleUtil.normalize(locale);
        ensurePhases();
        SplitPayEmailPhase cfg = phaseRepository.findById(ph)
                .orElseThrow(() -> new IllegalStateException("이메일 단계 설정을 찾을 수 없습니다."));
        SplitPayContract sampleC = sampleContract(loc);
        SplitPayInstallment sampleI = sampleInstallment(sampleC);
        String payUrl = "https://pay.example.com/split-pay.html?token=SAMPLE-TOKEN";
        String subjectTpl = resolveSubjectTemplate(cfg, loc);
        String bodyTpl = resolveBodyTemplate(cfg, loc);
        String subject = "[TEST] " + SplitPayMailTemplateRenderer.render(subjectTpl, sampleC, sampleI, payUrl);
        String body = "(This is a test message from PG admin — split payment email template preview.)\n\n"
                + SplitPayMailTemplateRenderer.render(bodyTpl, sampleC, sampleI, payUrl);
        HqLedgerSysSettings smtp = hqLedgerSysSettingsService.getOrCreate();
        String to = testRecipientEmail.trim();
        try {
            sendWithOverrides(smtp, cfg, to, subject, body);
            mailSendLogService.append(MailSendLogService.KIND_SPLIT_PAY_TEST, MailSendLogService.STATUS_SUCCESS,
                    to, subject, body, null, sampleI.getOrderNo(), actorUsername);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            mailSendLogService.append(MailSendLogService.KIND_SPLIT_PAY_TEST, MailSendLogService.STATUS_FAIL,
                    to, subject, body, msg, sampleI.getOrderNo(), actorUsername);
            throw e;
        }
    }

    public SplitPayEmailSettingsService.ResolvedMail resolveMail(String phase, SplitPayContract contract, SplitPayInstallment inst, String payUrl) {
        ensurePhases();
        String ph = normalizePhaseForTemplate(phase);
        String loc = SplitPayMailLocaleUtil.normalize(
                contract != null && contract.getCustomerLocale() != null ? contract.getCustomerLocale() : SplitPayMailLocaleUtil.KOR);
        SplitPayEmailPhase cfg = phaseRepository.findById(ph).orElse(null);
        String subjectTpl = resolveSubjectTemplate(cfg, loc);
        String bodyTpl = resolveBodyTemplate(cfg, loc);
        String subject = SplitPayMailTemplateRenderer.render(subjectTpl, contract, inst, payUrl);
        String body = SplitPayMailTemplateRenderer.render(bodyTpl, contract, inst, payUrl);
        return new ResolvedMail(subject, body, cfg);
    }

    public void deliverMail(HqLedgerSysSettings smtp, ResolvedMail mail, SplitPayEmailPhase cfg,
                            String primaryTo, String logKind, String orderNo, String actor) {
        if (primaryTo == null || primaryTo.isBlank()) {
            return;
        }
        String subject = mail.subject();
        String body = mail.body();
        String to = primaryTo.trim();
        try {
            sendWithOverrides(smtp, cfg, to, subject, body);
            mailSendLogService.append(logKind, MailSendLogService.STATUS_SUCCESS,
                    to, subject, body, null, orderNo, actor);
            notifyAlertRecipients(smtp, cfg, to, subject, body, logKind, orderNo, actor);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            mailSendLogService.append(logKind, MailSendLogService.STATUS_FAIL,
                    to, subject, body, msg, orderNo, actor);
            throw e;
        }
    }

    private void notifyAlertRecipients(HqLedgerSysSettings smtp, SplitPayEmailPhase cfg, String primaryTo,
                                       String subject, String body, String logKind,
                                       String orderNo, String actor) {
        if (cfg == null || cfg.getAlertRecipientEmails() == null || cfg.getAlertRecipientEmails().isBlank()) {
            return;
        }
        for (String alertTo : splitEmails(cfg.getAlertRecipientEmails())) {
            if (alertTo.isBlank() || alertTo.equalsIgnoreCase(primaryTo)) {
                continue;
            }
            try {
                sendWithOverrides(smtp, cfg, alertTo, "[ALERT] " + subject, body);
                mailSendLogService.append(logKind + "_ALERT", MailSendLogService.STATUS_SUCCESS,
                        alertTo, subject, body, null, orderNo, actor);
            } catch (Exception ignored) {
                mailSendLogService.append(logKind + "_ALERT", MailSendLogService.STATUS_FAIL,
                        alertTo, subject, body, "alert copy failed", orderNo, actor);
            }
        }
    }

    private void sendWithOverrides(HqLedgerSysSettings smtp, SplitPayEmailPhase cfg,
                                   String to, String subject, String body) {
        String fromAddr = cfg != null && cfg.getMailFromAddress() != null && !cfg.getMailFromAddress().isBlank()
                ? cfg.getMailFromAddress().trim() : null;
        String fromName = cfg != null && cfg.getMailFromName() != null ? cfg.getMailFromName().trim() : null;
        ledgerSmtpMailService.sendPlainText(smtp, to, subject, body, fromAddr, fromName);
    }

    private static List<String> splitEmails(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String p : raw.split("[,;\\s]+")) {
            String t = p != null ? p.trim() : "";
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private String resolveSubjectTemplate(SplitPayEmailPhase cfg, String locale) {
        String custom = subjectForLocale(cfg, locale);
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return SplitPayMailI18n.subject(locale, "SAMPLE-CONTRACT", 1);
    }

    private String resolveBodyTemplate(SplitPayEmailPhase cfg, String locale) {
        String custom = bodyForLocale(cfg, locale);
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return defaultBodyTemplate(locale);
    }

    private static String defaultBodyTemplate(String locale) {
        return switch (SplitPayMailLocaleUtil.normalize(locale)) {
            case SplitPayMailLocaleUtil.ENG -> """
                    ICOPAY split payment notice

                    Contract: {{contractNo}}
                    Installment: {{installmentNo}} / {{installmentCount}}
                    Amount: {{amount}} {{currency}}
                    Due date: {{dueDate}}
                    Order No: {{orderNo}}

                    Please pay at the link below:
                    {{payUrl}}
                    """;
            case SplitPayMailLocaleUtil.JPN -> """
                    ICOPAY 分割払いのご案内

                    契約番号: {{contractNo}}
                    回数: {{installmentNo}} / {{installmentCount}}
                    金額: {{amount}} {{currency}}
                    支払期日: {{dueDate}}
                    注文番号: {{orderNo}}

                    下記リンクよりお支払いください。
                    {{payUrl}}
                    """;
            case SplitPayMailLocaleUtil.CHN -> """
                    ICOPAY 分期付款通知

                    合同号: {{contractNo}}
                    期数: {{installmentNo}} / {{installmentCount}}
                    金额: {{amount}} {{currency}}
                    到期日: {{dueDate}}
                    订单号: {{orderNo}}

                    请点击以下链接完成付款:
                    {{payUrl}}
                    """;
            case SplitPayMailLocaleUtil.THA -> """
                    แจ้งการชำระแบบแบ่งงวด ICOPAY

                    เลขสัญญา: {{contractNo}}
                    งวด: {{installmentNo}} / {{installmentCount}}
                    จำนวน: {{amount}} {{currency}}
                    วันครบกำหนด: {{dueDate}}
                    เลขคำสั่ง: {{orderNo}}

                    ชำระได้ที่ลิงก์:
                    {{payUrl}}
                    """;
            default -> """
                    ICOPAY 분할결제 안내

                    계약번호: {{contractNo}}
                    회차: {{installmentNo}} / {{installmentCount}}
                    금액: {{amount}} {{currency}}
                    결제예정일: {{dueDate}}
                    주문번호: {{orderNo}}

                    아래 링크에서 결제해 주세요.
                    {{payUrl}}
                    """;
        };
    }

    private static String subjectForLocale(SplitPayEmailPhase p, String locale) {
        if (p == null) {
            return null;
        }
        return switch (SplitPayMailLocaleUtil.normalize(locale)) {
            case SplitPayMailLocaleUtil.ENG -> p.getSubjectEng();
            case SplitPayMailLocaleUtil.JPN -> p.getSubjectJpn();
            case SplitPayMailLocaleUtil.CHN -> p.getSubjectChn();
            case SplitPayMailLocaleUtil.THA -> p.getSubjectTha();
            default -> p.getSubjectKor();
        };
    }

    private static String bodyForLocale(SplitPayEmailPhase p, String locale) {
        if (p == null) {
            return null;
        }
        return switch (SplitPayMailLocaleUtil.normalize(locale)) {
            case SplitPayMailLocaleUtil.ENG -> p.getBodyEng();
            case SplitPayMailLocaleUtil.JPN -> p.getBodyJpn();
            case SplitPayMailLocaleUtil.CHN -> p.getBodyChn();
            case SplitPayMailLocaleUtil.THA -> p.getBodyTha();
            default -> p.getBodyKor();
        };
    }

    private void applyPhaseFields(SplitPayEmailPhase row, Map<?, ?> m) {
        row.setMailFromAddress(str(m.get("mailFromAddress")));
        row.setMailFromName(str(m.get("mailFromName")));
        row.setAlertRecipientEmails(str(m.get("alertRecipientEmails")));
        row.setTestRecipientEmail(str(m.get("testRecipientEmail")));
        Object locRaw = m.get("locales");
        if (locRaw instanceof Map<?, ?> locMap) {
            applyLocale(row, SplitPayMailLocaleUtil.KOR, locMap.get("KOR"));
            applyLocale(row, SplitPayMailLocaleUtil.ENG, locMap.get("ENG"));
            applyLocale(row, SplitPayMailLocaleUtil.JPN, locMap.get("JPN"));
            applyLocale(row, SplitPayMailLocaleUtil.CHN, locMap.get("CHN"));
            applyLocale(row, SplitPayMailLocaleUtil.THA, locMap.get("THA"));
        }
    }

    private void applyLocale(SplitPayEmailPhase row, String locale, Object node) {
        if (!(node instanceof Map<?, ?> m)) {
            return;
        }
        String sub = str(m.get("subject"));
        String body = str(m.get("body"));
        switch (locale) {
            case SplitPayMailLocaleUtil.ENG -> {
                row.setSubjectEng(sub);
                row.setBodyEng(body);
            }
            case SplitPayMailLocaleUtil.JPN -> {
                row.setSubjectJpn(sub);
                row.setBodyJpn(body);
            }
            case SplitPayMailLocaleUtil.CHN -> {
                row.setSubjectChn(sub);
                row.setBodyChn(body);
            }
            case SplitPayMailLocaleUtil.THA -> {
                row.setSubjectTha(sub);
                row.setBodyTha(body);
            }
            default -> {
                row.setSubjectKor(sub);
                row.setBodyKor(body);
            }
        }
    }

    private Map<String, Object> toPhaseMap(SplitPayEmailPhase p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", p.getPhase());
        m.put("mailFromAddress", nz(p.getMailFromAddress()));
        m.put("mailFromName", nz(p.getMailFromName()));
        m.put("alertRecipientEmails", nz(p.getAlertRecipientEmails()));
        m.put("testRecipientEmail", nz(p.getTestRecipientEmail()));
        Map<String, Object> locales = new LinkedHashMap<>();
        locales.put("KOR", localeNode(p.getSubjectKor(), p.getBodyKor()));
        locales.put("ENG", localeNode(p.getSubjectEng(), p.getBodyEng()));
        locales.put("JPN", localeNode(p.getSubjectJpn(), p.getBodyJpn()));
        locales.put("CHN", localeNode(p.getSubjectChn(), p.getBodyChn()));
        locales.put("THA", localeNode(p.getSubjectTha(), p.getBodyTha()));
        m.put("locales", locales);
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString().replace('T', ' ') : "");
        return m;
    }

    private static Map<String, Object> localeNode(String subject, String body) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("subject", nz(subject));
        n.put("body", nz(body));
        return n;
    }

    private Map<String, String> smtpHint() {
        HqLedgerSysSettings s = hqLedgerSysSettingsService.getOrCreate();
        Map<String, String> h = new LinkedHashMap<>();
        h.put("mailFromAddress", nz(s.getMailFromAddress()));
        h.put("mailFromName", nz(s.getMailFromName()));
        h.put("smtpHost", nz(s.getSmtpHost()));
        return h;
    }

    private void ensurePhases() {
        Set<String> existing = phaseRepository.findAll().stream()
                .map(SplitPayEmailPhase::getPhase)
                .collect(java.util.stream.Collectors.toSet());
        for (String ph : PHASES) {
            if (!existing.contains(ph)) {
                SplitPayEmailPhase n = new SplitPayEmailPhase();
                n.setPhase(ph);
                phaseRepository.save(n);
            }
        }
    }

    public static String normalizePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return "D0";
        }
        String u = phase.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "D_MINUS1", "DM1", "D-1" -> "D_MINUS1";
            case "D1", "D+1" -> "D1";
            case "D2", "D+2" -> "D2";
            case "D3", "D+3" -> "D3";
            case "CREATE" -> "D0";
            default -> "D0";
        };
    }

    private static String normalizePhaseForTemplate(String phase) {
        if (phase == null || phase.isBlank()) {
            return "D0";
        }
        String u = phase.trim().toUpperCase(Locale.ROOT);
        if ("CREATE".equals(u)) {
            return "D0";
        }
        return normalizePhase(phase);
    }

    private static SplitPayContract sampleContract(String locale) {
        SplitPayContract c = new SplitPayContract();
        c.setContractNo("SAMPLE-CONTRACT-001");
        c.setCustomerEmail("customer@example.com");
        c.setCustomerName("Sample Customer");
        c.setCustomerLocale(locale);
        c.setTotalAmount(new BigDecimal("10000"));
        c.setCurrencyCode("JPY");
        c.setInstallmentCount(3);
        return c;
    }

    private static SplitPayInstallment sampleInstallment(SplitPayContract c) {
        SplitPayInstallment i = new SplitPayInstallment();
        i.setInstallmentNo(2);
        i.setOrderNo("SAMPLE-ORDER-002");
        i.setAmount(new BigDecimal("3333.33"));
        i.setDueDateAdjusted(LocalDate.now().plusDays(1));
        return i;
    }

    private static String str(Object v) {
        return v != null ? v.toString().trim() : "";
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }

    public record ResolvedMail(String subject, String body, SplitPayEmailPhase phaseConfig) {
    }
}
