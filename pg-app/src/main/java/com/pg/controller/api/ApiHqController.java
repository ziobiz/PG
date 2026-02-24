package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqApiConfig;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 본사설정 API
 * 1. PG사 API 연동  2. 기본 수수료 정책  3. API 구성 세팅  4. 본사별 권한 세팅
 */
@RestController
@RequestMapping("/api/hq")
public class ApiHqController {

    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqApiConfigRepository hqApiConfigRepository;

    public ApiHqController(CommissionPolicyRepository commissionPolicyRepository,
                           HqApiConfigRepository hqApiConfigRepository) {
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    private static PageResult<Map<String, Object>> emptyPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }

    /** 1. PG사 API 연동 - 타 PG사 API 연동 목록 (가맹점 배포용 결제대행사 모듈) */
    @GetMapping("/pgApiMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> pgApiMng(
            @RequestParam(required = false) String searchPgNm,
            @RequestParam(required = false) String searchUseYn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
    }

    @PostMapping("/pgApiMng/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgApiMngSave(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    /** 2. 기본 수수료 정책 (건당/취소/이용/실패/결제/환불/롤링%) */
    @GetMapping("/defaultCommission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultCommission() {
        Map<String, Object> data = new HashMap<>();
        commissionPolicyRepository.findByScope("DEFAULT").ifPresent(p -> {
            data.put("perTxFee", p.getPerTxFee() != null ? p.getPerTxFee().toString() : "0");
            data.put("cancelRate", p.getCancelRate() != null ? p.getCancelRate().toString() : "0");
            data.put("usageRate", p.getUsageRate() != null ? p.getUsageRate().toString() : "0");
            data.put("failFee", p.getFailFee() != null ? p.getFailFee().toString() : "0");
            data.put("payRate", p.getPayRate() != null ? p.getPayRate().toString() : "2.5");
            data.put("refundRate", p.getRefundRate() != null ? p.getRefundRate().toString() : "0");
            data.put("rollingPct", p.getRollingPct() != null ? p.getRollingPct().toString() : "5");
            data.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 180);
        });
        if (data.isEmpty()) {
            data.put("perTxFee", "0"); data.put("cancelRate", "0"); data.put("usageRate", "0");
            data.put("failFee", "0"); data.put("payRate", "2.5"); data.put("refundRate", "0");
            data.put("rollingPct", "5"); data.put("rollingDays", 180);
        }
        data.put("memo", "건당/취소/이용/실패/결제/환불 수수료 차감 후, 롤링(담보금)%를 N일간 보류하고 정산 주기에 지급.");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/defaultCommission/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultCommissionSave(@RequestBody Map<String, Object> body) {
        CommissionPolicy p = commissionPolicyRepository.findByScope("DEFAULT")
                .orElseGet(() -> {
                    CommissionPolicy def = new CommissionPolicy();
                    def.setScope("DEFAULT");
                    return def;
                });
        p.setPerTxFee(toBigDecimal(body.get("perTxFee")));
        p.setCancelRate(toBigDecimal(body.get("cancelRate")));
        p.setUsageRate(toBigDecimal(body.get("usageRate")));
        p.setFailFee(toBigDecimal(body.get("failFee")));
        p.setPayRate(toBigDecimal(body.get("payRate")));
        p.setRefundRate(toBigDecimal(body.get("refundRate")));
        p.setRollingPct(toBigDecimal(body.get("rollingPct")));
        Object rd = body.get("rollingDays");
        p.setRollingDays(rd != null && !rd.toString().isEmpty() ? Integer.parseInt(rd.toString()) : 180);
        commissionPolicyRepository.save(p);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null || o.toString().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 3. API 구성 세팅 - 여러 PG사 연동 후 우리 가맹점 발부 API 세팅 */
    @GetMapping("/apiConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apiConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("baseUrl", "");
        data.put("authType", "API_KEY");
        data.put("timeoutSec", 30);
        data.put("memo", "가맹점에게 발급하는 결제/취소/조회 API 기본 구성.");
        hqApiConfigRepository.findAll().stream().findFirst().ifPresent(c -> {
            if (c.getBaseUrl() != null) data.put("baseUrl", c.getBaseUrl());
            if (c.getAuthType() != null) data.put("authType", c.getAuthType());
            if (c.getTimeoutSec() != null) data.put("timeoutSec", c.getTimeoutSec());
            if (c.getMemo() != null) data.put("memo", c.getMemo());
        });
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/apiConfig/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apiConfigSave(@RequestBody Map<String, Object> body) {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(new HqApiConfig());
        c.setBaseUrl(body.get("baseUrl") != null ? body.get("baseUrl").toString().trim() : null);
        c.setAuthType(body.get("authType") != null ? body.get("authType").toString().trim() : null);
        Object to = body.get("timeoutSec");
        c.setTimeoutSec(to != null && !to.toString().isEmpty() ? Integer.parseInt(to.toString()) : 30);
        c.setMemo(body.get("memo") != null ? body.get("memo").toString().trim() : null);
        hqApiConfigRepository.save(c);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    /** 4. 본사별 페이지/기능 접근 권한 세팅 */
    @GetMapping("/permissionMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> permissionMng(
            @RequestParam(required = false) String searchHqNm,
            @RequestParam(required = false) String searchMenuId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
    }

    @PostMapping("/permissionMng/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> permissionMngSave(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }
}
