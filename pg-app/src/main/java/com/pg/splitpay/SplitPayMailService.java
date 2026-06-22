package com.pg.splitpay;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.MailSendLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SplitPayMailService {

    private static final Logger log = LoggerFactory.getLogger(SplitPayMailService.class);

    public static final String KIND_D_MINUS1 = MailSendLogService.KIND_SPLIT_PAY_D_MINUS1;
    public static final String KIND_D0 = MailSendLogService.KIND_SPLIT_PAY_D0;
    public static final String KIND_D1 = MailSendLogService.KIND_SPLIT_PAY_D1;
    public static final String KIND_D2 = MailSendLogService.KIND_SPLIT_PAY_D2;
    public static final String KIND_D3 = MailSendLogService.KIND_SPLIT_PAY_D3;
    public static final String KIND_CREATE = MailSendLogService.KIND_SPLIT_PAY_CREATE;

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final SplitPayEmailSettingsService emailSettingsService;
    private final MailSendLogService mailSendLogService;

    public SplitPayMailService(HqLedgerSysSettingsService hqLedgerSysSettingsService,
                               SplitPayEmailSettingsService emailSettingsService,
                               MailSendLogService mailSendLogService) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.emailSettingsService = emailSettingsService;
        this.mailSendLogService = mailSendLogService;
    }

    public void sendInstallmentLink(SplitPayContract contract, SplitPayInstallment inst, String siteBase, String phase) {
        if (contract == null || inst == null || contract.getCustomerEmail() == null || contract.getCustomerEmail().isBlank()) {
            return;
        }
        String kind = mapPhaseToKind(phase);
        String payUrl = trimSlash(siteBase) + "/split-pay.html?token=" + inst.getPayToken();
        try {
            HqLedgerSysSettings smtp = hqLedgerSysSettingsService.getOrCreate();
            SplitPayEmailSettingsService.ResolvedMail mail =
                    emailSettingsService.resolveMail(phase, contract, inst, payUrl);
            emailSettingsService.deliverMail(smtp, mail, mail.phaseConfig(),
                    contract.getCustomerEmail().trim(), kind, inst.getOrderNo(), "split-pay-scheduler");
        } catch (Exception e) {
            log.warn("분할결제 메일 실패 contract={} inst={}: {}", contract.getContractNo(), inst.getInstallmentNo(), e.getMessage());
            try {
                String fallbackSub = SplitPayMailI18n.subject(resolveLocale(contract), contract.getContractNo(), inst.getInstallmentNo());
                String fallbackBody = SplitPayMailI18n.body(resolveLocale(contract), contract, inst, payUrl);
                mailSendLogService.append(kind, MailSendLogService.STATUS_FAIL,
                        contract.getCustomerEmail(), fallbackSub, fallbackBody, e.getMessage(), inst.getOrderNo(), "split-pay-scheduler");
            } catch (Exception ignored) {
            }
        }
    }

    private static String resolveLocale(SplitPayContract contract) {
        return SplitPayMailLocaleUtil.normalize(
                contract != null ? contract.getCustomerLocale() : SplitPayMailLocaleUtil.KOR);
    }

    private static String mapPhaseToKind(String phase) {
        if (phase == null) {
            return KIND_D0;
        }
        return switch (phase.toUpperCase(Locale.ROOT)) {
            case "D_MINUS1", "DM1" -> KIND_D_MINUS1;
            case "D1" -> KIND_D1;
            case "D2" -> KIND_D2;
            case "D3" -> KIND_D3;
            case "CREATE" -> KIND_CREATE;
            default -> KIND_D0;
        };
    }

    private static String trimSlash(String base) {
        return base != null ? base.trim().replaceAll("/+$", "") : "";
    }
}
