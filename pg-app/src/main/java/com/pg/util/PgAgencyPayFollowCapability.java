package com.pg.util;

import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.service.PayListActionService;

import java.util.Map;

/**
 * 결제대행사({@code tb_pg_agency})별 ICOPAY 후속조치 허용.
 * <p>
 * 노티 미들웨어({@code integ_noti_yn})는 MID·Route·Key로 <strong>거래를 적재</strong>만 한다.
 * 후속조치는 적재된 승인 건에 대해 ICOPAY UI/API가 노출하는 기능이며, 다음이 모두 Y여야 활성화된다.
 * <ol>
 *   <li>이 클래스의 대행사 허용(본사 연동배포/결제대행사 설정)</li>
 *   <li>전산설정 결제 후속조치(NOTI 환경설정 전역)</li>
 *   <li>조직 단계별 후속조치 상한</li>
 *   <li>가맹점 프로필(MERCHANT 로그인)</li>
 *   <li>계열 하드 규칙(JPAY 수동만, ChillPay 이메일무효, EP/Eximbay 환불 API만) 및 시간 창</li>
 * </ol>
 */
public final class PgAgencyPayFollowCapability {

    private PgAgencyPayFollowCapability() {
    }

    public static boolean yn(String s) {
        return s != null && "Y".equalsIgnoreCase(s.trim());
    }

    public static String ynOrN(String s) {
        return yn(s) ? "Y" : "N";
    }

    /**
     * 결제망 API·ICOPAY 구현상 해당 계열이 수행할 수 있는 후속조치.
     * HQ 스위치를 켜도 불가능한 조합은 여기서 막는다.
     */
    public static boolean familyApiAllows(String pgCd, PayListActionService.PayFollowAction action) {
        if (action == null) {
            return false;
        }
        boolean jpay = PgVendor.isJpayFamily(pgCd);
        boolean chill = PgVendor.isChillPayFamily(pgCd);
        boolean refundApiOnly = PgVendor.isElementPayFamily(pgCd) || PgVendor.isEximbayFamily(pgCd);
        return switch (action) {
            case AUTO_VOID -> chill && !jpay && !refundApiOnly;
            case EMAIL_VOID -> chill && !jpay;
            case MANUAL_VOID, MANUAL_REFUND -> jpay;
            case AUTO_REFUND, FORCE_REFUND -> true;
        };
    }

    public static boolean familyAllowsSameDayRefund(String pgCd) {
        return PgVendor.isElementPayFamily(pgCd);
    }

    /** 신규 행·마이그레이션과 동일한 계열 기본값(현재 하드 규칙과 맞춤). */
    public static void applyFamilyDefaults(PgAgency a) {
        if (a == null) {
            return;
        }
        String cd = a.getPgCd();
        if (PgVendor.isChillPayFamily(cd)) {
            a.setPayFollowAutoVoidYn("Y");
            a.setPayFollowEmailVoidYn("Y");
            a.setPayFollowManualVoidYn("N");
            a.setPayFollowAutoRefundYn("Y");
            a.setPayFollowManualRefundYn("N");
            a.setPayFollowForceRefundYn("Y");
            a.setPayFollowSameDayRefundYn("N");
            return;
        }
        if (PgVendor.isJpayFamily(cd)) {
            a.setPayFollowAutoVoidYn("N");
            a.setPayFollowEmailVoidYn("N");
            a.setPayFollowManualVoidYn("Y");
            a.setPayFollowAutoRefundYn("Y");
            a.setPayFollowManualRefundYn("Y");
            a.setPayFollowForceRefundYn("Y");
            a.setPayFollowSameDayRefundYn("N");
            return;
        }
        if (PgVendor.isElementPayFamily(cd)) {
            a.setPayFollowAutoVoidYn("N");
            a.setPayFollowEmailVoidYn("N");
            a.setPayFollowManualVoidYn("N");
            a.setPayFollowAutoRefundYn("Y");
            a.setPayFollowManualRefundYn("N");
            a.setPayFollowForceRefundYn("Y");
            a.setPayFollowSameDayRefundYn("Y");
            return;
        }
        a.setPayFollowAutoVoidYn("N");
        a.setPayFollowEmailVoidYn("N");
        a.setPayFollowManualVoidYn("N");
        a.setPayFollowAutoRefundYn("Y");
        a.setPayFollowManualRefundYn("N");
        a.setPayFollowForceRefundYn("Y");
        a.setPayFollowSameDayRefundYn("N");
    }

    public static boolean allows(PgAgency a, PayListActionService.PayFollowAction action) {
        if (a == null || action == null) {
            return false;
        }
        if (!familyApiAllows(a.getPgCd(), action)) {
            return false;
        }
        return switch (action) {
            case AUTO_VOID -> yn(a.getPayFollowAutoVoidYn());
            case EMAIL_VOID -> yn(a.getPayFollowEmailVoidYn());
            case MANUAL_VOID -> yn(a.getPayFollowManualVoidYn());
            case AUTO_REFUND -> yn(a.getPayFollowAutoRefundYn());
            case MANUAL_REFUND -> yn(a.getPayFollowManualRefundYn());
            case FORCE_REFUND -> yn(a.getPayFollowForceRefundYn());
        };
    }

    public static boolean allowsSameDayRefund(PgAgency a) {
        if (a == null) {
            return false;
        }
        return familyAllowsSameDayRefund(a.getPgCd()) && yn(a.getPayFollowSameDayRefundYn());
    }

    public static void putFlags(Map<String, Object> m, PgAgency p) {
        if (m == null || p == null) {
            return;
        }
        m.put("payFollowAutoVoidYn", ynOrN(p.getPayFollowAutoVoidYn()));
        m.put("payFollowEmailVoidYn", ynOrN(p.getPayFollowEmailVoidYn()));
        m.put("payFollowManualVoidYn", ynOrN(p.getPayFollowManualVoidYn()));
        m.put("payFollowAutoRefundYn", ynOrN(p.getPayFollowAutoRefundYn()));
        m.put("payFollowManualRefundYn", ynOrN(p.getPayFollowManualRefundYn()));
        m.put("payFollowForceRefundYn", ynOrN(p.getPayFollowForceRefundYn()));
        m.put("payFollowSameDayRefundYn", ynOrN(p.getPayFollowSameDayRefundYn()));
    }
}
