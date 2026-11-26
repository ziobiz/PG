package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ksnet.KsnetCredentials;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.TrnTimeDualZoneDisplay;
import com.pg.util.ViewDisplayTimezoneResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * KSNET 대사(WebFEP card/trade) · 통합내역·통합체크.
 * 기간 목록은 ICOPAY 적재 KSNET 거래를 기준으로 하고,
 * pgapi 가 있으면 단건 trade 조회로 카드·수수료·결제시각을 보강한다.
 * 입력통화(display*)는 결제내역(ICOPAY) 값을 사용한다.
 */
@Service
public class KsnetIntegratedListService {

    private static final DateTimeFormatter WALL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_SEARCH_ROWS = 5000;
    private static final int MAX_TRADE_ENRICH = 40;

    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final PayListService payListService;
    private final KsnetCardEnrichService ksnetCardEnrichService;

    public KsnetIntegratedListService(PgAgencyRepository pgAgencyRepository,
                                      PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                      PayListService payListService,
                                      KsnetCardEnrichService ksnetCardEnrichService) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.payListService = payListService;
        this.ksnetCardEnrichService = ksnetCardEnrichService;
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
        KsnetCredentials creds = resolveCredentials().orElse(null);
        List<PgTrnsctn> txns = loadKsnetTxns(tFrom, tTo);
        int[] tradeBudget = {MAX_TRADE_ENRICH};
        List<Map<String, Object>> all = new ArrayList<>();
        List<PgTrnsctn> matched = new ArrayList<>();
        for (PgTrnsctn t : txns) {
            if (all.size() >= MAX_SEARCH_ROWS) {
                break;
            }
            maybeEnrichTrade(t, creds, tradeBudget);
            Map<String, Object> row = toPaymentRow(t, all.size() + 1);
            if (!matchesKeyword(row, fieldType, keyword)) {
                continue;
            }
            if (!matchesPayDiv(row, payDiv)) {
                continue;
            }
            matched.add(t);
            all.add(row);
        }
        boolean asc = orderDir != null && "ASC".equalsIgnoreCase(orderDir.trim());
        all.sort(Comparator.comparing(
                (Map<String, Object> m) -> String.valueOf(m.getOrDefault("transactionDate", "")),
                asc ? Comparator.naturalOrder() : Comparator.reverseOrder()));

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
        out.setTotalElements(all.size());
        out.setTotalPages(Math.max(1, (int) Math.ceil(all.size() / (double) ps)));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("acquirer", "KSNET");
        meta.put("ksnetTradeApi", creds != null && creds.isTradeUsable());
        meta.put("ksnetReconPasswordSet", creds != null && creds.hasReconPassword());
        meta.putAll(payListService.buildIntegratedListSummary(matched, authentication));
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        out.setMeta(meta);
        return out;
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
        List<PgTrnsctn> txns = loadKsnetTxns(tFrom.minusDays(1), tTo.plusDays(1));
        Map<LocalDate, DailyAgg> byDay = new HashMap<>();
        for (PgTrnsctn t : txns) {
            LocalDateTime at = t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt();
            if (at == null) {
                continue;
            }
            LocalDate day = at.atZone(dayZone).toLocalDate();
            if (day.isBefore(tFrom) || day.isAfter(tTo)) {
                continue;
            }
            Map<String, Object> row = toPaymentRow(t, 0);
            if (!matchesKeyword(row, fieldType, keyword) || !matchesPayDiv(row, payDiv)) {
                continue;
            }
            DailyAgg agg = byDay.computeIfAbsent(day, d -> new DailyAgg());
            String mapped = str(row.get("status"));
            String bucket = PayListStatusBarBuckets.bucketForPgStatus(mapped);
            agg.total++;
            agg.buckets.merge(bucket, 1L, Long::sum);
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                String cur = PayListStatusBarBuckets.normalizeCurrency(str(row.get("currency")));
                if (cur.isBlank()) {
                    cur = "USD";
                }
                agg.approveByCur.merge(cur, parseMoney(str(row.get("amount"))), BigDecimal::add);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = tFrom; !d.isAfter(tTo); d = d.plusDays(1)) {
            DailyAgg agg = byDay.getOrDefault(d, new DailyAgg());
            Map<String, Long> buckets = new LinkedHashMap<>();
            for (String key : PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER) {
                buckets.put(key, agg.buckets.getOrDefault(key, 0L));
            }
            Map<String, String> approveByCurrency = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> e : agg.approveByCur.entrySet()) {
                approveByCurrency.put(e.getKey(), PayListStatusBarBuckets.stripTrailingZeros(e.getValue()));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("totalCount", agg.total);
            row.put("statusBuckets", buckets);
            row.put("approveByCurrency", approveByCurrency);
            list.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("capped", Boolean.FALSE);
        out.put("acquirer", "KSNET");
        return out;
    }

    private void maybeEnrichTrade(PgTrnsctn t, KsnetCredentials creds, int[] budget) {
        if (creds == null || budget == null || budget.length == 0 || budget[0] <= 0) {
            return;
        }
        if (!KsnetCardEnrichService.needsEnrichment(t)) {
            return;
        }
        if (!"10".equals(trim(t.getStatus())) && !needsCardOnly(t)) {
            return;
        }
        budget[0]--;
        try {
            ksnetCardEnrichService.enrichNow(t, creds);
        } catch (Exception ignored) {
            /* 목록 유지 */
        }
    }

    private static boolean needsCardOnly(PgTrnsctn t) {
        return t != null && (t.getCardPanDisplay() == null || t.getCardPanDisplay().isBlank()
                || "-".equals(t.getCardPanDisplay().trim()));
    }

    private List<PgTrnsctn> loadKsnetTxns(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay().minusNanos(1);
        List<PgTrnsctn> all = pgTrnsctnRepository.findForReportRange(fromDt, toDt, null);
        List<PgTrnsctn> out = new ArrayList<>();
        for (PgTrnsctn t : all) {
            if (t != null && PgVendor.isKsnetFamily(t.getVan())) {
                out.add(t);
            }
        }
        out.sort(Comparator.comparing(
                (PgTrnsctn t) -> t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private Map<String, Object> toPaymentRow(PgTrnsctn t, int rowNo) {
        Map<String, Object> m = new LinkedHashMap<>();
        String payRef = trim(t.getPayNo());
        String approval = trim(t.getApprovalNo());
        String displayTxn = !approval.isBlank() ? approval : payRef;
        if (displayTxn.isBlank()) {
            displayTxn = trim(t.getChillTransactionId());
        }
        m.put("rowNo", rowNo);
        m.put("transactionId", displayTxn);
        m.put("orderNo", trim(t.getOrderNo()));
        m.put("referenceNo", payRef);
        m.put("paymentChannel", "CARD");
        String amt = t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : "";
        m.put("amount", amt);
        m.put("fee", t.getChillFeeAmt() != null ? t.getChillFeeAmt().stripTrailingZeros().toPlainString() : "");
        m.put("totalAmount", t.getTotalAmt() != null ? t.getTotalAmt().stripTrailingZeros().toPlainString() : amt);
        m.put("refundAmount", "");
        m.put("discount", "");
        m.put("cardType", "");
        m.put("payCardNo", KsnetSaleRecordService.normalizeCardPanDisplay(trim(t.getCardPanDisplay())));
        String st = trim(t.getStatus());
        m.put("status", st);
        m.put("description", trim(t.getChillPaymentStatus()));
        String cur = trim(t.getCurType());
        m.put("currency", cur.isBlank() ? "USD" : cur);
        m.put("customer", trim(t.getCustomerNm()));
        m.put("merchant", trim(t.getMerchantId()));
        m.put("routeNo", t.getRouteNo() != null ? String.valueOf(t.getRouteNo()) : "");
        m.put("compId", trim(t.getMerchantId()));
        m.put("compNm", "");
        if (t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
            orgUnitRepository.findByCode(t.getMerchantId().trim()).ifPresent(ou -> {
                if (ou.getName() != null) {
                    m.put("compNm", ou.getName());
                }
            });
        }
        m.put("settled", trim(t.getSettledYn()));
        m.put("icopay", "Y");
        m.put("chillTransactionId", displayTxn);
        m.put("approvalNo", displayTxn);
        m.put("trnId", t.getTrnId());
        putDisplayCurrency(m, t, amt);
        LocalDateTime at = t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt();
        applyPaymentTimes(m, at);
        return m;
    }

    /** 입력통화 = ICOPAY 결제내역 display* (DP) — 대사 API 금액과 별개. */
    private static void putDisplayCurrency(Map<String, Object> m, PgTrnsctn t, String chargeAmt) {
        String displayCur = t.getDisplayCurType() != null ? t.getDisplayCurType().trim() : "";
        String chargeCur = str(m.get("currency"));
        String inputCur = !displayCur.isEmpty() ? displayCur : chargeCur;
        String inputAmt = t.getDisplayAmt() != null
                ? t.getDisplayAmt().stripTrailingZeros().toPlainString()
                : chargeAmt;
        if (!inputCur.isEmpty() && !inputAmt.isEmpty()) {
            m.put("displayPayCur", inputCur);
            m.put("displayPayAmt", inputAmt);
            m.put("displayPaySummary", inputCur + " " + inputAmt);
        } else {
            m.put("displayPayCur", "");
            m.put("displayPayAmt", "");
            m.put("displayPaySummary", "");
        }
    }

    private void applyPaymentTimes(Map<String, Object> m, LocalDateTime at) {
        if (at == null) {
            m.put("transactionDate", "");
            m.put("paymentDate", "");
            m.put("transactionTimeDual", "");
            m.put("trnDate", "");
            m.put("trnTime", "");
            m.put("payCompletedAt", "");
            return;
        }
        ZoneId ledger = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId compare = ViewDisplayTimezoneResolver.currentRequestOverride()
                .orElseGet(hqLedgerSysSettingsService::resolveOperationalDisplayZoneId);
        LocalDateTime wall = at.truncatedTo(ChronoUnit.SECONDS);
        String wallStr = wall.format(WALL);
        m.put("transactionDate", wallStr);
        m.put("paymentDate", wallStr);
        m.put("transactionTimeDual", TrnTimeDualZoneDisplay.formatDualLineDateTime(at, ledger));
        m.put("trnDate", wall.toLocalDate().toString());
        m.put("trnTime", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(
                at.atZone(ledger).toInstant(), ledger, compare));
        m.put("payCompletedAt", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(
                at.atZone(ledger).toInstant(), ledger, compare));
    }

    private Optional<KsnetCredentials> resolveCredentials() {
        return ksnetCardEnrichService.resolveCredentials().or(() -> {
            List<PgAgency> agencies = pgAgencyRepository.findAllByOrderByPgCdAsc();
            for (PgAgency a : agencies) {
                if (a == null || !PgVendor.isKsnetFamily(a.getPgCd())) {
                    continue;
                }
                KsnetCredentials c = KsnetCredentials.fromAgency(a);
                if (c.isUsable()) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        });
    }

    private static boolean matchesKeyword(Map<String, Object> row, String fieldType, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        String ft = fieldType != null ? fieldType.trim().toUpperCase(Locale.ROOT) : "ALL";
        return switch (ft) {
            case "ORDER_NO" -> str(row.get("orderNo")).toLowerCase(Locale.ROOT).contains(kw);
            case "APPROVAL_NO" -> str(row.get("approvalNo")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("transactionId")).toLowerCase(Locale.ROOT).contains(kw);
            case "MID", "COMP_ID" -> str(row.get("compId")).toLowerCase(Locale.ROOT).contains(kw);
            case "CUSTOMER_ID", "CUSTOMER_NAME" -> str(row.get("customer")).toLowerCase(Locale.ROOT).contains(kw);
            case "STATUS" -> str(row.get("status")).toLowerCase(Locale.ROOT).contains(kw);
            default -> str(row.get("orderNo")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("approvalNo")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("transactionId")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("compId")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("customer")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(row.get("payCardNo")).toLowerCase(Locale.ROOT).contains(kw);
        };
    }

    private static boolean matchesPayDiv(Map<String, Object> row, String payDiv) {
        if (payDiv == null || payDiv.isBlank()) {
            return true;
        }
        String want = payDiv.trim().toUpperCase(Locale.ROOT);
        String st = str(row.get("status")).toUpperCase(Locale.ROOT);
        if ("FAIL".equals(want)) {
            return "99".equals(st) || st.contains("FAIL");
        }
        return st.equals(want);
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class DailyAgg {
        long total;
        final Map<String, Long> buckets = new HashMap<>();
        final Map<String, BigDecimal> approveByCur = new HashMap<>();
    }
}
