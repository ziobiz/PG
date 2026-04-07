package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.HqLedgerSysSettingsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * 결제내역 「이메일무효」 — 전산설정 SMTP·템플릿으로 PG사(예: ChillPay)에 VOID 요청 메일 발송.
 */
@Service
public class PayFollowEmailVoidService {

    private static final String DEFAULT_TO = "help@chillpay.co";
    private static final String DEFAULT_SUBJECT = "VOID request — Order {{orderNo}} / ChillPay Txn {{transNo}}";
    private static final String DEFAULT_BODY = "Dear ChillPay Support,\n\n"
            + "This is {{companyName}} (contact: {{contactName}}).\n"
            + "We request a VOID for the transaction below:\n\n"
            + "- ChillPay TransactionId: {{transNo}}\n"
            + "- OrderNo: {{orderNo}}\n"
            + "- Amount: {{amount}}\n"
            + "- RouteNo: {{routeNo}}\n"
            + "- Payment date: {{paymentDate}}\n"
            + "- Merchant (ICOPAY code): {{mid}}\n\n"
            + "Thank you.";

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HqLedgerSysSettingsRepository ledgerSysSettingsRepository;
    private final LedgerSmtpMailService ledgerSmtpMailService;

    public PayFollowEmailVoidService(HqLedgerSysSettingsRepository ledgerSysSettingsRepository,
                                     LedgerSmtpMailService ledgerSmtpMailService) {
        this.ledgerSysSettingsRepository = ledgerSysSettingsRepository;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
    }

    public void sendVoidRequestMail(PgTrnsctn t) {
        HqLedgerSysSettings s = ledgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        if (s == null) {
            throw new IllegalStateException("전산설정을 찾을 수 없습니다.");
        }
        String to = firstNonBlank(s.getEmailVoidTo(), DEFAULT_TO);
        String subjTpl = firstNonBlank(s.getEmailVoidSubject(), DEFAULT_SUBJECT);
        String bodyTpl = firstNonBlank(s.getEmailVoidBodyTemplate(), DEFAULT_BODY);
        String subject = replacePlaceholders(subjTpl, t, s);
        String body = replacePlaceholders(bodyTpl, t, s);
        ledgerSmtpMailService.sendPlainText(s, to, subject, body);
    }

    private static String replacePlaceholders(String tpl, PgTrnsctn t, HqLedgerSysSettings s) {
        if (tpl == null) {
            return "";
        }
        String transNo = firstNonBlank(t.getChillTransactionId(), t.getTrnId());
        String orderNo = nz(t.getOrderNo());
        String amount = formatAmount(firstNonNullAmt(t));
        String routeNo = nz(t.getRouteNo());
        String paymentDate = t.getPaidAt() != null ? t.getPaidAt().format(DT)
                : (t.getCreatedAt() != null ? t.getCreatedAt().format(DT) : "");
        String mid = nz(t.getMerchantId());
        String company = firstNonBlank(s.getEmailVoidCompanyName(), "(company)");
        String contact = firstNonBlank(s.getEmailVoidContactName(), "(contact)");
        return tpl
                .replace("{{transNo}}", transNo)
                .replace("{{orderNo}}", orderNo)
                .replace("{{amount}}", amount)
                .replace("{{routeNo}}", routeNo)
                .replace("{{paymentDate}}", paymentDate)
                .replace("{{mid}}", mid)
                .replace("{{companyName}}", company)
                .replace("{{contactName}}", contact);
    }

    private static BigDecimal firstNonNullAmt(PgTrnsctn t) {
        if (t.getTotalAmt() != null) {
            return t.getTotalAmt();
        }
        if (t.getAmtKrw() != null) {
            return t.getAmtKrw();
        }
        if (t.getIcopayAmt() != null) {
            return t.getIcopayAmt();
        }
        return null;
    }

    private static String formatAmount(BigDecimal a) {
        if (a == null) {
            return "";
        }
        return a.stripTrailingZeros().toPlainString();
    }

    private static String nz(String v) {
        return v != null ? v.trim() : "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b != null ? b : "";
    }
}
