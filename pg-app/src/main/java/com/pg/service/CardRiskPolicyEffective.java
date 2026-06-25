package com.pg.service;

/** 가맹점·본사 정책 병합 결과 — 결제 실패 쿨다운·자동 비활성 트리거 */
public record CardRiskPolicyEffective(
        boolean enabled,
        int[] tierTotalMinutes,
        int autoBlacklistTriggerTier,
        String policySource
) {
    public int tierMinutes(int tier1Based) {
        if (tierTotalMinutes == null || tier1Based < 1 || tier1Based > tierTotalMinutes.length) {
            return 0;
        }
        return Math.max(0, tierTotalMinutes[tier1Based - 1]);
    }
}
