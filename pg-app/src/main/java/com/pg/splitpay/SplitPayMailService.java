package com.pg.splitpay;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.LedgerSmtpMailService;
import com.pg.service.MailSendLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
public class SplitPayMailService {

    private static final Logger log = LoggerFactory.getLogger(SplitPayMailService.class);

    public static final String KIND_D_MINUS1 = "SPLIT_PAY_D_MINUS1";
    public static final String KIND_D0 = "SPLIT_PAY_D0";
    public static final String KIND_D1 = "SPLIT_PAY_D1";
    public static final String KIND_D2 = "SPLIT_PAY_D2";
    public static final String KIND_CREATE = "SPLIT_PAY_CREATE";

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final MailSendLogService mailSendLogService;

    public SplitPayMailService(HqLedgerSysSettingsService hqLedgerSysSettingsService,
                               LedgerSmtpMailService ledgerSmtpMailService,
                               MailSendLogService mailSendLogService) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.mailSendLogService = mailSendLogService;
    }

    public void sendInstallmentLink(SplitPayContract contract, SplitPayInstallment inst, String siteBase, String phase) {
        if (contract == null || inst == null || contract.getCustomerEmail() == null || contract.getCustomerEmail().isBlank()) {
            return;
        }
        String kind = mapPhaseToKind(phase);
        String payUrl = trimSlash(siteBase) + "/split-pay.html?token=" + inst.getPayToken();
        String lang = "KOR";
        String subject = SplitPayMailI18n.subject(lang, contract.getContractNo(), inst.getInstallmentNo());
        String body = SplitPayMailI18n.body(lang, contract, inst, payUrl);
        try {
            HqLedgerSysSettings smtp = hqLedgerSysSettingsService.getOrCreate();
            ledgerSmtpMailService.sendPlainText(smtp, contract.getCustomerEmail().trim(), subject, body);
            mailSendLogService.append(kind, MailSendLogService.STATUS_SUCCESS,
                    contract.getCustomerEmail(), subject, body, null, inst.getOrderNo(), "split-pay-scheduler");
        } catch (Exception e) {
            log.warn("분할결제 메일 실패 contract={} inst={}: {}", contract.getContractNo(), inst.getInstallmentNo(), e.getMessage());
            mailSendLogService.append(kind, MailSendLogService.STATUS_FAIL,
                    contract.getCustomerEmail(), subject, body, e.getMessage(), inst.getOrderNo(), "split-pay-scheduler");
        }
    }

    private static String mapPhaseToKind(String phase) {
        if (phase == null) {
            return KIND_D0;
        }
        return switch (phase.toUpperCase(Locale.ROOT)) {
            case "D_MINUS1", "DM1" -> KIND_D_MINUS1;
            case "D1" -> KIND_D1;
            case "D2" -> KIND_D2;
            case "CREATE" -> KIND_CREATE;
            default -> KIND_D0;
        };
    }

    private static String trimSlash(String base) {
        return base != null ? base.trim().replaceAll("/+$", "") : "";
    }
}
