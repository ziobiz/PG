package com.pg.service;

/**
 * 가맹점·본사 사전 리스크 필터(presale) 병합 결과.
 * policySource: FOLLOW_HQ | CUSTOM | DISABLED
 */
public record PresaleRiskFilterEffective(
        boolean enabled,
        String policySource,
        String filterBuyerContactMismatchYn,
        String filterHolderNameYn,
        String filterPhoneInvalidYn,
        String filterEmailInvalidYn,
        String filterVelocityCardYn,
        String filterVelocityEmailYn,
        String filterVelocityIpYn,
        int velocityCardWindowMinutes,
        int velocityCardMaxAttempts,
        int velocityEmailWindowMinutes,
        int velocityEmailMaxAttempts,
        int velocityIpWindowMinutes,
        int velocityIpMaxAttempts
) {
    public static PresaleRiskFilterEffective disabled(String source) {
        return new PresaleRiskFilterEffective(
                false, source,
                "N", "N", "N", "N", "N", "N", "N",
                10, 3, 30, 5, 15, 10);
    }
}
