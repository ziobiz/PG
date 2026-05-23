package com.pg.util;

import com.pg.entity.AppUser;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.OrgUnitRepository;

import java.math.BigDecimal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 결제내역·통합내역 상단 상태별(성공·실패·취소·무효·이메일무효·환불·강제환불·기타) 건수·통화별 금액 요약.
 */
public final class PayListStatusBarBuckets {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final String VOID = "VOID";
    /** 내부 22·41 및 칠페이 이메일무효 계열 — {@link #VOID}(21·40 등)과 구분 */
    public static final String EMAIL_VOID = "EMAIL_VOID";
    public static final String REFUND = "REFUND";
    /** 내부 상태 31 강제환불 */
    public static final String FORCE_REFUND = "FORCE_REFUND";
    /** ICOPAY 내부 취소(20) 및 칠페이 취소 계열 — {@link #OTHER} 에서 제외 */
    public static final String CANCEL = "CANCEL";
    public static final String OTHER = "OTHER";

    private PayListStatusBarBuckets() {
    }

    /**
     * 결제내역·통합내역 상단 상태바 및 일별통합/일별결제 집계 열 순서.
     * 성공·실패·취소·무효(21·40)·이메일무효(22·41)·환불(30·42)·강제환불(31)·기타.
     */
    public static final List<String> DEFAULT_STATUS_BAR_BUCKET_ORDER = List.of(
            SUCCESS, FAIL, CANCEL, VOID, EMAIL_VOID, REFUND, FORCE_REFUND, OTHER);

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
        if ("22".equals(s) || "41".equals(s)) {
            return EMAIL_VOID;
        }
        if ("21".equals(s) || "40".equals(s)) {
            return VOID;
        }
        if ("30".equals(s) || "42".equals(s)) {
            return REFUND;
        }
        if ("31".equals(s)) {
            return FORCE_REFUND;
        }
        if ("20".equals(s)) {
            return CANCEL;
        }
        return OTHER;
    }

    /** 결제내역·통합 리포트 상세 — 성공·실패·취소·무효 등 한글 표기 */
    public static String pgStatusDisplayLabel(String statusRaw) {
        if (statusRaw != null && "08".equals(statusRaw.trim())) {
            return "요청";
        }
        return switch (bucketForPgStatus(statusRaw)) {
            case SUCCESS -> "성공";
            case FAIL -> "실패";
            case CANCEL -> "취소";
            case VOID -> "무효";
            case EMAIL_VOID -> "이메일 무효";
            case REFUND -> "환불";
            case FORCE_REFUND -> "강제환불";
            default -> "기타";
        };
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
            return CANCEL;
        }
        if ("1".equals(raw) || "3".equals(raw) || "4".equals(raw)) {
            return FAIL;
        }
        if (low.contains("emailvoid") || low.contains("email_void") || low.contains("이메일무효") || low.contains("이메일 무효")) {
            return EMAIL_VOID;
        }
        if (low.contains("voided") || low.contains("void") || low.contains("무효")) {
            return VOID;
        }
        if (low.contains("refund") || low.contains("환불")) {
            return REFUND;
        }
        if (low.contains("cancel") || low.contains("cancelled") || low.contains("canceled") || low.contains("취소")) {
            return CANCEL;
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

    /**
     * 집계 키 정규화: ISO 4217 숫자(392·840 등)와 {@code JPY (392)} 형태를 알파 코드로 맞춥니다.
     * 그리드·노티매핑과 동일 계열({@code HqNotifyMappingService} 통화 폴백)입니다.
     */
    private static final Map<String, String> ISO4217_NUMERIC_TO_ALPHA = Map.ofEntries(
            Map.entry("036", "AUD"),
            Map.entry("124", "CAD"),
            Map.entry("156", "CNY"),
            Map.entry("344", "HKD"),
            Map.entry("360", "IDR"),
            Map.entry("392", "JPY"),
            Map.entry("410", "KRW"),
            Map.entry("458", "MYR"),
            Map.entry("608", "PHP"),
            Map.entry("702", "SGD"),
            Map.entry("682", "SAR"),
            Map.entry("554", "NZD"),
            Map.entry("756", "CHF"),
            Map.entry("764", "THB"),
            Map.entry("784", "AED"),
            Map.entry("826", "GBP"),
            Map.entry("840", "USD"),
            Map.entry("978", "EUR")
    );

    /**
     * {@code cur_type} 등에 저장된 ISO 4217 숫자(392·764…)와 표시 통화 알파(JPY·THB…) 검색을 연결합니다.
     * 알파·숫자·혼합 입력에서 DB와 동등 비교할 후보 문자열만 반환합니다(부분 검색은 {@link #buildCurTypeSearchPredicate}의 LIKE).
     */
    public static List<String> expandCurTypeSearchExactVariants(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        String t = raw.trim();
        String asAlpha = normalizeCurrency(t);
        if (asAlpha != null && !asAlpha.isBlank() && asAlpha.length() <= 8) {
            keys.add(asAlpha);
        }
        String digits = t.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            String upperD = digits;
            String key;
            if (upperD.length() < 3) {
                key = "0".repeat(3 - upperD.length()) + upperD;
            } else if (upperD.length() == 3) {
                key = upperD;
            } else {
                String norm = upperD.replaceFirst("^0+(?!$)", "");
                if (norm.length() > 3) {
                    key = norm.substring(norm.length() - 3);
                } else if (norm.length() < 3) {
                    key = "0".repeat(3 - norm.length()) + norm;
                } else {
                    key = norm;
                }
            }
            if (key.length() == 3) {
                keys.add(key);
                String a = ISO4217_NUMERIC_TO_ALPHA.get(key);
                if (a != null) {
                    keys.add(a);
                }
            }
        }
        if (t.matches("(?i)^[A-Za-z]{3}$")) {
            String u = t.toUpperCase(Locale.ROOT);
            for (Map.Entry<String, String> e : ISO4217_NUMERIC_TO_ALPHA.entrySet()) {
                if (e.getValue().equals(u)) {
                    keys.add(e.getKey());
                    break;
                }
            }
        }
        keys.removeIf(s -> s == null || s.isBlank());
        return new ArrayList<>(keys);
    }

    /** JPA: 통화 검색 — 표시 알파·ISO 숫자 어느 쪽으로 넣어도 {@code cur_type} 과 매칭 */
    public static Predicate buildCurTypeSearchPredicate(CriteriaBuilder cb, Path<String> curTypePath, String raw) {
        if (raw == null || raw.isBlank()) {
            return cb.conjunction();
        }
        String trimmed = raw.trim();
        List<Predicate> ors = new ArrayList<>();
        String escLike = escapeSqlLikeMeta(trimmed);
        String pat = "%" + escLike.toLowerCase(Locale.ROOT) + "%";
        ors.add(cb.and(cb.isNotNull(curTypePath), cb.like(cb.lower(curTypePath), pat, '\\')));
        for (String k : expandCurTypeSearchExactVariants(trimmed)) {
            ors.add(cb.equal(curTypePath, k));
        }
        return cb.or(ors.toArray(Predicate[]::new));
    }

    private static String escapeSqlLikeMeta(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 메모리 필터: 그리드 행의 통화 문자열이 검색어(알파/숫자)와 일치하는지 */
    public static boolean curTypeRowMatchesKeyword(Object curDb, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        if (curDb == null) {
            return false;
        }
        String db = String.valueOf(curDb).trim();
        if (db.isEmpty()) {
            return false;
        }
        String dbL = db.toLowerCase(Locale.ROOT);
        String kw = keyword.trim();
        String kLow = kw.toLowerCase(Locale.ROOT);
        if (dbL.contains(kLow)) {
            return true;
        }
        for (String v : expandCurTypeSearchExactVariants(kw)) {
            if (v != null && dbL.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeCurrency(String cur) {
        if (cur == null || cur.isBlank()) {
            return "KRW";
        }
        String s = cur.trim();
        if (s.matches("(?i)^[A-Za-z]+\\s+\\(\\d+\\)$")) {
            return s.replaceFirst("(?i)\\s+\\(\\d+\\)$", "").trim().toUpperCase(Locale.ROOT);
        }
        String upper = s.toUpperCase(Locale.ROOT);
        if (upper.matches("\\d+")) {
            String key;
            if (upper.length() < 3) {
                key = "0".repeat(3 - upper.length()) + upper;
            } else if (upper.length() == 3) {
                key = upper;
            } else {
                key = upper.replaceFirst("^0+(?!$)", "");
                if (key.length() > 3) {
                    key = key.substring(key.length() - 3);
                } else if (key.length() < 3) {
                    key = "0".repeat(3 - key.length()) + key;
                }
            }
            return ISO4217_NUMERIC_TO_ALPHA.getOrDefault(key, key);
        }
        return upper;
    }

    /** 업체 {@code baseCurrency} CSV → 정규화·중복 제거·순서 유지 */
    public static List<String> parseBaseCurrencyCsv(String baseCurrencyCsv) {
        if (baseCurrencyCsv == null || baseCurrencyCsv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : baseCurrencyCsv.split(",")) {
            if (p == null || p.isBlank()) {
                continue;
            }
            String n = normalizeCurrency(p.trim());
            if (!out.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * 로그인 조직 기준 표시 통화 순서. 총본사·본사는 CSV 전체, 총판·하위는 첫 통화만.
     * CSV가 비어 있으면 {@code fallbackPrimary}(수수료 정책 등) 한 종류만 사용합니다.
     */
    public static List<String> resolveDisplayCurrencyOrder(boolean multiCurrencyViewer,
                                                           String baseCurrencyCsv,
                                                           String fallbackPrimary) {
        String prim = normalizeCurrency(fallbackPrimary != null && !fallbackPrimary.isBlank() ? fallbackPrimary : "KRW");
        List<String> parsed = parseBaseCurrencyCsv(baseCurrencyCsv);
        if (parsed.isEmpty()) {
            return List.of(prim);
        }
        if (!multiCurrencyViewer) {
            return List.of(parsed.get(0));
        }
        return List.copyOf(parsed);
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
        return buildBarPayload(multiCurrency, primaryCurrency, amountsByBucketCurrency,
                countByBucketAndCurrency, partial, showEmptyBuckets, null);
    }

    /**
     * @param displayCurrencyOrder 다통화일 때 이 순서로만 열을 노출(조직 기준통화). null 이면 집계에 나온 통화를 기존 정렬로 표시.
     */
    public static Map<String, Object> buildBarPayload(
            boolean multiCurrency,
            String primaryCurrency,
            Map<String, Map<String, BigDecimal>> amountsByBucketCurrency,
            Map<String, Map<String, Long>> countByBucketAndCurrency,
            boolean partial,
            boolean showEmptyBuckets,
            List<String> displayCurrencyOrder) {
        return buildBarPayload(multiCurrency, primaryCurrency, amountsByBucketCurrency,
                countByBucketAndCurrency, partial, showEmptyBuckets, displayCurrencyOrder, null);
    }

    /**
     * @param bucketOrder 노출할 버킷 키 순서. null/빈값이면 {@link #DEFAULT_STATUS_BAR_BUCKET_ORDER}(8종).
     */
    public static Map<String, Object> buildBarPayload(
            boolean multiCurrency,
            String primaryCurrency,
            Map<String, Map<String, BigDecimal>> amountsByBucketCurrency,
            Map<String, Map<String, Long>> countByBucketAndCurrency,
            boolean partial,
            boolean showEmptyBuckets,
            List<String> displayCurrencyOrder,
            List<String> bucketOrder) {
        String primary = normalizeCurrency(primaryCurrency != null ? primaryCurrency : "KRW");
        List<String> bucketOrderEff = (bucketOrder != null && !bucketOrder.isEmpty())
                ? bucketOrder
                : DEFAULT_STATUS_BAR_BUCKET_ORDER;
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (String key : bucketOrderEff) {
            Map<String, BigDecimal> amts = amountsByBucketCurrency.getOrDefault(key, Collections.emptyMap());
            Map<String, Long> cnts = countByBucketAndCurrency.getOrDefault(key, Collections.emptyMap());
            long totalCount;
            Map<String, String> amountsPlain = new LinkedHashMap<>();
            Map<String, Long> countsByCurForRow = null;
            if (multiCurrency) {
                totalCount = cnts.values().stream().mapToLong(Long::longValue).sum();
                List<String> curs;
                if (displayCurrencyOrder != null && !displayCurrencyOrder.isEmpty()) {
                    curs = new ArrayList<>(displayCurrencyOrder);
                } else {
                    curs = new ArrayList<>(amts.keySet());
                    curs.sort(PayListStatusBarBuckets::currencySort);
                }
                for (String c : curs) {
                    amountsPlain.put(c, stripTrailingZeros(amts.getOrDefault(c, BigDecimal.ZERO)));
                }
                countsByCurForRow = new LinkedHashMap<>();
                for (String c : curs) {
                    countsByCurForRow.put(c, cnts.getOrDefault(c, 0L));
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
            if (countsByCurForRow != null) {
                row.put("countsByCurrency", countsByCurForRow);
            }
            buckets.add(row);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("multiCurrency", multiCurrency);
        meta.put("primaryCurrency", primary);
        meta.put("buckets", buckets);
        meta.put("partial", partial);
        meta.put("showEmptyBuckets", showEmptyBuckets);
        if (displayCurrencyOrder != null && !displayCurrencyOrder.isEmpty()) {
            meta.put("currencyOrder", new ArrayList<>(displayCurrencyOrder));
        }
        return meta;
    }

    private static int currencyRank(String c) {
        if (c == null || c.isBlank()) {
            return 999;
        }
        /* 기본 나열: 주요 통화 → KRW → 기타(알파벳) */
        return switch (c.trim().toUpperCase(Locale.ROOT)) {
            case "JPY" -> 0;
            case "USD" -> 1;
            case "THB" -> 2;
            case "EUR" -> 3;
            case "GBP" -> 4;
            case "SGD" -> 5;
            case "HKD" -> 6;
            case "CNY" -> 7;
            case "MYR" -> 8;
            case "CHF" -> 9;
            case "AUD" -> 10;
            case "NZD" -> 11;
            case "KRW" -> 12;
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
        return resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository, null);
    }

    /**
     * @param hqPayDisplayCurrencyAlpha 본사 전산설정 결제 통화(알파). 수수료 정책 등으로 통화가 없을 때 폴백. null·공백이면 KRW.
     */
    public static String resolveViewerPrimaryCurrency(AppUser user,
                                                      OrgUnitRepository orgUnitRepository,
                                                      CommissionPolicyRepository commissionPolicyRepository,
                                                      String hqPayDisplayCurrencyAlpha) {
        String fallback = normalizeCurrency(
                hqPayDisplayCurrencyAlpha != null && !hqPayDisplayCurrencyAlpha.isBlank()
                        ? hqPayDisplayCurrencyAlpha : "KRW");
        if (commissionPolicyRepository == null) {
            return fallback;
        }
        if (user == null) {
            return commissionPolicyRepository.findByScope("DEFAULT")
                    .map(CommissionPolicy::getCurrencyCode)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .orElse(fallback);
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return commissionPolicyRepository.findByScope("DEFAULT")
                    .map(CommissionPolicy::getCurrencyCode)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .orElse(fallback);
        }
        Optional<OrgUnit> ou = orgUnitRepository != null ? orgUnitRepository.findByCode(code.trim()) : Optional.empty();
        if (ou.isEmpty()) {
            return fallback;
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
                .orElse(fallback);
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
            return buildBarPayload(multiCurrency, primaryCurrency, amounts, counts, partial, showEmptyBuckets, null);
        }

        public Map<String, Object> toPayload(boolean multiCurrency, String primaryCurrency, boolean partial,
                                             boolean showEmptyBuckets, List<String> displayCurrencyOrder) {
            return buildBarPayload(multiCurrency, primaryCurrency, amounts, counts, partial, showEmptyBuckets,
                    displayCurrencyOrder, null);
        }

        public Map<String, Object> toPayload(boolean multiCurrency, String primaryCurrency, boolean partial,
                                             boolean showEmptyBuckets, List<String> displayCurrencyOrder,
                                             List<String> bucketOrder) {
            return buildBarPayload(multiCurrency, primaryCurrency, amounts, counts, partial, showEmptyBuckets,
                    displayCurrencyOrder, bucketOrder);
        }

        /**
         * 강제환불(31) 버킷을 일반 환불(30) 표시용 버킷에 합산 — 통합·URL 등 기본 화면용.
         */
        public void mergeBucketInto(String fromBucket, String toBucket) {
            if (fromBucket == null || toBucket == null || fromBucket.equals(toBucket)) {
                return;
            }
            Map<String, BigDecimal> fromAmt = amounts.remove(fromBucket);
            Map<String, Long> fromCnt = counts.remove(fromBucket);
            if (fromAmt != null) {
                Map<String, BigDecimal> toA = amounts.computeIfAbsent(toBucket, k -> new TreeMap<>());
                for (Map.Entry<String, BigDecimal> e : fromAmt.entrySet()) {
                    toA.merge(e.getKey(), e.getValue(), BigDecimal::add);
                }
            }
            if (fromCnt != null) {
                Map<String, Long> toC = counts.computeIfAbsent(toBucket, k -> new TreeMap<>());
                for (Map.Entry<String, Long> e : fromCnt.entrySet()) {
                    toC.merge(e.getKey(), e.getValue(), Long::sum);
                }
            }
        }
    }
}
