package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 승인(10) 반영 전 PG 승인 근거(승인번호·TransactionId·카드 BIN 등) 검증.
 * <p>ICOPAY 아웃바운드({@code event=pg.payment.status})가 노티미들웨어·칠페이 매핑으로 재유입되어
 * {@code status=10} 만으로 오승인되는 경우를 무효(21)로 내립니다.</p>
 */
public final class PaidApprovalEvidenceGuard {

    public static final String ST_VOID = "21";
    public static final String OUTCOME_CODE_INCOMPLETE_PARAMS = "INCOMPLETE_PARAMS";
    public static final String OUTCOME_REASON_INCOMPLETE_PARAMS = "불안전한 파라미터 정보 오류";

    private PaidApprovalEvidenceGuard() {
    }

    /**
     * 승인(10)인데 근거가 없으면 무효(21)로 조정합니다. 실패(99)가 아닌 무효를 우선합니다.
     */
    public static String adjustIfPaidWithoutEvidence(String mergedStatus,
                                                     PgTrnsctn txn,
                                                     JsonNode notifyRoot,
                                                     Map<String, String> mappedByKey,
                                                     String vendorCode,
                                                     String jpayReturnCode) {
        if (!PgNotifyInternalStatusMapper.ST_PAID.equals(norm(mergedStatus))) {
            return mergedStatus;
        }
        if (hasPaidEvidence(txn, notifyRoot, mappedByKey, vendorCode, jpayReturnCode)) {
            return mergedStatus;
        }
        return ST_VOID;
    }

    public static boolean wasDowngradedFromPaid(String beforeAdjust, String afterAdjust) {
        return PgNotifyInternalStatusMapper.ST_PAID.equals(norm(beforeAdjust))
                && !PgNotifyInternalStatusMapper.ST_PAID.equals(norm(afterAdjust));
    }

    /** 동일 주문번호 후보 중 URL·API 등 실거래 행을 NOTI 유령 행보다 우선합니다. */
    @SafeVarargs
    public static Optional<PgTrnsctn> pickPreferredOrderRow(Optional<PgTrnsctn>... candidates) {
        if (candidates == null || candidates.length == 0) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> withEvidence = Optional.empty();
        Optional<PgTrnsctn> url = Optional.empty();
        Optional<PgTrnsctn> api = Optional.empty();
        Optional<PgTrnsctn> sub = Optional.empty();
        Optional<PgTrnsctn> noti = Optional.empty();
        Optional<PgTrnsctn> other = Optional.empty();
        for (Optional<PgTrnsctn> opt : candidates) {
            if (opt == null || opt.isEmpty()) {
                continue;
            }
            PgTrnsctn t = opt.get();
            if (hasPaidEvidenceOnTxn(t)) {
                withEvidence = opt;
            }
            String origin = normOrigin(t.getOrigin());
            switch (origin) {
                case "URL" -> url = opt;
                case "MERCHANT_API" -> api = opt;
                case "SUBSCRIPTION" -> sub = opt;
                case "NOTI" -> noti = opt;
                default -> other = opt;
            }
        }
        if (withEvidence.isPresent()) {
            return withEvidence;
        }
        if (url.isPresent()) {
            return url;
        }
        if (api.isPresent()) {
            return api;
        }
        if (sub.isPresent()) {
            return sub;
        }
        if (other.isPresent()) {
            return other;
        }
        return noti;
    }

    /** 동일 주문번호 후보 목록에서 URL·API·비게스트 NOTI 등 실거래 행을 우선합니다. */
    public static Optional<PgTrnsctn> pickPreferredOrderRowFromList(List<PgTrnsctn> rows) {
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn>[] opts = rows.stream()
                .filter(t -> t != null)
                .map(Optional::of)
                .toArray(Optional[]::new);
        if (opts.length == 0) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> withEvidence = Optional.empty();
        Optional<PgTrnsctn> url = Optional.empty();
        Optional<PgTrnsctn> api = Optional.empty();
        Optional<PgTrnsctn> sub = Optional.empty();
        Optional<PgTrnsctn> notiNonGuest = Optional.empty();
        Optional<PgTrnsctn> noti = Optional.empty();
        Optional<PgTrnsctn> other = Optional.empty();
        for (Optional<PgTrnsctn> opt : opts) {
            if (opt.isEmpty()) {
                continue;
            }
            PgTrnsctn t = opt.get();
            if (hasPaidEvidenceOnTxn(t)) {
                withEvidence = opt;
            }
            String origin = normOrigin(t.getOrigin());
            switch (origin) {
                case "URL" -> url = opt;
                case "MERCHANT_API" -> api = opt;
                case "SUBSCRIPTION" -> sub = opt;
                case "NOTI" -> {
                    if (isGuestCustomer(t)) {
                        noti = opt;
                    } else {
                        notiNonGuest = opt;
                    }
                }
                default -> other = opt;
            }
        }
        if (withEvidence.isPresent()) {
            return withEvidence;
        }
        if (url.isPresent()) {
            return url;
        }
        if (api.isPresent()) {
            return api;
        }
        if (sub.isPresent()) {
            return sub;
        }
        if (other.isPresent()) {
            return other;
        }
        if (notiNonGuest.isPresent()) {
            return notiNonGuest;
        }
        return noti;
    }

    public static boolean isGuestCustomer(PgTrnsctn t) {
        if (t == null) {
            return true;
        }
        String id = t.getCustomerId();
        return id == null || id.isBlank() || "guest".equalsIgnoreCase(id.trim());
    }

    public static boolean isGuestNotiRow(PgTrnsctn t) {
        return t != null && "NOTI".equals(normOrigin(t.getOrigin())) && isGuestCustomer(t);
    }

    public static boolean isIcopayOutboundEchoClaimingPaid(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        String event = text(root, "event");
        if (!"pg.payment.status".equalsIgnoreCase(event)) {
            return false;
        }
        String status = text(root, "status");
        if (!PgNotifyInternalStatusMapper.ST_PAID.equals(status)) {
            return false;
        }
        String pgTxnId = text(root, "pgTxnId", "pgtxnid");
        return pgTxnId.isBlank() || "null".equalsIgnoreCase(pgTxnId);
    }

    public static boolean hasPaidEvidence(PgTrnsctn txn,
                                          JsonNode notifyRoot,
                                          Map<String, String> mappedByKey,
                                          String vendorCode,
                                          String jpayReturnCode) {
        if (hasPaidEvidenceOnTxn(txn)) {
            return true;
        }
        if (mappedByKey != null) {
            if (nonBlank(mappedByKey.get("chillTransactionId"))) {
                return true;
            }
            if (nonBlank(mappedByKey.get("cardAprvNo")) || nonBlank(mappedByKey.get("pgApproveNo"))) {
                return true;
            }
        }
        if (notifyRoot != null) {
            String txnId = firstNonBlank(
                    text(notifyRoot, "transaction_id", "transactionId", "TransactionId"),
                    text(notifyRoot, "pgTxnId", "pgtxnid"));
            if (nonBlank(txnId) && !"null".equalsIgnoreCase(txnId.trim())) {
                return true;
            }
        }
        if (PgVendor.isJpayFamily(vendorCode)) {
            String rc = jpayReturnCode != null ? jpayReturnCode.trim() : "";
            if ("00".equals(rc) || "0".equals(rc)) {
                String txnId = notifyRoot != null
                        ? text(notifyRoot, "transaction_id", "transactionId")
                        : "";
                if (mappedByKey != null && txnId.isBlank()) {
                    txnId = firstNonBlank(
                            mappedByKey.get("chillTransactionId"),
                            mappedByKey.get("transaction_id"));
                }
                return nonBlank(txnId);
            }
            return false;
        }
        return false;
    }

    static boolean hasPaidEvidenceOnTxn(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        if (nonBlank(t.getChillTransactionId())) {
            return true;
        }
        if (nonBlank(t.getApprovalNo())) {
            return true;
        }
        return nonBlank(t.getCardPanDisplay());
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normOrigin(String origin) {
        return origin == null ? "" : origin.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String text(JsonNode root, String... keys) {
        if (root == null) {
            return "";
        }
        for (String k : keys) {
            JsonNode n = root.get(k);
            if (n == null || n.isNull()) {
                continue;
            }
            if (n.isValueNode()) {
                String v = n.asText("").trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return "";
    }

    private static String firstNonBlank(String a, String b) {
        if (nonBlank(a)) {
            return a.trim();
        }
        if (nonBlank(b)) {
            return b.trim();
        }
        return "";
    }
}
