package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqApiConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.HolidayPresetService;
import com.pg.service.HqServerManageService;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.OrgUnitChangeAuditService;
import com.pg.service.ServerUsageService;
import com.pg.util.CommissionTierJsonHelper;
import com.pg.util.PercentDecimalHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 본사설정 API
 * 1. PG사 API 연동  2. 기본 수수료 정책  3. API 구성 세팅  4. 조직별 권한 세팅
 */
@RestController
@RequestMapping(value = "/api/hq", produces = "application/json")
public class ApiHqController {
    private static final String TEMPLATE_SCOPE_PREFIX = "HQPOL:";
    private static final ObjectMapper HQ_OBJECT_MAPPER = new ObjectMapper();

    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final HolidayPresetService holidayPresetService;
    private final OrgPagePermissionService orgPagePermissionService;
    private final HqServerManageService hqServerManageService;
    private final ServerUsageService serverUsageService;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final OrgUnitChangeAuditService orgUnitChangeAuditService;

    public ApiHqController(CommissionPolicyRepository commissionPolicyRepository,
                           ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                           HqApiConfigRepository hqApiConfigRepository,
                           PgAgencyRepository pgAgencyRepository,
                           MerchantPgBindingRepository merchantPgBindingRepository,
                           HolidayPresetService holidayPresetService,
                           OrgPagePermissionService orgPagePermissionService,
                           HqServerManageService hqServerManageService,
                           ServerUsageService serverUsageService,
                           OrgUnitRepository orgUnitRepository,
                           AuthService authService,
                           OrgUnitChangeAuditService orgUnitChangeAuditService) {
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.holidayPresetService = holidayPresetService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.hqServerManageService = hqServerManageService;
        this.serverUsageService = serverUsageService;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.orgUnitChangeAuditService = orgUnitChangeAuditService;
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

    /** 사용(Y)인 결제대행사가 정확히 1건이면 운영(operational)을 자동 Y로 맞춤 */
    private void ensureSingleUseAgencyOperational() {
        List<PgAgency> all = pgAgencyRepository.findAllByOrderByPgCdAsc();
        List<PgAgency> active = all.stream()
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .toList();
        if (active.size() != 1) {
            return;
        }
        PgAgency only = active.get(0);
        String op = only.getOperationalYn();
        if (op == null || !"Y".equalsIgnoreCase(op.trim())) {
            only.setOperationalYn("Y");
            pgAgencyRepository.save(only);
        }
    }

    private static boolean isPgOperationalYes(PgAgency p) {
        return p.getOperationalYn() != null && "Y".equalsIgnoreCase(p.getOperationalYn().trim());
    }

    private static boolean ynPg(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }

    private static String integrationScopeLabel(PgAgency p) {
        List<String> parts = new ArrayList<>();
        if (ynPg(p.getIntegNotiYn())) {
            parts.add("노티");
        }
        if (ynPg(p.getIntegUrlPayYn())) {
            parts.add("URL");
        }
        if (ynPg(p.getIntegWebChatbotYn())) {
            parts.add("챗봇");
        }
        if (ynPg(p.getIntegApiYn())) {
            parts.add("API");
        }
        return parts.isEmpty() ? "—" : String.join("/", parts);
    }

    /** DB 플래그 기준 단일/복합 용도 코드: NOTI, URL_PAY, WEB_CHATBOT, API, MULTI(레거시), 빈 문자열 */
    private static String resolveIntegKind(PgAgency p) {
        List<String> ys = new ArrayList<>();
        if (ynPg(p.getIntegNotiYn())) {
            ys.add("NOTI");
        }
        if (ynPg(p.getIntegUrlPayYn())) {
            ys.add("URL_PAY");
        }
        if (ynPg(p.getIntegWebChatbotYn())) {
            ys.add("WEB_CHATBOT");
        }
        if (ynPg(p.getIntegApiYn())) {
            ys.add("API");
        }
        if (ys.size() == 1) {
            return ys.get(0);
        }
        if (ys.size() > 1) {
            return "MULTI";
        }
        return "";
    }

    private static String integKindLabel(String kind) {
        if (kind == null || kind.isEmpty()) {
            return "";
        }
        return switch (kind) {
            case "NOTI" -> "노티";
            case "URL_PAY" -> "URL결제";
            case "WEB_CHATBOT" -> "웹챗봇";
            case "API" -> "API";
            case "MULTI" -> "복합(레거시)";
            default -> kind;
        };
    }

    private static String primaryEndpointForRow(PgAgency p, String kind) {
        if ("NOTI".equals(kind)) {
            return nz(p.getEndpointNoti());
        }
        if ("URL_PAY".equals(kind)) {
            return nz(p.getEndpointUrlPay());
        }
        if ("WEB_CHATBOT".equals(kind) || "API".equals(kind)) {
            String a = nz(p.getEndpointApi());
            return !a.isEmpty() ? a : nz(p.getApiEndpoint());
        }
        if ("MULTI".equals(kind)) {
            return endpointsSummary(p);
        }
        return firstNonBlank(nz(p.getEndpointNoti()), nz(p.getEndpointUrlPay()), nz(p.getEndpointApi()), nz(p.getApiEndpoint()));
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }

    private static String firstNonBlank(String... xs) {
        if (xs == null) {
            return "";
        }
        for (String x : xs) {
            if (x != null && !x.isBlank()) {
                return x.trim();
            }
        }
        return "";
    }

    private static String endpointsSummary(PgAgency p) {
        StringBuilder sb = new StringBuilder();
        appendEp(sb, "노티", p.getEndpointNoti());
        appendEp(sb, "URL", p.getEndpointUrlPay());
        appendEp(sb, "API", p.getEndpointApi());
        if (p.getApiEndpoint() != null && !p.getApiEndpoint().isBlank()) {
            appendEp(sb, "구버전", p.getApiEndpoint());
        }
        String s = sb.toString().trim();
        if (s.length() > 140) {
            return s.substring(0, 137) + "...";
        }
        return s.isEmpty() ? "—" : s;
    }

    private static void appendEp(StringBuilder sb, String tag, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        String u = url.trim();
        if (u.length() > 48) {
            u = u.substring(0, 45) + "...";
        }
        sb.append(tag).append(':').append(u);
    }

    /** 1. PG사 API 연동 - 결제대행사 목록 (가맹점 배포용 결제 모듈) */
    @GetMapping("/pgApiMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> pgApiMng(
            @RequestParam(required = false) String searchPgNm,
            @RequestParam(required = false) String searchUseYn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ensureSingleUseAgencyOperational();
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
                    m.put("endpointNoti", p.getEndpointNoti() != null ? p.getEndpointNoti() : "");
                    m.put("endpointUrlPay", p.getEndpointUrlPay() != null ? p.getEndpointUrlPay() : "");
                    m.put("endpointApi", p.getEndpointApi() != null ? p.getEndpointApi() : "");
                    m.put("integNotiYn", ynPg(p.getIntegNotiYn()) ? "Y" : "N");
                    m.put("integUrlPayYn", ynPg(p.getIntegUrlPayYn()) ? "Y" : "N");
                    m.put("integWebChatbotYn", ynPg(p.getIntegWebChatbotYn()) ? "Y" : "N");
                    m.put("integApiYn", ynPg(p.getIntegApiYn()) ? "Y" : "N");
                    String integKind = resolveIntegKind(p);
                    m.put("integKind", integKind);
                    m.put("integKindLabel", integKindLabel(integKind));
                    m.put("primaryEndpoint", primaryEndpointForRow(p, integKind));
                    m.put("integrationScopeLabel", integrationScopeLabel(p));
                    m.put("endpointsSummary", endpointsSummary(p));
                    m.put("useYn", p.getUseYn());
                    m.put("operationalYn", isPgOperationalYes(p) ? "Y" : "N");
                    m.put("merchantMid", p.getMerchantMid() != null ? p.getMerchantMid() : "");
                    boolean cred = p.getApiKey() != null && !p.getApiKey().isBlank()
                            && p.getMd5SecretKey() != null && !p.getMd5SecretKey().isBlank();
                    m.put("hasCredentials", cred ? "Y" : "N");
                    m.put("routeNo", p.getRouteNo() != null ? p.getRouteNo() : "");
                    m.put("sandboxYn", p.getSandboxYn() != null ? p.getSandboxYn() : "Y");
                    m.put("credentialsExtraJson", p.getCredentialsExtraJson() != null ? p.getCredentialsExtraJson() : "");
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

    /**
     * 결제대행사 목록 (가맹점 등록·결제대행사 설정 드롭다운용).
     * API연동설정에서 <strong>사용(Y)</strong>인 행 전부. 본사 「운영」체크 여부는 참고용(hqOperationalYn)으로만 내려가며,
     * 가맹점이 선택·운영(가맹점 행의 operational)하는 데 필수는 아닙니다.
     */
    @GetMapping("/pgAgencyList")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> pgAgencyList() {
        ensureSingleUseAgencyOperational();
        List<Map<String, Object>> list = pgAgencyRepository.findByUseYnOrderByPgCdAsc("Y").stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("pgCd", p.getPgCd());
                    m.put("pgNm", p.getPgNm());
                    m.put("defaultMid", p.getMerchantMid() != null ? p.getMerchantMid() : "");
                    m.put("routeNo", p.getRouteNo() != null ? p.getRouteNo() : "");
                    m.put("sandboxYn", p.getSandboxYn() != null ? p.getSandboxYn() : "Y");
                    m.put("apiEndpoint", p.getApiEndpoint() != null ? p.getApiEndpoint() : "");
                    m.put("endpointNoti", p.getEndpointNoti() != null ? p.getEndpointNoti() : "");
                    m.put("endpointUrlPay", p.getEndpointUrlPay() != null ? p.getEndpointUrlPay() : "");
                    m.put("endpointApi", p.getEndpointApi() != null ? p.getEndpointApi() : "");
                    m.put("integNotiYn", ynPg(p.getIntegNotiYn()) ? "Y" : "N");
                    m.put("integUrlPayYn", ynPg(p.getIntegUrlPayYn()) ? "Y" : "N");
                    m.put("integWebChatbotYn", ynPg(p.getIntegWebChatbotYn()) ? "Y" : "N");
                    m.put("integApiYn", ynPg(p.getIntegApiYn()) ? "Y" : "N");
                    String integKind = resolveIntegKind(p);
                    m.put("integKind", integKind);
                    m.put("integKindLabel", integKindLabel(integKind));
                    m.put("integrationScopeLabel", integrationScopeLabel(p));
                    m.put("hqOperationalYn", isPgOperationalYes(p) ? "Y" : "N");
                    boolean cred = p.getApiKey() != null && !p.getApiKey().isBlank()
                            && p.getMd5SecretKey() != null && !p.getMd5SecretKey().isBlank();
                    m.put("hasCredentials", cred ? "Y" : "N");
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** 결제대행사 운영 지정 저장: 전체를 N 후, 요청한 코드(사용 Y인 것만)를 Y */
    @PostMapping("/pgApiMng/operational")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgApiMngOperationalSave(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> raw = body != null ? (List<Object>) body.get("operationalPgCds") : null;
            List<String> want = new ArrayList<>();
            if (raw != null) {
                for (Object o : raw) {
                    if (o == null) continue;
                    String s = o.toString().trim().toUpperCase();
                    if (!s.isEmpty()) want.add(s);
                }
            }
            List<PgAgency> all = pgAgencyRepository.findAllByOrderByPgCdAsc();
            for (PgAgency a : all) {
                a.setOperationalYn("N");
            }
            Set<String> wantSet = new HashSet<>(want);
            for (PgAgency a : all) {
                if (!wantSet.contains(a.getPgCd())) {
                    continue;
                }
                if (a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim())) {
                    a.setOperationalYn("Y");
                }
            }
            pgAgencyRepository.saveAll(all);
            ensureSingleUseAgencyOperational();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "운영 설정이 저장되었습니다.");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (ClassCastException e) {
            return ResponseEntity.ok(ApiResponse.fail("operationalPgCds 형식이 올바르지 않습니다.", "VALIDATION"));
        }
    }

    @PostMapping("/pgApiMng/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgApiMngSave(@RequestBody Map<String, Object> body) {
        try {
            String pgNm = hqStr(body, "pgNm");
            String pgCdRaw = hqStr(body, "pgCd");
            if (pgNm == null || pgNm.isBlank() || pgCdRaw == null || pgCdRaw.isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("PG사코드와 PG사명은 필수입니다.", "VALIDATION"));
            }
            String pgCd = pgCdRaw.trim().toUpperCase(Locale.ROOT);
            String useYn = "N".equalsIgnoreCase(hqStr(body, "useYn")) ? "N" : "Y";

            PgAgency entity;
            Object idObj = body.get("id");
            boolean isNew = idObj == null || idObj.toString().isBlank();
            if (isNew) {
                String ik = hqStr(body, "integKind");
                if (ik == null || ik.isBlank()) {
                    return ResponseEntity.ok(ApiResponse.fail("신규 PG 연동은 연동용도를 선택해야 합니다. (PG코드는 용도별로 나누어 등록하세요)", "VALIDATION"));
                }
            }
            if (idObj != null && !idObj.toString().isBlank()) {
                long id = Long.parseLong(idObj.toString().trim());
                entity = pgAgencyRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("PG사 정보를 찾을 수 없습니다."));
                entity.setPgNm(pgNm.trim());
                if (body.containsKey("apiEndpoint")) {
                    String ep = hqStr(body, "apiEndpoint");
                    entity.setApiEndpoint(ep != null && !ep.isBlank() ? ep.trim() : null);
                }
                entity.setUseYn(useYn);
                applyPgAgencyCredentialFields(entity, body, true);
                applyPgAgencyIntegrationScope(entity, body, true);
            } else {
                if (pgAgencyRepository.findByPgCd(pgCd).isPresent()) {
                    return ResponseEntity.ok(ApiResponse.fail("이미 등록된 PG사코드입니다.", "DUPLICATE"));
                }
                entity = new PgAgency();
                entity.setPgCd(pgCd);
                entity.setPgNm(pgNm.trim());
                String endpointNew = hqStr(body, "apiEndpoint");
                entity.setApiEndpoint(endpointNew != null && !endpointNew.isBlank() ? endpointNew.trim() : null);
                entity.setUseYn(useYn);
                applyPgAgencyCredentialFields(entity, body, false);
                applyPgAgencyIntegrationScope(entity, body, false);
            }
            if ("Y".equalsIgnoreCase(entity.getUseYn())) {
                if (!ynPg(entity.getIntegNotiYn()) && !ynPg(entity.getIntegUrlPayYn())
                        && !ynPg(entity.getIntegWebChatbotYn()) && !ynPg(entity.getIntegApiYn())) {
                    return ResponseEntity.ok(ApiResponse.fail("사용(Y)인 경우 연동 용도를 한 가지 이상 지정하세요.", "VALIDATION"));
                }
            }
            pgAgencyRepository.save(entity);
            ensureSingleUseAgencyOperational();
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

    /** PG사 연동 삭제 — 가맹점 결제대행사(tb_merchant_pg_binding)에서 해당 pg_cd를 쓰는 행이 있으면 거부 */
    @PostMapping("/pgApiMng/delete")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> pgApiMngDelete(@RequestBody Map<String, Object> body) {
        try {
            Object idObj = body != null ? body.get("id") : null;
            if (idObj == null || idObj.toString().isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("삭제할 PG사 ID가 필요합니다.", "VALIDATION"));
            }
            long id = Long.parseLong(idObj.toString().trim());
            PgAgency entity = pgAgencyRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("PG사 정보를 찾을 수 없습니다."));
            String pgCd = entity.getPgCd();
            if (pgCd != null && merchantPgBindingRepository.existsByPgCd(pgCd.trim())) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "가맹점 등록의 결제대행사 설정에서 이 PG(" + pgCd + ")를 사용 중입니다. 먼저 해당 연동을 제거한 뒤 삭제하세요.",
                        "IN_USE"));
            }
            pgAgencyRepository.delete(entity);
            ensureSingleUseAgencyOperational();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "삭제되었습니다.");
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

    /**
     * PG사 자격 필드 반영. 수정 시 apiKey·md5Key는 비어 있으면 기존 값 유지(화면에 비밀 미표시).
     */
    private static void applyPgAgencyCredentialFields(PgAgency entity, Map<String, Object> body, boolean isUpdate) {
        if (body.containsKey("mid")) {
            String mid = hqStr(body, "mid");
            entity.setMerchantMid(mid != null && !mid.isBlank() ? mid.trim() : null);
        } else if (!isUpdate) {
            entity.setMerchantMid(null);
        }
        String ak = hqStr(body, "apiKey");
        if (ak != null && !ak.isBlank()) {
            entity.setApiKey(ak.trim());
        } else if (!isUpdate) {
            entity.setApiKey(null);
        }
        String mk = hqStr(body, "md5Key");
        if (mk != null && !mk.isBlank()) {
            entity.setMd5SecretKey(mk.trim());
        } else if (!isUpdate) {
            entity.setMd5SecretKey(null);
        }
        if (body.containsKey("routeNo")) {
            Object r = body.get("routeNo");
            if (r == null || r.toString().isBlank()) {
                entity.setRouteNo(null);
            } else {
                try {
                    entity.setRouteNo(Integer.parseInt(r.toString().trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Route No는 정수여야 합니다.");
                }
            }
        } else if (!isUpdate) {
            entity.setRouteNo(null);
        }
        if (body.containsKey("sandboxYn")) {
            entity.setSandboxYn("N".equalsIgnoreCase(hqStr(body, "sandboxYn")) ? "N" : "Y");
        } else if (!isUpdate) {
            entity.setSandboxYn("Y");
        }
        if (body.containsKey("credentialsExtraJson")) {
            String j = hqStr(body, "credentialsExtraJson");
            entity.setCredentialsExtraJson(j != null && !j.isBlank() ? j.trim() : null);
        } else if (!isUpdate) {
            entity.setCredentialsExtraJson(null);
        }
    }

    /**
     * 연동 범위·엔드포인트. {@code integKind} 가 오면 용도 1개 + {@code integrationEndpoint} 1개만 반영(나머지 플래그·용도별 URL 초기화).
     * 없으면 레거시(개별 YN·엔드포인트 필드) 경로.
     */
    private static void applyPgAgencyIntegrationScope(PgAgency entity, Map<String, Object> body, boolean isUpdate) {
        String integKind = hqStr(body, "integKind");
        if (integKind != null && !integKind.isBlank()) {
            applySingleIntegrationKind(entity, integKind.trim().toUpperCase(Locale.ROOT), body);
            return;
        }
        if (body.containsKey("integNotiYn")) {
            entity.setIntegNotiYn("Y".equalsIgnoreCase(hqStr(body, "integNotiYn")) ? "Y" : "N");
        } else if (!isUpdate) {
            entity.setIntegNotiYn("N");
        }
        if (body.containsKey("integUrlPayYn")) {
            entity.setIntegUrlPayYn("Y".equalsIgnoreCase(hqStr(body, "integUrlPayYn")) ? "Y" : "N");
        } else if (!isUpdate) {
            entity.setIntegUrlPayYn("N");
        }
        if (body.containsKey("integWebChatbotYn")) {
            entity.setIntegWebChatbotYn("Y".equalsIgnoreCase(hqStr(body, "integWebChatbotYn")) ? "Y" : "N");
        } else if (!isUpdate) {
            entity.setIntegWebChatbotYn("N");
        }
        if (body.containsKey("integApiYn")) {
            entity.setIntegApiYn("Y".equalsIgnoreCase(hqStr(body, "integApiYn")) ? "Y" : "N");
        } else if (!isUpdate) {
            entity.setIntegApiYn("N");
        }
        if (body.containsKey("endpointNoti")) {
            String s = hqStr(body, "endpointNoti");
            entity.setEndpointNoti(s != null && !s.isBlank() ? s.trim() : null);
        } else if (!isUpdate) {
            entity.setEndpointNoti(null);
        }
        if (body.containsKey("endpointUrlPay")) {
            String s = hqStr(body, "endpointUrlPay");
            entity.setEndpointUrlPay(s != null && !s.isBlank() ? s.trim() : null);
        } else if (!isUpdate) {
            entity.setEndpointUrlPay(null);
        }
        if (body.containsKey("endpointApi")) {
            String s = hqStr(body, "endpointApi");
            entity.setEndpointApi(s != null && !s.isBlank() ? s.trim() : null);
        } else if (!isUpdate) {
            entity.setEndpointApi(null);
        }
    }

    /** 용도 1개만 Y, 해당 용도 엔드포인트만 설정(다른 용도 URL·플래그 제거) */
    private static void applySingleIntegrationKind(PgAgency entity, String kind, Map<String, Object> body) {
        String epRaw = hqStr(body, "integrationEndpoint");
        String ep = epRaw != null && !epRaw.isBlank() ? epRaw.trim() : null;
        entity.setIntegNotiYn("N");
        entity.setIntegUrlPayYn("N");
        entity.setIntegWebChatbotYn("N");
        entity.setIntegApiYn("N");
        entity.setEndpointNoti(null);
        entity.setEndpointUrlPay(null);
        entity.setEndpointApi(null);
        switch (kind) {
            case "NOTI" -> {
                entity.setIntegNotiYn("Y");
                entity.setEndpointNoti(ep);
            }
            case "URL_PAY" -> {
                entity.setIntegUrlPayYn("Y");
                entity.setEndpointUrlPay(ep);
            }
            case "WEB_CHATBOT" -> {
                entity.setIntegWebChatbotYn("Y");
                entity.setEndpointApi(ep);
            }
            case "API" -> {
                entity.setIntegApiYn("Y");
                entity.setEndpointApi(ep);
            }
            default -> throw new IllegalArgumentException("연동용도는 NOTI, URL_PAY, WEB_CHATBOT, API 중 하나여야 합니다.");
        }
    }

    /** 2. 기본정책 (건당/이용/실패/취소/환불/결제/정산/USDT/FX/롤링%) */
    @GetMapping("/defaultCommission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultCommission() {
        Map<String, Object> data = new HashMap<>();
        commissionPolicyRepository.findByScope("DEFAULT").ifPresent(p -> {
            data.put("perTxFee", p.getPerTxFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getPerTxFee()) : "0.0");
            data.put("usageRate", p.getUsageRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsageRate()) : "0.0");
            data.put("failFee", p.getFailFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFailFee()) : "0.0");
            data.put("cancelRate", p.getCancelRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getCancelRate()) : "0.0");
            data.put("voidFeePerTx", p.getVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getVoidFeePerTx()) : "0.0");
            data.put("manualVoidFeePerTx", p.getManualVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getManualVoidFeePerTx()) : "0.0");
            data.put("refundRate", p.getRefundRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRefundRate()) : "0.0");
            data.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "2.5");
            data.put("feeSettlementPerTx", p.getFeeSettlementPerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFeeSettlementPerTx()) : "0.0");
            data.put("remittanceTransferFee", p.getRemittanceTransferFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRemittanceTransferFee()) : "0.0");
            data.put("usdtTransferFeeUsd", p.getUsdtTransferFeeUsd() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsdtTransferFeeUsd()) : "0.0");
            data.put("feeUsdt", p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "0");
            data.put("feeFx", p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "0");
            data.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "5");
            data.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 180);
            data.put("currencyCode", p.getCurrencyCode() != null && !p.getCurrencyCode().isBlank() ? p.getCurrencyCode() : "KRW");
            data.put("policyRemark", p.getPolicyRemark() != null ? p.getPolicyRemark() : "");
            data.put("fee3dsRate", p.getFee3dsRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFee3dsRate()) : "0");
            data.put("chargebackFeePerTx", p.getChargebackFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getChargebackFeePerTx()) : "0.0");
            data.put("chargebackPolicyId", p.getChargebackPolicyId() != null ? p.getChargebackPolicyId() : "");
            putExtraFeeScalarsOnMap(data, p);
            data.put("tierCommission", tierMapForPolicy(p));
            CommissionTierJsonHelper.applyTierJsonSumsToDisplayMap(data, p);
        });
        if (!data.containsKey("payRate")) {
            data.put("perTxFee", "0.0"); data.put("usageRate", "0.0"); data.put("failFee", "0.0");
            data.put("cancelRate", "0.0"); data.put("voidFeePerTx", "0.0"); data.put("manualVoidFeePerTx", "0.0");
            data.put("refundRate", "0.0"); data.put("payRate", "2.5");
            data.put("feeSettlementPerTx", "0.0"); data.put("remittanceTransferFee", "0.0"); data.put("usdtTransferFeeUsd", "0.0"); data.put("feeUsdt", "0"); data.put("feeFx", "0");
            data.put("rollingPct", "5"); data.put("rollingDays", 180);
            data.put("currencyCode", "KRW");
            data.put("policyRemark", "");
            data.put("fee3dsRate", "0");
            data.put("chargebackFeePerTx", "0.0");
            data.put("chargebackPolicyId", "");
            putExtraFeeScalarsOnMap(data, null);
            data.put("tierCommission", Map.of("rows", Map.of(), "extras", List.of()));
        }
        Map<Long, String> chargebackNames = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChargebackFeePolicy::getId, ChargebackFeePolicy::getName, (a, b) -> a));
        List<Map<String, Object>> cbOpts = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .map(p -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", p.getId());
                    o.put("name", p.getName() != null ? p.getName() : "");
                    o.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode() : "KRW");
                    return o;
                })
                .toList();
        data.put("chargebackPolicyOptions", cbOpts);
        Object cpidObj = data.get("chargebackPolicyId");
        if (cpidObj != null && !cpidObj.toString().isBlank()) {
            try {
                long cid = Long.parseLong(cpidObj.toString().trim());
                data.put("chargebackPolicyName", chargebackNames.getOrDefault(cid, ""));
            } catch (NumberFormatException e) {
                data.put("chargebackPolicyName", "");
            }
        } else {
            data.put("chargebackPolicyName", "");
        }
        List<Map<String, Object>> templates = listAllTemplatePolicies()
                .stream()
                .map(p -> enrichPolicyMapWithChargebackName(policyToMap(p), p.getChargebackPolicyId(), chargebackNames))
                .toList();
        data.put("templates", templates);
        List<String> deployedScopes = templates.stream()
                .filter(m -> "Y".equalsIgnoreCase(String.valueOf(m.getOrDefault("deployYn", "N"))))
                .map(m -> String.valueOf(m.getOrDefault("scope", "")))
                .filter(s -> !s.isBlank())
                .toList();
        data.put("deployedTemplateScopes", deployedScopes);
        String deployedScope = deployedScopes.isEmpty() ? "" : deployedScopes.get(0);
        data.put("deployedTemplateScope", deployedScope);
        data.put("memo", "조직별 격자: 총본사~영업점은 배분(결제율·건당)에 반영됩니다. 가맹 열은 위 6단계 합계(가맹점 적용분)로 표시·저장됩니다. 가맹점이 본사설정을 따르면 이 합계가 기준이 되고, 업체관리 수수료에서 수정 시 그 값이 우선합니다. 결제·USDT·FX·3DS는 승인금액 기준 %. 건당·고정액은 통화 단위(소수 첫째 자리). 차지백 구간정책·롤링은 격자 외 필드입니다.");
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
        boolean fromTier = false;
        try {
            String tj = tierCommissionJsonFromBody(body);
            if (tj != null) {
                String normalized = CommissionTierJsonHelper.normalizeTierJsonMerchantSums(tj);
                p.setTierCommissionJson(normalized);
                CommissionTierJsonHelper.applyTierJsonToPolicy(p, normalized);
                fromTier = true;
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("조직별 수수료(tierCommission) 형식이 올바르지 않습니다.", "VALIDATION"));
        }
        if (!fromTier) {
            p.setPerTxFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("perTxFee")));
            p.setUsageRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("usageRate")));
            p.setFailFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("failFee")));
            p.setCancelRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("cancelRate")));
            p.setVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("voidFeePerTx")));
            p.setManualVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("manualVoidFeePerTx")));
            p.setRefundRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("refundRate")));
            p.setPayRate(PercentDecimalHelper.parsePercentOneDecimal(body.get("payRate")));
            p.setFeeSettlementPerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("feeSettlementPerTx")));
            p.setRemittanceTransferFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("remittanceTransferFee")));
            p.setUsdtTransferFeeUsd(PercentDecimalHelper.parseAmountOneDecimal(body.get("usdtTransferFeeUsd")));
            p.setFeeUsdt(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeUsdt")));
            p.setFeeFx(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeFx")));
            p.setFee3dsRate(PercentDecimalHelper.parsePercentOneDecimal(body.get("fee3dsRate")));
            p.setChargebackFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("chargebackFeePerTx")));
            applyExtraFeesFromBody(p, body);
            p.setTierCommissionJson(CommissionTierJsonHelper.buildTierJsonFromPolicyScalars(p));
        }
        p.setRollingPct(PercentDecimalHelper.parsePercentOneDecimal(body.get("rollingPct")));
        Object rd = body.get("rollingDays");
        p.setRollingDays(rd != null && !rd.toString().isEmpty() ? Integer.parseInt(rd.toString()) : 180);
        String cc = hqStr(body, "currencyCode");
        p.setCurrencyCode(cc != null && !cc.isBlank() ? cc.trim().toUpperCase(Locale.ROOT) : "KRW");
        p.setPolicyRemark(hqStr(body, "policyRemark"));
        p.setChargebackPolicyId(parseOptionalPolicyLong(body.get("chargebackPolicyId")));
        commissionPolicyRepository.save(p);
        /* 배포(Y): 가맹점 등록 시 동일 통화 기준으로 선택 가능한 정책으로 취급. 여러 템플릿을 동시에 배포할 수 있으며,
           다른 템플릿을 자동 미배포로 바꾸지 않고 DEFAULT에 덮어쓰지도 않는다. */
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    @GetMapping("/defaultCommission/templateOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> defaultCommissionTemplateOptions() {
        Map<Long, String> chargebackNames = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChargebackFeePolicy::getId, ChargebackFeePolicy::getName, (a, b) -> a));
        List<Map<String, Object>> list = listAllTemplatePolicies()
                .stream()
                .map(po -> enrichPolicyMapWithChargebackName(policyToMap(po), po.getChargebackPolicyId(), chargebackNames))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private List<CommissionPolicy> listAllTemplatePolicies() {
        return commissionPolicyRepository.findAll().stream()
                .filter(p -> {
                    String scope = p.getScope() != null ? p.getScope().trim() : "";
                    if (scope.isBlank() || "DEFAULT".equalsIgnoreCase(scope)) return false;
                    if (scope.startsWith(TEMPLATE_SCOPE_PREFIX)) return true;
                    String policyName = p.getPolicyName() != null ? p.getPolicyName().trim() : "";
                    // 레거시 템플릿: 정책명이 있고, scope가 순수 숫자(업체코드) 형식이 아닌 항목
                    return !policyName.isBlank() && !scope.matches("\\d{6,}");
                })
                .sorted(Comparator.comparing(CommissionPolicy::getScope, Comparator.nullsLast(String::compareTo)))
                .toList();
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
        m.put("perTxFee", p.getPerTxFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getPerTxFee()) : "0.0");
        m.put("usageRate", p.getUsageRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsageRate()) : "0.0");
        m.put("failFee", p.getFailFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFailFee()) : "0.0");
        m.put("cancelRate", p.getCancelRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getCancelRate()) : "0.0");
        m.put("voidFeePerTx", p.getVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getVoidFeePerTx()) : "0.0");
        m.put("manualVoidFeePerTx", p.getManualVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getManualVoidFeePerTx()) : "0.0");
        m.put("refundRate", p.getRefundRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRefundRate()) : "0.0");
        m.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "0");
        m.put("feeSettlementPerTx", p.getFeeSettlementPerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFeeSettlementPerTx()) : "0.0");
        m.put("remittanceTransferFee", p.getRemittanceTransferFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRemittanceTransferFee()) : "0.0");
        m.put("usdtTransferFeeUsd", p.getUsdtTransferFeeUsd() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsdtTransferFeeUsd()) : "0.0");
        m.put("feeUsdt", p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "0");
        m.put("feeFx", p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "0");
        m.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "0");
        m.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 180);
        m.put("currencyCode", p.getCurrencyCode() != null && !p.getCurrencyCode().isBlank() ? p.getCurrencyCode() : "KRW");
        m.put("policyRemark", p.getPolicyRemark() != null ? p.getPolicyRemark() : "");
        m.put("fee3dsRate", p.getFee3dsRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFee3dsRate()) : "0");
        m.put("chargebackFeePerTx", p.getChargebackFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getChargebackFeePerTx()) : "0.0");
        m.put("chargebackPolicyId", p.getChargebackPolicyId() != null ? p.getChargebackPolicyId() : "");
        putExtraFeeScalarsOnMap(m, p);
        m.put("tierCommission", tierMapForPolicy(p));
        CommissionTierJsonHelper.applyTierJsonSumsToDisplayMap(m, p);
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return m;
    }

    private static Map<String, Object> tierMapForPolicy(CommissionPolicy p) {
        String j = CommissionTierJsonHelper.hasTierJson(p.getTierCommissionJson())
                ? p.getTierCommissionJson()
                : CommissionTierJsonHelper.buildTierJsonFromPolicyScalars(p);
        if (CommissionTierJsonHelper.hasTierJson(p.getTierCommissionJson())) {
            j = CommissionTierJsonHelper.normalizeTierJsonMerchantSums(j);
        }
        return CommissionTierJsonHelper.parseTierJsonToMap(j);
    }

    private static String tierCommissionJsonFromBody(Map<String, Object> body) throws Exception {
        Object t = body.get("tierCommission");
        if (t == null) {
            return null;
        }
        if (t instanceof String s) {
            return s.isBlank() ? null : s.trim();
        }
        if (t instanceof Map<?, ?> m && !m.isEmpty()) {
            return HQ_OBJECT_MAPPER.writeValueAsString(t);
        }
        return null;
    }

    private static Map<String, Object> enrichPolicyMapWithChargebackName(Map<String, Object> m, Long chargebackPolicyId,
                                                                          Map<Long, String> names) {
        if (chargebackPolicyId != null && names != null && names.containsKey(chargebackPolicyId)) {
            m.put("chargebackPolicyName", names.get(chargebackPolicyId));
        } else {
            m.put("chargebackPolicyName", "");
        }
        return m;
    }

    private static Long parseOptionalPolicyLong(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void putExtraFeeScalarsOnMap(Map<String, Object> data, CommissionPolicy p) {
        for (int i = 1; i <= 4; i++) {
            String name = "";
            String mode = "";
            String val = "0";
            if (p != null) {
                switch (i) {
                    case 1 -> {
                        name = nzStr(p.getExtraFee1Name());
                        mode = nzStr(p.getExtraFee1Mode());
                        val = extraValStr(p.getExtraFee1Value(), mode);
                    }
                    case 2 -> {
                        name = nzStr(p.getExtraFee2Name());
                        mode = nzStr(p.getExtraFee2Mode());
                        val = extraValStr(p.getExtraFee2Value(), mode);
                    }
                    case 3 -> {
                        name = nzStr(p.getExtraFee3Name());
                        mode = nzStr(p.getExtraFee3Mode());
                        val = extraValStr(p.getExtraFee3Value(), mode);
                    }
                    case 4 -> {
                        name = nzStr(p.getExtraFee4Name());
                        mode = nzStr(p.getExtraFee4Mode());
                        val = extraValStr(p.getExtraFee4Value(), mode);
                    }
                    default -> {
                    }
                }
            }
            data.put("extraFee" + i + "Name", name);
            data.put("extraFee" + i + "Mode", mode);
            data.put("extraFee" + i + "Value", val);
        }
    }

    private static String nzStr(String s) {
        return s != null ? s : "";
    }

    private static String extraValStr(BigDecimal b, String mode) {
        if (b == null) {
            return "0";
        }
        if ("PCT".equalsIgnoreCase(mode)) {
            return PercentDecimalHelper.toPlainOneDecimal(b);
        }
        return b.stripTrailingZeros().toPlainString();
    }

    private void applyExtraFeesFromBody(CommissionPolicy p, Map<String, Object> body) {
        for (int i = 1; i <= 4; i++) {
            applyExtraFeeSlot(p, i, body);
        }
    }

    private void applyExtraFeeSlot(CommissionPolicy p, int slot, Map<String, Object> body) {
        String nk = "extraFee" + slot + "Name";
        String mk = "extraFee" + slot + "Mode";
        String vk = "extraFee" + slot + "Value";
        String name = hqStr(body, nk);
        String modeNorm = normalizeExtraMode(hqStr(body, mk));
        BigDecimal val = "PCT".equals(modeNorm)
                ? PercentDecimalHelper.parsePercentOneDecimal(body.get(vk))
                : toBigDecimal(body.get(vk));
        if (name == null || name.isBlank() || modeNorm == null) {
            clearExtraFeeSlot(p, slot);
            return;
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64) {
            trimmed = trimmed.substring(0, 64);
        }
        setExtraFeeSlot(p, slot, trimmed, modeNorm, val);
    }

    private static String normalizeExtraMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("PCT".equals(u) || "%".equals(u)) {
            return "PCT";
        }
        if ("FIX".equals(u) || "고정".equals(u)) {
            return "FIX";
        }
        return null;
    }

    private static void clearExtraFeeSlot(CommissionPolicy p, int slot) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(null);
                p.setExtraFee1Mode(null);
                p.setExtraFee1Value(null);
            }
            case 2 -> {
                p.setExtraFee2Name(null);
                p.setExtraFee2Mode(null);
                p.setExtraFee2Value(null);
            }
            case 3 -> {
                p.setExtraFee3Name(null);
                p.setExtraFee3Mode(null);
                p.setExtraFee3Value(null);
            }
            case 4 -> {
                p.setExtraFee4Name(null);
                p.setExtraFee4Mode(null);
                p.setExtraFee4Value(null);
            }
            default -> {
            }
        }
    }

    private static void setExtraFeeSlot(CommissionPolicy p, int slot, String name, String mode, BigDecimal val) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(name);
                p.setExtraFee1Mode(mode);
                p.setExtraFee1Value(val);
            }
            case 2 -> {
                p.setExtraFee2Name(name);
                p.setExtraFee2Mode(mode);
                p.setExtraFee2Value(val);
            }
            case 3 -> {
                p.setExtraFee3Name(name);
                p.setExtraFee3Mode(mode);
                p.setExtraFee3Value(val);
            }
            case 4 -> {
                p.setExtraFee4Name(name);
                p.setExtraFee4Mode(mode);
                p.setExtraFee4Value(val);
            }
            default -> {
            }
        }
    }

    private static void copyPolicyValues(CommissionPolicy src, CommissionPolicy dst) {
        dst.setPerTxFee(src.getPerTxFee());
        dst.setUsageRate(src.getUsageRate());
        dst.setFailFee(src.getFailFee());
        dst.setCancelRate(src.getCancelRate());
        dst.setVoidFeePerTx(src.getVoidFeePerTx());
        dst.setManualVoidFeePerTx(src.getManualVoidFeePerTx());
        dst.setRefundRate(src.getRefundRate());
        dst.setPayRate(src.getPayRate());
        dst.setFeeSettlementPerTx(src.getFeeSettlementPerTx());
        dst.setRemittanceTransferFee(src.getRemittanceTransferFee());
        dst.setUsdtTransferFeeUsd(src.getUsdtTransferFeeUsd());
        dst.setFeeUsdt(src.getFeeUsdt());
        dst.setFeeFx(src.getFeeFx());
        dst.setRollingPct(src.getRollingPct());
        dst.setRollingDays(src.getRollingDays());
        dst.setCurrencyCode(src.getCurrencyCode());
        dst.setPolicyRemark(src.getPolicyRemark());
        dst.setFee3dsRate(src.getFee3dsRate());
        dst.setChargebackFeePerTx(src.getChargebackFeePerTx());
        dst.setChargebackPolicyId(src.getChargebackPolicyId());
        dst.setExtraFee1Name(src.getExtraFee1Name());
        dst.setExtraFee1Mode(src.getExtraFee1Mode());
        dst.setExtraFee1Value(src.getExtraFee1Value());
        dst.setExtraFee2Name(src.getExtraFee2Name());
        dst.setExtraFee2Mode(src.getExtraFee2Mode());
        dst.setExtraFee2Value(src.getExtraFee2Value());
        dst.setExtraFee3Name(src.getExtraFee3Name());
        dst.setExtraFee3Mode(src.getExtraFee3Mode());
        dst.setExtraFee3Value(src.getExtraFee3Value());
        dst.setExtraFee4Name(src.getExtraFee4Name());
        dst.setExtraFee4Mode(src.getExtraFee4Mode());
        dst.setExtraFee4Value(src.getExtraFee4Value());
        dst.setTierCommissionJson(src.getTierCommissionJson());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null || o.toString().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 차지백(정산 후 환불·강제환불) 월간 건수 구간 정책 — 수수료 정책에서 선택해 연결 */
    @GetMapping("/chargebackPolicy/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> chargebackPolicyList() {
        List<Map<String, Object>> list = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName() != null ? p.getName() : "");
                    m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode() : "KRW");
                    m.put("remark", p.getRemark() != null ? p.getRemark() : "");
                    m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/chargebackPolicy/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chargebackPolicyDetail(@PathVariable("id") long id) {
        return chargebackFeePolicyRepository.findByIdWithTiers(id)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(chargebackPolicyToDetailMap(p))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("차지백 정책을 찾을 수 없습니다.", "NOT_FOUND")));
    }

    @PostMapping("/chargebackPolicy/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chargebackPolicySave(@RequestBody Map<String, Object> body) {
        try {
            ChargebackFeePolicy policy;
            Object idObj = body.get("id");
            if (idObj != null && !idObj.toString().isBlank()) {
                long pid = Long.parseLong(idObj.toString().trim());
                policy = chargebackFeePolicyRepository.findByIdWithTiers(pid)
                        .orElseThrow(() -> new IllegalArgumentException("차지백 정책을 찾을 수 없습니다."));
            } else {
                policy = new ChargebackFeePolicy();
            }
            String name = hqStr(body, "name");
            if (name == null || name.isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("이름은 필수입니다.", "VALIDATION"));
            }
            policy.setName(name.trim());
            policy.setRemark(hqStr(body, "remark"));
            String cur = hqStr(body, "currencyCode");
            if (cur == null || cur.isBlank()) {
                policy.setCurrencyCode("KRW");
            } else {
                policy.setCurrencyCode(cur.trim().toUpperCase(Locale.ROOT));
            }
            policy.getTiers().clear();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tierRows = (List<Map<String, Object>>) body.get("tiers");
            if (tierRows != null) {
                int idx = 0;
                for (Map<String, Object> row : tierRows) {
                    if (row == null) {
                        continue;
                    }
                    ChargebackFeeTier t = new ChargebackFeeTier();
                    t.setPolicy(policy);
                    t.setSortOrder(parseTierInt(row.get("sortOrder"), idx));
                    t.setCountMin(Math.max(0, parseTierInt(row.get("countMin"), 0)));
                    t.setCountMax(parseTierNullableInt(row.get("countMax")));
                    t.setFeePerCase(toBigDecimal(row.get("feePerCase")));
                    policy.getTiers().add(t);
                    idx++;
                }
            }
            chargebackFeePolicyRepository.save(policy);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("id", policy.getId(), "message", "저장되었습니다.")));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("ID 형식이 올바르지 않습니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/chargebackPolicy/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chargebackPolicyDelete(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null || idObj.toString().isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("삭제할 ID가 없습니다.", "VALIDATION"));
        }
        try {
            long id = Long.parseLong(idObj.toString().trim());
            if (commissionPolicyRepository.countByChargebackPolicyId(id) > 0) {
                return ResponseEntity.ok(ApiResponse.fail("이 차지백 정책을 사용 중인 수수료 템플릿이 있어 삭제할 수 없습니다.", "IN_USE"));
            }
            chargebackFeePolicyRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "삭제되었습니다.")));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("ID 형식이 올바르지 않습니다.", "VALIDATION"));
        }
    }

    private Map<String, Object> chargebackPolicyToDetailMap(ChargebackFeePolicy p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName() != null ? p.getName() : "");
        m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode() : "KRW");
        m.put("remark", p.getRemark() != null ? p.getRemark() : "");
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (ChargebackFeeTier t : p.getTiers()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("sortOrder", t.getSortOrder());
            row.put("countMin", t.getCountMin());
            row.put("countMax", t.getCountMax());
            row.put("feePerCase", t.getFeePerCase() != null ? t.getFeePerCase().toPlainString() : "0");
            tiers.add(row);
        }
        m.put("tiers", tiers);
        return m;
    }

    private static int parseTierInt(Object o, int defaultVal) {
        if (o == null || o.toString().isBlank()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static Integer parseTierNullableInt(Object o) {
        if (o == null || o.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
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
        data.put("apiBrokerDefaultFlowType", "INLINE");
        data.put("urlPayDefaultFlowType", "REDIRECT");
        data.put("urlPayPathTemplate", "/pay/{compCode}");
        data.put("apiBrokerInlineEnabledYn", "Y");
        data.put("apiBrokerRedirectEnabledYn", "Y");
        data.put("urlPayInlineEnabledYn", "Y");
        data.put("urlPayRedirectEnabledYn", "Y");
        data.put("urlPayFormMode", "FULL");
        data.put("paymentProviderRegistryJson", "{\n  \"version\": 1,\n  \"vendors\": [\n    {\n      \"vendorCode\": \"CHILLPAY\",\n      \"vendorName\": \"칠리페이\",\n      \"integrationTypes\": [\"API_BROKER\", \"URL_PAY\"],\n      \"flowTypes\": [\"INLINE\", \"REDIRECT\"],\n      \"activeYn\": \"Y\"\n    }\n  ]\n}");
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
            if (c.getApiBrokerDefaultFlowType() != null) data.put("apiBrokerDefaultFlowType", c.getApiBrokerDefaultFlowType());
            if (c.getUrlPayDefaultFlowType() != null) data.put("urlPayDefaultFlowType", c.getUrlPayDefaultFlowType());
            if (c.getUrlPayPathTemplate() != null) data.put("urlPayPathTemplate", c.getUrlPayPathTemplate());
            if (c.getApiBrokerInlineEnabledYn() != null) data.put("apiBrokerInlineEnabledYn", c.getApiBrokerInlineEnabledYn());
            if (c.getApiBrokerRedirectEnabledYn() != null) data.put("apiBrokerRedirectEnabledYn", c.getApiBrokerRedirectEnabledYn());
            if (c.getUrlPayInlineEnabledYn() != null) data.put("urlPayInlineEnabledYn", c.getUrlPayInlineEnabledYn());
            if (c.getUrlPayRedirectEnabledYn() != null) data.put("urlPayRedirectEnabledYn", c.getUrlPayRedirectEnabledYn());
            if (c.getUrlPayFormMode() != null) data.put("urlPayFormMode", c.getUrlPayFormMode());
            if (c.getPaymentProviderRegistryJson() != null) data.put("paymentProviderRegistryJson", c.getPaymentProviderRegistryJson());
            if (c.getPublicAdminSiteUrl() != null) {
                data.put("publicAdminSiteUrl", hqHttpsUrlForDisplay(c.getPublicAdminSiteUrl()));
            }
            if (c.getPublicApiBaseUrl() != null) {
                data.put("publicApiBaseUrl", hqHttpsUrlForDisplay(c.getPublicApiBaseUrl()));
            }
        });
        if (!data.containsKey("publicAdminSiteUrl")) {
            data.put("publicAdminSiteUrl", "");
            data.put("publicApiBaseUrl", "");
        }
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
        c.setApiBrokerDefaultFlowType("REDIRECT".equalsIgnoreCase(String.valueOf(body.getOrDefault("apiBrokerDefaultFlowType", "INLINE"))) ? "REDIRECT" : "INLINE");
        c.setUrlPayDefaultFlowType("INLINE".equalsIgnoreCase(String.valueOf(body.getOrDefault("urlPayDefaultFlowType", "REDIRECT"))) ? "INLINE" : "REDIRECT");
        String pathTpl = body.get("urlPayPathTemplate") != null ? body.get("urlPayPathTemplate").toString().trim() : "";
        c.setUrlPayPathTemplate(pathTpl.isEmpty() ? "/pay/{compCode}" : pathTpl);
        c.setApiBrokerInlineEnabledYn("N".equalsIgnoreCase(String.valueOf(body.getOrDefault("apiBrokerInlineEnabledYn", "Y"))) ? "N" : "Y");
        c.setApiBrokerRedirectEnabledYn("N".equalsIgnoreCase(String.valueOf(body.getOrDefault("apiBrokerRedirectEnabledYn", "Y"))) ? "N" : "Y");
        c.setUrlPayInlineEnabledYn("N".equalsIgnoreCase(String.valueOf(body.getOrDefault("urlPayInlineEnabledYn", "Y"))) ? "N" : "Y");
        c.setUrlPayRedirectEnabledYn("N".equalsIgnoreCase(String.valueOf(body.getOrDefault("urlPayRedirectEnabledYn", "Y"))) ? "N" : "Y");
        String upForm = body.get("urlPayFormMode") != null ? body.get("urlPayFormMode").toString().trim() : "FULL";
        c.setUrlPayFormMode("SIMPLE".equalsIgnoreCase(upForm) ? "SIMPLE" : "FULL");
        c.setPaymentProviderRegistryJson(body.get("paymentProviderRegistryJson") != null ? body.get("paymentProviderRegistryJson").toString().trim() : null);
        if (body.get("publicAdminSiteUrl") != null) {
            c.setPublicAdminSiteUrl(hqHttpsUrlForSave(body.get("publicAdminSiteUrl")));
        }
        if (body.get("publicApiBaseUrl") != null) {
            c.setPublicApiBaseUrl(hqHttpsUrlForSave(body.get("publicApiBaseUrl")));
        }
        hqApiConfigRepository.save(c);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    /** 도메인·공개 URL (관리자/API 안내용) + 본사·총판 조직별 도메인 행 */
    @GetMapping("/domainConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> domainConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("publicAdminSiteUrl", "");
        data.put("publicApiBaseUrl", "");
        data.put("memo", "가맹점·문서·노티 안내에 사용할 공개 URL입니다.");
        String pubAdmin = "";
        String pubApi = "";
        var cfgRow = hqApiConfigRepository.findAll().stream().findFirst();
        if (cfgRow.isPresent()) {
            HqApiConfig c = cfgRow.get();
            if (c.getPublicAdminSiteUrl() != null) {
                pubAdmin = hqHttpsUrlForDisplay(c.getPublicAdminSiteUrl());
                data.put("publicAdminSiteUrl", pubAdmin);
            }
            if (c.getPublicApiBaseUrl() != null) {
                pubApi = hqHttpsUrlForDisplay(c.getPublicApiBaseUrl());
                data.put("publicApiBaseUrl", pubApi);
            }
        }
        List<Map<String, Object>> orgRows = loadOrgDomainRows();
        data.put("orgDomainRows", orgRows);
        data.put("sslDomainLinkage", hqServerManageService.buildSslDomainLinkage(pubAdmin, pubApi, orgRows));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/domainConfig/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> domainConfigSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(new HqApiConfig());
        c.setPublicAdminSiteUrl(hqHttpsUrlForSave(body != null ? body.get("publicAdminSiteUrl") : null));
        c.setPublicApiBaseUrl(hqHttpsUrlForSave(body != null ? body.get("publicApiBaseUrl") : null));
        hqApiConfigRepository.save(c);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "저장되었습니다.");
        String pa = c.getPublicAdminSiteUrl() != null ? c.getPublicAdminSiteUrl() : "";
        String pap = c.getPublicApiBaseUrl() != null ? c.getPublicApiBaseUrl() : "";
        out.put("sslDomainLinkage", hqServerManageService.buildSslDomainLinkage(pa, pap, loadOrgDomainRows()));
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    /** 본사·총판(tb_org_unit) 조직별 공개 URL·설정명 저장 */
    @PostMapping("/domainConfig/orgSave")
    public ResponseEntity<ApiResponse<Map<String, Object>>> domainConfigOrgSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        if (body == null || body.get("orgUnitId") == null || body.get("orgUnitId").toString().isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("조직(업체)을 선택하세요.", "VALIDATION"));
        }
        long orgUnitId;
        try {
            orgUnitId = Long.parseLong(body.get("orgUnitId").toString().trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("조직 ID가 올바르지 않습니다.", "VALIDATION"));
        }
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null) {
            return ResponseEntity.ok(ApiResponse.fail("조직을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (ou.getOrgLevel() != OrgLevel.REGIONAL && ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return ResponseEntity.ok(ApiResponse.fail("본사·총판만 도메인을 설정할 수 있습니다.", "VALIDATION"));
        }
        String oldNm = ou.getDomainSettingName() != null ? ou.getDomainSettingName().trim() : "";
        String oldAdm = ou.getOrgDomainAdminUrl() != null ? ou.getOrgDomainAdminUrl().trim() : "";
        String oldApi = ou.getOrgDomainApiUrl() != null ? ou.getOrgDomainApiUrl().trim() : "";
        ou.setDomainSettingName(hqTrimToNull(body.get("domainSettingName")));
        ou.setOrgDomainAdminUrl(hqHttpsUrlForSave(body.get("orgDomainAdminUrl")));
        ou.setOrgDomainApiUrl(hqHttpsUrlForSave(body.get("orgDomainApiUrl")));
        ou.setDomainUrlsUpdatedAt(LocalDateTime.now());
        orgUnitRepository.save(ou);
        String compNm = ou.getName() != null ? ou.getName().trim() : "";
        String p = "[도메인구성설정] ";
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "설정표시명",
                oldNm, ou.getDomainSettingName() != null ? ou.getDomainSettingName().trim() : "");
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "관리자 URL",
                oldAdm, ou.getOrgDomainAdminUrl() != null ? ou.getOrgDomainAdminUrl().trim() : "");
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "API URL",
                oldApi, ou.getOrgDomainApiUrl() != null ? ou.getOrgDomainApiUrl().trim() : "");
        List<Map<String, Object>> orgRows = loadOrgDomainRows();
        String pa = "";
        String pap = "";
        Optional<HqApiConfig> cfg = hqApiConfigRepository.findAll().stream().findFirst();
        if (cfg.isPresent()) {
            if (cfg.get().getPublicAdminSiteUrl() != null) {
                pa = cfg.get().getPublicAdminSiteUrl();
            }
            if (cfg.get().getPublicApiBaseUrl() != null) {
                pap = cfg.get().getPublicApiBaseUrl();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "도메인 설정이 저장되었습니다.");
        out.put("orgDomainRows", orgRows);
        out.put("sslDomainLinkage", hqServerManageService.buildSslDomainLinkage(pa, pap, orgRows));
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    /** 본사·총판 조직별 도메인 설정 비우기(설정명·관리자 URL·API URL) */
    @PostMapping("/domainConfig/orgDelete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> domainConfigOrgDelete(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 삭제할 수 있습니다.", "FORBIDDEN"));
        }
        if (body == null || body.get("orgUnitId") == null || body.get("orgUnitId").toString().isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("조직(업체)을 지정하세요.", "VALIDATION"));
        }
        long orgUnitId;
        try {
            orgUnitId = Long.parseLong(body.get("orgUnitId").toString().trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("조직 ID가 올바르지 않습니다.", "VALIDATION"));
        }
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null) {
            return ResponseEntity.ok(ApiResponse.fail("조직을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (ou.getOrgLevel() != OrgLevel.REGIONAL && ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return ResponseEntity.ok(ApiResponse.fail("본사·총판만 도메인 설정 대상입니다.", "VALIDATION"));
        }
        String oldNm = ou.getDomainSettingName() != null ? ou.getDomainSettingName().trim() : "";
        String oldAdm = ou.getOrgDomainAdminUrl() != null ? ou.getOrgDomainAdminUrl().trim() : "";
        String oldApi = ou.getOrgDomainApiUrl() != null ? ou.getOrgDomainApiUrl().trim() : "";
        ou.setDomainSettingName(null);
        ou.setOrgDomainAdminUrl(null);
        ou.setOrgDomainApiUrl(null);
        ou.setDomainUrlsUpdatedAt(LocalDateTime.now());
        orgUnitRepository.save(ou);
        String compNm = ou.getName() != null ? ou.getName().trim() : "";
        String p = "[도메인구성설정] ";
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "설정표시명", oldNm, "");
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "관리자 URL", oldAdm, "");
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), ou.getCode(), compNm, p + "API URL", oldApi, "");
        List<Map<String, Object>> orgRows = loadOrgDomainRows();
        String pa = "";
        String pap = "";
        Optional<HqApiConfig> cfg = hqApiConfigRepository.findAll().stream().findFirst();
        if (cfg.isPresent()) {
            if (cfg.get().getPublicAdminSiteUrl() != null) {
                pa = cfg.get().getPublicAdminSiteUrl();
            }
            if (cfg.get().getPublicApiBaseUrl() != null) {
                pap = cfg.get().getPublicApiBaseUrl();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "도메인 설정을 삭제했습니다.");
        out.put("orgDomainRows", orgRows);
        out.put("sslDomainLinkage", hqServerManageService.buildSslDomainLinkage(pa, pap, orgRows));
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    private List<Map<String, Object>> loadOrgDomainRows() {
        List<OrgUnit> orgs = orgUnitRepository.findByOrgLevelInOrderByNameAsc(List.of(OrgLevel.REGIONAL, OrgLevel.MASTER_DIST));
        return orgs.stream()
                .filter(o -> o.getStatus() == null || "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .map(this::orgUnitToDomainRow)
                .collect(Collectors.toList());
    }

    private Map<String, Object> orgUnitToDomainRow(OrgUnit o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orgUnitId", o.getId());
        m.put("code", o.getCode());
        m.put("name", o.getName());
        m.put("orgLevel", o.getOrgLevel() != null ? o.getOrgLevel().name() : "");
        m.put("orgLevelLabel", o.getOrgLevel() != null ? o.getOrgLevel().getNameKo() : "");
        m.put("domainSettingName", o.getDomainSettingName() != null ? o.getDomainSettingName() : "");
        m.put("orgDomainAdminUrl", hqHttpsUrlForDisplay(o.getOrgDomainAdminUrl()));
        m.put("orgDomainApiUrl", hqHttpsUrlForDisplay(o.getOrgDomainApiUrl()));
        m.put("domainUrlsUpdatedAt", o.getDomainUrlsUpdatedAt() != null ? o.getDomainUrlsUpdatedAt().toString() : "");
        return m;
    }

    /** 서버운영관리 요약(SSL·Certbot·호스트 등) */
    @GetMapping("/serverManage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serverManage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(hqServerManageService.buildSummary()));
    }

    /**
     * 일간/주간/월간 트래픽(송수신 합)·메모리 피크 시계열 + 현황 요약 (NOTI 시스템 모니터 유사).
     * grain=daily | weekly | monthly
     */
    @GetMapping("/serverUsage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serverUsage(
            @RequestParam(name = "grain", defaultValue = "daily") String grain) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(serverUsageService.buildUsageReport(grain)));
    }

    @PostMapping("/serverManage/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serverManageSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u) || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return ResponseEntity.ok(ApiResponse.fail("관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(new HqApiConfig());
        c.setServerManageSslCertPath(hqTrimToNull(body != null ? body.get("serverManageSslCertPath") : null));
        c.setServerManageSslLeDomain(hqTrimToNull(body != null ? body.get("serverManageSslLeDomain") : null));
        c.setServerManageContractDiskMb(hqParsePositiveInt(body != null ? body.get("serverManageContractDiskMb") : null));
        c.setServerManageContractTrafficMb(hqParsePositiveInt(body != null ? body.get("serverManageContractTrafficMb") : null));
        c.setServerManageTrafficUsedMb(hqParseOptionalNonNegativeInt(body != null ? body.get("serverManageTrafficUsedMb") : null));
        c.setServerManageContractStart(hqParseLocalDate(body != null ? body.get("serverManageContractStart") : null));
        c.setServerManageContractEnd(hqParseLocalDate(body != null ? body.get("serverManageContractEnd") : null));
        c.setServerManageUiRefreshSec(hqParseUiRefreshSec(body != null ? body.get("serverManageUiRefreshSec") : null));
        hqApiConfigRepository.save(c);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.")));
    }

    /** 15~3600 또는 비움(NULL) → yml 기본 사용 (JSON 숫자가 Double 로 올 수 있음) */
    private static Integer hqParseUiRefreshSec(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            int v = (int) Math.round(n.doubleValue());
            if (v < 15 || v > 3600) {
                return null;
            }
            return v;
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(s.replace(",", ""));
            if (v < 15 || v > 3600) {
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String hqTrimToNull(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 저장: 스킴 없으면 https:// 부착. http(s) 명시 시 유지. */
    private static String hqHttpsUrlForSave(Object o) {
        String t = hqTrimToNull(o);
        if (t == null) return null;
        if (t.matches("(?i)https?://.*")) return t;
        return "https://" + t.replaceFirst("^/+", "");
    }

    /** 조회·목록 표시: 비면 "", 스킴 없으면 https:// 부착. */
    private static String hqHttpsUrlForDisplay(String stored) {
        String t = hqTrimToNull(stored);
        if (t == null) return "";
        if (t.matches("(?i)https?://.*")) return t;
        return "https://" + t.replaceFirst("^/+", "");
    }

    private static Integer hqParsePositiveInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) {
            int v = (int) Math.round(n.doubleValue());
            return v <= 0 ? null : v;
        }
        String s = o.toString().trim().replace(",", "");
        if (s.isEmpty()) return null;
        try {
            int v = Integer.parseInt(s);
            return v <= 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate hqParseLocalDate(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 비우면 null, 0 이상 정수 (트래픽 누적 등). JSON Number 가 Double 이면 parseInt 실패하던 문제 방지 */
    private static Integer hqParseOptionalNonNegativeInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) {
            int v = (int) Math.round(n.doubleValue());
            return Math.max(0, v);
        }
        String s = o.toString().trim().replace(",", "");
        if (s.isEmpty()) return null;
        try {
            int v = Integer.parseInt(s);
            return Math.max(0, v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 본사설정 > 영업일설정 목록 조회 */
    @GetMapping("/businessDaySettings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> businessDaySettings() {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        String raw = c != null ? c.getBusinessDaySettingsJson() : null;
        List<Map<String, Object>> list = parseBusinessDaySettings(raw);
        enrichBusinessDayHolidayCounts(list);
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
        if (!Set.of("KR", "US", "JP", "TH", "CN", "GLOBAL").contains(countryCode)) {
            return ResponseEntity.ok(ApiResponse.fail("기준국가는 KR/US/JP/TH/CN/GLOBAL만 가능합니다.", "VALIDATION"));
        }
        if ("DELETE".equals(mode)) {
            if (id.isEmpty()) return ResponseEntity.ok(ApiResponse.fail("삭제할 ID가 필요합니다.", "VALIDATION"));
            final String deleteId = id;
            list = list.stream().filter(m -> !Objects.equals(String.valueOf(m.getOrDefault("id", "")), deleteId)).collect(Collectors.toList());
            c.setBusinessDaySettingsJson(writeBusinessDaySettings(list));
            hqApiConfigRepository.save(c);
            enrichBusinessDayHolidayCounts(list);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "삭제되었습니다.", "list", list)));
        }
        if (name.isEmpty()) return ResponseEntity.ok(ApiResponse.fail("이름은 필수입니다.", "VALIDATION"));
        String originalIdFromClient = id;
        if (id.isEmpty()) id = UUID.randomUUID().toString();
        String finalId = id;
        for (Map<String, Object> m : list) {
            String mid = String.valueOf(m.getOrDefault("id", ""));
            String mn = String.valueOf(m.getOrDefault("name", ""));
            if (!mid.equals(finalId) && mn.equalsIgnoreCase(name)) {
                return ResponseEntity.ok(ApiResponse.fail("동일한 이름이 이미 있습니다.", "DUPLICATE"));
            }
        }
        List<Map<String, Object>> manualEntries = parseHolidayManualEntriesFromBody(body.get("holidayManualEntries"));
        String extraFromEntries = expandHolidayManualEntriesToDates(manualEntries);
        String mergedExtra = mergeHolidayDateLines(extraDates, extraFromEntries);

        String preservedCreatedBy = "";
        String preservedCreatedAt = "";
        String preservedUpdatedAt = "";
        for (Map<String, Object> ex : list) {
            if (Objects.equals(String.valueOf(ex.getOrDefault("id", "")), finalId)) {
                preservedCreatedBy = String.valueOf(ex.getOrDefault("createdBy", ""));
                preservedCreatedAt = String.valueOf(ex.getOrDefault("createdAt", ""));
                preservedUpdatedAt = String.valueOf(ex.getOrDefault("updatedAt", ""));
                break;
            }
        }
        boolean isNewProfile = originalIdFromClient.isEmpty();
        String actor = resolveCurrentLoginId();
        String createdByVal = isNewProfile ? actor : (preservedCreatedBy.isBlank() ? actor : preservedCreatedBy);
        String today = java.time.LocalDate.now().toString();
        String createdAtVal;
        if (isNewProfile) {
            createdAtVal = today;
        } else {
            if (preservedCreatedAt != null && !preservedCreatedAt.isBlank() && !"null".equalsIgnoreCase(preservedCreatedAt)) {
                createdAtVal = preservedCreatedAt;
            } else if (preservedUpdatedAt != null && !preservedUpdatedAt.isBlank() && !"null".equalsIgnoreCase(preservedUpdatedAt)) {
                createdAtVal = preservedUpdatedAt;
            } else {
                createdAtVal = today;
            }
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("countryCode", countryCode);
        row.put("businessHolidayExtraDates", mergedExtra);
        row.put("holidayManualEntries", manualEntries);
        row.put("createdBy", createdByVal);
        row.put("createdAt", createdAtVal);
        row.put("updatedAt", today);
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
        enrichBusinessDayHolidayCounts(list);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "저장되었습니다.", "id", id, "list", list)));
    }

    /** 저장된 비영업 일자 중 공식(토·일·해당국 프리셋 법정일) / 추가(그 외) / 총 — API 응답용 (DB JSON에는 넣지 않음) */
    private void enrichBusinessDayHolidayCounts(List<Map<String, Object>> list) {
        if (list == null || holidayPresetService == null) return;
        for (Map<String, Object> row : list) {
            attachHolidayKindCounts(row);
        }
    }

    private void attachHolidayKindCounts(Map<String, Object> row) {
        Set<String> totalSet = parseBizdayHolidayDateSet(String.valueOf(row.getOrDefault("businessHolidayExtraDates", "")));
        int total = totalSet.size();
        String cc = String.valueOf(row.getOrDefault("countryCode", "KR")).trim().toUpperCase(Locale.ROOT);
        Set<Integer> years = new TreeSet<>();
        for (String d : totalSet) {
            if (d == null || d.length() < 4) continue;
            try {
                years.add(Integer.parseInt(d.substring(0, 4)));
            } catch (NumberFormatException ignored) {
            }
        }
        Set<String> officialRef = new TreeSet<>();
        for (int y : years) {
            officialRef.addAll(holidayPresetService.officialHolidayDatesForProfile(y, cc));
        }
        int official = 0;
        for (String d : totalSet) {
            if (officialRef.contains(d)) {
                official++;
            }
        }
        int additional = total - official;
        row.put("holidayCountOfficial", official);
        row.put("holidayCountAdditional", Math.max(0, additional));
        row.put("holidayCountTotal", total);
    }

    private static Set<String> parseBizdayHolidayDateSet(String raw) {
        Set<String> set = new TreeSet<>();
        if (raw == null || raw.isBlank()) {
            return set;
        }
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                set.add(t.substring(0, 10));
            }
        }
        return set;
    }

    private static String resolveCurrentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u && u.getUsername() != null) {
            return u.getUsername().trim();
        }
        if (auth != null && auth.getName() != null) {
            return auth.getName().trim();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseHolidayManualEntriesFromBody(Object raw) {
        if (raw == null) return new ArrayList<>();
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            if (raw instanceof String s) {
                s = s.trim();
                if (s.isEmpty()) return new ArrayList<>();
                return om.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }
            if (raw instanceof List<?> l) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object o : l) {
                    if (o instanceof Map<?, ?> mm) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        mm.forEach((k, v) -> row.put(String.valueOf(k), v));
                        out.add(row);
                    }
                }
                return out;
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private static String expandHolidayManualEntriesToDates(List<Map<String, Object>> entries) {
        if (entries == null || entries.isEmpty()) return "";
        Set<String> days = new TreeSet<>();
        for (Map<String, Object> e : entries) {
            String from = String.valueOf(e.getOrDefault("fromDate", "")).trim();
            String to = String.valueOf(e.getOrDefault("toDate", "")).trim();
            if (from.isEmpty()) continue;
            if (to.isEmpty()) to = from;
            try {
                LocalDate a = LocalDate.parse(from);
                LocalDate b = LocalDate.parse(to);
                if (b.isBefore(a)) {
                    LocalDate t = a;
                    a = b;
                    b = t;
                }
                for (LocalDate d = a; !d.isAfter(b); d = d.plusDays(1)) {
                    days.add(d.toString());
                }
            } catch (Exception ignored) {
            }
        }
        return String.join("\n", days);
    }

    private static String mergeHolidayDateLines(String a, String b) {
        Set<String> set = new TreeSet<>();
        for (String part : new String[] { a, b }) {
            if (part == null || part.isBlank()) continue;
            for (String line : part.split("\\r?\\n")) {
                String t = line.trim();
                if (t.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    set.add(t.substring(0, 10));
                }
            }
        }
        return String.join("\n", set);
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
                Object createdByVal = mm.get("createdBy");
                Object createdAtVal = mm.get("createdAt");
                row.put("id", idVal == null ? "" : String.valueOf(idVal));
                row.put("name", nameVal == null ? "" : String.valueOf(nameVal));
                row.put("countryCode", ccVal == null ? "KR" : String.valueOf(ccVal));
                row.put("businessHolidayExtraDates", extraVal == null ? "" : String.valueOf(extraVal));
                row.put("updatedAt", updVal == null ? "" : String.valueOf(updVal));
                row.put("createdBy", createdByVal == null ? "" : String.valueOf(createdByVal));
                row.put("createdAt", createdAtVal == null ? "" : String.valueOf(createdAtVal));
                List<Map<String, Object>> manual = extractHolidayManualEntries(mm.get("holidayManualEntries"));
                if (manual.isEmpty()) {
                    manual = migrateExtraDatesLinesToEntries(String.valueOf(row.get("businessHolidayExtraDates")));
                }
                row.put("holidayManualEntries", manual);
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractHolidayManualEntries(Object raw) {
        if (raw == null) return new ArrayList<>();
        if (raw instanceof List<?> l) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : l) {
                if (o instanceof Map<?, ?> mm) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    mm.forEach((k, v) -> row.put(String.valueOf(k), v));
                    out.add(row);
                }
            }
            return out;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                var om = new com.fasterxml.jackson.databind.ObjectMapper();
                return om.readValue(s.trim(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>();
    }

    private static List<Map<String, Object>> migrateExtraDatesLinesToEntries(String extra) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (extra == null || extra.isBlank()) return out;
        for (String line : extra.split("\\r?\\n")) {
            String t = line.trim();
            if (t.length() >= 10 && t.substring(0, 10).matches("\\d{4}-\\d{2}-\\d{2}")) {
                String day = t.substring(0, 10);
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("fromDate", day);
                e.put("toDate", day);
                e.put("holidayKind", "공휴일");
                e.put("note", "");
                out.add(e);
            }
        }
        return out;
    }

    /** 4. 조직별 페이지/기능 접근 권한 세팅 — 총본사(HEADQUARTERS)·ADMIN: 전체, 본사·총판: 담당자 권한그룹만 */
    @GetMapping("/permissionMng")
    public ResponseEntity<ApiResponse<Map<String, Object>>> permissionMng() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "FORBIDDEN"));
        }
        if (!mayOpenPermissionScreen(u)) {
            return ResponseEntity.ok(ApiResponse.fail("이 메뉴를 열 권한이 없습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(orgPagePermissionService.buildPermissionMngPayload(u)));
    }

    @PostMapping("/permissionMng/save")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> permissionMngSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "FORBIDDEN"));
        }
        if (!maySaveOrgLevelMatrix(u)) {
            return ResponseEntity.ok(ApiResponse.fail("조직 단계별 기본 권한은 총본사(또는 시스템 관리자)만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Object raw = body != null ? body.get("matrix") : null;
            if (!(raw instanceof Map)) {
                return ResponseEntity.ok(ApiResponse.fail("matrix 형식이 올바르지 않습니다.", "VALIDATION"));
            }
            Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                String orgLv = e.getKey() != null ? e.getKey().toString() : "";
                if (!(e.getValue() instanceof Map<?, ?> sub)) continue;
                Map<String, String> pages = new LinkedHashMap<>();
                for (Map.Entry<?, ?> pe : sub.entrySet()) {
                    pages.put(String.valueOf(pe.getKey()), pe.getValue() != null ? pe.getValue().toString() : "");
                }
                matrix.put(orgLv, pages);
            }
            orgPagePermissionService.saveMatrix(matrix);
            return ResponseEntity.ok(ApiResponse.ok(orgPagePermissionService.buildPermissionMngPayload(u)));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "저장 실패", "ERROR"));
        }
    }

    /** 개별 조직(총본사~가맹점) 권한 조회 — 단계 기본·최종·담당자(권한그룹)별 매트릭스 포함 */
    @GetMapping("/orgUnitPermission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orgUnitPermission(@RequestParam Long orgUnitId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "FORBIDDEN"));
        }
        if (!mayInspectOrgUnitPermission(u, orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("해당 조직 권한을 조회할 수 없습니다.", "FORBIDDEN"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(orgPagePermissionService.buildOrgUnitPermissionPayload(orgUnitId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 개별 조직 권한 저장 (단계 기본 따름 / 개별 설정) — 총본사·ADMIN만 */
    @PostMapping("/orgUnitPermission/save")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orgUnitPermissionSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "FORBIDDEN"));
        }
        if (!maySaveOrgUnitCustomPermission(u)) {
            return ResponseEntity.ok(ApiResponse.fail("개별 조직 권한은 총본사(또는 시스템 관리자)만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Object idObj = body != null ? body.get("orgUnitId") : null;
            if (idObj == null) {
                return ResponseEntity.ok(ApiResponse.fail("orgUnitId가 필요합니다.", "VALIDATION"));
            }
            long orgUnitId = Long.parseLong(idObj.toString().trim());
            String mode = body != null && body.get("mode") != null ? body.get("mode").toString() : OrgPagePermissionService.MODE_LEVEL_DEFAULT;
            Map<String, String> pages = new LinkedHashMap<>();
            Object rawPages = body != null ? body.get("pages") : null;
            if (rawPages instanceof Map<?, ?> rm) {
                for (Map.Entry<?, ?> pe : rm.entrySet()) {
                    pages.put(String.valueOf(pe.getKey()), pe.getValue() != null ? pe.getValue().toString() : "");
                }
            }
            orgPagePermissionService.saveOrgUnitPermission(orgUnitId, mode, pages);
            return ResponseEntity.ok(ApiResponse.ok(orgPagePermissionService.buildOrgUnitPermissionPayload(orgUnitId)));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("orgUnitId 형식이 올바르지 않습니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "저장 실패", "ERROR"));
        }
    }

    /**
     * 담당자 권한그룹(관리/운영/정산/기술)별 메뉴 권한 저장 — 본사·총판·총본사는 자기 조직만, ADMIN은 전체.
     */
    @PostMapping("/orgUnitAssistantPermission/save")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orgUnitAssistantPermissionSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "FORBIDDEN"));
        }
        try {
            Object idObj = body != null ? body.get("orgUnitId") : null;
            if (idObj == null) {
                return ResponseEntity.ok(ApiResponse.fail("orgUnitId가 필요합니다.", "VALIDATION"));
            }
            long orgUnitId = Long.parseLong(idObj.toString().trim());
            if (!maySaveAssistantOrgPermission(u, orgUnitId)) {
                return ResponseEntity.ok(ApiResponse.fail("담당자 권한그룹 설정을 저장할 수 없습니다.", "FORBIDDEN"));
            }
            Object rawMatrix = body != null ? body.get("matrix") : null;
            Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
            if (rawMatrix instanceof Map<?, ?> rm) {
                for (Map.Entry<?, ?> e : rm.entrySet()) {
                    String role = e.getKey() != null ? e.getKey().toString() : "";
                    if (!(e.getValue() instanceof Map<?, ?> sub)) {
                        continue;
                    }
                    Map<String, String> pages = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> pe : sub.entrySet()) {
                        pages.put(String.valueOf(pe.getKey()), pe.getValue() != null ? pe.getValue().toString() : "");
                    }
                    matrix.put(role, pages);
                }
            }
            orgPagePermissionService.saveOrgUnitAssistantPermission(orgUnitId, matrix);
            return ResponseEntity.ok(ApiResponse.ok(orgPagePermissionService.buildOrgUnitPermissionPayload(orgUnitId)));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.fail("orgUnitId 형식이 올바르지 않습니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "저장 실패", "ERROR"));
        }
    }

    private boolean mayOpenPermissionScreen(AppUser u) {
        if (u == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) {
            return false;
        }
        String ol = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        return "HEADQUARTERS".equals(ol) || "REGIONAL".equals(ol) || "MASTER_DIST".equals(ol);
    }

    private boolean maySaveOrgLevelMatrix(AppUser u) {
        if (u == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) {
            return false;
        }
        return "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }

    private boolean maySaveOrgUnitCustomPermission(AppUser u) {
        return maySaveOrgLevelMatrix(u);
    }

    private boolean mayInspectOrgUnitPermission(AppUser u, long orgUnitId) {
        if (u == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) {
            return false;
        }
        String ol = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        if ("HEADQUARTERS".equals(ol)) {
            return true;
        }
        if ("REGIONAL".equals(ol) || "MASTER_DIST".equals(ol)) {
            Object idObj = org.get("orgUnitId");
            if (idObj == null) {
                return false;
            }
            try {
                return orgUnitId == Long.parseLong(idObj.toString().trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean maySaveAssistantOrgPermission(AppUser u, long orgUnitId) {
        if (u == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) {
            return false;
        }
        String ol = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        if (!"HEADQUARTERS".equals(ol) && !"REGIONAL".equals(ol) && !"MASTER_DIST".equals(ol)) {
            return false;
        }
        Object idObj = org.get("orgUnitId");
        if (idObj == null) {
            return false;
        }
        try {
            return orgUnitId == Long.parseLong(idObj.toString().trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
