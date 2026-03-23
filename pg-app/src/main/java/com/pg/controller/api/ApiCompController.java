package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.StyledExcelExportRequest;
import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import com.pg.service.ExcelStyledExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/comp", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiCompController {

    private final CompService compService;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final ExcelStyledExportService excelStyledExportService;

    public ApiCompController(CompService compService, OrgUnitRepository orgUnitRepository, AuthService authService,
                             ExcelStyledExportService excelStyledExportService) {
        this.compService = compService;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.excelStyledExportService = excelStyledExportService;
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
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                if (org != null && org.get("compId") != null) {
                    scopeCompId = org.get("compId").toString();
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
                String target = compId != null ? compId.trim() : "";
                if (mine.isEmpty() || target.isEmpty() || !mine.equals(target)) {
                    return ResponseEntity.ok(ApiResponse.fail("본인 소속 업체만 조회할 수 있습니다.", "FORBIDDEN"));
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
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String defaultProductName,
            @RequestParam(required = false) String defaultProductCode,
            @RequestParam(required = false) String defaultProductAmount,
            @RequestParam(required = false) String defaultProductDesc,
            @RequestParam(required = false) String notifyUrlBackground,
            @RequestParam(required = false) String notifyUrlResult,
            @RequestParam(required = false) String notifyUrl1,
            @RequestParam(required = false) String notifyUrl2,
            @RequestParam(required = false) String notifyUrl3,
            @RequestParam(required = false) String notifyUrl4,
            @RequestParam(required = false) String commissionFollowHq,
            @RequestParam(required = false) String hqPolicyScope,
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
            @RequestParam(required = false) String feeFx,
            @RequestParam(required = false) String regionalSettings) {
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
                withdrawDays, withdrawStartTime, withdrawEndTime, payLimitDefault, payLimitExtra, payLimitAlertSms,
                holdRateFollowHq, holdRate, holdDaysInt, calcCycle, calcCloseTime, transferType, transferCycleDaysInt, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget, calcStartTime, calcProcType, calcMinAmt, transferExecTime,
                pgBindings, webPaymentUseYn, baseCurrency,
                defaultProductName, defaultProductCode, defaultProductAmount, defaultProductDesc,
                notifyUrlBackground, notifyUrlResult,
                notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4,
                commissionFollowHq, hqPolicyScope, perTxFee, cancelRate, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                feeSettlementPerTx, feeUsdt, feeFx, regionalSettings);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("compId", saved.getCode(), "compNm", saved.getName())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
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
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String siteUrl,
            @RequestParam(required = false) String siteSummary,
            @RequestParam(required = false) String pgBindings,
            @RequestParam(required = false) String regionalSettings,
            @RequestParam(required = false) String assistantLoginId,
            @RequestParam(required = false) String assistantPwd,
            @RequestParam(required = false) String assistantRoleType,
            @RequestParam(required = false) String brandingEditAllowedYn,
            @RequestParam(required = false) String notifyUrl1,
            @RequestParam(required = false) String notifyUrl2,
            @RequestParam(required = false) String notifyUrl3,
            @RequestParam(required = false) String notifyUrl4) {
        var targetOpt = compService.getDetail(compId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!"ADMIN".equalsIgnoreCase(u0.getRole())) {
                Map<String, Object> org0 = authService.getOrgInfo(u0.getUsername());
                String mine0 = org0 != null && org0.get("compId") != null ? org0.get("compId").toString().trim() : "";
                if (mine0.isEmpty() || !mine0.equals(compId != null ? compId.trim() : "")) {
                    return ResponseEntity.ok(ApiResponse.fail("본인 소속 업체만 수정할 수 있습니다.", "FORBIDDEN"));
                }
            }
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
            boolean ok = compService.update(compId, compNm, compDiv, parentId, compTel, zipCode, addr, addrDetail, addrEtc, addrCountryCd,
                    ceoNm, ceoMobile, useYn, loginId, pwd, regNo, bizType, industry, bizNature, product, homepage, settleName, settleTelNo, fax, email,
                    bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder, remark, commissionConfigAllowed, webPaymentUseYn, baseCurrency, siteUrl, siteSummary, pgBindings, regionalSettings,
                    assistantLoginId, assistantPwd, assistantRoleType, brandingEditAllowedYn,
                    notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4);
            return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다."))
                    : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 업체 대표 계정 비밀번호 초기화 — 임시 비밀번호 로그인ID+1! */
    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestParam String compId) {
        Authentication auth0 = SecurityContextHolder.getContext().getAuthentication();
        if (auth0 != null && auth0.getPrincipal() instanceof AppUser u0) {
            if (!"ADMIN".equalsIgnoreCase(u0.getRole())) {
                Map<String, Object> org0 = authService.getOrgInfo(u0.getUsername());
                String mine0 = org0 != null && org0.get("compId") != null ? org0.get("compId").toString().trim() : "";
                if (mine0.isEmpty() || !mine0.equals(compId != null ? compId.trim() : "")) {
                    return ResponseEntity.ok(ApiResponse.fail("본인 소속 업체만 비밀번호를 초기화할 수 있습니다.", "FORBIDDEN"));
                }
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

    /** 가맹점 결제대행사 연동 1건 저장 (업체정보 상세) */
    @PostMapping(value = "/pgBinding/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgBindingSave(@RequestBody Map<String, Object> body) {
        try {
            String compId = body.get("compId") != null ? body.get("compId").toString().trim() : "";
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
            Map<String, Object> saved = compService.saveMerchantPgBinding(compId, bindingId, pgCd, payMethod,
                    mid, rootNo, apiKey, ivKey, activationYn, operationalYn, installmentYn, maxMo);
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
            @RequestParam(required = false) String transferExecTime) {
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
        boolean ok = compService.saveSettlementSetting(compId, withdrawDays, payLimitDefault, payLimitExtra,
                holdRate, holdDaysInt, calcCycle,
                calcCloseTime, calcStartTime, transferCycleDaysInt,
                calcProcType, transferType, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget,
                calcMinAmt, transferExecTime);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
    }
}
