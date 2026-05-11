package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * URL·챗봇 인라인 DirectCredit 승인({@link com.pg.service.ChillPayDirectCreditRecordService}) 직후
 * 미들웨어 JSON 외 추가 알림 — LINE Notify, 가맹점 대표 이메일(전산 SMTP).
 */
@Service
public class UrlPaySuccessAlertService {

    private static final Logger log = LoggerFactory.getLogger(UrlPaySuccessAlertService.class);

    /** ChillPay Paid — 정산·알림용 */
    private static final String STATUS_PAID = "10";

    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final LineNotifyClient lineNotifyClient;

    public UrlPaySuccessAlertService(MerchantOutboundNotifyService merchantOutboundNotifyService,
                                    MerchantProfileRepository merchantProfileRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                    LedgerSmtpMailService ledgerSmtpMailService,
                                    LineNotifyClient lineNotifyClient) {
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.lineNotifyClient = lineNotifyClient;
    }

    /**
     * DirectCredit 레코드 저장 직후(동일 트랜잭션): 커밋 후 미들웨어 JSON + 선택 알림 실행.
     */
    public void scheduleAfterDirectCreditSave(PgTrnsctn t) {
        if (t == null || t.getTrnId() == null || t.getTrnId().isBlank()) {
            return;
        }
        String origin = t.getOrigin() != null ? t.getOrigin().trim().toUpperCase() : "";
        if (!"URL".equals(origin) && !"CHATBOT".equals(origin)) {
            return;
        }
        final PgTrnsctn txn = t;
        Runnable job = () -> runAfterCommitJobs(txn);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    private void runAfterCommitJobs(PgTrnsctn txn) {
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(txn, null, "DIRECT_CREDIT");
        } catch (Exception e) {
            log.warn("DirectCredit 후 미들웨어 알림 예약 실패 trnId={}: {}", txn.getTrnId(), e.getMessage());
        }
        if (!STATUS_PAID.equals(txn.getStatus())) {
            return;
        }
        try {
            sendLineNotifyIfConfigured(txn);
        } catch (Exception e) {
            log.warn("LINE Notify 실패 trnId={}: {}", txn.getTrnId(), e.getMessage());
        }
        try {
            sendMerchantEmailIfConfigured(txn);
        } catch (Exception e) {
            log.warn("URL결제 승인 알림메일 실패 trnId={}: {}", txn.getTrnId(), e.getMessage());
        }
    }

    private OptionalMerchantProfileCtx resolveMerchant(String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return null;
        }
        OrgUnit ou = orgUnitRepository.findByCode(merchantCode.trim())
                .or(() -> orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim()))
                .orElse(null);
        if (ou == null) {
            return null;
        }
        return merchantProfileRepository.findByOrgUnitId(ou.getId())
                .map(mp -> new OptionalMerchantProfileCtx(ou.getCode(), mp))
                .orElse(null);
    }

    private record OptionalMerchantProfileCtx(String compId, MerchantProfile profile) {}

    private void sendLineNotifyIfConfigured(PgTrnsctn t) {
        OptionalMerchantProfileCtx ctx = resolveMerchant(t.getMerchantId());
        if (ctx == null) {
            return;
        }
        String token = ctx.profile().getUrlPayLineNotifyToken();
        if (token == null || token.isBlank()) {
            return;
        }
        String msg = buildAlertText(t, ctx.compId());
        try {
            lineNotifyClient.postNotify(token, msg);
        } catch (Exception e) {
            log.warn("LINE Notify HTTP 실패 merchant={}: {}", ctx.compId(), e.getMessage());
        }
    }

    private void sendMerchantEmailIfConfigured(PgTrnsctn t) {
        OptionalMerchantProfileCtx ctx = resolveMerchant(t.getMerchantId());
        if (ctx == null) {
            return;
        }
        if (!"Y".equalsIgnoreCase(ctx.profile().getUrlPayAlertEmailYn() != null ? ctx.profile().getUrlPayAlertEmailYn().trim() : "")) {
            return;
        }
        String to = ctx.profile().getEmail();
        if (to == null || to.isBlank()) {
            log.warn("url_pay_alert_email_yn=Y 이지만 대표 이메일 없음 merchant={}", ctx.compId());
            return;
        }
        HqLedgerSysSettings smtp = hqLedgerSysSettingsService.getOrCreate();
        String subject = "[ICOPAY] URL결제 승인 " + ctx.compId()
                + (t.getOrderNo() != null ? " #" + nz(t.getOrderNo()) : "");
        String body = buildAlertText(t, ctx.compId());
        ledgerSmtpMailService.sendPlainText(smtp, to.trim(), subject, body);
    }

    private static String buildAlertText(PgTrnsctn t, String compId) {
        StringBuilder sb = new StringBuilder();
        sb.append("[URL/챗봇 결제 승인]\n");
        sb.append("가맹점: ").append(nz(compId)).append('\n');
        sb.append("출처(origin): ").append(nz(t.getOrigin())).append('\n');
        sb.append("주문번호: ").append(nz(t.getOrderNo())).append('\n');
        sb.append("전산 거래번호: ").append(nz(t.getTrnId())).append('\n');
        if (t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
            sb.append("ChillTxn: ").append(nz(t.getChillTransactionId())).append('\n');
        }
        sb.append("금액: ").append(amtPlain(t.getAmtKrw())).append(' ')
                .append(nz(t.getCurType())).append('\n');
        if (t.getCustomerNm() != null && !t.getCustomerNm().isBlank()) {
            sb.append("결제자표시명: ").append(t.getCustomerNm().trim()).append('\n');
        }
        if (t.getPaidAt() != null) {
            sb.append("결제일시: ").append(t.getPaidAt().toString()).append('\n');
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String amtPlain(BigDecimal a) {
        if (a == null) {
            return "0";
        }
        return a.stripTrailingZeros().toPlainString();
    }
}
