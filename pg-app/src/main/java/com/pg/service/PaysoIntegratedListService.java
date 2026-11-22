package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.payso.PaysoApiClient;
import com.pg.integration.pg.payso.PaysoCredentials;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.TrnTimeDualZoneDisplay;
import com.pg.util.ViewDisplayTimezoneResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * PAYSO Inquiry API → 통합내역·통합체크 일별 집계.
 * ElementPay Client API 경로와 동일 UX(그리드 필드·icopay Y/N·일자 버킷).
 */
@Service
public class PaysoIntegratedListService {

    private static final DateTimeFormatter PAYSO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter WALL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 100;
    private static final int MAX_SEARCH_ROWS = 1000;
    private static final int MAX_DAILY_ROWS = 5000;

    /** PAYSO 포털 MDR 기본(Inquiry에 fee 없을 때 통합내역 표시용). */
    private static final BigDecimal PAYSO_DEFAULT_FEE_RATE = new BigDecimal("0.028");

    private final PaysoApiClient paysoApiClient;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final PayListService payListService;
    private final PaysoSaleRecordService paysoSaleRecordService;

    public PaysoIntegratedListService(PaysoApiClient paysoApiClient,
                                      PgAgencyRepository pgAgencyRepository,
                                      PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                      PayListService payListService,
                                      PaysoSaleRecordService paysoSaleRecordService) {
        this.paysoApiClient = paysoApiClient;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.payListService = payListService;
        this.paysoSaleRecordService = paysoSaleRecordService;
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
        PaysoCredentials creds = resolveInquiryCredentials();
        FetchResult fetched = fetchInquiryRows(creds, tFrom, tTo, MAX_SEARCH_ROWS);
        List<Map<String, Object>> all = new ArrayList<>();
        List<PgTrnsctn> matched = new ArrayList<>();
        int[] cardDetailBudget = {30};
        for (Map<String, Object> raw : fetched.rows) {
            Map<String, Object> row = toPaymentRow(raw, all.size() + 1, creds, cardDetailBudget);
            if (!matchesKeyword(row, fieldType, keyword)) {
                continue;
            }
            if (!matchesPayDiv(row, payDiv)) {
                continue;
            }
            Object txn = row.remove("_matchedTxn");
            if (txn instanceof PgTrnsctn pgTxn) {
                matched.add(pgTxn);
            }
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
        long total = fetched.capped ? Math.max(fetched.reportedTotal, all.size()) : all.size();
        out.setTotalElements(total);
        out.setTotalPages(Math.max(1, (int) Math.ceil(total / (double) ps)));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("acquirer", "PAYSO");
        meta.putAll(payListService.buildIntegratedListSummary(matched, authentication));
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        if (fetched.capped) {
            meta.put("paysoSearchCapped", Boolean.TRUE);
        }
        out.setMeta(meta);
        return out;
    }

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

        PaysoCredentials creds = resolveInquiryCredentials();
        FetchResult fetched = fetchInquiryRows(creds, fetchFrom, fetchTo, MAX_DAILY_ROWS);
        Map<LocalDate, DailyCheckAgg> byDay = new HashMap<>();
        for (Map<String, Object> raw : fetched.rows) {
            String mapped = mapPaysoStatus(
                    str(raw.get("statusCode")), str(raw.get("statusDesc")));
            if (!matchesDailyPayDiv(mapped, payDiv)) {
                continue;
            }
            if (!matchesKeywordRaw(raw, fieldType, keyword)) {
                continue;
            }
            Instant at = parseInstant(str(raw.get("orderDateTime")));
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
                String cur = PayListStatusBarBuckets.normalizeCurrency(str(raw.get("currencyCode")));
                if (cur.isBlank()) {
                    cur = "THB";
                }
                agg.approveByCur.merge(cur, parseMoney(str(raw.get("amount"))), BigDecimal::add);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = tFrom; !d.isAfter(tTo); d = d.plusDays(1)) {
            DailyCheckAgg agg = byDay.getOrDefault(d, new DailyCheckAgg());
            Map<String, Long> buckets = new LinkedHashMap<>();
            for (String key : PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER) {
                buckets.put(key, agg.buckets.getOrDefault(key, 0L));
            }
            Map<String, String> approveByCurrency = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> e : agg.approveByCur.entrySet()) {
                approveByCurrency.put(e.getKey(), PayListStatusBarBuckets.stripTrailingZeros(e.getValue()));
            }
            Map<String, Object> fin = new LinkedHashMap<>();
            fin.put("approveByCurrency", approveByCurrency);
            fin.put("paymentByCurrency", approveByCurrency);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("totalElements", agg.total);
            row.put("statusBucketCounts", buckets);
            row.put("meta", Map.of("payListFinancialSummary", fin));
            list.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("capped", fetched.capped);
        return out;
    }

    private FetchResult fetchInquiryRows(PaysoCredentials creds, LocalDate from, LocalDate to, int maxRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        long reportedTotal = 0;
        boolean capped = false;
        String start = from.atStartOfDay().format(PAYSO_DT);
        String end = to.atTime(23, 59, 59).format(PAYSO_DT);
        for (int page = 1; rows.size() < maxRows; page++) {
            Map<String, Object> resp = paysoApiClient.inquiryTransaction(
                    creds, "ALL", "", start, end, page, PAGE_SIZE);
            if (!Boolean.TRUE.equals(resp.get("ok"))) {
                String msg = resp.get("message") != null ? String.valueOf(resp.get("message"))
                        : "Payso Inquiry 조회에 실패했습니다.";
                throw new IllegalStateException(msg);
            }
            reportedTotal = resp.get("totalRecords") instanceof Number n
                    ? n.longValue() : reportedTotal;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pageRows = resp.get("data") instanceof List<?> l
                    ? (List<Map<String, Object>>) l : List.of();
            if (pageRows.isEmpty()) {
                break;
            }
            for (Map<String, Object> r : pageRows) {
                rows.add(r);
                if (rows.size() >= maxRows) {
                    capped = true;
                    break;
                }
            }
            if (pageRows.size() < PAGE_SIZE) {
                break;
            }
            if (reportedTotal > 0 && rows.size() >= reportedTotal) {
                break;
            }
            if (page * PAGE_SIZE >= maxRows) {
                capped = true;
                break;
            }
        }
        return new FetchResult(rows, reportedTotal, capped);
    }

    private PaysoCredentials resolveInquiryCredentials() {
        List<PgAgency> agencies = pgAgencyRepository.findAllByOrderByPgCdAsc();
        PaysoCredentials sandboxFallback = null;
        for (PgAgency a : agencies) {
            if (a == null || !PgVendor.isPaysoFamily(a.getPgCd())) {
                continue;
            }
            PaysoCredentials c = PaysoCredentials.fromAgency(a);
            if (!c.isInquiryUsable()) {
                continue;
            }
            if (!c.sandbox()) {
                return c;
            }
            if (sandboxFallback == null) {
                sandboxFallback = c;
            }
        }
        if (sandboxFallback != null) {
            return sandboxFallback;
        }
        throw new IllegalStateException(
                "Payso Inquiry API 키가 없습니다. 결제대행사 설정에서 PAYSO(또는 PAYSO JPDP) apiKey·secretKey·merchantId를 저장하세요.");
    }

    private Map<String, Object> toPaymentRow(Map<String, Object> raw, int rowNo) {
        return toPaymentRow(raw, rowNo, null, null);
    }

    private Map<String, Object> toPaymentRow(Map<String, Object> raw, int rowNo,
                                            PaysoCredentials creds, int[] cardDetailBudget) {
        String statusCode = str(raw.get("statusCode"));
        String statusDesc = str(raw.get("statusDesc"));
        String mapped = mapPaysoStatus(statusCode, statusDesc);
        String statusText = paymentStatusLabel(mapped, statusDesc);
        String orderNo = str(raw.get("orderNo"));
        String referenceNo = str(raw.get("referenceNo"));
        String transactionId = str(raw.get("transactionId"));
        /* 승인번호 표시 = Reference(숫자 12자리)만. hex·PG Order No 혼용 금지 */
        String displayTxn = PaysoSaleRecordService.isPaysoReferenceNo(referenceNo) ? referenceNo.trim()
                : (PaysoSaleRecordService.isPaysoReferenceNo(transactionId) ? transactionId.trim()
                : (PaysoSaleRecordService.isPaysoReferenceNo(orderNo) ? orderNo.trim() : referenceNo));
        if (displayTxn.isEmpty()) {
            displayTxn = !referenceNo.isEmpty() ? referenceNo
                    : (!transactionId.isEmpty() ? transactionId : orderNo);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rowNo", rowNo);
        m.put("transactionId", displayTxn);
        m.put("orderNo", orderNo);
        m.put("referenceNo", referenceNo);
        m.put("paymentChannel", str(raw.get("channelCode")));
        m.put("amount", str(raw.get("amount")));
        String feeDisp = resolvePaysoFeeDisplay(raw, str(raw.get("amount")), mapped);
        m.put("fee", feeDisp);
        m.put("totalAmount", str(raw.get("amount")));
        m.put("refundAmount", "");
        m.put("discount", "");
        m.put("cardType", str(raw.get("cardType")));
        m.put("payCardNo", PaysoSaleRecordService.normalizePaysoCardMask(str(raw.get("cardNo"))));
        boolean icopayCode = "10".equals(mapped) || "99".equals(mapped) || "42".equals(mapped) || "20".equals(mapped);
        m.put("status", icopayCode ? mapped : statusText);
        m.put("description", statusText);
        String cur = str(raw.get("currencyCode"));
        m.put("currency", cur.isEmpty() ? "THB" : cur);
        m.put("customer", "");
        m.put("merchant", "");
        m.put("routeNo", "");
        m.put("compNm", "");
        m.put("compId", "");
        m.put("settled", "");
        m.put("icopay", "N");
        /* 통합내역 승인번호 = Reference(ICOPAY→PAYSO 조인 키) */
        m.put("chillTransactionId", displayTxn);
        m.put("approvalNo", displayTxn);
        String when = str(raw.get("orderDateTime"));
        applyPaymentTimes(m, when, when);
        findLocalTxn(referenceNo, orderNo, transactionId).ifPresent(txn -> {
            enrichFromLocal(m, txn);
            maybeFillCardFromOrderDetail(raw, m, creds, cardDetailBudget, referenceNo, orderNo);
            if (str(m.get("fee")).isEmpty() && !feeDisp.isEmpty()) {
                m.put("fee", feeDisp);
            }
            String feeForPersist = !str(m.get("fee")).isEmpty() ? str(m.get("fee")) : feeDisp;
            persistInquiryEnrichment(txn, raw, feeForPersist);
        });
        return m;
    }

    private void maybeFillCardFromOrderDetail(Map<String, Object> raw, Map<String, Object> m,
                                              PaysoCredentials creds, int[] cardDetailBudget,
                                              String referenceNo, String orderNo) {
        if (creds == null || cardDetailBudget == null || cardDetailBudget.length == 0 || cardDetailBudget[0] <= 0) {
            return;
        }
        boolean needCard = str(m.get("payCardNo")).isEmpty() && str(raw.get("cardNo")).isEmpty();
        boolean needFee = str(m.get("fee")).isEmpty() && str(raw.get("fee")).isEmpty()
                && str(raw.get("feePercent")).isEmpty();
        if (!needCard && !needFee) {
            return;
        }
        try {
            cardDetailBudget[0]--;
            Map<String, Object> detail = paysoApiClient.orderDetail(creds, referenceNo, orderNo);
            if (!Boolean.TRUE.equals(detail.get("ok"))) {
                return;
            }
            String card = PaysoSaleRecordService.normalizePaysoCardMask(str(detail.get("cardNo")));
            if (!card.isEmpty()) {
                m.put("payCardNo", card);
                raw.put("cardNo", card);
            }
            String feePct = str(detail.get("feePercent"));
            if (!feePct.isEmpty()) {
                raw.put("feePercent", feePct);
                String amt = str(m.get("amount"));
                if (str(m.get("fee")).isEmpty()) {
                    String computed = resolvePaysoFeeDisplay(raw, amt, str(m.get("status")));
                    if (!computed.isEmpty()) {
                        m.put("fee", computed);
                        raw.put("fee", computed);
                    }
                }
            }
            String fee = str(detail.get("fee"));
            if (!fee.isEmpty() && str(m.get("fee")).isEmpty()) {
                m.put("fee", fee);
                raw.put("fee", fee);
            }
            String ref = str(detail.get("referenceNo"));
            if (!ref.isEmpty() && str(raw.get("referenceNo")).isEmpty()) {
                raw.put("referenceNo", ref);
            }
            String pgOrder = str(detail.get("orderNo"));
            if (!pgOrder.isEmpty() && str(raw.get("orderNo")).isEmpty()) {
                raw.put("orderNo", pgOrder);
            }
        } catch (Exception ignored) {
            /* 목록 유지 */
        }
    }

    private static String resolvePaysoFeeDisplay(Map<String, Object> raw, String amountStr, String mappedStatus) {
        String fee = str(raw.get("fee"));
        if (!fee.isEmpty()) {
            return fee;
        }
        String pct = str(raw.get("feePercent"));
        BigDecimal amt = parseDecimal(amountStr);
        if (amt != null && !pct.isEmpty()) {
            BigDecimal rate = parseDecimal(pct);
            if (rate != null) {
                if (rate.compareTo(BigDecimal.ONE) > 0) {
                    rate = rate.movePointLeft(2);
                }
                return amt.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
        }
        /* Inquiry에 fee 없으면 포털과 동일 MDR 2.8%로 표시(성공·완료 건) */
        if (amt != null && ("10".equals(mappedStatus) || "CP".equalsIgnoreCase(str(raw.get("statusCode")))
                || str(raw.get("statusDesc")).toUpperCase(Locale.ROOT).contains("COMPLETE")
                || str(raw.get("statusDesc")).toUpperCase(Locale.ROOT).contains("SUCCESS")
                || str(raw.get("statusDesc")).toUpperCase(Locale.ROOT).contains("PAID"))) {
            return amt.multiply(PAYSO_DEFAULT_FEE_RATE).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return "";
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private void persistInquiryEnrichment(PgTrnsctn txn, Map<String, Object> raw, String feeDisp) {
        try {
            BigDecimal feeBd = parseDecimal(feeDisp);
            if (feeBd == null) {
                feeBd = parseDecimal(str(raw.get("fee")));
            }
            paysoSaleRecordService.applyInquiryEnrichment(
                    txn,
                    str(raw.get("referenceNo")),
                    str(raw.get("orderNo")),
                    str(raw.get("transactionId")),
                    str(raw.get("cardNo")),
                    feeBd);
        } catch (Exception ignored) {
            /* 목록 조회는 유지 */
        }
    }

    private Optional<PgTrnsctn> findLocalTxn(String referenceNo, String orderNo, String transactionId) {
        if (!referenceNo.isBlank()) {
            Optional<PgTrnsctn> byRef = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(referenceNo);
            if (byRef.isPresent()) {
                return byRef;
            }
            byRef = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(referenceNo);
            if (byRef.isPresent()) {
                return byRef;
            }
        }
        if (!transactionId.isBlank() && !transactionId.equals(referenceNo)) {
            Optional<PgTrnsctn> byTxn = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(transactionId);
            if (byTxn.isPresent()) {
                return byTxn;
            }
            byTxn = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(transactionId);
            if (byTxn.isPresent()) {
                return byTxn;
            }
        }
        if (!orderNo.isBlank()) {
            return pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
        }
        return Optional.empty();
    }

    private void enrichFromLocal(Map<String, Object> m, PgTrnsctn txn) {
        m.put("icopay", "Y");
        m.put("_matchedTxn", txn);
        if (txn.getTrnId() != null) {
            m.put("trnId", txn.getTrnId());
        }
        /* 승인번호 = pay_no(Reference) 우선 — 결제내역 1안과 동일 */
        String pay = txn.getPayNo() != null ? txn.getPayNo().trim() : "";
        String order = txn.getOrderNo() != null ? txn.getOrderNo().trim() : "";
        if (!pay.isBlank() && !pay.equalsIgnoreCase(order)) {
            m.put("chillTransactionId", pay);
            m.put("approvalNo", pay);
            m.put("transactionId", pay);
        }
        if (txn.getOrderNo() != null && !txn.getOrderNo().isBlank()) {
            /* 가맹 주문 — PG Order No 와 혼동 금지: 로컬 매칭 시 가맹 order 로 표시 */
            m.put("merchantOrderNo", txn.getOrderNo().trim());
        }
        if (txn.getStatus() != null && !txn.getStatus().isBlank()) {
            m.put("status", txn.getStatus().trim());
        }
        if (txn.getCustomerNm() != null) {
            m.put("customer", txn.getCustomerNm());
        }
        if (txn.getCardPanDisplay() != null && !txn.getCardPanDisplay().isBlank()) {
            String localCard = txn.getCardPanDisplay().trim();
            if (str(m.get("payCardNo")).isEmpty()) {
                m.put("payCardNo", localCard);
            }
        }
        if (txn.getChillFeeAmt() != null && str(m.get("fee")).isEmpty()) {
            m.put("fee", txn.getChillFeeAmt().stripTrailingZeros().toPlainString());
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
            chargeCur = String.valueOf(m.getOrDefault("currency", "THB"));
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
            m.put("trnTime", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(createdAt, base, compare));
        }
        if (paidAt == null) {
            String statusAt = formatWall(statusIso, base);
            m.put("paymentDate", statusAt);
            m.put("payCompletedAt", statusAt);
        } else {
            m.put("paymentDate", WALL.format(paidAt.atZone(base).truncatedTo(ChronoUnit.SECONDS)));
            m.put("payCompletedAt", TrnTimeDualZoneDisplay.formatInstantDualTimeOnly(paidAt, base, compare));
        }
    }

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
        return view.orElseGet(hqLedgerSysSettingsService::resolveOperationalDisplayZoneId);
    }

    static String mapPaysoStatus(String statusCode, String statusDesc) {
        String c = statusCode != null ? statusCode.trim().toUpperCase(Locale.ROOT) : "";
        String d = statusDesc != null ? statusDesc.trim().toUpperCase(Locale.ROOT) : "";
        if (c.equals("CP") || c.equals("COMPLETED") || c.equals("SUCCESS") || c.equals("OK")
                || d.contains("COMPLETE") || d.contains("SUCCESS") || d.equals("PAID") || d.contains(" APPROVED")) {
            return "10";
        }
        if (c.equals("RF") || c.equals("REFUND") || c.equals("REFUNDED")
                || d.contains("REFUND")) {
            return "42";
        }
        if (c.equals("CN") || c.equals("CANCEL") || c.equals("CANCELLED") || c.equals("CANCELED")
                || d.contains("CANCEL")) {
            return "20";
        }
        if (c.equals("FL") || c.equals("FAIL") || c.equals("FAILED") || c.equals("DECLINED") || c.equals("REJECT")
                || d.contains("FAIL") || d.contains("DECLIN") || d.contains("REJECT")) {
            return "99";
        }
        return "";
    }

    static String paymentStatusLabel(String mapped, String statusDesc) {
        if ("10".equals(mapped)) {
            return "성공";
        }
        if ("99".equals(mapped)) {
            return "실패";
        }
        if ("42".equals(mapped)) {
            return "환불";
        }
        if ("20".equals(mapped)) {
            return "취소";
        }
        return statusDesc != null && !statusDesc.isBlank() ? statusDesc.trim() : "";
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
        if (payDiv == null || payDiv.isBlank()) {
            return true;
        }
        if ("10".equals(payDiv)) {
            return "10".equals(String.valueOf(row.get("status")));
        }
        if ("FAIL".equalsIgnoreCase(payDiv) || "99".equals(payDiv)) {
            return "99".equals(String.valueOf(row.get("status")));
        }
        if ("30".equals(payDiv) || "31".equals(payDiv) || "42".equals(payDiv)) {
            return "42".equals(String.valueOf(row.get("status")));
        }
        if ("20".equals(payDiv)) {
            return "20".equals(String.valueOf(row.get("status")));
        }
        return payDiv.equals(String.valueOf(row.get("status")));
    }

    private static boolean matchesKeyword(Map<String, Object> row, String fieldType, String keyword) {
        String kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) {
            return true;
        }
        String ft = fieldType != null ? fieldType.trim().toUpperCase(Locale.ROOT) : "ALL";
        String orderNo = str(row.get("orderNo"));
        String txnId = str(row.get("transactionId"));
        String ref = str(row.get("referenceNo"));
        String mid = str(row.get("compId"));
        String st = str(row.get("status"));
        return switch (ft) {
            case "ORDER_NO" -> containsIgnore(orderNo, kw);
            case "APPROVAL_NO" -> containsIgnore(txnId, kw) || containsIgnore(ref, kw);
            case "MID", "COMP_ID" -> containsIgnore(mid, kw);
            case "STATUS" -> containsIgnore(st, kw) || containsIgnore(str(row.get("description")), kw);
            default -> containsIgnore(orderNo, kw) || containsIgnore(txnId, kw)
                    || containsIgnore(ref, kw) || containsIgnore(mid, kw)
                    || containsIgnore(str(row.get("customer")), kw);
        };
    }

    private static boolean matchesKeywordRaw(Map<String, Object> raw, String fieldType, String keyword) {
        String kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) {
            return true;
        }
        String ft = fieldType != null ? fieldType.trim().toUpperCase(Locale.ROOT) : "ALL";
        String orderNo = str(raw.get("orderNo"));
        String txnId = str(raw.get("transactionId"));
        String ref = str(raw.get("referenceNo"));
        return switch (ft) {
            case "ORDER_NO" -> containsIgnore(orderNo, kw);
            case "APPROVAL_NO" -> containsIgnore(txnId, kw) || containsIgnore(ref, kw);
            default -> containsIgnore(orderNo, kw) || containsIgnore(txnId, kw) || containsIgnore(ref, kw);
        };
    }

    private static boolean containsIgnore(String hay, String needle) {
        if (hay == null || needle == null || needle.isEmpty()) {
            return false;
        }
        return hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (s.endsWith("Z") || s.contains("+") || s.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return Instant.parse(s);
            }
        } catch (Exception ignored) {
            /* fall through */
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, PAYSO_DT);
            return ldt.atZone(ZoneId.of("Asia/Bangkok")).toInstant();
        } catch (Exception ignored) {
            /* fall through */
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(s.replace('T', ' ').substring(0, Math.min(19, s.length())),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.atZone(ZoneId.of("Asia/Bangkok")).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatWall(String raw, ZoneId zone) {
        Instant at = parseInstant(raw);
        if (at != null) {
            return WALL.format(at.atZone(zone).truncatedTo(ChronoUnit.SECONDS));
        }
        return raw != null ? raw.trim() : "";
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

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static final class DailyCheckAgg {
        private long total;
        private final Map<String, Long> buckets = new HashMap<>();
        private final Map<String, BigDecimal> approveByCur = new LinkedHashMap<>();
    }

    private record FetchResult(List<Map<String, Object>> rows, long reportedTotal, boolean capped) {
    }
}
