package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.util.JpayNotifyStatusResolver;
import com.pg.util.TxnOutcomeReasonApplier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * 결제내역 그리드 후속조치 ({@code tb_hq_notify_env_config} 자동무효·이메일무효·자동환불·강제환불 — 본사설정 전산설정관리에서 편집).
 * <p>
 * ChillPay: 자동무효·환불·강제환불은 Transaction API 호출 후 내부 상태 갱신.
 * JPAY: 자동환불·강제환불은 {@code /pay/trade/refund} API 호출. 무효는 수동무효·포털 처리.
 * ElementPay: 자동환불·강제환불은 {@code /merchant/initRefund}. 자동무효(voidPayment)는 2단계 결제 전용이라 미지원.
 * Eximbay: 자동환불·강제환불은 {@code /v1/payments/{transaction_id}/cancel}. 자동무효는 미지원.
 */
@Service
public class PayListActionService {

    public enum PayFollowAction {
        AUTO_VOID,
        EMAIL_VOID,
        AUTO_REFUND,
        FORCE_REFUND,
        /** JPAY 전용 — JPAY 포털 무효 승인 후 ICOPAY 수동 반영 */
        MANUAL_VOID,
        /** JPAY 전용 — JPAY 포털 환불 승인 후 ICOPAY 수동 반영 */
        MANUAL_REFUND
    }

    private final PayFollowPolicyService payFollowPolicyService;
    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ChillPayService chillPayService;
    private final PayFollowEmailVoidService payFollowEmailVoidService;
    private final SettlementArrearsService settlementArrearsService;
    private final JpayManualFollowUpNotifyService jpayManualFollowUpNotifyService;
    private final JpayTradeApiService jpayTradeApiService;
    private final ElementPayPaymentService elementPayPaymentService;
    private final EximbayPaymentService eximbayPaymentService;
    private final OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator;

    public PayListActionService(PayFollowPolicyService payFollowPolicyService,
                                PgTrnsctnRepository trnsctnRepository,
                                OrgUnitRepository orgUnitRepository,
                                ChillPayService chillPayService,
                                PayFollowEmailVoidService payFollowEmailVoidService,
                                SettlementArrearsService settlementArrearsService,
                                JpayManualFollowUpNotifyService jpayManualFollowUpNotifyService,
                                JpayTradeApiService jpayTradeApiService,
                                ElementPayPaymentService elementPayPaymentService,
                                EximbayPaymentService eximbayPaymentService,
                                OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator) {
        this.payFollowPolicyService = payFollowPolicyService;
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.chillPayService = chillPayService;
        this.payFollowEmailVoidService = payFollowEmailVoidService;
        this.settlementArrearsService = settlementArrearsService;
        this.jpayManualFollowUpNotifyService = jpayManualFollowUpNotifyService;
        this.jpayTradeApiService = jpayTradeApiService;
        this.elementPayPaymentService = elementPayPaymentService;
        this.eximbayPaymentService = eximbayPaymentService;
        this.outcomeReasonWarmCoordinator = outcomeReasonWarmCoordinator;
    }

    @Transactional
    public void apply(Authentication authentication, String trnId, String actionRaw) {
        apply(authentication, trnId, actionRaw, null);
    }

    @Transactional
    public void apply(Authentication authentication, String trnId, String actionRaw, String adminReason) {
        if (trnId == null || trnId.isBlank()) {
            throw new IllegalArgumentException("거래번호(trnId)가 필요합니다.");
        }
        if (actionRaw == null || actionRaw.isBlank()) {
            throw new IllegalArgumentException("action이 필요합니다.");
        }
        PayFollowAction action;
        try {
            action = PayFollowAction.valueOf(actionRaw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 action입니다: " + actionRaw);
        }
        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        payFollowPolicyService.assertMayExecute(user, trnId, action);
        PgTrnsctn t = trnsctnRepository.findById(trnId.trim())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        boolean jpay = PgVendor.isJpayFamily(t.getVan());
        boolean elementPay = PgVendor.isElementPayFamily(t.getVan());
        boolean eximbay = PgVendor.isEximbayFamily(t.getVan());
        if (jpay && (action == PayFollowAction.AUTO_VOID || action == PayFollowAction.EMAIL_VOID)) {
            throw new IllegalStateException(
                    "JPAY 거래는 자동무효·이메일무효를 사용할 수 없습니다. 수동무효 또는 JPAY 포털에서 처리하세요.");
        }
        if (elementPay && action == PayFollowAction.AUTO_VOID) {
            throw new IllegalStateException(
                    "ElementPay 거래는 자동무효를 지원하지 않습니다. 승인 완료 건은 자동환불·강제환불을 사용하세요.");
        }
        if (eximbay && action == PayFollowAction.AUTO_VOID) {
            throw new IllegalStateException(
                    "해당 거래는 자동무효를 지원하지 않습니다. 승인 완료 건은 자동환불·강제환불을 사용하세요.");
        }
        if (!jpay && (action == PayFollowAction.MANUAL_VOID || action == PayFollowAction.MANUAL_REFUND)) {
            throw new IllegalStateException("수동무효·수동환불은 JPAY 거래만 지원합니다.");
        }

        String prevStatus = t.getStatus();
        String prevSettledYn = t.getSettledYn();
        String actor = user != null ? user.getUsername() : null;
        String apiDetail = null;

        switch (action) {
            case EMAIL_VOID -> payFollowEmailVoidService.sendVoidRequestMail(t, actor);
            case AUTO_VOID -> {
                long ouId = resolveMerchantOrgUnitId(t);
                long chillTxn = parseChillPayTransactionId(t);
                requireChillPayVan(t);
                apiDetail = chillPayService.requestChillPayVoid(ouId, chillTxn);
            }
            case AUTO_REFUND, FORCE_REFUND -> {
                String refundReason = adminReason != null && !adminReason.isBlank()
                        ? adminReason.trim()
                        : (action == PayFollowAction.FORCE_REFUND ? "icopay force refund" : "icopay refund");
                if (jpay) {
                    apiDetail = jpayTradeApiService.requestRefund(t, null, refundReason);
                } else if (elementPay) {
                    apiDetail = elementPayPaymentService.requestRefund(t, null, refundReason);
                } else if (eximbay) {
                    apiDetail = eximbayPaymentService.requestCancel(t, refundReason);
                } else {
                    long ouId = resolveMerchantOrgUnitId(t);
                    long chillTxn = parseChillPayTransactionId(t);
                    requireChillPayVan(t);
                    apiDetail = chillPayService.requestChillPayRefund(ouId, chillTxn);
                }
            }
            case MANUAL_VOID, MANUAL_REFUND -> requireJpayVan(t);
        }

        String nextStatus = switch (action) {
            case AUTO_VOID -> "40";
            case EMAIL_VOID -> "41";
            case AUTO_REFUND -> "42";
            case FORCE_REFUND -> "31";
            case MANUAL_VOID -> "21";
            case MANUAL_REFUND -> "30";
        };
        t.setStatus(nextStatus);
        if (jpay) {
            String rc = switch (action) {
                case MANUAL_REFUND, AUTO_REFUND, FORCE_REFUND -> "09";
                case MANUAL_VOID -> "08";
                default -> "00";
            };
            t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(nextStatus, rc));
            t.setPaidAt(null);
        } else if ((elementPay || eximbay) && (action == PayFollowAction.AUTO_REFUND || action == PayFollowAction.FORCE_REFUND)) {
            t.setPaidAt(null);
            if (apiDetail != null && !apiDetail.isBlank()) {
                String label = apiDetail.length() > 50 ? apiDetail.substring(0, 50) : apiDetail;
                t.setChillPaymentStatus(label);
            }
        }
        Optional<String> recordedReason = TxnOutcomeReasonApplier.applyIcopayFollowUp(t, prevStatus, nextStatus, action.name(), actor, adminReason, apiDetail);
        trnsctnRepository.save(t);
        outcomeReasonWarmCoordinator.onRecorded(recordedReason);

        try {
            settlementArrearsService.registerPostSettlementRecoveryIfDue(prevStatus, prevSettledYn, t);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(PayListActionService.class)
                    .warn("환수금 자동등록 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }

        if (action == PayFollowAction.MANUAL_VOID || action == PayFollowAction.MANUAL_REFUND) {
            jpayManualFollowUpNotifyService.sendAfterManualFollowUp(t, action, actor);
        }
    }

    private static void requireChillPayVan(PgTrnsctn t) {
        String v = t.getVan();
        if (v == null || !PgVendor.CHILLPAY.equalsIgnoreCase(v.trim())) {
            throw new IllegalStateException("ChillPay 거래만 API 무효·환불을 호출할 수 있습니다.");
        }
    }

    private static void requireJpayVan(PgTrnsctn t) {
        if (!PgVendor.isJpayFamily(t.getVan())) {
            throw new IllegalStateException("JPAY 거래만 수동무효·수동환불을 사용할 수 있습니다.");
        }
    }

    private long resolveMerchantOrgUnitId(PgTrnsctn t) {
        String code = t.getMerchantId();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("거래에 가맹점 코드가 없습니다.");
        }
        OrgUnit ou = orgUnitRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalStateException("가맹점(조직) 코드를 찾을 수 없습니다: " + code.trim()));
        return ou.getId();
    }

    private static long parseChillPayTransactionId(PgTrnsctn t) {
        String s = t.getChillTransactionId();
        if (s == null || s.isBlank()) {
            throw new IllegalStateException("ChillPay TransactionId가 없어 API 후속조치를 호출할 수 없습니다.");
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("ChillPay TransactionId 형식이 올바르지 않습니다: " + s);
        }
    }
}
