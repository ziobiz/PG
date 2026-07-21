package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqBulkLoginRestriction;
import com.pg.entity.HqBulkOpsPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqBulkLoginRestrictionRepository;
import com.pg.repository.HqBulkOpsPolicyRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.HqBulkOpsModes;
import com.pg.util.OrgUseYnUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 본사 일괄운영관리 — 가맹점사용·URL결제·로그인 제한.
 * 총본사(HEADQUARTERS)는 일괄 정책 대상에서 제외됩니다.
 */
@Service
public class HqBulkOpsService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Set<Long>> LONG_SET = new TypeReference<>() {};

    private final HqBulkOpsPolicyRepository policyRepository;
    private final HqBulkLoginRestrictionRepository loginRestrictionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public HqBulkOpsService(HqBulkOpsPolicyRepository policyRepository,
                            HqBulkLoginRestrictionRepository loginRestrictionRepository,
                            OrgUnitRepository orgUnitRepository,
                            MerchantProfileRepository merchantProfileRepository) {
        this.policyRepository = policyRepository;
        this.loginRestrictionRepository = loginRestrictionRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public Map<String, Object> snapshotForApi() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orgUse", policyToMap(getOrCreatePolicy(HqBulkOpsModes.POLICY_ORG_USE)));
        out.put("urlPay", policyToMap(getOrCreatePolicy(HqBulkOpsModes.POLICY_URL_PAY)));
        out.put("loginRestrictions", listLoginRestrictions());
        return out;
    }

    @Transactional
    public Map<String, Object> applyOrgUseAction(String action, String updatedBy) {
        applyPolicyAction(HqBulkOpsModes.POLICY_ORG_USE, action, updatedBy, this::collectOrgUsePauseSnapshot);
        return policyToMap(getOrCreatePolicy(HqBulkOpsModes.POLICY_ORG_USE));
    }

    @Transactional
    public Map<String, Object> applyUrlPayAction(String action, String updatedBy) {
        applyPolicyAction(HqBulkOpsModes.POLICY_URL_PAY, action, updatedBy, this::collectUrlPayPauseSnapshot);
        return policyToMap(getOrCreatePolicy(HqBulkOpsModes.POLICY_URL_PAY));
    }

    public List<Map<String, Object>> listLoginRestrictions() {
        return loginRestrictionRepository.findByStatusOrderByIdDesc(HqBulkOpsModes.STATUS_ACTIVE).stream()
                .map(this::loginRestrictionToMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveLoginRestriction(Map<String, Object> body, String updatedBy) {
        Long id = longVal(body, "id");
        HqBulkLoginRestriction row = id != null
                ? loginRestrictionRepository.findById(id).orElseGet(HqBulkLoginRestriction::new)
                : new HqBulkLoginRestriction();
        if (id != null) {
            row.setId(id);
        }

        String level = str(body, "targetOrgLevel");
        Long orgUnitId = longVal(body, "targetOrgUnitId");
        String orgCode = str(body, "targetOrgCode");
        if (orgUnitId == null && orgCode != null && !orgCode.isBlank()) {
            orgUnitId = orgUnitRepository.findByCode(orgCode.trim())
                    .or(() -> orgUnitRepository.findByCodeIgnoreCase(orgCode.trim()))
                    .map(OrgUnit::getId)
                    .orElse(null);
            if (orgUnitId == null) {
                throw new IllegalArgumentException("업체코드를 찾을 수 없습니다.");
            }
        }
        if (orgUnitId != null) {
            OrgUnit ou = orgUnitRepository.findById(orgUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("대상 조직을 찾을 수 없습니다."));
            row.setTargetOrgUnitId(ou.getId());
            row.setTargetOrgCode(ou.getCode());
            row.setTargetOrgName(ou.getName());
            row.setTargetOrgLevel(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : level);
        } else if (level != null && !level.isBlank()) {
            row.setTargetOrgLevel(level.trim().toUpperCase(Locale.ROOT));
            row.setTargetOrgUnitId(null);
            row.setTargetOrgCode(null);
            row.setTargetOrgName(orgLevelLabel(row.getTargetOrgLevel()) + " (전체)");
        } else {
            throw new IllegalArgumentException("대상 조직 단계 또는 조직을 지정하세요.");
        }

        String action = str(body, "action");
        if (action != null && !action.isBlank()) {
            applyLoginRestrictionAction(row, action);
        } else {
            row.setMode(HqBulkOpsModes.normalizeMode(str(body, "mode")));
        }
        row.setStatus(HqBulkOpsModes.STATUS_ACTIVE);
        row.setUpdatedBy(updatedBy);
        return loginRestrictionToMap(loginRestrictionRepository.save(row));
    }

    @Transactional
    public void deleteLoginRestriction(Long id) {
        if (id == null) {
            return;
        }
        loginRestrictionRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> releaseLoginRestriction(Long id, String updatedBy) {
        HqBulkLoginRestriction row = loginRestrictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다."));
        row.setMode(HqBulkOpsModes.MODE_NONE);
        row.setPauseSnapshotJson(null);
        row.setStatus(HqBulkOpsModes.STATUS_RELEASED);
        row.setUpdatedBy(updatedBy);
        return loginRestrictionToMap(loginRestrictionRepository.save(row));
    }

    @Transactional
    public Map<String, Object> applyLoginRestrictionAction(Long id, String action, String updatedBy) {
        HqBulkLoginRestriction row = loginRestrictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다."));
        applyLoginRestrictionAction(row, action);
        row.setUpdatedBy(updatedBy);
        row.setStatus(HqBulkOpsModes.STATUS_ACTIVE);
        return loginRestrictionToMap(loginRestrictionRepository.save(row));
    }

    public boolean isHeadquarters(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .orElse(false);
    }

    /** 프로필 use_yn + 일괄운영(ORG_USE) 반영. 총본사는 프로필만. */
    public String resolveEffectiveOrgUseYn(Long orgUnitId) {
        String raw = rawProfileUseYn(orgUnitId);
        if (isHeadquarters(orgUnitId)) {
            return raw;
        }
        return applyGlobalPolicy(getOrCreatePolicy(HqBulkOpsModes.POLICY_ORG_USE), orgUnitId, raw);
    }

    /** web_payment_use_yn + 일괄운영(URL_PAY) 반영. 비가맹·총본사는 프로필만. */
    public String resolveEffectiveWebPaymentUseYn(Long orgUnitId) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null || ou.getOrgLevel() != OrgLevel.MERCHANT || isHeadquarters(orgUnitId)) {
            return rawWebPaymentUseYn(orgUnitId);
        }
        String raw = rawWebPaymentUseYn(orgUnitId);
        return applyGlobalPolicy(getOrCreatePolicy(HqBulkOpsModes.POLICY_URL_PAY), orgUnitId, raw);
    }

    public boolean isWebPaymentAllowed(Long orgUnitId) {
        return "Y".equalsIgnoreCase(resolveEffectiveWebPaymentUseYn(orgUnitId));
    }

    /** 일괄 로그인 제한 — 매칭 ACTIVE 규칙이 있으면 로그인 차단 */
    public boolean isLoginBlockedByBulkOps(Long orgUnitId) {
        if (orgUnitId == null || isHeadquarters(orgUnitId)) {
            return false;
        }
        List<HqBulkLoginRestriction> rules = loginRestrictionRepository
                .findByStatusOrderByIdDesc(HqBulkOpsModes.STATUS_ACTIVE);
        for (HqBulkLoginRestriction rule : rules) {
            if (!matchesLoginRuleScope(orgUnitId, rule)) {
                continue;
            }
            String mode = HqBulkOpsModes.normalizeMode(rule.getMode());
            if (HqBulkOpsModes.MODE_FORCE_N.equals(mode)) {
                return true;
            }
            if (HqBulkOpsModes.MODE_PAUSED.equals(mode)) {
                Set<Long> snap = parseSnapshot(rule.getPauseSnapshotJson());
                if (snap.contains(orgUnitId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void applyLoginRestrictionAction(HqBulkLoginRestriction row, String action) {
        String act = HqBulkOpsModes.normalizeAction(action);
        if (HqBulkOpsModes.ACTION_PAUSE.equals(act)) {
            row.setMode(HqBulkOpsModes.MODE_PAUSED);
            row.setPauseSnapshotJson(writeSnapshot(collectLoginPauseSnapshot(row)));
            return;
        }
        if (HqBulkOpsModes.ACTION_RELEASE.equals(act)) {
            row.setMode(HqBulkOpsModes.MODE_NONE);
            row.setPauseSnapshotJson(null);
            return;
        }
        row.setMode(HqBulkOpsModes.actionToMode(act));
        row.setPauseSnapshotJson(null);
    }

    private Set<Long> collectLoginPauseSnapshot(HqBulkLoginRestriction rule) {
        Set<Long> ids = new HashSet<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (isHeadquarters(ou.getId()) || !matchesLoginRuleScope(ou.getId(), rule)) {
                continue;
            }
            if (wasLoginAllowedBeforeBulk(ou.getId())) {
                ids.add(ou.getId());
            }
        }
        return ids;
    }

    private boolean wasLoginAllowedBeforeBulk(Long orgUnitId) {
        if (OrgUseYnUtil.isLoginBlocked(rawProfileUseYn(orgUnitId))) {
            return false;
        }
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            Long id = cur.get().getId();
            if (isHeadquarters(id)) {
                break;
            }
            if (OrgUseYnUtil.isLoginBlocked(rawProfileUseYn(id))) {
                return false;
            }
            Long pid = cur.get().getParentId();
            if (pid == null) {
                break;
            }
            cur = orgUnitRepository.findById(pid);
        }
        return true;
    }

    private boolean matchesLoginRuleScope(Long orgUnitId, HqBulkLoginRestriction rule) {
        if (rule.getTargetOrgUnitId() != null) {
            if (orgUnitId.equals(rule.getTargetOrgUnitId())) {
                return true;
            }
            return isDescendantOf(orgUnitId, rule.getTargetOrgUnitId());
        }
        String level = rule.getTargetOrgLevel();
        if (level == null || level.isBlank()) {
            return false;
        }
        OrgLevel targetLevel;
        try {
            targetLevel = OrgLevel.valueOf(level.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        Optional<OrgUnit> anchor = findNearestAncestorAtLevel(orgUnitId, targetLevel);
        return anchor.isPresent();
    }

    private Optional<OrgUnit> findNearestAncestorAtLevel(Long orgUnitId, OrgLevel targetLevel) {
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            OrgUnit ou = cur.get();
            if (ou.getOrgLevel() == targetLevel) {
                return Optional.of(ou);
            }
            Long pid = ou.getParentId();
            if (pid == null) {
                break;
            }
            cur = orgUnitRepository.findById(pid);
        }
        return Optional.empty();
    }

    private boolean isDescendantOf(Long orgUnitId, Long ancestorId) {
        if (orgUnitId == null || ancestorId == null || orgUnitId.equals(ancestorId)) {
            return false;
        }
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            Long pid = cur.get().getParentId();
            if (pid == null) {
                return false;
            }
            if (pid.equals(ancestorId)) {
                return true;
            }
            cur = orgUnitRepository.findById(pid);
        }
        return false;
    }

    private String applyGlobalPolicy(HqBulkOpsPolicy policy, Long orgUnitId, String raw) {
        /* orgUnitId: 호출부 시그니처 유지(총본사 제외 등은 상위에서 처리). 실효값은 mode만으로 전원 동일. */
        String mode = HqBulkOpsModes.normalizeMode(policy.getMode());
        return switch (mode) {
            case HqBulkOpsModes.MODE_FORCE_Y -> OrgUseYnUtil.Y;
            case HqBulkOpsModes.MODE_FORCE_N -> OrgUseYnUtil.N;
            case HqBulkOpsModes.MODE_PAUSED -> OrgUseYnUtil.N;
            default -> raw;
        };
    }

    private void applyPolicyAction(String policyType, String action, String updatedBy,
                                   java.util.function.Supplier<Set<Long>> pauseCollector) {
        HqBulkOpsPolicy policy = getOrCreatePolicy(policyType);
        String act = HqBulkOpsModes.normalizeAction(action);
        if (HqBulkOpsModes.ACTION_PAUSE.equals(act)) {
            policy.setMode(HqBulkOpsModes.MODE_PAUSED);
            /* 감사·안내용: 당시 사용(Y) 이던 대상 기록. 실효 차단은 전원(PAUSED→N) */
            policy.setPauseSnapshotJson(writeSnapshot(pauseCollector.get()));
        } else if (HqBulkOpsModes.ACTION_RELEASE.equals(act)) {
            policy.setMode(HqBulkOpsModes.MODE_NONE);
            policy.setPauseSnapshotJson(null);
        } else {
            policy.setMode(HqBulkOpsModes.actionToMode(act));
            policy.setPauseSnapshotJson(null);
        }
        policy.setUpdatedBy(updatedBy);
        policyRepository.save(policy);
    }

    private Set<Long> collectOrgUsePauseSnapshot() {
        Set<Long> ids = new HashSet<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (isHeadquarters(ou.getId())) {
                continue;
            }
            if (OrgUseYnUtil.isServiceAllowed(rawProfileUseYn(ou.getId()))) {
                ids.add(ou.getId());
            }
        }
        return ids;
    }

    private Set<Long> collectUrlPayPauseSnapshot() {
        Set<Long> ids = new HashSet<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (isHeadquarters(ou.getId()) || ou.getOrgLevel() != OrgLevel.MERCHANT) {
                continue;
            }
            if ("Y".equalsIgnoreCase(rawWebPaymentUseYn(ou.getId()))) {
                ids.add(ou.getId());
            }
        }
        return ids;
    }

    private HqBulkOpsPolicy getOrCreatePolicy(String policyType) {
        return policyRepository.findByPolicyType(policyType).orElseGet(() -> {
            HqBulkOpsPolicy row = new HqBulkOpsPolicy();
            row.setId(HqBulkOpsModes.POLICY_ORG_USE.equals(policyType) ? 1L : 2L);
            row.setPolicyType(policyType);
            row.setMode(HqBulkOpsModes.MODE_NONE);
            return policyRepository.save(row);
        });
    }

    private String rawProfileUseYn(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> OrgUseYnUtil.normalize(mp.getUseYn()))
                .orElse(OrgUseYnUtil.Y);
    }

    private String rawWebPaymentUseYn(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> {
                    String w = mp.getWebPaymentUseYn();
                    return w != null && "Y".equalsIgnoreCase(w.trim()) ? "Y" : "N";
                })
                .orElse("N");
    }

    private Map<String, Object> policyToMap(HqBulkOpsPolicy p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("policyType", p.getPolicyType());
        m.put("mode", HqBulkOpsModes.normalizeMode(p.getMode()));
        m.put("modeLabelKey", HqBulkOpsModes.modeToActionLabelKey(p.getMode()));
        m.put("pauseCount", parseSnapshot(p.getPauseSnapshotJson()).size());
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().format(DT) : "");
        m.put("updatedBy", p.getUpdatedBy() != null ? p.getUpdatedBy() : "");
        return m;
    }

    private Map<String, Object> loginRestrictionToMap(HqBulkLoginRestriction r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("targetOrgLevel", r.getTargetOrgLevel());
        m.put("targetOrgLevelLabel", orgLevelLabel(r.getTargetOrgLevel()));
        m.put("targetOrgUnitId", r.getTargetOrgUnitId());
        m.put("targetOrgCode", r.getTargetOrgCode());
        m.put("targetOrgName", r.getTargetOrgName());
        m.put("mode", HqBulkOpsModes.normalizeMode(r.getMode()));
        m.put("modeLabelKey", HqBulkOpsModes.modeToActionLabelKey(r.getMode()));
        m.put("status", r.getStatus());
        m.put("pauseCount", parseSnapshot(r.getPauseSnapshotJson()).size());
        m.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().format(DT) : "");
        m.put("updatedBy", r.getUpdatedBy() != null ? r.getUpdatedBy() : "");
        return m;
    }

    private static String orgLevelLabel(String level) {
        if (level == null || level.isBlank()) {
            return "";
        }
        try {
            return OrgLevel.valueOf(level.trim().toUpperCase(Locale.ROOT)).getNameKo();
        } catch (IllegalArgumentException ex) {
            return level;
        }
    }

    private Set<Long> parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        try {
            Set<Long> s = JSON.readValue(json, LONG_SET);
            return s != null ? s : Set.of();
        } catch (Exception ex) {
            return Set.of();
        }
    }

    private String writeSnapshot(Set<Long> ids) {
        try {
            return JSON.writeValueAsString(ids != null ? ids : Set.of());
        } catch (Exception ex) {
            return "[]";
        }
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || key == null) {
            return null;
        }
        Object v = body.get(key);
        return v != null ? String.valueOf(v).trim() : null;
    }

    private static Long longVal(Map<String, Object> body, String key) {
        String s = str(body, key);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
