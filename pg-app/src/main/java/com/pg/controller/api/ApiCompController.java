package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/comp", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiCompController {

    private final CompService compService;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;

    public ApiCompController(CompService compService, OrgUnitRepository orgUnitRepository, AuthService authService) {
        this.compService = compService;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
    }

    @GetMapping("/changeHistory")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> changeHistory(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> result = compService.changeHistory(searchCompId, null, null, page, size);
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String scopeCompId = null;
        if (Boolean.TRUE.equals(myOrgOnly)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser u) {
                if ("ADMIN".equalsIgnoreCase(u.getRole())) {
                    scopeCompId = null;
                } else {
                    Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                    if (org != null && org.get("compId") != null) {
                        scopeCompId = org.get("compId").toString();
                    }
                }
            }
        }
        PageResult<Map<String, Object>> result = compService.search(
                searchCompId, searchCompNm, searchCompDiv, searchUseYn, searchPayHoldYn,
                searchCeoNm, searchTerminalId, searchCeoMobile, searchRegNo, searchIncludeSub,
                page, size, scopeCompId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 지역 본사(업체) 상세 - 업체정보조회 화면에서 사용 */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam String compId) {
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
            @RequestParam(required = false) String ceoNm,
            @RequestParam(required = false) String ceoMobile,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String loginId,
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
            @RequestParam(required = false) String pwd,
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
            @RequestParam(required = false) String pgBindings,
            @RequestParam(required = false) String webPaymentUseYn,
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String defaultProductName,
            @RequestParam(required = false) String defaultProductCode,
            @RequestParam(required = false) String defaultProductAmount,
            @RequestParam(required = false) String defaultProductDesc,
            @RequestParam(required = false) String notifyUrlBackground,
            @RequestParam(required = false) String notifyUrlResult,
            @RequestParam(required = false) String commissionFollowHq,
            @RequestParam(required = false) String perTxFee,
            @RequestParam(required = false) String cancelRate,
            @RequestParam(required = false) String usageRate,
            @RequestParam(required = false) String failFee,
            @RequestParam(required = false) String payRate,
            @RequestParam(required = false) String refundRate,
            @RequestParam(required = false) String rollingPct,
            @RequestParam(required = false) String rollingDays,
            @RequestParam(required = false) String feeSettlementPerTx,
            @RequestParam(required = false) String feeUsdt,
            @RequestParam(required = false) String feeFx) {
        Long parentIdVal = parentId;
        if (parentIdVal == null && parentComp != null && !parentComp.isEmpty()) {
            String trimmed = parentComp.trim();
            // 숫자만 들어온 경우: OrgUnit PK로 간주
            if (trimmed.matches("\\d+")) {
                parentIdVal = Long.parseLong(trimmed);
            } else {
                // 그 외에는 상위업체 코드(업체코드)로 간주 → OrgUnit.code 로 조회
                parentIdVal = orgUnitRepository.findByCode(trimmed)
                        .map(OrgUnit::getId)
                        .orElse(null);
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
                compTel, zipCode, addr, addrDetail,
                ceoNm, ceoMobile, useYn, loginId,
                regNo, bizType, industry, bizNature, product, homepage, settleName, settleTelNo, settleType, commissionRate, limitAmt, fax,
                email, pwd,
                bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder,
                countryCd, swift, branchName, branchAddr, contactTel, walletAddress, networkName, siteUrl, siteSummary,
                remark,
                withdrawDays, withdrawStartTime, withdrawEndTime, payLimitDefault, payLimitExtra, payLimitAlertSms,
                holdRateFollowHq, holdRate, holdDaysInt, calcCycle, calcCloseTime, transferType, transferCycleDaysInt, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget, calcStartTime,
                pgBindings, webPaymentUseYn, baseCurrency,
                defaultProductName, defaultProductCode, defaultProductAmount, defaultProductDesc,
                notifyUrlBackground, notifyUrlResult,
                commissionFollowHq, perTxFee, cancelRate, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                feeSettlementPerTx, feeUsdt, feeFx);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("compId", saved.getCode(), "compNm", saved.getName())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
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
            @RequestParam(required = false) String ceoNm,
            @RequestParam(required = false) String ceoMobile,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String loginId,
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
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String siteUrl,
            @RequestParam(required = false) String siteSummary,
            @RequestParam(required = false) String pgBindings) {
        var targetOpt = compService.getDetail(compId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        String targetCompDiv = (String) targetOpt.get().get("compDiv");
        if ("REGIONAL".equals(targetCompDiv)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser u) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                if (org != null && compId.equals(org.get("compId")) && "REGIONAL".equals(org.get("orgLevel"))) {
                    return ResponseEntity.ok(ApiResponse.fail("본사는 직접 수정할 수 없습니다. 총본사만 수정 가능합니다.", "NO_PERMISSION"));
                }
            }
        }
        try {
            boolean ok = compService.update(compId, compNm, compDiv, parentId, compTel, zipCode, addr, addrDetail,
                    ceoNm, ceoMobile, useYn, loginId, regNo, bizType, industry, bizNature, product, homepage, settleName, settleTelNo, fax, email,
                    bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder, remark, commissionConfigAllowed, webPaymentUseYn, baseCurrency, siteUrl, siteSummary, pgBindings);
            return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다."))
                    : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 업체 비밀번호 초기화 - 기본 비밀번호(test123!)로 재설정 */
    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestParam String compId) {
        boolean ok = compService.resetPassword(compId);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "비밀번호가 초기화되었습니다.", "tempPassword", "test123!"))
                : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
    }

    /** 업체 로그인ID 변경 */
    @PostMapping("/changeLoginId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeLoginId(
            @RequestParam String compId,
            @RequestParam String newLoginId) {
        try {
            boolean ok = compService.changeLoginId(compId, newLoginId.trim());
            return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "로그인ID가 변경되었습니다."))
                    : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/settlementSetting")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementSetting(@RequestParam String compId) {
        return compService.getSettlementSetting(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체 또는 정산설정을 찾을 수 없습니다.", "NOT_FOUND")));
    }

    @PostMapping("/settlementSetting/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementSettingSave(
            @RequestParam String compId,
            @RequestParam(required = false) String withdrawLimitDays,
            @RequestParam(required = false) String payLimitDefault,
            @RequestParam(required = false) String payLimitExtra,
            @RequestParam(required = false) String holdRate,
            @RequestParam(required = false) String holdDays,
            @RequestParam(required = false) String calcCycle,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String autoTransferMin,
            @RequestParam(required = false) String payHoldYn) {
        Integer withdrawDays = null;
        if (withdrawLimitDays != null && !withdrawLimitDays.trim().isEmpty()) {
            try { withdrawDays = Integer.parseInt(withdrawLimitDays.trim()); } catch (NumberFormatException ignored) {}
        }
        Integer holdDaysInt = null;
        if (holdDays != null && !holdDays.trim().isEmpty()) {
            try { holdDaysInt = Integer.parseInt(holdDays.trim()); } catch (NumberFormatException ignored) {}
        }
        boolean ok = compService.saveSettlementSetting(compId, withdrawDays, payLimitDefault, payLimitExtra,
                holdRate, holdDaysInt, calcCycle, transferType, autoTransferMin, payHoldYn);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
    }
}
