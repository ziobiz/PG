package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.HqSettlementCycleDef;
import com.pg.entity.MasterDistSettlementCycleConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.service.AuthService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqSettlementCycleAdminService;
import com.pg.service.MasterDistSettlementCronZoneService;
import com.pg.service.MasterDistSettlementCycleConfigService;
import com.pg.service.ReceivableRecoveryModeService;
import com.pg.service.SettlementCalcCycleTransitionService;
import com.pg.service.settlement.SettlementAutoRunService;
import com.pg.util.ReceivableRecoveryModeUtil;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 본사설정 — 정산관리설정(정산주기 관리·정산일정 요약).
 */
@RestController
@RequestMapping(value = "/api/hq/settlement", produces = "application/json")
public class ApiHqSettlementController {

    private final HqSettlementCycleAdminService hqSettlementCycleAdminService;
    private final MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService;
    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final SettlementCalcCycleTransitionService settlementCalcCycleTransitionService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;
    private final SettlementAutoRunService settlementAutoRunService;
    private final ReceivableRecoveryModeService receivableRecoveryModeService;
    private final boolean appSettlementAutoRunEnabled;

    public ApiHqSettlementController(HqSettlementCycleAdminService hqSettlementCycleAdminService,
                                     MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService,
                                     AuthService authService,
                                     OrgUnitRepository orgUnitRepository,
                                     SettlementSettingRepository settlementSettingRepository,
                                     SettlementCalcCycleTransitionService settlementCalcCycleTransitionService,
                                     HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                     MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,
                                     SettlementAutoRunService settlementAutoRunService,
                                     ReceivableRecoveryModeService receivableRecoveryModeService,
                                     @Value("${app.settlement.autoRunEnabled:false}") boolean appSettlementAutoRunEnabled) {
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
        this.masterDistSettlementCycleConfigService = masterDistSettlementCycleConfigService;
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.settlementCalcCycleTransitionService = settlementCalcCycleTransitionService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
        this.settlementAutoRunService = settlementAutoRunService;
        this.receivableRecoveryModeService = receivableRecoveryModeService;
        this.appSettlementAutoRunEnabled = appSettlementAutoRunEnabled;
    }

    private boolean canManageSettlementSettings(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        return org != null && "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }

    /** 가맹·검색 화면 정산주기 셀렉트용(로그인 사용자) */
    @GetMapping("/cycleOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleOptions() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listActiveSelectOptions()));
    }

    /** 총판별 가맹 정산주기 설정 등 — 병합 표준 전체(미사용 N 포함), 순서·코드표시는 정산주기관리 병합 표와 동일 */
    @GetMapping("/cycleOptionsCatalog")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleOptionsCatalog() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listCatalogSelectOptions()));
    }

    /**
     * 가맹 조직 트리 임의 노드 기준(권장: 가맹 {@code fromOrgUnitId}): 총판 허용 슬롯만(슬롯 순),
     * 본사 직속·총판 미설정이면 본사 필수 정산주기만(활성 Y).
     * {@code parentOrgUnitId} 는 하위 호환용 별칭({@code fromOrgUnitId} 가 없을 때만 사용).
     * {@code ensureCycleCode} 가 목록에 없으면(과거 저장값) 병합 카탈로그 행을 한 줄 덧붙이고 {@code orphanSavedCycleYn}=Y.
     */
    @GetMapping("/cycleOptionsScoped")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cycleOptionsScoped(
            @RequestParam(required = false) Long parentOrgUnitId,
            @RequestParam(required = false) Long fromOrgUnitId,
            @RequestParam(required = false) String ensureCycleCode) {
        Long start = fromOrgUnitId != null ? fromOrgUnitId : parentOrgUnitId;
        return ResponseEntity.ok(ApiResponse.ok(
                masterDistSettlementCycleConfigService.buildScopedCycleOptionsForMerchantParent(start, ensureCycleCode)));
    }

    /**
     * 서버 정산 자동 배치: JVM 설정(app.settlement.autoRunEnabled) + DB 본사 모드(ACTIVE / INACTIVE / AUTO).
     */
    @GetMapping("/autoBatch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoBatchGet(Authentication auth) {
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        HqLedgerSysSettings s = hqLedgerSysSettingsService.getOrCreate();
        boolean peekDue = settlementAutoRunService.peekAnyDueAutoWorkThisTick();
        boolean dbAllows = hqLedgerSysSettingsService.isSettlementAutoBatchDbTickAllowed(peekDue);
        String mode = HqLedgerSysSettingsService.normalizeSettlementAutoBatchMode(s.getSettlementAutoBatchMode());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("settlementAutoBatchMode", mode);
        m.put("settlementAutoBatchEnabledYn",
                HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_ACTIVE.equals(mode)
                        || HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_AUTO.equals(mode) ? "Y" : "N");
        m.put("peekDueAutoWorkThisTick", peekDue);
        m.put("serverAutoRunPropertyEnabled", appSettlementAutoRunEnabled);
        m.put("batchTickEffective", appSettlementAutoRunEnabled && dbAllows);
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/autoBatch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoBatchSave(Authentication auth,
                                                                           @RequestBody Map<String, Object> body) {
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        String mode = null;
        if (body != null && body.get("settlementAutoBatchMode") != null) {
            mode = String.valueOf(body.get("settlementAutoBatchMode")).trim();
        }
        if ((mode == null || mode.isBlank()) && body != null && body.get("settlementAutoBatchEnabledYn") != null) {
            String yn = String.valueOf(body.get("settlementAutoBatchEnabledYn")).trim();
            mode = "Y".equalsIgnoreCase(yn)
                    ? HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_ACTIVE
                    : HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_INACTIVE;
        }
        if (mode == null || mode.isBlank()) {
            mode = HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_INACTIVE;
        }
        hqLedgerSysSettingsService.updateSettlementAutoBatchMode(mode);
        HqLedgerSysSettings s = hqLedgerSysSettingsService.getOrCreate();
        boolean peekDue = settlementAutoRunService.peekAnyDueAutoWorkThisTick();
        boolean dbAllows = hqLedgerSysSettingsService.isSettlementAutoBatchDbTickAllowed(peekDue);
        String modeOut = HqLedgerSysSettingsService.normalizeSettlementAutoBatchMode(s.getSettlementAutoBatchMode());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("settlementAutoBatchMode", modeOut);
        m.put("settlementAutoBatchEnabledYn",
                HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_ACTIVE.equals(modeOut)
                        || HqLedgerSysSettingsService.SETTLEMENT_AUTO_BATCH_AUTO.equals(modeOut) ? "Y" : "N");
        m.put("peekDueAutoWorkThisTick", peekDue);
        m.put("serverAutoRunPropertyEnabled", appSettlementAutoRunEnabled);
        m.put("batchTickEffective", appSettlementAutoRunEnabled && dbAllows);
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @GetMapping("/masterDistOrgOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> masterDistOrgOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.listMasterDistOrgOptions()));
    }

    /**
     * 총판별 영업일 프로필(총판 regionalSettings 표시) + 정산 크론 기준 Zone — 영업일 로직과 별도 설정.
     */
    @GetMapping("/masterDistBizCronZone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistBizCronZoneGet() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", masterDistSettlementCronZoneService.listMasterDistBizCronRows());
        out.put("presets", MasterDistSettlementCronZoneService.settlementCronZonePresetOptions());
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    @PostMapping("/masterDistSettlementCronZone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistSettlementCronZoneSave(Authentication auth,
                                                                                             @RequestBody Map<String, Object> body) {
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            if (body == null || body.get("orgUnitId") == null) {
                return ResponseEntity.ok(ApiResponse.fail("orgUnitId가 필요합니다.", "VALIDATION"));
            }
            long orgUnitId = parseRequestLong(body.get("orgUnitId"), "orgUnitId");
            String zoneOrPreset = body.get("settlementCronZoneId") != null
                    ? String.valueOf(body.get("settlementCronZoneId")).trim()
                    : (body.get("zoneOrPreset") != null ? String.valueOf(body.get("zoneOrPreset")).trim() : "");
            if (zoneOrPreset.isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("settlementCronZoneId(또는 zoneOrPreset)가 필요합니다.", "VALIDATION"));
            }
            Map<String, Object> saved = masterDistSettlementCronZoneService.saveSettlementCronZoneOnly(orgUnitId, zoneOrPreset);
            return ResponseEntity.ok(ApiResponse.ok(saved));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = "정산 크론 기준 저장 중 오류가 발생했습니다. (" + e.getClass().getSimpleName() + ")";
            }
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        }
    }

    @GetMapping("/masterDistCalcCycleConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistCalcCycleGet(
            @RequestParam long orgUnitId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return masterDistSettlementCycleConfigService.findByMasterDistOrgId(orgUnitId)
                .map(c -> ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.toApiMap(c))))
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("orgUnitId", orgUnitId);
                    List<String> slots = new ArrayList<>(10);
                    for (int i = 0; i < 10; i++) {
                        slots.add(null);
                    }
                    empty.put("slots", slots);
                    empty.put("defaultSlot", 0);
                    return ResponseEntity.ok(ApiResponse.ok(empty));
                });
    }

    @PostMapping("/masterDistCalcCycleConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistCalcCycleSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            if (body == null || body.get("orgUnitId") == null) {
                return ResponseEntity.ok(ApiResponse.fail("orgUnitId가 필요합니다.", "VALIDATION"));
            }
            long orgUnitId = parseRequestLong(body.get("orgUnitId"), "orgUnitId");
            int defaultSlot = parseRequestInt(body.get("defaultSlot"), 0, "defaultSlot");
            @SuppressWarnings("unchecked")
            List<Object> rawSlots = body.get("slots") instanceof List<?> l ? (List<Object>) l : new ArrayList<>();
            List<String> slots = new ArrayList<>();
            for (Object o : rawSlots) {
                slots.add(o != null ? String.valueOf(o).trim() : "");
            }
            MasterDistSettlementCycleConfig saved =
                    masterDistSettlementCycleConfigService.saveForMasterDist(orgUnitId, slots, defaultSlot);
            return ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.toApiMap(saved)));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = "총판 정산주기 저장 중 오류가 발생했습니다. (" + e.getClass().getSimpleName() + ")";
            }
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        }
    }

    /** JSON Map 값이 Integer/Long/Double 등일 때 안전하게 long 변환(예: 11.0 문자열 방지). */
    private static long parseRequestLong(Object raw, String fieldName) {
        if (raw == null) {
            throw new IllegalArgumentException(fieldName + "가 필요합니다.");
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "가 비어 있습니다.");
        }
        try {
            if (s.contains("e") || s.contains("E") || s.indexOf('.') >= 0) {
                return (long) Double.parseDouble(s);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 숫자 형식이 올바르지 않습니다: " + s);
        }
    }

    private static int parseRequestInt(Object raw, int defaultVal, String fieldName) {
        if (raw == null) {
            return defaultVal;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return defaultVal;
        }
        try {
            if (s.contains("e") || s.contains("E") || s.indexOf('.') >= 0) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 숫자 형식이 올바르지 않습니다: " + s);
        }
    }

    @GetMapping("/cycleDefs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleDefs() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listMergedDefinitions()));
    }

    @GetMapping("/schedulePreview")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> schedulePreview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.schedulePreview(fromDate, toDate)));
    }

    @GetMapping("/merchantAutoCounts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> merchantAutoCounts() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.autoMerchantCountByCycle()));
    }

    /** 가맹 정산주기 변경 이력(즉시 적용·다음 정산 후·예약 적용 완료) */
    @GetMapping("/calcCycleChangeHistory")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> calcCycleChangeHistory(
            @RequestParam(required = false) String merchantCode,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                settlementCalcCycleTransitionService.listHistory(merchantCode, limit != null ? limit : 100)));
    }

    /**
     * 본사설정 — 환수/미수금: 총판(MASTER_DIST) 기본 + 가맹 개별 오버라이드(우선).
     */
    @GetMapping("/receivableRecoverySettings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableRecoverySettingsGet() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        String hqDef = receivableRecoveryModeService.hqDefaultMode();
        out.put("defaultMode", hqDef);
        out.put("hqReceivableRecoveryDefaultMode", hqDef);

        List<Map<String, Object>> masterDistOptions = new ArrayList<>();
        for (OrgUnit md : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST)) {
            if (md == null || md.getCode() == null || md.getCode().isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgUnitId", md.getId());
            row.put("compId", md.getCode().trim());
            row.put("compNm", md.getName() != null ? md.getName() : md.getCode().trim());
            String mdMode = settlementSettingRepository.findByOrgUnitId(md.getId())
                    .map(s -> ReceivableRecoveryModeUtil.normalize(s.getReceivableRecoveryMode()))
                    .orElse(hqDef);
            row.put("receivableRecoveryMode", mdMode);
            masterDistOptions.add(row);
        }
        out.put("masterDistOptions", masterDistOptions);

        List<Map<String, Object>> merchantOptions = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MERCHANT)) {
            if (ou == null || ou.getCode() == null || ou.getCode().isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgUnitId", ou.getId());
            row.put("compId", ou.getCode().trim());
            row.put("compNm", ou.getName() != null ? ou.getName() : ou.getCode().trim());
            SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
            String oyn = ss != null && ss.getReceivableRecoveryOverrideYn() != null
                    ? ss.getReceivableRecoveryOverrideYn().trim().toUpperCase(Locale.ROOT) : "N";
            row.put("receivableRecoveryOverrideYn", "Y".equals(oyn) ? "Y" : "N");
            row.put("storedReceivableRecoveryMode", ss != null
                    ? ReceivableRecoveryModeUtil.normalize(ss.getReceivableRecoveryMode()) : hqDef);
            row.put("effectiveReceivableRecoveryMode",
                    receivableRecoveryModeService.resolveEffectiveModeForMerchantOrgUnitId(ou.getId()));
            row.put("inheritedReceivableRecoveryMode",
                    receivableRecoveryModeService.resolveInheritedModeForMerchantOrgUnitId(ou.getId()));
            row.put("masterDistCompId", receivableRecoveryModeService.findMasterDistCompCodeForMerchantOrgUnitId(ou.getId()).orElse(""));
            merchantOptions.add(row);
        }
        out.put("merchantOptions", merchantOptions);

        List<Map<String, Object>> manualMerchants = new ArrayList<>();
        for (Map<String, Object> mo : merchantOptions) {
            if (!"MANUAL".equalsIgnoreCase(String.valueOf(mo.getOrDefault("effectiveReceivableRecoveryMode", "AUTO")))) {
                continue;
            }
            long orgId = ((Number) mo.get("orgUnitId")).longValue();
            SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(orgId).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgUnitId", orgId);
            m.put("compId", mo.get("compId"));
            m.put("compNm", mo.get("compNm"));
            m.put("receivableRecoveryMode", "MANUAL");
            m.put("effectiveSourceKr", receivableManualEffectiveSourceKr(orgId, ss));
            manualMerchants.add(m);
        }
        manualMerchants.sort(Comparator.comparing(o -> String.valueOf(o.getOrDefault("compId", "")).toLowerCase(Locale.ROOT)));
        out.put("manualMerchants", manualMerchants);
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    /** 유효 모드가 수동일 때, 그 수동이 어디 설정에서 왔는지(표시용). */
    private String receivableManualEffectiveSourceKr(long merchantOrgUnitId, SettlementSetting ss) {
        if (ss != null && ReceivableRecoveryModeService.merchantOverrides(ss)) {
            return "가맹 개별";
        }
        if (receivableRecoveryModeService.findMasterDistCompCodeForMerchantOrgUnitId(merchantOrgUnitId).isPresent()) {
            return "총판 설정";
        }
        return "본사 기본";
    }

    @PostMapping("/receivableRecoverySettings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableRecoverySettingsSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        if (body == null) {
            return ResponseEntity.ok(ApiResponse.fail("요청 본문이 없습니다.", "VALIDATION"));
        }
        String compId = body.get("compId") != null ? String.valueOf(body.get("compId")).trim() : "";
        if (compId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("업체 코드(compId)는 필수입니다.", "VALIDATION"));
        }
        OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
        if (ou == null) {
            return ResponseEntity.ok(ApiResponse.fail("업체 코드를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        String scopeRaw = body.get("scope") != null ? String.valueOf(body.get("scope")).trim().toUpperCase(Locale.ROOT) : "";
        boolean masterDistRow = ou.getOrgLevel() == OrgLevel.MASTER_DIST;
        boolean merchantRow = ou.getOrgLevel() == OrgLevel.MERCHANT;
        boolean saveMasterDist = "MASTER_DIST".equals(scopeRaw) || (scopeRaw.isEmpty() && masterDistRow);
        if (!saveMasterDist && !merchantRow && !"MERCHANT".equals(scopeRaw)) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점(MERCHANT) 또는 총판(MASTER_DIST) 코드만 지원합니다.", "VALIDATION"));
        }
        if ("MERCHANT".equals(scopeRaw) && !merchantRow) {
            return ResponseEntity.ok(ApiResponse.fail("scope=MERCHANT 인 경우 가맹점 코드여야 합니다.", "VALIDATION"));
        }
        if ("MASTER_DIST".equals(scopeRaw) && !masterDistRow) {
            return ResponseEntity.ok(ApiResponse.fail("scope=MASTER_DIST 인 경우 총판 코드여야 합니다.", "VALIDATION"));
        }

        SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
        if (ss == null) {
            return ResponseEntity.ok(ApiResponse.fail("해당 조직의 정산설정이 없습니다. 업체 등록·정산설정을 먼저 완료하세요.", "NOT_FOUND"));
        }

        if (saveMasterDist) {
            String mode = body.get("mode") != null ? String.valueOf(body.get("mode")).trim().toUpperCase(Locale.ROOT) : "";
            if (!"AUTO".equals(mode) && !"MANUAL".equals(mode)) {
                return ResponseEntity.ok(ApiResponse.fail("mode는 AUTO 또는 MANUAL 이어야 합니다.", "VALIDATION"));
            }
            String norm = ReceivableRecoveryModeUtil.normalize(mode);
            ss.setReceivableRecoveryMode(norm);
            ss.setReceivableRecoveryOverrideYn("N");
            settlementSettingRepository.save(ss);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", true,
                    "scope", "MASTER_DIST",
                    "compId", compId,
                    "mode", norm)));
        }

        boolean inheritFromMaster = Boolean.TRUE.equals(body.get("inheritFromMaster"))
                || "Y".equalsIgnoreCase(String.valueOf(body.get("inheritFromMaster")));
        if (inheritFromMaster) {
            String inh = receivableRecoveryModeService.resolveInheritedModeForMerchantOrgUnitId(ou.getId());
            ss.setReceivableRecoveryOverrideYn("N");
            ss.setReceivableRecoveryMode(ReceivableRecoveryModeUtil.normalize(inh));
            settlementSettingRepository.save(ss);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", true,
                    "scope", "MERCHANT",
                    "compId", compId,
                    "inheritFromMaster", true,
                    "effectiveMode", receivableRecoveryModeService.resolveEffectiveModeForMerchantOrgUnitId(ou.getId()))));
        }
        String mode = body.get("mode") != null ? String.valueOf(body.get("mode")).trim().toUpperCase(Locale.ROOT) : "";
        if (!"AUTO".equals(mode) && !"MANUAL".equals(mode)) {
            return ResponseEntity.ok(ApiResponse.fail("개별 설정 시 mode는 AUTO 또는 MANUAL 이어야 합니다.", "VALIDATION"));
        }
        String norm = ReceivableRecoveryModeUtil.normalize(mode);
        ss.setReceivableRecoveryOverrideYn("Y");
        ss.setReceivableRecoveryMode(norm);
        settlementSettingRepository.save(ss);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "success", true,
                "scope", "MERCHANT",
                "compId", compId,
                "inheritFromMaster", false,
                "mode", norm)));
    }

    @PostMapping("/cycleDefs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCycleDef(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 등록할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String family = body != null && body.get("family") != null ? String.valueOf(body.get("family")) : "";
            Integer offset = parseIntOrNull(body != null ? body.get("offset") : null);
            String wkKey = body != null && body.get("wkKey") != null ? String.valueOf(body.get("wkKey")) : "";
            String displayLabel = body != null && body.get("displayLabel") != null ? String.valueOf(body.get("displayLabel")) : "";
            String description = body != null && body.get("description") != null ? String.valueOf(body.get("description")) : "";
            Integer sortParsed = parseIntOrNull(body != null ? body.get("sortOrder") : null);
            int sort = sortParsed != null ? sortParsed : 100;
            String activeYn = body != null && body.get("activeYn") != null ? String.valueOf(body.get("activeYn")) : "Y";
            HqSettlementCycleDef saved = hqSettlementCycleAdminService.createCustom(family, offset, wkKey, displayLabel, description, sort, activeYn);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", saved.getId());
            m.put("cycleCode", saved.getCycleCode());
            return ResponseEntity.ok(ApiResponse.ok(m));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PutMapping("/cycleDefs/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCycleDef(@PathVariable long id,
                                                                           @RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 수정할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String displayLabel = body != null && body.get("displayLabel") != null ? String.valueOf(body.get("displayLabel")) : null;
            String description = body != null && body.get("description") != null ? String.valueOf(body.get("description")) : null;
            Integer sortOrder = parseIntOrNull(body != null ? body.get("sortOrder") : null);
            String activeYn = body != null && body.get("activeYn") != null ? String.valueOf(body.get("activeYn")) : null;
            hqSettlementCycleAdminService.updateRow(id, displayLabel, description, sortOrder, activeYn);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /**
     * 표준 정산주기 코드 중 DB에 없는 행만 일괄 INSERT(내장 목록과 동일한 표시·설명·순서·사용=Y).
     */
    @PostMapping("/cycleDefs/seedMissing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedMissingCycleDefs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            int inserted = hqSettlementCycleAdminService.seedMissingStandardCycleDefs();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("inserted", inserted)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @DeleteMapping("/cycleDefs/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCycleDef(@PathVariable long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 삭제할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            hqSettlementCycleAdminService.deleteRow(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 본사 기본 + 총판별 무효·환불 정산 방식(MASTER_DIST tb_settlement_setting) */
    @GetMapping("/voidRefundSettlementModes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voidRefundSettlementModesGet(Authentication auth) {
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        HqLedgerSysSettings s = hqLedgerSysSettingsService.getOrCreate();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("voidSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getVoidSettlementMode()));
        m.put("manualVoidSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getManualVoidSettlementMode()));
        m.put("refundSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getRefundSettlementMode()));
        m.put("forceRefundSettlementMode", VoidRefundSettlementModeUtil.normalize(s.getForceRefundSettlementMode()));
        m.put("receivableRecoveryDefaultMode", ReceivableRecoveryModeUtil.normalize(s.getReceivableRecoveryDefaultMode()));
        List<Map<String, Object>> masterDistOptions = new ArrayList<>();
        for (OrgUnit md : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST)) {
            if (md == null || md.getCode() == null || md.getCode().isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgUnitId", md.getId());
            row.put("compId", md.getCode().trim());
            row.put("compNm", md.getName() != null ? md.getName() : md.getCode().trim());
            settlementSettingRepository.findByOrgUnitId(md.getId()).ifPresent(ss -> {
                row.put("voidSettlementMode", ss.getVoidSettlementMode() != null && !ss.getVoidSettlementMode().isBlank()
                        ? VoidRefundSettlementModeUtil.normalize(ss.getVoidSettlementMode()) : null);
                row.put("manualVoidSettlementMode", ss.getManualVoidSettlementMode() != null && !ss.getManualVoidSettlementMode().isBlank()
                        ? VoidRefundSettlementModeUtil.normalize(ss.getManualVoidSettlementMode()) : null);
                row.put("refundSettlementMode", ss.getRefundSettlementMode() != null && !ss.getRefundSettlementMode().isBlank()
                        ? VoidRefundSettlementModeUtil.normalize(ss.getRefundSettlementMode()) : null);
                row.put("forceRefundSettlementMode", ss.getForceRefundSettlementMode() != null && !ss.getForceRefundSettlementMode().isBlank()
                        ? VoidRefundSettlementModeUtil.normalize(ss.getForceRefundSettlementMode()) : null);
            });
            masterDistOptions.add(row);
        }
        m.put("masterDistOptions", masterDistOptions);
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/voidRefundSettlementModes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voidRefundSettlementModesSave(
            Authentication auth, @RequestBody Map<String, Object> body) {
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 수정할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Map<String, Object> req = body != null ? body : Map.of();
            String compId = req.get("compId") != null ? String.valueOf(req.get("compId")).trim() : "";
            if (!compId.isBlank()) {
                Map<String, Object> savedMd = saveVoidRefundSettlementModesForMasterDist(compId, req);
                return ResponseEntity.ok(ApiResponse.ok(savedMd));
            }
            HqLedgerSysSettings saved = hqLedgerSysSettingsService.updateVoidRefundSettlementModes(req);
            if (req.containsKey("receivableRecoveryDefaultMode")
                    && Boolean.TRUE.equals(req.get("syncReceivableRecoveryDefaultToAllMerchants"))) {
                hqLedgerSysSettingsService.applyReceivableRecoveryDefaultToAllMerchants(
                        String.valueOf(req.get("receivableRecoveryDefaultMode")));
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("voidSettlementMode", VoidRefundSettlementModeUtil.normalize(saved.getVoidSettlementMode()));
            m.put("manualVoidSettlementMode", VoidRefundSettlementModeUtil.normalize(saved.getManualVoidSettlementMode()));
            m.put("refundSettlementMode", VoidRefundSettlementModeUtil.normalize(saved.getRefundSettlementMode()));
            m.put("forceRefundSettlementMode", VoidRefundSettlementModeUtil.normalize(saved.getForceRefundSettlementMode()));
            m.put("receivableRecoveryDefaultMode", ReceivableRecoveryModeUtil.normalize(saved.getReceivableRecoveryDefaultMode()));
            return ResponseEntity.ok(ApiResponse.ok(m));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private Map<String, Object> saveVoidRefundSettlementModesForMasterDist(String compId, Map<String, Object> req) {
        OrgUnit ou = orgUnitRepository.findByCode(compId).orElseThrow(() -> new IllegalArgumentException("업체 코드를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("총판(MASTER_DIST) 코드만 저장할 수 있습니다.");
        }
        SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 조직의 정산설정이 없습니다. 업체 등록을 먼저 완료하세요."));
        if (req.containsKey("voidSettlementMode")) {
            String v = String.valueOf(req.get("voidSettlementMode")).trim();
            ss.setVoidSettlementMode(v.isEmpty() ? null : VoidRefundSettlementModeUtil.normalize(v));
        }
        if (req.containsKey("manualVoidSettlementMode")) {
            String v = String.valueOf(req.get("manualVoidSettlementMode")).trim();
            ss.setManualVoidSettlementMode(v.isEmpty() ? null : VoidRefundSettlementModeUtil.normalize(v));
        }
        if (req.containsKey("refundSettlementMode")) {
            String v = String.valueOf(req.get("refundSettlementMode")).trim();
            ss.setRefundSettlementMode(v.isEmpty() ? null : VoidRefundSettlementModeUtil.normalize(v));
        }
        if (req.containsKey("forceRefundSettlementMode")) {
            String v = String.valueOf(req.get("forceRefundSettlementMode")).trim();
            ss.setForceRefundSettlementMode(v.isEmpty() ? null : VoidRefundSettlementModeUtil.normalize(v));
        }
        ss.setVoidRefundSettlementOverrideYn("N");
        settlementSettingRepository.save(ss);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("scope", "MASTER_DIST");
        out.put("compId", ou.getCode());
        out.put("voidSettlementMode", ss.getVoidSettlementMode() != null ? VoidRefundSettlementModeUtil.normalize(ss.getVoidSettlementMode()) : null);
        out.put("manualVoidSettlementMode", ss.getManualVoidSettlementMode() != null ? VoidRefundSettlementModeUtil.normalize(ss.getManualVoidSettlementMode()) : null);
        out.put("refundSettlementMode", ss.getRefundSettlementMode() != null ? VoidRefundSettlementModeUtil.normalize(ss.getRefundSettlementMode()) : null);
        out.put("forceRefundSettlementMode", ss.getForceRefundSettlementMode() != null ? VoidRefundSettlementModeUtil.normalize(ss.getForceRefundSettlementMode()) : null);
        return out;
    }

    private static Integer parseIntOrNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
