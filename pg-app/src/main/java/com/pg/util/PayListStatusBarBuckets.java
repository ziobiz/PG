package com.pg.util;

import com.pg.entity.AppUser;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.OrgUnitRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 결제내역·통합내역 상단 상태별(성공·실패·무효·환불·기타) 건수·통화별 금액 요약.
 */
public final class PayListStatusBarBuckets {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final String VOID = "VOID";
    public static final String REFUND = "REFUND";
    public static final String OTHER = "OTHER";

    private PayListStatusBarBuckets() {
    }

    /**
     * 다통화(버킷별 JPY | USD …) 표시 여부.
     * 총본사·본사만 통화별로 나란히 표시. 총판·지사 이하는 조직 기준 통화 한 줄만 집계.
     */
    public static boolean isMultiCurrencyViewer(OrgLevel level) {
        if (level == null) {
            return true;
        }
        return level == OrgLevel.HEADQUARTERS
                || level == OrgLevel.REGIONAL;
    }

    /** 통화 코드 목록 정렬(JPY·USD 순 등) — 집계 JSON·UI 공통 */
    public static void sortCurrencyCodes(List<String> codes) {
        if (codes == null) {
            return;
        }
        codes.sort(PayListStatusBarBuckets::currencySort);
    }

    public static String bucketForPgStatus(String statusRaw) {
        if (statusRaw == null) {
            return OTHER;
        }
        String s = statusRaw.trim();
        if (s.isEmpty()) {
            return OTHER;
        }
        if ("10".equals(s)) {
            return SUCCESS;
        }
        if ("F0".equalsIgnoreCase(s) || "99".equals(s)) {
            return FAIL;
        }
        if ("21".equals(s) || "22".equals(s) || "40".equals(s) || "41".equals(s) || "42".equals(s)) {
            return VOID;
        }
        if ("30".equals(s) || "31".equals(s)) {
            return REFUND;
        }
        return OTHER;
    }

    /**
     * ChillPay Transaction Status / PaymentStatus 문자열 — {@code resolveChillTrRowTone} 와 동일 계열.
     */
    public static String bucketForChillStatus(String statusRaw) {
        if (statusRaw == null) {
            return OTHER;
        }
        String raw = statusRaw.trim();
        if (raw.isEmpty()) {
            return OTHER;
        }
        String low = raw.toLowerCase(Locale.ROOT);
        if ("0".equals(raw)) {
            return SUCCESS;
        }
        if ("2".equals(raw)) {
            return OTHER;
        }
        if ("1".equals(raw) || "3".equals(raw) || "4".equals(raw)) {
            return FAIL;
        }
        if (low.contains("voided") || low.contains("void") || low.contains("emailvoid")
                || low.contains("무효") || low.contains("이메일무효")) {
            return VOID;
        }
        if (low.contains("refund") || low.contains("환불")) {
            return REFUND;
        }
        if (low.contains("cancel") || low.contains("cancelled") || low.contains("canceled") || low.contains("취소")) {
            return OTHER;
        }
        if (low.contains("fail") || low.contains("error") || low.contains("declin") || low.contains("오류")) {
            return FAIL;
        }
        if (low.contains("paid") || low.contains("success") || low.contains("complete")
                || low.contains("authorized") || low.contains("settled") || low.contains("성공")) {
            return SUCCESS;
        }
        if (low.contains("pending") || low.contains("wait") || low.contains("request")
                || low.contains("processing") || low.contains("authorize") || low.contains("요청") || low.contains("대기")) {
            return OTHER;
        }
        return OTHER;
    }

    public static String normalizeCurrency(String cur) {
        if (cur == null || cur.isBlank()) {
            return "KRW";
        }
        return cur.trim().toUpperCase(Locale.ROOT);
    }

    public static BigDecimal parseMoney(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = String.valueOf(v).trim().replace(",", "");
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static Map<String, Object> buildBarPayload(
            boolean multiCurrency,
            String primaryCurrency,
            Map<String, Map<String, BigDecimal>> amountsByBucketCurrency,
            Map<String, Map<String, Long>> countByBucketAndCurrency,
            boolean partial,
            boolean showEmptyBuckets) {
        String primary = normalizeCurrency(primaryCurrency != null ? primaryCurrency : "KRW");
        List<String> order = List.of(SUCCESS, FAIL, VOID, REFUND, OTHER);
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (String key : order) {
            Map<String, BigDecimal> amts = amountsByBucketCurrency.getOrDefault(key, Collections.emptyMap());
            Map<String, Long> cnts = countByBucketAndCurrency.getOrDefault(key, Collections.emptyMap());
            long totalCount;
            Map<String, String> amountsPlain = new LinkedHashMap<>();
            if (multiCurrency) {
                totalCount = cnts.values().stream().mapToLong(Long::longValue).sum();
                List<String> curs = new ArrayList<>(amts.keySet());
                curs.sort(PayListStatusBarBuckets::currencySort);
                for (String c : curs) {
                    amountsPlain.put(c, stripTrailingZeros(amts.get(c)));
                }
            } else {
                totalCount = cnts.getOrDefault(primary, 0L);
                BigDecimal one = amts.getOrDefault(primary, BigDecimal.ZERO);
                amountsPlain.put(primary, stripTrailingZeros(one));
            }
            if (!showEmptyBuckets && totalCount <= 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("count", totalCount);
            row.put("amountsByCurrency", amountsPlain);
            buckets.add(row);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("multiCurrency", multiCurrency);
        meta.put("primaryCurrency", primary);
        meta.put("buckets", buckets);
        meta.put("partial", partial);
        meta.put("showEmptyBuckets", showEmptyBuckets);
        return meta;
    }

    private static int currencyRank(String c) {
        if (c == null || c.isBlank()) {
            return 999;
        }
        return switch (c.trim().toUpperCase(Locale.ROOT)) {
            case "KRW" -> 0;
            case "JPY" -> 1;
            case "USD" -> 2;
            case "EUR" -> 3;
            case "THB" -> 4;
            default -> 40;
        };
    }

    private static int currencySort(String a, String b) {
        int ra = currencyRank(a);
        int rb = currencyRank(b);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    public static String stripTrailingZeros(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        return v.stripTrailingZeros().toPlainString();
    }

    public static OrgLevel resolveViewerOrgLevel(AppUser user, OrgUnitRepository orgUnitRepository) {
        if (user == null || orgUnitRepository == null) {
            return null;
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        return orgUnitRepository.findByCode(code.trim()).map(OrgUnit::getOrgLevel).orElse(null);
    }

    public static String resolveViewerPrimaryCurrency(AppUser user,
                                                      OrgUnitRepository orgUnitRepository,
                                                      CommissionPolicyRepository commissionPolicyRepository) {
        if (commissionPolicyRepository == null) {
            return "KRW";
        }
        if (user == null) {
            return commissionPolicyRepository.findByScope("DEFAULT")
                    .map(CommissionPolicy::getCurrencyCode)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .orElse("KRW");
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return commissionPolicyRepository.findByScope("DEFAULT")
                    .map(CommissionPolicy::getCurrencyCode)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .orElse("KRW");
        }
        Optional<OrgUnit> ou = orgUnitRepository != null ? orgUnitRepository.findByCode(code.trim()) : Optional.empty();
        if (ou.isEmpty()) {
            return "KRW";
        }
        String comp = ou.get().getCode();
        return commissionPolicyRepository.findByScope(comp)
                .map(CommissionPolicy::getCurrencyCode)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .or(() -> commissionPolicyRepository.findByScope("DEFAULT")
                        .map(CommissionPolicy::getCurrencyCode)
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase(Locale.ROOT)))
                .orElse("KRW");
    }

    /** 집계용: 버킷 → 통화 → 합계·건수 */
    public static class MutableRollup {
        private final Map<String, Map<String, BigDecimal>> amounts = new LinkedHashMap<>();
        private final Map<String, Map<String, Long>> counts = new LinkedHashMap<>();

        public void add(String bucket, String currency, BigDecimal amt, long rowCount) {
            String b = bucket != null ? bucket : OTHER;
            String c = normalizeCurrency(currency);
            amounts.computeIfAbsent(b, k -> new TreeMap<>()).merge(c, amt, BigDecimal::add);
            counts.computeIfAbsent(b, k -> new TreeMap<>()).merge(c, rowCount, Long::sum);
        }

        public Map<String, Object> toPayload(boolean multiCurrency, String primaryCurrency, boolean partial) {
            return toPayload(multiCurrency, primaryCurrency, partial, true);
        }

        /** @param showEmptyBuckets 통합·결제내역 true = 건수 0 버킷도 표시, 변형 화면 false = 해당 건만 요약 */
        public Map<String, Object> toPayload(boolean multiCurrency, String primaryCurrency, boolean partial,
                                             boolean showEmptyBuckets) {
            return buildBarPayload(multiCurrency, primaryCurrency, amounts, counts, partial, showEmptyBuckets);
        }
    }
}
