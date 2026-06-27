package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqNotifyMappingService;
import com.pg.service.IntegratedCheckService;
import com.pg.service.OrgAccessService;
import com.pg.service.JpayIntegratedListService;
import com.pg.service.JpaySyncTrigger;
import com.pg.service.JpayTradeApiService;
import com.pg.service.LoginNoticePublicService;
import com.pg.service.OutcomeReasonTranslateService;
import com.pg.service.PayListActionService;
import com.pg.service.PayListService;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.PayListStatusBarBuckets;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
import java.util.Set;

@RestController
@RequestMapping(value = "/api/calc", produces = "application/json")
public class ApiCalcController {

    private final PayListService payListService;
    private final PayListActionService payListActionService;
    private final ChillPayService chillPayService;
    private final HqNotifyMappingService hqNotifyMappingService;
    private final OrgUnitRepository orgUnitRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final OrgAccessService orgAccessService;
    private final JpayIntegratedListService jpayIntegratedListService;
    private final IntegratedCheckService integratedCheckService;
    private final JpayTradeApiService jpayTradeApiService;
    private final OutcomeReasonTranslateService outcomeReasonTranslateService;

    public ApiCalcController(PayListService payListService, PayListActionService payListActionService,
                             ChillPayService chillPayService, HqNotifyMappingService hqNotifyMappingService,
                             OrgUnitRepository orgUnitRepository, CommissionPolicyRepository commissionPolicyRepository,
                             HqLedgerSysSettingsService hqLedgerSysSettingsService,
                             OrgAccessService orgAccessService,
                             JpayIntegratedListService jpayIntegratedListService,
                             IntegratedCheckService integratedCheckService,
                             JpayTradeApiService jpayTradeApiService,
                             OutcomeReasonTranslateService outcomeReasonTranslateService) {
        this.payListService = payListService;
        this.payListActionService = payListActionService;
        this.chillPayService = chillPayService;
        this.hqNotifyMappingService = hqNotifyMappingService;
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.orgAccessService = orgAccessService;
        this.jpayIntegratedListService = jpayIntegratedListService;
        this.integratedCheckService = integratedCheckService;
        this.jpayTradeApiService = jpayTradeApiService;
        this.outcomeReasonTranslateService = outcomeReasonTranslateService;
    }

    private static PageResult<Map<String, Object>> emptyChillPayPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }

    /**
     * 칠페이 API는 가맹점 코드 1건 위주 — 비관리자는 허용 범위와 교차 적용.
     * 하위 가맹이 여러 곳이면 통합내역·통합정산에서 가맹점 코드(또는 MID)를 지정해 검색해야 합니다.
     * <p>예외: 로그인 조직이 <strong>총본사(HEADQUARTERS)</strong>이면 본사 ChillPay 자격으로 범위를 넓혀
     * (요청 필터가 비어 있을 때) 빈 문자열을 반환합니다. 그렇지 않으면 가맹이 2곳 이상일 때 {@code __NONE__}으로
     * 동일 MID에 섞인 타 조직 거래가 노출되는 것을 막습니다.
     */
    private String resolveChillPayMerchantCodeFilter(Authentication authentication, String requested) {
        Set<String> allowed = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowed == null) {
            return requested;
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

    /** 총본사 소속(ADMIN 은 visible 이 null 이라 여기까지 오지 않음) */
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

    /**
     * 통합정산 상단 「상태」그룹 → 칠페이 Search API 의 Status(단일 문자열)에 대응.
     * 세부(환불 vs 강제환불·성공 제외 등)는 ChillPayService 에서 ICOPAY DB로 보조 필터합니다.
     */
    private static String chillPayApiStatusFromSearchGroup(String searchStatusGroup) {
        if (searchStatusGroup == null || searchStatusGroup.isBlank()) {
            return "";
        }
        String g = searchStatusGroup.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(g) || "EXCLUDE_SUCCESS".equals(g)) {
            return "";
        }
        return switch (g) {
            case "SUCCESS" -> "Paid";
            case "FAIL" -> "Failed";
            case "CANCEL" -> "Cancelled";
            case "VOID" -> "Voided";
            case "MANUAL_VOID" -> "EmailVoid";
            case "REFUND", "FORCE_REFUND" -> "Refunded";
            default -> "";
        };
    }

    @GetMapping("/payList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payList(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            Authentication authentication) {
        PayListSearchRequest req = PayListSearchRequest.fromParams(params);
        req.setAdminUiLocale(LoginNoticePublicService.pickLangBucket(acceptLanguage));
        PageResult<Map<String, Object>> result = payListService.search(req, authentication);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 노티매핑설정의 columnCatalogs·pageCatalogAssignments 를 반영한 결제내역 계열 그리드 레이아웃.
     * 탭 제목(catalogDisplayTitle)·열 순서/노출·2단 헤더(headerGroups)에 사용합니다.
     */
    @GetMapping("/payListScreenLayout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payListScreenLayout(
            @RequestParam("pageUrl") String pageUrl) {
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyMappingService.resolvePayListScreenLayout(pageUrl)));
    }

    /**
     * 결제내역 처리사유 — UI 언어 전환 시 목록 재조회 없이 캐시·사전 번역만 반환합니다.
     */
    @PostMapping("/outcomeReasonTranslate")
    public ResponseEntity<ApiResponse<Map<String, String>>> outcomeReasonTranslate(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        List<String> texts = new ArrayList<>();
        Object raw = body != null ? body.get("texts") : null;
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String t = String.valueOf(item).trim();
                    if (!t.isEmpty()) {
                        texts.add(t);
                    }
                }
            }
        }
        String locale = resolveOutcomeReasonUiLocale(body, acceptLanguage);
        Map<String, String> translated = outcomeReasonTranslateService.translateBatchFromCacheOnly(texts, locale);
        return ResponseEntity.ok(ApiResponse.ok(translated));
    }

    /**
     * ChillPay Transaction Services — Search Payment Transaction (실시간).
     * 문서: ChillPay-API-Transaction-Services-Document-EN_v1.0.6 Table 1.2~1.3
     */
    @GetMapping("/chillPayTrSearch")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> chillPayTrSearch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchOrderBy,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchMerchantCode,
            @RequestParam(required = false) String searchPaymentChannel,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchChillStatus,
            @RequestParam(required = false) String searchRouteNo,
            @RequestParam(required = false) String searchFieldType,
            /** 결제내역과 동일: ICOPAY 상태(tb_pg_trnsctn 보강 후) 기준 필터 */
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        try {
            AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
            boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                    PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
            String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
            String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                    user, orgUnitRepository, commissionPolicyRepository, ledgerCur);

            String sftRaw = searchFieldType;
            boolean unified = false;
            String ft = "";
            if (sftRaw != null && !sftRaw.isBlank()) {
                unified = true;
                ft = sftRaw.trim().toUpperCase(Locale.ROOT);
            }
            String kw = searchKeyword != null ? searchKeyword.trim() : "";
            String sk = unified ? "" : (searchKeyword != null ? searchKeyword.trim() : "");
            String smc = unified ? "" : (searchMerchantCode != null ? searchMerchantCode.trim() : "");
            String spc = unified ? "" : (searchPaymentChannel != null ? searchPaymentChannel.trim() : "");
            String son = unified ? "" : (searchOrderNo != null ? searchOrderNo.trim() : "");
            String scs = unified ? "" : (searchChillStatus != null ? searchChillStatus.trim() : "");
            String srn = unified ? "" : (searchRouteNo != null ? searchRouteNo.trim() : "");

            if (unified) {
                switch (ft) {
                    case "ALL" -> sk = kw;
                    case "MID", "COMP_ID" -> smc = kw;
                    case "ORDER_NO" -> son = kw;
                    case "APPROVAL_NO" -> sk = kw;
                    case "ROUTE" -> srn = kw;
                    case "STATUS" -> scs = kw;
                    case "CUSTOMER_ID", "AMOUNT", "CURRENCY", "COMP_NM" -> sk = kw;
                    default -> sk = kw;
                }
            }

            Integer routeNo = null;
            if (srn != null && !srn.isBlank()) {
                try {
                    routeNo = Integer.parseInt(srn.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }

            String merchantFilter = resolveChillPayMerchantCodeFilter(authentication,
                    smc != null && !smc.isEmpty() ? smc : null);
            if ("__NONE__".equals(merchantFilter)) {
                return ResponseEntity.ok(ApiResponse.ok(emptyChillPayPage(page, size)));
            }
            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null && tTo == null) {
                tTo = LocalDate.now();
                tFrom = tTo.minusDays(1);
            }
            PageResult<Map<String, Object>> r = chillPayService.searchChillPayPaymentTransactions(
                    null,
                    page,
                    size,
                    searchOrderBy,
                    searchOrderDir,
                    sk,
                    merchantFilter,
                    spc,
                    routeNo,
                    son,
                    scs,
                    tFrom,
                    tTo,
                    searchPayDivCd,
                    multiCurrency,
                    primaryCurrency,
                    authentication);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    private static final int DAILY_CHILL_SUMMARY_MAX_DAYS = 93;

    /**
     * 통합내역(칠페이 결제 검색)과 동일 자격·필터로, 거래일자(TransactionDate) 구간을 일 단위로 나눠
     * 일자별 총건수·상태 버킷·금액 요약(meta)을 채웁니다. 상세는 해당 일자로 {@code /api/calc/chillPayTrSearch} 를 호출합니다.
     */
    @GetMapping("/dailyChillIntegratedSummary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dailyChillIntegratedSummary(
            @RequestParam(required = false) String searchOrderBy,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchMerchantCode,
            @RequestParam(required = false) String searchPaymentChannel,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchChillStatus,
            @RequestParam(required = false) String searchRouteNo,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        try {
            AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
            boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                    PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
            String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
            String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                    user, orgUnitRepository, commissionPolicyRepository, ledgerCur);

            String sftRaw = searchFieldType;
            boolean unified = false;
            String ft = "";
            if (sftRaw != null && !sftRaw.isBlank()) {
                unified = true;
                ft = sftRaw.trim().toUpperCase(Locale.ROOT);
            }
            String kw = searchKeyword != null ? searchKeyword.trim() : "";
            String sk = unified ? "" : (searchKeyword != null ? searchKeyword.trim() : "");
            String smc = unified ? "" : (searchMerchantCode != null ? searchMerchantCode.trim() : "");
            String spc = unified ? "" : (searchPaymentChannel != null ? searchPaymentChannel.trim() : "");
            String son = unified ? "" : (searchOrderNo != null ? searchOrderNo.trim() : "");
            String scs = unified ? "" : (searchChillStatus != null ? searchChillStatus.trim() : "");
            String srn = unified ? "" : (searchRouteNo != null ? searchRouteNo.trim() : "");

            if (unified) {
                switch (ft) {
                    case "ALL" -> sk = kw;
                    case "MID", "COMP_ID" -> smc = kw;
                    case "ORDER_NO" -> son = kw;
                    case "APPROVAL_NO" -> sk = kw;
                    case "ROUTE" -> srn = kw;
                    case "STATUS" -> scs = kw;
                    case "CUSTOMER_ID", "AMOUNT", "CURRENCY", "COMP_NM" -> sk = kw;
                    default -> sk = kw;
                }
            }

            Integer routeNo = null;
            if (srn != null && !srn.isBlank()) {
                try {
                    routeNo = Integer.parseInt(srn.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }

            String merchantFilter = resolveChillPayMerchantCodeFilter(authentication,
                    smc != null && !smc.isEmpty() ? smc : null);
            if ("__NONE__".equals(merchantFilter)) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("list", List.of());
                empty.put("meta", Map.of("note", "조회 가능한 가맹 범위가 없습니다."));
                return ResponseEntity.ok(ApiResponse.ok(empty));
            }

            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null || tTo == null) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.", "VALIDATION"));
            }
            if (tFrom.isAfter(tTo)) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자 시작이 종료보다 늦을 수 없습니다.", "VALIDATION"));
            }
            long span = ChronoUnit.DAYS.between(tFrom, tTo) + 1;
            if (span > DAILY_CHILL_SUMMARY_MAX_DAYS) {
                return ResponseEntity.ok(ApiResponse.fail("조회 기간은 " + DAILY_CHILL_SUMMARY_MAX_DAYS + "일 이내로 지정해 주세요.", "VALIDATION"));
            }
            ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            LocalDate today = LocalDate.now(ledgerTz);
            LocalDate effectiveTo = tTo.isAfter(today) ? today : tTo;
            if (tFrom.isAfter(effectiveTo)) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("list", List.of());
                empty.put("meta", Map.of("note", "조회 구간에 포함된 일자가 없습니다(미래 일자는 표시하지 않습니다)."));
                return ResponseEntity.ok(ApiResponse.ok(empty));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (LocalDate d = effectiveTo; !d.isBefore(tFrom); d = d.minusDays(1)) {
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("day", d.toString());
                try {
                    PageResult<Map<String, Object>> pr = chillPayService.searchChillPayPaymentTransactionsDailySummary(
                            null,
                            searchOrderBy,
                            searchOrderDir,
                            sk,
                            merchantFilter,
                            spc,
                            routeNo,
                            son,
                            scs,
                            d,
                            d,
                            searchPayDivCd,
                            multiCurrency,
                            primaryCurrency,
                            authentication);
                    one.put("totalElements", pr.getTotalElements());
                    Map<String, Object> meta = pr.getMeta() != null ? new LinkedHashMap<>(pr.getMeta()) : new LinkedHashMap<>();
                    one.put("meta", meta);
                    one.put("statusBucketCounts", statusBucketCountsFromPayListStatusBar(meta));
                    String note = buildChillDailySummaryNote(meta);
                    if (!note.isEmpty()) {
                        one.put("note", note);
                    }
                } catch (IllegalStateException ex) {
                    one.put("error", ex.getMessage());
                    one.put("totalElements", 0L);
                    one.put("statusBucketCounts", Map.of());
                }
                rows.add(one);
            }
            PayListService.applyDailySummaryDayListOrder(rows, searchOrderDir);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("list", rows);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("dailyChillNote", "일자별 상세는 동일 조건으로 chillPayTrSearch(통합내역) 에 해당 일자만 지정해 조회합니다.");
            if (tTo.isAfter(today)) {
                meta.put("displayToDate", today.toString());
                meta.put("requestedToDate", tTo.toString());
            }
            payload.put("meta", meta);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    @GetMapping("/dailyJpayIntegratedSummary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dailyJpayIntegratedSummary(
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate) {
        try {
            String sk = searchKeyword != null ? searchKeyword.trim() : "";
            String son = searchOrderNo != null ? searchOrderNo.trim() : "";
            String sft = searchFieldType != null ? searchFieldType.trim().toUpperCase(Locale.ROOT) : "";
            if (!sft.isEmpty() && !sk.isEmpty()) {
                switch (sft) {
                    case "ORDER_NO" -> {
                        son = sk;
                        sk = "";
                    }
                    case "APPROVAL_NO", "MID" -> { /* searchKeyword blob filter */ }
                    case "ALL" -> { /* keep sk */ }
                    default -> { /* keep sk */ }
                }
            }

            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null || tTo == null) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자 시작·종료(searchFromDate, searchToDate)는 필수입니다.", "VALIDATION"));
            }
            if (tFrom.isAfter(tTo)) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자 시작이 종료보다 늦을 수 없습니다.", "VALIDATION"));
            }
            long span = ChronoUnit.DAYS.between(tFrom, tTo) + 1;
            if (span > DAILY_CHILL_SUMMARY_MAX_DAYS) {
                return ResponseEntity.ok(ApiResponse.fail("조회 기간은 " + DAILY_CHILL_SUMMARY_MAX_DAYS + "일 이내로 지정해 주세요.", "VALIDATION"));
            }
            ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            LocalDate today = LocalDate.now(ledgerTz);
            LocalDate effectiveTo = tTo.isAfter(today) ? today : tTo;
            if (tFrom.isAfter(effectiveTo)) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("list", List.of());
                empty.put("meta", Map.of("note", "조회 구간에 포함된 일자가 없습니다(미래 일자는 표시하지 않습니다)."));
                return ResponseEntity.ok(ApiResponse.ok(empty));
            }

            Map<String, Object> payload = jpayIntegratedListService.buildDailyIntegratedSummary(
                    tFrom, tTo, effectiveTo, sk, son, searchPayDivCd, searchOrderDir);
            if (tTo.isAfter(today)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = payload.get("meta") instanceof Map<?, ?> m
                        ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
                meta.put("displayToDate", today.toString());
                meta.put("requestedToDate", tTo.toString());
                payload.put("meta", meta);
            }
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "JPAY"));
        }
    }

    /** 통합체크 — JPAY 일별(조회통합) vs ICOPAY 일별(일별결제) 대조 */
    @GetMapping("/integratedCheckSummary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> integratedCheckSummary(
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        try {
            String sk = searchKeyword != null ? searchKeyword.trim() : "";
            String son = searchOrderNo != null ? searchOrderNo.trim() : "";
            String sft = searchFieldType != null ? searchFieldType.trim().toUpperCase(Locale.ROOT) : "";
            if (!sft.isEmpty() && !sk.isEmpty()) {
                switch (sft) {
                    case "ORDER_NO" -> {
                        son = sk;
                        sk = "";
                    }
                    case "APPROVAL_NO", "MID" -> { /* searchKeyword blob filter */ }
                    case "ALL" -> { /* keep sk */ }
                    default -> { /* keep sk */ }
                }
            }
            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null || tTo == null) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자(searchFromDate, searchToDate)는 필수입니다.", "VALIDATION"));
            }
            if (tFrom.isAfter(tTo)) {
                return ResponseEntity.ok(ApiResponse.fail("거래일자 시작이 종료보다 늦을 수 없습니다.", "VALIDATION"));
            }
            long span = ChronoUnit.DAYS.between(tFrom, tTo) + 1;
            if (span > DAILY_CHILL_SUMMARY_MAX_DAYS) {
                return ResponseEntity.ok(ApiResponse.fail("조회 기간은 " + DAILY_CHILL_SUMMARY_MAX_DAYS + "일 이내로 지정해 주세요.", "VALIDATION"));
            }
            PayListSearchRequest payReq = PayListSearchRequest.fromParams(params);
            Map<String, Object> payload = integratedCheckService.buildIntegratedCheckSummary(
                    tFrom, tTo, sk, son, searchPayDivCd, searchOrderDir, payReq, authentication);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "JPAY"));
        }
    }

    private static Map<String, Long> statusBucketCountsFromPayListStatusBar(Map<String, Object> meta) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (meta == null) {
            return out;
        }
        Object barObj = meta.get("payListStatusBar");
        if (!(barObj instanceof Map<?, ?> bar)) {
            return out;
        }
        Object bucketsObj = bar.get("buckets");
        if (!(bucketsObj instanceof List<?> buckets)) {
            return out;
        }
        for (Object bObj : buckets) {
            if (!(bObj instanceof Map<?, ?> b)) {
                continue;
            }
            Object key = b.get("key");
            Object cnt = b.get("count");
            if (key == null) {
                continue;
            }
            long n = 0L;
            if (cnt instanceof Number num) {
                n = num.longValue();
            }
            out.put(String.valueOf(key), n);
        }
        return out;
    }

    private static String buildChillDailySummaryNote(Map<String, Object> meta) {
        if (meta == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (Boolean.TRUE.equals(meta.get("chillDailySummaryScanCapped"))) {
            parts.add("금액·상태 요약은 일 15,000건(500×30페이지) 스캔 상한까지 반영");
        }
        if (Boolean.TRUE.equals(meta.get("chillDailySummaryPayDivClientFiltered"))) {
            parts.add("상태구분은 ChillPay 목록 전페이지 매칭 집계");
        }
        return String.join(" · ", parts);
    }

    /** 결제내역과 동일 필터로 적재일(createdAt) 기준 일자별 집계. */
    @GetMapping("/dailyPaySummary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dailyPaySummary(@RequestParam Map<String, String> params,
                                                                            Authentication authentication) {
        PayListSearchRequest req = PayListSearchRequest.fromParams(params);
        LocalDate from = req.getSearchFromDate();
        LocalDate to = req.getSearchToDate();
        if (from == null || to == null) {
            return ResponseEntity.ok(ApiResponse.fail("거래일자(searchFromDate, searchToDate)는 필수입니다.", "VALIDATION"));
        }
        if (from.isAfter(to)) {
            return ResponseEntity.ok(ApiResponse.fail("거래일자 시작이 종료보다 늦을 수 없습니다.", "VALIDATION"));
        }
        try {
            req.setPayListVariant("INTEGRATED");
            List<Map<String, Object>> list = payListService.buildDailyPayListSummary(from, to, req, authentication);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("list", list);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("dailyPayNote", "일자별 상세는 결제내역(/calc/payList, INTEGRATED)과 동일 필터·적재일(createdAt) 기준으로 payList 에 해당 일자만 지정해 조회합니다.");
            ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            LocalDate today = LocalDate.now(ledgerTz);
            if (to.isAfter(today)) {
                meta.put("displayToDate", today.toString());
                meta.put("requestedToDate", to.toString());
            }
            payload.put("meta", meta);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /**
     * ChillPay Transaction Services — <strong>Search Settlement Transaction</strong>
     * ({@code /api/v1/settlement/search}, v1.0.6 Table 2.2) 를
     * <strong>PaymentDateFrom/To</strong> 중심으로 호출합니다.
     * (ICOPAY 내부 정산 실행·유통망 정산 테이블과 무관)
     */
    @GetMapping("/chillPaySettlementSearch")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> chillPaySettlementSearch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchOrderBy,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchMerchantCode,
            @RequestParam(required = false) String searchPaymentChannel,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchChillStatus,
            @RequestParam(required = false) String searchRouteNo,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchStatusGroup,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchTxnFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchTxnToDate,
            Authentication authentication) {
        try {
            AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
            boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                    PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
            String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
            String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                    user, orgUnitRepository, commissionPolicyRepository, ledgerCur);

            String sftRaw = searchFieldType;
            boolean unified = false;
            String ft = "";
            if (sftRaw != null && !sftRaw.isBlank()) {
                unified = true;
                ft = sftRaw.trim().toUpperCase(Locale.ROOT);
            }
            String kw = searchKeyword != null ? searchKeyword.trim() : "";
            String sg = searchStatusGroup != null ? searchStatusGroup.trim().toUpperCase(Locale.ROOT) : "ALL";
            if (sg.isEmpty()) {
                sg = "ALL";
            }
            if (unified && "COMP_NM".equals(ft) && kw.isEmpty()) {
                ft = "ALL";
            }

            String sk = "";
            String smc = unified ? "" : (searchMerchantCode != null ? searchMerchantCode.trim() : "");
            String spc = unified ? "" : (searchPaymentChannel != null ? searchPaymentChannel.trim() : "");
            String son = unified ? "" : (searchOrderNo != null ? searchOrderNo.trim() : "");
            String scs = unified ? "" : (searchChillStatus != null ? searchChillStatus.trim() : "");
            String srn = unified ? "" : (searchRouteNo != null ? searchRouteNo.trim() : "");

            if (unified) {
                switch (ft) {
                    case "ALL" -> sk = kw;
                    case "COMP_NM" -> { /* 아래에서 업체코드로 치환 */ }
                    case "COMP_ID", "MID" -> smc = kw;
                    case "ORDER_NO" -> son = kw;
                    case "APPROVAL_NO" -> sk = kw;
                    case "ROUTE" -> srn = kw;
                    case "CURRENCY" -> sk = kw;
                    case "STATUS" -> scs = kw;
                    case "AMOUNT" -> sk = kw;
                    default -> {
                    }
                }
                if ("COMP_NM".equals(ft) && !kw.isEmpty()) {
                    Set<String> allowed = orgAccessService.visibleMerchantCompCodes(authentication);
                    Set<String> nm = new HashSet<>();
                    for (OrgUnit ou : orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, kw)) {
                        if (ou.getCode() == null || ou.getCode().isBlank()) {
                            continue;
                        }
                        String code = ou.getCode().trim();
                        if (allowed == null || allowed.contains(code)) {
                            nm.add(code);
                        }
                    }
                    if (nm.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.ok(emptyChillPayPage(page, size)));
                    }
                    if (nm.size() > 1) {
                        PageResult<Map<String, Object>> empty = emptyChillPayPage(page, size);
                        Map<String, Object> meta = new java.util.LinkedHashMap<>();
                        meta.put("searchNote", "업체명이 여러 건과 일치합니다. 업체코드·MID로 좁혀 주세요.");
                        empty.setMeta(meta);
                        return ResponseEntity.ok(ApiResponse.ok(empty));
                    }
                    smc = nm.iterator().next();
                }
                String fromGroup = chillPayApiStatusFromSearchGroup(sg);
                if (!fromGroup.isEmpty()) {
                    scs = fromGroup;
                } else if ("STATUS".equals(ft) && !kw.isEmpty()) {
                    scs = kw;
                }
            } else {
                sk = searchKeyword != null ? searchKeyword.trim() : "";
                smc = searchMerchantCode != null ? searchMerchantCode.trim() : "";
                spc = searchPaymentChannel != null ? searchPaymentChannel.trim() : "";
                son = searchOrderNo != null ? searchOrderNo.trim() : "";
                scs = searchChillStatus != null ? searchChillStatus.trim() : "";
                srn = searchRouteNo != null ? searchRouteNo.trim() : "";
            }

            Integer routeNo = null;
            if (srn != null && !srn.isBlank()) {
                try {
                    routeNo = Integer.parseInt(srn.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }

            String merchantFilter = resolveChillPayMerchantCodeFilter(authentication,
                    smc != null && !smc.isEmpty() ? smc : null);
            if ("__NONE__".equals(merchantFilter)) {
                return ResponseEntity.ok(ApiResponse.ok(emptyChillPayPage(page, size)));
            }
            String icopayPost = unified && needsChillSettlementIcopayPostFilter(sg) ? sg : null;
            PageResult<Map<String, Object>> r = chillPayService.searchChillPaySettlementTransactions(
                    null,
                    page,
                    size,
                    searchOrderBy,
                    searchOrderDir,
                    sk,
                    merchantFilter,
                    spc,
                    routeNo,
                    son,
                    scs,
                    searchFromDate,
                    searchToDate,
                    searchTxnFromDate,
                    searchTxnToDate,
                    multiCurrency,
                    primaryCurrency,
                    authentication,
                    icopayPost);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    private static boolean needsChillSettlementIcopayPostFilter(String sg) {
        if (sg == null || sg.isEmpty() || "ALL".equals(sg)) {
            return false;
        }
        return switch (sg) {
            case "REFUND", "FORCE_REFUND", "VOID", "MANUAL_VOID", "EXCLUDE_SUCCESS" -> true;
            default -> false;
        };
    }

    /** JPAY Trade API — 단건 상태 재조회 후 결제내역 반영 */
    @PostMapping("/jpayTradeQuery")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpayTradeQuery(@RequestBody Map<String, String> body) {
        try {
            String trnId = body != null ? body.get("trnId") : null;
            String txnId = body != null ? firstNonBlank(body.get("transactionId"), body.get("chillTransactionId")) : null;
            if (trnId != null && !trnId.isBlank()) {
                return ResponseEntity.ok(ApiResponse.ok(jpayTradeApiService.queryAndApplyToTxn(trnId.trim())));
            }
            if (txnId != null && !txnId.isBlank()) {
                return ResponseEntity.ok(ApiResponse.ok(jpayTradeApiService.queryAndApplyByChillTransactionId(txnId.trim())));
            }
            return ResponseEntity.ok(ApiResponse.fail("trnId 또는 transactionId(승인번호)가 필요합니다.", "VALIDATION"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "JPAY"));
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    /**
     * JPAY 통합내역 — 포털 Export 비동기 동기화 시작.
     * 즉시 반환(RUNNING)하므로 프록시·게이트웨이 504가 발생하지 않습니다. 진행 상태는 {@code /jpayTrSyncStatus} 폴링.
     */
    @PostMapping("/jpayTrSync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpayTrSync(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false, defaultValue = "false") boolean fullResync) {
        try {
            JpaySyncTrigger trigger;
            if (fullResync) {
                trigger = JpaySyncTrigger.FULL_RESYNC;
            } else if (searchFromDate != null || searchToDate != null) {
                trigger = JpaySyncTrigger.EXPLICIT_RANGE;
            } else {
                trigger = JpaySyncTrigger.MANUAL;
            }
            return ResponseEntity.ok(ApiResponse.ok(
                    jpayIntegratedListService.startSyncJob(searchFromDate, searchToDate, trigger)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "JPAY"));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.ok(ApiResponse.fail("JPAY 포털 동기화 시작 실패: " + msg, "JPAY"));
        }
    }

    /** JPAY 통합내역 — 비동기 동기화 진행 상태(폴링) */
    @GetMapping("/jpayTrSyncStatus")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpayTrSyncStatus() {
        return ResponseEntity.ok(ApiResponse.ok(jpayIntegratedListService.syncJobStatusMap()));
    }

    /** JPAY 통합내역 — 마지막 동기화 캐시 목록 */
    @GetMapping("/jpayTrSearch")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> jpayTrSearch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(defaultValue = "false") boolean autoSync,
            Authentication authentication) {
        try {
            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null && tTo == null) {
                tTo = LocalDate.now();
                int days = hqLedgerSysSettingsService.getOrCreate().getJpayTrRecentSyncDays() != null
                        ? hqLedgerSysSettingsService.getOrCreate().getJpayTrRecentSyncDays() : 7;
                tFrom = tTo.minusDays(Math.max(1, days) - 1L);
            }
            /* 조회(GET)는 캐시만 읽음. Playwright Export 동기화는 POST /jpayTrSync 전용(504 방지). */
            PageResult<Map<String, Object>> r = jpayIntegratedListService.search(
                    page, size, searchKeyword, searchOrderNo, searchPayDivCd, tFrom, tTo, false, searchFieldType,
                    authentication);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "JPAY"));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.ok(ApiResponse.fail("통합조회 조회 실패: " + msg, "JPAY"));
        }
    }

    /** 결제내역 후속조치: 자동무효·이메일무효·자동환불·강제환불 (본사 환경설정 Y 일 때만) */
    @PostMapping("/payAction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payAction(@RequestBody Map<String, String> body) {
        try {
            String trnId = body != null ? body.get("trnId") : null;
            String action = body != null ? body.get("action") : null;
            String reason = body != null ? body.get("reason") : null;
            payListActionService.apply(SecurityContextHolder.getContext().getAuthentication(), trnId, action, reason);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "처리되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }

    private static String resolveOutcomeReasonUiLocale(Map<String, Object> body, String acceptLanguage) {
        if (body != null) {
            Object locObj = body.get("locale");
            if (locObj != null) {
                String raw = String.valueOf(locObj).trim();
                if (!raw.isEmpty()) {
                    return OutcomeReasonTranslateService.normalizeLocale(raw);
                }
            }
        }
        return LoginNoticePublicService.pickLangBucket(acceptLanguage);
    }
}
