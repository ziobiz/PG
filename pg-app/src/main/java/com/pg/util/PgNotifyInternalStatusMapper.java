package com.pg.util;

import com.pg.integration.pg.PgVendor;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PG 노티 본문의 PaymentStatus·Status 문자열을 ICOPAY 내부 거래 상태 코드로 변환.
 * {@link com.pg.service.ChillPayNotifyToTrnsctnService} 칠페이 직접 경로와
 * {@link com.pg.service.HqNotifyMappingService} 노티매핑 경로가 동일 규칙을 쓰도록 공유합니다.
 * <p>취소(20)와 승인 후 무효(21·22·40·41·42)를 구분합니다.</p>
 */
public final class PgNotifyInternalStatusMapper {

    public static final String ST_PAID = "10";
    public static final String ST_AUTH_PENDING = "08";
    public static final String ST_CANCEL = "20";
    public static final String ST_REFUND = "30";
    public static final String ST_FAIL = "99";

    private static final Pattern VOID_TOKEN_IN_PAYMENT_STATUS = Pattern.compile(
            "(?i)(^|[^a-z0-9_])void([^a-z0-9_]|$)");

    private PgNotifyInternalStatusMapper() {
    }

    /**
     * @param processingMapsToPaid true: ChillPay 노티 직접 적재와 동일하게 {@code Processing} → 승인(10).
     *                             false: 매핑 화면 등에서 벤더별로 다르게 쓸 때(비칠페이는 보통 false).
     */
    public static String mapPaymentAndStatus(String paymentStatus, String statusField, boolean processingMapsToPaid) {
        String p = paymentStatus != null ? paymentStatus.trim().toLowerCase(Locale.ROOT) : "";
        String s = statusField != null ? statusField.trim().toLowerCase(Locale.ROOT) : "";
        if (!p.isEmpty()) {
            if (p.contains("emailvoid") || p.contains("email_void")
                    || (p.contains("email") && p.contains("void") && !p.contains("cancel"))) {
                return "22";
            }
            if (p.contains("voided") || p.contains("auto void") || p.contains("autovoid")
                    || p.contains("manualvoid") || p.contains("manual_void") || p.contains("_void")
                    || "invalid".equals(p) || p.contains("무효") || p.contains("이메일무효")
                    || VOID_TOKEN_IN_PAYMENT_STATUS.matcher(p).find()) {
                return "21";
            }
            boolean completeOk = (p.contains("complete") || p.contains("completed")) && !p.contains("incomplete");
            if (p.contains("paid") || p.contains("success") || completeOk
                    || p.contains("authorized") || p.contains("authorised") || p.contains("settled")
                    || p.contains("captured") || p.contains("approved") || p.contains("confirmed")) {
                return ST_PAID;
            }
            if (processingMapsToPaid && "processing".equals(p)) {
                return ST_PAID;
            }
            if (p.contains("wait") || p.contains("authorize") || p.contains("pending") || p.contains("request")) {
                return ST_AUTH_PENDING;
            }
            if (p.contains("cancel") || p.contains("cancelled") || p.contains("canceled")) {
                return ST_CANCEL;
            }
            if (p.contains("refund")) {
                return ST_REFUND;
            }
            if (p.contains("fail") || p.contains("error")) {
                return ST_FAIL;
            }
            if (p.matches("^\\d+$")) {
                if ("0".equals(p)) {
                    /*
                     * ChillPay 콜백에서 PaymentStatus=0 은 통상 승인이나, 무효·취소 후속 노티에서
                     * 동일하게 0 이 오고 실제 사유는 Status·RespCode·ResultCode(21·2 등)에만 있는 경우가 있다.
                     * 노티미들웨어는 Status 기준으로 무효를 보여주므로 PG 전산도 Status 우선으로 맞춘다.
                     */
                    String fromStatusOnly = mapPaymentAndStatus(null, statusField, processingMapsToPaid);
                    if (fromStatusOnly != null && !ST_PAID.equals(fromStatusOnly)
                            && NotifyToTxnStatusMerge.isTerminalOutcome(fromStatusOnly)) {
                        return fromStatusOnly;
                    }
                    return ST_PAID;
                }
                if ("1".equals(p) || "3".equals(p) || "4".equals(p)) {
                    return ST_FAIL;
                }
                if ("2".equals(p)) {
                    /*
                     * ChillPay 콜백 숫자 2는 "취소" 원문이다.
                     * 승인(10) → 2 로 덮어쓰는 경우만 무효(21)로 승격하며,
                     * 그 판단은 저장 경로에서 기존 거래를 보고 수행한다(ChillPayNotifyOutcomeAdjust).
                     */
                    return ST_CANCEL;
                }
                if ("21".equals(p)) {
                    return "21";
                }
                if ("22".equals(p)) {
                    return "22";
                }
                if ("40".equals(p)) {
                    return "40";
                }
                if ("41".equals(p)) {
                    return "41";
                }
                if ("42".equals(p)) {
                    return "42";
                }
            }
        }
        if (!s.isEmpty()) {
            if ("10".equals(s) || "paid".equals(s) || "success".equals(s)
                    || "complete".equals(s) || "completed".equals(s)) {
                return ST_PAID;
            }
            if ("21".equals(s)) {
                return "21";
            }
            if ("22".equals(s)) {
                return "22";
            }
            if ("40".equals(s)) {
                return "40";
            }
            if ("41".equals(s)) {
                return "41";
            }
            if ("42".equals(s)) {
                return "42";
            }
            if ("void".equals(s)) {
                return "21";
            }
            if ("20".equals(s) || "cancel".equals(s)) {
                return ST_CANCEL;
            }
            if ("30".equals(s) || s.contains("refund")) {
                return ST_REFUND;
            }
            if ("99".equals(s) || "f0".equals(s) || s.contains("fail")) {
                return ST_FAIL;
            }
            if ("08".equals(s)) {
                return ST_AUTH_PENDING;
            }
            if (s.matches("^\\d+$")) {
                if ("0".equals(s)) {
                    return ST_PAID;
                }
                if ("1".equals(s) || "3".equals(s) || "4".equals(s)) {
                    return ST_FAIL;
                }
                if ("2".equals(s)) {
                    /* 위 PaymentStatus 숫자 2와 동일 규칙 */
                    return ST_CANCEL;
                }
                if ("21".equals(s)) {
                    return "21";
                }
                if ("22".equals(s)) {
                    return "22";
                }
                if ("40".equals(s)) {
                    return "40";
                }
                if ("41".equals(s)) {
                    return "41";
                }
                if ("42".equals(s)) {
                    return "42";
                }
            }
        }
        return null;
    }

    /** 노티매핑: 벤더가 칠페이 계열일 때만 Processing → 승인. JPAY {@code returncode} 00/2 처리. */
    public static String mapForMappedNotify(String paymentStatus, String statusField, String vendorCode) {
        String vc = vendorCode != null ? vendorCode.trim().toUpperCase(Locale.ROOT) : "";
        if (PgVendor.JPAY.equals(vc) || vc.startsWith(PgVendor.JPAY + "_")) {
            String s = statusField != null ? statusField.trim() : "";
            String fromRc = JpayNotifyStatusResolver.fromReturnCode(s);
            if (fromRc != null) {
                return fromRc;
            }
            String fromPayRc = JpayNotifyStatusResolver.fromReturnCode(paymentStatus);
            if (fromPayRc != null) {
                return fromPayRc;
            }
            String p = paymentStatus != null ? paymentStatus.trim().toLowerCase(Locale.ROOT) : "";
            if ("succeeded".equals(p) || "success".equals(p) || "paid".equals(p)) {
                return ST_PAID;
            }
            if ("failed".equals(p) || "fail".equals(p) || "failure".equals(p)) {
                return ST_FAIL;
            }
        }
        boolean chill = PgVendor.isChillPayVendorCode(vendorCode);
        return mapPaymentAndStatus(paymentStatus, statusField, chill);
    }
}
