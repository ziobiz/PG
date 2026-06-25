package com.pg.util;

import java.util.Locale;

/**
 * 동일 거래에 CALLBACK·RESULT 등 노티가 순차 도착할 때 {@link com.pg.entity.PgTrnsctn#getStatus()} 병합.
 * <ul>
 *   <li>RESULT 로 실패·취소·환불·무효 등 터미널 상태가 오면, 이전 CALLBACK 성공(10)을 덮어씁니다.</li>
 *   <li>CALLBACK 은 일반적으로 더 강한(높은 rank) 상태만 반영해 성공→실패 순서를 허용합니다.</li>
 *   <li>JPAY UNPAID 임시 취소(20) — 늦은 실패·환불·무효 노티가 최종 확정. 서명 검증된 성공(10) 노티는 최종 승인으로 승격.</li>
 *   <li>JPAY 비동기 승인 — pay_index 동기 실패(99) 등 선행 상태 뒤 {@code returncode=00} 성공(10) 노티는 승인으로 갱신합니다.</li>
 *   <li>이번 노티에서 상태를 판단할 수 없으면(incoming null) 기존 상태를 유지합니다.</li>
 * </ul>
 */
public final class NotifyToTxnStatusMerge {

    /** JPAY UNPAID Trade Query·포털 동기화로 부여한 임시 취소 — 늦은 JPAY 노티(실패·성공 등)가 최종 확정 */
    public static final String OUTCOME_CODE_UNPAID_PROVISIONAL = "UNPAID";

    private NotifyToTxnStatusMerge() {
    }

    /**
     * @param previous   기존 DB 상태 (없으면 null)
     * @param incoming   이번 노티에서 해석한 상태 (없으면 null — 유지)
     * @param notifyChannel CALLBACK, RESULT 등 (대소문자 무시)
     */
    public static String merge(String previous, String incoming, String notifyChannel) {
        return merge(previous, incoming, notifyChannel, null);
    }

    /**
     * @param prevOutcomeReasonCode 기존 행 {@code outcome_reason_code} — UNPAID 임시 취소 판별용
     */
    public static String merge(String previous, String incoming, String notifyChannel, String prevOutcomeReasonCode) {
        if (incoming == null || incoming.isBlank()) {
            if (previous != null && !previous.isBlank()) {
                return previous.trim();
            }
            return "08";
        }
        String inc = incoming.trim();
        if (previous == null || previous.isBlank()) {
            return inc;
        }
        String prev = previous.trim();
        /* UNPAID 동기화 임시 취소(20) — 늦은 JPAY 실패·환불·무효 노티가 최종 확정 */
        if (isUnpaidProvisionalCancel(prev, prevOutcomeReasonCode) && isTerminalOutcome(inc)) {
            return inc;
        }
        /* UNPAID 임시 취소(20) — 서명 검증된 JPAY 성공(10) 노티는 최종 승인으로 승격 */
        if (isUnpaidProvisionalCancel(prev, prevOutcomeReasonCode) && PgNotifyInternalStatusMapper.ST_PAID.equals(inc)) {
            return inc;
        }
        /* 승인 완료(10) 건에 이어지는 취소·무효·환불·실패 노티는 즉시 반영(피지·노티미들웨어 후속 통지) */
        if ("10".equals(prev) && isTerminalOutcome(inc)) {
            return inc;
        }
        /* JPAY 비동기 승인 — 선행 실패(99)·대기(08) 뒤 returncode=00 등 성공 노티는 최종 승인으로 갱신 */
        if ("10".equals(inc) && isProvisionalPreSuccess(prev)) {
            return inc;
        }
        String ch = notifyChannel == null ? "" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        if ("RESULT".equals(ch) && isTerminalOutcome(inc)) {
            return inc;
        }
        int rp = rank(prev);
        int ri = rank(inc);
        return ri > rp ? inc : prev;
    }

    /** UNPAID Trade Query·포털 동기화로 부여한 임시 취소(20) 여부 */
    public static boolean isUnpaidProvisionalCancel(String status, String outcomeReasonCode) {
        if (status == null || !PgNotifyInternalStatusMapper.ST_CANCEL.equals(status.trim())) {
            return false;
        }
        String code = outcomeReasonCode != null ? outcomeReasonCode.trim() : "";
        return OUTCOME_CODE_UNPAID_PROVISIONAL.equalsIgnoreCase(code);
    }

    /** 실패·취소·환불·무효·오류 등 최종 확정에 가까운 상태 */
    public static boolean isTerminalOutcome(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        return switch (st.trim()) {
            case "99", "F0", "f0", "20", "30", "31", "21", "22", "40", "41", "42" -> true;
            default -> false;
        };
    }

    /** pay_index 동기 실패·결제 대기 등 — 비동기 승인 노티로 성공(10)으로 바뀔 수 있는 상태 */
    private static boolean isProvisionalPreSuccess(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        return switch (st.trim()) {
            case "99", "F0", "f0", "08" -> true;
            default -> false;
        };
    }

    private static int rank(String st) {
        if (st == null || st.isBlank()) {
            return 0;
        }
        return switch (st.trim()) {
            case "99", "F0", "f0" -> 100;
            case "30", "31" -> 95;
            case "20" -> 90;
            case "21", "22", "40", "41", "42" -> 88;
            case "10" -> 50;
            case "08" -> 40;
            default -> 25;
        };
    }
}
