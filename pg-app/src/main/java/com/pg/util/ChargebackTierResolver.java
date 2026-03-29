package com.pg.util;

import com.pg.entity.ChargebackFeeTier;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 월간 차지백(환불·강제환불) 건수에 맞는 구간의 건당 수수료를 고른다.
 * 구간은 {@link ChargebackFeeTier#getSortOrder()} 오름차순으로 검사하며,
 * 첫 번째로 {@code monthCount >= countMin && (countMax == null || monthCount <= countMax)} 인 구간을 사용한다.
 */
public final class ChargebackTierResolver {

    private ChargebackTierResolver() {
    }

    public static BigDecimal feePerCaseForMonthlyCount(int monthCount, List<ChargebackFeeTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return tiers.stream()
                .sorted(Comparator.comparingInt(ChargebackFeeTier::getSortOrder))
                .filter(t -> monthCount >= t.getCountMin()
                        && (t.getCountMax() == null || monthCount <= t.getCountMax()))
                .map(ChargebackFeeTier::getFeePerCase)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
