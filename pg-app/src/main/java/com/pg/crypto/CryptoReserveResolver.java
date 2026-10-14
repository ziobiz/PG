package com.pg.crypto;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.MerchantCryptoConfig;
import com.pg.service.CommissionService;
import com.pg.util.CommissionFeeChannel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 가맹 크립토 보증금(%)·보류일수 실효값.
 * 본사정책 따름(Y)이면 CRYPTO 수수료정책 rollingPct / rollingDays.
 * 직접입력(N)이면 MerchantCryptoConfig.reservePct / holdDays.
 */
@Component
public class CryptoReserveResolver {

    private final CommissionService commissionService;

    public CryptoReserveResolver(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    public static boolean isFollowHq(String followYn) {
        if (followYn == null || followYn.isBlank()) return true;
        return !"N".equalsIgnoreCase(followYn.trim());
    }

    public Effective resolve(MerchantCryptoConfig cfg, String merchantCode) {
        boolean follow = cfg == null || isFollowHq(cfg.getReserveFollowHq());
        if (follow) {
            CommissionPolicy pol = null;
            String mid = merchantCode != null ? merchantCode.trim() : "";
            if (!mid.isEmpty()) {
                try {
                    pol = commissionService.resolveCommissionPolicyForSettlement(mid, CommissionFeeChannel.CRYPTO);
                } catch (Exception ignored) {
                    pol = null;
                }
            }
            BigDecimal pct = BigDecimal.ZERO;
            int days = 0;
            if (pol != null) {
                if (pol.getRollingPct() != null) pct = pol.getRollingPct();
                if (pol.getRollingDays() != null) days = Math.max(0, pol.getRollingDays());
            }
            if (pct.compareTo(BigDecimal.ZERO) < 0) pct = BigDecimal.ZERO;
            if (pct.compareTo(new BigDecimal("100")) > 0) pct = new BigDecimal("100");
            return new Effective(true, pct, days, pct, days);
        }
        BigDecimal pct = cfg.getReservePct() != null ? cfg.getReservePct() : BigDecimal.ZERO;
        int days = cfg.getHoldDays() != null ? Math.max(0, cfg.getHoldDays()) : 3;
        if (pct.compareTo(BigDecimal.ZERO) < 0) pct = BigDecimal.ZERO;
        if (pct.compareTo(new BigDecimal("100")) > 0) pct = new BigDecimal("100");
        return new Effective(false, pct, days, pct, days);
    }

    /** HQ CRYPTO 정책의 롤링 값(표시·본사따름 미리보기). */
    public HqRolling hqRolling(String merchantCode) {
        String mid = merchantCode != null ? merchantCode.trim() : "";
        CommissionPolicy pol = null;
        if (!mid.isEmpty()) {
            try {
                pol = commissionService.resolveCommissionPolicyForSettlement(mid, CommissionFeeChannel.CRYPTO);
            } catch (Exception ignored) {
                pol = null;
            }
        }
        BigDecimal pct = BigDecimal.ZERO;
        int days = 0;
        if (pol != null) {
            if (pol.getRollingPct() != null) pct = pol.getRollingPct();
            if (pol.getRollingDays() != null) days = Math.max(0, pol.getRollingDays());
        }
        return new HqRolling(pct, days);
    }

    public record Effective(boolean followHq, BigDecimal reservePct, int holdDays,
                            BigDecimal effectiveReservePct, int effectiveHoldDays) {}

    public record HqRolling(BigDecimal rollingPct, int rollingDays) {}
}
