package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.dto.PageResult;
import com.pg.entity.PgNotifyInbound;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.util.NotifyIngressDeliveryKindResolver;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 본사설정 — 노티 서버(노티미들웨어·PG)에서 수신해 저장한 {@link PgNotifyInbound} 조회
 */
@Service
public class HqNotifyInboundQueryService {

    private static final int PREVIEW_MAX = 280;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PgNotifyInboundRepository inboundRepository;

    public HqNotifyInboundQueryService(PgNotifyInboundRepository inboundRepository) {
        this.inboundRepository = inboundRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page, int size,
                                                  String searchKey,
                                                  String searchValue,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {
        int p = Math.max(1, page);
        int sz = Math.min(1000, Math.max(1, size));
        Specification<PgNotifyInbound> spec = buildSpec(searchKey, searchValue, fromDate, toDate);
        Page<PgNotifyInbound> pg = inboundRepository.findAll(spec,
                PageRequest.of(p - 1, sz, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(pg, this::toSummaryRow);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findDetail(long id) {
        return inboundRepository.findById(id).map(this::toDetailRow);
    }

    private Specification<PgNotifyInbound> buildSpec(String searchKey,
                                                     String searchValue,
                                                     LocalDate fromDate,
                                                     LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            addSearchFieldPredicate(root, cb, preds, searchKey, searchValue);
            LocalDateTime fromDt = null;
            LocalDateTime toEx = null;
            if (fromDate != null) {
                fromDt = fromDate.atStartOfDay();
            }
            if (toDate != null) {
                toEx = toDate.plusDays(1).atStartOfDay();
            }
            if (fromDt != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (toEx != null) {
                preds.add(cb.lessThan(root.get("createdAt"), toEx));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(Predicate[]::new));
        };
    }

    /**
     * 검색 항목: MID, ROUTE, MERCHANT, STATUS, TXN_ID(TransactionId·승인번호 등), ORDER_NO — 본문 JSON은 부분 일치(LIKE).
     */
    private static void addSearchFieldPredicate(Root<PgNotifyInbound> root,
                                                CriteriaBuilder cb,
                                                List<Predicate> preds,
                                                String searchKey,
                                                String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return;
        }
        String rawKey = searchKey == null ? "" : searchKey.trim().toUpperCase(Locale.ROOT);
        if (rawKey.isEmpty()) {
            rawKey = "MID";
        }
        String val = searchValue.trim();
        if (val.isEmpty()) {
            return;
        }
        String likePat = "%" + sanitizeForLikeSubstring(val.toLowerCase(Locale.ROOT)) + "%";
        switch (rawKey) {
            case "ROUTE" -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("rootNo"), cb.literal(""))), likePat));
            case "MERCHANT" -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("merchantId"), cb.literal(""))), likePat));
            case "STATUS" -> preds.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("processStatus"), cb.literal(""))), likePat),
                    cb.like(cb.lower(cb.coalesce(root.get("rawBody"), cb.literal(""))), likePat)));
            case "TXN_ID" -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("rawBody"), cb.literal(""))), likePat));
            case "ORDER_NO" -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("rawBody"), cb.literal(""))), likePat));
            case "MID" -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("mid"), cb.literal(""))), likePat));
            default -> preds.add(cb.like(cb.lower(cb.coalesce(root.get("mid"), cb.literal(""))), likePat));
        }
    }

    /** LIKE 패턴을 깨뜨리지 않도록 % · _ · \ 제거 */
    private static String sanitizeForLikeSubstring(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "").replace("%", "").replace("_", "");
    }

    private Map<String, Object> toSummaryRow(PgNotifyInbound in) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", in.getId());
        m.put("createdAt", in.getCreatedAt() != null ? in.getCreatedAt().toString() : "");
        m.put("notifyTargetCode", blankToDash(in.getNotifyTargetCode()));
        m.put("notifyChannelType", blankToDash(in.getNotifyChannelType()));
        String idk = ingressDeliveryKindNorm(in.getIngressDeliveryKind());
        m.put("ingressDeliveryKind", idk);
        m.put("ingressDeliveryKindLabel", ingressDeliveryKindLabelKo(idk));
        m.put("mid", blankToDash(in.getMid()));
        m.put("rootNo", blankToDash(in.getRootNo()));
        m.put("merchantId", blankToDash(in.getMerchantId()));
        m.put("orgUnitId", in.getOrgUnitId());
        m.put("payloadCompId", blankToDash(in.getPayloadCompId()));
        String raw = in.getRawBody();
        String payStRaw = extractPaymentStatusRaw(raw);
        String payStLabel = notiAdminTransactionsStatusKo(raw, payStRaw);
        String payDisp = payStLabel.isEmpty() ? "-" : payStLabel;
        m.put("processStatus", payDisp);
        m.put("paymentStatusLabel", payDisp);
        m.put("parseStatusLabel", parseProcessStatusKo(in.getProcessStatus()));
        m.put("paymentStatusRaw", payStRaw != null ? payStRaw : "");
        m.put("transactionId", extractTransactionIdDisplay(raw));
        if (raw != null && raw.length() > PREVIEW_MAX) {
            m.put("rawPreview", raw.substring(0, PREVIEW_MAX) + "…");
            m.put("rawTruncated", true);
        } else {
            m.put("rawPreview", raw != null ? raw : "");
            m.put("rawTruncated", false);
        }
        String err = in.getErrorMessage();
        if (err != null && err.length() > 120) {
            m.put("errorMessage", err.substring(0, 120) + "…");
        } else {
            m.put("errorMessage", err != null ? err : "");
        }
        return m;
    }

    private Map<String, Object> toDetailRow(PgNotifyInbound in) {
        Map<String, Object> m = new LinkedHashMap<>(toSummaryRow(in));
        m.put("rawBody", in.getRawBody() != null ? in.getRawBody() : "");
        m.put("errorMessage", in.getErrorMessage() != null ? in.getErrorMessage() : "");
        m.put("clientIp", in.getClientIp() != null ? in.getClientIp().trim() : "");
        m.put("parseStatus", blankToDash(in.getProcessStatus()));
        m.remove("rawPreview");
        m.remove("rawTruncated");
        return m;
    }

    private static String blankToDash(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.trim();
    }

    private static String ingressDeliveryKindNorm(String raw) {
        if (raw == null || raw.isBlank()) {
            return NotifyIngressDeliveryKindResolver.UNKNOWN;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (NotifyIngressDeliveryKindResolver.LIVE.equals(u)) {
            return NotifyIngressDeliveryKindResolver.LIVE;
        }
        if (NotifyIngressDeliveryKindResolver.RETRY.equals(u)) {
            return NotifyIngressDeliveryKindResolver.RETRY;
        }
        return NotifyIngressDeliveryKindResolver.UNKNOWN;
    }

    private static String ingressDeliveryKindLabelKo(String code) {
        if (NotifyIngressDeliveryKindResolver.LIVE.equals(code)) {
            return "라이브";
        }
        if (NotifyIngressDeliveryKindResolver.RETRY.equals(code)) {
            return "재전송";
        }
        return "미표시";
    }

    /** DB {@code process_status} — 목록·상세에서 본문 기반 결제상태와 구분해 표시 */
    private static String parseProcessStatusKo(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String u = code.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "PARSED" -> "매핑완료";
            case "MERCHANT_UNRESOLVED" -> "가맹점미매핑";
            case "MERCHANT_DISABLED" -> "업체차단(프로필N·구버전)";
            case "URL_PAY_NEEDS_COMP_ID" -> "URL결제·업체코드필요";
            case "UNKNOWN_COMP" -> "업체미확인";
            case "NO_PG_BINDING" -> "바인딩없음";
            case "COMP_MID_MISMATCH" -> "MID불일치";
            case "COMP_NOT_URL_PAY_PG" -> "URL결제PG아님";
            case "BOUND_CURRENCY_MISMATCH" -> "통화불일치(수신경로)";
            case "INGRESS_ORG_SCOPE_MISMATCH" -> "수신경로업체불일치";
            default -> code.trim();
        };
    }

    /** 노티 JSON에서 TransactionId·승인번호 계열 첫 값 */
    private static String extractTransactionIdDisplay(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[][] keys = {
                {"TransactionId"}, {"transactionId"},
                {"PgTransactionId"}, {"pgTransactionId"},
                {"ApprovalNo"}, {"approvalNo"}, {"AuthNo"}, {"authNo"},
                {"PgApproveNo"}, {"pgApproveNo"}, {"ApproveNo"}, {"approveNo"}
        };
        for (String[] k : keys) {
            String v = jsonStringValue(raw, k[0]);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        Matcher num = Pattern.compile("\"(?:TransactionId|transactionId)\"\\s*:\\s*([0-9]+)").matcher(raw);
        if (num.find()) {
            return num.group(1).trim();
        }
        return "";
    }

    private static String jsonStringValue(String raw, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 노티 본문 전체에서 첫 {@code PaymentStatus} / {@code paymentStatus} 값(문자열 또는 숫자).
     * ChillPay·NOTI JSON 어디에 있든 동일 키면 매칭합니다.
     */
    private static String extractPaymentStatusRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher mq = Pattern.compile("(?i)\"paymentStatus\"\\s*:\\s*\"([^\"]*)\"").matcher(raw);
        if (mq.find()) {
            String s = mq.group(1).trim();
            return s.isEmpty() ? null : s;
        }
        Matcher mn = Pattern.compile("(?i)\"paymentStatus\"\\s*:\\s*(-?\\d+)").matcher(raw);
        if (mn.find()) {
            return mn.group(1).trim();
        }
        return null;
    }

    /**
     * ziobiz/NOTI {@code /admin/transactions} 목록의 「상태」 뱃지와 동일한 판정 순서(취소 → 성공 → 오류 → 실패).
     * 근거: NOTI {@code server.js} — {@code isSuccessPaymentBody}, {@code isDefinitelyCancelPaymentStatus},
     * {@code isErrorPaymentStatus}, 콜백 숫자(0 성공, 1·3 실패, 2 취소, 4 오류).
     * Void/환불 후속 노티 구분은 NOTI가 별도 맵으로 하므로 본 화면(수신 원문만)에서는 미적용.
     */
    private static String notiAdminTransactionsStatusKo(String rawBody, String paymentStatusRawFallback) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }
        JsonNode root = tryParseJsonObject(rawBody);
        if (root != null) {
            String ps = paymentStatusAsString(root);
            if (isDefinitelyCancelPaymentStatusNoti(ps)) {
                return "취소";
            }
            if (notiIsSuccessPaymentBody(root)) {
                return "성공";
            }
            if (isErrorPaymentStatusNoti(ps)) {
                return "오류";
            }
            return "실패";
        }
        /* JSON 파싱 실패 시 PaymentStatus·JPAY returncode 휴리스틱 */
        String ps = paymentStatusRawFallback;
        if (ps == null || ps.isBlank()) {
            ps = extractPaymentStatusRaw(rawBody);
        }
        if (rawBodyLooksJpayAsyncSuccess(rawBody)) {
            return "성공";
        }
        if (ps != null && !ps.isBlank() && isDefinitelyCancelPaymentStatusNoti(ps)) {
            return "취소";
        }
        if (ps != null && !ps.isBlank() && isNotiCallbackNumericSuccess(ps)) {
            return "성공";
        }
        if (ps != null && !ps.isBlank() && notiChillPayStyleSuccessToken(ps)) {
            return "성공";
        }
        if (ps != null && !ps.isBlank() && isErrorPaymentStatusNoti(ps)) {
            return "오류";
        }
        if (ps != null && !ps.isBlank() && isIcPayInternalStatusCodeLabel(ps)) {
            return icPayInternalStatusCodeToKo(ps.trim());
        }
        if ("f0".equalsIgnoreCase(String.valueOf(ps).trim())) {
            return "실패";
        }
        return ps != null && !ps.isBlank() ? ps.trim() : "";
    }

    private static JsonNode tryParseJsonObject(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (!t.startsWith("{")) {
            return null;
        }
        try {
            JsonNode n = OBJECT_MAPPER.readTree(t);
            return n != null && n.isObject() ? n : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode firstNonNull(JsonNode a, JsonNode b) {
        if (a != null && !a.isNull() && !a.isMissingNode()) {
            return a;
        }
        return b;
    }

    private static String paymentStatusAsString(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode n = firstNonNull(root.get("PaymentStatus"), firstNonNull(root.get("paymentStatus"), root.get("status")));
        if (n == null || n.isNull() || n.isMissingNode()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asText();
        }
        return n.asText("");
    }

    /** NOTI {@code isJpaySaleAsyncNotifyBody} + {@code returncode} 00/0 이면 성공 (원문만 있을 때). */
    private static boolean rawBodyLooksJpayAsyncSuccess(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        boolean hasTid = Pattern.compile("(?i)\"(?:transaction_id|transactionId)\"\\s*:").matcher(raw).find();
        if (!hasTid) {
            return false;
        }
        Matcher mq = Pattern.compile("(?i)\"returncode\"\\s*:\\s*\"([^\"]*)\"").matcher(raw);
        if (mq.find()) {
            String rc = mq.group(1).trim();
            return "00".equals(rc) || "0".equals(rc);
        }
        Matcher mn = Pattern.compile("(?i)\"returncode\"\\s*:\\s*(0+)\\b").matcher(raw);
        return mn.find();
    }

    private static boolean isJpaySaleAsyncNotifyBody(JsonNode body) {
        if (body == null || !body.isObject()) {
            return false;
        }
        JsonNode tid = firstNonNull(body.get("transaction_id"), body.get("transactionId"));
        if (tid == null || tid.isNull() || tid.asText("").isBlank()) {
            return false;
        }
        JsonNode rc = body.get("returncode");
        return rc != null && !rc.isNull() && !rc.asText("").isBlank();
    }

    /**
     * NOTI {@code isSuccessPaymentBody} + 칠페이 관용 문자열(Paid, Processing 등) — 실제 노티에서 흔함.
     */
    private static boolean notiIsSuccessPaymentBody(JsonNode body) {
        if (body == null || !body.isObject()) {
            return false;
        }
        if (isJpaySaleAsyncNotifyBody(body)) {
            String rc = body.get("returncode").asText("").trim();
            return "00".equals(rc) || "0".equals(rc);
        }
        JsonNode orderIdSync = firstNonNull(body.get("orderID"), body.get("orderid"));
        JsonNode payStLower = body.get("paymentStatus");
        if (payStLower != null && !payStLower.isNull() && orderIdSync != null && !orderIdSync.isNull()
                && !body.has("PaymentStatus")) {
            String st = payStLower.asText("").toLowerCase(Locale.ROOT);
            if ("succeeded".equals(st) || "success".equals(st)) {
                return true;
            }
        }
        String ps = paymentStatusAsString(body);
        if (ps != null && !ps.isBlank()) {
            String trim = ps.trim();
            if ("0".equals(trim)) {
                return true;
            }
            String lower = trim.toLowerCase(Locale.ROOT);
            if ("success".equals(lower) || "complete".equals(lower)) {
                return true;
            }
            if (notiChillPayStyleSuccessToken(trim)) {
                return true;
            }
        }
        return false;
    }

    private static boolean notiChillPayStyleSuccessToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String p = rawToken.trim().toLowerCase(Locale.ROOT);
        boolean completeOk = (p.contains("complete") || p.contains("completed")) && !p.contains("incomplete");
        if (p.contains("paid") || p.contains("success") || completeOk
                || p.contains("authorized") || p.contains("authorised") || p.contains("settled")
                || p.contains("captured") || p.contains("approved") || p.contains("confirmed")) {
            return true;
        }
        return "processing".equals(p);
    }

    private static boolean isNotiCallbackNumericSuccess(String ps) {
        if (ps == null) {
            return false;
        }
        String t = ps.trim();
        return "0".equals(t);
    }

    /** NOTI {@code isDefinitelyCancelPaymentStatus} — 0은 취소 아님. */
    private static boolean isDefinitelyCancelPaymentStatusNoti(String ps) {
        if (ps == null || ps.isBlank()) {
            return false;
        }
        String t = ps.trim();
        if ("0".equals(t)) {
            return false;
        }
        return "2".equals(t)
                || "Cancel".equals(ps.trim()) || "Canceled".equals(ps.trim()) || "Cancelled".equals(ps.trim())
                || "cancel".equalsIgnoreCase(t);
    }

    /** NOTI {@code isErrorPaymentStatus} — 숫자 3은 취소가 아니라 실패 분기(목록 끝 "실패")로 처리. */
    private static boolean isErrorPaymentStatusNoti(String ps) {
        if (ps == null || ps.isBlank()) {
            return false;
        }
        String t = ps.trim();
        if ("4".equals(t)) {
            return true;
        }
        return "error".equalsIgnoreCase(t);
    }

    private static boolean isIcPayInternalStatusCodeLabel(String ps) {
        if (ps == null) {
            return false;
        }
        String t = ps.trim();
        return t.matches("^(08|10|20|21|22|30|31|99|F0|f0)$");
    }

    private static String icPayInternalStatusCodeToKo(String norm) {
        String u = norm.toUpperCase(Locale.ROOT);
        return switch (u) {
            case "08" -> "요청";
            case "10" -> "성공";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "이메일무효";
            case "30" -> "환불";
            case "31" -> "강제환불";
            case "99", "F0" -> "실패";
            default -> norm;
        };
    }
}
