package com.pg.receipt;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.merchantdeploy.MerchantCheckoutLangUtil;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.LedgerSmtpMailService;
import com.pg.service.MailSendLogService;
import com.pg.splitpay.SplitPayMailLocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class TransactionReceiptEmailService {

    private static final Logger log = LoggerFactory.getLogger(TransactionReceiptEmailService.class);
    private static final String STATUS_PAID = "10";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s<>]+@[^@\\s<>]+\\.[^@\\s<>]+$");

    public static final String MAIL_KIND = "RECEIPT_TXN";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final TransactionReceiptEmailPolicyService policyService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final LedgerSmtpMailService ledgerSmtpMailService;
    private final MailSendLogService mailSendLogService;

    public TransactionReceiptEmailService(PgTrnsctnRepository pgTrnsctnRepository,
                                            OrgUnitRepository orgUnitRepository,
                                            MerchantProfileRepository merchantProfileRepository,
                                            PgAgencyRepository pgAgencyRepository,
                                            TransactionReceiptEmailPolicyService policyService,
                                            HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                            LedgerSmtpMailService ledgerSmtpMailService,
                                            MailSendLogService mailSendLogService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.policyService = policyService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.ledgerSmtpMailService = ledgerSmtpMailService;
        this.mailSendLogService = mailSendLogService;
    }

    /**
     * 전산설정 — 샘플 HTML 거래명세서 테스트 발송.
     * @param body toEmail(필수), lang, acquirer, paymentSwitcher, paymentProvider, merchant
     */
    public void sendTestReceipt(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("요청 본문이 없습니다.");
        }
        String to = str(body.get("toEmail"));
        if (to.isBlank()) {
            to = str(body.get("testRecipientEmail"));
        }
        if (to.isBlank() || !EMAIL.matcher(to).matches()) {
            throw new IllegalArgumentException("받는사람 이메일을 올바르게 입력하세요.");
        }
        String acquirer = str(body.get("acquirer"));
        String switcher = str(body.get("paymentSwitcher"));
        String provider = str(body.get("paymentProvider"));
        String merchant = str(body.get("merchant"));
        if (acquirer.isBlank() && switcher.isBlank() && provider.isBlank() && merchant.isBlank()) {
            throw new IllegalArgumentException("Acquirer·Payment Switcher·Payment Provider·Merchant 중 하나 이상 입력하세요.");
        }
        String lang = MerchantCheckoutLangUtil.normalize(str(body.get("lang")));
        if (lang.isEmpty()) {
            lang = MerchantCheckoutLangUtil.normalize(str(body.get("langCode")));
        }
        if (lang.isEmpty()) {
            lang = "ENG";
        }
        lang = SplitPayMailLocaleUtil.normalize(lang);

        String sampleEmail = to;
        TransactionReceiptHtmlRenderer.TransactionReceiptViewModel vm =
                TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                        .lang(lang)
                        .currency(nzOr(str(body.get("currency")), "JPY"))
                        .amount(parseAmount(body.get("amount"), "1000"))
                        .acquirer(acquirer)
                        .paymentSwitcher(switcher)
                        .paymentProvider(provider)
                        .merchant(merchant)
                        .transactionId("TEST-" + System.currentTimeMillis())
                        .customerEmail(sampleEmail)
                        .customerTel(nzOr(str(body.get("customerTel")), "+81-90-1234-5678"))
                        .serviceItem(nzOr(str(body.get("serviceItem")), "ICOPAY Test Receipt"))
                        .orderNumber(nzOr(str(body.get("orderNumber")), "ORD-TEST-001"))
                        .cardholder(nzOr(str(body.get("cardholder")), "TEST CARDHOLDER"))
                        .authorizedDateTime(java.time.LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .approvalCode("TESTOK")
                        .paymentMethod("Credit/Debit Card")
                        .build();
        String subject = "[TEST] " + TransactionReceiptEmailI18n.subject(lang);
        String html = TransactionReceiptHtmlRenderer.render(vm);
        String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
        var smtp = hqLedgerSysSettingsService.getOrCreate();
        try {
            ledgerSmtpMailService.sendHtml(smtp, to, subject, html, plain);
            mailSendLogService.append(MailSendLogService.KIND_RECEIPT_TEST, MailSendLogService.STATUS_SUCCESS,
                    to, subject, plain, null, null, null);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            mailSendLogService.append(MailSendLogService.KIND_RECEIPT_TEST, MailSendLogService.STATUS_FAIL,
                    to, subject, plain, msg, null, null);
            throw new IllegalStateException("테스트 메일 발송 실패: " + msg, ex);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String nzOr(String v, String fallback) {
        return v != null && !v.isBlank() ? v : fallback;
    }

    private static java.math.BigDecimal parseAmount(Object raw, String fallback) {
        try {
            if (raw != null && !String.valueOf(raw).isBlank()) {
                return new java.math.BigDecimal(String.valueOf(raw).trim());
            }
        } catch (Exception ignored) {
        }
        try {
            return new java.math.BigDecimal(fallback);
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    public void scheduleAfterPaid(PgTrnsctn txn) {
        if (txn == null || txn.getTrnId() == null || txn.getTrnId().isBlank()) {
            return;
        }
        if (!STATUS_PAID.equals(txn.getStatus())) {
            return;
        }
        final String trnId = txn.getTrnId().trim();
        Runnable job = () -> sendIfDue(trnId);
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

    private void sendIfDue(String trnId) {
        try {
            Optional<PgTrnsctn> opt = pgTrnsctnRepository.findById(trnId);
            if (opt.isEmpty()) {
                return;
            }
            PgTrnsctn t = opt.get();
            if (!STATUS_PAID.equals(t.getStatus())) {
                return;
            }
            if (t.getReceiptMailSentAt() != null) {
                return;
            }
            String merchantCode = t.getMerchantId();
            if (merchantCode == null || merchantCode.isBlank()) {
                return;
            }
            if (!policyService.isEnabledForMerchantCode(merchantCode)) {
                return;
            }
            String to = resolveCustomerEmail(t);
            if (to == null || to.isBlank()) {
                log.debug("거래명세서 스킵 — 고객 이메일 없음 trnId={}", trnId);
                return;
            }
            Optional<OrgUnit> merchantOu = orgUnitRepository.findByCode(merchantCode.trim());
            if (merchantOu.isEmpty()) {
                merchantOu = orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim());
            }
            if (merchantOu.isEmpty()) {
                return;
            }
            TransactionReceiptHtmlRenderer.TransactionReceiptViewModel vm =
                    buildViewModel(t, merchantOu.get());
            String lang = vm.lang();
            String subject = TransactionReceiptEmailI18n.subject(lang);
            String html = TransactionReceiptHtmlRenderer.render(vm);
            String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
            var smtp = hqLedgerSysSettingsService.getOrCreate();
            try {
                ledgerSmtpMailService.sendHtml(smtp, to, subject, html, plain);
                markSent(trnId);
                mailSendLogService.append(MAIL_KIND, MailSendLogService.STATUS_SUCCESS, to, subject, plain,
                        null, trnId, null);
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                mailSendLogService.append(MAIL_KIND, MailSendLogService.STATUS_FAIL, to, subject, plain,
                        msg, trnId, null);
                log.warn("거래명세서 메일 실패 trnId={}: {}", trnId, msg);
            }
        } catch (Exception e) {
            log.warn("거래명세서 처리 오류 trnId={}: {}", trnId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(String trnId) {
        pgTrnsctnRepository.findById(trnId).ifPresent(t -> {
            t.setReceiptMailSentAt(LocalDateTime.now());
            pgTrnsctnRepository.save(t);
        });
    }

    private TransactionReceiptHtmlRenderer.TransactionReceiptViewModel buildViewModel(PgTrnsctn t, OrgUnit merchantOu) {
        String lang = resolveLang(t);
        PgAgency agency = resolvePgAgency(t.getVan());
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(merchantOu.getId()).orElse(null);
        OrgUnit masterDist = policyService.findMasterDistForMerchantOrgUnitId(merchantOu.getId()).orElse(null);
        MerchantProfile mdProfile = masterDist != null
                ? merchantProfileRepository.findByOrgUnitId(masterDist.getId()).orElse(null) : null;

        String acquirer = agency != null
                ? TransactionReceiptContactBlock.of(agency.getAcquirerNm(), agency.getAcquirerTel(), agency.getAcquirerEmail()).displayLine()
                : "";
        String switcher = agency != null
                ? TransactionReceiptContactBlock.of(agency.getPaymentSwitcherNm(), agency.getPaymentSwitcherTel(),
                agency.getPaymentSwitcherEmail()).displayLine() : "";
        String provider = masterDist != null
                ? TransactionReceiptContactBlock.of(
                masterDist.getName(),
                mdProfile != null ? firstNonBlank(mdProfile.getContactTel(), mdProfile.getCompTel(), mdProfile.getCeoMobile()) : null,
                mdProfile != null ? mdProfile.getEmail() : null).displayLine() : "";
        String merchant = TransactionReceiptContactBlock.of(
                merchantOu.getName(),
                mp != null ? firstNonBlank(mp.getContactTel(), mp.getCompTel(), mp.getCeoMobile()) : null,
                mp != null ? mp.getEmail() : null).displayLine();

        String currency = t.getDisplayCurType() != null && !t.getDisplayCurType().isBlank()
                ? t.getDisplayCurType().trim().toUpperCase(Locale.ROOT)
                : (t.getCurType() != null ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "");
        var amount = t.getDisplayAmt() != null ? t.getDisplayAmt() : t.getAmtKrw();

        return TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                .lang(lang)
                .currency(currency)
                .amount(amount)
                .acquirer(acquirer)
                .paymentSwitcher(switcher)
                .paymentProvider(provider)
                .merchant(merchant)
                .transactionId(firstNonBlank(t.getChillTransactionId(), t.getTrnId()))
                .customerEmail(resolveCustomerEmail(t))
                .customerTel(t.getCustomerTel())
                .serviceItem(firstNonBlank(t.getOrderNo(), t.getPayNo()))
                .orderNumber(t.getOrderNo())
                .cardholder(firstNonBlank(t.getCustomerNm(), t.getCustomerId()))
                .authorizedDateTime(formatPaidAt(t.getPaidAt()))
                .approvalCode(t.getApprovalNo())
                .paymentMethod(resolvePaymentMethod(t))
                .build();
    }

    private String resolveLang(PgTrnsctn t) {
        String fromTxn = MerchantCheckoutLangUtil.normalize(t.getCheckoutLang());
        if (!fromTxn.isEmpty()) {
            return SplitPayMailLocaleUtil.normalize(fromTxn);
        }
        String cur = t.getCurType() != null ? t.getCurType() : t.getDisplayCurType();
        return TransactionReceiptEmailI18n.resolveLangFromCurrency(cur);
    }

    private PgAgency resolvePgAgency(String van) {
        if (van == null || van.isBlank()) {
            return null;
        }
        String v = van.trim().toUpperCase(Locale.ROOT);
        Optional<PgAgency> direct = pgAgencyRepository.findByPgCd(v);
        if (direct.isPresent()) {
            return direct.get();
        }
        return pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(a -> a.getPgCd() != null && v.startsWith(a.getPgCd().trim().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseGet(() -> pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                        .filter(a -> matchesVanFamily(v, a.getPgCd()))
                        .findFirst()
                        .orElse(null));
    }

    private static boolean matchesVanFamily(String van, String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        String p = pgCd.trim().toUpperCase(Locale.ROOT);
        if (PgVendor.isJpayFamily(p) && PgVendor.isJpayFamily(van)) {
            return true;
        }
        if (PgVendor.isChillPayVendorCode(p) && PgVendor.isChillPayVendorCode(van)) {
            return true;
        }
        if (PgVendor.isEximbayFamily(p) && PgVendor.isEximbayFamily(van)) {
            return true;
        }
        return PgVendor.isElementPayFamily(p) && PgVendor.isElementPayFamily(van);
    }

    private static String resolveCustomerEmail(PgTrnsctn t) {
        if (t == null) {
            return null;
        }
        String cid = t.getCustomerId();
        if (cid != null && looksLikeEmail(cid)) {
            return cid.trim();
        }
        return null;
    }

    private static boolean looksLikeEmail(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        return EMAIL.matcher(s.trim()).matches();
    }

    private static String resolvePaymentMethod(PgTrnsctn t) {
        if (t.getPaymentChannel() != null && !t.getPaymentChannel().isBlank()) {
            return t.getPaymentChannel().trim();
        }
        return "Credit/Debit Card";
    }

    private static String formatPaidAt(LocalDateTime paidAt) {
        if (paidAt == null) {
            return "";
        }
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        return paidAt.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
