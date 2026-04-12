package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.AppUser;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqNotifyMappingService;
import com.pg.service.OrgAccessService;
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
import java.util.ArrayList;
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

    public ApiCalcController(PayListService payListService, PayListActionService payListActionService,
                             ChillPayService chillPayService, HqNotifyMappingService hqNotifyMappingService,
                             OrgUnitRepository orgUnitRepository, CommissionPolicyRepository commissionPolicyRepository,
                             HqLedgerSysSettingsService hqLedgerSysSettingsService,
                             OrgAccessService orgAccessService) {
        this.payListService = payListService;
        this.payListActionService = payListActionService;
        this.chillPayService = chillPayService;
        this.hqNotifyMappingService = hqNotifyMappingService;
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.orgAccessService = orgAccessService;
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
     * 하위 가맹이 여러 곳이면 통합내역에서 가맹점 코드를 지정해 검색해야 합니다.
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
        return "__NONE__";
    }

    @GetMapping("/payList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payList(
            @RequestParam Map<String, String> params,
            Authentication authentication) {
        PayListSearchRequest req = PayListSearchRequest.fromParams(params);
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        try {
            Integer routeNo = null;
            if (searchRouteNo != null && !searchRouteNo.isBlank()) {
                try {
                    routeNo = Integer.parseInt(searchRouteNo.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }
            AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
            boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                    PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
            String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
            String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                    user, orgUnitRepository, commissionPolicyRepository, ledgerCur);
            String merchantFilter = resolveChillPayMerchantCodeFilter(authentication, searchMerchantCode);
            if ("__NONE__".equals(merchantFilter)) {
                return ResponseEntity.ok(ApiResponse.ok(emptyChillPayPage(page, size)));
            }
            LocalDate tFrom = searchFromDate;
            LocalDate tTo = searchToDate;
            if (tFrom == null && tTo == null) {
                var ls = hqLedgerSysSettingsService.getOrCreate();
                int days = ls.getChillpayTrRecentSyncDays() != null && ls.getChillpayTrRecentSyncDays() > 0
                        ? ls.getChillpayTrRecentSyncDays() : 2;
                tTo = LocalDate.now();
                tFrom = tTo.minusDays(Math.max(1, days) - 1L);
            }
            PageResult<Map<String, Object>> r = chillPayService.searchChillPayPaymentTransactions(
                    null,
                    page,
                    size,
                    searchOrderBy,
                    searchOrderDir,
                    searchKeyword,
                    merchantFilter,
                    searchPaymentChannel,
                    routeNo,
                    searchOrderNo,
                    searchChillStatus,
                    tFrom,
                    tTo,
                    multiCurrency,
                    primaryCurrency,
                    authentication);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    /**
     * ChillPay Transaction Services — Search Payment Transaction 을
     * <strong>PaymentDateFrom/To</strong> 중심으로 호출해 칠페이 정책상 정산·Settled·수수료 등을 조회합니다.
     * (ICOPAY 내부 정산 실행 로직과 무관)
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchTxnFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchTxnToDate,
            Authentication authentication) {
        try {
            Integer routeNo = null;
            if (searchRouteNo != null && !searchRouteNo.isBlank()) {
                try {
                    routeNo = Integer.parseInt(searchRouteNo.trim());
                } catch (NumberFormatException ignored) {
                    routeNo = null;
                }
            }
            AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
            boolean multiCurrency = PayListStatusBarBuckets.isMultiCurrencyViewer(
                    PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository));
            String ledgerCur = PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
            String primaryCurrency = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(
                    user, orgUnitRepository, commissionPolicyRepository, ledgerCur);
            String merchantFilter = resolveChillPayMerchantCodeFilter(authentication, searchMerchantCode);
            if ("__NONE__".equals(merchantFilter)) {
                return ResponseEntity.ok(ApiResponse.ok(emptyChillPayPage(page, size)));
            }
            PageResult<Map<String, Object>> r = chillPayService.searchChillPaySettlementTransactions(
                    null,
                    page,
                    size,
                    searchOrderBy,
                    searchOrderDir,
                    searchKeyword,
                    merchantFilter,
                    searchPaymentChannel,
                    routeNo,
                    searchOrderNo,
                    searchChillStatus,
                    searchFromDate,
                    searchToDate,
                    searchTxnFromDate,
                    searchTxnToDate,
                    multiCurrency,
                    primaryCurrency,
                    authentication);
            return ResponseEntity.ok(ApiResponse.ok(r));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY"));
        }
    }

    /** 결제내역 후속조치: 자동무효·이메일무효·자동환불·강제환불 (본사 환경설정 Y 일 때만) */
    @PostMapping("/payAction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payAction(@RequestBody Map<String, String> body) {
        try {
            String trnId = body != null ? body.get("trnId") : null;
            String action = body != null ? body.get("action") : null;
            payListActionService.apply(SecurityContextHolder.getContext().getAuthentication(), trnId, action);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "처리되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }
}
