package com.pg.service.ops;

import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.ChillPayService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.OrgAccessService;
import com.pg.service.PayListService;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.PgNotifyInternalStatusMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 운영관리 — 검증 리포트 (ChillPay 통합내역 API ↔ NOTI 결제내역).
 * 기준은 ChillPay API이며, 통합에 없고 결제에만 있는 건은 오류로 보지 않습니다.
 */
@Service
public class OpsVerifyReportService {

    private static final int VERIFY_REPORT_MAX_DAYS = 93;

    private final TaxReportService taxReportService;
    private final ChillPayService chillPayService;
    private final PayListService payListService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgAccessService orgAccessService;
    private final OrgUnitRepository orgUnitRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public OpsVerifyReportService(TaxReportService taxReportService,
                                  ChillPayService chillPayService,
                                  PayListService payListService,
                                  PgTrnsctnRepository pgTrnsctnRepository,
                                  OrgAccessService orgAccessService,
                                  OrgUnitRepository orgUnitRepository,
                                  CommissionPolicyRepository commissionPolicyRepository,
                                  HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.taxReportService = taxReportService;
        this.chillPayService = chillPayService;
        this.payListService = payListService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgAccessService = orgAccessService;
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        return taxReportService.accessMeta(authentication);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildVerifyReport(Map<String, String> params, Authentication authentication) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            throw new IllegalStateException(deny.get());
        }

        ChillSearchParams chill = ChillSearchParams.from(params);
        LocalDate from = chill.fromDate;
        LocalDate to = chill.toDate;
        if (from == null || to == null) {
            throw new IllegalArgumentException("거래일자(searchFromDate, searchToDate)는 필수입니다.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("거래일자 시작이 종료보다 늦을 수 없습니다.");
        }
        long span = ChronoUnit.DAYS.between(from, to) + 1;
        if (span > VERIFY_REPORT_MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 " + VERIFY_REPORT_MAX_DAYS + "일 이내로 지정해 주세요.");
        }

        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        LocalDate effectiveTo = to.isAfter(today) ? today : to;
        if (from.isAfter(effectiveTo)) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("mismatches", List.of());
            empty.put("meta", Map.of("note", "조회 구간에 포함된 일자가 없습니다(미래 일자는 표시하지 않습니다)."));
            return empty;
        }

        String merchantFilter = resolveChillPayMerchantCodeFilter(authentication, chill.merchantCode);
        if ("__NONE__".equals(merchantFilter)) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("mismatches", List.of());
            empty.put("meta", Map.of("note", "조회 가능한 가맹 범위가 없습니다."));
            return empty;
        }

        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
        String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
        String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                user, orgUnitRepository, commissionPolicyRepository, ledgerCur);

        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        List<Map<String, Object>> dayRows = new ArrayList<>();
        List<Map<String, Object>> mismatches = new ArrayList<>();
        int rowNo = 0;

        for (LocalDate d = effectiveTo; !d.isBefore(from); d = d.minusDays(1)) {
            List<Map<String, Object>> chillRows;
            try {
                chillRows = chillPayService.listChillPayPaymentRowsForTransactionDate(
                        null,
                        chill.orderBy,
                        chill.orderDir,
                        chill.searchKeyword,
                        merchantFilter,
                        chill.paymentChannel,
                        chill.routeNo,
                        chill.orderNo,
                        chill.chillStatus,
                        d,
                        chill.payDivCd,
                        authentication);
            } catch (RuntimeException ex) {
                rowNo++;
                Map<String, Object> errRow = new LinkedHashMap<>();
                errRow.put("rowNo", rowNo);
                errRow.put("day", d.toString());
                errRow.put("chillCount", 0);
                errRow.put("matchedCount", 0);
                errRow.put("mismatchCount", 0);
                errRow.put("hasMismatch", false);
                errRow.put("note", ex.getMessage() != null ? ex.getMessage() : "ChillPay 조회 실패");
                errRow.put("error", true);
                dayRows.add(errRow);
                continue;
            }

            Set<String> approvalKeys = new HashSet<>();
            for (Map<String, Object> row : chillRows) {
                String ap = approvalNoFromChillRow(row);
                if (!ap.isEmpty()) {
                    approvalKeys.add(ap);
                }
            }
            Map<String, PgTrnsctn> notiByApproval = loadLatestNotiByApprovalNos(approvalKeys, allowedMerchants);

            int matched = 0;
            int mismatchCount = 0;
            int notiMissing = 0;
            int statusDiff = 0;
            int amountDiff = 0;
            int requestNoNoti = 0;

            for (Map<String, Object> chillRow : chillRows) {
                String approvalNo = approvalNoFromChillRow(chillRow);
                if (approvalNo.isEmpty()) {
                    continue;
                }
                PgTrnsctn noti = notiByApproval.get(approvalNo);
                if (noti == null && isChillRequestPendingRow(chillRow)) {
                    matched++;
                    requestNoNoti++;
                    continue;
                }
                MismatchEval ev = evaluateMismatch(chillRow, noti);
                if (ev.isMatch) {
                    matched++;
                    continue;
                }
                mismatchCount++;
                switch (ev.type) {
                    case "NOTI_MISSING" -> notiMissing++;
                    case "STATUS" -> statusDiff++;
                    case "AMOUNT" -> amountDiff++;
                    case "STATUS_AMOUNT" -> {
                        statusDiff++;
                        amountDiff++;
                    }
                    default -> { /* ignore */ }
                }
                mismatches.add(buildMismatchRow(d, chillRow, noti, ev));
            }

            rowNo++;
            Map<String, Object> dayRow = new LinkedHashMap<>();
            dayRow.put("rowNo", rowNo);
            dayRow.put("day", d.toString());
            dayRow.put("chillCount", chillRows.size());
            dayRow.put("matchedCount", matched);
            dayRow.put("mismatchCount", mismatchCount);
            dayRow.put("hasMismatch", mismatchCount > 0);
            dayRow.put("notiMissingCount", notiMissing);
            dayRow.put("statusDiffCount", statusDiff);
            dayRow.put("amountDiffCount", amountDiff);
            dayRow.put("requestNoNotiCount", requestNoNoti);
            dayRow.put("note", buildDayNote(mismatchCount, notiMissing, statusDiff, amountDiff, requestNoNoti));
            dayRows.add(dayRow);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("primaryCurrency", PayListStatusBarBuckets.normalizeCurrency(primaryCurrency));
        meta.put("multiCurrency", multiCurrency);
        meta.put("verifyReportNote",
                "ChillPay 통합내역(API, 거래일 TransactionDate)을 기준으로 결제내역 NOTI(origin=NOTI)와 승인번호(TransactionId)로 대조합니다. "
                        + "일치(승인번호·결제액·상태) 건은 하단 목록에서 제외합니다. "
                        + "통합 상태가 요청·대기(인증 전)이면 노티가 오지 않는 것이 정상이므로 NOTI 미수신으로 잡지 않습니다. "
                        + "JPAY 등 다른 PG·URL/챗봇만 있는 건은 본 화면 대상이 아니며, 통합에 없고 결제에만 있는 건은 오류로 표시하지 않습니다.");
        if (to.isAfter(today)) {
            meta.put("displayToDate", effectiveTo.toString());
            meta.put("requestedToDate", to.toString());
        }
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", dayRows);
        out.put("mismatches", mismatches);
        out.put("meta", meta);
        return out;
    }

    private Map<String, PgTrnsctn> loadLatestNotiByApprovalNos(Set<String> approvalNos, Set<String> allowedMerchants) {
        if (approvalNos == null || approvalNos.isEmpty()) {
            return Map.of();
        }
        Set<String> queryIds = new HashSet<>();
        for (String ap : approvalNos) {
            if (ap == null || ap.isBlank()) {
                continue;
            }
            queryIds.add(ap.trim());
            String norm = normalizeChillTxnId(ap);
            if (!norm.isEmpty()) {
                queryIds.add(norm);
            }
        }
        if (queryIds.isEmpty()) {
            return Map.of();
        }
        List<PgTrnsctn> rows = pgTrnsctnRepository.findAllByChillTransactionIdIn(queryIds);
        Map<String, PgTrnsctn> best = new HashMap<>();
        for (PgTrnsctn t : rows) {
            if (t.getOrigin() == null || !"NOTI".equalsIgnoreCase(t.getOrigin().trim())) {
                continue;
            }
            String mid = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
            if (allowedMerchants != null && (mid.isEmpty() || !allowedMerchants.contains(mid))) {
                continue;
            }
            String key = normalizeChillTxnId(t.getChillTransactionId());
            if (key.isEmpty()) {
                continue;
            }
            PgTrnsctn prev = best.get(key);
            if (prev == null || isAfter(t, prev)) {
                best.put(key, t);
            }
        }
        return best;
    }

    private static boolean isAfter(PgTrnsctn a, PgTrnsctn b) {
        if (a.getCreatedAt() == null) {
            return false;
        }
        if (b.getCreatedAt() == null) {
            return true;
        }
        return a.getCreatedAt().isAfter(b.getCreatedAt());
    }

    private static MismatchEval evaluateMismatch(Map<String, Object> chillRow, PgTrnsctn noti) {
        if (noti == null) {
            return MismatchEval.of("NOTI_MISSING", "NOTI 미수신");
        }
        String chillBucket = chillStatusBucket(chillRow);
        String notiBucket = PayListStatusBarBuckets.bucketForPgStatus(noti.getStatus());
        BigDecimal chillAmt = chillAmount(chillRow);
        BigDecimal notiAmt = noti.getAmtKrw();
        boolean statusOk = chillBucket.equals(notiBucket);
        boolean amountOk = amountsEqual(chillAmt, notiAmt);
        if (statusOk && amountOk) {
            return MismatchEval.allMatch();
        }
        if (!statusOk && !amountOk) {
            return MismatchEval.of("STATUS_AMOUNT", "상태·결제액 불일치");
        }
        if (!statusOk) {
            return MismatchEval.of("STATUS", "상태 불일치");
        }
        return MismatchEval.of("AMOUNT", "결제액 불일치");
    }

    private Map<String, Object> buildMismatchRow(LocalDate day, Map<String, Object> chillRow, PgTrnsctn noti, MismatchEval ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("day", day.toString());
        m.put("mismatchType", ev.type);
        m.put("reason", ev.reason);
        m.put("approvalNo", approvalNoFromChillRow(chillRow));
        m.put("orderNo", firstNonBlank(chillRow, "orderNo", "OrderNo"));
        m.put("compId", firstNonBlank(chillRow, "compId", "merchant", "Merchant", "merchantCode", "MerchantCode"));
        m.put("compNm", firstNonBlank(chillRow, "compNm", "comp_nm"));
        m.put("chillAmount", moneyPlain(chillAmount(chillRow)));
        m.put("chillCurrency", firstNonBlank(chillRow, "currency", "Currency", "payCur"));
        m.put("chillStatus", firstNonBlank(chillRow, "status", "Status"));
        m.put("chillStatusBucket", chillStatusBucket(chillRow));
        if (noti != null) {
            m.put("notiTrnId", noti.getTrnId());
            m.put("notiAmount", moneyPlain(noti.getAmtKrw()));
            m.put("notiCurrency", noti.getCurType());
            m.put("notiStatus", noti.getStatus());
            m.put("notiStatusBucket", PayListStatusBarBuckets.bucketForPgStatus(noti.getStatus()));
        }
        return m;
    }

    /**
     * 검증 리포트 「상태 불일치」 — ChillPay 통합(기준) 상태에 맞춰 NOTI 결제내역({@code origin=NOTI}) 상태를 갱신합니다.
     * 금액 불일치가 함께 있는 건은 처리하지 않습니다.
     */
    @Transactional
    public Map<String, Object> syncNotiStatusFromChill(String approvalNoRaw, String dayRaw, Authentication authentication) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            throw new IllegalStateException(deny.get());
        }
        String approvalNo = normalizeChillTxnId(approvalNoRaw);
        if (approvalNo.isEmpty()) {
            throw new IllegalArgumentException("승인번호(approvalNo)는 필수입니다.");
        }
        if (dayRaw == null || dayRaw.isBlank()) {
            throw new IllegalArgumentException("거래일(day)은 필수입니다.");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(dayRaw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("거래일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).");
        }

        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        Set<String> queryIds = new HashSet<>();
        queryIds.add(approvalNo);
        Map<String, PgTrnsctn> notiByApproval = loadLatestNotiByApprovalNos(Set.of(approvalNo), allowedMerchants);
        PgTrnsctn noti = notiByApproval.get(approvalNo);
        if (noti == null) {
            throw new IllegalArgumentException("NOTI 결제내역(origin=NOTI)을 찾을 수 없습니다. 승인번호=" + approvalNo);
        }

        String merchantFilter = resolveChillPayMerchantCodeFilter(authentication, noti.getMerchantId());
        if ("__NONE__".equals(merchantFilter)) {
            throw new IllegalStateException("조회 가능한 가맹 범위가 없습니다.");
        }
        List<Map<String, Object>> chillRows = chillPayService.listChillPayPaymentRowsForTransactionDate(
                null, "", "DESC", "", merchantFilter, "", null, "", "", day, "", authentication);
        Map<String, Object> chillRow = null;
        for (Map<String, Object> row : chillRows) {
            if (approvalNo.equals(approvalNoFromChillRow(row))) {
                chillRow = row;
                break;
            }
        }
        if (chillRow == null) {
            throw new IllegalArgumentException("해당 거래일 ChillPay 통합내역에서 승인번호를 찾을 수 없습니다.");
        }

        MismatchEval ev = evaluateMismatch(chillRow, noti);
        if (ev.isMatch) {
            Map<String, Object> already = new LinkedHashMap<>();
            already.put("approvalNo", approvalNo);
            already.put("day", day.toString());
            already.put("trnId", noti.getTrnId());
            already.put("alreadyMatched", true);
            already.put("message", "이미 일치합니다.");
            return already;
        }
        if ("NOTI_MISSING".equals(ev.type)) {
            throw new IllegalArgumentException("NOTI 미수신 건은 본 기능으로 처리할 수 없습니다.");
        }
        if ("AMOUNT".equals(ev.type) || "STATUS_AMOUNT".equals(ev.type)) {
            throw new IllegalArgumentException("결제액 불일치가 있는 건은 상태만 맞출 수 없습니다. 금액을 먼저 확인하세요.");
        }
        if (!"STATUS".equals(ev.type)) {
            throw new IllegalArgumentException("상태 불일치 건만 처리할 수 있습니다.");
        }

        String chillStatusRaw = firstNonBlank(chillRow, "status", "Status");
        String targetStatus = internalStatusFromChillStatus(chillStatusRaw);
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new IllegalArgumentException("통합 상태를 결제내역 코드로 변환할 수 없습니다: " + chillStatusRaw);
        }
        String oldStatus = noti.getStatus() != null ? noti.getStatus().trim() : "";
        if (targetStatus.equals(oldStatus)) {
            Map<String, Object> same = new LinkedHashMap<>();
            same.put("approvalNo", approvalNo);
            same.put("day", day.toString());
            same.put("trnId", noti.getTrnId());
            same.put("status", targetStatus);
            same.put("message", "결제내역 상태가 이미 통합과 동일한 코드입니다.");
            return same;
        }

        noti.setStatus(targetStatus);
        if (chillStatusRaw != null && !chillStatusRaw.isBlank()) {
            String cps = chillStatusRaw.trim();
            noti.setChillPaymentStatus(cps.length() > 50 ? cps.substring(0, 50) : cps);
        }
        if (!PgNotifyInternalStatusMapper.ST_PAID.equals(targetStatus)) {
            noti.setPaidAt(null);
        }
        pgTrnsctnRepository.save(noti);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("approvalNo", approvalNo);
        out.put("day", day.toString());
        out.put("trnId", noti.getTrnId());
        out.put("merchantId", noti.getMerchantId());
        out.put("oldStatus", oldStatus);
        out.put("newStatus", targetStatus);
        out.put("chillStatus", chillStatusRaw);
        out.put("oldStatusBucket", PayListStatusBarBuckets.bucketForPgStatus(oldStatus));
        out.put("newStatusBucket", PayListStatusBarBuckets.bucketForPgStatus(targetStatus));
        out.put("chillStatusBucket", chillStatusBucket(chillRow));
        out.put("message", "NOTI 결제내역 상태를 통합(ChillPay) 기준으로 갱신했습니다.");
        return out;
    }

    /**
     * 검증 리포트 — 선택 일자의 「상태 불일치」 건을 통합(ChillPay) 기준으로 일괄 맞춤.
     * 금액 불일치·NOTI 미수신 건은 건너뜁니다.
     */
    @Transactional
    public Map<String, Object> syncStatusMismatchesForDay(Map<String, String> params, Authentication authentication) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            throw new IllegalStateException(deny.get());
        }
        String dayRaw = params != null && params.get("day") != null ? params.get("day").trim() : "";
        if (dayRaw.isEmpty()) {
            throw new IllegalArgumentException("거래일(day)은 필수입니다.");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(dayRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("거래일(day) 형식이 올바르지 않습니다(yyyy-MM-dd).");
        }

        ChillSearchParams chill = ChillSearchParams.from(params != null ? params : Map.of());
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        String merchantFilter = resolveChillPayMerchantCodeFilter(authentication, chill.merchantCode);

        List<Map<String, Object>> chillRows;
        try {
            chillRows = chillPayService.listChillPayPaymentRowsForTransactionDate(
                    null,
                    chill.orderBy,
                    chill.orderDir,
                    chill.searchKeyword,
                    merchantFilter,
                    chill.paymentChannel,
                    chill.routeNo,
                    chill.orderNo,
                    chill.chillStatus,
                    day,
                    chill.payDivCd,
                    authentication);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(ex.getMessage() != null ? ex.getMessage() : "ChillPay 조회 실패");
        }

        Set<String> approvalKeys = new HashSet<>();
        for (Map<String, Object> row : chillRows) {
            String ap = approvalNoFromChillRow(row);
            if (!ap.isEmpty()) {
                approvalKeys.add(ap);
            }
        }
        Map<String, PgTrnsctn> notiByApproval = loadLatestNotiByApprovalNos(approvalKeys, allowedMerchants);

        List<Map<String, Object>> synced = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        for (Map<String, Object> chillRow : chillRows) {
            String approvalNo = approvalNoFromChillRow(chillRow);
            if (approvalNo.isEmpty()) {
                continue;
            }
            if (isChillRequestPendingRow(chillRow)) {
                continue;
            }
            PgTrnsctn noti = notiByApproval.get(approvalNo);
            MismatchEval ev = evaluateMismatch(chillRow, noti);
            if (ev.isMatch || !"STATUS".equals(ev.type)) {
                if (!ev.isMatch) {
                    Map<String, Object> skip = new LinkedHashMap<>();
                    skip.put("approvalNo", approvalNo);
                    skip.put("mismatchType", ev.type);
                    skip.put("reason", ev.reason);
                    skipped.add(skip);
                }
                continue;
            }
            try {
                synced.add(syncNotiStatusFromChill(approvalNo, dayRaw, authentication));
            } catch (RuntimeException ex) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("approvalNo", approvalNo);
                fail.put("message", ex.getMessage() != null ? ex.getMessage() : "상태 맞춤 실패");
                failed.add(fail);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("day", dayRaw);
        out.put("syncedCount", synced.size());
        out.put("skippedCount", skipped.size());
        out.put("failedCount", failed.size());
        out.put("synced", synced);
        out.put("skipped", skipped);
        out.put("failed", failed);
        out.put("message", synced.isEmpty()
                ? "상태 불일치 맞춤 대상이 없거나 모두 건너뛰었습니다."
                : ("상태 불일치 " + synced.size() + "건을 통합 기준으로 맞췄습니다."));
        return out;
    }

    private static boolean isChillRequestPendingRow(Map<String, Object> chillRow) {
        return PayListStatusBarBuckets.isChillRequestPendingStatus(firstNonBlank(chillRow, "status", "Status"));
    }

    /** ChillPay 통합 status/PaymentStatus 문자열 → ICOPAY 내부 코드(10·20·99 등). */
    private static String internalStatusFromChillStatus(String chillStatusRaw) {
        if (chillStatusRaw == null || chillStatusRaw.isBlank()) {
            return null;
        }
        String raw = chillStatusRaw.trim();
        String mapped = PgNotifyInternalStatusMapper.mapPaymentAndStatus(raw, null, true);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return switch (PayListStatusBarBuckets.bucketForChillStatus(raw)) {
            case PayListStatusBarBuckets.SUCCESS -> PgNotifyInternalStatusMapper.ST_PAID;
            case PayListStatusBarBuckets.FAIL -> PgNotifyInternalStatusMapper.ST_FAIL;
            case PayListStatusBarBuckets.CANCEL -> PgNotifyInternalStatusMapper.ST_CANCEL;
            case PayListStatusBarBuckets.VOID -> "21";
            case PayListStatusBarBuckets.EMAIL_VOID -> "22";
            case PayListStatusBarBuckets.REFUND -> PgNotifyInternalStatusMapper.ST_REFUND;
            case PayListStatusBarBuckets.FORCE_REFUND -> "31";
            default -> null;
        };
    }

    private static String buildDayNote(int mismatchCount, int notiMissing, int statusDiff, int amountDiff, int requestNoNoti) {
        if (mismatchCount <= 0) {
            if (requestNoNoti > 0) {
                return "일치 (요청·대기 " + requestNoNoti + "건은 노티 없음 정상·제외)";
            }
            return "일치";
        }
        List<String> parts = new ArrayList<>();
        if (notiMissing > 0) {
            parts.add("NOTI 미수신 " + notiMissing + "건");
        }
        if (statusDiff > 0) {
            parts.add("상태 불일치 " + statusDiff + "건");
        }
        if (amountDiff > 0) {
            parts.add("결제액 불일치 " + amountDiff + "건");
        }
        if (requestNoNoti > 0) {
            parts.add("요청·대기 " + requestNoNoti + "건 제외");
        }
        return String.join(", ", parts);
    }

    private static String approvalNoFromChillRow(Map<String, Object> row) {
        return normalizeChillTxnId(firstNonBlank(row, "transactionId", "TransactionId", "chillTransactionId"));
    }

    private static String chillStatusBucket(Map<String, Object> row) {
        String st = firstNonBlank(row, "status", "Status");
        if (st.isEmpty()) {
            return PayListStatusBarBuckets.OTHER;
        }
        return PayListStatusBarBuckets.bucketForChillStatus(st);
    }

    private static BigDecimal chillAmount(Map<String, Object> row) {
        BigDecimal a = PayListStatusBarBuckets.parseMoney(row.get("amount"));
        if (a == null) {
            a = PayListStatusBarBuckets.parseMoney(row.get("Amount"));
        }
        if (a == null) {
            a = PayListStatusBarBuckets.parseMoney(row.get("totalAmount"));
        }
        if (a == null) {
            a = PayListStatusBarBuckets.parseMoney(row.get("TotalAmount"));
        }
        return a;
    }

    private static boolean amountsEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.stripTrailingZeros().compareTo(b.stripTrailingZeros()) == 0;
    }

    private static String moneyPlain(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static String firstNonBlank(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String k : keys) {
            Object v = row.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }

    private static String normalizeChillTxnId(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return "";
        }
        try {
            if (s.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
                BigDecimal bd = new BigDecimal(s);
                s = bd.stripTrailingZeros().toPlainString();
            }
        } catch (Exception ignored) {
            // keep
        }
        if (s.endsWith(".0")) {
            int dot = s.indexOf('.');
            if (dot > 0 && "0".equals(s.substring(dot + 1))) {
                s = s.substring(0, dot);
            }
        }
        if (s.length() > 64) {
            s = s.substring(0, 64);
        }
        return s;
    }

    private String resolveChillPayMerchantCodeFilter(Authentication authentication, String requested) {
        Set<String> allowed = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowed == null) {
            return requested != null ? requested.trim() : "";
        }
        if (allowed.isEmpty()) {
            return "__NONE__";
        }
        String req = requested != null ? requested.trim() : "";
        if (!req.isEmpty()) {
            return allowed.contains(req) ? req : "__NONE__";
        }
        if (allowed.size() == 1) {
            return allowed.iterator().next();
        }
        if (isHeadquartersOrgViewer(authentication)) {
            return "";
        }
        return "__NONE__";
    }

    private boolean isHeadquartersOrgViewer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return false;
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return false;
        }
        return orgUnitRepository.findByCode(code.trim())
                .map(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .orElse(false);
    }

    private record MismatchEval(boolean isMatch, String type, String reason) {
        static MismatchEval allMatch() {
            return new MismatchEval(true, "MATCH", "");
        }

        static MismatchEval of(String type, String reason) {
            return new MismatchEval(false, type, reason);
        }
    }

    private static final class ChillSearchParams {
        LocalDate fromDate;
        LocalDate toDate;
        String orderBy = "";
        String orderDir = "DESC";
        String searchKeyword = "";
        String merchantCode = "";
        String paymentChannel = "";
        Integer routeNo;
        String orderNo = "";
        String chillStatus = "";
        String payDivCd = "";

        static ChillSearchParams from(Map<String, String> params) {
            PayListSearchRequest req = PayListSearchRequest.fromParams(params);
            ChillSearchParams c = new ChillSearchParams();
            c.fromDate = req.getSearchFromDate();
            c.toDate = req.getSearchToDate();
            c.orderBy = opt(params, "searchOrderBy");
            c.orderDir = opt(params, "searchOrderDir");
            if (c.orderDir.isBlank()) {
                c.orderDir = "DESC";
            }

            String sftRaw = opt(params, "searchFieldType");
            boolean unified = sftRaw != null && !sftRaw.isBlank();
            String ft = unified ? sftRaw.trim().toUpperCase(Locale.ROOT) : "";
            String kw = opt(params, "searchKeyword");

            String sk = unified ? "" : kw;
            String smc = unified ? "" : opt(params, "searchMerchantCode");
            String spc = unified ? "" : opt(params, "searchPaymentChannel");
            String son = unified ? "" : opt(params, "searchOrderNo");
            String scs = unified ? "" : opt(params, "searchChillStatus");
            String srn = unified ? "" : opt(params, "searchRouteNo");

            if (unified) {
                switch (ft) {
                    case "ALL" -> sk = kw;
                    case "MID", "COMP_ID" -> smc = kw;
                    case "ORDER_NO" -> son = kw;
                    case "APPROVAL_NO" -> sk = kw;
                    case "ROUTE" -> srn = kw;
                    case "STATUS" -> scs = kw;
                    default -> sk = kw;
                }
            }
            c.searchKeyword = sk;
            c.merchantCode = smc;
            c.paymentChannel = spc;
            c.orderNo = son;
            c.chillStatus = scs;
            c.payDivCd = opt(params, "searchPayDivCd");
            if (srn != null && !srn.isBlank()) {
                try {
                    c.routeNo = Integer.parseInt(srn.trim());
                } catch (NumberFormatException ignored) {
                    c.routeNo = null;
                }
            }
            return c;
        }

        private static String opt(Map<String, String> params, String key) {
            if (params == null || key == null) {
                return "";
            }
            String v = params.get(key);
            return v != null ? v.trim() : "";
        }
    }
}
