package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.elementpay.ElementPayCredentials;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.LedgerZoneDisplayTag;
import com.pg.util.TrnTimeDualZoneDisplay;
import com.pg.util.ViewDisplayTimezoneResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ElementPay Client API. 송금은 반자동입니다. ICOPAY가 요청을 만들고,
 * 은행 이체 서명은 ElementPay 관리자 2차 인증에서 합니다.
 */
@Service
public class ElementPayClientApiService {

    private static final String SANDBOX_BASE = "https://api-sbox.elementpay.io";
    private static final String LIVE_BASE = "https://api.elementpay.io";
    private static final String USER_AGENT = "Mozilla/5.0 (ICOPAY; compatible)";
    private static final DateTimeFormatter EP_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter WALL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String MSG_NO_TOKEN =
            "ElementPay Client API 토큰이 없습니다. 결제대행사 설정의 Client API 토큰을 저장하세요.";
    private static final String MSG_BAD_TOKEN =
            "Client API 토큰이 올바르지 않습니다. 결제대행사 설정의 Client API 토큰을 확인하세요.";
    private static final String MSG_MERCHANT =
            "송금관리는 운영 계정에서만 사용할 수 있습니다.";

    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final PayListService payListService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public ElementPayClientApiService(PgAgencyRepository pgAgencyRepository,
                                      PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                      PayListService payListService) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.payListService = payListService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public PageResult<Map<String, Object>> searchPayments(int page, int size, String orderDir,
                                                          String fieldType, String keyword, String payDiv,
                                                          LocalDate from, LocalDate to,
                                                          Authentication authentication) {
        int pn = Math.max(page, 1);
        int ps = Math.min(Math.max(size, 1), 250);
        LocalDate tTo = to != null ? to : LocalDate.now();
        LocalDate tFrom = from != null ? from : tTo.minusDays(1);
        if (tFrom.isAfter(tTo)) {
            LocalDate swap = tFrom;
            tFrom = tTo;
            tTo = swap;
        }
        Session session = openSession();
        String filterQuery = paymentFilterQuery(fieldType, keyword, payDiv, tFrom, tTo, orderDir);
        List<Map<String, Object>> all = new ArrayList<>();
        List<PgTrnsctn> matched = new ArrayList<>();
        int fetchSize = 250;
        int maxRows = 1000;
        long reportedTotal = 0;
        boolean capped = false;
        for (int offset = 0; offset < maxRows; offset += fetchSize) {
            JsonNode root = getJson(session, "/clientApi/payments?" + filterQuery
                    + "&limit=" + fetchSize + "&offset=" + offset);
            reportedTotal = root.path("total").asLong(reportedTotal);
            JsonNode payments = root.path("payments");
            int rawCount = 0;
            if (payments.isArray()) {
                for (JsonNode p : payments) {
                    rawCount++;
                    Map<String, Object> row = toPaymentRow(p, all.size() + 1);
                    if (!matchesPayDiv(row, payDiv)) {
                        continue;
                    }
                    Object txn = row.remove("_matchedTxn");
                    if (txn instanceof PgTrnsctn pgTxn) {
                        matched.add(pgTxn);
                    }
                    all.add(row);
                }
            }
            if (rawCount < fetchSize || offset + rawCount >= reportedTotal) {
                break;
            }
            if (offset + fetchSize >= maxRows) {
                capped = true;
            }
        }
        int fromIdx = (pn - 1) * ps;
        List<Map<String, Object>> pageRows = new ArrayList<>();
        for (int i = fromIdx; i < Math.min(fromIdx + ps, all.size()); i++) {
            Map<String, Object> row = all.get(i);
            row.put("rowNo", i + 1);
            pageRows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setList(pageRows);
        out.setPage(pn);
        out.setSize(ps);
        long total = capped ? Math.max(reportedTotal, all.size()) : all.size();
        out.setTotalElements(total);
        out.setTotalPages(Math.max(1, (int) Math.ceil(total / (double) ps)));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("acquirer", "ELEMENTPAY");
        meta.putAll(payListService.buildIntegratedListSummary(matched, authentication));
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        if (capped) {
            meta.put("elementPaySearchCapped", Boolean.TRUE);
        }
        out.setMeta(meta);
        return out;
    }

    /**
     * 통합체크 — ElementPay 결제 조회를 전산 표준일 기준으로 묶어 JPAY 일별 행과 같은 형태로 돌려줍니다.
     */
    public Map<String, Object> buildDailyCheckRows(LocalDate from, LocalDate to,
                                                    String fieldType, String keyword, String payDiv) {
        return buildDailyCheckRows(from, to, fieldType, keyword, payDiv, null);
    }

    public Map<String, Object> buildDailyCheckRows(LocalDate from, LocalDate to,
                                                    String fieldType, String keyword, String payDiv,
                                                    ZoneId dayZoneOverride) {
        LocalDate tTo = to != null ? to : LocalDate.now();
        LocalDate tFrom = from != null ? from : tTo;
        if (tFrom.isAfter(tTo)) {
            LocalDate swap = tFrom;
            tFrom = tTo;
            tTo = swap;
        }
        ZoneId ledgerZone = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId dayZone = dayZoneOverride != null ? dayZoneOverride : ledgerZone;
        boolean shift = !dayZone.equals(ledgerZone);
        LocalDate fetchFrom = shift ? tFrom.minusDays(1) : tFrom;
        LocalDate fetchTo = shift ? tTo.plusDays(1) : tTo;
        Session session = openSession();
        String filterQuery = paymentFilterQuery(fieldType, keyword, payDiv, fetchFrom, fetchTo, "DESC");
        Map<LocalDate, DailyCheckAgg> byDay = new HashMap<>();
        int fetchSize = 250;
        int maxRows = 5000;
        boolean capped = false;
        long reportedTotal = 0;
        for (int offset = 0; offset < maxRows; offset += fetchSize) {
            JsonNode root = getJson(session, "/clientApi/payments?" + filterQuery
                    + "&limit=" + fetchSize + "&offset=" + offset);
            reportedTotal = root.path("total").asLong(reportedTotal);
            JsonNode payments = root.path("payments");
            int rawCount = 0;
            if (payments.isArray()) {
                for (JsonNode p : payments) {
                    rawCount++;
                    String mapped = mapPaymentStatus(p.path("status_id").asText(""));
                    if (!matchesDailyPayDiv(mapped, payDiv)) {
                        continue;
                    }
                    Instant at = parseInstant(text(p, "date"));
                    if (at == null) {
                        continue;
                    }
                    LocalDate day = at.atZone(dayZone).toLocalDate();
                    if (day.isBefore(tFrom) || day.isAfter(tTo)) {
                        continue;
                    }
                    DailyCheckAgg agg = byDay.computeIfAbsent(day, d -> new DailyCheckAgg());
                    String bucket = PayListStatusBarBuckets.bucketForPgStatus(mapped);
                    agg.total++;
                    agg.buckets.merge(bucket, 1L, Long::sum);
                    if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                        agg.approveThb = agg.approveThb.add(parseMoney(text(p, "amount")));
                    }
                }
            }
            if (rawCount < fetchSize || offset + rawCount >= reportedTotal) {
                break;
            }
            if (offset + fetchSize >= maxRows) {
                capped = true;
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = tFrom; !d.isAfter(tTo); d = d.plusDays(1)) {
            DailyCheckAgg agg = byDay.getOrDefault(d, new DailyCheckAgg());
            Map<String, Long> buckets = new LinkedHashMap<>();
            for (String key : PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER) {
                buckets.put(key, agg.buckets.getOrDefault(key, 0L));
            }
            Map<String, Object> fin = new LinkedHashMap<>();
            fin.put("approveByCurrency", Map.of("THB", PayListStatusBarBuckets.stripTrailingZeros(agg.approveThb)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("totalElements", agg.total);
            row.put("statusBucketCounts", buckets);
            row.put("meta", Map.of("payListFinancialSummary", fin));
            list.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("capped", capped);
        return out;
    }

    private static BigDecimal nodeMoney(JsonNode node, String... keys) {
        if (node == null || keys == null) {
            return BigDecimal.ZERO;
        }
        for (String key : keys) {
            JsonNode v = node.path(key);
            if (v.isMissingNode() || v.isNull()) {
                continue;
            }
            if (v.isNumber()) {
                return v.decimalValue();
            }
            String raw = v.asText("");
            if (!raw.isBlank()) {
                return parseMoney(raw);
            }
        }
        return BigDecimal.ZERO;
    }

    private static String moneyPlain(BigDecimal amount) {
        BigDecimal v = amount != null ? amount : BigDecimal.ZERO;
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static final class DailyCheckAgg {
        private long total;
        private final Map<String, Long> buckets = new HashMap<>();
        private BigDecimal approveThb = BigDecimal.ZERO;
    }

    private static String paymentFilterQuery(String fieldType, String keyword, String payDiv,
                                             LocalDate tFrom, LocalDate tTo, String orderDir) {
        StringBuilder q = new StringBuilder();
        q.append("filter[date_from]=").append(enc(tFrom.format(EP_DATE)));
        q.append("&filter[date_to]=").append(enc(tTo.format(EP_DATE)));
        if (orderDir == null || orderDir.isBlank() || "DESC".equalsIgnoreCase(orderDir.trim())) {
            q.append("&order=-");
        }
        String ft = fieldType != null ? fieldType.trim().toUpperCase(Locale.ROOT) : "";
        String kw = keyword != null ? keyword.trim() : "";
        if (!kw.isEmpty()) {
            if ("APPROVAL_NO".equals(ft) || (ft.isEmpty() && kw.chars().allMatch(Character::isDigit))) {
                q.append("&filter[id]=").append(enc(kw));
            } else if (!"MID".equals(ft) && !"ROUTE".equals(ft) && !"CUSTOMER_ID".equals(ft) && !"STATUS".equals(ft)) {
                q.append("&filter[order]=").append(enc(kw));
            }
        }
        if ("99".equals(payDiv)) {
            q.append("&filter[status_id]=204");
        } else if ("42".equals(payDiv) || "31".equals(payDiv) || "30".equals(payDiv)) {
            q.append("&filter[status_id]=207");
        }
        return q.toString();
    }

    public PageResult<Map<String, Object>> listTransfers(Authentication authentication, int page, int size) {
        assertOperator(authentication);
        int pn = Math.max(page, 1);
        int ps = Math.min(Math.max(size, 1), 250);
        Session session = openSession();
        String q = "limit=" + ps + "&offset=" + ((pn - 1L) * ps) + "&order=-";
        JsonNode root = getJson(session, "/clientApi/transfers?" + q);
        List<Map<String, Object>> rows = new ArrayList<>();
        JsonNode transfers = root.path("transfers");
        int rowNo = (pn - 1) * ps;
        if (transfers.isArray()) {
            for (JsonNode n : transfers) {
                rows.add(toTransferRow(n, ++rowNo));
            }
        }
        long total = root.path("total").asLong(rows.size());
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setList(rows);
        out.setPage(pn);
        out.setSize(ps);
        out.setTotalElements(total);
        out.setTotalPages(Math.max(1, (int) Math.ceil(total / (double) ps)));
        return out;
    }

    public Map<String, Object> listRecipients(Authentication authentication) {
        assertOperator(authentication);
        Session session = openSession();
        JsonNode root = getJson(session, "/clientApi/recipients?limit=250&offset=0&order=-");
        List<Map<String, Object>> rows = new ArrayList<>();
        JsonNode arr = root.path("recipients");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", text(n, "id"));
                row.put("name", text(n, "name"));
                row.put("type", text(n, "type"));
                row.put("lastDigits", text(n, "last_digits"));
                row.put("country", text(n, "country"));
                rows.add(row);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recipients", rows);
        return out;
    }

    /** 개인 수취인. 계좌관리가 ElementPay 등록 계좌를 기본 계좌로 가져올 때 씁니다. */
    public List<Map<String, Object>> listRecipientCards(Authentication authentication) {
        assertOperator(authentication);
        Session session = openSession();
        JsonNode root = getJson(session, "/clientApi/recipients?limit=250&offset=0&order=-");
        List<Map<String, Object>> rows = new ArrayList<>();
        JsonNode arr = root.path("recipients");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                String id = text(n, "id");
                if (id.isEmpty()) {
                    continue;
                }
                boolean deleted = n.path("deleted").asBoolean(false);
                boolean active = !deleted && (!n.path("active").isBoolean() || n.path("active").asBoolean(true));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", id);
                row.put("name", text(n, "name"));
                row.put("type", text(n, "type"));
                row.put("lastDigits", text(n, "last_digits"));
                row.put("country", text(n, "country"));
                row.put("active", active);
                rows.add(row);
            }
        }
        return rows;
    }

    public Map<String, Object> createPersonalRecipient(Authentication authentication, Map<String, String> body) {
        assertOperator(authentication);
        Map<String, String> in = body != null ? body : Map.of();
        Session session = openSession();
        String id = createRecipient(session, in);
        String account = trim(in.get("account")).replaceAll("\\s+", "");
        String last = account.length() >= 4 ? account.substring(account.length() - 4) : account;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", trim(in.get("name")));
        row.put("type", trim(in.get("type")).equalsIgnoreCase("company") ? "company" : "personal");
        row.put("lastDigits", last);
        row.put("country", trim(in.get("country")).toUpperCase(Locale.ROOT));
        row.put("active", Boolean.TRUE);
        return row;
    }

    public Map<String, Object> listBanks(Authentication authentication) {
        assertOperator(authentication);
        Session session = openSession();
        JsonNode root = getJson(session, "/clientApi/banks?limit=250&offset=0");
        List<Map<String, Object>> rows = new ArrayList<>();
        JsonNode arr = root.path("banks");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", n.path("id").asText(""));
                row.put("name", text(n, "name"));
                rows.add(row);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("banks", rows);
        return out;
    }

    /**
     * 캐비닛 Transfers 상단 잔액. creditHold=입금 보류, available=송금 가능, debitHold=송금 처리 중.
     */
    public Map<String, Object> balanceReport(Authentication authentication) {
        assertOperator(authentication);
        Session session = openSession();
        JsonNode root = getJson(session, "/clientApi/balance");
        BigDecimal payInHold = nodeMoney(root, "creditHold", "credit_hold");
        BigDecimal available = nodeMoney(root, "available");
        BigDecimal payOutHold = nodeMoney(root, "debitHold", "debit_hold");
        String currency = text(root, "currency");
        if (currency.isEmpty()) {
            currency = "THB";
        }
        BigDecimal total = payInHold.add(available).add(payOutHold);
        ZonedDateTime asOf = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).truncatedTo(ChronoUnit.SECONDS);
        String offset = asOf.getOffset().getId();
        if ("Z".equals(offset)) {
            offset = "+00:00";
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("payInHold", moneyPlain(payInHold));
        out.put("available", moneyPlain(available));
        out.put("payOutHold", moneyPlain(payOutHold));
        out.put("total", moneyPlain(total));
        out.put("currency", currency.toUpperCase(Locale.ROOT));
        out.put("asOf", asOf.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " (GMT" + offset + ")");
        return out;
    }

    public Map<String, Object> createTransfer(Authentication authentication, Map<String, String> body) {
        assertOperator(authentication);
        Map<String, String> in = body != null ? body : Map.of();
        Session session = openSession();
        String recipient = trim(in.get("recipientId"));
        if (recipient.isEmpty()) {
            throw new IllegalArgumentException("수취계좌를 선택하세요. 계좌는 본사설정 계좌관리에서 등록합니다.");
        }
        String orderRaw = trim(in.get("orderId"));
        if (orderRaw.isEmpty()) {
            orderRaw = "R" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        }
        String orderId = normalizeOrderId(orderRaw);
        BigDecimal amount = parseAmount(trim(in.get("amount")));
        String currency = trim(in.get("currency"));
        if (currency.isEmpty()) {
            currency = "THB";
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("통화는 3자리 코드입니다.");
        }
        currency = currency.toUpperCase(Locale.ROOT);
        assertAmountLeavesRemitFee(authentication, amount, currency);
        String description = trim(in.get("description"));
        if (description.length() > 255) {
            description = description.substring(0, 255);
        }
        if ("company".equalsIgnoreCase(trim(in.get("recipientType")))) {
            ObjectNode payout = objectMapper.createObjectNode();
            payout.put("amount", amount);
            JsonNode createdPayout = postJson(session, "/clientApi/payouts", payout);
            Map<String, Object> payoutRow = new LinkedHashMap<>();
            payoutRow.put("id", text(createdPayout, "id"));
            payoutRow.put("clientAmount", text(createdPayout, "client_amount"));
            payoutRow.put("clientCurrency", text(createdPayout, "client_currency"));
            payoutRow.put("message", "회사 지급계좌로 송금 요청을 등록했습니다. ElementPay 관리자에서 처리됩니다.");
            return payoutRow;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", recipient);
        payload.put("order_id", orderId);
        payload.put("amount", amount);
        payload.put("currency", currency);
        if (!description.isEmpty()) {
            payload.put("description", description);
        }
        JsonNode created = postJson(session, "/clientApi/transfers", payload);
        Map<String, Object> row = toTransferRow(created, 0);
        row.put("message", "송금 요청을 등록했습니다. ElementPay 관리자에서 2차 인증 서명이 필요합니다.");
        return row;
    }

    /** 송금 가능액에서 수수료 25를 남기지 못하면 신청을 거절합니다. */
    private void assertAmountLeavesRemitFee(Authentication authentication, BigDecimal amount, String currency) {
        BigDecimal fee = new BigDecimal("25.00");
        Map<String, Object> bal = balanceReport(authentication);
        String balCur = String.valueOf(bal.getOrDefault("currency", "THB")).trim().toUpperCase(Locale.ROOT);
        if (!balCur.equals(currency)) {
            throw new IllegalArgumentException("송금 신청 통화는 송금 가능액 통화와 같아야 합니다.");
        }
        BigDecimal available;
        try {
            available = new BigDecimal(String.valueOf(bal.get("available"))).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("송금 가능액을 확인한 뒤 다시 신청하세요.");
        }
        BigDecimal max = available.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        if (max.signum() < 0) {
            max = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (amount.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "송금 가능액 " + moneyPlain(available) + " " + balCur
                            + "에서 수수료 25를 뺀 금액만 신청할 수 있습니다. 최대 가능 금액은 "
                            + moneyPlain(max) + " " + balCur + "입니다.");
        }
    }

    private String createRecipient(Session session, Map<String, String> in) {
        String bankId = trim(in.get("bankId"));
        String account = trim(in.get("account")).replaceAll("\\s+", "");
        String name = trim(in.get("name"));
        String country = trim(in.get("country")).toUpperCase(Locale.ROOT);
        String citizenId = trim(in.get("citizenId"));
        String phone = trim(in.get("phone")).replaceAll("\\D", "");
        String type = trim(in.get("type")).toLowerCase(Locale.ROOT);
        if (!"company".equals(type)) {
            type = "personal";
        }
        String email = trim(in.get("email"));
        if (bankId.isEmpty() || account.isEmpty() || name.isEmpty()
                || country.isEmpty() || citizenId.isEmpty() || phone.isEmpty()) {
            throw new IllegalArgumentException("은행, 계좌번호, 예금주, 국가, 실명번호, 연락처를 입력하세요.");
        }
        if (phone.length() < 9 || phone.length() > 32) {
            throw new IllegalArgumentException("연락처는 숫자 9자리에서 32자리입니다.");
        }
        if (!account.chars().allMatch(Character::isDigit) || account.length() < 5 || account.length() > 19) {
            throw new IllegalArgumentException("계좌번호는 숫자 5자리에서 19자리입니다.");
        }
        if (country.length() != 2) {
            throw new IllegalArgumentException("국가는 2자리 코드입니다. 예: TH");
        }
        int bankNum;
        try {
            bankNum = Integer.parseInt(bankId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("은행을 선택하세요.");
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("bank_id", bankNum);
        payload.put("account", account);
        payload.put("name", name.length() > 64 ? name.substring(0, 64) : name);
        payload.put("type", type);
        payload.put("country", country);
        payload.put("citizen_id", citizenId);
        payload.put("phone", phone);
        payload.put("active", true);
        if (!email.isEmpty()) {
            payload.put("email", email.length() > 40 ? email.substring(0, 40) : email);
        }
        JsonNode created = postJson(session, "/clientApi/recipients", payload);
        String id = text(created, "id");
        if (id.isEmpty()) {
            throw new IllegalStateException("수취계좌를 만들었지만 식별번호가 없습니다.");
        }
        return id;
    }

    private Map<String, Object> toPaymentRow(JsonNode p, int rowNo) {
        String statusId = p.path("status_id").asText("");
        String mapped = mapPaymentStatus(statusId);
        String statusText = paymentStatusLabel(statusId, text(p, "status_name"));
        String order = text(p, "order");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rowNo", rowNo);
        m.put("transactionId", p.path("id").asText(""));
        m.put("orderNo", order);
        m.put("paymentChannel", text(p, "service_name"));
        m.put("amount", text(p, "amount"));
        m.put("fee", text(p, "fee"));
        m.put("totalAmount", text(p, "merchant_amount"));
        m.put("refundAmount", "");
        m.put("discount", "");
        boolean icopayCode = "10".equals(mapped) || "99".equals(mapped) || "42".equals(mapped);
        m.put("status", icopayCode ? mapped : statusText);
        m.put("description", statusText);
        m.put("currency", "THB");
        m.put("customer", "");
        m.put("merchant", "");
        m.put("routeNo", "");
        m.put("compNm", "");
        m.put("compId", "");
        m.put("settled", "");
        m.put("icopay", "N");
        applyPaymentTimes(m, text(p, "date"), text(p, "status_date"));
        if (!order.isEmpty()) {
            pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(order).ifPresent(txn -> enrichFromLocal(m, txn));
        }
        return m;
    }

    private void enrichFromLocal(Map<String, Object> m, PgTrnsctn txn) {
        m.put("icopay", "Y");
        m.put("_matchedTxn", txn);
        if (txn.getTrnId() != null) {
            m.put("trnId", txn.getTrnId());
        }
        if (txn.getStatus() != null && !txn.getStatus().isBlank()) {
            m.put("status", txn.getStatus().trim());
        }
        if (txn.getCustomerNm() != null) {
            m.put("customer", txn.getCustomerNm());
        }
        if (txn.getMerchantId() != null) {
            m.put("merchant", txn.getMerchantId());
            m.put("compId", txn.getMerchantId());
            orgUnitRepository.findByCode(txn.getMerchantId()).ifPresent(ou -> {
                if (ou.getName() != null) {
                    m.put("compNm", ou.getName());
                }
            });
        }
        if (txn.getRouteNo() != null) {
            m.put("routeNo", txn.getRouteNo());
        }
        if (txn.getSettledYn() != null) {
            m.put("settled", txn.getSettledYn());
        }
        String displayCur = txn.getDisplayCurType() != null ? txn.getDisplayCurType().trim() : "";
        String chargeCur = txn.getCurType() != null ? txn.getCurType().trim() : "";
        if (chargeCur.isEmpty()) {
            chargeCur = "THB";
        }
        m.put("currency", chargeCur);
        String chargeAmt = txn.getAmtKrw() != null ? txn.getAmtKrw().stripTrailingZeros().toPlainString() : "";
        if (!chargeAmt.isEmpty()) {
            m.put("amount", chargeAmt);
        }
        if (txn.getTotalAmt() != null) {
            m.put("totalAmount", txn.getTotalAmt().stripTrailingZeros().toPlainString());
        } else if (!chargeAmt.isEmpty()) {
            m.put("totalAmount", chargeAmt);
        }
        String inputCur = !displayCur.isEmpty() ? displayCur : chargeCur;
        String inputAmt = txn.getDisplayAmt() != null
                ? txn.getDisplayAmt().stripTrailingZeros().toPlainString()
                : String.valueOf(m.getOrDefault("amount", ""));
        if (!inputCur.isEmpty() && !inputAmt.isEmpty()) {
            m.put("displayPayCur", inputCur);
            m.put("displayPayAmt", inputAmt);
            m.put("displayPaySummary", inputCur + " " + inputAmt);
        }
    }

    /**
     * 통합내역 거래시간. 1줄은 통합결제 기준인 태국시간.
     * 2줄은 새로고침 옆 표시 시간대이고, 본사설정이면 전산 운영 시간대입니다.
     */
    private void applyPaymentTimes(Map<String, Object> m, String createdIso, String statusIso) {
        if (ViewDisplayTimezoneResolver.integratedCheckTimes()) {
            applyIntegratedCheckTimes(m, createdIso, statusIso);
            return;
        }
        ZoneId base = ZoneId.of("Asia/Bangkok");
        ZoneId compare = resolveIntegratedCompareZone();
        Instant createdAt = parseInstant(createdIso);
        Instant paidAt = parseInstant(statusIso);
        if (createdAt == null) {
            String created = formatWall(createdIso, base);
            m.put("transactionDate", created);
            if (created.length() >= 10) {
                m.put("trnDate", created.substring(0, 10));
                m.put("trnTime", created.length() > 11 ? created.substring(11) : "");
            } else {
                m.put("trnDate", "");
                m.put("trnTime", "");
            }
        } else {
            ZonedDateTime baseCreated = createdAt.atZone(base).truncatedTo(ChronoUnit.SECONDS);
            m.put("transactionDate", WALL.format(baseCreated));
            m.put("trnDate", baseCreated.toLocalDate().toString());
            m.put("trnTime", dualTimeOnly(createdAt, base, compare));
        }
        if (paidAt == null) {
            String statusAt = formatWall(statusIso, base);
            m.put("paymentDate", statusAt);
            m.put("payCompletedAt", statusAt);
        } else {
            m.put("paymentDate", WALL.format(paidAt.atZone(base).truncatedTo(ChronoUnit.SECONDS)));
            m.put("payCompletedAt", dualDateTime(paidAt, base, compare));
        }
    }

    /** 통합체크 상세. 1줄은 비교정렬(없으면 전산 표준), 2줄은 1줄 추가(없으면 운영 시간대). */
    private void applyIntegratedCheckTimes(Map<String, Object> m, String createdIso, String statusIso) {
        ZoneId ledger = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId base = ViewDisplayTimezoneResolver.currentCompareAlign().orElse(ledger);
        ZoneId extra = ViewDisplayTimezoneResolver.currentLine1Extra()
                .orElse(hqLedgerSysSettingsService.resolveOperationalDisplayZoneId());
        Instant createdAt = parseInstant(createdIso);
        Instant paidAt = parseInstant(statusIso);
        if (createdAt == null) {
            String created = formatWall(createdIso, base);
            m.put("transactionDate", created);
            if (created.length() >= 10) {
                m.put("trnDate", created.substring(0, 10));
                m.put("trnTime", created.length() > 11 ? created.substring(11) : "");
            } else {
                m.put("trnDate", "");
                m.put("trnTime", "");
            }
        } else {
            m.put("transactionDate", WALL.format(createdAt.atZone(base).truncatedTo(ChronoUnit.SECONDS)));
            m.put("trnDate", createdAt.atZone(base).toLocalDate().toString());
            m.put("trnTime", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(createdAt, base, extra));
        }
        if (paidAt == null) {
            String statusAt = formatWall(statusIso, base);
            m.put("paymentDate", statusAt);
            m.put("payCompletedAt", statusAt);
        } else {
            m.put("paymentDate", WALL.format(paidAt.atZone(base).truncatedTo(ChronoUnit.SECONDS)));
            m.put("payCompletedAt", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(paidAt, base, extra));
        }
    }

    private ZoneId resolveIntegratedCompareZone() {
        Optional<ZoneId> view = ViewDisplayTimezoneResolver.currentRequestOverride();
        if (view.isPresent()) {
            return view.get();
        }
        ZoneId operational = hqLedgerSysSettingsService.resolveOperationalDisplayZoneId();
        return operational != null ? operational : ZoneId.of("Asia/Tokyo");
    }

    private static String dualTimeOnly(Instant instant, ZoneId base, ZoneId compare) {
        LocalDateTime naive = LocalDateTime.ofInstant(instant, base);
        return TrnTimeDualZoneDisplay.formatConfigurableDualLineTimeOnly(
                naive, base, "TH", base, LedgerZoneDisplayTag.zoneIdToShortTag(compare), compare);
    }

    private static String dualDateTime(Instant instant, ZoneId base, ZoneId compare) {
        LocalDateTime naive = LocalDateTime.ofInstant(instant, base);
        return TrnTimeDualZoneDisplay.formatConfigurableDualLineDateTime(
                naive, base, "TH", base, LedgerZoneDisplayTag.zoneIdToShortTag(compare), compare);
    }

    /** 등록일시. 1줄은 태국, 2줄은 목록의 표시 시간대(본사설정이면 전산 운영 시간대). */
    private String formatTransferCreatedAt(String raw) {
        Instant created = parseTransferInstant(raw);
        if (created == null) {
            return raw == null ? "" : raw;
        }
        return dualDateTime(created, ZoneId.of("Asia/Bangkok"), resolveIntegratedCompareZone());
    }

    private static Instant parseTransferInstant(String raw) {
        Instant parsed = parseInstant(raw);
        if (parsed != null) {
            return parsed;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().replace('T', ' ');
        int dot = s.indexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        try {
            LocalDateTime wall = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return wall.atZone(ZoneId.of("Asia/Bangkok")).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        String s = iso.trim();
        try {
            return Instant.parse(s);
        } catch (Exception ignored) {
            // ISO instant 가 아니면 offset 시각으로 다시 읽습니다.
        }
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> toTransferRow(JsonNode n, int rowNo) {
        JsonNode bank = n.path("bank_account");
        String status = text(n, "status");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rowNo", rowNo);
        m.put("id", text(n, "id"));
        m.put("orderId", text(n, "order_id"));
        m.put("status", status);
        m.put("statusLabel", transferStatusLabel(status));
        m.put("recipient", text(n, "recipient"));
        m.put("holder", bank.path("holder").asText(""));
        m.put("bank", text(bank, "bank"));
        m.put("accountLast", text(bank, "last_digits"));
        m.put("clientAmount", n.path("client_amount").asText(""));
        m.put("clientCurrency", text(n, "client_currency"));
        m.put("amount", n.path("amount").asText(""));
        m.put("fee", n.path("fee").asText(""));
        m.put("currency", text(n, "currency"));
        m.put("createdAt", formatTransferCreatedAt(text(n, "created_at")));
        m.put("description", text(n, "description"));
        return m;
    }

    static String mapPaymentStatus(String statusId) {
        return switch (statusId != null ? statusId.trim() : "") {
            case "203", "205", "210" -> "10";
            case "204" -> "99";
            case "207" -> "42";
            case "208" -> "208";
            case "214", "206" -> "206";
            default -> "";
        };
    }

    static String paymentStatusLabel(String statusId, String statusName) {
        String mapped = mapPaymentStatus(statusId);
        if ("10".equals(mapped)) {
            return "성공";
        }
        if ("99".equals(mapped)) {
            return "실패";
        }
        if ("42".equals(mapped)) {
            return "환불";
        }
        if ("208".equals(mapped)) {
            return "분쟁";
        }
        String name = statusName != null ? statusName.trim().toLowerCase(Locale.ROOT) : "";
        if (name.contains("wait") || "206".equals(statusId) || "214".equals(statusId)) {
            return "대기";
        }
        if (name.contains("reject") || name.contains("fail")) {
            return "실패";
        }
        if (name.contains("refund")) {
            return "환불";
        }
        return statusName != null ? statusName : "";
    }

    static String transferStatusLabel(String status) {
        return switch (status != null ? status.trim().toLowerCase(Locale.ROOT) : "") {
            case "new" -> "요청";
            case "authorizing" -> "서명대기";
            case "processing" -> "처리중";
            case "sent" -> "송금중";
            case "paid" -> "송금완료";
            case "failed" -> "실패";
            case "rejected" -> "거절";
            case "deleted" -> "삭제";
            case "reversed" -> "반환";
            case "seized" -> "보류";
            default -> status != null ? status : "";
        };
    }

    private static boolean matchesDailyPayDiv(String mappedStatus, String payDiv) {
        if (payDiv == null || payDiv.isBlank()) {
            return true;
        }
        if ("30".equals(payDiv) || "31".equals(payDiv) || "42".equals(payDiv)) {
            return "42".equals(mappedStatus);
        }
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("status", mappedStatus);
        return matchesPayDiv(probe, payDiv);
    }

    private static boolean matchesPayDiv(Map<String, Object> row, String payDiv) {
        if (payDiv == null || payDiv.isBlank() || "10".equals(payDiv)) {
            if (payDiv == null || payDiv.isBlank()) {
                return true;
            }
            return "10".equals(String.valueOf(row.get("status")));
        }
        if ("20".equals(payDiv)) {
            return false;
        }
        return payDiv.equals(String.valueOf(row.get("status")));
    }

    public void assertRemittanceOperator(Authentication authentication) {
        assertOperator(authentication);
    }

    private void assertOperator(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return;
        }
        OrgUnit ou = orgUnitRepository.findByCode(code.trim()).orElse(null);
        if (ou != null && ou.getOrgLevel() == OrgLevel.MERCHANT) {
            throw new IllegalStateException(MSG_MERCHANT);
        }
    }

    private Session openSession() {
        PgAgency agency = null;
        PgAgency fallback = null;
        for (PgAgency a : pgAgencyRepository.findByUseYnOrderByPgCdAsc("Y")) {
            if (a.getPgCd() == null || !PgVendor.isElementPayFamily(a.getPgCd())) {
                continue;
            }
            if (fallback == null) {
                fallback = a;
            }
            if (!ElementPayCredentials.clientApiToken(a).isBlank()) {
                agency = a;
                break;
            }
        }
        if (agency == null) {
            agency = fallback;
        }
        if (agency == null) {
            throw new IllegalStateException("사용 중인 ElementPay 결제대행사가 없습니다.");
        }
        String token = ElementPayCredentials.clientApiToken(agency);
        if (token.isBlank()) {
            throw new IllegalStateException(MSG_NO_TOKEN);
        }
        boolean sandbox = !"N".equalsIgnoreCase(
                agency.getSandboxYn() != null ? agency.getSandboxYn().trim() : "Y");
        return new Session(sandbox ? SANDBOX_BASE : LIVE_BASE, token);
    }

    private JsonNode getJson(Session session, String pathAndQuery) {
        return exchange(session, HttpMethod.GET, pathAndQuery, null);
    }

    private JsonNode postJson(Session session, String path, ObjectNode body) {
        return exchange(session, HttpMethod.POST, path, body);
    }

    private JsonNode exchange(Session session, HttpMethod method, String pathAndQuery, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set("x-client-api-token", session.token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity;
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            } catch (Exception e) {
                throw new IllegalStateException("요청 본문을 만들지 못했습니다.");
            }
        } else {
            entity = new HttpEntity<>(headers);
        }
        String url = session.base + pathAndQuery;
        try {
            ResponseEntity<String> res = restTemplate.exchange(url, method, entity, String.class);
            return parseBody(res.getBody());
        } catch (HttpStatusCodeException e) {
            String raw = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 401) {
                throw new IllegalStateException(MSG_BAD_TOKEN);
            }
            JsonNode parsed = parseQuiet(raw);
            throw new IllegalStateException(errorText(parsed, raw));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("결제대행사 연결에 실패했습니다.");
        }
    }

    private JsonNode parseBody(String raw) {
        JsonNode root = parseQuiet(raw);
        if (root == null) {
            throw new IllegalStateException("결제대행사 응답을 읽지 못했습니다.");
        }
        if (root.has("error") && !root.get("error").isNull()) {
            throw new IllegalStateException(errorText(root, raw));
        }
        return root;
    }

    private JsonNode parseQuiet(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static String errorText(JsonNode root, String raw) {
        if (root != null && root.has("error")) {
            String code = root.path("error").path("code").asText("");
            String message = root.path("error").path("message").asText("");
            if ("4010".equals(code) || "401".equals(code)) {
                return MSG_BAD_TOKEN;
            }
            if (!message.isBlank()) {
                return "결제대행사에서 요청을 거절했습니다. " + message;
            }
        }
        if (raw != null && !raw.isBlank() && raw.length() < 300) {
            return "결제대행사에서 요청을 거절했습니다. " + raw;
        }
        return "결제대행사에서 요청을 거절했습니다.";
    }

    private static String formatWall(String iso, ZoneId zone) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            Instant instant = Instant.parse(iso.trim());
            ZonedDateTime z = instant.atZone(zone != null ? zone : ZoneId.of("Asia/Bangkok"));
            return WALL.format(z.truncatedTo(ChronoUnit.SECONDS));
        } catch (Exception e) {
            String s = iso.trim().replace('T', ' ');
            int dot = s.indexOf('.');
            if (dot > 0) {
                s = s.substring(0, dot);
            }
            if (s.endsWith("Z")) {
                s = s.substring(0, s.length() - 1);
            }
            return s.length() > 19 ? s.substring(0, 19) : s;
        }
    }

    private static String normalizeOrderId(String raw) {
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("주문번호가 필요합니다.");
        }
        if (raw.length() > 36 || !raw.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-')) {
            throw new IllegalArgumentException("주문번호는 영문, 숫자, 하이픈만 36자까지 사용할 수 있습니다.");
        }
        return raw;
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("송금 금액이 필요합니다.");
        }
        try {
            BigDecimal n = new BigDecimal(raw.replace(",", "")).setScale(2, RoundingMode.HALF_UP);
            if (n.signum() <= 0) {
                throw new IllegalArgumentException("송금 금액이 필요합니다.");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("송금 금액이 올바르지 않습니다.");
        }
    }

    private static String text(JsonNode n, String field) {
        if (n == null || field == null) {
            return "";
        }
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return "";
        }
        return v.asText("").trim();
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private record Session(String base, String token) {
    }
}
