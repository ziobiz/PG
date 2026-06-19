package com.pg.util;

import com.pg.entity.PgNotifyInbound;

/**
 * {@link com.pg.entity.PgNotifyInbound} 저장 전 컬럼 길이를 엔티티·DB 한도에 맞춥니다.
 * 운영 DB가 마이그레이션(V173 등) 이전이어도 수신 로그가 쌓이도록 보수적으로 자릅니다.
 */
public final class PgNotifyInboundSanitizer {

    private PgNotifyInboundSanitizer() {
    }

    public static void sanitize(PgNotifyInbound in) {
        if (in == null) {
            return;
        }
        in.setMid(trunc(in.getMid(), 80));
        in.setRootNo(trunc(in.getRootNo(), 40));
        in.setPayloadCompId(trunc(in.getPayloadCompId(), 64));
        in.setMerchantId(trunc(in.getMerchantId(), 50));
        in.setContentType(trunc(in.getContentType(), 120));
        in.setClientIp(trunc(in.getClientIp(), 64));
        in.setNotifyTargetCode(trunc(in.getNotifyTargetCode(), 64));
        in.setNotifyChannelType(trunc(in.getNotifyChannelType(), 20));
        in.setIngressDeliveryKind(trunc(in.getIngressDeliveryKind(), 16));
        in.setProcessStatus(trunc(in.getProcessStatus(), 32));
        in.setErrorMessage(trunc(in.getErrorMessage(), 500));
    }

    /** 레거시 DB({@code merchant_id VARCHAR(20)}, {@code process_status VARCHAR(20)}) 폴백 */
    public static void applyLegacyDbLimits(PgNotifyInbound in) {
        if (in == null) {
            return;
        }
        in.setMerchantId(trunc(in.getMerchantId(), 20));
        in.setProcessStatus(trunc(in.getProcessStatus(), 20));
    }

    private static String trunc(String s, int max) {
        if (s == null || s.isBlank()) {
            return s;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
