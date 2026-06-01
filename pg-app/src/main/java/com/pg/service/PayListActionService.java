package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 결제내역 그리드 후속조치 ({@code tb_hq_notify_env_config} 자동무효·이메일무효·자동환불·강제환불 — 본사설정 전산설정관리에서 편집).
 * <p>
 * 자동무효·환불·강제환불은 ChillPay Transaction API(void/refund request)를 호출한 뒤 성공 시 내부 상태를 갱신하고,
 * 이메일무효는 전산설정 SMTP로 템플릿 메일을 발송한 뒤 상태를 갱신합니다.
 */
@Service
public class PayListActionService {

    public enum PayFollowAction {
        AUTO_VOID,
        EMAIL_VOID,
        AUTO_REFUND,
        FORCE_REFUND
    }

    private final PayFollowPolicyService payFollowPolicyService;
    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ChillPayService chillPayService;
    private final PayFollowEmailVoidService payFollowEmailVoidService;

    public PayListActionService(PayFollowPolicyService payFollowPolicyService,
                                PgTrnsctnRepository trnsctnRepository,
                                OrgUnitRepository orgUnitRepository,
                                ChillPayService chillPayService,
                                PayFollowEmailVoidService payFollowEmailVoidService) {
        this.payFollowPolicyService = payFollowPolicyService;
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.chillPayService = chillPayService;
        this.payFollowEmailVoidService = payFollowEmailVoidService;
    }

    @Transactional
    public void apply(Authentication authentication, String trnId, String actionRaw) {
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
        if (PayFollowPolicyService.isPayFollowHiddenForTransaction(t)) {
            throw new IllegalStateException(
                    "JPAY 거래는 결제 후속조치(무효·환불)를 지원하지 않습니다. PG 운영 처리 및 노티 반영으로 확인하세요.");
        }

        switch (action) {
            case EMAIL_VOID -> payFollowEmailVoidService.sendVoidRequestMail(t,
                    user != null ? user.getUsername() : null);
            case AUTO_VOID -> {
                long ouId = resolveMerchantOrgUnitId(t);
                long chillTxn = parseChillPayTransactionId(t);
                requireChillPayVan(t);
                chillPayService.requestChillPayVoid(ouId, chillTxn);
            }
            case AUTO_REFUND, FORCE_REFUND -> {
                long ouId = resolveMerchantOrgUnitId(t);
                long chillTxn = parseChillPayTransactionId(t);
                requireChillPayVan(t);
                chillPayService.requestChillPayRefund(ouId, chillTxn);
            }
        }

        t.setStatus(switch (action) {
            case AUTO_VOID -> "40";
            case EMAIL_VOID -> "41";
            case AUTO_REFUND -> "42";
            case FORCE_REFUND -> "31";
        });
        trnsctnRepository.save(t);
    }

    private static void requireChillPayVan(PgTrnsctn t) {
        String v = t.getVan();
        if (v == null || !PgVendor.CHILLPAY.equalsIgnoreCase(v.trim())) {
            throw new IllegalStateException("ChillPay 거래만 API 무효·환불을 호출할 수 있습니다.");
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
