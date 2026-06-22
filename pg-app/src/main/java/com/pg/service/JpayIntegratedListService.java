package com.pg.service;



import com.pg.api.dto.PageResult;

import com.pg.entity.JpayPortalAccount;

import com.pg.entity.OrgUnit;

import com.pg.entity.PgTrnsctn;

import com.pg.integration.pg.PgVendor;

import com.pg.repository.OrgUnitRepository;

import com.pg.repository.PgTrnsctnRepository;

import com.pg.util.JpayNotifyStatusResolver;

import com.pg.util.JpayTradeStatusMapper;

import com.pg.util.PayListStatusBarBuckets;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.nio.file.Files;

import java.nio.file.Path;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.math.BigDecimal;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Set;

import java.util.Locale;

import java.util.Map;

import java.util.Optional;

import java.util.stream.Collectors;



/**

 * JPAY 통합내역 — 포털 Export 자동 다운로드·파싱·ICOPAY 대조·목록 제공.

 * 총판(MASTER_DIST)별 포털 계정을 순회하며 Export를 병합합니다.

 */

@Service

public class JpayIntegratedListService {



    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    private final JpayPortalAccountService jpayPortalAccountService;

    private final JpayPortalExportRunner portalExportRunner;

    private final JpayOrderExcelParseService excelParseService;

    private final PgTrnsctnRepository pgTrnsctnRepository;

    private final OrgUnitRepository orgUnitRepository;

    private final OrgAccessService orgAccessService;



    private volatile LocalDateTime lastSyncAt;

    private volatile String lastSyncMessage = "";

    private volatile List<Map<String, Object>> cachedRows = List.of();



    public JpayIntegratedListService(HqLedgerSysSettingsService hqLedgerSysSettingsService,

                                     JpayPortalAccountService jpayPortalAccountService,

                                     JpayPortalExportRunner portalExportRunner,

                                     JpayOrderExcelParseService excelParseService,

                                     PgTrnsctnRepository pgTrnsctnRepository,

                                     OrgUnitRepository orgUnitRepository,

                                     OrgAccessService orgAccessService) {

        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;

        this.jpayPortalAccountService = jpayPortalAccountService;

        this.portalExportRunner = portalExportRunner;

        this.excelParseService = excelParseService;

        this.pgTrnsctnRepository = pgTrnsctnRepository;

        this.orgUnitRepository = orgUnitRepository;

        this.orgAccessService = orgAccessService;

    }



    @Transactional

    public Map<String, Object> syncFromPortal(LocalDate from, LocalDate to) throws Exception {

        var settings = hqLedgerSysSettingsService.getOrCreate();

        LocalDate tTo = to != null ? to : LocalDate.now();

        LocalDate tFrom = from != null ? from : tTo.minusDays(

                Math.max(1, settings.getJpayTrRecentSyncDays() != null ? settings.getJpayTrRecentSyncDays() : 2) - 1L);



        List<JpayPortalAccount> accounts = jpayPortalAccountService.listActiveForSync();

        List<Map<String, String>> allRaw = new ArrayList<>();

        int matched = 0;

        int updated = 0;

        int unmatched = 0;

        List<String> accountMessages = new ArrayList<>();



        if (accounts.isEmpty()) {

            String user = settings.getJpayPortalUsername();

            String pass = settings.getJpayPortalPassword();

            if (user == null || user.isBlank() || pass == null || pass.isBlank()) {

                throw new IllegalStateException(

                        "JPAY 포털 계정이 없습니다. 본사설정 > 결제대행사로직에서 총판별 계정을 등록하세요.");

            }

            allRaw.addAll(exportAndParse(tFrom, tTo, user, pass, null));

            ReconcileSummary summary = reconcileRows(allRaw, null);

            matched = summary.matched();

            updated = summary.updated();

            unmatched = summary.unmatched();

            accountMessages.add("레거시 단일계정 " + allRaw.size() + "건");

        } else {

            for (JpayPortalAccount acc : accounts) {

                List<Map<String, String>> raw = exportAndParse(tFrom, tTo,

                        acc.getPortalUsername(), acc.getPortalPassword(), acc);

                allRaw.addAll(raw);

                ReconcileSummary s = reconcileRows(raw, acc.getMasterCompCode());

                matched += s.matched();

                updated += s.updated();

                unmatched += s.unmatched();

                String label = acc.getLabel() != null && !acc.getLabel().isBlank()

                        ? acc.getLabel() : acc.getMasterCompCode();

                accountMessages.add(label + " " + raw.size() + "건");

            }

        }



        cachedRows = enrichRows(allRaw);

        lastSyncAt = LocalDateTime.now();

        lastSyncMessage = "JPAY 포털 Export " + allRaw.size() + "건 (" + String.join(", ", accountMessages)

                + ") — ICOPAY 반영 " + updated + "건";

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("fromDate", tFrom.toString());

        out.put("toDate", tTo.toString());

        out.put("portalRows", allRaw.size());

        out.put("accountCount", accounts.isEmpty() ? 1 : accounts.size());

        out.put("matched", matched);

        out.put("updated", updated);

        out.put("unmatched", unmatched);

        out.put("syncedAt", lastSyncAt.toString());

        out.put("message", lastSyncMessage);

        return out;

    }



    private List<Map<String, String>> exportAndParse(LocalDate from, LocalDate to,

                                                     String user, String pass,

                                                     JpayPortalAccount acc) throws Exception {

        Path xlsx = portalExportRunner.runExport(user, pass, from.toString(), to.toString());

        try {

            List<Map<String, String>> raw = excelParseService.parseFile(xlsx);

            if (acc != null) {

                for (Map<String, String> row : raw) {

                    row.put("_portalAccountId", String.valueOf(acc.getId()));

                    row.put("_masterCompCode", acc.getMasterCompCode() != null ? acc.getMasterCompCode() : "");

                    row.put("_portalLabel", acc.getLabel() != null ? acc.getLabel() : "");

                    row.put("_portalPgCd", acc.getPgCd() != null ? acc.getPgCd() : "");

                }

            }

            return raw;

        } finally {

            try {

                Files.deleteIfExists(xlsx);

            } catch (Exception ignored) {

                /* temp cleanup */

            }

        }

    }



    public PageResult<Map<String, Object>> search(int page, int size,

                                                  String searchKeyword,

                                                  String searchOrderNo,

                                                  String searchPayDivCd,

                                                  LocalDate from, LocalDate to,

                                                  boolean triggerSyncIfEmpty) throws Exception {

        if (cachedRows.isEmpty() && triggerSyncIfEmpty) {

            syncFromPortal(from, to);

        }

        List<Map<String, Object>> filtered = filterRows(cachedRows, searchKeyword, searchOrderNo, searchPayDivCd, from, to);

        int p = Math.max(1, page);

        int sz = Math.min(500, Math.max(1, size));

        int start = (p - 1) * sz;

        int end = Math.min(start + sz, filtered.size());

        List<Map<String, Object>> slice = start < filtered.size() ? filtered.subList(start, end) : List.of();

        int rowNo = start + 1;

        for (Map<String, Object> row : slice) {

            row.put("rowNo", rowNo++);

        }

        PageResult<Map<String, Object>> pr = new PageResult<>();

        pr.setList(slice);

        pr.setPage(p);

        pr.setSize(sz);

        pr.setTotalElements(filtered.size());

        pr.setTotalPages(sz > 0 ? (int) Math.ceil(filtered.size() / (double) sz) : 0);

        Map<String, Object> meta = new LinkedHashMap<>();

        meta.put("jpayIntegrated", true);

        meta.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");

        meta.put("lastSyncMessage", lastSyncMessage);

        meta.put("cachedTotal", cachedRows.size());

        pr.setMeta(meta);

        return pr;

    }



    public Map<String, Object> syncMeta() {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");

        m.put("lastSyncMessage", lastSyncMessage);

        m.put("cachedTotal", cachedRows.size());

        return m;

    }



    private ReconcileSummary reconcileRows(List<Map<String, String>> raw, String masterCompCode) {

        int matched = 0;

        int updated = 0;

        int unmatched = 0;

        for (Map<String, String> row : raw) {

            String orderNo = JpayOrderExcelParseService.col(row, "Merchant Order Number", "pay_orderid", "orderid");

            String txnId = JpayOrderExcelParseService.col(row, "Transaction ID", "transaction_id");

            if (orderNo.isBlank() && txnId.isBlank()) {

                unmatched++;

                continue;

            }

            Optional<PgTrnsctn> opt = findTxn(orderNo, txnId);

            if (opt.isEmpty()) {

                unmatched++;

                continue;

            }

            PgTrnsctn t = opt.get();

            if (masterCompCode != null && !masterCompCode.isBlank()) {

                String merchantId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";

                if (merchantId.isEmpty()

                        || !orgAccessService.isTargetUnderViewerOrg(masterCompCode, merchantId)) {

                    unmatched++;

                    continue;

                }

            }

            matched++;

            if (!PgVendor.isJpayFamily(t.getVan())) {

                continue;

            }

            String trading = JpayOrderExcelParseService.col(row, "Trading Status");

            String chargeback = JpayOrderExcelParseService.col(row, "Is it a chargeback?");

            String rdr = JpayOrderExcelParseService.col(row, "RDR");

            String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading, chargeback, rdr);

            if (mapped == null) {

                continue;

            }

            String old = t.getStatus() != null ? t.getStatus().trim() : "";

            if (!mapped.equals(old)) {

                t.setStatus(mapped);

                t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(mapped,

                        mapped.equals("30") ? "09" : (mapped.equals("31") ? "09" : "00")));

                if (!"10".equals(mapped)) {

                    t.setPaidAt(null);

                }

                if (!txnId.isBlank()) {

                    t.setChillTransactionId(txnId);

                }

                pgTrnsctnRepository.save(t);

                updated++;

            }

        }

        return new ReconcileSummary(matched, updated, unmatched);

    }



    private Optional<PgTrnsctn> findTxn(String orderNo, String txnId) {

        if (txnId != null && !txnId.isBlank()) {

            Optional<PgTrnsctn> byTxn = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(txnId.trim());

            if (byTxn.isPresent()) {

                return byTxn;

            }

        }

        if (orderNo != null && !orderNo.isBlank()) {

            return pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo.trim());

        }

        return Optional.empty();

    }



    private List<Map<String, Object>> enrichRows(List<Map<String, String>> raw) {

        List<Map<String, Object>> out = new ArrayList<>();

        for (Map<String, String> row : raw) {

            Map<String, Object> m = new LinkedHashMap<>();

            String orderNo = JpayOrderExcelParseService.col(row, "Merchant Order Number");

            String txnId = JpayOrderExcelParseService.col(row, "Transaction ID");

            String trading = JpayOrderExcelParseService.col(row, "Trading Status");

            String amount = JpayOrderExcelParseService.col(row, "Transaction Amount");

            String currency = JpayOrderExcelParseService.col(row, "Transaction Currency", "Original Currency");

            String mid = JpayOrderExcelParseService.col(row, "Gateway Access Number");

            String txnDate = JpayOrderExcelParseService.col(row, "Transaction Date");

            String masterCode = row.getOrDefault("_masterCompCode", "");

            String portalLabel = row.getOrDefault("_portalLabel", "");

            String portalPgCd = row.getOrDefault("_portalPgCd", "");

            m.put("transactionId", txnId);

            m.put("orderNo", orderNo);

            m.put("merchant", mid);

            m.put("amount", amount);

            m.put("currency", currency);

            m.put("status", trading);

            m.put("tradingStatus", trading);

            m.put("transactionDate", txnDate);

            m.put("masterDistCompId", masterCode);

            m.put("portalLabel", portalLabel);

            m.put("portalPgCd", portalPgCd);

            if (!masterCode.isBlank()) {

                orgUnitRepository.findByCodeIgnoreCase(masterCode)

                        .map(OrgUnit::getName)

                        .ifPresent(nm -> m.put("masterDistNm", nm));

            }

            splitDateTime(txnDate, m);

            String mapped = JpayTradeStatusMapper.fromPortalTradingStatus(trading,

                    JpayOrderExcelParseService.col(row, "Is it a chargeback?"),

                    JpayOrderExcelParseService.col(row, "RDR"));

            m.put("icopayStatus", mapped != null ? mapped : "");

            Optional<PgTrnsctn> txn = findTxn(orderNo, txnId);

            if (txn.isPresent()) {

                PgTrnsctn t = txn.get();

                m.put("icopay", "Y");

                m.put("trnId", t.getTrnId());

                m.put("compId", t.getMerchantId());

                m.put("dbStatus", t.getStatus());

                orgUnitRepository.findByCodeIgnoreCase(t.getMerchantId() != null ? t.getMerchantId().trim() : "")

                        .map(OrgUnit::getName)

                        .ifPresent(nm -> m.put("compNm", nm));

            } else {

                m.put("icopay", "N");

            }

            m.put("fee", JpayOrderExcelParseService.col(row, "Fee"));

            m.put("refundStatus", JpayOrderExcelParseService.col(row, "Refund Status"));

            m.put("chargeback", JpayOrderExcelParseService.col(row, "Is it a chargeback?"));

            m.put("urlSource", JpayOrderExcelParseService.col(row, "URL Source"));

            m.put("cardBin", JpayOrderExcelParseService.col(row, "Card BIN"));

            out.add(m);

        }

        return out;

    }



    private static void splitDateTime(String raw, Map<String, Object> m) {

        if (raw == null || raw.isBlank()) {

            return;

        }

        String t = raw.trim();

        if (t.length() >= 10) {

            m.put("trnDate", t.substring(0, 10));

        }

        if (t.length() > 11) {

            m.put("trnTime", t.substring(11).trim());

        }

    }



    private static List<Map<String, Object>> filterRows(List<Map<String, Object>> rows,

                                                      String keyword, String orderNo, String payDivCd,

                                                      LocalDate from, LocalDate to) {

        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";

        String on = orderNo != null ? orderNo.trim().toLowerCase(Locale.ROOT) : "";

        String status = payDivCd != null ? payDivCd.trim() : "";

        return rows.stream()

                .filter(r -> {

                    if (!on.isEmpty()) {

                        String o = String.valueOf(r.getOrDefault("orderNo", "")).toLowerCase(Locale.ROOT);

                        if (!o.contains(on)) {

                            return false;

                        }

                    }

                    if (!status.isEmpty()) {

                        String ic = String.valueOf(r.getOrDefault("icopayStatus", ""));

                        String db = String.valueOf(r.getOrDefault("dbStatus", ""));

                        if (!status.equals(ic) && !status.equals(db)) {

                            return false;

                        }

                    }

                    if (from != null || to != null) {

                        String d = String.valueOf(r.getOrDefault("trnDate", ""));

                        if (!d.isBlank()) {

                            try {

                                LocalDate ld = LocalDate.parse(d.substring(0, Math.min(10, d.length())));

                                if (from != null && ld.isBefore(from)) {

                                    return false;

                                }

                                if (to != null && ld.isAfter(to)) {

                                    return false;

                                }

                            } catch (Exception ignored) {

                                /* skip date filter */

                            }

                        }

                    }

                    if (!kw.isEmpty()) {

                        String blob = (r.getOrDefault("transactionId", "") + " "

                                + r.getOrDefault("orderNo", "") + " "

                                + r.getOrDefault("merchant", "") + " "

                                + r.getOrDefault("compId", "") + " "

                                + r.getOrDefault("compNm", "") + " "

                                + r.getOrDefault("masterDistCompId", "") + " "

                                + r.getOrDefault("masterDistNm", "") + " "

                                + r.getOrDefault("portalLabel", "") + " "

                                + r.getOrDefault("status", "")).toLowerCase(Locale.ROOT);

                        if (!blob.contains(kw)) {

                            return false;

                        }

                    }

                    return true;

                })

                .collect(Collectors.toList());

    }



    /**
     * 통합조회(JPAY Export 캐시) — 거래일 기준 일별 집계(조회통합 화면).
     */
    public Map<String, Object> buildDailyIntegratedSummary(LocalDate tFrom, LocalDate tTo, LocalDate effectiveTo,
                                                           String searchKeyword, String searchOrderNo,
                                                           String searchPayDivCd, String searchOrderDir) {
        List<Map<String, Object>> filtered = filterRows(cachedRows, searchKeyword, searchOrderNo, searchPayDivCd,
                tFrom, effectiveTo);
        Map<LocalDate, JpayDayAgg> byDay = new LinkedHashMap<>();
        for (LocalDate d = tFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
            byDay.put(d, new JpayDayAgg());
        }
        for (Map<String, Object> row : filtered) {
            String dStr = String.valueOf(row.getOrDefault("trnDate", "")).trim();
            if (dStr.length() < 10) {
                continue;
            }
            LocalDate ld;
            try {
                ld = LocalDate.parse(dStr.substring(0, 10));
            } catch (Exception ignored) {
                continue;
            }
            JpayDayAgg agg = byDay.get(ld);
            if (agg == null) {
                continue;
            }
            agg.count++;
            String st = String.valueOf(row.getOrDefault("icopayStatus", "")).trim();
            if (st.isEmpty()) {
                st = String.valueOf(row.getOrDefault("dbStatus", "")).trim();
            }
            String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
            agg.bucketCount.merge(bucket, 1L, Long::sum);
            String cur = PayListStatusBarBuckets.normalizeCurrency(String.valueOf(row.getOrDefault("currency", "")));
            BigDecimal amt = PayListStatusBarBuckets.parseMoney(row.get("amount"));
            agg.totalTxn.merge(cur, amt, BigDecimal::add);
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                agg.successCount++;
                agg.approve.merge(cur, amt, BigDecimal::add);
                agg.approveCountByCur.merge(cur, 1L, Long::sum);
            } else if (isJpayCancelFinancialBucket(bucket)) {
                agg.cancel.merge(cur, amt, BigDecimal::add);
                agg.cancelCountByCur.merge(cur, 1L, Long::sum);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(tFrom); d = d.minusDays(1)) {
            JpayDayAgg agg = byDay.getOrDefault(d, new JpayDayAgg());
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("day", d.toString());
            one.put("totalElements", agg.count);
            one.put("statusBucketCounts", new LinkedHashMap<>(agg.bucketCount));
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("payListFinancialSummary", agg.toFinancialSummary());
            one.put("meta", meta);
            rows.add(one);
        }
        PayListService.applyDailySummaryDayListOrder(rows, searchOrderDir);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", rows);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("jpayIntegrated", true);
        meta.put("cachedTotal", cachedRows.size());
        meta.put("lastSyncAt", lastSyncAt != null ? lastSyncAt.toString() : "");
        meta.put("dailyJpayNote",
                "일자별 상세는 동일 조건으로 jpayTrSearch(통합조회)에 해당 일자만 지정해 조회합니다.");
        if (cachedRows.isEmpty()) {
            meta.put("note", "캐시된 통합조회 데이터가 없습니다. 통합조회 화면에서 [JPAY 동기화]를 실행하세요.");
        }
        payload.put("meta", meta);
        return payload;
    }

    private static boolean isJpayCancelFinancialBucket(String bucket) {
        return PayListStatusBarBuckets.CANCEL.equals(bucket)
                || PayListStatusBarBuckets.REFUND.equals(bucket)
                || PayListStatusBarBuckets.VOID.equals(bucket)
                || PayListStatusBarBuckets.EMAIL_VOID.equals(bucket)
                || PayListStatusBarBuckets.FORCE_REFUND.equals(bucket);
    }

    private static final class JpayDayAgg {
        long count;
        long successCount;
        final Map<String, Long> bucketCount = new HashMap<>();
        final Map<String, BigDecimal> totalTxn = new HashMap<>();
        final Map<String, BigDecimal> approve = new HashMap<>();
        final Map<String, BigDecimal> cancel = new HashMap<>();
        final Map<String, Long> approveCountByCur = new HashMap<>();
        final Map<String, Long> cancelCountByCur = new HashMap<>();

        Map<String, Object> toFinancialSummary() {
            Set<String> union = new HashSet<>();
            union.addAll(totalTxn.keySet());
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            List<String> currencyOrder = new ArrayList<>(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
            if (currencyOrder.isEmpty()) {
                currencyOrder.add("JPY");
            }
            Map<String, String> totalTxnPlain = new LinkedHashMap<>();
            Map<String, String> approvePlain = new LinkedHashMap<>();
            Map<String, String> cancelPlain = new LinkedHashMap<>();
            Map<String, String> paymentPlain = new LinkedHashMap<>();
            Map<String, Long> approveCountPlain = new LinkedHashMap<>();
            Map<String, Long> cancelCountPlain = new LinkedHashMap<>();
            for (String c : currencyOrder) {
                BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
                BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
                totalTxnPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(totalTxn.getOrDefault(c, BigDecimal.ZERO)));
                approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
                cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
                paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a.subtract(k)));
                approveCountPlain.put(c, approveCountByCur.getOrDefault(c, 0L));
                cancelCountPlain.put(c, cancelCountByCur.getOrDefault(c, 0L));
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("successCount", successCount);
            out.put("multiCurrency", currencyOrder.size() > 1);
            out.put("primaryCurrency", currencyOrder.get(0));
            out.put("currencyOrder", currencyOrder);
            out.put("totalTxnByCurrency", totalTxnPlain);
            out.put("approveByCurrency", approvePlain);
            out.put("cancelByCurrency", cancelPlain);
            out.put("approveCountByCurrency", approveCountPlain);
            out.put("cancelCountByCurrency", cancelCountPlain);
            out.put("paymentByCurrency", paymentPlain);
            return out;
        }
    }

    private record ReconcileSummary(int matched, int updated, int unmatched) {

    }

}


