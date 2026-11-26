package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.splitpay.SplitPayPaymentHookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * KSNET 서버 승인 후 공통 후처리(분할결제·실시간 자동정산·가맹 아웃바운드 통보).
 * (인터페이스 없는 구체 빈 — 트랜잭션 프록시 타입 충돌 방지를 위해 ingress 핸들러와 분리)
 */
@Service
public class KsnetPostApproveService {

    private static final Logger log = LoggerFactory.getLogger(KsnetPostApproveService.class);

    private final SettlementCalcService settlementCalcService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final KsnetNotiMirrorService ksnetNotiMirrorService;
    private final KsnetCardEnrichService ksnetCardEnrichService;

    public KsnetPostApproveService(SettlementCalcService settlementCalcService,
                                   SplitPayPaymentHookService splitPayPaymentHookService,
                                   MerchantOutboundNotifyService merchantOutboundNotifyService,
                                   KsnetNotiMirrorService ksnetNotiMirrorService,
                                   KsnetCardEnrichService ksnetCardEnrichService) {
        this.settlementCalcService = settlementCalcService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.ksnetNotiMirrorService = ksnetNotiMirrorService;
        this.ksnetCardEnrichService = ksnetCardEnrichService;
    }

    public void afterApproved(PgTrnsctn t, String channel) {
        if (t == null) {
            return;
        }
        if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
            try {
                splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
            } catch (Exception e) {
                log.warn("KSNET 분할결제 연동 실패 orderNo={}: {}", t.getOrderNo(), e.getMessage());
            }
        }
        boolean paid = "10".equals(t.getStatus() != null ? t.getStatus().trim() : "");
        if (paid) {
            try {
                if (t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                    settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                }
            } catch (Exception e) {
                log.warn("KSNET 실시간 자동정산 트리거 실패 trnId={}: {}", t.getTrnId(), e.getMessage());
            }
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null,
                    channel != null && !channel.isBlank() ? channel : "KSNET_APPROVE");
        } catch (Exception e) {
            log.warn("KSNET 가맹 아웃바운드 노티 예약 실패 orderNo={}: {}", t.getOrderNo(), e.getMessage());
        }
        try {
            ksnetNotiMirrorService.scheduleAfterCommit(t, paid);
        } catch (Exception e) {
            log.warn("KSNET NOTI 미러 예약 실패 orderNo={}: {}", t.getOrderNo(), e.getMessage());
        }
        if (paid) {
            try {
                ksnetCardEnrichService.scheduleAfterCommit(t);
            } catch (Exception e) {
                log.warn("KSNET 카드 보강 예약 실패 orderNo={}: {}", t.getOrderNo(), e.getMessage());
            }
        }
    }
}
