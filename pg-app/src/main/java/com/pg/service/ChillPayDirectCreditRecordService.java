package com.pg.service;

import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * ChillPay DirectCredit 동기 응답을 {@link PgTrnsctn}에 반영해 결제내역·정산 파이프라인과 맞춥니다.
 */
@Service
public class ChillPayDirectCreditRecordService {

    private static final Logger log = LoggerFactory.getLogger(ChillPayDirectCreditRecordService.class);

    /** 승인 완료 — 정산 후보 */
    private static final String STATUS_PAID = "10";
    /** OTP·추가 인증 대기 — 아직 매출 확정 아님 */
    private static final String STATUS_AUTH_PENDING = "08";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public ChillPayDirectCreditRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                            OrgUnitRepository orgUnitRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    /**
     * ChillPay 본문 status=200 이고 data.paymentStatus 가 Paid 또는 WaitAuthorize 일 때만 행을 생성합니다.
     * 저장 실패는 로그만 남기고 호출부 결제 응답에는 영향을 주지 않습니다.
     */
    @Transactional
    public void recordAfterDirectCreditResponse(Long merchantOrgUnitId,
                                                ChillPayDirectCreditResponse res,
                                                long requestAmount,
                                                String requestOrderNo,
                                                String requestCustomerId,
                                                int routeNo) {
        try {
            doRecord(merchantOrgUnitId, res, requestAmount, requestOrderNo, requestCustomerId, routeNo);
        } catch (Exception e) {
            log.warn("DirectCredit 거래 적재 실패 (결제 API 응답은 유지): {}", e.getMessage());
        }
    }

    private void doRecord(Long merchantOrgUnitId,
                          ChillPayDirectCreditResponse res,
                          long requestAmount,
                          String requestOrderNo,
                          String requestCustomerId,
                          int routeNo) {
        if (res == null || res.getStatus() != 200 || res.getData() == null) {
            return;
        }
        ChillPayDirectCreditResponse.Data d = res.getData();
        String ps = d.getPaymentStatus();
        if (ps == null || ps.isBlank()) {
            return;
        }
        String psl = ps.trim();
        boolean paid = "Paid".equalsIgnoreCase(psl);
        boolean waitAuth = "WaitAuthorize".equalsIgnoreCase(psl);
        if (!paid && !waitAuth) {
            return;
        }

        String merchantId = resolveMerchantId(merchantOrgUnitId);
        String orderNo = firstNonBlank(d.getOrderNo(), requestOrderNo);
        if (orderNo == null || orderNo.isBlank()) {
            if (d.getTransactionId() != null) {
                orderNo = "CP" + d.getTransactionId();
            } else {
                orderNo = "ORD" + System.currentTimeMillis();
            }
        }
        if (orderNo.length() > 64) {
            orderNo = orderNo.substring(0, 64);
        }
        String payNo = orderNo.length() > 50 ? orderNo.substring(0, 50) : orderNo;
        String customerId = firstNonBlank(d.getCustomerId(), requestCustomerId);
        if (customerId == null) {
            customerId = "guest";
        }

        long amountVal = d.getAmount() != null ? d.getAmount() : requestAmount;
        if (amountVal <= 0) {
            return;
        }

        PgTrnsctn t = new PgTrnsctn();
        t.setTrnId(newTrnId());
        t.setMerchantId(merchantId);
        t.setServiceType("API");
        t.setStatus(paid ? STATUS_PAID : STATUS_AUTH_PENDING);
        t.setCurType("JPY");
        t.setAmtKrw(BigDecimal.valueOf(amountVal));
        t.setPayNo(payNo);
        t.setOrderNo(orderNo);
        t.setCustomerId(customerId);
        t.setVan("CHILLPAY");
        t.setOrigin("URL");
        t.setChillPaymentStatus(psl);
        t.setRouteNo(String.valueOf(routeNo));
        if (d.getTransactionId() != null) {
            t.setChillTransactionId(String.valueOf(d.getTransactionId()));
        }
        if (d.getChannelCode() != null && !d.getChannelCode().isBlank()) {
            t.setPaymentChannel(d.getChannelCode().trim());
        }
        if (d.getFee() != null) {
            t.setChillFeeAmt(BigDecimal.valueOf(d.getFee()));
        }
        if (d.getTotalAmount() != null) {
            t.setTotalAmt(BigDecimal.valueOf(d.getTotalAmount()));
        } else {
            t.setTotalAmt(BigDecimal.valueOf(amountVal));
        }
        if (d.getIcopay() != null) {
            t.setIcopayAmt(BigDecimal.valueOf(d.getIcopay()));
        }
        if (paid) {
            t.setPaidAt(LocalDateTime.now());
        }
        t.setSettledYn("N");
        pgTrnsctnRepository.save(t);
        log.info("DirectCredit 거래 적재 trnId={} merchantId={} orderNo={} status={}", t.getTrnId(), merchantId, orderNo, t.getStatus());
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private String resolveMerchantId(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return "UNKNOWN";
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(merchantOrgUnitId);
        if (ou.isEmpty()) {
            return "UNKNOWN";
        }
        String code = ou.get().getCode();
        return (code != null && !code.isBlank()) ? code.trim() : "UNKNOWN";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }
}
