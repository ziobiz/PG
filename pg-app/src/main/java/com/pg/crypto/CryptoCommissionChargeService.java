package com.pg.crypto;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.CryptoDepositTxn;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.CryptoDepositTxnRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.CommissionService;
import com.pg.util.CommissionFeeChannel;
import com.pg.util.PercentDecimalHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * 크립토 확정 입금 시 CRYPTO 수수료 과금(=카드 승인 성공과 동일 취급).
 * 결제내역({@link PgTrnsctn} status=10) 적재 + deposit.fee_amt 스냅샷.
 */
@Service
public class CryptoCommissionChargeService {

    private static final Logger log = LoggerFactory.getLogger(CryptoCommissionChargeService.class);
    public static final String ORIGIN_CRYPTO = "CRYPTO";

    private final CommissionService commissionService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final CryptoDepositTxnRepository depositTxnRepository;

    public CryptoCommissionChargeService(CommissionService commissionService,
                                         PgTrnsctnRepository pgTrnsctnRepository,
                                         CryptoDepositTxnRepository depositTxnRepository) {
        this.commissionService = commissionService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.depositTxnRepository = depositTxnRepository;
    }

    /**
     * 확정 입금 후 1회 과금. 이미 fee_charged_yn=Y 이면 스킵.
     * @return 산정된 수수료(없으면 0)
     */
    @Transactional
    public BigDecimal chargeOnConfirmedDeposit(CryptoDepositTxn deposit) {
        if (deposit == null || deposit.getId() == null) {
            return BigDecimal.ZERO;
        }
        if ("Y".equalsIgnoreCase(deposit.getFeeChargedYn())) {
            return deposit.getFeeAmt() != null ? deposit.getFeeAmt() : BigDecimal.ZERO;
        }
        String mid = deposit.getCompId() != null ? deposit.getCompId().trim() : "";
        BigDecimal amount = deposit.getAmount() != null ? deposit.getAmount() : BigDecimal.ZERO;
        CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(mid, CommissionFeeChannel.CRYPTO);
        BigDecimal fee = computeCryptoFee(amount, pol);

        String trnId = deposit.getPayTrnId();
        if (trnId == null || trnId.isBlank()) {
            trnId = newTrnId();
            PgTrnsctn t = new PgTrnsctn();
            t.setTrnId(trnId);
            t.setMerchantId(mid.length() > 20 ? mid.substring(0, 20) : mid);
            t.setServiceType("CRYPTO");
            t.setStatus("10");
            t.setOrigin(ORIGIN_CRYPTO);
            t.setVan(PgVendor.CRYPTO.length() > 10 ? PgVendor.CRYPTO.substring(0, 10) : PgVendor.CRYPTO);
            String on = deposit.getOrderNo() != null ? deposit.getOrderNo().trim() : "";
            t.setOrderNo(on.length() > 64 ? on.substring(0, 64) : on);
            t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
            t.setAmtKrw(amount);
            String asset = deposit.getAssetCode() != null ? deposit.getAssetCode().trim().toUpperCase(Locale.ROOT) : "USD";
            t.setCurType(asset.length() > 3 ? asset.substring(0, 3) : asset);
            t.setCustomerNm(truncate(deposit.getBuyerName(), 200));
            t.setCustomerId(truncate(
                    deposit.getBuyerEmail() != null && !deposit.getBuyerEmail().isBlank()
                            ? deposit.getBuyerEmail() : "guest", 100));
            t.setPaymentChannel(truncate(deposit.getNetworkCode(), 80));
            t.setChillPaymentStatus("CRYPTO_CONFIRMED");
            t.setChillTransactionId(truncate(deposit.getDepositTxHash(), 64));
            t.setPaidAt(deposit.getConfirmedAt() != null ? deposit.getConfirmedAt() : LocalDateTime.now());
            t.setSettledYn("N");
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                t.setChillFeeAmt(fee);
            }
            pgTrnsctnRepository.save(t);
            deposit.setPayTrnId(trnId);
        }

        deposit.setFeeAmt(fee);
        deposit.setFeeChargedYn("Y");
        depositTxnRepository.save(deposit);
        log.info("crypto fee charged depositId={} mid={} fee={} trnId={}",
                deposit.getId(), mid, PercentDecimalHelper.toPlainAmountOneDecimal(fee), trnId);
        return fee;
    }

    /** 매입% + 에스크로% + 인증(건) + 결제% + 건당 */
    public static BigDecimal computeCryptoFee(BigDecimal amount, CommissionPolicy pol) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (pol == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal pct = nz(pol.getPayRate())
                .add(nz(pol.getAcquisitionOpsFeePct()))
                .add(nz(pol.getEscrowOpsFeePct()));
        BigDecimal pctAmt = amount.multiply(pct).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal fixed = nz(pol.getPerTxFee()).add(nz(pol.getAuthFeePerTx()));
        return pctAmt.add(fixed).setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return "CR" + (u.length() <= 18 ? u : u.substring(0, 18));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
