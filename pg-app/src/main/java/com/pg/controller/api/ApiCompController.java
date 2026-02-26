package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.CompService;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comp")
public class ApiCompController {

    private final CompService compService;
    private final OrgUnitRepository orgUnitRepository;

    public ApiCompController(CompService compService, OrgUnitRepository orgUnitRepository) {
        this.compService = compService;
        this.orgUnitRepository = orgUnitRepository;
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> result = compService.search(searchCompId, searchCompNm, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 지역 본사(업체) 상세 - 업체정보조회 화면에서 사용 */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam String compId) {
        return compService.getDetail(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.")));
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
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String pwd,
            @RequestParam(required = false) String bankCd,
            @RequestParam(required = false) String transferFee,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String withdrawLimitDays,
            @RequestParam(required = false) String payLimitDefault,
            @RequestParam(required = false) String payLimitExtra,
            @RequestParam(required = false) String holdRate,
            @RequestParam(required = false) String holdDays,
            @RequestParam(required = false) String calcCycle,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String autoTransferMin,
            @RequestParam(required = false) String payHoldYn) {
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
        String code = "C" + System.currentTimeMillis();
        OrgUnit saved = compService.registerWithExtra(code, compNm, compDiv, parentIdVal,
                compTel, zipCode, addr, addrDetail,
                ceoNm, ceoMobile, useYn, loginId,
                regNo, bizType, industry, fax,
                email, pwd,
                bankCd, transferFee, accountNo, accountHolder,
                remark,
                withdrawDays, payLimitDefault, payLimitExtra, holdRate, holdDaysInt,
                calcCycle, transferType, autoTransferMin, payHoldYn);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("compId", saved.getCode(), "compNm", saved.getName())));
    }

    /** 지역 본사(업체) 정보 수정 - 업체정보조회 상세에서 저장 */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestParam String compId,
            @RequestParam(required = false) String compNm,
            @RequestParam(required = false) String compDiv,
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
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String bankCd,
            @RequestParam(required = false) String transferFee,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String commissionConfigAllowed) {
        boolean ok = compService.update(compId, compNm, compDiv, compTel, zipCode, addr, addrDetail,
                ceoNm, ceoMobile, useYn, loginId, regNo, bizType, industry, fax, email,
                bankCd, transferFee, accountNo, accountHolder, remark, commissionConfigAllowed);
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다."))
                : ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
    }

    @GetMapping("/settlementSetting")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementSetting(@RequestParam String compId) {
        return compService.getSettlementSetting(compId)
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.fail("업체 또는 정산설정을 찾을 수 없습니다.")));
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
        return ResponseEntity.ok(ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.fail("업체를 찾을 수 없습니다."));
    }
}
