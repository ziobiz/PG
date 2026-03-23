package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqApiConfig;
import com.pg.entity.PgAgency;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.PgAgencyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 본사설정 API
 * 1. PG사 API 연동  2. 기본 수수료 정책  3. API 구성 세팅  4. 본사별 권한 세팅
 */
@RestController
@RequestMapping(value = "/api/hq", produces = "application/json")
public class ApiHqController {
    private static final String TEMPLATE_SCOPE_PREFIX = "HQPOL:";

    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final PgAgencyRepository pgAgencyRepository;

    public ApiHqController(CommissionPolicyRepository commissionPolicyRepository,
                           HqApiConfigRepository hqApiConfigRepository,
                           PgAgencyRepository pgAgencyRepository) {
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.pgAgencyRepository = pgAgencyRepository;
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

    /** 1. PG사 API 연동 - 결제대행사 목록 (가맹점 배포용 결제 모듈) */
    @GetMapping("/pgApiMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> pgApiMng(
            @RequestParam(required = false) String searchPgNm,
            @RequestParam(required = false) String searchUseYn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<PgAgency> all = pgAgencyRepository.findAllByOrderByPgCdAsc();
        List<PgAgency> filtered = all.stream()
                .filter(p -> (searchPgNm == null || searchPgNm.isEmpty() || (p.getPgNm() != null && p.getPgNm().contains(searchPgNm))))
                .filter(p -> (searchUseYn == null || searchUseYn.isEmpty() || (p.getUseYn() != null && p.getUseYn().equals(searchUseYn))))
                .toList();
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = filtered.subList(start, Math.max(start, end)).stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("pgCd", p.getPgCd());
                    m.put("pgNm", p.getPgNm());
                    m.put("apiEndpoint", p.getApiEndpoint());
                    m.put("useYn", p.getUseYn());
                    m.put("regDt", p.getCreatedAt() != null ? p.getCreatedAt().toString().substring(0, 10) : null);
                    return m;
                })
                .toList();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(filtered.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) filtered.size() / size)));
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /** 결제대행사 목록 (드롭다운용 - 전체) */
    @GetMapping("/pgAgencyList")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> pgAgencyList() {
        List<Map<String, Object>> list = pgAgencyRepository.findByUseYnOrderByPgCdAsc("Y").stream()
                .map(p -> Map.<String, Object>of("pgCd", p.getPgCd(), "pgNm", p.getPgNm()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/pgApiMng/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgApiMngSave(@RequestBody Map<String, Object> body) {
        try {
            String pgNm = hqStr(body, "pgNm");
            String pgCdRaw = hqStr(body, "pgCd");
            if (pgNm == null || pgNm.isBlank() || pgCdRaw == null || pgCdRaw.isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("PG사코드와 PG사명은 필수입니다.", "VALIDATION"));
            }
            String pgCd = pgCdRaw.trim().toUpperCase();
            String endpoint = hqStr(body, "apiEndpoint");
            String useYn = "N".equalsIgnoreCase(hqStr(body, "useYn")) ? "N" : "Y";

            PgAgency entity;
            Object idObj = body.get("id");
            if (idObj != null && !idObj.toString().isBlank()) {
                long id = Long.parseLong(idObj.toString().trim());
                entity = pgAgencyRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("PG사 정보를 찾을 수 없습니다."));
                entity.setPgNm(pgNm.trim());
                if (endpoint != null) entity.setApiEndpoint(endpoint.trim());
                entity.setUseYn(useYn);
            } else {
                if (pgAgencyRepository.findByPgCd(pgCd).isPresent()) {
                    return ResponseEntity.ok(ApiResponse.fail("이미 등록된 PG사코드입니다.", "DUPLICATE"));
                }
                entity = new PgAgency();
                entity.setPgCd(pgCd);
                entity.setPgNm(pgNm.trim());
                entity.setApiEndpoint(endpoint != null ? endpoint.trim() : null);
                entity.setUseYn(useYn);
            }
            pgAgencyRepository.save(entity);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "저장되었습니다.");
            data.put("id", entity.getId());
            data.put("pgCd", entity.getPgCd());
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("ID 형식이 올바르지 않습니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static String hqStr(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : v.toString();
    }

    /** 2. 기본정책 (건당/이용/실패/취소/환불/결제/정산/USDT/FX/롤링%) */
    @GetMapping("/defaultCommission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultCommission() {
        Map<String, Object> data = new HashMap<>();
        commissionPolicyRepository.findByScope("DEFAULT").ifPresent(p -> {
            data.put("perTxFee", p.getPerTxFee() != null ? p.getPerTxFee().toString() : "0");
            data.put("usageRate", p.getUsageRate() != null ? p.getUsageRate().toString() : "0");
            data.put("failFee", p.getFailFee() != null ? p.getFailFee().toString() : "0");
            data.put("cancelRate", p.getCancelRate() != null ? p.getCancelRate().toString() : "0");
            data.put("refundRate", p.getRefundRate() != null ? p.getRefundRate().toString() : "0");
            data.put("payRate", p.getPayRate() != null ? p.getPayRate().toString() : "2.5");
            data.put("feeSettlementPerTx", p.getFeeSettlementPerTx() != null ? p.getFeeSettlementPerTx().toString() : "0");
            data.put("feeUsdt", p.getFeeUsdt() != null ? p.getFeeUsdt().toString() : "0");
            data.put("feeFx", p.getFeeFx() != null ? p.getFeeFx().toString() : "0");
            data.put("rollingPct", p.getRollingPct() != null ? p.getRollingPct().toString() : "5");
            data.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 180);
        });
        if (!data.containsKey("payRate")) {
            data.put("perTxFee", "0"); data.put("usageRate", "0"); data.put("failFee", "0");
            data.put("cancelRate", "0"); data.put("refundRate", "0"); data.put("payRate", "2.5");
            data.put("feeSettlementPerTx", "0"); data.put("feeUsdt", "0"); data.put("feeFx", "0");
            data.put("rollingPct", "5"); data.put("rollingDays", 180);
        }
        List<Map<String, Object>> templates = commissionPolicyRepository
                .findByScopeStartingWithOrderByScopeAsc(TEMPLATE_SCOPE_PREFIX)
                .stream()
                .map(this::policyToMap)
                .toList();
        data.put("templates", templates);
        String deployedScope = commissionPolicyRepository
                .findFirstByScopeStartingWithAndDeployYnOrderByUpdatedAtDesc(TEMPLATE_SCOPE_PREFIX, "Y")
                .map(CommissionPolicy::getScope)
                .orElse("");
        data.put("deployedTemplateScope", deployedScope);
        data.put("memo", "건당/취소/이용/실패/결제/환불 수수료 차감 후, 롤링(담보금)%를 N일간 보류하고 정산 주기에 지급.");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/defaultCommission/save")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultCommissionSave(@RequestBody Map<String, Object> body) {
        String templateScope = hqStr(body, "templateScope");
        String scope = (templateScope != null && !templateScope.trim().isEmpty()) ? templateScope.trim() : "DEFAULT";
        CommissionPolicy p = commissionPolicyRepository.findByScope(scope).orElseGet(() -> {
            CommissionPolicy def = new CommissionPolicy();
            def.setScope(scope);
            return def;
        });
        if (scope.startsWith(TEMPLATE_SCOPE_PREFIX)) {
            String policyName = hqStr(body, "policyName");
            p.setPolicyName(policyName != null && !policyName.trim().isEmpty() ? policyName.trim() : scope.substring(TEMPLATE_SCOPE_PREFIX.length()));
            p.setDeployYn("Y".equalsIgnoreCase(hqStr(body, "deployYn")) ? "Y" : "N");
        }
        p.setPerTxFee(toBigDecimal(body.get("perTxFee")));
        p.setUsageRate(toBigDecimal(body.get("usageRate")));
        p.setFailFee(toBigDecimal(body.get("failFee")));
        p.setCancelRate(toBigDecimal(body.get("cancelRate")));
        p.setRefundRate(toBigDecimal(body.get("refundRate")));
        p.setPayRate(toBigDecimal(body.get("payRate")));
        p.setFeeSettlementPerTx(toBigDecimal(body.get("feeSettlementPerTx")));
        p.setFeeUsdt(toBigDecimal(body.get("feeUsdt")));
        p.setFeeFx(toBigDecimal(body.get("feeFx")));
        p.setRollingPct(toBigDecimal(body.get("rollingPct")));
        Object rd = body.get("rollingDays");
        p.setRollingDays(rd != null && !rd.toString().isEmpty() ? Integer.parseInt(rd.toString()) : 180);
        commissionPolicyRepository.save(p);
        if (scope.startsWith(TEMPLATE_SCOPE_PREFIX) && "Y".equalsIgnoreCase(p.getDeployYn())) {
            // 다른 템플릿 deploy 해제
            commissionPolicyRepository.findByScopeStartingWithOrderByScopeAsc(TEMPLATE_SCOPE_PREFIX).forEach(tp -> {
                if (!Objects.equals(tp.getId(), p.getId())) {
                    tp.setDeployYn("N");
                    commissionPolicyRepository.save(tp);
                }
            });
            // 배포: DEFAULT에 복사
            CommissionPolicy def = commissionPolicyRepository.findByScope("DEFAULT").orElseGet(() -> {
                CommissionPolicy x = new CommissionPolicy();
                x.setScope("DEFAULT");
                return x;
            });
            copyPolicyValues(p, def);
            commissionPolicyRepository.save(def);
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    @GetMapping("/defaultCommission/templateOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> defaultCommissionTemplateOptions() {
        List<Map<String, Object>> list = commissionPolicyRepository.findByScopeStartingWithOrderByScopeAsc(TEMPLATE_SCOPE_PREFIX)
                .stream()
                .map(this::policyToMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/defaultCommission/template/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addDefaultCommissionTemplate(@RequestBody Map<String, Object> body) {
        String codeRaw = hqStr(body, "templateCode");
        String code = (codeRaw == null || codeRaw.isBlank()) ? null : codeRaw.trim().toUpperCase(Locale.ROOT);
        if (code == null) {
            for (char c = 'A'; c <= 'Z'; c++) {
                String s = String.valueOf(c);
                if (commissionPolicyRepository.findByScope(TEMPLATE_SCOPE_PREFIX + s).isEmpty()) {
                    code = s;
                    break;
                }
            }
        }
        if (code == null || !code.matches("[A-Z0-9_\\-]{1,20}")) {
            return ResponseEntity.ok(ApiResponse.fail("정책코드는 영문 대문자/숫자(1~20자)로 입력하세요.", "VALIDATION"));
        }
        String scope = TEMPLATE_SCOPE_PREFIX + code;
        if (commissionPolicyRepository.findByScope(scope).isPresent()) {
            return ResponseEntity.ok(ApiResponse.fail("이미 존재하는 정책코드입니다.", "DUPLICATE"));
        }
        CommissionPolicy src = commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new);
        CommissionPolicy n = new CommissionPolicy();
        n.setScope(scope);
        n.setPolicyName(code);
        n.setDeployYn("N");
        copyPolicyValues(src, n);
        commissionPolicyRepository.save(n);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("scope", scope, "policyName", code, "message", "정책 템플릿이 추가되었습니다.")));
    }

    @PostMapping("/defaultCommission/template/delete")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteDefaultCommissionTemplate(@RequestBody Map<String, Object> body) {
        String scope = hqStr(body, "scope");
        if (scope == null || scope.isBlank() || !scope.startsWith(TEMPLATE_SCOPE_PREFIX)) {
            return ResponseEntity.ok(ApiResponse.fail("삭제할 정책 scope가 올바르지 않습니다.", "VALIDATION"));
        }
        Optional<CommissionPolicy> opt = commissionPolicyRepository.findByScope(scope.trim());
        if (opt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("정책을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        commissionPolicyRepository.delete(opt.get());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "정책 템플릿이 삭제되었습니다.")));
    }

    private Map<String, Object> policyToMap(CommissionPolicy p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", p.getScope());
        m.put("policyName", p.getPolicyName() != null ? p.getPolicyName() : "");
        m.put("deployYn", p.getDeployYn() != null ? p.getDeployYn() : "N");
        m.put("perTxFee", p.getPerTxFee() != null ? p.getPerTxFee().toString() : "0");
        m.put("usageRate", p.getUsageRate() != null ? p.getUsageRate().toString() : "0");
        m.put("failFee", p.getFailFee() != null ? p.getFailFee().toString() : "0");
        m.put("cancelRate", p.getCancelRate() != null ? p.getCancelRate().toString() : "0");
        m.put("refundRate", p.getRefundRate() != null ? p.getRefundRate().toString() : "0");
        m.put("payRate", p.getPayRate() != null ? p.getPayRate().toString() : "0");
        m.put("feeSettlementPerTx", p.getFeeSettlementPerTx() != null ? p.getFeeSettlementPerTx().toString() : "0");
        m.put("feeUsdt", p.getFeeUsdt() != null ? p.getFeeUsdt().toString() : "0");
        m.put("feeFx", p.getFeeFx() != null ? p.getFeeFx().toString() : "0");
        m.put("rollingPct", p.getRollingPct() != null ? p.getRollingPct().toString() : "0");
        m.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 180);
        return m;
    }

    private static void copyPolicyValues(CommissionPolicy src, CommissionPolicy dst) {
        dst.setPerTxFee(src.getPerTxFee());
        dst.setUsageRate(src.getUsageRate());
        dst.setFailFee(src.getFailFee());
        dst.setCancelRate(src.getCancelRate());
        dst.setRefundRate(src.getRefundRate());
        dst.setPayRate(src.getPayRate());
        dst.setFeeSettlementPerTx(src.getFeeSettlementPerTx());
        dst.setFeeUsdt(src.getFeeUsdt());
        dst.setFeeFx(src.getFeeFx());
        dst.setRollingPct(src.getRollingPct());
        dst.setRollingDays(src.getRollingDays());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null || o.toString().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 3. API 구성 세팅 - 여러 PG사 연동 후 우리 가맹점 발부 API 세팅 + ChillPay(칠리페이) 연동 */
    @GetMapping("/apiConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apiConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("baseUrl", "");
        data.put("authType", "API_KEY");
        data.put("timeoutSec", 30);
        data.put("memo", "가맹점에게 발급하는 결제/취소/조회 API 기본 구성.");
        data.put("chillpayMerchantCode", "M035594");
        data.put("chillpayApiKey", "");
        data.put("chillpayMd5Key", "");
        data.put("chillpayRouteNo", 4);
        data.put("chillpaySandbox", "Y");
        data.put("recallIncludeFeeYn", "N");
        data.put("settlementVatApplyYn", "Y");
        hqApiConfigRepository.findAll().stream().findFirst().ifPresent(c -> {
            if (c.getBaseUrl() != null) data.put("baseUrl", c.getBaseUrl());
            if (c.getAuthType() != null) data.put("authType", c.getAuthType());
            if (c.getTimeoutSec() != null) data.put("timeoutSec", c.getTimeoutSec());
            if (c.getMemo() != null) data.put("memo", c.getMemo());
            if (c.getChillpayMerchantCode() != null) data.put("chillpayMerchantCode", c.getChillpayMerchantCode());
            if (c.getChillpayApiKey() != null) data.put("chillpayApiKey", c.getChillpayApiKey());
            if (c.getChillpayMd5Key() != null) data.put("chillpayMd5Key", c.getChillpayMd5Key());
            if (c.getChillpayRouteNo() != null) data.put("chillpayRouteNo", c.getChillpayRouteNo());
            if (c.getChillpaySandbox() != null) data.put("chillpaySandbox", c.getChillpaySandbox());
            if (c.getRecallIncludeFeeYn() != null) data.put("recallIncludeFeeYn", c.getRecallIncludeFeeYn());
            if (c.getSettlementVatApplyYn() != null) data.put("settlementVatApplyYn", c.getSettlementVatApplyYn());
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
        c.setChillpayMerchantCode(body.get("chillpayMerchantCode") != null ? body.get("chillpayMerchantCode").toString().trim() : null);
        c.setChillpayApiKey(body.get("chillpayApiKey") != null ? body.get("chillpayApiKey").toString().trim() : null);
        c.setChillpayMd5Key(body.get("chillpayMd5Key") != null ? body.get("chillpayMd5Key").toString().trim() : null);
        Object rn = body.get("chillpayRouteNo");
        c.setChillpayRouteNo(rn != null && !rn.toString().isEmpty() ? Integer.parseInt(rn.toString()) : 4);
        c.setChillpaySandbox(body.get("chillpaySandbox") != null ? body.get("chillpaySandbox").toString().trim() : "Y");
        c.setRecallIncludeFeeYn("Y".equalsIgnoreCase(String.valueOf(body.getOrDefault("recallIncludeFeeYn", "N"))) ? "Y" : "N");
        c.setSettlementVatApplyYn("N".equalsIgnoreCase(String.valueOf(body.getOrDefault("settlementVatApplyYn", "Y"))) ? "N" : "Y");
        hqApiConfigRepository.save(c);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    /** 본사설정 > 영업일설정 목록 조회 */
    @GetMapping("/businessDaySettings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> businessDaySettings() {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        String raw = c != null ? c.getBusinessDaySettingsJson() : null;
        List<Map<String, Object>> list = parseBusinessDaySettings(raw);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** 본사설정 > 영업일설정 저장(추가/수정/삭제) */
    @PostMapping("/businessDaySettings/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> businessDaySettingsSave(@RequestBody Map<String, Object> body) {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(new HqApiConfig());
        List<Map<String, Object>> list = parseBusinessDaySettings(c.getBusinessDaySettingsJson());
        String mode = Optional.ofNullable(hqStr(body, "mode")).orElse("UPSERT").trim().toUpperCase(Locale.ROOT);
        String id = Optional.ofNullable(hqStr(body, "id")).orElse("").trim();
        String name = Optional.ofNullable(hqStr(body, "name")).orElse("").trim();
        String countryCode = Optional.ofNullable(hqStr(body, "countryCode")).orElse("KR").trim().toUpperCase(Locale.ROOT);
        String extraDates = Optional.ofNullable(hqStr(body, "businessHolidayExtraDates")).orElse("").trim();
        if (!Set.of("KR", "US", "JP", "TH").contains(countryCode)) {
            return ResponseEntity.ok(ApiResponse.fail("기준국가는 KR/US/JP/TH만 가능합니다.", "VALIDATION"));
        }
        if ("DELETE".equals(mode)) {
            if (id.isEmpty()) return ResponseEntity.ok(ApiResponse.fail("삭제할 ID가 필요합니다.", "VALIDATION"));
            final String deleteId = id;
            list = list.stream().filter(m -> !Objects.equals(String.valueOf(m.getOrDefault("id", "")), deleteId)).collect(Collectors.toList());
            c.setBusinessDaySettingsJson(writeBusinessDaySettings(list));
            hqApiConfigRepository.save(c);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "삭제되었습니다.", "list", list)));
        }
        if (name.isEmpty()) return ResponseEntity.ok(ApiResponse.fail("이름은 필수입니다.", "VALIDATION"));
        if (id.isEmpty()) id = UUID.randomUUID().toString();
        String finalId = id;
        for (Map<String, Object> m : list) {
            String mid = String.valueOf(m.getOrDefault("id", ""));
            String mn = String.valueOf(m.getOrDefault("name", ""));
            if (!mid.equals(finalId) && mn.equalsIgnoreCase(name)) {
                return ResponseEntity.ok(ApiResponse.fail("동일한 이름이 이미 있습니다.", "DUPLICATE"));
            }
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("countryCode", countryCode);
        row.put("businessHolidayExtraDates", extraDates);
        row.put("updatedAt", java.time.LocalDate.now().toString());
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(String.valueOf(list.get(i).getOrDefault("id", "")), id)) {
                list.set(i, row);
                updated = true;
                break;
            }
        }
        if (!updated) list.add(row);
        c.setBusinessDaySettingsJson(writeBusinessDaySettings(list));
        hqApiConfigRepository.save(c);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.", "id", id, "list", list)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseBusinessDaySettings(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            Object parsed = om.readValue(raw, Object.class);
            if (!(parsed instanceof List<?> l)) return new ArrayList<>();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object it : l) {
                if (!(it instanceof Map<?, ?> mm)) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                Object idVal = mm.get("id");
                Object nameVal = mm.get("name");
                Object ccVal = mm.get("countryCode");
                Object extraVal = mm.get("businessHolidayExtraDates");
                Object updVal = mm.get("updatedAt");
                row.put("id", idVal == null ? "" : String.valueOf(idVal));
                row.put("name", nameVal == null ? "" : String.valueOf(nameVal));
                row.put("countryCode", ccVal == null ? "KR" : String.valueOf(ccVal));
                row.put("businessHolidayExtraDates", extraVal == null ? "" : String.valueOf(extraVal));
                row.put("updatedAt", updVal == null ? "" : String.valueOf(updVal));
                out.add(row);
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeBusinessDaySettings(List<Map<String, Object>> list) {
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
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
