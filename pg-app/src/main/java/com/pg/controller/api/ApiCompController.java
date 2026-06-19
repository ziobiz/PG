package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.StyledExcelExportRequest;
import com.pg.entity.AppUser;
import com.pg.entity.MerchantReceivable;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import com.pg.service.ExcelStyledExportService;
import com.pg.util.ChatbotProductPricingUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.pg.service.ChatbotHeaderLogoUploadService;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(value = "/api/comp", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiCompController {

    private static final Logger log = LoggerFactory.getLogger(ApiCompController.class);

    private final CompService compService;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final ExcelStyledExportService excelStyledExportService;
    private final ChatbotHeaderLogoUploadService chatbotHeaderLogoUploadService;
    private final MerchantReceivableRepository merchantReceivableRepository;

    public ApiCompController(CompService compService, OrgUnitRepository orgUnitRepository, AuthService authService,
                             ExcelStyledExportService excelStyledExportService,
                             ChatbotHeaderLogoUploadService chatbotHeaderLogoUploadService,
                             MerchantReceivableRepository merchantReceivableRepository) {
        this.compService = compService;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.excelStyledExportService = excelStyledExportService;
        this.chatbotHeaderLogoUploadService = chatbotHeaderLogoUploadService;
        this.merchantReceivableRepository = merchantReceivableRepository;
    }

    @GetMapping("/changeHistory")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> changeHistory(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchChangedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> result = compService.changeHistory(
                searchCompId, searchCompNm, searchChangedBy, searchFromDate, searchToDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompDiv,
            @RequestParam(required = false) String searchUseYn,
            @RequestParam(required = false) String searchPayHoldYn,
            @RequestParam(required = false) String searchCeoNm,
            @RequestParam(required = false) String searchTerminalId,
            @RequestParam(required = false) String searchCeoMobile,
            @RequestParam(required = false) String searchRegNo,
            @RequestParam(required = false) Boolean searchIncludeSub,
            @RequestParam(required = false) String searchParentCompId,
            @RequestParam(required = false) Boolean myOrgOnly,
            @RequestParam(required = false) Boolean includeLoginOrgInScope,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String scopeCompId = null;
        boolean scopeSubtreeBelow = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppUser appUser = (auth != null && auth.getPrincipal() instanceof AppUser au) ? au : null;

        String viewerOrgLevel = null;
        if (Boolean.TRUE.equals(myOrgOnly) || (appUser != null && !"ADMIN".equalsIgnoreCase(appUser.getRole()))) {
            if (appUser != null) {
                Map<String, Object> org = authService.getOrgInfo(appUser.getUsername());
                if (org != null && org.get("compId") != null) {
                    scopeCompId = org.get("compId").toString().trim();
                }
                if (org != null && org.get("orgLevel") != null) {
                    viewerOrgLevel = org.get("orgLevel").toString();
                }
            }
            if (Boolean.TRUE.equals(myOrgOnly) && (scopeCompId == null || scopeCompId.isEmpty())) {
                return ResponseEntity.ok(ApiResponse.ok(emptyCompPage(page, size)));
            }
        }
        if (appUser != null && !"ADMIN".equalsIgnoreCase(appUser.getRole())
                && (scopeCompId == null || scopeCompId.isBlank())) {
            return ResponseEntity.ok(ApiResponse.ok(emptyCompPage(page, size)));
        }
        if (!Boolean.TRUE.equals(myOrgOnly) && appUser != null && !"ADMIN".equalsIgnoreCase(appUser.getRole())) {
            scopeSubtreeBelow = true;
            searchCompDiv = compService.sanitizeSearchCompDivForSubtreeViewer(viewerOrgLevel, searchCompDiv);
        }

        String effectiveSearchUseYn = searchUseYn;
        if ((Boolean.TRUE.equals(myOrgOnly) || scopeSubtreeBelow) && (searchUseYn == null || searchUseYn.isBlank())) {
            effectiveSearchUseYn = "ALL";
        }

        boolean includeSelfInScope = Boolean.TRUE.equals(includeLoginOrgInScope);
        PageResult<Map<String, Object>> result = compService.search(
                searchCompId, searchCompNm, searchCompDiv, effectiveSearchUseYn, searchPayHoldYn,
                searchCeoNm, searchTerminalId, searchCeoMobile, searchRegNo, searchIncludeSub,
                page, size, scopeCompId, scopeSubtreeBelow, includeSelfInScope);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 지역 본사(업체) 상세 - 업체정보조회 화면에서 사용 */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
                String target = compId != null ? compId.trim() : "";
                if (mine.isEmpty() || target.isEmpty() || !compService.isTargetUnderViewerOrg(mine, target)) {
                    return ResponseEntity.ok(ApiResponse.fail("소속 업체 및 하위 업체만 조회할 수 있습니다.", "FORBIDDEN"));
                }
            }
        }
        return compService.getDetail(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND")));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestParam(required = false) String compNm,
            @RequestParam(required = false) String compDiv,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String parentComp,
            @RequestParam(required = false) String compTel,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String addr,
            @RequestParam(required = false) String addrDetail,
            @RequestParam(required = false) String addrEtc,
            @RequestParam(required = false) String addrCountryCd,
            @RequestParam(required = false) String ceoNm,
            @RequestParam(required = false) String ceoMobile,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String pwd,
            @RequestParam(required = false) String regNo,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String bizNature,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String homepage,
            @RequestParam(required = false) String settleName,
            @RequestParam(required = false) String settleTelNo,
            @RequestParam(required = false) String settleType,
            @RequestParam(required = false) String commissionRate,
            @RequestParam(required = false) String limitAmt,
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String bankCd,
            @RequestParam(required = false) String transferFee,
            @RequestParam(required = false) String cryptoTransferFee,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String countryCd,
            @RequestParam(required = false) String swift,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String branchAddr,
            @RequestParam(required = false) String contactTel,
            @RequestParam(required = false) String walletAddress,
            @RequestParam(required = false) String networkName,
            @RequestParam(required = false) String siteUrl,
            @RequestParam(required = false) String siteSummary,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String withdrawRestrictType,
            @RequestParam(required = false) String withdrawLimitDays,
            @RequestParam(required = false) String withdrawStartTime,
            @RequestParam(required = false) String withdrawEndTime,
            @RequestParam(required = false) String payLimitDefault,
            @RequestParam(required = false) String payLimitExtra,
            @RequestParam(required = false) String payLimitAlertSms,
            @RequestParam(required = false) String holdRateFollowHq,
            @RequestParam(required = false) String holdRate,
            @RequestParam(required = false) String holdDays,
            @RequestParam(required = false) String calcCycle,
            @RequestParam(required = false) String calcCloseTime,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String transferCycleDays,
            @RequestParam(required = false) String autoTransferMin,
            @RequestParam(required = false) String payHoldYn,
            @RequestParam(required = false) String calcExcludeYn,
            @RequestParam(required = false) String calcExcludeTarget,
            @RequestParam(required = false) String calcStartTime,
            @RequestParam(required = false) String calcProcType,
            @RequestParam(required = false) String calcMinAmt,
            @RequestParam(required = false) String transferExecTime,
            @RequestParam(required = false) String pgBindings,
            @RequestParam(required = false) String webPaymentUseYn,
            @RequestParam(required = false) String chatbotPaymentUseYn,
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String defaultProductName,
            @RequestParam(required = false) String defaultProductCode,
            @RequestParam(required = false) String defaultProductAmount,
            @RequestParam(required = false) String defaultProductDesc,
            @RequestParam(required = false) String notifyUrlBackground,
            @RequestParam(required = false) String notifyUrlResult,
            @RequestParam(required = false) String jpayNotifyUrl,
            @RequestParam(required = false) String jpayCallbackUrl,
            @RequestParam(required = false) String notifyUrl1,
            @RequestParam(required = false) String notifyUrl2,
            @RequestParam(required = false) String notifyUrl3,
            @RequestParam(required = false) String notifyUrl4,
            @RequestParam(required = false) String middlewareNotifyUrl,
            @RequestParam(required = false) String middlewareNotifySecret,
            @RequestParam(required = false) String commissionFollowHq,
            @RequestParam(required = false) String hqPolicyScope,
            @RequestParam(required = false) String perTxFee,
            @RequestParam(required = false) String cancelRate,
            @RequestParam(required = false) String voidFeePerTx,
            @RequestParam(required = false) String manualVoidFeePerTx,
            @RequestParam(required = false) String usageRate,
            @RequestParam(required = false) String failFee,
            @RequestParam(required = false) String payRate,
            @RequestParam(required = false) String refundRate,
            @RequestParam(required = false) String rollingPct,
            @RequestParam(required = false) String rollingDays,
            @RequestParam(required = false) String feeSettlementPerTx,
            @RequestParam(required = false) String remittanceTransferFee,
            @RequestParam(required = false) String usdtTransferFeeUsd,
            @RequestParam(required = false) String feeUsdt,
            @RequestParam(required = false) String feeFx,
            @RequestParam(required = false) String fee3dsRate,
            @RequestParam(required = false) String chargebackFeePerTx,
            @RequestParam(required = false) String chargebackPolicyId,
            @RequestParam(required = false) String voidSettlementMode,
            @RequestParam(required = false) String manualVoidSettlementMode,
            @RequestParam(required = false) String refundSettlementMode,
            @RequestParam(required = false) String forceRefundSettlementMode,
            @RequestParam(required = false) String payFollowMerchantUseYn,
            @RequestParam(required = false) String payFollowAutoVoidYn,
            @RequestParam(required = false) String payFollowEmailVoidYn,
            @RequestParam(required = false) String payFollowAutoRefundYn,
            @RequestParam(required = false) String payFollowForceRefundYn,
            @RequestParam(required = false) String urlPayAlertEmailYn,
            @RequestParam(required = false) String urlPayLineNotifyToken,
            @RequestParam(required = false) String feeVatApplyYn,
            @RequestParam(required = false) String feeVatRatePct,
            @RequestParam(required = false) String regionalSettings,
            @RequestParam(required = false) String urlPayCheckoutMode,
            @RequestParam(required = false) String urlPayProductNameUseYn,
            @RequestParam(required = false) String urlPayCompanyNameShowYn,
            @RequestParam(required = false) String urlPayLangMenuUseYn,
            @RequestParam(required = false) String apiUrlPayCheckoutMode,
            @RequestParam(required = false) String chatbotUrlPayCheckoutMode,
            @RequestParam(required = false) String apiJpaySubscriptionUseYn,
            @RequestParam(required = false) String apiBrokerInlineUseYn,
            @RequestParam(required = false) String apiBrokerRedirectUseYn,
            @RequestParam(required = false) String apiWordpressUseYn,
            @RequestParam(required = false) String jpayCheckoutFieldMode,
            @RequestParam(required = false) String jpayPhoneDialCodeYn,
            @RequestParam(required = false) String tabletFeatureUseYn) {
        Long parentIdVal = parentId;
        if (parentIdVal == null && parentComp != null && !parentComp.isEmpty()) {
            String trimmed = parentComp.trim();
            // 우선 업체코드(문자열)로 조회하고, 없을 때만 PK 숫자값으로 해석
            parentIdVal = orgUnitRepository.findByCode(trimmed)
                    .map(OrgUnit::getId)
                    .orElse(null);
            if (parentIdVal == null && trimmed.matches("\\d+")) {
                parentIdVal = Long.parseLong(trimmed);
            }
        }
        Integer withdrawDays = null;
        if (withdrawLimitDays != null && !withdrawLimitDays.trim().isEmpty()) {
            try { withdrawDays = Integer.parseInt(withdrawLimitDays.trim()); } catch (NumberFormatException ignored) {}
        }
        Integer holdDaysInt = null;
        if (holdDays != null && !holdDays.trim().isEmpty()) {
            try { holdDaysInt = Integer.parseInt(holdDays.trim()); } catch (NumberFormatException ignored) {}
        }
        Integer transferCycleDaysInt = null;
        if (transferCycleDays != null && !transferCycleDays.trim().isEmpty()) {
            try { transferCycleDaysInt = Integer.parseInt(transferCycleDays.trim()); } catch (NumberFormatException ignored) {}
        }
        try {
        OrgUnit saved = compService.registerWithExtra(null, compNm, compDiv, parentIdVal,
                compTel, zipCode, addr, addrDetail, addrEtc, addrCountryCd,
                ceoNm, ceoMobile, useYn, loginId,
                regNo, bizType, industry, bizNature, product, homepage, settleName, settleTelNo, settleType, commissionRate, limitAmt, fax,
                email, pwd,
                bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder,
                countryCd, swift, branchName, branchAddr, contactTel, walletAddress, networkName, siteUrl, siteSummary,
                remark,
                withdrawRestrictType,
                withdrawDays, withdrawStartTime, withdrawEndTime, payLimitDefault, payLimitExtra, payLimitAlertSms,
                holdRateFollowHq, holdRate, holdDaysInt, calcCycle, calcCloseTime, transferType, transferCycleDaysInt, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget, calcStartTime, calcProcType, calcMinAmt, transferExecTime,
                pgBindings, webPaymentUseYn, chatbotPaymentUseYn, baseCurrency,
                defaultProductName, defaultProductCode, defaultProductAmount, defaultProductDesc,
                notifyUrlBackground, notifyUrlResult,
                jpayNotifyUrl, jpayCallbackUrl,
                notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4,
                middlewareNotifyUrl, middlewareNotifySecret,
                commissionFollowHq, hqPolicyScope, perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx, fee3dsRate, chargebackFeePerTx, chargebackPolicyId,
                voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode,
                payFollowMerchantUseYn, payFollowAutoVoidYn, payFollowEmailVoidYn, payFollowAutoRefundYn, payFollowForceRefundYn,
                urlPayAlertEmailYn, urlPayLineNotifyToken,
                feeVatApplyYn, feeVatRatePct,
                regionalSettings,
                urlPayCheckoutMode,
                urlPayProductNameUseYn,
                urlPayCompanyNameShowYn,
                urlPayLangMenuUseYn,
                apiUrlPayCheckoutMode,
                chatbotUrlPayCheckoutMode,
                apiJpaySubscriptionUseYn,
                apiBrokerInlineUseYn,
                apiBrokerRedirectUseYn,
                apiWordpressUseYn,
                jpayCheckoutFieldMode,
                jpayPhoneDialCodeYn,
                tabletFeatureUseYn);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("compId", saved.getCode(), "compNm", saved.getName())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (DataIntegrityViolationException e) {
            log.warn("comp register data integrity: {}", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
            return ResponseEntity.ok(ApiResponse.fail(
                    "저장 중 DB 제약 오류가 났습니다. 노티 URL이 너무 길지 않은지 확인하고, 운영 DB에 db/V48_merchant_notify_url_length.sql 적용 여부를 확인하세요.",
                    "DATA_INTEGRITY"));
        }
    }

    @GetMapping("/check-login-id")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkLoginId(
            @RequestParam(required = false) String loginId) {
        String lid = loginId != null ? loginId.trim() : "";
        if (lid.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("로그인ID를 입력하세요.", "VALIDATION"));
        }
        boolean available = compService.isLoginIdAvailable(lid);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("loginId", lid, "available", available)));
    }

    /**
     * 가맹 챗봇 관리자 로그인ID 저장 전 중복·소속 검증.
     */
    @GetMapping("/check-chatbot-admin-username")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkChatbotAdminUsername(
            @RequestParam String compId,
            @RequestParam(required = false) String chatbotAdminUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        String cid = compId != null ? compId.trim() : "";
        if (!canAccessCompAsViewer(u0, cid)) {
            return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
        }
        String cu = chatbotAdminUsername != null ? chatbotAdminUsername.trim() : "";
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("compId", cid);
        body.put("chatbotAdminUsername", cu);
        try {
            compService.validateChatbotAdminUsernameAssignable(cid, chatbotAdminUsername);
            body.put("available", true);
        } catch (IllegalArgumentException e) {
            body.put("available", false);
            body.put("message", e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    /** 지역 본사(업체) 정보 수정 - 업체정보조회 상세에서 저장. 본사(REGIONAL)는 총본사만 수정 가능. parentId로 소속 이동. */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestParam String compId,
            @RequestParam(required = false) String compNm,
            @RequestParam(required = false) String compDiv,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String compTel,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String addr,
            @RequestParam(required = false) String addrDetail,
            @RequestParam(required = false) String addrEtc,
            @RequestParam(required = false) String addrCountryCd,
            @RequestParam(required = false) String ceoNm,
            @RequestParam(required = false) String ceoMobile,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String pwd,
            @RequestParam(required = false) String regNo,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String bizNature,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String homepage,
            @RequestParam(required = false) String settleName,
            @RequestParam(required = false) String settleTelNo,
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String bankCd,
            @RequestParam(required = false) String transferFee,
            @RequestParam(required = false) String cryptoTransferFee,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String commissionConfigAllowed,
            @RequestParam(required = false) String webPaymentUseYn,
            @RequestParam(required = false) String webPaymentHeaderLogoMode,
            @RequestParam(required = false) String webPaymentHeaderLogoUrl,
            @RequestParam(required = false) String webPaymentHeaderSubtitleMode,
            @RequestParam(required = false) String webPaymentHeaderSubtitleText,
            @RequestParam(required = false) String chatbotPaymentUseYn,
            @RequestParam(required = false) Integer chatbotProductSlotLimit,
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String siteUrl,
            @RequestParam(required = false) String siteSummary,
            @RequestParam(required = false) String pgBindings,
            @RequestParam(required = false) String regionalSettings,
            @RequestParam(required = false) String assistantLoginId,
            @RequestParam(required = false) String assistantPwd,
            @RequestParam(required = false) String assistantRoleType,
            @RequestParam(required = false) String brandingEditAllowedYn,
            @RequestParam(required = false) String defaultProductName,
            @RequestParam(required = false) String defaultProductCode,
            @RequestParam(required = false) String defaultProductAmount,
            @RequestParam(required = false) String defaultProductDesc,
            @RequestParam(required = false) String notifyUrlBackground,
            @RequestParam(required = false) String notifyUrlResult,
            @RequestParam(required = false) String jpayNotifyUrl,
            @RequestParam(required = false) String jpayCallbackUrl,
            @RequestParam(required = false) String notifyUrl1,
            @RequestParam(required = false) String notifyUrl2,
            @RequestParam(required = false) String notifyUrl3,
            @RequestParam(required = false) String notifyUrl4,
            @RequestParam(required = false) String middlewareNotifyUrl,
            @RequestParam(required = false) String middlewareNotifySecret,
            @RequestParam(required = false) String commissionFollowHq,
            @RequestParam(required = false) String hqPolicyScope,
            @RequestParam(required = false) String perTxFee,
            @RequestParam(required = false) String cancelRate,
            @RequestParam(required = false) String voidFeePerTx,
            @RequestParam(required = false) String manualVoidFeePerTx,
            @RequestParam(required = false) String usageRate,
            @RequestParam(required = false) String failFee,
            @RequestParam(required = false) String payRate,
            @RequestParam(required = false) String refundRate,
            @RequestParam(required = false) String rollingPct,
            @RequestParam(required = false) String rollingDays,
            @RequestParam(required = false) String feeSettlementPerTx,
            @RequestParam(required = false) String remittanceTransferFee,
            @RequestParam(required = false) String usdtTransferFeeUsd,
            @RequestParam(required = false) String feeUsdt,
            @RequestParam(required = false) String feeFx,
            @RequestParam(required = false) String fee3dsRate,
            @RequestParam(required = false) String chargebackFeePerTx,
            @RequestParam(required = false) String chargebackPolicyId,
            @RequestParam(required = false) String voidSettlementMode,
            @RequestParam(required = false) String manualVoidSettlementMode,
            @RequestParam(required = false) String refundSettlementMode,
            @RequestParam(required = false) String forceRefundSettlementMode,
            @RequestParam(required = false) String payFollowMerchantUseYn,
            @RequestParam(required = false) String payFollowAutoVoidYn,
            @RequestParam(required = false) String payFollowEmailVoidYn,
            @RequestParam(required = false) String payFollowAutoRefundYn,
            @RequestParam(required = false) String payFollowForceRefundYn,
            @RequestParam(required = false) String urlPayAlertEmailYn,
            @RequestParam(required = false) String urlPayLineNotifyToken,
            @RequestParam(required = false) String chatbotHeaderLogoUrl,
            @RequestParam(required = false) String chatbotAdminUsername,
            @RequestParam(required = false) String chatbotCatalogListingGrant,
            @RequestParam(required = false) Integer chatbotMaxProductImagesGrant,
            @RequestParam(required = false) String chatbotCatalogListingEnabled,
            @RequestParam(required = false) String chatbotPromotionShelfMode,
            @RequestParam(required = false) Integer chatbotPromotionRotateSeconds,
            @RequestParam(required = false) String urlPayCheckoutMode,
            @RequestParam(required = false) String urlPayProductNameUseYn,
            @RequestParam(required = false) String urlPayCompanyNameShowYn,
            @RequestParam(required = false) String urlPayLangMenuUseYn,
            @RequestParam(required = false) String apiUrlPayCheckoutMode,
            @RequestParam(required = false) String chatbotUrlPayCheckoutMode,
            @RequestParam(required = false) String apiJpaySubscriptionUseYn,
            @RequestParam(required = false) String apiBrokerInlineUseYn,
            @RequestParam(required = false) String apiBrokerRedirectUseYn,
            @RequestParam(required = false) String apiWordpressUseYn,
            @RequestParam(required = false) String jpayCheckoutFieldMode,
            @RequestParam(required = false) String jpayPhoneDialCodeYn,
            @RequestParam(required = false) String tabletFeatureUseYn) {
        var targetOpt = compService.getDetail(compId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("소속 업체 및 하위 업체만 수정할 수 있습니다.", "FORBIDDEN"));
            }
            if (!"ADMIN".equalsIgnoreCase(u0.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u0.getUsername());
                String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
                if (!mine.isEmpty() && compId.trim().equalsIgnoreCase(mine)) {
                    return ResponseEntity.ok(ApiResponse.fail(
                            "본인 소속 업체 정보는 조회 전용입니다. 변경은 상위 조직·관리자에서 진행하세요.", "READ_ONLY_SELF_COMP"));
                }
            }
        }
        try {
            boolean ok = compService.update(compId, compNm, compDiv, parentId, compTel, zipCode, addr, addrDetail, addrEtc, addrCountryCd,
                    ceoNm, ceoMobile, useYn, loginId, pwd, regNo, bizType, industry, bizNature, product, homepage, settleName, settleTelNo, fax, email,
                    bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder, remark, commissionConfigAllowed, webPaymentUseYn,
                    webPaymentHeaderLogoMode, webPaymentHeaderLogoUrl,
                    webPaymentHeaderSubtitleMode, webPaymentHeaderSubtitleText,
                    chatbotPaymentUseYn, chatbotProductSlotLimit, baseCurrency, siteUrl, siteSummary, pgBindings, regionalSettings,
                    assistantLoginId, assistantPwd, assistantRoleType, brandingEditAllowedYn,
                    defaultProductName, defaultProductCode, defaultProductAmount, defaultProductDesc,
                    notifyUrlBackground, notifyUrlResult,
                    jpayNotifyUrl, jpayCallbackUrl,
                    notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4,
                    middlewareNotifyUrl, middlewareNotifySecret,
                    commissionFollowHq, hqPolicyScope, perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                    feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx, fee3dsRate, chargebackFeePerTx, chargebackPolicyId,
                    voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode,
                    payFollowMerchantUseYn, payFollowAutoVoidYn, payFollowEmailVoidYn, payFollowAutoRefundYn, payFollowForceRefundYn,
                    urlPayAlertEmailYn, urlPayLineNotifyToken, chatbotHeaderLogoUrl, chatbotAdminUsername,
                    chatbotCatalogListingGrant, chatbotMaxProductImagesGrant, chatbotCatalogListingEnabled,
                    chatbotPromotionShelfMode, chatbotPromotionRotateSeconds,
                    urlPayCheckoutMode,
                    urlPayProductNameUseYn,
                    urlPayCompanyNameShowYn,
                    urlPayLangMenuUseYn,
                    apiUrlPayCheckoutMode,
                    chatbotUrlPayCheckoutMode,
                    apiJpaySubscriptionUseYn,
                    apiBrokerInlineUseYn,
                    apiBrokerRedirectUseYn,
                    apiWordpressUseYn,
                    jpayCheckoutFieldMode,
                    jpayPhoneDialCodeYn,
                    tabletFeatureUseYn);
            return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다."))
                    : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            log.warn("comp update data integrity: {}", cause);
            return ResponseEntity.ok(ApiResponse.fail(
                    "저장 중 DB 제약 오류가 났습니다. (" + (cause != null ? cause : "")
                            + ") 결제대행사(PG) 목록에 동일 PG·결제구분(WEB 등)이 중복되지 않는지, 총판·가맹 노티 URL(tb_merchant_notify_url 의 org_unit_id+url_type 중복), 노티 URL 길이(2048자) 등을 확인하세요. (tb_merchant_pg_binding 유니크 / V48 노티 URL 컬럼 길이)",
                    "DATA_INTEGRITY"));
        }
    }

    /** 업체 대표 계정 비밀번호 초기화 — 임시 비밀번호 로그인ID+1! */
    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestParam String compId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("소속 업체 및 하위 업체에 대해서만 비밀번호를 초기화할 수 있습니다.", "FORBIDDEN"));
            }
        }
        return compService.resetPassword(compId)
                .map(temp -> {
                    Map<String, Object> body = new java.util.LinkedHashMap<>();
                    body.put("success", true);
                    body.put("message", "비밀번호가 초기화되었습니다.");
                    body.put("tempPassword", temp);
                    return ResponseEntity.ok(ApiResponse.ok(body));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없거나 대표 로그인ID가 없습니다.", "NOT_FOUND")));
    }

    /** 보조(assistant) 계정 비밀번호 초기화 — 임시 비밀번호 보조로그인ID+1! */
    @PostMapping("/resetAssistantPassword")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetAssistantPassword(@RequestParam String compId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("소속 업체 및 하위 업체에 대해서만 비밀번호를 초기화할 수 있습니다.", "FORBIDDEN"));
            }
        }
        return compService.resetAssistantPassword(compId)
                .map(temp -> {
                    Map<String, Object> body = new java.util.LinkedHashMap<>();
                    body.put("success", true);
                    body.put("message", "비밀번호가 초기화되었습니다.");
                    body.put("tempPassword", temp);
                    return ResponseEntity.ok(ApiResponse.ok(body));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("보조 계정을 찾을 수 없습니다.", "NOT_FOUND")));
    }

    /** 업체 로그인ID 변경 */
    @PostMapping("/changeLoginId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeLoginId(
            @RequestParam String compId,
            @RequestParam String newLoginId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
            }
        }
        try {
            boolean ok = compService.changeLoginId(compId, newLoginId.trim());
            return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "로그인ID가 변경되었습니다."))
                    : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 가맹점 결제대행사 연동 1건 저장 (업체정보 상세) */
    @PostMapping(value = "/pgBinding/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgBindingSave(@RequestBody Map<String, Object> body) {
        try {
            String compId = body.get("compId") != null ? body.get("compId").toString().trim() : "";
            Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
            if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
                if (!canAccessCompAsViewer(u0, compId)) {
                    return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
                }
            }
            Long bindingId = null;
            Object idObj = body.get("id");
            if (idObj != null && !idObj.toString().isBlank()) {
                bindingId = Long.parseLong(idObj.toString().trim());
            }
            String pgCd = body.get("pgCd") != null ? body.get("pgCd").toString() : "";
            String payMethod = body.get("payMethod") != null ? body.get("payMethod").toString() : "WEB";
            String mid = body.get("mid") != null ? body.get("mid").toString() : "";
            String rootNo = body.get("rootNo") != null ? body.get("rootNo").toString() : "";
            String apiKey = body.get("apiKey") != null ? body.get("apiKey").toString() : "";
            String ivKey = body.get("ivKey") != null ? body.get("ivKey").toString() : "";
            String activationYn = body.get("activationYn") != null ? body.get("activationYn").toString() : "Y";
            String operationalYn = body.get("operationalYn") != null ? body.get("operationalYn").toString() : "N";
            String installmentYn = body.get("installmentYn") != null ? body.get("installmentYn").toString() : "N";
            String maxMo = body.get("maxInstallmentMonths") != null ? body.get("maxInstallmentMonths").toString() : "";
            String urlPayPricingMode = body.get("urlPayPricingMode") != null ? body.get("urlPayPricingMode").toString() : "";
            String cardBrandScope = body.get("cardBrandScope") != null ? body.get("cardBrandScope").toString() : "";
            boolean extPresent = body.containsKey("extSettleMode");
            String extMode = body.get("extSettleMode") != null ? body.get("extSettleMode").toString() : "";
            String extLag = body.get("extSettleLag") != null ? body.get("extSettleLag").toString() : "";
            String extBt = body.get("extSettleBatchTime") != null ? body.get("extSettleBatchTime").toString() : "";
            Map<String, Object> saved = compService.saveMerchantPgBinding(compId, bindingId, pgCd, payMethod,
                    mid, rootNo, apiKey, ivKey, activationYn, operationalYn, installmentYn, maxMo, urlPayPricingMode,
                    cardBrandScope,
                    extPresent, extMode, extLag, extBt);
            return ResponseEntity.ok(ApiResponse.ok(saved));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("ID 형식이 올바르지 않습니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @DeleteMapping("/pgBinding/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgBindingDelete(
            @PathVariable Long id,
            @RequestParam String compId) {
        try {
            Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
            if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
                if (!canAccessCompAsViewer(u0, compId)) {
                    return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
                }
            }
            compService.deleteMerchantPgBinding(compId, id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "삭제되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 업체 엑셀등록용 SAMPLE — 헤더 색·가운데 정렬·테두리·계좌번호 등 텍스트 서식 */
    @GetMapping(value = "/excelSample", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excelSample() {
        try {
            byte[] bytes = excelStyledExportService.buildCompRegisterSample();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comp_register_SAMPLE.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 그리드 목록 등 — 헤더/데이터 행을 JSON으로 받아 동일 서식의 xlsx 생성.
     */
    @PostMapping(value = "/exportStyledExcel", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportStyledExcel(@RequestBody StyledExcelExportRequest req) {
        try {
            if (req.getHeaders() == null || req.getHeaders().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            Set<Integer> textSet = new HashSet<>();
            if (req.getTextColumnIndexes() != null) {
                textSet.addAll(req.getTextColumnIndexes());
            }
            byte[] bytes = excelStyledExportService.buildStyledTable(
                    req.getSheetName(),
                    req.getHeaders(),
                    req.getRows(),
                    textSet);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 엑셀 파일 업로드로 업체 일괄 등록. 1행=헤더(업체명, 업체구분, 상위코드 등), 2행~=데이터 */
    @PostMapping(value = "/excelRegister", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> excelRegister(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("엑셀 파일을 선택하세요.", "VALIDATION"));
        }
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            return ResponseEntity.ok(ApiResponse.fail("xlsx 또는 xls 파일만 업로드 가능합니다.", "VALIDATION"));
        }
        try {
            Map<String, Object> result = compService.importFromExcel(file);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "엑셀 처리 중 오류가 발생했습니다.", "ERROR"));
        }
    }

    @GetMapping("/settlementSetting")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementSetting(@RequestParam String compId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
            }
        }
        return compService.getSettlementSetting(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체 또는 정산설정을 찾을 수 없습니다.", "NOT_FOUND")));
    }

    /** 챗봇관리 — 고객 안내(회사·연락처·소개 등, 병합 표시값). 가맹 로그인 포함. */
    @GetMapping("/chatbotKb")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbotKb(@RequestParam String compId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
        }
        return compService.getChatbotKbDisplay(compId.trim())
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND")));
    }

    /** 챗봇관리 — 비용관리(히스토리): 챗봇 상품등록 플랜 월이용료 미수금 내역 */
    @GetMapping("/chatbotKb/billingHistory")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> chatbotKbBillingHistory(
            @RequestParam String compId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
        }
        String mid = compId != null ? compId.trim() : "";
        if (mid.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("업체코드(compId)는 필수입니다.", "VALIDATION"));
        }
        int p = Math.max(1, page);
        int sz = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(p - 1, sz, Sort.by(Sort.Direction.DESC, "id"));

        Specification<MerchantReceivable> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("merchantId"), mid),
                root.get("reasonCode").in(List.of(
                        ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_MONTHLY,
                        ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_UPGRADE))
        );
        Page<MerchantReceivable> slice = merchantReceivableRepository.findAll(spec, pageable);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (MerchantReceivable r : slice.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle() != null ? r.getTitle() : "");
            m.put("totalAmount", r.getTotalAmount() != null ? r.getTotalAmount() : 0);
            m.put("remainingAmount", r.getRemainingAmount() != null ? r.getRemainingAmount() : 0);
            m.put("status", r.getStatus() != null ? r.getStatus() : "");
            m.put("memo", r.getMemo() != null ? r.getMemo() : "");
            m.put("reasonCode", r.getReasonCode() != null ? r.getReasonCode() : "");
            m.put("billingCcy", r.getBillingCcy() != null ? r.getBillingCcy() : "");
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            rows.add(m);
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(sz);
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));

        MerchantReceivable latest = merchantReceivableRepository
                .findFirstByMerchantIdAndReasonCodeInOrderByIdDesc(mid, List.of(
                        ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_MONTHLY,
                        ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_UPGRADE))
                .orElse(null);
        if (latest != null) {
            pr.getMeta().put("latestTitle", latest.getTitle() != null ? latest.getTitle() : "");
            pr.getMeta().put("latestTotalAmount", latest.getTotalAmount() != null ? latest.getTotalAmount() : 0);
            pr.getMeta().put("latestMemo", latest.getMemo() != null ? latest.getMemo() : "");
            pr.getMeta().put("latestCreatedAt", latest.getCreatedAt() != null ? latest.getCreatedAt().toString() : "");
        } else {
            pr.getMeta().put("latestTitle", "");
            pr.getMeta().put("latestTotalAmount", 0);
            pr.getMeta().put("latestMemo", "");
            pr.getMeta().put("latestCreatedAt", "");
        }
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /** 총본사·본사·총판 등: 산하 가맹점 중 챗봇결제 사용(Y)만 — 기본 안내(표시값) 목록 — 가맹점 로그인은 제외 */
    @GetMapping("/chatbotKb/merchantList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> chatbotKbMerchantList(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        boolean admin = "ADMIN".equalsIgnoreCase(u0.getRole());
        OrgLevel vl;
        String viewerComp = "";
        if (!admin) {
            Map<String, Object> org = authService.getOrgInfo(u0.getUsername());
            viewerComp = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
            String olStr = org != null && org.get("orgLevel") != null ? org.get("orgLevel").toString().trim() : "";
            try {
                vl = OrgLevel.valueOf(olStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(ApiResponse.fail("조직 정보를 확인할 수 없습니다.", "FORBIDDEN"));
            }
            if (vl == OrgLevel.MERCHANT) {
                return ResponseEntity.ok(ApiResponse.fail("가맹점 계정에서는 산하 목록 조회가 제공되지 않습니다.", "FORBIDDEN"));
            }
            if (viewerComp.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("업체 정보가 없습니다.", "FORBIDDEN"));
            }
        } else {
            vl = OrgLevel.HEADQUARTERS;
        }
        PageResult<Map<String, Object>> result =
                compService.searchChatbotKbMerchantsPage(admin, viewerComp, vl, searchCompId, searchCompNm, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 본사·총본사·총판 등: 산하 가맹 챗봇 상업 기능(상품·주문·예약) 운영 보류 설정. 가맹 계정 불가 */
    @PostMapping("/chatbotKb/commerceHold")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbotKbCommerceHold(
            @RequestParam String compId,
            @RequestParam String holdYn) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        boolean admin = "ADMIN".equalsIgnoreCase(u0.getRole());
        OrgLevel vl;
        String viewerComp = "";
        if (!admin) {
            Map<String, Object> org = authService.getOrgInfo(u0.getUsername());
            viewerComp = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
            String olStr = org != null && org.get("orgLevel") != null ? org.get("orgLevel").toString().trim() : "";
            try {
                vl = OrgLevel.valueOf(olStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(ApiResponse.fail("조직 정보를 확인할 수 없습니다.", "FORBIDDEN"));
            }
            if (vl == OrgLevel.MERCHANT) {
                return ResponseEntity.ok(ApiResponse.fail("가맹점 계정에서는 이 설정을 변경할 수 없습니다.", "FORBIDDEN"));
            }
            if (viewerComp.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("업체 정보가 없습니다.", "FORBIDDEN"));
            }
        } else {
            vl = OrgLevel.HEADQUARTERS;
        }
        if (!admin && !canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("저장 권한이 없습니다.", "FORBIDDEN"));
        }
        try {
            boolean hold = "Y".equalsIgnoreCase(holdYn != null ? holdYn.trim() : "");
            compService.saveChatbotCommerceHold(admin, viewerComp, vl, compId.trim(), hold);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "chatbotCommerceHoldYn", hold ? "Y" : "N")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/chatbotKb/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbotKbSave(
            @RequestParam String compId,
            @RequestParam(required = false) String chatbotKbCompanyNm,
            @RequestParam(required = false) String chatbotKbAddr,
            @RequestParam(required = false) String chatbotKbTel,
            @RequestParam(required = false) String chatbotKbEmail,
            @RequestParam(required = false) String chatbotKbContactNm,
            @RequestParam(required = false) String chatbotKbIntro,
            @RequestParam(required = false) String chatbotKbProductDesc,
            @RequestParam(required = false) String chatbotKbWelcomeHint,
            @RequestParam(required = false) String chatbotOperationMode,
            @RequestParam(required = false) String chatbotProductSlotLimit,
            @RequestParam(required = false) String chatbotProductSlotPlanUseSplit,
            @RequestParam(required = false) String chatbotProductSlotLimitNext,
            @RequestParam(required = false) String chatbotReservationSlotMinutes,
            @RequestParam(required = false) String chatbotReservationZoneId,
            @RequestParam(required = false) String chatbotCatalogListingEnabled,
            @RequestParam(required = false) String chatbotMerchantVertical,
            @RequestParam(required = false) String chatbotMerchantVerticalNotes,
            @RequestParam(required = false) String chatbotOrderSheetUiJson) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("저장 권한이 없습니다.", "FORBIDDEN"));
        }
        try {
            compService.saveMerchantChatbotKb(compId.trim(), chatbotKbCompanyNm, chatbotKbAddr,
                    chatbotKbTel, chatbotKbEmail, chatbotKbContactNm, chatbotKbIntro, chatbotKbProductDesc,
                    chatbotProductSlotLimit, chatbotProductSlotPlanUseSplit, chatbotProductSlotLimitNext,
                    chatbotOperationMode, chatbotKbWelcomeHint,
                    chatbotReservationSlotMinutes, chatbotReservationZoneId, chatbotCatalogListingEnabled,
                    chatbotMerchantVertical, chatbotMerchantVerticalNotes, chatbotOrderSheetUiJson);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 본사 AI설정 API를 사용해 회사소개·판매상품·첫화면 기본 안내 초안(kind=intro|product|welcome). */
    @PostMapping("/chatbotKb/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbotKbSuggest(
            @RequestParam String compId,
            @RequestParam(defaultValue = "intro") String kind) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        try {
            String text = compService.suggestChatbotKbDraft(compId.trim(), kind);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", text != null ? text : "");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "LLM 오류";
            return ResponseEntity.ok(ApiResponse.fail(msg, "LLM_ERROR"));
        }
    }

    /** 가맹 챗봇 상단 로고 이미지 업로드(자동 재압축·저장). 본사 AI설정 config_json 참고(chatbot_logo_* 키). */
    @PostMapping(value = "/chatbotHeaderLogo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbotHeaderLogoUpload(@RequestParam String compId,
                                                                                    @RequestParam("file") MultipartFile file) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("업로드 권한이 없습니다.", "FORBIDDEN"));
        }
        if (!"ADMIN".equalsIgnoreCase(u0.getRole())) {
            Map<String, Object> org = authService.getOrgInfo(u0.getUsername());
            String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
            if (!mine.isEmpty() && compId.trim().equalsIgnoreCase(mine)) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "본인 소속 업체는 조회만 가능합니다. 변경·업로드는 상위 조직 또는 관리자에서 진행하세요.",
                        "READ_ONLY_SELF_COMP"));
            }
        }
        Optional<OrgUnit> ou = chatbotHeaderLogoUploadService.requireMerchantOrg(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드만 업로드 가능합니다.", "INVALID"));
        }
        try {
            ChatbotHeaderLogoUploadService.UploadResult ur =
                    chatbotHeaderLogoUploadService.processAndPersist(ou.get().getId(), compId.trim(), file);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", ur.publicUrlRelative());
            payload.put("usedLlmTuningHint", ur.usedLlmTuningHint());
            payload.put("message", ur.detailMessage());
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "파일 처리 오류: " + (e.getMessage() != null ? e.getMessage() : "IO"), "IO_ERROR"));
        }
    }

    /** 가맹 웹결제(URL·JPAY) 상단 로고 이미지 업로드 — 로고설정「활성」 시 사용 */
    @PostMapping(value = "/webPaymentHeaderLogo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> webPaymentHeaderLogoUpload(@RequestParam String compId,
                                                                                     @RequestParam("file") MultipartFile file) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth0 != null && auth0.getPrincipal() instanceof AppUser u0)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessCompAsViewer(u0, compId)) {
            return ResponseEntity.ok(ApiResponse.fail("업로드 권한이 없습니다.", "FORBIDDEN"));
        }
        if (!"ADMIN".equalsIgnoreCase(u0.getRole())) {
            Map<String, Object> org = authService.getOrgInfo(u0.getUsername());
            String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
            if (!mine.isEmpty() && compId.trim().equalsIgnoreCase(mine)) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "본인 소속 업체는 조회만 가능합니다. 변경·업로드는 상위 조직 또는 관리자에서 진행하세요.",
                        "READ_ONLY_SELF_COMP"));
            }
        }
        Optional<OrgUnit> ou = chatbotHeaderLogoUploadService.requireMerchantOrg(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드만 업로드 가능합니다.", "INVALID"));
        }
        try {
            ChatbotHeaderLogoUploadService.UploadResult ur =
                    chatbotHeaderLogoUploadService.processAndPersistWebPaymentHeader(ou.get().getId(), compId.trim(), file);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", ur.publicUrlRelative());
            payload.put("usedLlmTuningHint", ur.usedLlmTuningHint());
            payload.put("message", ur.detailMessage());
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "파일 처리 오류: " + (e.getMessage() != null ? e.getMessage() : "IO"), "IO_ERROR"));
        }
    }

    @PostMapping("/settlementSetting/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementSettingSave(
            @RequestParam String compId,
            @RequestParam(required = false) String withdrawRestrictType,
            @RequestParam(required = false) String withdrawLimitDays,
            @RequestParam(required = false) String withdrawStartTime,
            @RequestParam(required = false) String withdrawEndTime,
            @RequestParam(required = false) String payLimitDefault,
            @RequestParam(required = false) String payLimitExtra,
            @RequestParam(required = false) String holdRate,
            @RequestParam(required = false) String holdDays,
            @RequestParam(required = false) String calcCycle,
            @RequestParam(required = false) String calcCloseTime,
            @RequestParam(required = false) String calcStartTime,
            @RequestParam(required = false) String transferCycleDays,
            @RequestParam(required = false) String calcProcType,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String autoTransferMin,
            @RequestParam(required = false) String payHoldYn,
            @RequestParam(required = false) String calcExcludeYn,
            @RequestParam(required = false) String calcExcludeTarget,
            @RequestParam(required = false) String calcMinAmt,
            @RequestParam(required = false) String transferExecTime,
            @RequestParam(required = false) String feeVatApplyYn,
            @RequestParam(required = false) String feeVatRatePct,
            @RequestParam(required = false) String calcCycleTransitionMode,
            @RequestParam(required = false) String calcCycleChangeRemark) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        String actor = "";
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            actor = u0.getUsername() != null ? u0.getUsername().trim() : "";
            if (!canAccessCompAsViewer(u0, compId)) {
                return ResponseEntity.ok(ApiResponse.fail("저장 권한이 없습니다.", "FORBIDDEN"));
            }
        }
        Integer withdrawDays = null;
        if (withdrawLimitDays != null && !withdrawLimitDays.trim().isEmpty()) {
            try { withdrawDays = Integer.parseInt(withdrawLimitDays.trim()); } catch (NumberFormatException ignored) {}
        }
        Integer holdDaysInt = null;
        if (holdDays != null && !holdDays.trim().isEmpty()) {
            try { holdDaysInt = Integer.parseInt(holdDays.trim()); } catch (NumberFormatException ignored) {}
        }
        Integer transferCycleDaysInt = null;
        if (transferCycleDays != null && !transferCycleDays.trim().isEmpty()) {
            try { transferCycleDaysInt = Integer.parseInt(transferCycleDays.trim()); } catch (NumberFormatException ignored) {}
        }
        boolean ok = compService.saveSettlementSetting(compId, withdrawRestrictType, withdrawDays,
                withdrawStartTime, withdrawEndTime,
                payLimitDefault, payLimitExtra,
                holdRate, holdDaysInt, calcCycle,
                calcCloseTime, calcStartTime, transferCycleDaysInt,
                calcProcType, transferType, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget,
                calcMinAmt, transferExecTime,
                feeVatApplyYn, feeVatRatePct,
                calcCycleTransitionMode, calcCycleChangeRemark, actor);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
    }

    private static PageResult<Map<String, Object>> emptyCompPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }

    /** ADMIN은 전체, 그 외 로그인 소속 업체·하위 업체만 */
    private boolean canAccessCompAsViewer(AppUser u, String targetCompId) {
        if (u == null) return false;
        if ("ADMIN".equalsIgnoreCase(u.getRole())) return true;
        if (targetCompId == null || targetCompId.isBlank()) return false;
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        return !mine.isEmpty() && compService.isTargetUnderViewerOrg(mine, targetCompId.trim());
    }
}
