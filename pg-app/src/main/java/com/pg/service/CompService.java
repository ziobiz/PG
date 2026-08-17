package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.CompListItemDto;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantProfile;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.entity.SettlementSetting;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.AppUser;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.entity.PgAgency;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.UserRepository;
import com.pg.entity.OrgUnitChangeLog;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.CommissionTierJsonHelper;
import com.pg.util.MerchantPayNotifyUrlRules;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.ReceivableRecoveryModeUtil;
import com.pg.util.RouteNoDisplayUtil;
import com.pg.util.VoidRefundSettlementModeUtil;
import com.pg.util.CardBrandScopeUtil;
import com.pg.util.CurrencyScopeUtil;
import com.pg.util.MerchantPgCredentialUtil;
import com.pg.chatbot.ChatbotCatalogPolicy;
import com.pg.chatbot.ChatbotPromotionShelfMode;
import com.pg.merchantdeploy.MerchantApiDeploymentService;
import com.pg.merchantdeploy.MerchantApiIntegrationChannelService;
import com.pg.util.ChatbotMerchantAdminConstants;
import com.pg.util.SupervisorAssistantConstants;
import com.pg.util.OrgUseYnUtil;
import com.pg.util.ChatbotProductPricingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompExcelImportService compExcelImportService;
    private final OrgUnitChangeAuditService orgUnitChangeAuditService;
    private final PayFollowPolicyService payFollowPolicyService;
    private final HqNotifyTargetService hqNotifyTargetService;
    private final MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService;
    private final SettlementCalcCycleTransitionService settlementCalcCycleTransitionService;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final ChillPayService chillPayService;
    private final MerchantChatbotKbService merchantChatbotKbService;
    private final MerchantChatbotProductService merchantChatbotProductService;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService;
    private final ChatbotPlanProrationService chatbotPlanProrationService;
    private final OrgUserSuspensionService orgUserSuspensionService;
    private final MerchantApiDeploymentService merchantApiDeploymentService;
    private final MerchantApiIntegrationChannelService merchantApiIntegrationChannelService;
    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final MerchantJpayNotifyUrlSyncService merchantJpayNotifyUrlSyncService;
    private final CommissionService commissionService;

    private static LocalTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String t = s.trim();
            if (t.matches("\\d{1,2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm"));
            if (t.matches("\\d{1,2}:\\d{2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm:ss"));
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) { return null; }
    }

    /**
     * 가맹 정산설정: 마감·개시 시각 반영. RT는 둘 다 비움.
     * 수동이거나 D0·실시간·분·시 격자(TM/TH 포함) 주기는 정산개시시간을 저장하지 않는다.
     */
    private void applyMerchantSettlementCloseStartFromForm(SettlementSetting ss, String calcCloseTime, String calcStartTime) {
        String norm = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
        if (SettlementCycleTiming.isRtPerTransactionCode(norm)) {
            ss.setCalcCloseTime(null);
            ss.setCalcStartTime(null);
            return;
        }
        if (parseTime(calcCloseTime) != null) {
            ss.setCalcCloseTime(parseTime(calcCloseTime));
        }
        String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
        if ("MANUAL".equalsIgnoreCase(proc) || !SettlementCycleTiming.isCalcStartTimeApplicableForAuto(norm)) {
            ss.setCalcStartTime(null);
        } else if (parseTime(calcStartTime) != null) {
            ss.setCalcStartTime(parseTime(calcStartTime));
        }
    }

    /**
     * 상위 조직으로의 이동만 허용. 하위 조직으로의 이동은 금지.
     * 예: 대리점은 영업점 밑으로 이동 불가. 가맹점은 총판/본사/지사/대리점/영업점 중 어디든 배치 가능.
     * @param parentId 상위 조직 ID (null이면 검증 스킵)
     * @param childLevel 자식 조직의 OrgLevel
     * @param childId 자식 조직 ID (자기 자신을 parent로 설정하는 것 방지용, 등록 시 null)
     */
    private void validateParentLevel(Long parentId, OrgLevel childLevel, Long childId) {
        if (parentId == null || childLevel == null) return;
        if (childId != null && parentId.equals(childId)) {
            throw new IllegalArgumentException("상위업체로 자기 자신을 선택할 수 없습니다.");
        }
        OrgUnit parent = orgUnitRepository.findById(parentId).orElse(null);
        if (parent == null || parent.getOrgLevel() == null) return;
        int parentCode = parent.getOrgLevel().getCode();
        int childCode = childLevel.getCode();
        if (parentCode > childCode) {
            throw new IllegalArgumentException(
                "하위 조직으로의 이동은 불가합니다. " + parent.getOrgLevel().getNameKo() + "은(는) " + childLevel.getNameKo() + "보다 하위 조직입니다.");
        }
    }

    /** 상위 체인에서 가장 가까운 총판(MASTER_DIST) 조직 ID. 없으면 empty (본사 직속 등). */
    private Optional<Long> findNearestMasterDistAncestorId(Long orgUnitId) {
        if (orgUnitId == null) return Optional.empty();
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) break;
            if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) return Optional.of(ou.getId());
            cur = ou.getParentId();
        }
        return Optional.empty();
    }

    /**
     * candidateParentId 조직이 masterDistOrgId 총판과 같거나, 그 총판의 하위(후손)인지.
     * 즉 동일 총판 산하에서만 상위 변경 허용할 때 사용.
     */
    private boolean isUnderMasterDistScope(Long masterDistOrgId, Long candidateParentId) {
        if (masterDistOrgId == null || candidateParentId == null) return false;
        Long cur = candidateParentId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if (masterDistOrgId.equals(cur)) return true;
            OrgUnit x = orgUnitRepository.findById(cur).orElse(null);
            cur = x != null ? x.getParentId() : null;
        }
        return false;
    }

    private void validateParentNotDescendantOfChild(Long childOrgUnitId, Long newParentId) {
        if (childOrgUnitId == null || newParentId == null) return;
        if (newParentId.equals(childOrgUnitId)) {
            throw new IllegalArgumentException("상위업체로 자기 자신을 선택할 수 없습니다.");
        }
        if (collectDescendantIds(childOrgUnitId).contains(newParentId)) {
            throw new IllegalArgumentException("소속 하위 조직을 상위로 지정할 수 없습니다.");
        }
    }

    /** 기준 화폐 검증: 본사 최대 3종(comma구분), 총판 1종만 */
    private static void validateBaseCurrency(String compDiv, String baseCurrency) {
        if (baseCurrency == null || baseCurrency.trim().isEmpty()) return;
        String val = baseCurrency.trim();
        if ("REGIONAL".equalsIgnoreCase(compDiv)) {
            String[] parts = val.split(",\\s*");
            if (parts.length > 3) throw new IllegalArgumentException("본사는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.");
        } else if ("MASTER_DIST".equalsIgnoreCase(compDiv)) {
            if (val.contains(",")) throw new IllegalArgumentException("총판은 1가지 화폐만 지정할 수 있습니다.");
        }
    }

    /**
     * 총판 기준 화폐는 직계 상위가 본사(REGIONAL)인 경우, 해당 본사가 설정한 기준 화폐(최대 3종) 중 하나만 허용.
     */
    private void validateMasterDistBaseCurrencyAgainstRegionalParent(Long parentOrgUnitId, String chosenCurrency) {
        if (parentOrgUnitId == null || chosenCurrency == null || chosenCurrency.isBlank()) return;
        Optional<OrgUnit> pou = orgUnitRepository.findById(parentOrgUnitId);
        if (pou.isEmpty() || pou.get().getOrgLevel() != OrgLevel.REGIONAL) return;
        Optional<MerchantProfile> pmp = merchantProfileRepository.findByOrgUnitId(parentOrgUnitId);
        if (pmp.isEmpty()) {
            throw new IllegalArgumentException("상위 본사 프로필을 찾을 수 없습니다. 본사 등록 상태를 확인하세요.");
        }
        String pc = pmp.get().getBaseCurrency();
        if (pc == null || pc.trim().isEmpty()) {
            throw new IllegalArgumentException("상위 본사에 기준 화폐가 설정되어 있지 않습니다. 본사에서 기준 화폐를 먼저 설정한 뒤 총판을 등록·수정하세요.");
        }
        String chosen = chosenCurrency.trim();
        boolean ok = false;
        for (String part : pc.split(",\\s*")) {
            if (chosen.equalsIgnoreCase(part.trim())) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("총판 기준 화폐는 상위 본사가 지정한 기준 화폐 중에서만 선택할 수 있습니다.");
        }
    }

    /**
     * 가맹점 기준 화폐는 가장 가까운 상위 조직(총판/본사)에서 설정한 기준 화폐와 일치해야 한다.
     * - 상위가 본사(REGIONAL)면 다중 통화 중 1개 허용
     * - 상위가 총판(MASTER_DIST) 등 단일 통화면 동일 값만 허용
     */
    private void validateMerchantBaseCurrencyAgainstParent(Long parentOrgUnitId, String chosenCurrency) {
        if (parentOrgUnitId == null || chosenCurrency == null || chosenCurrency.isBlank()) return;
        String chosen = chosenCurrency.trim().toUpperCase();
        Long cur = parentOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) break;
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(cur);
            String bc = mp.map(MerchantProfile::getBaseCurrency).orElse("");
            if (bc != null && !bc.trim().isEmpty()) {
                boolean ok = false;
                for (String part : bc.split(",\\s*")) {
                    if (chosen.equalsIgnoreCase(part.trim())) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    throw new IllegalArgumentException("가맹점 기준 화폐는 상위 조직 기준 화폐와 동일해야 합니다. 상위 기준: " + bc);
                }
                return;
            }
            cur = ou.getParentId();
        }
    }

    /**
     * 가맹점 등록·수정 시 기준통화 미입력이면, 상위 조직 체인에서 처음 나오는 기준통화(콤마 목록의 첫 토큰)를 상속합니다.
     * 총판(MASTER_DIST) 단일 통화 정책과 맞춥니다.
     */
    private String resolveInheritedBaseCurrencyForMerchant(Long parentOrgUnitId) {
        if (parentOrgUnitId == null) {
            return null;
        }
        Long cur = parentOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) {
                break;
            }
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(cur);
            String bc = mp.map(MerchantProfile::getBaseCurrency).orElse("");
            if (bc != null && !bc.trim().isEmpty()) {
                String[] parts = bc.split(",\\s*");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    return parts[0].trim();
                }
            }
            cur = ou.getParentId();
        }
        return null;
    }

    private static String nzUpper(String v) {
        return v == null ? "" : v.trim().toUpperCase();
    }

    /**
     * 가맹점 저장 시 통화-정책 정합성 검증:
     * - 본사정책 따름: 선택한 본사 템플릿 통화 == 가맹점 통화
     * - 직접입력: 선택한 차지백 구간정책 통화 == 가맹점 통화
     */
    private void validateMerchantPolicyCurrencyCompatibility(String chosenCurrency,
                                                             String commissionFollowHq,
                                                             String hqPolicyScope,
                                                             String chargebackPolicyId) {
        String cc = nzUpper(chosenCurrency);
        if (cc.isEmpty()) return;
        boolean custom = "N".equalsIgnoreCase(commissionFollowHq != null ? commissionFollowHq.trim() : "");

        Long effectiveChargebackPolicyId = null;
        if (!custom) {
            String srcScope = (hqPolicyScope != null && !hqPolicyScope.trim().isEmpty()) ? hqPolicyScope.trim() : "DEFAULT";
            CommissionPolicy src = commissionPolicyRepository.findByScope(srcScope).orElse(null);
            if (src != null) {
                String policyCur = nzUpper(src.getCurrencyCode());
                if (!policyCur.isEmpty() && !policyCur.equals(cc)) {
                    throw new IllegalArgumentException("선택한 본사 수수료 정책 통화(" + policyCur + ")와 가맹점 기준 화폐(" + cc + ")가 다릅니다.");
                }
            }
        }
        if (chargebackPolicyId != null && !chargebackPolicyId.trim().isEmpty()) {
            try {
                effectiveChargebackPolicyId = Long.parseLong(chargebackPolicyId.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("차지백 구간정책 ID 형식이 올바르지 않습니다.");
            }
        }

        if (effectiveChargebackPolicyId != null) {
            ChargebackFeePolicy cp = chargebackFeePolicyRepository.findById(effectiveChargebackPolicyId).orElse(null);
            if (cp != null) {
                String cpCur = nzUpper(cp.getCurrencyCode());
                if (!cpCur.isEmpty() && !cpCur.equals(cc)) {
                    throw new IllegalArgumentException("차지백 구간정책 통화(" + cpCur + ")와 가맹점 기준 화폐(" + cc + ")가 다릅니다.");
                }
            }
        }
    }

    /** 가맹점은 차지백 구간정책을 수수료정책과 별도로 선택할 수 있어야 하므로 저장 시 별도 반영 */
    private void applyMerchantIndependentChargebackPolicy(String compCode, String chargebackPolicyId) {
        if (compCode == null || compCode.isBlank()) return;
        if (chargebackPolicyId == null) return;
        CommissionPolicy policy = commissionPolicyRepository.findByScope(compCode.trim()).orElseGet(CommissionPolicy::new);
        policy.setScope(compCode.trim());
        String cp = chargebackPolicyId.trim();
        if (cp.isEmpty()) {
            policy.setChargebackPolicyId(null);
        } else {
            try {
                policy.setChargebackPolicyId(Long.parseLong(cp));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("차지백 구간정책 ID 형식이 올바르지 않습니다.");
            }
        }
        commissionPolicyRepository.save(policy);
    }

    /** 업체구분(compDiv) 문자열 → OrgLevel 매핑 (총본사/본사/총판/지사/대리점/영업점/가맹점) */
    private static OrgLevel orgLevelFromCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return OrgLevel.AGENCY;
        return switch (compDiv.toUpperCase()) {
            case "HEADQUARTERS" -> OrgLevel.HEADQUARTERS;
            case "REGIONAL" -> OrgLevel.REGIONAL;
            case "MASTER_DIST" -> OrgLevel.MASTER_DIST;
            case "BRANCH" -> OrgLevel.BRANCH;
            case "AGENCY" -> OrgLevel.AGENCY;
            case "SALES_OFFICE" -> OrgLevel.SALES_OFFICE;
            case "MERCHANT" -> OrgLevel.MERCHANT;
            default -> OrgLevel.AGENCY;
        };
    }

    private static boolean isHeadquartersFixedCode(String compId) {
        String v = compId != null ? compId.trim() : "";
        return "0000000000".equals(v);
    }

    /** 업체구분별 접두사: 본사=10, 총판=20, 지사=30, 대리점=40, 영업점=50, 가맹점=60. 총본사(HQ)는 0000000000 고정. */
    private static String compCodePrefixFromCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return "40";
        return switch (compDiv.toUpperCase()) {
            case "HEADQUARTERS" -> "00";
            case "REGIONAL" -> "10";
            case "MASTER_DIST" -> "20";
            case "BRANCH" -> "30";
            case "AGENCY" -> "40";
            case "SALES_OFFICE" -> "50";
            case "MERCHANT" -> "60";
            default -> "40";
        };
    }

    public CompService(OrgUnitRepository orgUnitRepository, MerchantProfileRepository merchantProfileRepository,
                       SettlementSettingRepository settlementSettingRepository,
                       MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                       MerchantPgBindingRepository merchantPgBindingRepository,
                       PgAgencyRepository pgAgencyRepository,
                       MerchantDefaultProductRepository merchantDefaultProductRepository,
                       MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                       CommissionPolicyRepository commissionPolicyRepository,
                       ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                       DistributionFeeConfigRepository distributionFeeConfigRepository,
                       UserRepository userRepository, PasswordEncoder passwordEncoder,
                       CompExcelImportService compExcelImportService,
                       OrgUnitChangeAuditService orgUnitChangeAuditService,
                       PayFollowPolicyService payFollowPolicyService,
                       HqNotifyTargetService hqNotifyTargetService,
                       MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService,
                       SettlementCalcCycleTransitionService settlementCalcCycleTransitionService,
                       HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                       ChillPayService chillPayService,
                       MerchantChatbotKbService merchantChatbotKbService,
                       MerchantChatbotProductService merchantChatbotProductService,
                       HqChatbotAiSettingsService hqChatbotAiSettingsService,
                       ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService,
                       ChatbotPlanProrationService chatbotPlanProrationService,
                       OrgUserSuspensionService orgUserSuspensionService,
                       @Lazy MerchantApiDeploymentService merchantApiDeploymentService,
                       MerchantApiIntegrationChannelService merchantApiIntegrationChannelService,
                       HqRiskCardPolicyService hqRiskCardPolicyService,
                       MerchantJpayNotifyUrlSyncService merchantJpayNotifyUrlSyncService,
                       @Lazy CommissionService commissionService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.compExcelImportService = compExcelImportService;
        this.orgUnitChangeAuditService = orgUnitChangeAuditService;
        this.payFollowPolicyService = payFollowPolicyService;
        this.hqNotifyTargetService = hqNotifyTargetService;
        this.masterDistSettlementCycleConfigService = masterDistSettlementCycleConfigService;
        this.settlementCalcCycleTransitionService = settlementCalcCycleTransitionService;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.chillPayService = chillPayService;
        this.merchantChatbotKbService = merchantChatbotKbService;
        this.merchantChatbotProductService = merchantChatbotProductService;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotProductMonthlyBillingService = chatbotProductMonthlyBillingService;
        this.chatbotPlanProrationService = chatbotPlanProrationService;
        this.orgUserSuspensionService = orgUserSuspensionService;
        this.merchantApiDeploymentService = merchantApiDeploymentService;
        this.merchantApiIntegrationChannelService = merchantApiIntegrationChannelService;
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.merchantJpayNotifyUrlSyncService = merchantJpayNotifyUrlSyncService;
        this.commissionService = commissionService;
    }

    /** 챗봇관리 — 고객 안내 문구(병합 표시값). 가맹만. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getChatbotKbDisplay(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("compId", ou.getCode());
                            m.putAll(merchantChatbotKbService.effectiveKbForDisplay(ou, mp));
                            putChatbotProductPlanFields(ou.getId(), mp, m);
                            return m;
                        }));
    }

    private void putChatbotProductPlanFields(Long merchantOrgUnitId, MerchantProfile mp, Map<String, Object> target) {
        String chatYn = mp.getChatbotPaymentUseYn() != null ? mp.getChatbotPaymentUseYn().trim() : "N";
        target.put("chatbotPaymentUseYn", chatYn);
        String holdYn = mp.getChatbotCommerceHoldYn() != null ? mp.getChatbotCommerceHoldYn().trim() : "N";
        target.put("chatbotCommerceHoldYn", "Y".equalsIgnoreCase(holdYn) ? "Y" : "N");
        Integer slotRaw = mp.getChatbotProductSlotLimit();
        target.put("chatbotProductSlotLimit", slotRaw != null ? slotRaw : "");
        Integer slotPend = mp.getChatbotProductSlotLimitPending();
        target.put("chatbotProductSlotLimitPending", slotPend != null ? slotPend : "");
        target.put("chatbotProductSlotPendingApplyYm",
                mp.getChatbotProductSlotPendingApplyYm() != null ? mp.getChatbotProductSlotPendingApplyYm().trim() : "");
        long reg = merchantChatbotProductService.countProductsForMerchant(merchantOrgUnitId);
        target.put("chatbotProductRegisteredCount", reg);
        long active = merchantChatbotProductService.countSaleActiveProductsForMerchant(merchantOrgUnitId);
        target.put("chatbotProductSaleActiveCount", active);
        int saleCapEff = merchantChatbotProductService.getEffectiveChatbotProductSlotCap(merchantOrgUnitId);
        target.put("chatbotProductSlotCapEffective", saleCapEff);
        int regCapEff = merchantChatbotProductService.getEffectiveRegistrationCap(merchantOrgUnitId);
        target.put("chatbotProductRegistrationCapEffective", regCapEff);
        if (saleCapEff > 0) {
            target.put("chatbotProductSlotsRemaining", Math.max(0L, (long) regCapEff - reg));
            target.put("chatbotProductSaleActiveRemaining", Math.max(0L, (long) saleCapEff - active));
        } else {
            target.put("chatbotProductSlotsRemaining", "");
            target.put("chatbotProductSaleActiveRemaining", "");
        }
        target.put("chatbotCatalogListingEnabled", mp.getChatbotCatalogListingEnabled() != null ? mp.getChatbotCatalogListingEnabled() : "");
        LinkedHashSet<String> effLt = merchantChatbotProductService.resolveEffectiveListingTypeCodes(merchantOrgUnitId);
        target.put("chatbotEffectiveListingTypesCsv", ChatbotCatalogPolicy.joinListingCsv(effLt));
        target.put("chatbotEffectiveMaxProductImages", merchantChatbotProductService.getEffectiveMaxProductImages(merchantOrgUnitId));
        ChatbotPromotionShelfMode psm = ChatbotPromotionShelfMode.resolveStored(mp.getChatbotPromotionShelfMode());
        target.put("chatbotPromotionShelfMode", psm.name());
        target.put("chatbotPromotionRotateSeconds",
                ChatbotPromotionShelfMode.normalizeRotateSeconds(mp.getChatbotPromotionRotateSeconds()));
        Map<String, Object> hqCfg = hqChatbotAiSettingsService.rawConfigForServerUse();
        String billCcy = chatbotProductMonthlyBillingService.resolveChatbotMonthlyBillingCurrency(merchantOrgUnitId);
        if (!ChatbotProductPricingUtil.isSupportedBillingCurrency(billCcy)) {
            String fromHq = ChatbotProductPricingUtil.firstSupportedCurrencyWithAnyNonZeroSlotFee(hqCfg);
            if (ChatbotProductPricingUtil.isSupportedBillingCurrency(fromHq)) {
                billCcy = fromHq;
            } else {
                billCcy = "KRW";
            }
        }
        target.put("chatbotPlanBillingCurrency", billCcy != null ? billCcy : "");
        Integer curSlot0 = mp.getChatbotProductSlotLimit();
        if (billCcy != null && ChatbotProductPricingUtil.isSupportedBillingCurrency(billCcy)
                && curSlot0 != null && curSlot0 > 0 && ChatbotProductPricingUtil.isAllowedSlot(curSlot0)) {
            BigDecimal curFee = ChatbotProductPricingUtil.monthlyFeeForSlotAndCurrency(hqCfg, curSlot0, billCcy);
            target.put("chatbotPlanMonthlyFee", curFee != null ? curFee.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        } else {
            target.put("chatbotPlanMonthlyFee", BigDecimal.ZERO);
        }
        Map<String, BigDecimal> feesBySlot = new LinkedHashMap<>();
        if (billCcy != null && ChatbotProductPricingUtil.isSupportedBillingCurrency(billCcy)) {
            for (Integer s : ChatbotProductPricingUtil.ALLOWED_SLOTS) {
                BigDecimal f = ChatbotProductPricingUtil.monthlyFeeForSlotAndCurrency(hqCfg, s, billCcy);
                feesBySlot.put(String.valueOf(s), f != null ? f.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            }
        } else {
            for (Integer s : ChatbotProductPricingUtil.ALLOWED_SLOTS) {
                feesBySlot.put(String.valueOf(s), BigDecimal.ZERO);
            }
        }
        target.put("chatbotPlanFeesBySlot", feesBySlot);
    }

    /**
     * 총본사·본사·총판 등 상위 조직: 산하 가맹점 중 챗봇결제 사용(Y) 가맹점만 — 챗봇 기본 안내(표시 병합값) 목록.
     * 가맹(MERCHANT) 조직은 빈 페이지를 반환하며, 호출은 컨트롤러에서 막는 것이 좋습니다.
     */
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> searchChatbotKbMerchantsPage(
            boolean viewerIsAdmin,
            String viewerCompCode,
            OrgLevel viewerOrgLevel,
            String searchCompId,
            String searchCompNm,
            int page,
            int size) {
        PageResult<Map<String, Object>> out = new PageResult<>();
        int p = Math.max(page, 1);
        int sz = Math.min(Math.max(size, 1), 100);
        if (!viewerIsAdmin && viewerOrgLevel == OrgLevel.MERCHANT) {
            out.setList(new ArrayList<>());
            out.setPage(p);
            out.setSize(sz);
            out.setTotalElements(0);
            out.setTotalPages(1);
            return out;
        }
        String sid = searchCompId != null ? searchCompId.trim().toLowerCase(Locale.ROOT) : "";
        String snm = searchCompNm != null ? searchCompNm.trim().toLowerCase(Locale.ROOT) : "";
        String scope = viewerCompCode != null ? viewerCompCode.trim() : "";
        List<OrgUnit> merchants = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MERCHANT);
        List<AbstractMap.SimpleEntry<OrgUnit, MerchantProfile>> paired = new ArrayList<>();
        for (OrgUnit ou : merchants) {
            if (ou == null || ou.getCode() == null) {
                continue;
            }
            String code = ou.getCode().trim();
            if (!viewerIsAdmin) {
                if (scope.isEmpty() || !isTargetUnderViewerOrg(scope, code)) {
                    continue;
                }
            }
            if (!sid.isEmpty() && !code.toLowerCase(Locale.ROOT).contains(sid)) {
                continue;
            }
            String nm = ou.getName() != null ? ou.getName().trim() : "";
            if (!snm.isEmpty() && !nm.toLowerCase(Locale.ROOT).contains(snm)) {
                continue;
            }
            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.getId());
            if (mpOpt.isEmpty()) {
                continue;
            }
            MerchantProfile mp = mpOpt.get();
            String chatYn = mp.getChatbotPaymentUseYn();
            if (chatYn == null || !"Y".equalsIgnoreCase(chatYn.trim())) {
                continue;
            }
            paired.add(new AbstractMap.SimpleEntry<>(ou, mp));
        }
        long total = paired.size();
        int totalPages = sz > 0 ? (int) Math.max(1L, (total + sz - 1) / sz) : 1;
        int from = (int) Math.min((long) (p - 1) * sz, total);
        int to = (int) Math.min((long) from + sz, total);
        List<Map<String, Object>> pageRows = new ArrayList<>();
        for (int i = from; i < to; i++) {
            AbstractMap.SimpleEntry<OrgUnit, MerchantProfile> e = paired.get(i);
            OrgUnit ou = e.getKey();
            MerchantProfile mp = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("compId", ou.getCode());
            row.put("compNm", ou.getName() != null ? ou.getName() : "");
            row.putAll(merchantChatbotKbService.effectiveKbForDisplay(ou, mp));
            putChatbotProductPlanFields(ou.getId(), mp, row);
            pageRows.add(row);
        }
        out.setList(pageRows);
        out.setPage(p);
        out.setSize(sz);
        out.setTotalElements(total);
        out.setTotalPages(totalPages);
        return out;
    }

    @Transactional
    public void saveMerchantChatbotKb(String compId,
                                      String companyNm, String addr, String tel, String email, String contactNm,
                                      String intro, String productDesc,
                                      String chatbotProductSlotLimitParam,
                                      String chatbotProductSlotPlanUseSplitParam,
                                      String chatbotProductSlotLimitNextParam,
                                      String chatbotOperationModeParam,
                                      String chatbotKbWelcomeHintParam,
                                      String chatbotReservationSlotMinutesParam,
                                      String chatbotReservationZoneIdParam,
                                      String chatbotCatalogListingEnabledParam,
                                      String chatbotMerchantVerticalParam,
                                      String chatbotMerchantVerticalNotesParam,
                                      String chatbotOrderSheetUiJsonParam) {
        OrgUnit ou = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 챗봇 기본안내를 저장할 수 있습니다.");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        MerchantAuditSnapshot snap = MerchantAuditSnapshot.capture(ou, mp, resolveChatbotAdminUsername(mp));
        merchantChatbotKbService.applyUserInput(mp, companyNm, addr, tel, email, contactNm, intro, productDesc);
        merchantChatbotKbService.applyOperationMode(mp, chatbotOperationModeParam);
        merchantChatbotKbService.applyWelcomeHint(mp, chatbotKbWelcomeHintParam);
        merchantChatbotKbService.applyReservationSettings(mp, chatbotReservationSlotMinutesParam, chatbotReservationZoneIdParam);
        merchantChatbotKbService.applyCatalogListingEnabled(mp, chatbotCatalogListingEnabledParam);
        merchantChatbotKbService.applyMerchantVertical(mp, chatbotMerchantVerticalParam, chatbotMerchantVerticalNotesParam);
        merchantChatbotKbService.applyOrderSheetUiJson(mp, chatbotOrderSheetUiJsonParam);
        boolean splitPlan = "Y".equalsIgnoreCase(
                chatbotProductSlotPlanUseSplitParam != null ? chatbotProductSlotPlanUseSplitParam.trim() : "");
        if (splitPlan) {
            applyChatbotProductSlotSplitFromChatbotKbSave(ou, mp, chatbotProductSlotLimitParam, chatbotProductSlotLimitNextParam);
        } else {
            applyChatbotProductSlotLimitFromChatbotKbSave(ou, mp, chatbotProductSlotLimitParam);
        }
        merchantProfileRepository.save(mp);
        persistMerchantAuditDiff(snap, ou, mp, false);
    }

    /**
     * 총본사·본사·총판 등: 산하 가맹의 챗봇 「운영 보류」(상품·주문·예약 차단, 문의 채팅 유지).
     * 챗봇결제가 활성화된 가맹에만 적용합니다.
     */
    @Transactional
    public void saveChatbotCommerceHold(boolean viewerIsAdmin,
                                       String viewerCompCode,
                                       OrgLevel viewerOrgLevel,
                                       String targetCompId,
                                       boolean hold) {
        if (!viewerIsAdmin && viewerOrgLevel == OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점 계정에서는 산하 운영 보류 설정을 변경할 수 없습니다.");
        }
        String tid = targetCompId != null ? targetCompId.trim() : "";
        if (tid.isEmpty()) {
            throw new IllegalArgumentException("가맹점 코드가 필요합니다.");
        }
        OrgUnit mer = orgUnitRepository.findByCode(tid)
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다."));
        if (!viewerIsAdmin) {
            String scope = viewerCompCode != null ? viewerCompCode.trim() : "";
            if (scope.isEmpty() || !isTargetUnderViewerOrg(scope, tid)) {
                throw new IllegalArgumentException("해당 업체를 관리할 수 없습니다.");
            }
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(mer.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        if (mp.getChatbotPaymentUseYn() == null || !"Y".equalsIgnoreCase(mp.getChatbotPaymentUseYn().trim())) {
            throw new IllegalArgumentException("챗봇결제가 활성화된 가맹점만 운영 보류를 적용할 수 있습니다.");
        }
        mp.setChatbotCommerceHoldYn(hold ? "Y" : "N");
        merchantProfileRepository.save(mp);
    }

    /**
     * 챗봇 기본설정 저장 API 전용 — 파라미터가 들어왔을 때만 반영합니다(구 클라이언트 호환).
     * 빈 문자열/null 은 무제한(건수 미지정).
     * 업그레이드: 즉시 반영 + 차액 미수금. 다운그레이드: 당월 유지, 다음 달(서울)부터 pending 적용.
     */
    private void applyChatbotProductSlotLimitFromChatbotKbSave(OrgUnit ou, MerchantProfile mp, String chatbotProductSlotLimitParam) {
        if (chatbotProductSlotLimitParam == null) {
            return;
        }
        String payYn = mp.getChatbotPaymentUseYn();
        if (payYn == null || !"Y".equalsIgnoreCase(payYn.trim())) {
            return;
        }
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        Integer previousSlot = mp.getChatbotProductSlotLimit();
        String raw = chatbotProductSlotLimitParam.trim();
        if (raw.isEmpty()) {
            mp.setChatbotProductSlotLimit(null);
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("등록 가능 플랜(건수) 값이 올바르지 않습니다.");
        }
        if (slot <= 0) {
            mp.setChatbotProductSlotLimit(null);
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        assertMerchantFitsChatbotProductSlotPlan(ou, slot);

        boolean hadCurrent = previousSlot != null && previousSlot > 0;
        if (!hadCurrent) {
            mp.setChatbotProductSlotLimit(slot);
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        if (slot == previousSlot) {
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        if (slot > previousSlot) {
            mp.setChatbotProductSlotLimit(slot);
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            chatbotPlanProrationService.postUpgradeDeltaIfNeeded(ou.getCode(), mp, previousSlot, slot);
            return;
        }
        // 다운그레이드: 당월 한도 유지, 다음 달(서울)부터 적용
        mp.setChatbotProductSlotLimitPending(slot);
        mp.setChatbotProductSlotPendingApplyYm(YearMonth.now(seoul).plusMonths(1).toString());
    }

    private void assertMerchantFitsChatbotProductSlotPlan(OrgUnit ou, int slot) {
        if (!ChatbotProductPricingUtil.isAllowedSlot(slot)) {
            throw new IllegalArgumentException(
                    "챗봇 상품 등록 한도는 " + ChatbotProductPricingUtil.ALLOWED_SLOTS + " 중 하나만 선택할 수 있습니다.");
        }
        long pc = merchantChatbotProductService.countProductsForMerchant(ou.getId());
        long activeY = merchantChatbotProductService.countSaleActiveProductsForMerchant(ou.getId());
        int extra = ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS;
        int regMax = slot + extra;
        if (pc > regMax) {
            throw new IllegalArgumentException(
                    "등록된 상품이 " + pc + "건으로, 선택한 플랜 기준 최대 등록(" + regMax + "건, 판매 활성 "
                            + slot + "+미판매 보관 " + extra + ")을 초과합니다. 상품을 삭제한 뒤 변경하세요.");
        }
        if (activeY > slot) {
            throw new IllegalArgumentException(
                    "판매 활성 상품이 " + activeY + "건인데 선택한 플랜의 판매 활성 상한은 " + slot
                            + "건입니다. 상품관리에서 판매 활성(사용)을 줄인 뒤 변경하세요.");
        }
    }

    /**
     * 챗봇 기본설정(플랜) 분할 저장: 즉시 상향(잔여일 차액 미수금)과 다음 플랜 예약(익월 적용, 상·하향 공통)을 분리.
     */
    private void applyChatbotProductSlotSplitFromChatbotKbSave(
            OrgUnit ou, MerchantProfile mp, String immediateRaw, String nextRaw) {
        String payYn = mp.getChatbotPaymentUseYn();
        if (payYn == null || !"Y".equalsIgnoreCase(payYn.trim())) {
            return;
        }
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        if (immediateRaw != null && !immediateRaw.isBlank()) {
            String ir = immediateRaw.trim();
            if ("__UNLIMITED__".equalsIgnoreCase(ir)) {
                mp.setChatbotProductSlotLimit(null);
                mp.setChatbotProductSlotLimitPending(null);
                mp.setChatbotProductSlotPendingApplyYm(null);
            } else {
                int imm;
                try {
                    imm = Integer.parseInt(ir);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("즉시 플랜(건수) 값이 올바르지 않습니다.");
                }
                if (imm <= 0) {
                    throw new IllegalArgumentException("즉시 변경은 양의 정수 건수 또는 무제한 항목만 선택할 수 있습니다.");
                }
                assertMerchantFitsChatbotProductSlotPlan(ou, imm);
                Integer cur = mp.getChatbotProductSlotLimit();
                boolean hadFinite = cur != null && cur > 0;
                if (hadFinite) {
                    if (imm <= cur) {
                        throw new IllegalArgumentException(
                                "즉시 적용은 상위 플랜(건수 증가)만 가능합니다. 감소·동일 반영은 「다음 플랜(예약)」에서 설정하세요.");
                    }
                    int oldCap = cur;
                    mp.setChatbotProductSlotLimit(imm);
                    mp.setChatbotProductSlotLimitPending(null);
                    mp.setChatbotProductSlotPendingApplyYm(null);
                    chatbotPlanProrationService.postUpgradeDeltaIfNeeded(ou.getCode(), mp, oldCap, imm);
                } else {
                    mp.setChatbotProductSlotLimit(imm);
                    mp.setChatbotProductSlotLimitPending(null);
                    mp.setChatbotProductSlotPendingApplyYm(null);
                    chatbotPlanProrationService.postUpgradeDeltaIfNeeded(ou.getCode(), mp, null, imm);
                }
            }
        }
        if (nextRaw == null) {
            return;
        }
        String nr = nextRaw.trim();
        Integer currentSlot = mp.getChatbotProductSlotLimit();
        if (nr.isEmpty()) {
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        int nxt;
        try {
            nxt = Integer.parseInt(nr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("다음 플랜(예약) 건수 값이 올바르지 않습니다.");
        }
        if (nxt <= 0) {
            throw new IllegalArgumentException("다음 플랜(예약)은 양의 정수 건수만 지정할 수 있습니다.");
        }
        assertMerchantFitsChatbotProductSlotPlan(ou, nxt);
        boolean currentFinite = currentSlot != null && currentSlot > 0;
        if (currentFinite && nxt == currentSlot) {
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            return;
        }
        mp.setChatbotProductSlotLimitPending(nxt);
        mp.setChatbotProductSlotPendingApplyYm(YearMonth.now(seoul).plusMonths(1).toString());
    }

    @Transactional(readOnly = true)
    public String suggestChatbotKbDraft(String compId, String kind) throws Exception {
        OrgUnit ou = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 초안 생성이 가능합니다.");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        if (kind != null && "product".equalsIgnoreCase(kind.trim())) {
            return merchantChatbotKbService.suggestProductDraft(ou, mp);
        }
        if (kind != null && ("welcome".equalsIgnoreCase(kind.trim()) || "welcomeHint".equalsIgnoreCase(kind.trim()))) {
            return merchantChatbotKbService.suggestWelcomeDraft(ou, mp);
        }
        return merchantChatbotKbService.suggestIntroDraft(ou, mp);
    }

    private static String resolveActorUsernameFallback() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AppUser u) {
            return u.getUsername() != null ? u.getUsername().trim() : "";
        }
        return "";
    }

    /** 요청값이 있을 때만 가맹점 결제 후속조치 플래그 반영 후 MERCHANT 단계 상한으로 클램프. */
    private void mergeMerchantPayFollowFromRequest(MerchantProfile mp,
            String payFollowMerchantUseYn,
            String payFollowAutoVoidYn,
            String payFollowEmailVoidYn,
            String payFollowAutoRefundYn,
            String payFollowForceRefundYn) {
        if (mp == null) {
            return;
        }
        if (payFollowMerchantUseYn != null && !payFollowMerchantUseYn.isBlank()) {
            mp.setPayFollowMerchantUseYn("Y".equalsIgnoreCase(payFollowMerchantUseYn.trim()) ? "Y" : "N");
        }
        if (payFollowAutoVoidYn != null && !payFollowAutoVoidYn.isBlank()) {
            mp.setPayFollowAutoVoidYn("Y".equalsIgnoreCase(payFollowAutoVoidYn.trim()) ? "Y" : "N");
        }
        if (payFollowEmailVoidYn != null && !payFollowEmailVoidYn.isBlank()) {
            mp.setPayFollowEmailVoidYn("Y".equalsIgnoreCase(payFollowEmailVoidYn.trim()) ? "Y" : "N");
        }
        if (payFollowAutoRefundYn != null && !payFollowAutoRefundYn.isBlank()) {
            mp.setPayFollowAutoRefundYn("Y".equalsIgnoreCase(payFollowAutoRefundYn.trim()) ? "Y" : "N");
        }
        if (payFollowForceRefundYn != null && !payFollowForceRefundYn.isBlank()) {
            mp.setPayFollowForceRefundYn("Y".equalsIgnoreCase(payFollowForceRefundYn.trim()) ? "Y" : "N");
        }
        payFollowPolicyService.clampMerchantPayFollowToLevelCeiling(mp);
    }

    private void mergeMerchantCardRiskIfAny(MerchantProfile mp,
                                            String cardRiskPolicyMode,
                                            String cardRiskTier1Hours, String cardRiskTier1Min,
                                            String cardRiskTier2Hours, String cardRiskTier2Min,
                                            String cardRiskTier3Hours, String cardRiskTier3Min,
                                            String cardRiskTier4Hours, String cardRiskTier4Min,
                                            String cardRiskAutoBlacklistTier) {
        mergeMerchantCardRiskIfAny(mp, cardRiskPolicyMode,
                cardRiskTier1Hours, cardRiskTier1Min,
                cardRiskTier2Hours, cardRiskTier2Min,
                cardRiskTier3Hours, cardRiskTier3Min,
                cardRiskTier4Hours, cardRiskTier4Min,
                cardRiskAutoBlacklistTier,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private void mergeMerchantCardRiskIfAny(MerchantProfile mp,
                                            String cardRiskPolicyMode,
                                            String cardRiskTier1Hours, String cardRiskTier1Min,
                                            String cardRiskTier2Hours, String cardRiskTier2Min,
                                            String cardRiskTier3Hours, String cardRiskTier3Min,
                                            String cardRiskTier4Hours, String cardRiskTier4Min,
                                            String cardRiskAutoBlacklistTier,
                                            String cardRiskPresaleMode,
                                            String cardRiskPresaleBuyerMismatchYn,
                                            String cardRiskPresaleHolderNameYn,
                                            String cardRiskPresalePhoneInvalidYn,
                                            String cardRiskPresaleEmailInvalidYn,
                                            String cardRiskPresaleVelocityCardYn,
                                            String cardRiskPresaleVelocityEmailYn,
                                            String cardRiskPresaleVelocityIpYn,
                                            String cardRiskPresaleVelCardWinMin,
                                            String cardRiskPresaleVelCardMax,
                                            String cardRiskPresaleVelEmailWinMin,
                                            String cardRiskPresaleVelEmailMax,
                                            String cardRiskPresaleVelIpWinMin,
                                            String cardRiskPresaleVelIpMax) {
        if (mp == null) {
            return;
        }
        boolean anyTrigger = cardRiskPolicyMode != null || cardRiskTier1Hours != null || cardRiskTier1Min != null
                || cardRiskTier2Hours != null || cardRiskTier2Min != null
                || cardRiskTier3Hours != null || cardRiskTier3Min != null
                || cardRiskTier4Hours != null || cardRiskTier4Min != null
                || cardRiskAutoBlacklistTier != null;
        boolean anyPresale = cardRiskPresaleMode != null
                || cardRiskPresaleBuyerMismatchYn != null || cardRiskPresaleHolderNameYn != null
                || cardRiskPresalePhoneInvalidYn != null || cardRiskPresaleEmailInvalidYn != null
                || cardRiskPresaleVelocityCardYn != null || cardRiskPresaleVelocityEmailYn != null
                || cardRiskPresaleVelocityIpYn != null
                || cardRiskPresaleVelCardWinMin != null || cardRiskPresaleVelCardMax != null
                || cardRiskPresaleVelEmailWinMin != null || cardRiskPresaleVelEmailMax != null
                || cardRiskPresaleVelIpWinMin != null || cardRiskPresaleVelIpMax != null;
        if (!anyTrigger && !anyPresale) {
            return;
        }
        Map<String, String> fields = new HashMap<>();
        if (cardRiskPolicyMode != null) {
            fields.put("cardRiskPolicyMode", cardRiskPolicyMode);
        }
        if (cardRiskTier1Hours != null) {
            fields.put("cardRiskTier1Hours", cardRiskTier1Hours);
        }
        if (cardRiskTier1Min != null) {
            fields.put("cardRiskTier1Min", cardRiskTier1Min);
        }
        if (cardRiskTier2Hours != null) {
            fields.put("cardRiskTier2Hours", cardRiskTier2Hours);
        }
        if (cardRiskTier2Min != null) {
            fields.put("cardRiskTier2Min", cardRiskTier2Min);
        }
        if (cardRiskTier3Hours != null) {
            fields.put("cardRiskTier3Hours", cardRiskTier3Hours);
        }
        if (cardRiskTier3Min != null) {
            fields.put("cardRiskTier3Min", cardRiskTier3Min);
        }
        if (cardRiskTier4Hours != null) {
            fields.put("cardRiskTier4Hours", cardRiskTier4Hours);
        }
        if (cardRiskTier4Min != null) {
            fields.put("cardRiskTier4Min", cardRiskTier4Min);
        }
        if (cardRiskAutoBlacklistTier != null) {
            fields.put("cardRiskAutoBlacklistTier", cardRiskAutoBlacklistTier);
        }
        if (anyTrigger) {
            hqRiskCardPolicyService.applyMerchantCardRiskFromRequest(mp, fields);
        }
        if (anyPresale) {
            Map<String, String> pFields = new HashMap<>();
            if (cardRiskPresaleMode != null) {
                pFields.put("cardRiskPresaleMode", cardRiskPresaleMode);
            }
            if (cardRiskPresaleBuyerMismatchYn != null) {
                pFields.put("cardRiskPresaleBuyerMismatchYn", cardRiskPresaleBuyerMismatchYn);
            }
            if (cardRiskPresaleHolderNameYn != null) {
                pFields.put("cardRiskPresaleHolderNameYn", cardRiskPresaleHolderNameYn);
            }
            if (cardRiskPresalePhoneInvalidYn != null) {
                pFields.put("cardRiskPresalePhoneInvalidYn", cardRiskPresalePhoneInvalidYn);
            }
            if (cardRiskPresaleEmailInvalidYn != null) {
                pFields.put("cardRiskPresaleEmailInvalidYn", cardRiskPresaleEmailInvalidYn);
            }
            if (cardRiskPresaleVelocityCardYn != null) {
                pFields.put("cardRiskPresaleVelocityCardYn", cardRiskPresaleVelocityCardYn);
            }
            if (cardRiskPresaleVelocityEmailYn != null) {
                pFields.put("cardRiskPresaleVelocityEmailYn", cardRiskPresaleVelocityEmailYn);
            }
            if (cardRiskPresaleVelocityIpYn != null) {
                pFields.put("cardRiskPresaleVelocityIpYn", cardRiskPresaleVelocityIpYn);
            }
            if (cardRiskPresaleVelCardWinMin != null) {
                pFields.put("cardRiskPresaleVelCardWinMin", cardRiskPresaleVelCardWinMin);
            }
            if (cardRiskPresaleVelCardMax != null) {
                pFields.put("cardRiskPresaleVelCardMax", cardRiskPresaleVelCardMax);
            }
            if (cardRiskPresaleVelEmailWinMin != null) {
                pFields.put("cardRiskPresaleVelEmailWinMin", cardRiskPresaleVelEmailWinMin);
            }
            if (cardRiskPresaleVelEmailMax != null) {
                pFields.put("cardRiskPresaleVelEmailMax", cardRiskPresaleVelEmailMax);
            }
            if (cardRiskPresaleVelIpWinMin != null) {
                pFields.put("cardRiskPresaleVelIpWinMin", cardRiskPresaleVelIpWinMin);
            }
            if (cardRiskPresaleVelIpMax != null) {
                pFields.put("cardRiskPresaleVelIpMax", cardRiskPresaleVelIpMax);
            }
            hqRiskCardPolicyService.applyMerchantPresaleRiskFromRequest(mp, pFields);
        }
    }

    /** scopeCompId: 로그인 사용자의 업체코드(본인 org만 조회, 업체정보조회용) */
    public PageResult<Map<String, Object>> search(String compId, String compNm, int page, int size, String scopeCompId) {
        return search(compId, compNm, null, null, null, null, null, null, null, null, page, size, scopeCompId, false, false);
    }

    /**
     * 업체관리 검색 - 확장 파라미터.
     * @param scopeCompId 로그인 사용자 업체코드 등
     * @param scopeSubtreeBelowLoginOrg true면 본인 조직 행은 제외하고 직·간접 하위만 목록에 포함(업체관리 트리용)
     * @param includeLoginOrgInScope true이면 scopeSubtreeBelowLoginOrg 시 로그인 조직 본인 행도 포함(상위업체 검색용)
     */
    public PageResult<Map<String, Object>> search(String compId, String compNm,
            String compDiv, String useYn, String payHoldYn, String ceoNm, String terminalId, String ceoMobile, String regNo, Boolean includeSub,
            int page, int size, String scopeCompId, boolean scopeSubtreeBelowLoginOrg, boolean includeLoginOrgInScope) {
        if (scopeSubtreeBelowLoginOrg && (scopeCompId == null || scopeCompId.trim().isEmpty())) {
            PageResult<Map<String, Object>> empty = new PageResult<>();
            empty.setList(new ArrayList<>());
            empty.setPage(page);
            empty.setSize(size);
            empty.setTotalElements(0);
            empty.setTotalPages(1);
            return empty;
        }
        String cId = (compId != null && !compId.trim().isEmpty()) ? compId.trim() : null;
        String cNm = (compNm != null && !compNm.trim().isEmpty()) ? compNm.trim() : null;
        String cDiv = (compDiv != null && !compDiv.trim().isEmpty()) ? compDiv.trim() : null;
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, OrgUnit> allById = all.stream()
                .collect(Collectors.toMap(OrgUnit::getId, ou -> ou, (a, b) -> a));
        List<OrgUnit> scoped;
        if (scopeCompId == null || scopeCompId.trim().isEmpty()) {
            scoped = all;
        } else if (scopeSubtreeBelowLoginOrg) {
            String sc = scopeCompId.trim();
            Optional<OrgUnit> rootOu = all.stream()
                    .filter(o -> o.getCode() != null && sc.equals(o.getCode().trim()))
                    .findFirst();
            if (rootOu.isEmpty()) {
                scoped = Collections.emptyList();
            } else {
                Set<Long> allowed = new HashSet<>(collectDescendantIds(rootOu.get().getId()));
                if (includeLoginOrgInScope) {
                    allowed.add(rootOu.get().getId());
                }
                scoped = all.stream().filter(o -> allowed.contains(o.getId())).collect(Collectors.toList());
            }
        } else {
            scoped = all.stream()
                    .filter(o -> o.getCode() != null && scopeCompId.trim().equals(o.getCode().trim()))
                    .collect(Collectors.toList());
        }
        List<OrgUnit> filtered;
        if (Boolean.TRUE.equals(includeSub) && cId != null) {
            Set<Long> subtreeIds = new HashSet<>();
            for (OrgUnit o : scoped) {
                if (o.getCode() != null && o.getCode().contains(cId)) {
                    subtreeIds.add(o.getId());
                    subtreeIds.addAll(collectDescendantIds(o.getId()));
                }
            }
            filtered = scoped.stream()
                    .filter(o -> subtreeIds.contains(o.getId()))
                    .filter(o -> (cNm == null || (o.getName() != null && o.getName().contains(cNm))))
                    .filter(o -> (cDiv == null || (o.getOrgLevel() != null && o.getOrgLevel().name().equals(cDiv))))
                    .filter(o -> matchUseYn(o, useYn))
                    .filter(o -> matchPayHoldYn(o, payHoldYn))
                    .filter(o -> matchCeoNm(o, ceoNm))
                    .filter(o -> matchCeoMobile(o, ceoMobile))
                    .filter(o -> matchRegNo(o, regNo))
                    .collect(Collectors.toList());
        } else {
            filtered = scoped.stream()
                    .filter(o -> (cId == null || (o.getCode() != null && o.getCode().contains(cId))))
                    .filter(o -> (cNm == null || (o.getName() != null && o.getName().contains(cNm))))
                    .filter(o -> (cDiv == null || (o.getOrgLevel() != null && o.getOrgLevel().name().equals(cDiv))))
                    .filter(o -> matchUseYn(o, useYn))
                    .filter(o -> matchPayHoldYn(o, payHoldYn))
                    .filter(o -> matchCeoNm(o, ceoNm))
                    .filter(o -> matchCeoMobile(o, ceoMobile))
                    .filter(o -> matchRegNo(o, regNo))
                    .collect(Collectors.toList());
        }
        String useYnFilterNorm = normalizeUseYnFilter(useYn);
        if (useYnFilterNorm != null
                && (OrgUseYnUtil.N.equalsIgnoreCase(useYnFilterNorm) || OrgUseYnUtil.S.equalsIgnoreCase(useYnFilterNorm))) {
            Set<Long> primaryMatchIds = filtered.stream().map(OrgUnit::getId).collect(Collectors.toSet());
            Set<Long> visibleIds = new HashSet<>(primaryMatchIds);
            Map<Long, OrgUnit> scopedById = scoped.stream()
                    .collect(Collectors.toMap(OrgUnit::getId, ou -> ou, (a, b) -> a));
            for (OrgUnit ou : filtered) {
                appendAncestorIdsForCompTree(ou, visibleIds, scopedById, allById);
            }
            filtered = scoped.stream()
                    .filter(o -> visibleIds.contains(o.getId()))
                    .filter(o -> {
                        if (o.getOrgLevel() == OrgLevel.MERCHANT) {
                            return primaryMatchIds.contains(o.getId());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        /* 목록 표시: 조직 트리 전위 순서(부모 직후 자식·가맹점). 형제는 업체코드 순 */
        List<OrgUnit> ordered = sortFilteredOrgsAsHierarchyTree(filtered);
        filtered.clear();
        filtered.addAll(ordered);
        /* 그리드 들여쓰기: 트리 깊이가 아니라 조직 단계(OrgLevel 코드) 기준. 총판 직속 가맹점이 영업점과 같은 열에 붙는 문제 방지 */
        int minOrgLevelCodeInFiltered = 1;
        if (!filtered.isEmpty()) {
            minOrgLevelCodeInFiltered = filtered.stream()
                    .mapToInt(o -> o.getOrgLevel() != null ? o.getOrgLevel().getCode() : 99)
                    .min()
                    .orElse(1);
        }
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = new ArrayList<>();
        if (start < filtered.size()) {
            List<OrgUnit> pageList = filtered.subList(start, end);
            Map<Long, String> masterDistBaseCurrencyCache = new HashMap<>();
            for (int i = 0; i < pageList.size(); i++) {
                OrgUnit ou = pageList.get(i);
                Map<String, Object> row = buildCompListItem(ou, masterDistBaseCurrencyCache);
                row.put("rowNo", start + i + 1);
                row.put("parentId", ou.getParentId());
                int levelCode = ou.getOrgLevel() != null ? ou.getOrgLevel().getCode() : 99;
                row.put("depth", levelCode - minOrgLevelCodeInFiltered);
                if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                    row.put("merchantTreeFolderTone", resolveMerchantTreeFolderTone(ou, allById));
                }
                String ownUse = merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> OrgUseYnUtil.normalize(mp.getUseYn()))
                        .orElse(OrgUseYnUtil.Y);
                row.put("ownUseYn", ownUse);
                row.put("treeRowMuted", !OrgUseYnUtil.Y.equals(ownUse));
                list.add(row);
            }
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(filtered.size());
        int totalPages = (size <= 0) ? 1 : (int) Math.ceil((double) Math.max(0, filtered.size()) / size);
        pr.setTotalPages(Math.max(1, totalPages));
        return pr;
    }

    /**
     * 업체관리 목록: 필터 결과를 실제 parentId 기준 트리 전위 순으로 정렬.
     * 가맹점은 상위 영업점·대리점 등 바로 아래에 나오며, 형제 간 순서는 업체코드(동일 시 id).
     */
    private List<OrgUnit> sortFilteredOrgsAsHierarchyTree(List<OrgUnit> filtered) {
        if (filtered == null || filtered.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> idSet = filtered.stream().map(OrgUnit::getId).collect(Collectors.toSet());
        Comparator<OrgUnit> byCode = Comparator
                .comparing((OrgUnit o) -> o.getCode() != null ? o.getCode().trim() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(OrgUnit::getId);
        Map<Long, List<OrgUnit>> byParent = new HashMap<>();
        for (OrgUnit o : filtered) {
            Long pid = o.getParentId();
            if (pid != null && idSet.contains(pid)) {
                byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(o);
            }
        }
        for (List<OrgUnit> lst : byParent.values()) {
            lst.sort(byCode);
        }
        List<OrgUnit> roots = filtered.stream()
                .filter(o -> o.getParentId() == null || !idSet.contains(o.getParentId()))
                .sorted(byCode)
                .collect(Collectors.toCollection(ArrayList::new));
        List<OrgUnit> ordered = new ArrayList<>(filtered.size());
        Set<Long> visited = new HashSet<>();
        for (OrgUnit r : roots) {
            appendOrgSubtreePreOrder(r, byParent, ordered, visited);
        }
        if (ordered.size() < filtered.size()) {
            Set<Long> seen = ordered.stream().map(OrgUnit::getId).collect(Collectors.toSet());
            filtered.stream()
                    .filter(o -> !seen.contains(o.getId()))
                    .sorted(byCode)
                    .forEach(ordered::add);
        }
        return ordered;
    }

    private void appendOrgSubtreePreOrder(OrgUnit node, Map<Long, List<OrgUnit>> byParent,
                                          List<OrgUnit> out, Set<Long> visited) {
        if (node == null || !visited.add(node.getId())) {
            return;
        }
        out.add(node);
        for (OrgUnit c : byParent.getOrDefault(node.getId(), Collections.emptyList())) {
            appendOrgSubtreePreOrder(c, byParent, out, visited);
        }
    }

    /** 미사용·영구정지 검색 시 매칭 행의 상위 조직(조회 범위 내)을 함께 노출 */
    private void appendAncestorIdsForCompTree(OrgUnit node, Set<Long> out,
                                              Map<Long, OrgUnit> scopedById,
                                              Map<Long, OrgUnit> allById) {
        if (node == null) {
            return;
        }
        Long pid = node.getParentId();
        while (pid != null) {
            if (!out.add(pid)) {
                break;
            }
            OrgUnit parent = scopedById.get(pid);
            if (parent == null) {
                parent = allById.get(pid);
            }
            if (parent == null) {
                break;
            }
            pid = parent.getParentId();
        }
    }

    /**
     * 업체관리 트리: 가맹점 행 톤 — 가맹점에서 상위로 올라가며 만나는 첫 영업 조직 단계.
     * (예: 총판→지사→가맹점이면 지사 톤, 총판→가맹점이면 총판 톤)
     * {@code DIRECT}=총판, {@code BRANCH}=지사, {@code AGENCY}=대리점, {@code SALES}=영업점,
     * {@code REGIONAL}=본사, {@code HEADQUARTERS}=총본사, 그 외 {@code OTHER}.
     */
    private static String resolveMerchantTreeFolderTone(OrgUnit merchant, Map<Long, OrgUnit> byId) {
        if (merchant == null || merchant.getOrgLevel() != OrgLevel.MERCHANT) {
            return "";
        }
        Long curId = merchant.getParentId();
        for (int i = 0; i < 64 && curId != null; i++) {
            OrgUnit anc = byId.get(curId);
            if (anc == null) {
                return "OTHER";
            }
            OrgLevel lv = anc.getOrgLevel();
            if (lv == null) {
                curId = anc.getParentId();
                continue;
            }
            switch (lv) {
                case MASTER_DIST:
                    return "DIRECT";
                case BRANCH:
                    return "BRANCH";
                case AGENCY:
                    return "AGENCY";
                case SALES_OFFICE:
                    return "SALES";
                case REGIONAL:
                    return "REGIONAL";
                case HEADQUARTERS:
                    return "HEADQUARTERS";
                case MERCHANT:
                default:
                    curId = anc.getParentId();
                    break;
            }
        }
        return "OTHER";
    }

    /** 업체변경이력 — 필드 단위 로그 조회 */
    public PageResult<Map<String, Object>> changeHistory(
            String searchCompId,
            String searchCompNm,
            String searchChangedBy,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            int page,
            int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        int sz = Math.max(1, Math.min(size, 5000));
        int pg = Math.max(1, page);
        LocalDateTime fromTs = searchFromDate != null ? searchFromDate.atStartOfDay() : null;
        LocalDateTime toTs = searchToDate != null ? searchToDate.plusDays(1).atStartOfDay() : null;
        Pageable pageable = PageRequest.of(pg - 1, sz, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrgUnitChangeLog> slice = orgUnitChangeAuditService.findAll(
                orgChangeLogSpec(searchCompId, searchCompNm, searchChangedBy, fromTs, toTs),
                pageable);
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> list = new ArrayList<>();
        long total = slice.getTotalElements();
        int start = (pg - 1) * sz;
        int idx = 0;
        for (OrgUnitChangeLog e : slice.getContent()) {
            Map<String, Object> row = new HashMap<>();
            row.put("rowNo", start + ++idx);
            row.put("chgDt", e.getCreatedAt() != null ? e.getCreatedAt().format(df) : "");
            row.put("compId", e.getCompId());
            row.put("compNm", e.getCompNm());
            row.put("chgTarget", e.getFieldLabel());
            row.put("chgBefore", e.getValueBefore());
            row.put("chgAfter", e.getValueAfter());
            row.put("changedBy", e.getChangedBy());
            list.add(row);
        }
        pr.setList(list);
        pr.setPage(pg);
        pr.setSize(sz);
        pr.setTotalElements(total);
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        return pr;
    }

    private Specification<OrgUnitChangeLog> orgChangeLogSpec(
            String searchCompId,
            String searchCompNm,
            String searchChangedBy,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (fromInclusive != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }
            if (toExclusive != null) {
                parts.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            String cid = searchCompId != null ? searchCompId.trim() : "";
            if (!cid.isEmpty()) {
                String lit = likeSafe(cid);
                parts.add(cb.like(cb.lower(root.get("compId")), "%" + lit.toLowerCase() + "%"));
            }
            String cnm = searchCompNm != null ? searchCompNm.trim() : "";
            if (!cnm.isEmpty()) {
                String lit = likeSafe(cnm);
                String pat = "%" + lit.toLowerCase() + "%";
                parts.add(cb.or(
                        cb.like(cb.lower(root.get("compNm")), pat),
                        cb.like(cb.lower(root.get("compId")), pat)));
            }
            String chg = searchChangedBy != null ? searchChangedBy.trim() : "";
            if (!chg.isEmpty()) {
                String lit = likeSafe(chg);
                parts.add(cb.like(cb.lower(root.get("changedBy")), "%" + lit.toLowerCase() + "%"));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    private static String likeSafe(String raw) {
        return raw.replace("%", "").replace("_", "").replace("\\", "");
    }

    private String parentLabel(Long id) {
        if (id == null) {
            return "";
        }
        return orgUnitRepository.findById(id)
                .map(p -> {
                    String c = p.getCode() != null ? p.getCode() : "";
                    String n = p.getName() != null ? p.getName() : "";
                    return n.isEmpty() ? c : c + " " + n;
                })
                .orElse(String.valueOf(id));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String abbrevAudit(String s) {
        if (s == null || s.isBlank()) {
            return "(없음)";
        }
        String t = s.trim().replace('\n', ' ');
        return t.length() > 120 ? t.substring(0, 120) + "…" : t;
    }

    private static String ynDisplay(String y) {
        return OrgUseYnUtil.display(y);
    }

    private static String ynAllowDisplay(String y) {
        if (y == null || y.isBlank()) {
            return "";
        }
        String t = y.trim();
        if ("Y".equalsIgnoreCase(t)) {
            return "허용";
        }
        if ("N".equalsIgnoreCase(t)) {
            return "미허용";
        }
        return t;
    }

    private void addOrgChangeRow(List<OrgUnitChangeLog> rows, Long orgUnitId, String compId, String compNm, String by,
                                 String label, String valueBefore, String valueAfter) {
        OrgUnitChangeLog e = new OrgUnitChangeLog();
        e.setOrgUnitId(orgUnitId);
        e.setCompId(compId);
        e.setCompNm(compNm);
        e.setFieldLabel(label);
        e.setValueBefore(valueBefore);
        e.setValueAfter(valueAfter);
        e.setChangedBy(by);
        rows.add(e);
    }

    private void addDiff(List<OrgUnitChangeLog> rows, Long orgUnitId, String compId, String compNm, String by,
                         String label, String before, String after) {
        if (Objects.equals(nz(before), nz(after))) {
            return;
        }
        addOrgChangeRow(rows, orgUnitId, compId, compNm, by, label, before, after);
    }

    private void addDiffYn(List<OrgUnitChangeLog> rows, Long orgUnitId, String compId, String compNm, String by,
                           String label, String before, String after) {
        if (Objects.equals(nz(before), nz(after))) {
            return;
        }
        addOrgChangeRow(rows, orgUnitId, compId, compNm, by, label, ynDisplay(before), ynDisplay(after));
    }

    private void addDiffYnAllow(List<OrgUnitChangeLog> rows, Long orgUnitId, String compId, String compNm, String by,
                                String label, String before, String after) {
        if (Objects.equals(nz(before), nz(after))) {
            return;
        }
        addOrgChangeRow(rows, orgUnitId, compId, compNm, by, label, ynAllowDisplay(before), ynAllowDisplay(after));
    }

    private void persistSingleOrgFieldChange(OrgUnit ou, String label, String before, String after) {
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), nz(ou.getCode()), nz(ou.getName()), label, before, after);
    }

    private void persistMerchantAuditDiff(MerchantAuditSnapshot snap, OrgUnit ou, MerchantProfile mp, boolean pwdChanged) {
        String by = orgUnitChangeAuditService.currentActor();
        String p = "[업체정보] ";
        String compId = nz(ou.getCode());
        String compNm = nz(ou.getName());
        Long oid = ou.getId();
        List<OrgUnitChangeLog> rows = new ArrayList<>();
        addDiff(rows, oid, compId, compNm, by, p + "업체명", snap.orgName(), nz(ou.getName()));
        addDiff(rows, oid, compId, compNm, by, p + "상위업체", parentLabel(snap.parentId()), parentLabel(ou.getParentId()));
        addDiffYn(rows, oid, compId, compNm, by, p + "업체사용여부", snap.useYn(), mp.getUseYn());
        addDiff(rows, oid, compId, compNm, by, p + "대표전화", snap.compTel(), nz(mp.getCompTel()));
        addDiff(rows, oid, compId, compNm, by, p + "우편번호", snap.zipCode(), nz(mp.getZipCode()));
        addDiff(rows, oid, compId, compNm, by, p + "주소", snap.addr(), nz(mp.getAddr()));
        addDiff(rows, oid, compId, compNm, by, p + "상세주소", snap.addrDetail(), nz(mp.getAddrDetail()));
        addDiff(rows, oid, compId, compNm, by, p + "기타주소", snap.addrEtc(), nz(mp.getAddrEtc()));
        addDiff(rows, oid, compId, compNm, by, p + "주소국가", snap.addrCountryCd(), nz(mp.getAddrCountryCd()));
        addDiff(rows, oid, compId, compNm, by, p + "대표자명", snap.ceoNm(), nz(mp.getCeoNm()));
        addDiff(rows, oid, compId, compNm, by, p + "휴대폰", snap.ceoMobile(), nz(mp.getCeoMobile()));
        addDiff(rows, oid, compId, compNm, by, p + "로그인ID", snap.loginId(), nz(mp.getLoginId()));
        addDiff(rows, oid, compId, compNm, by, p + "사업자번호", snap.regNo(), nz(mp.getRegNo()));
        addDiff(rows, oid, compId, compNm, by, p + "업태", snap.bizType(), nz(mp.getBizType()));
        addDiff(rows, oid, compId, compNm, by, p + "종목", snap.industry(), nz(mp.getIndustry()));
        addDiff(rows, oid, compId, compNm, by, p + "사업자형태", snap.bizNature(), nz(mp.getBizNature()));
        addDiff(rows, oid, compId, compNm, by, p + "취급물품", snap.product(), nz(mp.getProduct()));
        addDiff(rows, oid, compId, compNm, by, p + "대표사이트", snap.homepage(), nz(mp.getHomepage()));
        addDiff(rows, oid, compId, compNm, by, p + "정산담당자명", snap.settleName(), nz(mp.getSettleName()));
        addDiff(rows, oid, compId, compNm, by, p + "정산담당연락처", snap.settleTelNo(), nz(mp.getSettleTelNo()));
        addDiff(rows, oid, compId, compNm, by, p + "팩스", snap.fax(), nz(mp.getFax()));
        addDiff(rows, oid, compId, compNm, by, p + "이메일", snap.email(), nz(mp.getEmail()));
        addDiff(rows, oid, compId, compNm, by, p + "은행코드", snap.bankCd(), nz(mp.getBankCd()));
        addDiff(rows, oid, compId, compNm, by, p + "송금수수료", snap.transferFee(), nz(mp.getTransferFee()));
        addDiff(rows, oid, compId, compNm, by, p + "가상자산송금수수료", snap.cryptoTransferFee(), nz(mp.getCryptoTransferFee()));
        addDiff(rows, oid, compId, compNm, by, p + "계좌번호", snap.accountNo(), nz(mp.getAccountNo()));
        addDiff(rows, oid, compId, compNm, by, p + "예금주", snap.accountHolder(), nz(mp.getAccountHolder()));
        addDiff(rows, oid, compId, compNm, by, p + "비고", snap.remark(), nz(mp.getRemark()));
        addDiffYnAllow(rows, oid, compId, compNm, by, p + "수수료설정허용", snap.commissionConfigAllowed(), mp.getCommissionConfigAllowed());
        addDiffYn(rows, oid, compId, compNm, by, p + "웹결제사용여부", snap.webPaymentUseYn(), mp.getWebPaymentUseYn());
        addDiff(rows, oid, compId, compNm, by, p + "URL결제방식", snap.urlPayCheckoutMode(), nz(mp.getUrlPayCheckoutMode()));
        addDiffYn(rows, oid, compId, compNm, by, p + "URL상품명사용", snap.urlPayProductNameUseYn(), mp.getUrlPayProductNameUseYn());
        addDiffYn(rows, oid, compId, compNm, by, p + "URL가맹점명노출", snap.urlPayCompanyNameShowYn(), mp.getUrlPayCompanyNameShowYn());
        addDiffYn(rows, oid, compId, compNm, by, p + "URL다국어메뉴", snap.urlPayLangMenuUseYn(), mp.getUrlPayLangMenuUseYn());
        addDiffYn(rows, oid, compId, compNm, by, p + "URL배송주소", snap.urlPayShippingAddressUseYn(), mp.getUrlPayShippingAddressUseYn());
        addDiff(rows, oid, compId, compNm, by, p + "URL입력방식",
                com.pg.urlpay.UrlPayInputModeUtil.formatAuditLabel(snap.urlPayInputMode()),
                com.pg.urlpay.UrlPayInputModeUtil.formatAuditLabel(mp.getUrlPayInputMode()));
        addDiff(rows, oid, compId, compNm, by, p + "APIURL결제방식", snap.apiUrlPayCheckoutMode(), nz(mp.getApiUrlPayCheckoutMode()));
        addDiff(rows, oid, compId, compNm, by, p + "JPAY결제창입력필드",
                snap.jpayCheckoutFieldMode(), com.pg.urlpay.JpayCheckoutFieldModeUtil.formatMerchantUiValue(mp.getJpayCheckoutFieldMode()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇URL결제방식", snap.chatbotUrlPayCheckoutMode(), nz(mp.getChatbotUrlPayCheckoutMode()));
        addDiffYn(rows, oid, compId, compNm, by, p + "챗봇결제사용여부", snap.chatbotPaymentUseYn(), mp.getChatbotPaymentUseYn());
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 상품등록 한도(건)",
                snap.chatbotProductSlotLimit() != null ? String.valueOf(snap.chatbotProductSlotLimit()) : "",
                mp.getChatbotProductSlotLimit() != null ? String.valueOf(mp.getChatbotProductSlotLimit()) : "");
        addDiff(rows, oid, compId, compNm, by, p + "챗봇안내 회사명(DB)", snap.chatbotKbCompanyNm(), nz(mp.getChatbotKbCompanyNm()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇안내 주소(DB)", snap.chatbotKbAddr(), nz(mp.getChatbotKbAddr()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇안내 전화(DB)", snap.chatbotKbTel(), nz(mp.getChatbotKbTel()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇안내 이메일(DB)", snap.chatbotKbEmail(), nz(mp.getChatbotKbEmail()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇안내 담당자(DB)", snap.chatbotKbContactNm(), nz(mp.getChatbotKbContactNm()));
        if (!Objects.equals(nz(snap.chatbotKbIntro()), nz(mp.getChatbotKbIntro()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "챗봇안내 회사소개", abbrevAudit(snap.chatbotKbIntro()), abbrevAudit(mp.getChatbotKbIntro()));
        }
        if (!Objects.equals(nz(snap.chatbotKbProductDesc()), nz(mp.getChatbotKbProductDesc()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "챗봇안내 판매상품", abbrevAudit(snap.chatbotKbProductDesc()), abbrevAudit(mp.getChatbotKbProductDesc()));
        }
        if (!Objects.equals(nz(snap.chatbotKbWelcomeHint()), nz(mp.getChatbotKbWelcomeHint()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "챗봇 첫화면 안내",
                    abbrevAudit(snap.chatbotKbWelcomeHint()), abbrevAudit(mp.getChatbotKbWelcomeHint()));
        }
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 운영방식(DB)", snap.chatbotOperationMode(), nz(mp.getChatbotOperationMode()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 가맹점 업체성격(DB)", snap.chatbotMerchantVertical(), nz(mp.getChatbotMerchantVertical()));
        if (!Objects.equals(nz(snap.chatbotMerchantVerticalNotes()), nz(mp.getChatbotMerchantVerticalNotes()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "챗봇 업체성격 보조 메모",
                    abbrevAudit(snap.chatbotMerchantVerticalNotes()), abbrevAudit(mp.getChatbotMerchantVerticalNotes()));
        }
        if (!Objects.equals(nz(snap.chatbotOrderSheetUiJson()), nz(mp.getChatbotOrderSheetUiJson()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "챗봇 주문 시트 UI(JSON)",
                    abbrevAudit(snap.chatbotOrderSheetUiJson()), abbrevAudit(mp.getChatbotOrderSheetUiJson()));
        }
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 예약 슬롯(분)",
                snap.chatbotReservationSlotMinutes() != null ? String.valueOf(snap.chatbotReservationSlotMinutes()) : "",
                mp.getChatbotReservationSlotMinutes() != null ? String.valueOf(mp.getChatbotReservationSlotMinutes()) : "");
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 예약 타임존",
                snap.chatbotReservationZoneId(), nz(mp.getChatbotReservationZoneId()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 상단 로고 URL", snap.chatbotHeaderLogoUrl(), nz(mp.getChatbotHeaderLogoUrl()));
        addDiff(rows, oid, compId, compNm, by, p + "챗봇 관리자(로그인ID)", snap.chatbotAdminUsername(), resolveChatbotAdminUsername(mp));
        addDiffYn(rows, oid, compId, compNm, by, p + "URL·챗봇 승인 알림메일", snap.urlPayAlertEmailYn(), nz(mp.getUrlPayAlertEmailYn()));
        boolean afterLineTok = mp.getUrlPayLineNotifyToken() != null && !mp.getUrlPayLineNotifyToken().isBlank();
        if (snap.urlPayLineTokenConfigured() != afterLineTok) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "LINE Notify 토큰",
                    snap.urlPayLineTokenConfigured() ? "등록" : "미등록",
                    afterLineTok ? "등록" : "미등록");
        }
        addDiff(rows, oid, compId, compNm, by, p + "기준통화", snap.baseCurrency(), nz(mp.getBaseCurrency()));
        addDiff(rows, oid, compId, compNm, by, p + "사이트URL", snap.siteUrl(), nz(mp.getSiteUrl()));
        addDiff(rows, oid, compId, compNm, by, p + "사이트개요", snap.siteSummary(), nz(mp.getSiteSummary()));
        if (!Objects.equals(nz(snap.regionalSettings()), nz(mp.getRegionalSettings()))) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "본사/지역설정(JSON)",
                    snap.regionalSettings(), mp.getRegionalSettings());
        }
        if (pwdChanged) {
            addOrgChangeRow(rows, oid, compId, compNm, by, p + "대표비밀번호", "(유지)", "(변경됨)");
        }
        if (!rows.isEmpty()) {
            orgUnitChangeAuditService.appendAll(rows);
        }
    }

    private record MerchantAuditSnapshot(
            Long parentId,
            String orgName,
            String compTel,
            String zipCode,
            String addr,
            String addrDetail,
            String addrEtc,
            String addrCountryCd,
            String ceoNm,
            String ceoMobile,
            String useYn,
            String loginId,
            String regNo,
            String bizType,
            String industry,
            String bizNature,
            String product,
            String homepage,
            String settleName,
            String settleTelNo,
            String fax,
            String email,
            String bankCd,
            String transferFee,
            String cryptoTransferFee,
            String accountNo,
            String accountHolder,
            String remark,
            String commissionConfigAllowed,
            String webPaymentUseYn,
            String urlPayCheckoutMode,
            String urlPayProductNameUseYn,
            String urlPayCompanyNameShowYn,
            String urlPayLangMenuUseYn,
            String urlPayShippingAddressUseYn,
            String urlPayInputMode,
            String apiUrlPayCheckoutMode,
            String chatbotUrlPayCheckoutMode,
            String jpayCheckoutFieldMode,
            String chatbotPaymentUseYn,
            Integer chatbotProductSlotLimit,
            String chatbotKbCompanyNm,
            String chatbotKbAddr,
            String chatbotKbTel,
            String chatbotKbEmail,
            String chatbotKbContactNm,
            String chatbotKbIntro,
            String chatbotKbProductDesc,
            String chatbotKbWelcomeHint,
            String chatbotOperationMode,
            String chatbotMerchantVertical,
            String chatbotMerchantVerticalNotes,
            String chatbotOrderSheetUiJson,
            Integer chatbotReservationSlotMinutes,
            String chatbotReservationZoneId,
            String chatbotHeaderLogoUrl,
            String chatbotAdminUsername,
            String baseCurrency,
            String siteUrl,
            String siteSummary,
            String regionalSettings,
            String urlPayAlertEmailYn,
            boolean urlPayLineTokenConfigured) {
        static MerchantAuditSnapshot capture(OrgUnit ou, MerchantProfile mp, String chatbotAdminUsernameResolved) {
            return new MerchantAuditSnapshot(
                    ou.getParentId(),
                    nz(ou.getName()),
                    nz(mp.getCompTel()),
                    nz(mp.getZipCode()),
                    nz(mp.getAddr()),
                    nz(mp.getAddrDetail()),
                    nz(mp.getAddrEtc()),
                    nz(mp.getAddrCountryCd()),
                    nz(mp.getCeoNm()),
                    nz(mp.getCeoMobile()),
                    nz(mp.getUseYn()),
                    nz(mp.getLoginId()),
                    nz(mp.getRegNo()),
                    nz(mp.getBizType()),
                    nz(mp.getIndustry()),
                    nz(mp.getBizNature()),
                    nz(mp.getProduct()),
                    nz(mp.getHomepage()),
                    nz(mp.getSettleName()),
                    nz(mp.getSettleTelNo()),
                    nz(mp.getFax()),
                    nz(mp.getEmail()),
                    nz(mp.getBankCd()),
                    nz(mp.getTransferFee()),
                    nz(mp.getCryptoTransferFee()),
                    nz(mp.getAccountNo()),
                    nz(mp.getAccountHolder()),
                    nz(mp.getRemark()),
                    nz(mp.getCommissionConfigAllowed()),
                    nz(mp.getWebPaymentUseYn()),
                    nz(mp.getUrlPayCheckoutMode()),
                    nz(mp.getUrlPayProductNameUseYn()),
                    nz(mp.getUrlPayCompanyNameShowYn()),
                    nz(mp.getUrlPayLangMenuUseYn()),
                    nz(mp.getUrlPayShippingAddressUseYn()),
                    nz(mp.getUrlPayInputMode()),
                    nz(mp.getApiUrlPayCheckoutMode()),
                    nz(mp.getChatbotUrlPayCheckoutMode()),
                    com.pg.urlpay.JpayCheckoutFieldModeUtil.formatMerchantUiValue(mp.getJpayCheckoutFieldMode()),
                    nz(mp.getChatbotPaymentUseYn()),
                    mp.getChatbotProductSlotLimit(),
                    nz(mp.getChatbotKbCompanyNm()),
                    nz(mp.getChatbotKbAddr()),
                    nz(mp.getChatbotKbTel()),
                    nz(mp.getChatbotKbEmail()),
                    nz(mp.getChatbotKbContactNm()),
                    nz(mp.getChatbotKbIntro()),
                    nz(mp.getChatbotKbProductDesc()),
                    nz(mp.getChatbotKbWelcomeHint()),
                    nz(mp.getChatbotOperationMode()),
                    nz(mp.getChatbotMerchantVertical()),
                    nz(mp.getChatbotMerchantVerticalNotes()),
                    nz(mp.getChatbotOrderSheetUiJson()),
                    mp.getChatbotReservationSlotMinutes(),
                    nz(mp.getChatbotReservationZoneId()),
                    nz(mp.getChatbotHeaderLogoUrl()),
                    nz(chatbotAdminUsernameResolved),
                    nz(mp.getBaseCurrency()),
                    nz(mp.getSiteUrl()),
                    nz(mp.getSiteSummary()),
                    nz(mp.getRegionalSettings()),
                    nz(mp.getUrlPayAlertEmailYn()),
                    mp.getUrlPayLineNotifyToken() != null && !mp.getUrlPayLineNotifyToken().isBlank());
        }
    }

    private String resolveChatbotAdminUsername(MerchantProfile mp) {
        if (mp == null || mp.getChatbotAdminUserId() == null) {
            return "";
        }
        return userRepository.findById(mp.getChatbotAdminUserId()).map(AppUser::getUsername).orElse("");
    }

    private void assertExistingUserAssignableAsChatbotAdmin(OrgUnit merchantOu, AppUser existing) {
        String merchantCode = merchantOu.getCode() != null ? merchantOu.getCode().trim() : "";
        String occ = existing.getOrgUnitCode() != null ? existing.getOrgUnitCode().trim() : "";
        if (merchantCode.isEmpty() || occ.isEmpty() || !occ.equalsIgnoreCase(merchantCode)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 로그인ID입니다. 다른 가맹점 소속 계정이거나 사용자관리 등에 등록된 ID는 지정할 수 없습니다.");
        }
        if (merchantProfileRepository.existsByChatbotAdminUserIdAndOrgUnitIdNot(existing.getId(), merchantOu.getId())) {
            throw new IllegalArgumentException(
                    "이 로그인ID는 이미 다른 가맹점의 챗봇 관리자로 등록되어 있습니다.");
        }
    }

    /**
     * 업체 저장 전·중복확인 API용: 챗봇 관리자 ID를 해당 가맹에 둘 수 있는지 검사합니다.
     */
    @Transactional(readOnly = true)
    public void validateChatbotAdminUsernameAssignable(String compId, String rawUsername) {
        if (compId == null || compId.isBlank()) {
            throw new IllegalArgumentException("가맹점 코드가 필요합니다.");
        }
        String uid = rawUsername != null ? rawUsername.trim() : "";
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("챗봇 관리자 로그인ID를 입력하세요.");
        }
        if (uid.length() > 50) {
            throw new IllegalArgumentException("챗봇 관리자 로그인ID는 50자 이하여야 합니다.");
        }
        OrgUnit merchantOu = orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .orElseThrow(() -> new IllegalArgumentException("가맹점 코드를 확인하세요."));
        userRepository.findByUsernameIgnoreCase(uid).ifPresent(u -> assertExistingUserAssignableAsChatbotAdmin(merchantOu, u));
    }

    /**
     * 챗봇 관리자 로그인ID에 해당 사용자가 없으면 해당 가맹 소속 신규 계정을 만듭니다.
     * 비밀번호는 사용자관리 신규 등록과 동일하게 {@code 로그인ID + "1!"} (첫 로그인 시 변경 안내).
     * 공개 챗봇 관리 로그인은 {@link com.pg.service.ChatbotAdminAuthService}에서 OTP까지 요구합니다.
     */
    private AppUser resolveOrCreateChatbotAdminUser(OrgUnit merchantOu, String rawUsername) {
        if (merchantOu == null || merchantOu.getCode() == null || merchantOu.getCode().isBlank()) {
            throw new IllegalArgumentException("가맹 정보를 확인할 수 없습니다.");
        }
        String uid = rawUsername != null ? rawUsername.trim() : "";
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("챗봇 관리자 로그인ID를 입력하세요.");
        }
        if (uid.length() > 50) {
            throw new IllegalArgumentException("챗봇 관리자 로그인ID는 50자 이하여야 합니다.");
        }
        Optional<AppUser> existingOpt = userRepository.findByUsernameIgnoreCase(uid);
        if (existingOpt.isPresent()) {
            AppUser existing = existingOpt.get();
            assertExistingUserAssignableAsChatbotAdmin(merchantOu, existing);
            existing.setPermissionGroupNm(ChatbotMerchantAdminConstants.PERMISSION_GROUP_NM);
            userRepository.save(existing);
            return existing;
        }
        AppUser nu = new AppUser();
        nu.setUsername(uid);
        nu.setName(uid);
        nu.setPassword(passwordEncoder.encode(uid + "1!"));
        nu.setPasswordMustChangeYn("Y");
        nu.setOrgUnitCode(merchantOu.getCode().trim());
        nu.setPermissionGroupNm(ChatbotMerchantAdminConstants.PERMISSION_GROUP_NM);
        nu.setRole("USER");
        nu.setEnabled(true);
        nu.setUserStatus("ACTIVE");
        nu.setOtpRegisteredYn("N");
        nu.setUserType("REPRESENTATIVE");
        nu.setAssistantRoleType(null);
        nu.setParentUsername(null);
        userRepository.save(nu);
        return nu;
    }

    /** 업체코드 → 조직 레벨 이름(HEADQUARTERS 등). 없으면 빈 문자열. */
    @Transactional(readOnly = true)
    public String findOrgLevelNameByCompCode(String compCode) {
        if (compCode == null || compCode.isBlank()) {
            return "";
        }
        return orgUnitRepository.findByCode(compCode.trim())
                .map(ou -> ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "")
                .orElse("");
    }

    /** 지역 본사(업체) 상세 조회 - 업체정보조회/수정 폼용 */
    public Optional<Map<String, Object>> getDetail(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("compId", ou.getCode());
                            m.put("compNm", ou.getName());
                            m.put("compDiv", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
                            m.put("compDivNm", ou.getOrgLevel() != null ? ou.getOrgLevel().getNameKo() : null);
                            m.put("parentId", ou.getParentId());
                            OrgLevel ol = ou.getOrgLevel();
                            boolean parentOrgChangeLocked = ol == OrgLevel.HEADQUARTERS || ol == OrgLevel.REGIONAL || ol == OrgLevel.MASTER_DIST;
                            m.put("parentOrgChangeLocked", parentOrgChangeLocked);
                            findNearestMasterDistAncestorId(ou.getId()).ifPresentOrElse(
                                    mid -> m.put("masterDistScopeOrgId", mid),
                                    () -> m.put("masterDistScopeOrgId", null));
                            if (ou.getParentId() != null) {
                                orgUnitRepository.findById(ou.getParentId())
                                        .ifPresent(p -> m.put("parentComp", p.getCode() + (p.getName() != null ? " (" + p.getName() + ")" : "")));
                            }
                            m.put("compTel", mp.getCompTel());
                            m.put("zipCode", mp.getZipCode());
                            m.put("addr", mp.getAddr());
                            m.put("addrDetail", mp.getAddrDetail());
                            m.put("addrEtc", mp.getAddrEtc());
                            m.put("addrCountryCd", mp.getAddrCountryCd());
                            m.put("ceoNm", mp.getCeoNm());
                            m.put("ceoMobile", mp.getCeoMobile());
                            m.put("useYn", mp.getUseYn());
                            m.put("loginId", mp.getLoginId());
                            m.put("regNo", mp.getRegNo());
                            m.put("bizType", mp.getBizType());
                            m.put("industry", mp.getIndustry());
                            m.put("bizNature", mp.getBizNature());
                            m.put("product", mp.getProduct());
                            m.put("homepage", mp.getHomepage());
                            m.put("settleName", mp.getSettleName());
                            m.put("settleTelNo", mp.getSettleTelNo());
                            m.put("fax", mp.getFax());
                            m.put("email", mp.getEmail());
                            m.put("siteUrl", mp.getSiteUrl());
                            m.put("siteSummary", mp.getSiteSummary());
                            m.put("bankCd", mp.getBankCd());
                            m.put("transferFee", mp.getTransferFee());
                            m.put("cryptoTransferFee", mp.getCryptoTransferFee());
                            m.put("accountNo", mp.getAccountNo());
                            m.put("accountHolder", mp.getAccountHolder());
                            m.put("remark", mp.getRemark());
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT && canManageMerchantOperationRecord()) {
                                m.put("operationRecord", mp.getOperationRecord() != null ? mp.getOperationRecord() : "");
                            }
                            m.put("commissionConfigAllowed", mp.getCommissionConfigAllowed());
                            m.put("webPaymentUseYn", mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn() : "Y");
                            m.put("webPaymentHeaderLogoMode", com.pg.urlpay.WebPaymentHeaderLogoModeUtil.normalizeMerchantStored(mp.getWebPaymentHeaderLogoMode()));
                            m.put("webPaymentHeaderSubtitleMode", com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.normalizeMerchantStored(mp.getWebPaymentHeaderSubtitleMode()));
                            m.put("urlPayCheckoutMode", com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(mp.getUrlPayCheckoutMode()));
                            m.put("urlPayProductNameUseYn", mp.getUrlPayProductNameUseYn() != null ? mp.getUrlPayProductNameUseYn() : "Y");
                            m.put("urlPayCompanyNameShowYn", mp.getUrlPayCompanyNameShowYn() != null ? mp.getUrlPayCompanyNameShowYn() : "Y");
                            m.put("urlPayLangMenuUseYn", mp.getUrlPayLangMenuUseYn() != null ? mp.getUrlPayLangMenuUseYn() : "Y");
                            m.put("checkoutContactRememberMode",
                                    mp.getCheckoutContactRememberMode() != null ? mp.getCheckoutContactRememberMode() : "FOLLOW_HQ");
                            m.put("urlPayShippingAddressUseYn", mp.getUrlPayShippingAddressUseYn() != null ? mp.getUrlPayShippingAddressUseYn() : "N");
                            m.put("urlPayInputMode", com.pg.urlpay.UrlPayInputModeUtil.formatMerchantUiValue(mp.getUrlPayInputMode()));
                            m.put("urlPayCardExpiryMode", com.pg.urlpay.UrlPayCardExpiryModeUtil.formatMerchantUiValue(mp.getUrlPayCardExpiryMode()));
                            m.put("cardAuthMode", com.pg.urlpay.CardAuthModeUtil.formatMerchantUiValue(mp.getCardAuthMode()));
                            m.put("apiUrlPayCheckoutMode", com.pg.splitpay.SplitPayMerchantUtil.resolveApiCheckoutModeForDisplay(mp));
                            m.put("chatbotUrlPayCheckoutMode", com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(mp.getChatbotUrlPayCheckoutMode()));
                            m.put("jpayCheckoutFieldMode", com.pg.urlpay.JpayCheckoutFieldModeUtil.formatMerchantUiValue(mp.getJpayCheckoutFieldMode()));
                            m.put("jpayPhoneDialCodeYn", com.pg.urlpay.JpayPhoneDialCodeUtil.formatMerchantUiValue(mp.getJpayPhoneDialCodeYn()));
                            m.put("apiJpaySubscriptionUseYn", mp.getApiJpaySubscriptionUseYn() != null ? mp.getApiJpaySubscriptionUseYn() : "N");
                            m.put("apiBrokerInlineUseYn", mp.getApiBrokerInlineUseYn() != null ? mp.getApiBrokerInlineUseYn() : "Y");
                            m.put("apiBrokerRedirectUseYn", mp.getApiBrokerRedirectUseYn() != null ? mp.getApiBrokerRedirectUseYn() : "N");
                            m.put("apiWordpressUseYn", mp.getApiWordpressUseYn() != null ? mp.getApiWordpressUseYn() : "N");
                            m.put("mobileCheckoutMode", mp.getMobileCheckoutMode() != null ? mp.getMobileCheckoutMode() : "");
                            m.put("chatbotPaymentUseYn", mp.getChatbotPaymentUseYn() != null ? mp.getChatbotPaymentUseYn() : "N");
                            m.put("chatbotProductSlotLimit", mp.getChatbotProductSlotLimit() != null ? mp.getChatbotProductSlotLimit() : "");
                            m.put("chatbotCatalogListingGrant", mp.getChatbotCatalogListingGrant() != null ? mp.getChatbotCatalogListingGrant() : "");
                            m.put("chatbotCatalogListingEnabled", mp.getChatbotCatalogListingEnabled() != null ? mp.getChatbotCatalogListingEnabled() : "");
                            m.put("chatbotMaxProductImagesGrant", mp.getChatbotMaxProductImagesGrant() != null
                                    ? String.valueOf(mp.getChatbotMaxProductImagesGrant()) : "");
                            m.put("chatbotPromotionShelfMode",
                                    mp.getChatbotPromotionShelfMode() != null && !mp.getChatbotPromotionShelfMode().isBlank()
                                            ? mp.getChatbotPromotionShelfMode().trim() : "PROMOTION");
                            m.put("chatbotPromotionRotateSeconds",
                                    mp.getChatbotPromotionRotateSeconds() != null ? mp.getChatbotPromotionRotateSeconds() : 30);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                m.put("chatbotEffectiveListingTypes", ChatbotCatalogPolicy.joinListingCsv(
                                        merchantChatbotProductService.resolveEffectiveListingTypeCodes(ou.getId())));
                                m.put("chatbotEffectiveMaxProductImages", merchantChatbotProductService.getEffectiveMaxProductImages(ou.getId()));
                                m.put("payFollowMerchantUseYn", mp.getPayFollowMerchantUseYn());
                                m.put("payFollowAutoVoidYn", mp.getPayFollowAutoVoidYn());
                                m.put("payFollowEmailVoidYn", mp.getPayFollowEmailVoidYn());
                                m.put("payFollowAutoRefundYn", mp.getPayFollowAutoRefundYn());
                                m.put("payFollowForceRefundYn", mp.getPayFollowForceRefundYn());
                                hqRiskCardPolicyService.putMerchantCardRiskOnMap(m, mp);
                                m.put("chatbotHeaderLogoUrl", mp.getChatbotHeaderLogoUrl() != null ? mp.getChatbotHeaderLogoUrl() : "");
                                m.put("webPaymentHeaderLogoUrl", mp.getWebPaymentHeaderLogoUrl() != null ? mp.getWebPaymentHeaderLogoUrl() : "");
                                m.put("webPaymentHeaderHtmlTitle", mp.getWebPaymentHeaderHtmlTitle() != null ? mp.getWebPaymentHeaderHtmlTitle() : "");
                                m.put("webPaymentHeaderSubtitleText", mp.getWebPaymentHeaderSubtitleText() != null ? mp.getWebPaymentHeaderSubtitleText() : "");
                                m.put("chatbotAdminUsername", resolveChatbotAdminUsername(mp));
                                m.putAll(merchantChatbotKbService.effectiveKbForDisplay(ou, mp));
                            }
                            m.put("baseCurrency", mp.getBaseCurrency());
                            m.put("orgUnitId", ou.getId());
                            m.put("tabletFeatureUseYn", "Y".equalsIgnoreCase(ou.getTabletFeatureUseYn() != null ? ou.getTabletFeatureUseYn().trim() : "") ? "Y" : "N");
                            if ("MASTER_DIST".equalsIgnoreCase(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "")) {
                                for (MerchantNotifyUrl n : merchantNotifyUrlRepository.findByOrgUnitIdOrderByUrlTypeAsc(ou.getId())) {
                                    if (n.getUrlType() == null) continue;
                                    switch (n.getUrlType()) {
                                        case "NOTIFY_1" -> m.put("notifyUrl1", n.getNotiUrl());
                                        case "NOTIFY_2" -> m.put("notifyUrl2", n.getNotiUrl());
                                        case "NOTIFY_3" -> m.put("notifyUrl3", n.getNotiUrl());
                                        case "NOTIFY_4" -> m.put("notifyUrl4", n.getNotiUrl());
                                        default -> {}
                                    }
                                }
                            }
                            List<Map<String, Object>> pgBindings = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId()).stream()
                                    .map(b -> {
                                        Map<String, Object> bm = new HashMap<>();
                                        bm.put("id", b.getId());
                                        bm.put("pgCd", b.getPgCd() != null ? b.getPgCd() : "");
                                        bm.put("activationYn", b.getActivationYn() != null ? b.getActivationYn() : "Y");
                                        bm.put("operationalYn", b.getOperationalYn() != null ? b.getOperationalYn() : "N");
                                        bm.put("payMethod", b.getPayMethod() != null ? b.getPayMethod() : "WEB");
                                        bm.put("mid", b.getMid() != null ? b.getMid() : "");
                                        bm.put("rootNo", b.getRootNo() != null ? b.getRootNo() : "");
                                        putMerchantPgBindingSecretFields(bm, b);
                                        bm.put("installmentYn", b.getInstallmentYn() != null ? b.getInstallmentYn() : "N");
                                        bm.put("maxInstallmentMonths", b.getMaxInstallmentMonths() != null ? String.valueOf(b.getMaxInstallmentMonths()) : "");
                                        bm.put("urlPayPricingMode", b.getUrlPayPricingMode() != null ? b.getUrlPayPricingMode() : "CHECKOUT_CURRENCY");
                                        String pgc = b.getPgCd() != null ? b.getPgCd().trim() : "";
                                        String integUrl = "N";
                                        if (!pgc.isEmpty()) {
                                            integUrl = pgAgencyRepository.findByPgCd(pgc)
                                                    .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                                                    .map(a -> "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : "") ? "Y" : "N")
                                                    .orElse("N");
                                        }
                                        bm.put("integUrlPayYn", integUrl);
                                        bm.put("cardBrandScope", b.getCardBrandScope() != null ? b.getCardBrandScope() : "ALL");
                                        bm.put("currencyScope", b.getCurrencyScope() != null ? b.getCurrencyScope() : "ALL");
                                        bm.put("extSettleMode", b.getExtSettleMode() != null ? b.getExtSettleMode() : "");
                                        bm.put("extSettleLag", b.getExtSettleLag() != null ? String.valueOf(b.getExtSettleLag()) : "");
                                        bm.put("extSettleBatchTime", b.getExtSettleBatchTime() != null ? b.getExtSettleBatchTime().toString() : "");
                                        return bm;
                                    })
                                    .collect(Collectors.toList());
                            m.put("pgBindings", pgBindings);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                if (syncMerchantWebPaymentUseYnIfNoUrlPayBinding(ou.getId(), mp)) {
                                    merchantProfileRepository.save(mp);
                                }
                                m.put("webPaymentUseYn", mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn() : "Y");
                                m.put("urlPayCheckoutMode", com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(mp.getUrlPayCheckoutMode()));
                                m.put("urlPayProductNameUseYn", mp.getUrlPayProductNameUseYn() != null ? mp.getUrlPayProductNameUseYn() : "Y");
                                m.put("urlPayCompanyNameShowYn", mp.getUrlPayCompanyNameShowYn() != null ? mp.getUrlPayCompanyNameShowYn() : "Y");
                                m.put("urlPayLangMenuUseYn", mp.getUrlPayLangMenuUseYn() != null ? mp.getUrlPayLangMenuUseYn() : "Y");
                            m.put("checkoutContactRememberMode",
                                    mp.getCheckoutContactRememberMode() != null ? mp.getCheckoutContactRememberMode() : "FOLLOW_HQ");
                            m.put("urlPayShippingAddressUseYn", mp.getUrlPayShippingAddressUseYn() != null ? mp.getUrlPayShippingAddressUseYn() : "N");
                            m.put("urlPayInputMode", com.pg.urlpay.UrlPayInputModeUtil.formatMerchantUiValue(mp.getUrlPayInputMode()));
                            m.put("urlPayCardExpiryMode", com.pg.urlpay.UrlPayCardExpiryModeUtil.formatMerchantUiValue(mp.getUrlPayCardExpiryMode()));
                            m.put("cardAuthMode", com.pg.urlpay.CardAuthModeUtil.formatMerchantUiValue(mp.getCardAuthMode()));
                                m.put("apiUrlPayCheckoutMode", com.pg.splitpay.SplitPayMerchantUtil.resolveApiCheckoutModeForDisplay(mp));
                                m.put("chatbotUrlPayCheckoutMode", com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(mp.getChatbotUrlPayCheckoutMode()));
                                m.put("jpayCheckoutFieldMode", com.pg.urlpay.JpayCheckoutFieldModeUtil.formatMerchantUiValue(mp.getJpayCheckoutFieldMode()));
                                m.put("jpayPhoneDialCodeYn", com.pg.urlpay.JpayPhoneDialCodeUtil.formatMerchantUiValue(mp.getJpayPhoneDialCodeYn()));
                            m.put("apiJpaySubscriptionUseYn", mp.getApiJpaySubscriptionUseYn() != null ? mp.getApiJpaySubscriptionUseYn() : "N");
                            m.put("apiBrokerInlineUseYn", mp.getApiBrokerInlineUseYn() != null ? mp.getApiBrokerInlineUseYn() : "Y");
                            m.put("apiBrokerRedirectUseYn", mp.getApiBrokerRedirectUseYn() != null ? mp.getApiBrokerRedirectUseYn() : "N");
                            m.put("apiWordpressUseYn", mp.getApiWordpressUseYn() != null ? mp.getApiWordpressUseYn() : "N");
                            m.put("mobileCheckoutMode", mp.getMobileCheckoutMode() != null ? mp.getMobileCheckoutMode() : "");
                                m.put("urlPayWebSettingsAllowed",
                                        chillPayService.findOperationalWebBindingForUrlPay(ou.getId()).isPresent() ? "Y" : "N");
                            }
                            Map<String, Object> ownRegionalSettings = parseRegionalSettings(mp.getRegionalSettings());
                            if (!ownRegionalSettings.isEmpty()) {
                                m.putAll(ownRegionalSettings);
                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                    Object rcl = ownRegionalSettings.get("regionalCardLimits");
                                    if (rcl instanceof String) {
                                        try { m.put("regionalCardLimits", om.readValue((String) rcl, java.util.List.class)); } catch (Exception ignored) {}
                                    } else if (rcl instanceof java.util.List) m.put("regionalCardLimits", rcl);
                                    Object rt = ownRegionalSettings.get("regionalTerminals");
                                    if (rt instanceof String) {
                                        try { m.put("regionalTerminals", om.readValue((String) rt, java.util.List.class)); } catch (Exception ignored) {}
                                    } else if (rt instanceof java.util.List) m.put("regionalTerminals", rt);
                                } catch (Exception ignored) {}
                            }
                            // 총본사가 본사 영업일을 지정한 경우: 총판은 상위 본사 값을 강제 상속(하위 개별 설정 불가)
                            if (ou.getOrgLevel() == OrgLevel.REGIONAL) {
                                m.put("holidayManagedByHeadquartersYn", isHolidayManagedByHeadquarters(ownRegionalSettings) ? "Y" : "N");
                            } else if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                                boolean parentLockedByHq = isParentRegionalHolidayManagedByHeadquarters(ou.getParentId());
                                if (parentLockedByHq) {
                                    Map<String, Object> inherited = resolveInheritedHolidaySettings(ou.getParentId());
                                    if (!inherited.isEmpty()) {
                                        m.putAll(inherited);
                                    }
                                    m.put("holidayInheritedYn", "Y");
                                    m.put("holidayLockedByHeadquartersYn", "Y");
                                } else {
                                    boolean hasOwnHoliday = hasOwnHolidaySetting(ownRegionalSettings);
                                    if (!hasOwnHoliday) {
                                        Map<String, Object> inherited = resolveInheritedHolidaySettings(ou.getParentId());
                                        if (!inherited.isEmpty()) {
                                            m.putAll(inherited);
                                            m.put("holidayInheritedYn", "Y");
                                        }
                                    } else {
                                        m.put("holidayInheritedYn", "N");
                                    }
                                    m.put("holidayLockedByHeadquartersYn", "N");
                                }
                            }
                            String primaryLoginId = mp.getLoginId() != null ? mp.getLoginId().trim() : "";
                            String assistantLoginId = m.get("assistantLoginId") != null ? String.valueOf(m.get("assistantLoginId")).trim() : "";
                            if (assistantLoginId.isEmpty() && ou.getCode() != null && !ou.getCode().isBlank()) {
                                for (AppUser u : userRepository.findByOrgUnitCode(ou.getCode().trim())) {
                                    String uname = u.getUsername() != null ? u.getUsername().trim() : "";
                                    if (uname.isEmpty()) continue;
                                    if (!primaryLoginId.isEmpty() && primaryLoginId.equalsIgnoreCase(uname)) continue;
                                    if ("ADMIN".equalsIgnoreCase(u.getRole())) continue;
                                    assistantLoginId = uname;
                                    break;
                                }
                            }
                            if (!assistantLoginId.isEmpty()) m.put("assistantLoginId", assistantLoginId);
                            boolean assistantAccountExists = !assistantLoginId.isEmpty()
                                    && userRepository.findByUsername(assistantLoginId).isPresent();
                            m.put("assistantPwdSetYn", assistantAccountExists ? "Y" : "N");
                            String assistantRoleType = m.get("assistantRoleType") != null ? String.valueOf(m.get("assistantRoleType")).trim() : "";
                            if ((assistantRoleType == null || assistantRoleType.isBlank()) && !assistantLoginId.isEmpty()) {
                                userRepository.findByUsername(assistantLoginId).ifPresent(au -> {
                                    if (au.getAssistantRoleType() != null && !au.getAssistantRoleType().isBlank()) {
                                        m.put("assistantRoleType", au.getAssistantRoleType());
                                    }
                                });
                            }
                            String brandingYn = m.get("brandingEditAllowedYn") != null ? String.valueOf(m.get("brandingEditAllowedYn")) : "N";
                            m.put("brandingEditAllowedYn", "Y".equalsIgnoreCase(brandingYn) ? "Y" : "N");
                            settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                                if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                    m.put("calcCycle", ss.getCalcCycle());
                                    m.put("pendingCalcCycle", ss.getPendingCalcCycle());
                                    m.put("pendingCalcCycleAt", ss.getPendingCalcCycleAt() != null ? ss.getPendingCalcCycleAt().toString() : null);
                                    String pend = ss.getPendingCalcCycle();
                                    m.put("calcCycleTransitionMode",
                                            (pend != null && !pend.isBlank()) ? "NEXT_AFTER_RUN" : "IMMEDIATE");
                                } else {
                                    m.put("calcCycle", null);
                                    m.put("pendingCalcCycle", null);
                                    m.put("pendingCalcCycleAt", null);
                                    m.put("calcCycleTransitionMode", "IMMEDIATE");
                                }
                                m.put("calcProcType", ss.getCalcProcType());
                                m.put("transferType", ss.getTransferType());
                                m.put("holdRate", ss.getHoldRate() != null ? PercentDecimalHelper.toPlainOneDecimal(ss.getHoldRate()) : null);
                                m.put("holdDays", ss.getHoldDays());
                                m.put("payLimitDefault", ss.getPayLimitDefault());
                                m.put("withdrawRestrictType", ss.getWithdrawRestrictType());
                                m.put("withdrawLimitDays", ss.getWithdrawLimitDays());
                                m.put("withdrawStartTime", ss.getWithdrawStartTime() != null ? ss.getWithdrawStartTime().toString() : null);
                                m.put("withdrawEndTime", ss.getWithdrawEndTime() != null ? ss.getWithdrawEndTime().toString() : null);
                                m.put("payLimitExtra", ss.getPayLimitExtra());
                                m.put("payLimitAlertSms", ss.getPayLimitAlertSms());
                                m.put("holdRateFollowHq", ss.getHoldRateFollowHq());
                                m.put("calcCloseTime", ss.getCalcCloseTime() != null ? ss.getCalcCloseTime().toString() : null);
                                m.put("transferCycleDays", ss.getTransferCycleDays());
                                m.put("autoTransferMin", ss.getAutoTransferMin());
                                m.put("calcMinAmt", ss.getCalcMinAmt());
                                m.put("transferExecTime", ss.getTransferExecTime() != null ? ss.getTransferExecTime().toString() : null);
                                m.put("payHoldYn", ss.getPayHoldYn());
                                m.put("calcExcludeYn", ss.getCalcExcludeYn());
                                m.put("calcExcludeTarget", ss.getCalcExcludeTarget());
                                String normSt = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
                                String procSt = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
                                if ("MANUAL".equalsIgnoreCase(procSt) || !SettlementCycleTiming.isCalcStartTimeApplicableForAuto(normSt)) {
                                    m.put("calcStartTime", null);
                                } else {
                                    m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : null);
                                }
                                m.put("feeVatApplyYn", ss.getFeeVatApplyYn());
                                m.put("feeVatRatePct", ss.getFeeVatRatePct() != null ? ss.getFeeVatRatePct().stripTrailingZeros().toPlainString() : null);
                                if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                                    m.put("receiptEmailEnabledYn", ss.getReceiptEmailEnabledYn() != null ? ss.getReceiptEmailEnabledYn() : "");
                                }
                            });
                            applyCommissionDetailToMap(m, ou);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ssM ->
                                        applyMerchantVoidRefundModesDetailFromSettlement(ssM, m));
                            }
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                merchantDefaultProductRepository.findByOrgUnitId(ou.getId()).ifPresent(dp -> {
                                    if (dp.getProductName() != null) m.put("defaultProductName", dp.getProductName());
                                    if (dp.getProductCode() != null) m.put("defaultProductCode", dp.getProductCode());
                                    if (dp.getDefaultAmount() != null) {
                                        m.put("defaultProductAmount", dp.getDefaultAmount().stripTrailingZeros().toPlainString());
                                    }
                                    if (dp.getProductDesc() != null) m.put("defaultProductDesc", dp.getProductDesc());
                                });
                                putMerchantPayNotifyUrlsForDetail(m, mp, ou.getId());
                                m.put("urlPayAlertEmailYn", mp.getUrlPayAlertEmailYn() != null ? mp.getUrlPayAlertEmailYn() : "N");
                                m.put("urlPayLineNotifyTokenConfigured",
                                        (mp.getUrlPayLineNotifyToken() != null && !mp.getUrlPayLineNotifyToken().isBlank()) ? "Y" : "N");
                                m.put("receiptEmailFollowHqYn", mp.getReceiptEmailFollowHqYn() != null ? mp.getReceiptEmailFollowHqYn() : "Y");
                                m.put("receiptEmailUseYn", mp.getReceiptEmailUseYn() != null ? mp.getReceiptEmailUseYn() : "N");
                                m.put("splitPayEnabledYn", mp.getSplitPayEnabledYn() != null ? mp.getSplitPayEnabledYn() : "N");
                                m.put("splitPayContractCancelYn", mp.getSplitPayContractCancelYn() != null
                                        ? mp.getSplitPayContractCancelYn() : "FOLLOW_HQ");
                                m.put("splitPayIntervalMonthYn", mp.getSplitPayIntervalMonthYn() != null ? mp.getSplitPayIntervalMonthYn() : "Y");
                                m.put("splitPayIntervalDayYn", mp.getSplitPayIntervalDayYn() != null ? mp.getSplitPayIntervalDayYn() : "N");
                                m.put("splitPayIntervalMultiYn", mp.getSplitPayIntervalMultiYn() != null ? mp.getSplitPayIntervalMultiYn() : "N");
                                m.put("splitPayMultiMaxMonths", mp.getSplitPayMultiMaxMonths() != null ? mp.getSplitPayMultiMaxMonths() : 6);
                                m.put("splitPayDayIntervalDays", mp.getSplitPayDayIntervalDays() != null ? mp.getSplitPayDayIntervalDays() : 10);
                                m.put("splitPayMonthIntervalMonths", mp.getSplitPayMonthIntervalMonths() != null ? mp.getSplitPayMonthIntervalMonths() : 1);
                                m.put("splitPayFirstPayMode", mp.getSplitPayFirstPayMode() != null ? mp.getSplitPayFirstPayMode() : "IMMEDIATE");
                                m.put("splitPayHeaderLogoMode", com.pg.urlpay.WebPaymentHeaderLogoModeUtil.normalize(mp.getSplitPayHeaderLogoMode()));
                                m.put("splitPayHeaderLogoUrl", mp.getSplitPayHeaderLogoUrl() != null ? mp.getSplitPayHeaderLogoUrl() : "");
                                m.put("splitPayHeaderHtmlTitle", mp.getSplitPayHeaderHtmlTitle() != null ? mp.getSplitPayHeaderHtmlTitle() : "");
                                m.put("splitPayHeaderSubtitleMode", com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.normalize(mp.getSplitPayHeaderSubtitleMode()));
                                m.put("splitPayHeaderSubtitleText", mp.getSplitPayHeaderSubtitleText() != null ? mp.getSplitPayHeaderSubtitleText() : "");
                                m.put("splitPayLangMenuUseYn", mp.getSplitPayLangMenuUseYn() != null ? mp.getSplitPayLangMenuUseYn() : "Y");
                            }
                            return m;
                        }));
    }

    /** 지역 본사(업체) 정보 수정 */
    @Transactional
    public boolean update(String compId, String compNm, String compDiv, Long parentId, String compTel,
                          String zipCode, String addr, String addrDetail, String addrEtc, String addrCountryCd, String ceoNm, String ceoMobile,
                          String useYn, String loginId, String pwd, String regNo, String bizType, String industry,
                          String bizNature, String product, String homepage, String settleName, String settleTelNo,
                          String fax, String email, String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                          String remark, String commissionConfigAllowed, String webPaymentUseYn,
                          String webPaymentHeaderLogoMode, String webPaymentHeaderLogoUrl,
                          String webPaymentHeaderHtmlTitle,
                          String webPaymentHeaderSubtitleMode, String webPaymentHeaderSubtitleText,
                          String chatbotPaymentUseYn, Integer chatbotProductSlotLimit, String baseCurrency,
                          String siteUrl, String siteSummary, String pgBindings, String regionalSettings,
                          String assistantLoginId, String assistantPwd, String assistantRoleType, String brandingEditAllowedYn,
                          String defaultProductName, String defaultProductCode, String defaultProductAmount, String defaultProductDesc,
                          String notifyUrlBackground, String notifyUrlResult,
                          String jpayNotifyUrl, String jpayCallbackUrl,
                          String notifyUrl1, String notifyUrl2, String notifyUrl3, String notifyUrl4,
                          String middlewareNotifyUrl, String middlewareNotifySecret,
                          String commissionFollowHq, String hqPolicyScope, String perTxFee, String cancelRate,
                          String voidFeePerTx, String manualVoidFeePerTx, String usageRate,
                          String failFee, String payRate, String refundRate, String rollingPct, String rollingDays,
                          String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
                          String fee3dsRate, String chargebackFeePerTx, String chargebackPolicyId,
                          String voidSettlementMode, String manualVoidSettlementMode, String refundSettlementMode, String forceRefundSettlementMode,
                          String payFollowMerchantUseYn, String payFollowAutoVoidYn, String payFollowEmailVoidYn,
                          String payFollowAutoRefundYn, String payFollowForceRefundYn,
                          String urlPayAlertEmailYn, String urlPayLineNotifyToken,
                          String receiptEmailFollowHqYn, String receiptEmailUseYn, String receiptEmailEnabledYn,
                          String chatbotHeaderLogoUrl, String chatbotAdminUsername,
                          String chatbotCatalogListingGrant, Integer chatbotMaxProductImagesGrant,
                          String chatbotCatalogListingEnabled,
                          String chatbotPromotionShelfMode, Integer chatbotPromotionRotateSeconds,
                          String urlPayCheckoutMode,
                          String urlPayProductNameUseYn,
                          String urlPayCompanyNameShowYn,
                          String urlPayLangMenuUseYn,
                          String checkoutContactRememberMode,
                          String urlPayShippingAddressUseYn,
                          String urlPayInputMode,
                          String urlPayCardExpiryMode,
                          String cardAuthMode,
                          String apiUrlPayCheckoutMode,
                          String chatbotUrlPayCheckoutMode,
                          String apiJpaySubscriptionUseYn,
                          String apiBrokerInlineUseYn,
                          String apiBrokerRedirectUseYn,
                          String apiWordpressUseYn,
                          String jpayCheckoutFieldMode,
                          String jpayPhoneDialCodeYn,
                          String tabletFeatureUseYn,
                          String merchantSplitPayJson,
                          String splitPayEnabledYn,
                          String splitPayContractCancelYn,
                          String splitPayIntervalMonthYn,
                          String splitPayIntervalDayYn,
                          String splitPayIntervalMultiYn,
                          String splitPayDayIntervalDays,
                          String splitPayMonthIntervalMonths,
                          String splitPayMultiMaxMonths,
                          String splitPayFirstPayMode,
                          String splitPayHeaderLogoMode,
                          String splitPayHeaderLogoUrl,
                          String splitPayHeaderHtmlTitle,
                          String splitPayHeaderSubtitleMode,
                          String splitPayHeaderSubtitleText,
                          String splitPayLangMenuUseYn,
                          String cardRiskPolicyMode,
                          String cardRiskTier1Hours, String cardRiskTier1Min,
                          String cardRiskTier2Hours, String cardRiskTier2Min,
                          String cardRiskTier3Hours, String cardRiskTier3Min,
                          String cardRiskTier4Hours, String cardRiskTier4Min,
                          String cardRiskAutoBlacklistTier,
                          String cardRiskPresaleMode,
                          String cardRiskPresaleBuyerMismatchYn,
                          String cardRiskPresaleHolderNameYn,
                          String cardRiskPresalePhoneInvalidYn,
                          String cardRiskPresaleEmailInvalidYn,
                          String cardRiskPresaleVelocityCardYn,
                          String cardRiskPresaleVelocityEmailYn,
                          String cardRiskPresaleVelocityIpYn,
                          String cardRiskPresaleVelCardWinMin,
                          String cardRiskPresaleVelCardMax,
                          String cardRiskPresaleVelEmailWinMin,
                          String cardRiskPresaleVelEmailMax,
                          String cardRiskPresaleVelIpWinMin,
                          String cardRiskPresaleVelIpMax) {
        String[] splitPayMerged = mergeMerchantSplitPayParamsFromJson(merchantSplitPayJson,
                splitPayEnabledYn, splitPayContractCancelYn,
                splitPayIntervalMonthYn, splitPayIntervalDayYn, splitPayIntervalMultiYn,
                splitPayDayIntervalDays, splitPayMonthIntervalMonths, splitPayMultiMaxMonths, splitPayFirstPayMode,
                splitPayHeaderLogoMode, splitPayHeaderLogoUrl, splitPayHeaderHtmlTitle,
                splitPayHeaderSubtitleMode, splitPayHeaderSubtitleText, splitPayLangMenuUseYn);
        final String spEnabledYn = splitPayMerged[0];
        final String spContractCancelYn = splitPayMerged[1];
        final String spIntervalMonthYn = splitPayMerged[2];
        final String spIntervalDayYn = splitPayMerged[3];
        final String spIntervalMultiYn = splitPayMerged[4];
        final String spDayIntervalDays = splitPayMerged[5];
        final String spMonthIntervalMonths = splitPayMerged[6];
        final String spMultiMaxMonths = splitPayMerged[7];
        final String spFirstPayMode = splitPayMerged[8];
        final String spHeaderLogoMode = splitPayMerged[9];
        final String spHeaderLogoUrl = splitPayMerged[10];
        final String spHeaderHtmlTitle = splitPayMerged[11];
        final String spHeaderSubtitleMode = splitPayMerged[12];
        final String spHeaderSubtitleText = splitPayMerged[13];
        final String spLangMenuUseYn = splitPayMerged[14];
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            MerchantAuditSnapshot snap = MerchantAuditSnapshot.capture(ou, mp, resolveChatbotAdminUsername(mp));
                            /* 수수료 본사정책 따름/정책선택 — 병합 전 값(업체명만 저장해도 템플릿이 배분을 덮지 않도록) */
                            Map<String, Object> prevCommissionRs = parseRegionalSettings(mp.getRegionalSettings());
                            String prevCommissionFollow = String.valueOf(prevCommissionRs.getOrDefault("commissionFollowHq", "Y")).trim();
                            String prevHqPolicyScope = String.valueOf(prevCommissionRs.getOrDefault("hqPolicyScope", "")).trim();
                            if (compNm != null) ou.setName(compNm);
                            // 업체정보 수정에서는 기존 조직레벨 변경을 허용하지 않는다.
                            // (업체코드-조직레벨 정합성 보장: 총본사/본사/총판이 하위 레벨로 바뀌는 오작동 방지)
                            OrgLevel childLevel = ou.getOrgLevel();
                            // 총본사 고정코드(0000000000)는 레벨을 항상 HEADQUARTERS로 강제 보정한다.
                            if (isHeadquartersFixedCode(ou.getCode()) && childLevel != OrgLevel.HEADQUARTERS) {
                                ou.setOrgLevel(OrgLevel.HEADQUARTERS);
                                childLevel = OrgLevel.HEADQUARTERS;
                            }
                            if (childLevel == null) {
                                throw new IllegalArgumentException("조직 레벨 정보가 없습니다. 관리자에게 문의하세요.");
                            }
                            if (compDiv != null && !compDiv.isBlank()) {
                                String incomingDiv = compDiv.trim().toUpperCase();
                                String currentDiv = childLevel.name();
                                if (!incomingDiv.equals(currentDiv)) {
                                    throw new IllegalArgumentException("업체구분은 변경할 수 없습니다. 현재 조직구분(" + currentDiv + ")으로만 저장 가능합니다.");
                                }
                            }
                            boolean parentLocked = childLevel == OrgLevel.HEADQUARTERS
                                    || childLevel == OrgLevel.REGIONAL
                                    || childLevel == OrgLevel.MASTER_DIST;
                            if (parentId != null) {
                                if (parentLocked) {
                                    if (!Objects.equals(parentId, ou.getParentId())) {
                                        throw new IllegalArgumentException("본사·총판은 상위 조직을 변경할 수 없습니다.");
                                    }
                                } else {
                                    validateParentNotDescendantOfChild(ou.getId(), parentId);
                                    validateParentLevel(parentId, childLevel, ou.getId());
                                    Optional<Long> masterOpt = findNearestMasterDistAncestorId(ou.getId());
                                    if (masterOpt.isPresent()) {
                                        Long m = masterOpt.get();
                                        if (!isUnderMasterDistScope(m, parentId)) {
                                            throw new IllegalArgumentException(
                                                    "동일 총판 산하에서만 상위를 변경할 수 있습니다. 해당 총판 또는 그 하위 조직만 선택하세요.");
                                        }
                                    }
                                    ou.setParentId(parentId);
                                }
                            }
                            if (tabletFeatureUseYn != null && !tabletFeatureUseYn.isBlank()) {
                                String prevTf = ou.getTabletFeatureUseYn() != null && "Y".equalsIgnoreCase(ou.getTabletFeatureUseYn().trim()) ? "Y" : "N";
                                String nextTf = "Y".equalsIgnoreCase(tabletFeatureUseYn.trim()) ? "Y" : "N";
                                if (!nextTf.equals(prevTf)) {
                                    persistSingleOrgFieldChange(ou, "태블릿 UI 기능", prevTf, nextTf);
                                }
                                ou.setTabletFeatureUseYn(nextTf);
                            }
                            orgUnitRepository.save(ou);
                            String effDivForCommission = childLevel.name();
                            if (commissionConfigAllowed != null) mp.setCommissionConfigAllowed(commissionConfigAllowed);
                            if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
                            if (childLevel == OrgLevel.MERCHANT && webPaymentHeaderLogoMode != null && !webPaymentHeaderLogoMode.isBlank()) {
                                mp.setWebPaymentHeaderLogoMode(
                                        com.pg.urlpay.WebPaymentHeaderLogoModeUtil.normalizeMerchantStored(webPaymentHeaderLogoMode));
                            }
                            if (childLevel == OrgLevel.MERCHANT && webPaymentHeaderLogoUrl != null) {
                                String wpLogo = webPaymentHeaderLogoUrl.trim();
                                if (wpLogo.isEmpty()) {
                                    mp.setWebPaymentHeaderLogoUrl(null);
                                } else if (wpLogo.length() > 500) {
                                    throw new IllegalArgumentException("웹결제 상단 로고 URL은 500자 이하여야 합니다.");
                                } else {
                                    mp.setWebPaymentHeaderLogoUrl(wpLogo);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && webPaymentHeaderHtmlTitle != null) {
                                String htmlTitle = webPaymentHeaderHtmlTitle.trim();
                                if (htmlTitle.isEmpty()) {
                                    mp.setWebPaymentHeaderHtmlTitle(null);
                                } else if (htmlTitle.length() > 20) {
                                    throw new IllegalArgumentException("웹결제 HTML 표시명은 20자 이하여야 합니다.");
                                } else {
                                    mp.setWebPaymentHeaderHtmlTitle(htmlTitle);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && webPaymentHeaderSubtitleMode != null && !webPaymentHeaderSubtitleMode.isBlank()) {
                                /* FOLLOW_HQ 유지 — normalize()는 FOLLOW_HQ→DEFAULT 로 바꿔 본사 경고문구가 무시됨 */
                                String wpSubMode = com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.normalizeMerchantStored(webPaymentHeaderSubtitleMode);
                                mp.setWebPaymentHeaderSubtitleMode(wpSubMode);
                                if (com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.isPreset(wpSubMode)) {
                                    mp.setWebPaymentHeaderSubtitleText(null);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && webPaymentHeaderSubtitleText != null
                                    && com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.isDirectActive(mp.getWebPaymentHeaderSubtitleMode())) {
                                String wpSub = webPaymentHeaderSubtitleText.trim();
                                if (wpSub.isEmpty()) {
                                    mp.setWebPaymentHeaderSubtitleText(null);
                                } else if (wpSub.length() > 200) {
                                    throw new IllegalArgumentException("웹결제 경고메세지는 200자 이하여야 합니다.");
                                } else {
                                    mp.setWebPaymentHeaderSubtitleText(wpSub);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT) {
                                applyMerchantUrlPayCheckoutMode(mp, ou.getId(), urlPayCheckoutMode);
                                applyMerchantUrlPayPresentationOptions(mp,
                                        urlPayProductNameUseYn, urlPayCompanyNameShowYn, urlPayLangMenuUseYn,
                                        urlPayShippingAddressUseYn, checkoutContactRememberMode);
                                applyMerchantUrlPayInputMode(mp, urlPayInputMode);
                                applyMerchantUrlPayCardExpiryMode(mp, urlPayCardExpiryMode);
                                applyMerchantCardAuthMode(mp, cardAuthMode);
                                applyMerchantApiUrlPayCheckoutMode(mp, ou.getId(), apiUrlPayCheckoutMode);
                                applyMerchantChatbotUrlPayCheckoutMode(mp, ou.getId(), chatbotUrlPayCheckoutMode);
                                applyMerchantJpayCheckoutFieldMode(mp, jpayCheckoutFieldMode);
                                applyMerchantJpayPhoneDialCodeYn(mp, jpayPhoneDialCodeYn);
                            }
                            if (childLevel == OrgLevel.MERCHANT && apiJpaySubscriptionUseYn != null && !apiJpaySubscriptionUseYn.trim().isEmpty()) {
                                mp.setApiJpaySubscriptionUseYn(apiJpaySubscriptionUseYn.trim());
                            }
                            applyMerchantApiIntegrationChannels(mp, childLevel,
                                    apiBrokerInlineUseYn, apiBrokerRedirectUseYn, apiWordpressUseYn);
                            if (childLevel == OrgLevel.MERCHANT && chatbotPaymentUseYn != null && !chatbotPaymentUseYn.trim().isEmpty()) {
                                mp.setChatbotPaymentUseYn("Y".equalsIgnoreCase(chatbotPaymentUseYn.trim()) ? "Y" : "N");
                                if ("N".equalsIgnoreCase(mp.getChatbotPaymentUseYn() != null ? mp.getChatbotPaymentUseYn().trim() : "")) {
                                    mp.setChatbotProductSlotLimit(null);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotProductSlotLimit != null) {
                                if (chatbotProductSlotLimit <= 0) {
                                    mp.setChatbotProductSlotLimit(null);
                                } else if (!ChatbotProductPricingUtil.isAllowedSlot(chatbotProductSlotLimit)) {
                                    throw new IllegalArgumentException(
                                            "챗봇 상품 등록 한도는 " + ChatbotProductPricingUtil.ALLOWED_SLOTS + " 중 하나만 선택할 수 있습니다.");
                                } else {
                                    long pc = merchantChatbotProductService.countProductsForMerchant(ou.getId());
                                    long activeY = merchantChatbotProductService.countSaleActiveProductsForMerchant(ou.getId());
                                    int regMax = chatbotProductSlotLimit + ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS;
                                    int extra = ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS;
                                    if (pc > regMax) {
                                        throw new IllegalArgumentException(
                                                "등록된 상품이 " + pc + "건으로, 선택한 플랜 기준 최대 등록(" + regMax + "건, 판매 활성 "
                                                        + chatbotProductSlotLimit + "+미판매 보관 " + extra + ")을 초과합니다. 상품을 삭제한 뒤 변경하세요.");
                                    }
                                    if (activeY > chatbotProductSlotLimit) {
                                        throw new IllegalArgumentException(
                                                "판매 활성 상품이 " + activeY + "건인데 선택한 플랜의 판매 활성 상한은 "
                                                        + chatbotProductSlotLimit
                                                        + "건입니다. 상품관리에서 판매 활성(사용)을 줄인 뒤 변경하세요.");
                                    }
                                    mp.setChatbotProductSlotLimit(chatbotProductSlotLimit);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT
                                    && mp.getChatbotPaymentUseYn() != null
                                    && "Y".equalsIgnoreCase(mp.getChatbotPaymentUseYn().trim())) {
                                if (mp.getChatbotProductSlotLimit() == null || mp.getChatbotProductSlotLimit() <= 0) {
                                    mp.setChatbotProductSlotLimit(10);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotHeaderLogoUrl != null) {
                                String hLogo = chatbotHeaderLogoUrl.trim();
                                if (hLogo.isEmpty()) {
                                    mp.setChatbotHeaderLogoUrl(null);
                                } else if (hLogo.length() > 500) {
                                    throw new IllegalArgumentException("챗봇 상단 로고 URL은 500자 이하여야 합니다.");
                                } else {
                                    mp.setChatbotHeaderLogoUrl(hLogo);
                                }
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotAdminUsername != null) {
                                String cab = chatbotAdminUsername.trim();
                                if (cab.isEmpty()) {
                                    mp.setChatbotAdminUserId(null);
                                } else {
                                    AppUser cabUser = resolveOrCreateChatbotAdminUser(ou, cab);
                                    String occ = cabUser.getOrgUnitCode() != null ? cabUser.getOrgUnitCode().trim() : "";
                                    String ccode = ou.getCode() != null ? ou.getCode().trim() : "";
                                    if (!occ.equalsIgnoreCase(ccode)) {
                                        throw new IllegalArgumentException("챗봇 관리자는 해당 가맹점 소속 사용자만 지정할 수 있습니다.");
                                    }
                                    if (!cabUser.isEnabled()) {
                                        throw new IllegalArgumentException("챗봇 관리자로 지정할 수 없는 계정입니다.");
                                    }
                                    String ustat = cabUser.getUserStatus();
                                    if (ustat != null && !ustat.isBlank() && !"ACTIVE".equalsIgnoreCase(ustat.trim())) {
                                        throw new IllegalArgumentException("챗봇 관리자로 지정할 수 없는 계정입니다.");
                                    }
                                    mp.setChatbotAdminUserId(cabUser.getId());
                                }
                            }
                            if (baseCurrency != null && !baseCurrency.trim().isEmpty()) {
                                String divForVal = childLevel.name();
                                validateBaseCurrency(divForVal, baseCurrency);
                                if ("MASTER_DIST".equalsIgnoreCase(divForVal)) {
                                    Long pid = parentId != null ? parentId : ou.getParentId();
                                    validateMasterDistBaseCurrencyAgainstRegionalParent(pid, baseCurrency.trim());
                                }
                                mp.setBaseCurrency(baseCurrency.trim());
                            } else if (childLevel == OrgLevel.MERCHANT
                                    && (mp.getBaseCurrency() == null || mp.getBaseCurrency().isBlank())) {
                                Long effPid = parentId != null ? parentId : ou.getParentId();
                                String inh = resolveInheritedBaseCurrencyForMerchant(effPid);
                                if (inh != null && !inh.isBlank()) {
                                    mp.setBaseCurrency(inh);
                                }
                            }
                            if (siteUrl != null) mp.setSiteUrl(siteUrl.trim());
                            if (siteSummary != null) mp.setSiteSummary(siteSummary.trim());
                            mp.setCompTel(compTel);
                            mp.setZipCode(zipCode);
                            mp.setAddr(addr);
                            mp.setAddrDetail(addrDetail);
                            if (addrEtc != null) mp.setAddrEtc(addrEtc.trim());
                            if (addrCountryCd != null) mp.setAddrCountryCd(addrCountryCd.trim());
                            mp.setCeoNm(ceoNm);
                            mp.setCeoMobile(ceoMobile);
                            if (useYn != null) {
                                applyOrgUseYnChange(ou, mp, useYn);
                            }
                            String prevLoginId = mp.getLoginId() != null ? mp.getLoginId().trim() : "";
                            if (loginId != null) {
                                String nextLoginId = loginId.trim();
                                if (!nextLoginId.isEmpty()) {
                                    Optional<AppUser> conflict = userRepository.findByUsername(nextLoginId);
                                    if (conflict.isPresent() && (prevLoginId.isEmpty() || !nextLoginId.equalsIgnoreCase(prevLoginId))) {
                                        throw new IllegalArgumentException("이미 사용 중인 로그인ID입니다: " + nextLoginId);
                                    }
                                    mp.setLoginId(nextLoginId);
                                    if (!prevLoginId.isEmpty() && !nextLoginId.equalsIgnoreCase(prevLoginId)) {
                                        userRepository.findByUsername(prevLoginId).ifPresent(u -> {
                                            u.setUsername(nextLoginId);
                                            userRepository.save(u);
                                        });
                                    }
                                    prevLoginId = nextLoginId;
                                } else {
                                    mp.setLoginId("");
                                    prevLoginId = "";
                                }
                            }
                            mp.setRegNo(regNo);
                            mp.setBizType(bizType);
                            mp.setIndustry(industry);
                            if (bizNature != null) mp.setBizNature(bizNature);
                            if (product != null) mp.setProduct(product);
                            if (homepage != null) mp.setHomepage(homepage);
                            if (settleName != null) mp.setSettleName(settleName);
                            if (settleTelNo != null) mp.setSettleTelNo(settleTelNo);
                            mp.setFax(fax);
                            mp.setEmail(email);
                            mp.setBankCd(bankCd);
                            mp.setTransferFee(transferFee);
                            if (cryptoTransferFee != null) {
                                mp.setCryptoTransferFee(cryptoTransferFee.trim());
                            }
                            mp.setAccountNo(accountNo);
                            mp.setAccountHolder(accountHolder);
                            mp.setRemark(remark);
                            String mergedSettings = mergeRegionalSettings(mp.getRegionalSettings(), regionalSettings,
                                    assistantLoginId, assistantPwd, assistantRoleType, brandingEditAllowedYn);
                            if (mergedSettings != null && !mergedSettings.isBlank()) {
                                Map<String, Object> mergedMap = parseRegionalSettings(mergedSettings);
                                if (ou.getOrgLevel() == OrgLevel.REGIONAL) {
                                    // 총본사가 본사의 영업일을 지정한 경우, 하위(총판) 개별 설정 잠금 정책 활성화
                                    if (incomingHasHolidaySetting(regionalSettings)
                                            && resolveCurrentActorOrgLevel().orElse(null) == OrgLevel.HEADQUARTERS) {
                                        mergedMap.put("holidayManagedByHeadquartersYn", "Y");
                                    }
                                } else if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                                    // 상위 본사가 총본사에 의해 지정된 상태면 총판 영업일은 상속 강제(개별 저장 무효화)
                                    if (isParentRegionalHolidayManagedByHeadquarters(ou.getParentId())) {
                                        clearHolidayKeys(mergedMap);
                                        mergedMap.putAll(resolveInheritedHolidaySettings(ou.getParentId()));
                                        mergedMap.put("holidayLockedByHeadquartersYn", "Y");
                                    } else {
                                        mergedMap.remove("holidayLockedByHeadquartersYn");
                                    }
                                }
                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                    mp.setRegionalSettings(om.writeValueAsString(mergedMap));
                                } catch (Exception ignored) {
                                    mp.setRegionalSettings(mergedSettings);
                                }
                            }
                            if (pwd != null && !pwd.trim().isEmpty()) {
                                if (prevLoginId.isEmpty()) {
                                    throw new IllegalArgumentException("대표 아이디가 없어 비밀번호를 변경할 수 없습니다.");
                                }
                                String encoded = passwordEncoder.encode(pwd.trim());
                                mp.setPwd(encoded);
                                final String primaryLoginId = prevLoginId;
                                AppUser primary = userRepository.findByUsername(primaryLoginId).orElseGet(() -> {
                                    AppUser nu = new AppUser();
                                    nu.setUsername(primaryLoginId);
                                    nu.setName(ou.getName() != null && !ou.getName().isBlank() ? ou.getName().trim() : primaryLoginId);
                                    nu.setRole("USER");
                                    nu.setEnabled(true);
                                    nu.setOrgUnitCode(ou.getCode());
                                    nu.setPermissionGroupNm("업체사용자");
                                    nu.setOtpRegisteredYn("N");
                                    nu.setPasswordMustChangeYn("N");
                                    return nu;
                                });
                                primary.setPassword(encoded);
                                primary.setOrgUnitCode(ou.getCode());
                                primary.setPermissionGroupNm("업체사용자");
                                primary.setOtpRegisteredYn("N");
                                primary.setPasswordMustChangeYn("N");
                                primary.setRole("USER");
                                primary.setEnabled(true);
                                userRepository.save(primary);
                            }
                            if (usesCommissionPolicyForCompDiv(effDivForCommission)
                                    && !allCommissionParamsAbsent(commissionFollowHq, hqPolicyScope, perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate,
                                    failFee, payRate, refundRate, rollingPct, rollingDays, feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx,
                                    fee3dsRate, chargebackFeePerTx, chargebackPolicyId, voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode)) {
                                mergeCommissionUiIntoRegionalSettings(mp, commissionFollowHq, hqPolicyScope);
                            }
                            if (childLevel == OrgLevel.MERCHANT) {
                                mergeMerchantPayFollowFromRequest(mp, payFollowMerchantUseYn, payFollowAutoVoidYn,
                                        payFollowEmailVoidYn, payFollowAutoRefundYn, payFollowForceRefundYn);
                                mergeMerchantCardRiskIfAny(mp, cardRiskPolicyMode,
                                        cardRiskTier1Hours, cardRiskTier1Min,
                                        cardRiskTier2Hours, cardRiskTier2Min,
                                        cardRiskTier3Hours, cardRiskTier3Min,
                                        cardRiskTier4Hours, cardRiskTier4Min,
                                        cardRiskAutoBlacklistTier,
                                        cardRiskPresaleMode,
                                        cardRiskPresaleBuyerMismatchYn,
                                        cardRiskPresaleHolderNameYn,
                                        cardRiskPresalePhoneInvalidYn,
                                        cardRiskPresaleEmailInvalidYn,
                                        cardRiskPresaleVelocityCardYn,
                                        cardRiskPresaleVelocityEmailYn,
                                        cardRiskPresaleVelocityIpYn,
                                        cardRiskPresaleVelCardWinMin,
                                        cardRiskPresaleVelCardMax,
                                        cardRiskPresaleVelEmailWinMin,
                                        cardRiskPresaleVelEmailMax,
                                        cardRiskPresaleVelIpWinMin,
                                        cardRiskPresaleVelIpMax);
                                applyMerchantUrlPayAlerts(mp, urlPayAlertEmailYn, urlPayLineNotifyToken);
                                applyMerchantReceiptEmail(mp, receiptEmailFollowHqYn, receiptEmailUseYn);
                                applyMerchantSplitPay(mp, spEnabledYn, spContractCancelYn,
                                        spIntervalMonthYn,
                                        spIntervalDayYn, spIntervalMultiYn, spDayIntervalDays,
                                        spMonthIntervalMonths, spMultiMaxMonths, spFirstPayMode);
                                applyMerchantSplitPayCheckoutPresentation(mp, spHeaderLogoMode, spHeaderLogoUrl,
                                        spHeaderHtmlTitle, spHeaderSubtitleMode, spHeaderSubtitleText, spLangMenuUseYn);
                            }
                            merchantProfileRepository.save(mp);
                            if (childLevel == OrgLevel.MASTER_DIST) {
                                applyMasterDistReceiptEmailPolicy(ou.getId(), receiptEmailEnabledYn);
                            }
                            if (assistantLoginId != null && !assistantLoginId.trim().isEmpty()) {
                                String aid = assistantLoginId.trim();
                                final String representativeLoginId = prevLoginId;
                                Optional<AppUser> existed = userRepository.findByUsername(aid);
                                if (existed.isPresent() && (existed.get().getOrgUnitCode() == null
                                        || !ou.getCode().equalsIgnoreCase(existed.get().getOrgUnitCode()))) {
                                    throw new IllegalArgumentException("이미 사용 중인 보조 아이디입니다: " + aid);
                                }
                                AppUser au = existed.orElseGet(() -> {
                                    if (assistantPwd == null || assistantPwd.trim().isEmpty()) {
                                        throw new IllegalArgumentException("보조 아이디 신규 생성 시 비밀번호가 필요합니다.");
                                    }
                                    AppUser nu = new AppUser();
                                    nu.setUsername(aid);
                                    nu.setName((ou.getName() != null ? ou.getName() : aid) + " 보조");
                                    nu.setRole("USER");
                                    nu.setEnabled(true);
                                    nu.setOrgUnitCode(ou.getCode());
                                    nu.setPermissionGroupNm(permissionGroupByAssistantRole(assistantRoleType));
                                    nu.setOtpRegisteredYn("N");
                                    nu.setUserType("ASSISTANT");
                                    nu.setAssistantRoleType(normalizeAssistantRoleType(assistantRoleType));
                                    nu.setParentUsername(representativeLoginId.isEmpty() ? null : representativeLoginId);
                                    return nu;
                                });
                                au.setOrgUnitCode(ou.getCode());
                                au.setPermissionGroupNm(permissionGroupByAssistantRole(assistantRoleType));
                                au.setOtpRegisteredYn("N");
                                au.setRole("USER");
                                au.setEnabled(true);
                                au.setUserType("ASSISTANT");
                                au.setAssistantRoleType(normalizeAssistantRoleType(assistantRoleType));
                                au.setParentUsername(representativeLoginId.isEmpty() ? null : representativeLoginId);
                                if (assistantPwd != null && !assistantPwd.trim().isEmpty()) {
                                    au.setPassword(passwordEncoder.encode(assistantPwd.trim()));
                                    au.setPasswordMustChangeYn("N");
                                }
                                if (au.getPassword() == null || au.getPassword().isBlank()) {
                                    throw new IllegalArgumentException("보조 아이디 비밀번호를 입력하세요.");
                                }
                                userRepository.save(au);
                            }
                            if ("MASTER_DIST".equalsIgnoreCase(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "") && (notifyUrl1 != null || notifyUrl2 != null || notifyUrl3 != null || notifyUrl4 != null)) {
                                saveDistributorNotifyUrls(ou.getId(), notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4);
                            }
                            if ("MERCHANT".equalsIgnoreCase(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "") && pgBindings != null && !pgBindings.trim().isEmpty()) {
                                try {
                                    List<Map<String, Object>> list = PG_BINDINGS_OBJECT_MAPPER.readValue(pgBindings.trim(),
                                            new TypeReference<List<Map<String, Object>>>() {});
                                    list = dedupeMerchantPgBindingJsonRows(list);
                                    validateMerchantPgBindingJsonRows(list);
                                    Map<String, String> prevPgPricingModes = snapshotMerchantPgBindingPricingModes(ou.getId());
                                    Map<String, String[]> prevPgSecrets = snapshotMerchantPgBindingSecrets(ou.getId());
                                    merchantPgBindingRepository.deleteByOrgUnitId(ou.getId());
                                    int order = 0;
                                    for (Map<String, Object> m : list) {
                                        String pc = m.get("pgCd") != null ? m.get("pgCd").toString().trim() : "";
                                        if (pc.isEmpty()) continue;
                                        MerchantPgBinding binding = new MerchantPgBinding();
                                        binding.setOrgUnitId(ou.getId());
                                        binding.setPgCd(pc);
                                        binding.setActivationYn("Y".equalsIgnoreCase(optStr(m, "activationYn")) ? "Y" : "N");
                                        binding.setOperationalYn("Y".equalsIgnoreCase(optStr(m, "operationalYn")) ? "Y" : "N");
                                        String pm = optStr(m, "payMethod") != null && !optStr(m, "payMethod").isEmpty() ? optStr(m, "payMethod") : "WEB";
                                        binding.setPayMethod(pm);
                                        binding.setRootNo(optStr(m, "rootNo"));
                                        applyMerchantPgBindingCredentialsFromJson(binding, pc, pm, m, prevPgSecrets);
                                        binding.setInstallmentYn("Y".equalsIgnoreCase(optStr(m, "installmentYn")) ? "Y" : "N");
                                        String maxMo = optStr(m, "maxInstallmentMonths");
                                        if (maxMo != null && !maxMo.isEmpty()) {
                                            try { binding.setMaxInstallmentMonths(Integer.parseInt(maxMo.trim())); } catch (NumberFormatException ignored) {}
                                        }
                                        binding.setSortOrder(order++);
                                        applyUrlPayPricingModeFromJsonOrPrevious(binding, pc, pm, optStr(m, "urlPayPricingMode"), prevPgPricingModes);
                                        binding.setCardBrandScope(resolveMerchantPgCardBrandScopeForSave(pc, optStr(m, "cardBrandScope")));
                                        binding.setCurrencyScope(resolveMerchantPgCurrencyScopeForSave(pc, optStr(m, "currencyScope")));
                                        applyExtSettlementFromJsonMap(binding, m);
                                        merchantPgBindingRepository.save(binding);
                                    }
                                } catch (JsonProcessingException e) {
                                    throw new IllegalArgumentException("결제대행사(JSON) 형식이 올바르지 않습니다.", e);
                                }
                            }
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT && syncMerchantWebPaymentUseYnIfNoUrlPayBinding(ou.getId(), mp)) {
                                merchantProfileRepository.save(mp);
                            }
                            Map<String, Object> rsForCommission = parseRegionalSettings(mp.getRegionalSettings());
                            String effectiveFollow = (commissionFollowHq != null && !commissionFollowHq.trim().isEmpty())
                                    ? commissionFollowHq
                                    : String.valueOf(rsForCommission.getOrDefault("commissionFollowHq", "Y"));
                            String effectiveHqScope = (hqPolicyScope != null) ? hqPolicyScope
                                    : String.valueOf(rsForCommission.getOrDefault("hqPolicyScope", ""));
                            if ("MERCHANT".equalsIgnoreCase(effDivForCommission)) {
                                String chosenCur = (baseCurrency != null && !baseCurrency.trim().isEmpty())
                                        ? baseCurrency.trim()
                                        : mp.getBaseCurrency();
                                Long effectiveParentId = parentId != null ? parentId : ou.getParentId();
                                String effectiveChargebackPolicyId = chargebackPolicyId;
                                if (effectiveChargebackPolicyId == null || effectiveChargebackPolicyId.trim().isEmpty()) {
                                    CommissionPolicy curPolicy = commissionPolicyRepository.findByScope(ou.getCode()).orElse(null);
                                    if (curPolicy != null && curPolicy.getChargebackPolicyId() != null) {
                                        effectiveChargebackPolicyId = String.valueOf(curPolicy.getChargebackPolicyId());
                                    }
                                }
                                validateMerchantBaseCurrencyAgainstParent(effectiveParentId, chosenCur);
                                validateMerchantPolicyCurrencyCompatibility(chosenCur, effectiveFollow, effectiveHqScope, effectiveChargebackPolicyId);
                            }
                            if (usesCommissionPolicyForCompDiv(effDivForCommission)
                                    && !allCommissionParamsAbsent(commissionFollowHq, hqPolicyScope, perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate,
                                    failFee, payRate, refundRate, rollingPct, rollingDays, feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx,
                                    fee3dsRate, chargebackFeePerTx, chargebackPolicyId, voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode)) {
                                boolean followY = !"N".equalsIgnoreCase(effectiveFollow != null ? effectiveFollow.trim() : "");
                                String normPrevFollow = "N".equalsIgnoreCase(prevCommissionFollow) ? "N" : "Y";
                                String normEffFollow = followY ? "Y" : "N";
                                boolean followChanged = !normPrevFollow.equals(normEffFollow);
                                String effScopeTrim = effectiveHqScope != null ? effectiveHqScope.trim() : "";
                                boolean scopeChanged = !prevHqPolicyScope.equals(effScopeTrim);
                                boolean feeScalarsPresent = !allCommissionFeeScalarParamsAbsent(
                                        perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate,
                                        failFee, payRate, refundRate, rollingPct, rollingDays, feeSettlementPerTx,
                                        remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx,
                                        fee3dsRate, chargebackFeePerTx, chargebackPolicyId,
                                        voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode);
                                /*
                                 * 본사정책 따름(Y): 따름/정책선택 변경 시에만 HQ 템플릿으로 배분·정책을 재적용.
                                 * 업체명 등 일반 저장 시 수수료관리에서 조정한 본사 요율% 등이 템플릿(빈→0)으로 초기화되지 않게 한다.
                                 * 직접입력(N): 수수료 스칼라가 요청에 있거나 따름 모드가 바뀔 때만 반영.
                                 */
                                boolean shouldApplyCommission = followY
                                        ? (followChanged || scopeChanged)
                                        : (followChanged || feeScalarsPresent);
                                if (shouldApplyCommission) {
                                    applyCommissionPolicyForOrgCode(ou.getCode(), effDivForCommission, effectiveFollow, effectiveHqScope,
                                            perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                                            feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx, fee3dsRate, chargebackFeePerTx, chargebackPolicyId,
                                            voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode,
                                            followChanged || scopeChanged);
                                }
                            }
                            if ("MERCHANT".equalsIgnoreCase(effDivForCommission) && chargebackPolicyId != null) {
                                applyMerchantIndependentChargebackPolicy(ou.getCode(), chargebackPolicyId);
                            }
                            boolean pwdChanged = pwd != null && !pwd.trim().isEmpty();
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                saveMerchantDefaultProductOrClear(ou.getId(), defaultProductName, defaultProductCode,
                                        defaultProductAmount, defaultProductDesc);
                                String[] mwMerge = mergeMiddlewareNotifyParamsIfOmittedOnUpdate(ou.getId(),
                                        middlewareNotifyUrl, middlewareNotifySecret);
                                String[] jpayMerge = mergeJpayNotifyParamsIfOmittedOnUpdate(ou.getId(),
                                        jpayNotifyUrl, jpayCallbackUrl);
                                saveMerchantPayNotifyUrls(ou.getId(), notifyUrlBackground, notifyUrlResult,
                                        mwMerge[0], mwMerge[1], jpayMerge[0], jpayMerge[1]);
                            }
                            if (chatbotCatalogListingGrant != null && childLevel != OrgLevel.MERCHANT) {
                                merchantChatbotKbService.applyCatalogListingGrant(mp, chatbotCatalogListingGrant);
                            }
                            if (chatbotMaxProductImagesGrant != null && childLevel != OrgLevel.MERCHANT) {
                                merchantChatbotKbService.applyCatalogMaxProductImages(mp, chatbotMaxProductImagesGrant);
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotCatalogListingEnabled != null) {
                                merchantChatbotKbService.applyCatalogListingEnabled(mp, chatbotCatalogListingEnabled);
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotPromotionShelfMode != null
                                    && !chatbotPromotionShelfMode.isBlank()) {
                                mp.setChatbotPromotionShelfMode(
                                        ChatbotPromotionShelfMode.resolveStored(chatbotPromotionShelfMode).name());
                            }
                            if (childLevel == OrgLevel.MERCHANT && chatbotPromotionRotateSeconds != null) {
                                mp.setChatbotPromotionRotateSeconds(
                                        ChatbotPromotionShelfMode.normalizeRotateSeconds(chatbotPromotionRotateSeconds));
                            }
                            persistMerchantAuditDiff(snap, ou, mp, pwdChanged);
                            return true;
                        }))
                .orElse(false);
    }

    /**
     * 가맹점 온라인 URL 결제용 기본상품. 값이 모두 비면 행 삭제.
     */
    private void saveMerchantDefaultProductOrClear(Long orgUnitId,
                                                   String defaultProductName, String defaultProductCode,
                                                   String defaultProductAmount, String defaultProductDesc) {
        boolean hasAny = (defaultProductName != null && !defaultProductName.trim().isEmpty())
                || (defaultProductCode != null && !defaultProductCode.trim().isEmpty())
                || (defaultProductAmount != null && !defaultProductAmount.trim().isEmpty())
                || (defaultProductDesc != null && !defaultProductDesc.trim().isEmpty());
        java.util.Optional<MerchantDefaultProduct> existing = merchantDefaultProductRepository.findByOrgUnitId(orgUnitId);
        if (!hasAny) {
            existing.ifPresent(merchantDefaultProductRepository::delete);
            return;
        }
        MerchantDefaultProduct dp = existing.orElseGet(() -> {
            MerchantDefaultProduct x = new MerchantDefaultProduct();
            x.setOrgUnitId(orgUnitId);
            return x;
        });
        dp.setProductName(defaultProductName != null && !defaultProductName.trim().isEmpty() ? defaultProductName.trim() : null);
        dp.setProductCode(defaultProductCode != null && !defaultProductCode.trim().isEmpty() ? defaultProductCode.trim() : null);
        if (defaultProductAmount != null && !defaultProductAmount.trim().isEmpty()) {
            try {
                dp.setDefaultAmount(new java.math.BigDecimal(defaultProductAmount.trim()));
            } catch (NumberFormatException ignored) {
                dp.setDefaultAmount(null);
            }
        } else {
            dp.setDefaultAmount(null);
        }
        dp.setProductDesc(defaultProductDesc != null && !defaultProductDesc.trim().isEmpty() ? defaultProductDesc.trim() : null);
        merchantDefaultProductRepository.save(dp);
    }

    /** 플레이스홀더만 있는 값은 미등록으로 간주 */
    private static String normalizeMerchantPayNotifyUrl(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        if (t.isEmpty()) return "";
        String lower = t.toLowerCase();
        if ("https://".equals(lower) || "http://".equals(lower)) return "";
        return t;
    }

    /**
     * 가맹점 결제통보 URL Background/Result. 기존 행을 지운 뒤 재삽입하여 (org_unit_id, url_type) 중복을 방지.
     */
    private static final String MERCHANT_NOTIFY_MIDDLEWARE = "MIDDLEWARE";
    private static final String URL_PAY_LINE_TOKEN_CLEAR = "__CLEAR__";
    private static final int URL_PAY_LINE_TOKEN_MAX_LEN = 256;

    private void applyMerchantUrlPayAlerts(MerchantProfile mp, String urlPayAlertEmailYn, String urlPayLineNotifyToken) {
        if (mp == null) {
            return;
        }
        if (urlPayAlertEmailYn != null && !urlPayAlertEmailYn.isBlank()) {
            mp.setUrlPayAlertEmailYn("Y".equalsIgnoreCase(urlPayAlertEmailYn.trim()) ? "Y" : "N");
        }
        if (urlPayLineNotifyToken == null) {
            return;
        }
        String raw = urlPayLineNotifyToken.trim();
        if (raw.isEmpty()) {
            return;
        }
        if (URL_PAY_LINE_TOKEN_CLEAR.equalsIgnoreCase(raw)) {
            mp.setUrlPayLineNotifyToken(null);
            return;
        }
        if (raw.length() > URL_PAY_LINE_TOKEN_MAX_LEN) {
            throw new IllegalArgumentException("LINE Notify 토큰은 " + URL_PAY_LINE_TOKEN_MAX_LEN + "자 이하여야 합니다.");
        }
        mp.setUrlPayLineNotifyToken(raw);
    }

    private void applyMerchantReceiptEmail(MerchantProfile mp, String receiptEmailFollowHqYn, String receiptEmailUseYn) {
        if (mp == null) {
            return;
        }
        if (receiptEmailFollowHqYn != null && !receiptEmailFollowHqYn.isBlank()) {
            mp.setReceiptEmailFollowHqYn("N".equalsIgnoreCase(receiptEmailFollowHqYn.trim()) ? "N" : "Y");
        }
        if (receiptEmailUseYn != null && !receiptEmailUseYn.isBlank()) {
            mp.setReceiptEmailUseYn("Y".equalsIgnoreCase(receiptEmailUseYn.trim()) ? "Y" : "N");
        }
    }

    private void applyMasterDistReceiptEmailPolicy(long orgUnitId, String receiptEmailEnabledYn) {
        if (receiptEmailEnabledYn == null) {
            return;
        }
        String v = receiptEmailEnabledYn.trim();
        settlementSettingRepository.findByOrgUnitId(orgUnitId).ifPresent(ss -> {
            if (v.isEmpty()) {
                ss.setReceiptEmailEnabledYn(null);
            } else {
                ss.setReceiptEmailEnabledYn("Y".equalsIgnoreCase(v) ? "Y" : "N");
            }
            settlementSettingRepository.save(ss);
        });
    }

    /**
     * 가맹 URL 분할결제만 저장(대용량 /api/comp/update 와 분리).
     * 총본사·본사·총판 직권 변경이 폼 파라미터 누락으로 미반영되는 문제를 방지한다.
     */
    @Transactional
    public boolean updateMerchantSplitPayOnly(String compId,
                                              String merchantSplitPayJson,
                                              String splitPayEnabledYn,
                                              String splitPayContractCancelYn,
                                              String splitPayIntervalMonthYn,
                                              String splitPayIntervalDayYn,
                                              String splitPayIntervalMultiYn,
                                              String splitPayDayIntervalDays,
                                              String splitPayMonthIntervalMonths,
                                              String splitPayMultiMaxMonths,
                                              String splitPayFirstPayMode,
                                              String splitPayHeaderLogoMode,
                                              String splitPayHeaderLogoUrl,
                                              String splitPayHeaderHtmlTitle,
                                              String splitPayHeaderSubtitleMode,
                                              String splitPayHeaderSubtitleText,
                                              String splitPayLangMenuUseYn) {
        String code = compId != null ? compId.trim() : "";
        if (code.isEmpty()) {
            return false;
        }
        String[] merged = mergeMerchantSplitPayParamsFromJson(merchantSplitPayJson,
                splitPayEnabledYn, splitPayContractCancelYn,
                splitPayIntervalMonthYn, splitPayIntervalDayYn, splitPayIntervalMultiYn,
                splitPayDayIntervalDays, splitPayMonthIntervalMonths, splitPayMultiMaxMonths, splitPayFirstPayMode,
                splitPayHeaderLogoMode, splitPayHeaderLogoUrl, splitPayHeaderHtmlTitle,
                splitPayHeaderSubtitleMode, splitPayHeaderSubtitleText, splitPayLangMenuUseYn);
        return orgUnitRepository.findByCode(code)
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            applyMerchantSplitPay(mp, merged[0], merged[1],
                                    merged[2], merged[3], merged[4],
                                    merged[5], merged[6], merged[7], merged[8]);
                            applyMerchantSplitPayCheckoutPresentation(mp, merged[9], merged[10],
                                    merged[11], merged[12], merged[13], merged[14]);
                            merchantProfileRepository.saveAndFlush(mp);
                            return true;
                        }))
                .orElse(false);
    }

    /**
     * 개별 splitPay* RequestParam 이 누락됐을 때 merchantSplitPayJson 압축 백업으로 복원.
     * 순서: enabled, contractCancel, monthYn, dayYn, multiYn, dayDays, monthMonths, multiMax, firstPay,
     *       headerLogoMode, headerLogoUrl, headerHtmlTitle, headerSubtitleMode, headerSubtitleText, langMenuUseYn
     */
    private static String[] mergeMerchantSplitPayParamsFromJson(String merchantSplitPayJson,
                                                                String splitPayEnabledYn,
                                                                String splitPayContractCancelYn,
                                                                String splitPayIntervalMonthYn,
                                                                String splitPayIntervalDayYn,
                                                                String splitPayIntervalMultiYn,
                                                                String splitPayDayIntervalDays,
                                                                String splitPayMonthIntervalMonths,
                                                                String splitPayMultiMaxMonths,
                                                                String splitPayFirstPayMode,
                                                                String splitPayHeaderLogoMode,
                                                                String splitPayHeaderLogoUrl,
                                                                String splitPayHeaderHtmlTitle,
                                                                String splitPayHeaderSubtitleMode,
                                                                String splitPayHeaderSubtitleText,
                                                                String splitPayLangMenuUseYn) {
        String[] out = new String[] {
                splitPayEnabledYn, splitPayContractCancelYn,
                splitPayIntervalMonthYn, splitPayIntervalDayYn, splitPayIntervalMultiYn,
                splitPayDayIntervalDays, splitPayMonthIntervalMonths, splitPayMultiMaxMonths, splitPayFirstPayMode,
                splitPayHeaderLogoMode, splitPayHeaderLogoUrl, splitPayHeaderHtmlTitle,
                splitPayHeaderSubtitleMode, splitPayHeaderSubtitleText, splitPayLangMenuUseYn
        };
        if (merchantSplitPayJson == null || merchantSplitPayJson.isBlank()) {
            return out;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> pack = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(merchantSplitPayJson.trim(), Map.class);
            String[] keys = {
                    "splitPayEnabledYn", "splitPayContractCancelYn",
                    "splitPayIntervalMonthYn", "splitPayIntervalDayYn", "splitPayIntervalMultiYn",
                    "splitPayDayIntervalDays", "splitPayMonthIntervalMonths", "splitPayMultiMaxMonths", "splitPayFirstPayMode",
                    "splitPayHeaderLogoMode", "splitPayHeaderLogoUrl", "splitPayHeaderHtmlTitle",
                    "splitPayHeaderSubtitleMode", "splitPayHeaderSubtitleText", "splitPayLangMenuUseYn"
            };
            for (int i = 0; i < keys.length; i++) {
                if (out[i] != null && !out[i].isBlank()) {
                    continue;
                }
                Object v = pack.get(keys[i]);
                if (v != null && !String.valueOf(v).isBlank()) {
                    out[i] = String.valueOf(v).trim();
                }
            }
        } catch (Exception ignored) {
            /* 백업 JSON 파싱 실패 시 개별 파라미터만 사용 */
        }
        return out;
    }

    private void applyMerchantSplitPay(MerchantProfile mp,
                                       String splitPayEnabledYn,
                                       String splitPayContractCancelYn,
                                       String splitPayIntervalMonthYn,
                                       String splitPayIntervalDayYn,
                                       String splitPayIntervalMultiYn,
                                       String splitPayDayIntervalDays,
                                       String splitPayMonthIntervalMonths,
                                       String splitPayMultiMaxMonths,
                                       String splitPayFirstPayMode) {
        if (mp == null) {
            return;
        }
        if (splitPayEnabledYn != null && !splitPayEnabledYn.isBlank()) {
            mp.setSplitPayEnabledYn(splitPayEnabledYn);
        }
        if (splitPayContractCancelYn != null && !splitPayContractCancelYn.isBlank()) {
            mp.setSplitPayContractCancelYn(splitPayContractCancelYn);
        }
        if (splitPayIntervalMonthYn != null && !splitPayIntervalMonthYn.isBlank()) {
            mp.setSplitPayIntervalMonthYn(splitPayIntervalMonthYn);
        }
        if (splitPayIntervalDayYn != null && !splitPayIntervalDayYn.isBlank()) {
            mp.setSplitPayIntervalDayYn(splitPayIntervalDayYn);
        }
        if (splitPayIntervalMultiYn != null && !splitPayIntervalMultiYn.isBlank()) {
            mp.setSplitPayIntervalMultiYn(splitPayIntervalMultiYn);
        }
        if ("Y".equalsIgnoreCase(mp.getSplitPayIntervalMultiYn() != null ? mp.getSplitPayIntervalMultiYn().trim() : "N")) {
            mp.setSplitPayIntervalMultiYn("Y");
            mp.setSplitPayIntervalMonthYn("N");
            mp.setSplitPayIntervalDayYn("N");
        } else if ("Y".equalsIgnoreCase(mp.getSplitPayIntervalDayYn() != null ? mp.getSplitPayIntervalDayYn().trim() : "N")) {
            mp.setSplitPayIntervalDayYn("Y");
            mp.setSplitPayIntervalMonthYn("N");
            mp.setSplitPayIntervalMultiYn("N");
        } else {
            mp.setSplitPayIntervalMonthYn("Y");
            mp.setSplitPayIntervalDayYn("N");
            mp.setSplitPayIntervalMultiYn("N");
        }
        if (splitPayDayIntervalDays != null && !splitPayDayIntervalDays.isBlank()) {
            try {
                mp.setSplitPayDayIntervalDays(Integer.parseInt(splitPayDayIntervalDays.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (splitPayMonthIntervalMonths != null && !splitPayMonthIntervalMonths.isBlank()) {
            try {
                mp.setSplitPayMonthIntervalMonths(Integer.parseInt(splitPayMonthIntervalMonths.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (splitPayMultiMaxMonths != null && !splitPayMultiMaxMonths.isBlank()) {
            try {
                mp.setSplitPayMultiMaxMonths(Integer.parseInt(splitPayMultiMaxMonths.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (splitPayFirstPayMode != null && !splitPayFirstPayMode.isBlank()) {
            mp.setSplitPayFirstPayMode(splitPayFirstPayMode);
        }
    }

    private void applyMerchantSplitPayCheckoutPresentation(MerchantProfile mp,
                                                           String headerLogoMode,
                                                           String headerLogoUrl,
                                                           String headerHtmlTitle,
                                                           String headerSubtitleMode,
                                                           String headerSubtitleText,
                                                           String langMenuUseYn) {
        if (mp == null) {
            return;
        }
        if (headerLogoMode != null && !headerLogoMode.isBlank()) {
            mp.setSplitPayHeaderLogoMode(com.pg.urlpay.WebPaymentHeaderLogoModeUtil.normalize(headerLogoMode));
        }
        if (headerLogoUrl != null) {
            String logo = headerLogoUrl.trim();
            if (logo.isEmpty()) {
                mp.setSplitPayHeaderLogoUrl(null);
            } else if (logo.length() > 500) {
                throw new IllegalArgumentException("분할결제 상단 로고 URL은 500자 이하여야 합니다.");
            } else {
                mp.setSplitPayHeaderLogoUrl(logo);
            }
        }
        if (headerHtmlTitle != null) {
            String htmlTitle = headerHtmlTitle.trim();
            if (htmlTitle.isEmpty()) {
                mp.setSplitPayHeaderHtmlTitle(null);
            } else if (htmlTitle.length() > 20) {
                throw new IllegalArgumentException("분할결제 HTML 표시명은 20자 이하여야 합니다.");
            } else {
                mp.setSplitPayHeaderHtmlTitle(htmlTitle);
            }
        }
        if (headerSubtitleMode != null && !headerSubtitleMode.isBlank()) {
            String spSubMode = com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.normalize(headerSubtitleMode);
            mp.setSplitPayHeaderSubtitleMode(spSubMode);
            if (com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.isPreset(spSubMode)) {
                mp.setSplitPayHeaderSubtitleText(null);
            }
        }
        if (headerSubtitleText != null
                && com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.isDirectActive(mp.getSplitPayHeaderSubtitleMode())) {
            String sub = headerSubtitleText.trim();
            if (sub.isEmpty()) {
                mp.setSplitPayHeaderSubtitleText(null);
            } else if (sub.length() > 200) {
                throw new IllegalArgumentException("분할결제 안내메세지는 200자 이하여야 합니다.");
            } else {
                mp.setSplitPayHeaderSubtitleText(sub);
            }
        }
        if (langMenuUseYn != null && !langMenuUseYn.isBlank()) {
            mp.setSplitPayLangMenuUseYn(langMenuUseYn);
        }
    }

    /** 가맹점 URL 결제창 표시 옵션 — 상품명·회사명·다국어 메뉴·배송 주소·자동기억 */
    private void applyMerchantUrlPayPresentationOptions(MerchantProfile mp,
                                                      String productNameUseYn,
                                                      String companyNameShowYn,
                                                      String langMenuUseYn,
                                                      String shippingAddressUseYn,
                                                      String checkoutContactRememberMode) {
        if (mp == null) {
            return;
        }
        if (productNameUseYn != null && !productNameUseYn.isBlank()) {
            mp.setUrlPayProductNameUseYn(productNameUseYn.trim());
        }
        if (companyNameShowYn != null && !companyNameShowYn.isBlank()) {
            mp.setUrlPayCompanyNameShowYn(companyNameShowYn.trim());
        }
        if (langMenuUseYn != null && !langMenuUseYn.isBlank()) {
            mp.setUrlPayLangMenuUseYn(langMenuUseYn.trim());
        }
        if (shippingAddressUseYn != null && !shippingAddressUseYn.isBlank()) {
            mp.setUrlPayShippingAddressUseYn(shippingAddressUseYn.trim());
        }
        if (checkoutContactRememberMode != null && !checkoutContactRememberMode.isBlank()) {
            mp.setCheckoutContactRememberMode(checkoutContactRememberMode.trim());
        }
    }

    /** 가맹점 URL 결제창 입력방식 — GENERAL | TYPE_AA | TYPE_BA | TYPE_AN | … | TYPE_CN (구 TYPE_A/B/C 는 normalize 시 AN/BN/CN) */
    private void applyMerchantUrlPayInputMode(MerchantProfile mp, String urlPayInputMode) {
        if (mp == null || urlPayInputMode == null || urlPayInputMode.isBlank()) {
            return;
        }
        String norm = com.pg.urlpay.UrlPayInputModeUtil.normalizeMerchantStored(urlPayInputMode.trim());
        mp.setUrlPayInputMode(norm);
        /* 표시 옵션·로고·경고는 폼에서 저장한 값 우선 — 입력방식 프리셋은 관리 화면 변경 시에만 JS 동기화 */
    }

    private void applyMerchantUrlPayCardExpiryMode(MerchantProfile mp, String urlPayCardExpiryMode) {
        if (mp == null || urlPayCardExpiryMode == null || urlPayCardExpiryMode.isBlank()) {
            return;
        }
        mp.setUrlPayCardExpiryMode(urlPayCardExpiryMode.trim());
    }

    private void applyMerchantCardAuthMode(MerchantProfile mp, String cardAuthMode) {
        if (mp == null || cardAuthMode == null || cardAuthMode.isBlank()) {
            return;
        }
        mp.setCardAuthMode(com.pg.urlpay.CardAuthModeUtil.normalizeMerchantStored(cardAuthMode.trim()));
    }

    /** 가맹점 공개 URL 결제 방식 (STANDARD | REPAY). */
    private void applyMerchantUrlPayCheckoutMode(MerchantProfile mp, Long orgUnitId, String urlPayCheckoutMode) {
        if (mp == null || orgUnitId == null || urlPayCheckoutMode == null || urlPayCheckoutMode.isBlank()) {
            return;
        }
        String norm = com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(urlPayCheckoutMode);
        if (com.pg.urlpay.UrlPayCheckoutModeUtil.isRepay(norm)) {
            validateUrlPayRepayModeAllowed(orgUnitId);
        }
        mp.setUrlPayCheckoutMode(norm);
    }

    /** 가맹점 API URL 인라인 중계 결제 방식 (STANDARD | REPAY | SPLIT_PAY). */
    private void applyMerchantApiUrlPayCheckoutMode(MerchantProfile mp, Long orgUnitId, String apiUrlPayCheckoutMode) {
        if (mp == null || orgUnitId == null || apiUrlPayCheckoutMode == null || apiUrlPayCheckoutMode.isBlank()) {
            return;
        }
        String norm = com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(apiUrlPayCheckoutMode);
        if (com.pg.urlpay.UrlPayCheckoutModeUtil.isRepay(norm)) {
            validateUrlPayRepayModeAllowed(orgUnitId);
        }
        mp.setApiUrlPayCheckoutMode(norm);
    }

    /** 가맹 API 연동 채널(인라인·리다이렉트·WordPress) — 업체관리 가맹 전용. */
    private void applyMerchantApiIntegrationChannels(MerchantProfile mp, OrgLevel level,
                                                    String inlineYn, String redirectYn, String wordpressYn) {
        if (mp == null || level != OrgLevel.MERCHANT) {
            return;
        }
        if (inlineYn != null && !inlineYn.isBlank()) {
            mp.setApiBrokerInlineUseYn(inlineYn.trim());
        }
        if (redirectYn != null && !redirectYn.isBlank()) {
            mp.setApiBrokerRedirectUseYn(redirectYn.trim());
        }
        if (wordpressYn != null && !wordpressYn.isBlank()) {
            mp.setApiWordpressUseYn(wordpressYn.trim());
        }
        com.pg.merchantdeploy.MerchantApiIntegrationChannelService.validateMerchantChannelCombination(
                mp.getApiBrokerInlineUseYn(), mp.getApiBrokerRedirectUseYn(), mp.getApiWordpressUseYn());
    }

    /** 가맹 모바일·embed 결제창 — 빈값이면 본사 기본(null). */
    @Transactional
    public void patchMerchantMobileCheckoutMode(String compId, String mobileCheckoutMode) {
        if (compId == null || compId.isBlank() || mobileCheckoutMode == null) {
            return;
        }
        orgUnitRepository.findByCode(compId.trim()).flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId()))
                .ifPresent(mp -> {
                    String v = mobileCheckoutMode.trim();
                    if (v.isEmpty()) {
                        mp.setMobileCheckoutMode(null);
                    } else {
                        mp.setMobileCheckoutMode(v);
                    }
                    merchantProfileRepository.save(mp);
                });
    }

    /** 총본사·본사·총판만 가맹점 운영기록 열람·저장 가능. */
    public boolean canManageMerchantOperationRecord() {
        OrgLevel lv = resolveCurrentActorOrgLevel().orElse(null);
        return lv == OrgLevel.HEADQUARTERS || lv == OrgLevel.REGIONAL || lv == OrgLevel.MASTER_DIST;
    }

    /**
     * 가맹점 운영기록 저장. 권한이 없거나 파라미터가 null(미전송)이면 무시.
     * 변경 시 업체변경이력에 작성자(로그인ID)와 함께 기록한다.
     */
    @Transactional
    public void applyMerchantOperationRecord(String compId, String operationRecord) {
        if (compId == null || compId.isBlank() || operationRecord == null) {
            return;
        }
        if (!canManageMerchantOperationRecord()) {
            return;
        }
        orgUnitRepository.findByCode(compId.trim()).ifPresent(ou -> {
            if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
                return;
            }
            merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
                String before = nz(mp.getOperationRecord());
                String after = operationRecord.trim();
                if (Objects.equals(before, after)) {
                    return;
                }
                mp.setOperationRecord(after.isEmpty() ? null : after);
                merchantProfileRepository.save(mp);
                orgUnitChangeAuditService.appendIfChanged(
                        ou.getId(),
                        nz(ou.getCode()),
                        nz(ou.getName()),
                        "[업체정보] 운영기록",
                        before,
                        after);
            });
        });
    }

    /** 가맹점 JPAY 결제창(jpay-pay.html) 입력 필드 오버라이드. FOLLOW_HQ·빈값 → 본사 기본 따름(null). */
    private void applyMerchantJpayCheckoutFieldMode(MerchantProfile mp, String jpayCheckoutFieldMode) {
        if (mp == null || jpayCheckoutFieldMode == null) {
            return;
        }
        mp.setJpayCheckoutFieldMode(jpayCheckoutFieldMode.trim());
    }

    private void applyMerchantJpayPhoneDialCodeYn(MerchantProfile mp, String jpayPhoneDialCodeYn) {
        if (mp == null || jpayPhoneDialCodeYn == null) {
            return;
        }
        mp.setJpayPhoneDialCodeYn(jpayPhoneDialCodeYn.trim());
    }

    /** 가맹점 챗봇 결제 URL 방식 (STANDARD | REPAY | SPLIT_PAY). */
    private void applyMerchantChatbotUrlPayCheckoutMode(MerchantProfile mp, Long orgUnitId, String chatbotUrlPayCheckoutMode) {
        if (mp == null || orgUnitId == null || chatbotUrlPayCheckoutMode == null || chatbotUrlPayCheckoutMode.isBlank()) {
            return;
        }
        String norm = com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(chatbotUrlPayCheckoutMode);
        if (com.pg.urlpay.UrlPayCheckoutModeUtil.isRepay(norm)) {
            validateUrlPayRepayModeAllowed(orgUnitId);
        }
        if (com.pg.urlpay.UrlPayCheckoutModeUtil.isSplitPay(norm)) {
            validateChatbotSplitPayModeAllowed(orgUnitId);
        }
        mp.setChatbotUrlPayCheckoutMode(norm);
    }

    private void validateChatbotSplitPayModeAllowed(Long orgUnitId) {
        com.pg.entity.MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (!com.pg.splitpay.SplitPayMerchantUtil.isEnabled(mp)) {
            throw new IllegalArgumentException(
                    "챗봇 URL 분할결제를 사용하려면 가맹 「URL 분할결제」사용을 켜 주세요.");
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (!com.pg.splitpay.SplitPayCheckoutPageUtil.hasSupportedOperationalPg(opPg)) {
            throw new IllegalArgumentException(
                    "챗봇 URL 분할결제를 사용하려면 운영(Y)·연동용도 URL결제 결제대행사 바인딩(ChillPay·JPAY)이 필요합니다.");
        }
    }

    private void validateUrlPayRepayModeAllowed(Long orgUnitId) {
        if (!chillPayService.isUrlPayRepayEnabledAtHq()) {
            throw new IllegalArgumentException(
                    "본사 설정에서 URL 재결제 기능이 꺼져 있어 재결제 URL 방식을 선택할 수 없습니다.");
        }
        if (chillPayService.findOperationalWebBindingForUrlPayRepay(orgUnitId).isEmpty()) {
            throw new IllegalArgumentException(
                    "재결제 URL 방식을 사용하려면 운영(Y)·연동용도 URL재결제 결제대행사 바인딩이 필요합니다.");
        }
    }

    /**
     * 가맹 결제통보 URL — 조회 시 항상 4개 키를 채우고, WordPress·NOTI ingress 오등록은 숨김.
     */
    private void putMerchantPayNotifyUrlsForDetail(Map<String, Object> m, MerchantProfile mp, Long orgUnitId) {
        String wordpressYn = mp.getApiWordpressUseYn() != null ? mp.getApiWordpressUseYn() : "N";
        String bg = "";
        String rs = "";
        String jn = "";
        String jc = "";
        for (MerchantNotifyUrl n : merchantNotifyUrlRepository.findByOrgUnitIdOrderByUrlTypeAsc(orgUnitId)) {
            if (n.getUrlType() == null || n.getNotiUrl() == null) {
                continue;
            }
            String url = n.getNotiUrl().trim();
            if ("BACKGROUND".equals(n.getUrlType())) {
                bg = url;
            } else if ("RESULT".equals(n.getUrlType())) {
                rs = url;
            } else if (MERCHANT_NOTIFY_MIDDLEWARE.equals(n.getUrlType())) {
                m.put("middlewareNotifyUrl", url);
            } else if (MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY.equals(n.getUrlType())) {
                jn = url;
            } else if (MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK.equals(n.getUrlType())) {
                jc = url;
            }
        }
        /* 노티생성 이력에는 URL이 있으나 가맹 테이블에 미반영된 경우 자동 보강 + 응답에 즉시 반영 */
        if ((jn.isEmpty() || jc.isEmpty()) && orgUnitId != null) {
            try {
                String[] hydrated = merchantJpayNotifyUrlSyncService.hydrateForDetail(orgUnitId, jn, jc);
                if (hydrated != null && hydrated.length >= 2) {
                    if (jn.isEmpty()) {
                        jn = hydrated[0] != null ? hydrated[0] : "";
                    }
                    if (jc.isEmpty()) {
                        jc = hydrated[1] != null ? hydrated[1] : "";
                    }
                }
            } catch (Exception ignored) {
                /* 상세 조회는 보강 실패해도 계속 */
            }
        }
        m.put("notifyUrlBackground", MerchantPayNotifyUrlRules.sanitizeBackgroundForMerchant(bg, wordpressYn));
        m.put("notifyUrlResult", MerchantPayNotifyUrlRules.sanitizeResultForMerchant(rs));
        m.put("jpayNotifyUrl", jn);
        m.put("jpayCallbackUrl", jc);
    }

    private void saveMerchantPayNotifyUrls(Long orgUnitId, String background, String result,
                                           String middlewareUrl, String middlewareSecret,
                                           String jpayNotifyUrl, String jpayCallbackUrl) {
        String wordpressYn = merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(MerchantProfile::getApiWordpressUseYn)
                .orElse("N");
        String bg = MerchantPayNotifyUrlRules.sanitizeBackgroundForMerchant(background, wordpressYn);
        String rs = MerchantPayNotifyUrlRules.sanitizeResultForMerchant(result);
        String mw = normalizeMerchantPayNotifyUrl(middlewareUrl);
        String jn = normalizeMerchantPayNotifyUrl(jpayNotifyUrl);
        String jc = normalizeMerchantPayNotifyUrl(jpayCallbackUrl);
        String sec = middlewareSecret != null ? middlewareSecret.trim() : "";
        if (sec.length() > 256) {
            throw new IllegalArgumentException("PG중계 콜백 시크릿은 256자 이하여야 합니다.");
        }
        final int maxLen = 2048;
        if (bg.length() > maxLen) {
            throw new IllegalArgumentException("URL Background는 " + maxLen + "자 이하여야 합니다. (현재 " + bg.length() + "자)");
        }
        if (rs.length() > maxLen) {
            throw new IllegalArgumentException("URL Result는 " + maxLen + "자 이하여야 합니다. (현재 " + rs.length() + "자)");
        }
        if (mw.length() > maxLen) {
            throw new IllegalArgumentException("PG중계 콜백 URL은 " + maxLen + "자 이하여야 합니다. (현재 " + mw.length() + "자)");
        }
        if (jn.length() > maxLen) {
            throw new IllegalArgumentException("JPAY Notify URL은 " + maxLen + "자 이하여야 합니다. (현재 " + jn.length() + "자)");
        }
        if (jc.length() > maxLen) {
            throw new IllegalArgumentException("JPAY Callback URL은 " + maxLen + "자 이하여야 합니다. (현재 " + jc.length() + "자)");
        }
        merchantNotifyUrlRepository.deleteByOrgUnitIdAndUrlTypeIn(orgUnitId,
                java.util.List.of("BACKGROUND", "RESULT", MERCHANT_NOTIFY_MIDDLEWARE,
                        MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK));
        merchantNotifyUrlRepository.flush();
        if (!bg.isEmpty()) {
            MerchantNotifyUrl n1 = new MerchantNotifyUrl();
            n1.setOrgUnitId(orgUnitId);
            n1.setUrlType("BACKGROUND");
            n1.setNotiUrl(bg);
            n1.setUseYn("Y");
            merchantNotifyUrlRepository.save(n1);
        }
        if (!rs.isEmpty()) {
            MerchantNotifyUrl n2 = new MerchantNotifyUrl();
            n2.setOrgUnitId(orgUnitId);
            n2.setUrlType("RESULT");
            n2.setNotiUrl(rs);
            n2.setUseYn("Y");
            merchantNotifyUrlRepository.save(n2);
        }
        if (!mw.isEmpty()) {
            MerchantNotifyUrl n3 = new MerchantNotifyUrl();
            n3.setOrgUnitId(orgUnitId);
            n3.setUrlType(MERCHANT_NOTIFY_MIDDLEWARE);
            n3.setNotiUrl(mw);
            n3.setSignSecret(sec.isEmpty() ? null : sec);
            n3.setUseYn("Y");
            merchantNotifyUrlRepository.save(n3);
        }
        if (!jn.isEmpty()) {
            MerchantNotifyUrl n4 = new MerchantNotifyUrl();
            n4.setOrgUnitId(orgUnitId);
            n4.setUrlType(MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
            n4.setNotiUrl(jn);
            n4.setUseYn("Y");
            merchantNotifyUrlRepository.save(n4);
        }
        if (!jc.isEmpty()) {
            MerchantNotifyUrl n5 = new MerchantNotifyUrl();
            n5.setOrgUnitId(orgUnitId);
            n5.setUrlType(MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK);
            n5.setNotiUrl(jc);
            n5.setUseYn("Y");
            merchantNotifyUrlRepository.save(n5);
        }
    }

    /** 업체 수정 시 null 인 필드는 DB 기존 MIDDLEWARE 행으로 보강(부분 갱신). 둘 다 null 이면 전부 유지. */
    private String[] mergeMiddlewareNotifyParamsIfOmittedOnUpdate(Long orgUnitId, String reqUrl, String reqSec) {
        Optional<MerchantNotifyUrl> ex = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, MERCHANT_NOTIFY_MIDDLEWARE);
        if (reqUrl == null && reqSec == null) {
            return ex.map(m -> new String[] { m.getNotiUrl(), m.getSignSecret() }).orElse(new String[] { null, null });
        }
        String url = reqUrl != null ? reqUrl : ex.map(MerchantNotifyUrl::getNotiUrl).orElse(null);
        String sec = reqSec != null ? reqSec : ex.map(MerchantNotifyUrl::getSignSecret).orElse(null);
        return new String[] { url, sec };
    }

    /** 업체 수정 시 JPAY URL 파라미터가 null(미전달)이면 기존 {@code JPAY_NOTIFY}/{@code JPAY_CALLBACK} 유지. */
    private String[] mergeJpayNotifyParamsIfOmittedOnUpdate(Long orgUnitId, String reqNotify, String reqCallback) {
        String existingNotify = merchantNotifyUrlRepository
                .findByOrgUnitIdAndUrlType(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY)
                .map(MerchantNotifyUrl::getNotiUrl)
                .orElse(null);
        String existingCallback = merchantNotifyUrlRepository
                .findByOrgUnitIdAndUrlType(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK)
                .map(MerchantNotifyUrl::getNotiUrl)
                .orElse(null);
        String n = reqNotify != null ? reqNotify : existingNotify;
        String c = reqCallback != null ? reqCallback : existingCallback;
        return new String[] { n, c };
    }

    /**
     * 업체 대표 계정 비밀번호 초기화 — 임시 비밀번호 {@code 로그인ID + "1!"} (MerchantProfile.pwd, AppUser 동기화).
     * @return 임시 평문 비밀번호, 실패 시 empty
     */
    @Transactional
    public java.util.Optional<String> resetPassword(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .flatMap(mp -> {
                            String loginId = mp.getLoginId();
                            if (loginId == null || loginId.isBlank()) {
                                return java.util.Optional.empty();
                            }
                            String lid = loginId.trim();
                            String tempPlain = lid + "1!";
                            String encoded = passwordEncoder.encode(tempPlain);
                            mp.setPwd(encoded);
                            merchantProfileRepository.save(mp);
                            AppUser primary = resolveOrCreatePrimaryRepresentativeUser(ou, mp, lid);
                            primary.setPassword(encoded);
                            primary.setPasswordMustChangeYn("Y");
                            orgUserSuspensionService.reactivateRepresentativeForLogin(ou, primary);
                            userRepository.save(primary);
                            persistSingleOrgFieldChange(ou, "[업체정보] 대표비밀번호", "(유지)", "(초기화)");
                            return java.util.Optional.of(tempPlain);
                        }));
    }

    /**
     * 로그인에 쓰이는 대표 {@link AppUser} — username 정확·대소문자 무시·org_unit_code 순 조회, 없으면 생성.
     * (프로필 loginId만 갱신되고 AppUser가 없거나 username 불일치인 경우 로그인 비밀번호가 바뀌지 않는 문제 방지)
     */
    private AppUser resolveOrCreatePrimaryRepresentativeUser(OrgUnit ou, MerchantProfile mp, String loginId) {
        String lid = loginId != null ? loginId.trim() : "";
        if (lid.isEmpty()) {
            throw new IllegalArgumentException("대표 로그인ID가 없습니다.");
        }
        Optional<AppUser> found = userRepository.findByUsername(lid);
        if (found.isEmpty()) {
            found = userRepository.findByUsernameIgnoreCase(lid);
        }
        if (found.isEmpty()) {
            String code = ou.getCode() != null ? ou.getCode().trim() : "";
            if (!code.isEmpty()) {
                AppUser repFallback = null;
                for (AppUser u : userRepository.findByOrgUnitCode(code)) {
                    if (u == null || "ADMIN".equalsIgnoreCase(trimToEmpty(u.getRole()))) {
                        continue;
                    }
                    String uname = trimToEmpty(u.getUsername());
                    if (uname.isEmpty()) {
                        continue;
                    }
                    if (uname.equalsIgnoreCase(lid)) {
                        found = Optional.of(u);
                        break;
                    }
                    if (repFallback == null && isRepresentativeUserType(u)) {
                        repFallback = u;
                    }
                }
                if (found.isEmpty() && repFallback != null) {
                    found = Optional.of(repFallback);
                }
            }
        }
        AppUser primary = found.orElseGet(() -> {
            AppUser nu = new AppUser();
            nu.setUsername(lid);
            String nm = ou.getName() != null && !ou.getName().isBlank() ? ou.getName().trim()
                    : (mp.getCeoNm() != null && !mp.getCeoNm().isBlank() ? mp.getCeoNm().trim() : lid);
            nu.setName(nm);
            nu.setRole("USER");
            nu.setEnabled(true);
            nu.setUserStatus("ACTIVE");
            nu.setUserType("REPRESENTATIVE");
            nu.setOrgUnitCode(ou.getCode() != null ? ou.getCode().trim() : null);
            nu.setPermissionGroupNm("업체사용자");
            nu.setOtpRegisteredYn("N");
            return nu;
        });
        alignRepresentativeUsername(primary, lid);
        if (primary.getOrgUnitCode() == null || primary.getOrgUnitCode().isBlank()) {
            primary.setOrgUnitCode(ou.getCode() != null ? ou.getCode().trim() : null);
        }
        if (primary.getPermissionGroupNm() == null || primary.getPermissionGroupNm().isBlank()) {
            primary.setPermissionGroupNm("업체사용자");
        }
        if (primary.getRole() == null || primary.getRole().isBlank()) {
            primary.setRole("USER");
        }
        return primary;
    }

    private static boolean isRepresentativeUserType(AppUser u) {
        String ut = trimToEmpty(u.getUserType());
        return ut.isEmpty() || "REPRESENTATIVE".equalsIgnoreCase(ut);
    }

    private void alignRepresentativeUsername(AppUser user, String loginId) {
        if (user == null || loginId == null || loginId.isBlank()) {
            return;
        }
        String lid = loginId.trim();
        String cur = trimToEmpty(user.getUsername());
        if (cur.equals(lid)) {
            return;
        }
        if (cur.equalsIgnoreCase(lid)) {
            user.setUsername(lid);
            return;
        }
        if (userRepository.findByUsername(lid).isEmpty()) {
            user.setUsername(lid);
        }
    }

    private static String trimToEmpty(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * 보조(assistant) 계정 비밀번호 초기화 — 임시 비밀번호 {@code 보조로그인ID + "1!"} (AppUser만 변경).
     */
    @Transactional
    public java.util.Optional<String> resetAssistantPassword(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .flatMap(mp -> {
                            String primaryLoginId = mp.getLoginId() != null ? mp.getLoginId().trim() : "";
                            Map<String, Object> rs = parseRegionalSettings(mp.getRegionalSettings());
                            String assistantLoginId = rs.get("assistantLoginId") != null
                                    ? String.valueOf(rs.get("assistantLoginId")).trim() : "";
                            if (assistantLoginId.isEmpty() && ou.getCode() != null && !ou.getCode().isBlank()) {
                                for (AppUser u : userRepository.findByOrgUnitCode(ou.getCode().trim())) {
                                    String uname = u.getUsername() != null ? u.getUsername().trim() : "";
                                    if (uname.isEmpty()) continue;
                                    if (!primaryLoginId.isEmpty() && primaryLoginId.equalsIgnoreCase(uname)) continue;
                                    if ("ADMIN".equalsIgnoreCase(u.getRole())) continue;
                                    assistantLoginId = uname;
                                    break;
                                }
                            }
                            if (assistantLoginId.isBlank()) {
                                return java.util.Optional.empty();
                            }
                            final String aid = assistantLoginId.trim();
                            return userRepository.findByUsername(aid).map(u -> {
                                String tempPlain = aid + "1!";
                                u.setPassword(passwordEncoder.encode(tempPlain));
                                u.setPasswordMustChangeYn("Y");
                                userRepository.save(u);
                                orgUnitChangeAuditService.appendIfChanged(ou.getId(), nz(ou.getCode()), nz(ou.getName()),
                                        "[업체정보] 보조비밀번호", "(유지)", "(초기화)");
                                return tempPlain;
                            });
                        }));
    }

    /** 업체 로그인ID 변경 - MerchantProfile.loginId, AppUser.username 동시 변경 */
    @Transactional
    public boolean changeLoginId(String compId, String newLoginId) {
        if (newLoginId == null || newLoginId.trim().isEmpty())
            throw new IllegalArgumentException("새 로그인ID를 입력하세요.");
        String trimmed = newLoginId.trim();
        if (userRepository.findByUsername(trimmed).isPresent())
            throw new IllegalArgumentException("이미 사용 중인 로그인ID입니다: " + trimmed);
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            String oldLoginId = mp.getLoginId();
                            mp.setLoginId(trimmed);
                            merchantProfileRepository.save(mp);
                            if (oldLoginId != null && !oldLoginId.isEmpty()) {
                                userRepository.findByUsername(oldLoginId).ifPresent(u -> {
                                    u.setUsername(trimmed);
                                    userRepository.save(u);
                                });
                            } else {
                                AppUser appUser = new AppUser();
                                appUser.setUsername(trimmed);
                                String initPlain = trimmed + "1!";
                                appUser.setPassword(passwordEncoder.encode(initPlain));
                                appUser.setName(mp.getCeoNm() != null ? mp.getCeoNm() : trimmed);
                                appUser.setRole("USER");
                                appUser.setEnabled(true);
                                appUser.setOrgUnitCode(compId);
                                appUser.setPermissionGroupNm("업체사용자");
                                appUser.setOtpRegisteredYn("N");
                                appUser.setPasswordMustChangeYn("Y");
                                userRepository.save(appUser);
                            }
                            persistSingleOrgFieldChange(ou, "[업체정보] 로그인ID",
                                    oldLoginId != null ? oldLoginId.trim() : "", trimmed);
                            return true;
                        }))
                .orElse(false);
    }

    public OrgUnit register(String code, String name, String compDiv, Long parentId,
                            String compTel, String zipCode, String addr, String addrDetail,
                            String ceoNm, String ceoMobile, String useYn, String loginId,
                            String regNo, String email, String pwd,
                            String bankCd, String transferFee, String accountNo, String accountHolder,
                            String remark) {
        return registerWithExtra(code, name, compDiv, parentId, compTel, zipCode, addr, addrDetail, null, null,
                ceoNm, ceoMobile, useYn, loginId, regNo,
                null, null, null, null, null, null, null, null, null, null, null,
                email, pwd,
                bankCd, transferFee, null, accountNo, accountHolder,
                null, null, null, null, null, null, null, null, null,
                remark,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                null, null, null,
                null, null,
                null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Transactional
    public OrgUnit registerWithExtra(String code, String name, String compDiv, Long parentId,
                                     String compTel, String zipCode, String addr, String addrDetail, String addrEtc, String addrCountryCd,
                                     String ceoNm, String ceoMobile, String useYn, String loginId,
                                     String regNo, String bizType, String industry,
                                     String bizNature, String product, String homepage, String settleName, String settleTelNo,
                                     String settleType, String commissionRate, String limitAmt,
                                     String fax, String email, String pwd,
                                     String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                                     String countryCd, String swift, String branchName, String branchAddr,
                                     String contactTel, String walletAddress, String networkName,                                      String siteUrl, String siteSummary,
                                     String remark,
                                     String withdrawRestrictType,
                                     Integer withdrawLimitDays, String withdrawStartTime, String withdrawEndTime,
                                     String payLimitDefault, String payLimitExtra, String payLimitAlertSms,
                                     String holdRateFollowHq, String holdRate, Integer holdDays, String calcCycle, String calcCloseTime,
                                     String transferType, Integer transferCycleDays, String autoTransferMin, String payHoldYn,
                                     String calcExcludeYn, String calcExcludeTarget, String calcStartTime,
                                     String calcProcType, String calcMinAmt, String transferExecTime,
                                     String pgBindings, String webPaymentUseYn, String chatbotPaymentUseYn, String baseCurrency,
                                     String defaultProductName, String defaultProductCode, String defaultProductAmount, String defaultProductDesc,
                                     String notifyUrlBackground, String notifyUrlResult,
                                     String jpayNotifyUrl, String jpayCallbackUrl,
                                     String notifyUrl1, String notifyUrl2, String notifyUrl3, String notifyUrl4,
                                     String middlewareNotifyUrl, String middlewareNotifySecret,
                                     String commissionFollowHq, String hqPolicyScope, String perTxFee, String cancelRate,
                                     String voidFeePerTx, String manualVoidFeePerTx, String usageRate,
                                     String failFee, String payRate, String refundRate, String rollingPct, String rollingDays,
                                     String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
                                     String urlPayAlertEmailYn, String urlPayLineNotifyToken,
                                     String receiptEmailFollowHqYn, String receiptEmailUseYn, String receiptEmailEnabledYn,
                                     String feeVatApplyYn, String feeVatRatePct,
                                     String regionalSettings,
                                     String urlPayCheckoutMode,
                                     String urlPayProductNameUseYn,
                                     String urlPayCompanyNameShowYn,
                                     String urlPayLangMenuUseYn,
                                     String checkoutContactRememberMode,
                                     String urlPayShippingAddressUseYn,
                                     String urlPayInputMode,
                                     String urlPayCardExpiryMode,
                                     String cardAuthMode,
                                     String apiUrlPayCheckoutMode,
                                     String chatbotUrlPayCheckoutMode,
                                     String apiJpaySubscriptionUseYn,
                                     String apiBrokerInlineUseYn,
                                     String apiBrokerRedirectUseYn,
                                     String apiWordpressUseYn,
                                     String jpayCheckoutFieldMode,
                                     String jpayPhoneDialCodeYn,
                                     String tabletFeatureUseYn,
                                     String cardRiskPolicyMode,
                                     String cardRiskTier1Hours, String cardRiskTier1Min,
                                     String cardRiskTier2Hours, String cardRiskTier2Min,
                                     String cardRiskTier3Hours, String cardRiskTier3Min,
                                     String cardRiskTier4Hours, String cardRiskTier4Min,
                                     String cardRiskAutoBlacklistTier,
                                     String cardRiskPresaleMode,
                                     String cardRiskPresaleBuyerMismatchYn,
                                     String cardRiskPresaleHolderNameYn,
                                     String cardRiskPresalePhoneInvalidYn,
                                     String cardRiskPresaleEmailInvalidYn,
                                     String cardRiskPresaleVelocityCardYn,
                                     String cardRiskPresaleVelocityEmailYn,
                                     String cardRiskPresaleVelocityIpYn,
                                     String cardRiskPresaleVelCardWinMin,
                                     String cardRiskPresaleVelCardMax,
                                     String cardRiskPresaleVelEmailWinMin,
                                     String cardRiskPresaleVelEmailMax,
                                     String cardRiskPresaleVelIpWinMin,
                                     String cardRiskPresaleVelIpMax) {
        return registerWithExtra(code, name, compDiv, parentId,
                compTel, zipCode, addr, addrDetail, addrEtc, addrCountryCd,
                ceoNm, ceoMobile, useYn, loginId,
                regNo, bizType, industry,
                bizNature, product, homepage, settleName, settleTelNo,
                settleType, commissionRate, limitAmt,
                fax, email, pwd,
                bankCd, transferFee, cryptoTransferFee, accountNo, accountHolder,
                countryCd, swift, branchName, branchAddr,
                contactTel, walletAddress, networkName, siteUrl, siteSummary,
                remark,
                withdrawRestrictType,
                withdrawLimitDays, withdrawStartTime, withdrawEndTime,
                payLimitDefault, payLimitExtra, payLimitAlertSms,
                holdRateFollowHq, holdRate, holdDays, calcCycle, calcCloseTime,
                transferType, transferCycleDays, autoTransferMin, payHoldYn,
                calcExcludeYn, calcExcludeTarget, calcStartTime,
                calcProcType, calcMinAmt, transferExecTime,
                pgBindings, webPaymentUseYn, chatbotPaymentUseYn, baseCurrency,
                defaultProductName, defaultProductCode, defaultProductAmount, defaultProductDesc,
                notifyUrlBackground, notifyUrlResult,
                jpayNotifyUrl, jpayCallbackUrl,
                notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4,
                middlewareNotifyUrl, middlewareNotifySecret,
                commissionFollowHq, hqPolicyScope, perTxFee, cancelRate,
                voidFeePerTx, manualVoidFeePerTx, usageRate,
                failFee, payRate, refundRate, rollingPct, rollingDays,
                feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx,
                null, null, null,
                null, null, null, null,
                null, null, null, null, null,
                urlPayAlertEmailYn, urlPayLineNotifyToken,
                receiptEmailFollowHqYn, receiptEmailUseYn, receiptEmailEnabledYn,
                feeVatApplyYn, feeVatRatePct,
                regionalSettings,
                urlPayCheckoutMode,
                urlPayProductNameUseYn,
                urlPayCompanyNameShowYn,
                urlPayLangMenuUseYn,
                checkoutContactRememberMode,
                urlPayShippingAddressUseYn,
                urlPayInputMode,
                urlPayCardExpiryMode,
                cardAuthMode,
                apiUrlPayCheckoutMode,
                chatbotUrlPayCheckoutMode,
                apiJpaySubscriptionUseYn,
                apiBrokerInlineUseYn,
                apiBrokerRedirectUseYn,
                apiWordpressUseYn,
                jpayCheckoutFieldMode,
                jpayPhoneDialCodeYn,
                tabletFeatureUseYn,
                cardRiskPolicyMode,
                cardRiskTier1Hours, cardRiskTier1Min,
                cardRiskTier2Hours, cardRiskTier2Min,
                cardRiskTier3Hours, cardRiskTier3Min,
                cardRiskTier4Hours, cardRiskTier4Min,
                cardRiskAutoBlacklistTier,
                cardRiskPresaleMode,
                cardRiskPresaleBuyerMismatchYn,
                cardRiskPresaleHolderNameYn,
                cardRiskPresalePhoneInvalidYn,
                cardRiskPresaleEmailInvalidYn,
                cardRiskPresaleVelocityCardYn,
                cardRiskPresaleVelocityEmailYn,
                cardRiskPresaleVelocityIpYn,
                cardRiskPresaleVelCardWinMin,
                cardRiskPresaleVelCardMax,
                cardRiskPresaleVelEmailWinMin,
                cardRiskPresaleVelEmailMax,
                cardRiskPresaleVelIpWinMin,
                cardRiskPresaleVelIpMax);
    }

    @Transactional
    public OrgUnit registerWithExtra(String code, String name, String compDiv, Long parentId,
                                     String compTel, String zipCode, String addr, String addrDetail, String addrEtc, String addrCountryCd,
                                     String ceoNm, String ceoMobile, String useYn, String loginId,
                                     String regNo, String bizType, String industry,
                                     String bizNature, String product, String homepage, String settleName, String settleTelNo,
                                     String settleType, String commissionRate, String limitAmt,
                                     String fax, String email, String pwd,
                                     String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                                     String countryCd, String swift, String branchName, String branchAddr,
                                     String contactTel, String walletAddress, String networkName,                                      String siteUrl, String siteSummary,
                                     String remark,
                                     String withdrawRestrictType,
                                     Integer withdrawLimitDays, String withdrawStartTime, String withdrawEndTime,
                                     String payLimitDefault, String payLimitExtra, String payLimitAlertSms,
                                     String holdRateFollowHq, String holdRate, Integer holdDays, String calcCycle, String calcCloseTime,
                                     String transferType, Integer transferCycleDays, String autoTransferMin, String payHoldYn,
                                     String calcExcludeYn, String calcExcludeTarget, String calcStartTime,
                                     String calcProcType, String calcMinAmt, String transferExecTime,
                                     String pgBindings, String webPaymentUseYn, String chatbotPaymentUseYn, String baseCurrency,
                                     String defaultProductName, String defaultProductCode, String defaultProductAmount, String defaultProductDesc,
                                     String notifyUrlBackground, String notifyUrlResult,
                                     String jpayNotifyUrl, String jpayCallbackUrl,
                                     String notifyUrl1, String notifyUrl2, String notifyUrl3, String notifyUrl4,
                                     String middlewareNotifyUrl, String middlewareNotifySecret,
                                     String commissionFollowHq, String hqPolicyScope, String perTxFee, String cancelRate,
                                     String voidFeePerTx, String manualVoidFeePerTx, String usageRate,
                                     String failFee, String payRate, String refundRate, String rollingPct, String rollingDays,
                                     String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
                                     String fee3dsRate, String chargebackFeePerTx, String chargebackPolicyId,
                                     String voidSettlementMode, String manualVoidSettlementMode, String refundSettlementMode, String forceRefundSettlementMode,
                                     String payFollowMerchantUseYn, String payFollowAutoVoidYn, String payFollowEmailVoidYn,
                                     String payFollowAutoRefundYn, String payFollowForceRefundYn,
                                     String urlPayAlertEmailYn, String urlPayLineNotifyToken,
                                     String receiptEmailFollowHqYn, String receiptEmailUseYn, String receiptEmailEnabledYn,
                                     String feeVatApplyYn, String feeVatRatePct,
                                     String regionalSettings,
                                     String urlPayCheckoutMode,
                                     String urlPayProductNameUseYn,
                                     String urlPayCompanyNameShowYn,
                                     String urlPayLangMenuUseYn,
                                     String checkoutContactRememberMode,
                                     String urlPayShippingAddressUseYn,
                                     String urlPayInputMode,
                                     String urlPayCardExpiryMode,
                                     String cardAuthMode,
                                     String apiUrlPayCheckoutMode,
                                     String chatbotUrlPayCheckoutMode,
                                     String apiJpaySubscriptionUseYn,
                                     String apiBrokerInlineUseYn,
                                     String apiBrokerRedirectUseYn,
                                     String apiWordpressUseYn,
                                     String jpayCheckoutFieldMode,
                                     String jpayPhoneDialCodeYn,
                                     String tabletFeatureUseYn,
                                     String cardRiskPolicyMode,
                                     String cardRiskTier1Hours, String cardRiskTier1Min,
                                     String cardRiskTier2Hours, String cardRiskTier2Min,
                                     String cardRiskTier3Hours, String cardRiskTier3Min,
                                     String cardRiskTier4Hours, String cardRiskTier4Min,
                                     String cardRiskAutoBlacklistTier,
                                     String cardRiskPresaleMode,
                                     String cardRiskPresaleBuyerMismatchYn,
                                     String cardRiskPresaleHolderNameYn,
                                     String cardRiskPresalePhoneInvalidYn,
                                     String cardRiskPresaleEmailInvalidYn,
                                     String cardRiskPresaleVelocityCardYn,
                                     String cardRiskPresaleVelocityEmailYn,
                                     String cardRiskPresaleVelocityIpYn,
                                     String cardRiskPresaleVelCardWinMin,
                                     String cardRiskPresaleVelCardMax,
                                     String cardRiskPresaleVelEmailWinMin,
                                     String cardRiskPresaleVelEmailMax,
                                     String cardRiskPresaleVelIpWinMin,
                                     String cardRiskPresaleVelIpMax) {
        OrgUnit o = new OrgUnit();
        String compDivVal = compDiv != null ? compDiv.trim() : "AGENCY";
        Long effectiveParentId = parentId;
        if ("REGIONAL".equalsIgnoreCase(compDivVal) && effectiveParentId == null) {
            effectiveParentId = orgUnitRepository.findAll().stream()
                    .filter(unit -> unit.getOrgLevel() == OrgLevel.HEADQUARTERS)
                    .map(OrgUnit::getId)
                    .min(Long::compareTo)
                    .orElse(null);
            if (effectiveParentId == null) {
                throw new IllegalArgumentException("등록된 총본사가 없어 본사를 등록할 수 없습니다.");
            }
        }
        // 업체코드는 항상 서버 규칙으로 자동 생성(요청값 무시)
        String finalCode = generateNextCompCode(compDivVal);
        if (orgUnitRepository.findByCode(finalCode).isPresent()) {
            throw new IllegalArgumentException("업체코드가 이미 존재합니다: " + finalCode);
        }
        o.setCode(finalCode);
        o.setName(name != null ? name : "");
        OrgLevel childLevel = orgLevelFromCompDiv(compDiv != null ? compDiv : "AGENCY");
        o.setOrgLevel(childLevel);
        validateParentLevel(effectiveParentId, childLevel, null);
        o.setParentId(effectiveParentId);
        o.setStatus("ACTIVE");
        o.setTabletFeatureUseYn(tabletFeatureUseYn != null && !tabletFeatureUseYn.isBlank()
                && !"Y".equalsIgnoreCase(tabletFeatureUseYn.trim()) ? "N" : "Y");
        OrgUnit saved = orgUnitRepository.save(o);

        MerchantProfile mp = new MerchantProfile();
        mp.setOrgUnitId(saved.getId());
        mp.setCompDiv(compDiv);
        mp.setCompTel(compTel);
        mp.setZipCode(zipCode);
        mp.setAddr(addr);
        mp.setAddrDetail(addrDetail);
        if (addrCountryCd != null) mp.setAddrCountryCd(addrCountryCd.trim());
        mp.setCeoNm(ceoNm);
        mp.setCeoMobile(ceoMobile);
        mp.setUseYn(OrgUseYnUtil.normalize(useYn));
        mp.setLoginId(loginId);
        mp.setRegNo(regNo);
        mp.setBizType(bizType);
        mp.setIndustry(industry);
        mp.setBizNature(bizNature);
        mp.setProduct(product);
        mp.setHomepage(homepage);
        mp.setSettleName(settleName);
        mp.setSettleTelNo(settleTelNo);
        if (settleType != null && !settleType.isEmpty()) mp.setSettleType(settleType);
        if (commissionRate != null && !commissionRate.isEmpty()) try { mp.setCommissionRate(new java.math.BigDecimal(commissionRate.trim())); } catch (Exception ignored) {}
        if (limitAmt != null && !limitAmt.isEmpty()) try { mp.setLimitAmt(new java.math.BigDecimal(limitAmt.trim())); } catch (Exception ignored) {}
        mp.setFax(fax);
        mp.setEmail(email);
        mp.setPwd(null);
        mp.setBankCd(bankCd);
        mp.setTransferFee(transferFee);
        if (cryptoTransferFee != null) mp.setCryptoTransferFee(cryptoTransferFee.trim());
        mp.setAccountNo(accountNo);
        mp.setAccountHolder(accountHolder);
        if (countryCd != null) mp.setCountryCd(countryCd.trim());
        if (swift != null) mp.setSwift(swift.trim());
        if (branchName != null) mp.setBranchName(branchName.trim());
        if (branchAddr != null) mp.setBranchAddr(branchAddr.trim());
        if (contactTel != null) mp.setContactTel(contactTel.trim());
        if (walletAddress != null) mp.setWalletAddress(walletAddress.trim());
        if (networkName != null) mp.setNetworkName(networkName.trim());
        if (siteUrl != null) mp.setSiteUrl(siteUrl.trim());
        if (siteSummary != null) mp.setSiteSummary(siteSummary.trim());
        mp.setRemark(remark);
        if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
        if ("MERCHANT".equalsIgnoreCase(compDivVal)) {
            applyMerchantUrlPayCheckoutMode(mp, saved.getId(), urlPayCheckoutMode);
            applyMerchantUrlPayPresentationOptions(mp,
                    urlPayProductNameUseYn, urlPayCompanyNameShowYn, urlPayLangMenuUseYn,
                    urlPayShippingAddressUseYn, checkoutContactRememberMode);
            applyMerchantUrlPayInputMode(mp, urlPayInputMode);
            applyMerchantUrlPayCardExpiryMode(mp, urlPayCardExpiryMode);
            applyMerchantCardAuthMode(mp, cardAuthMode);
            applyMerchantApiUrlPayCheckoutMode(mp, saved.getId(), apiUrlPayCheckoutMode);
            applyMerchantChatbotUrlPayCheckoutMode(mp, saved.getId(), chatbotUrlPayCheckoutMode);
            applyMerchantJpayCheckoutFieldMode(mp, jpayCheckoutFieldMode);
            applyMerchantJpayPhoneDialCodeYn(mp, jpayPhoneDialCodeYn);
            if (apiJpaySubscriptionUseYn != null && !apiJpaySubscriptionUseYn.trim().isEmpty()) {
                mp.setApiJpaySubscriptionUseYn(apiJpaySubscriptionUseYn.trim());
            }
            applyMerchantApiIntegrationChannels(mp, OrgLevel.MERCHANT,
                    apiBrokerInlineUseYn, apiBrokerRedirectUseYn, apiWordpressUseYn);
            if (chatbotPaymentUseYn != null && !chatbotPaymentUseYn.trim().isEmpty()) {
                mp.setChatbotPaymentUseYn("Y".equalsIgnoreCase(chatbotPaymentUseYn.trim()) ? "Y" : "N");
            } else {
                mp.setChatbotPaymentUseYn("N");
            }
        }
        if (baseCurrency != null && !baseCurrency.trim().isEmpty()) {
            validateBaseCurrency(compDiv != null ? compDiv : "AGENCY", baseCurrency);
            if ("MASTER_DIST".equalsIgnoreCase(compDivVal)) {
                validateMasterDistBaseCurrencyAgainstRegionalParent(effectiveParentId, baseCurrency.trim());
            }
            mp.setBaseCurrency(baseCurrency.trim());
        }
        if ("MERCHANT".equalsIgnoreCase(compDivVal)
                && (mp.getBaseCurrency() == null || mp.getBaseCurrency().isBlank())) {
            String inh = resolveInheritedBaseCurrencyForMerchant(effectiveParentId);
            if (inh != null && !inh.isBlank()) {
                mp.setBaseCurrency(inh);
            }
        }
        if (("REGIONAL".equalsIgnoreCase(compDiv) || "MASTER_DIST".equalsIgnoreCase(compDiv))
                && regionalSettings != null && !regionalSettings.trim().isEmpty()) {
            mp.setRegionalSettings(regionalSettings.trim());
        }
        if (usesCommissionPolicyForCompDiv(compDivVal)) {
            mergeCommissionUiIntoRegionalSettings(mp, commissionFollowHq, hqPolicyScope);
        }
        if ("MERCHANT".equalsIgnoreCase(compDivVal)) {
            String chosenCur = (baseCurrency != null && !baseCurrency.trim().isEmpty())
                    ? baseCurrency.trim()
                    : mp.getBaseCurrency();
            validateMerchantBaseCurrencyAgainstParent(effectiveParentId, chosenCur);
            validateMerchantPolicyCurrencyCompatibility(chosenCur, commissionFollowHq, hqPolicyScope, null);
            mergeMerchantPayFollowFromRequest(mp, payFollowMerchantUseYn, payFollowAutoVoidYn,
                    payFollowEmailVoidYn, payFollowAutoRefundYn, payFollowForceRefundYn);
            mergeMerchantCardRiskIfAny(mp, cardRiskPolicyMode,
                    cardRiskTier1Hours, cardRiskTier1Min,
                    cardRiskTier2Hours, cardRiskTier2Min,
                    cardRiskTier3Hours, cardRiskTier3Min,
                    cardRiskTier4Hours, cardRiskTier4Min,
                    cardRiskAutoBlacklistTier,
                    cardRiskPresaleMode,
                    cardRiskPresaleBuyerMismatchYn,
                    cardRiskPresaleHolderNameYn,
                    cardRiskPresalePhoneInvalidYn,
                    cardRiskPresaleEmailInvalidYn,
                    cardRiskPresaleVelocityCardYn,
                    cardRiskPresaleVelocityEmailYn,
                    cardRiskPresaleVelocityIpYn,
                    cardRiskPresaleVelCardWinMin,
                    cardRiskPresaleVelCardMax,
                    cardRiskPresaleVelEmailWinMin,
                    cardRiskPresaleVelEmailMax,
                    cardRiskPresaleVelIpWinMin,
                    cardRiskPresaleVelIpMax);
            applyMerchantUrlPayAlerts(mp, urlPayAlertEmailYn, urlPayLineNotifyToken);
            applyMerchantReceiptEmail(mp, receiptEmailFollowHqYn, receiptEmailUseYn);
            merchantChatbotKbService.seedFromRegistration(mp, saved);
        }
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(saved.getId());
        /** 정산주기(calcCycle)는 가맹점에만 부여. 총본사~영업점은 미사용(null). */
        if (childLevel == OrgLevel.MERCHANT) {
            String effCycle = calcCycle != null ? calcCycle.trim() : "";
            if (effCycle.isEmpty()) {
                effCycle = masterDistSettlementCycleConfigService.findNearestMasterDistOrgId(effectiveParentId)
                        .flatMap(masterDistSettlementCycleConfigService::getDefaultCycleCode)
                        .orElse("D7");
            } else {
                masterDistSettlementCycleConfigService.validateMerchantCalcCycle(effectiveParentId, effCycle);
                effCycle = SettlementPeriodResolver.normalizeCalcCycle(effCycle);
            }
            ss.setCalcCycle(effCycle);
        } else {
            ss.setCalcCycle(null);
        }
        if (calcProcType != null && !calcProcType.isBlank()) {
            ss.setCalcProcType(calcProcType.trim());
            ss.setTransferType(transferType != null && !transferType.isBlank() ? transferType.trim() : "MANUAL");
        } else {
            applyLegacySettlementFields(ss, transferType);
        }
        /* RT·T0 실시간 주기는 노티 직후 자동정산(calcProcType=AUTO) 전제 — 등록 폼 기본값 MANUAL이면 실시간이 동작하지 않음 */
        if (childLevel == OrgLevel.MERCHANT) {
            String normRt = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
            if (SettlementCycleTiming.isRealtimeCode(normRt)) {
                String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
                if (proc.isEmpty() || "MANUAL".equalsIgnoreCase(proc)) {
                    ss.setCalcProcType("AUTO");
                    if (ss.getTransferType() == null || ss.getTransferType().isBlank()) {
                        ss.setTransferType("MANUAL");
                    }
                }
            }
        }
        if (withdrawRestrictType != null && !withdrawRestrictType.isBlank()) {
            ss.setWithdrawRestrictType(withdrawRestrictType.trim());
        }
        if (withdrawLimitDays != null) ss.setWithdrawLimitDays(withdrawLimitDays);
        if (parseTime(withdrawStartTime) != null) ss.setWithdrawStartTime(parseTime(withdrawStartTime));
        if (parseTime(withdrawEndTime) != null) ss.setWithdrawEndTime(parseTime(withdrawEndTime));
        if (payLimitDefault != null && !payLimitDefault.isEmpty()) try { ss.setPayLimitDefault(new BigDecimal(payLimitDefault.trim())); } catch (Exception ignored) {}
        if (payLimitExtra != null && !payLimitExtra.isEmpty()) try { ss.setPayLimitExtra(new BigDecimal(payLimitExtra.trim())); } catch (Exception ignored) {}
        if (payLimitAlertSms != null && !payLimitAlertSms.isEmpty()) ss.setPayLimitAlertSms(payLimitAlertSms);
        if (holdRateFollowHq != null && !holdRateFollowHq.isEmpty()) ss.setHoldRateFollowHq(holdRateFollowHq.trim());
        if ("N".equalsIgnoreCase(holdRateFollowHq != null ? holdRateFollowHq.trim() : "")) {
            if (holdRate != null && !holdRate.isEmpty()) {
                ss.setHoldRate(PercentDecimalHelper.parsePercentOneDecimal(holdRate));
            }
            if (holdDays != null) ss.setHoldDays(holdDays);
        }
        if (childLevel == OrgLevel.MERCHANT) {
            applyMerchantSettlementCloseStartFromForm(ss, calcCloseTime, calcStartTime);
        }
        if (transferCycleDays != null) ss.setTransferCycleDays(transferCycleDays);
        if (autoTransferMin != null && !autoTransferMin.isEmpty()) try { ss.setAutoTransferMin(new BigDecimal(autoTransferMin.trim())); } catch (Exception ignored) {}
        if (payHoldYn != null && !payHoldYn.isEmpty()) ss.setPayHoldYn(payHoldYn);
        if (calcExcludeYn != null && !calcExcludeYn.isEmpty()) ss.setCalcExcludeYn(calcExcludeYn);
        if (calcExcludeTarget != null && !calcExcludeTarget.isEmpty()) ss.setCalcExcludeTarget(calcExcludeTarget);
        if (calcMinAmt != null && !calcMinAmt.isEmpty()) try { ss.setCalcMinAmt(new BigDecimal(calcMinAmt.trim())); } catch (Exception ignored) {}
        if (parseTime(transferExecTime) != null) ss.setTransferExecTime(parseTime(transferExecTime));
        if (feeVatApplyYn != null && !feeVatApplyYn.isBlank()) {
            ss.setFeeVatApplyYn("Y".equalsIgnoreCase(feeVatApplyYn.trim()) ? "Y" : "N");
        }
        if (feeVatRatePct != null && !feeVatRatePct.isBlank()) {
            try {
                ss.setFeeVatRatePct(new BigDecimal(feeVatRatePct.trim()));
            } catch (Exception ignored) {
            }
        } else if ("N".equalsIgnoreCase(ss.getFeeVatApplyYn() != null ? ss.getFeeVatApplyYn().trim() : "N")) {
            ss.setFeeVatRatePct(BigDecimal.ZERO);
        }
        if (childLevel == OrgLevel.MERCHANT) {
            String defRecv = hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                    .map(HqLedgerSysSettings::getReceivableRecoveryDefaultMode)
                    .map(ReceivableRecoveryModeUtil::normalize)
                    .orElse(ReceivableRecoveryModeUtil.AUTO);
            ss.setReceivableRecoveryMode(defRecv);
        }
        /* 신규 조직: 총판/본사 기본 따름 — 가맹은 HQ 미수금설정에서만 개별 오버라이드 */
        ss.setReceivableRecoveryOverrideYn("N");
        settlementSettingRepository.save(ss);
        if (childLevel == OrgLevel.MASTER_DIST) {
            applyMasterDistReceiptEmailPolicy(saved.getId(), receiptEmailEnabledYn);
        }

        MerchantCommissionExtra extra = new MerchantCommissionExtra();
        extra.setOrgUnitId(saved.getId());
        merchantCommissionExtraRepository.save(extra);

        if ("MERCHANT".equalsIgnoreCase(compDiv) && pgBindings != null && !pgBindings.trim().isEmpty()) {
            try {
                List<Map<String, Object>> list = PG_BINDINGS_OBJECT_MAPPER.readValue(pgBindings.trim(),
                        new TypeReference<List<Map<String, Object>>>() {});
                list = dedupeMerchantPgBindingJsonRows(list);
                validateMerchantPgBindingJsonRows(list);
                int order = 0;
                for (Map<String, Object> m : list) {
                    String pc = m.get("pgCd") != null ? m.get("pgCd").toString().trim() : "";
                    if (pc.isEmpty()) continue;
                    MerchantPgBinding binding = new MerchantPgBinding();
                    binding.setOrgUnitId(saved.getId());
                    binding.setPgCd(pc);
                    binding.setActivationYn("Y".equalsIgnoreCase(optStr(m, "activationYn")) ? "Y" : "N");
                    binding.setOperationalYn("Y".equalsIgnoreCase(optStr(m, "operationalYn")) ? "Y" : "N");
                    binding.setPayMethod(optStr(m, "payMethod") != null && !optStr(m, "payMethod").isEmpty() ? optStr(m, "payMethod") : "WEB");
                    binding.setRootNo(optStr(m, "rootNo"));
                    applyMerchantPgBindingCredentialsFromJson(binding, pc, binding.getPayMethod(), m, Map.of());
                    binding.setInstallmentYn("Y".equalsIgnoreCase(optStr(m, "installmentYn")) ? "Y" : "N");
                    String maxMo = optStr(m, "maxInstallmentMonths");
                    if (maxMo != null && !maxMo.isEmpty()) {
                        try { binding.setMaxInstallmentMonths(Integer.parseInt(maxMo.trim())); } catch (NumberFormatException ignored) {}
                    }
                    String upmR = optStr(m, "urlPayPricingMode");
                    if (upmR != null && "DISPLAY_FX_THB".equalsIgnoreCase(upmR.trim())) {
                        binding.setUrlPayPricingMode("DISPLAY_FX_THB");
                    } else {
                        binding.setUrlPayPricingMode("CHECKOUT_CURRENCY");
                    }
                    binding.setCardBrandScope(resolveMerchantPgCardBrandScopeForSave(pc, optStr(m, "cardBrandScope")));
                    binding.setCurrencyScope(resolveMerchantPgCurrencyScopeForSave(pc, optStr(m, "currencyScope")));
                    binding.setSortOrder(++order);
                    applyExtSettlementFromJsonMap(binding, m);
                    merchantPgBindingRepository.save(binding);
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("결제대행사(JSON) 형식이 올바르지 않습니다.", e);
            }
        }

        if ("MERCHANT".equalsIgnoreCase(compDiv)) {
            if (syncMerchantWebPaymentUseYnIfNoUrlPayBinding(saved.getId(), mp)) {
                merchantProfileRepository.save(mp);
            }
            saveMerchantDefaultProductOrClear(saved.getId(), defaultProductName, defaultProductCode,
                    defaultProductAmount, defaultProductDesc);
            saveMerchantPayNotifyUrls(saved.getId(), notifyUrlBackground, notifyUrlResult,
                    middlewareNotifyUrl, middlewareNotifySecret, jpayNotifyUrl, jpayCallbackUrl);
            copyNotifyUrlSlotsFromNearestMasterDistToMerchant(saved.getId(), effectiveParentId);
        }
        if ("MASTER_DIST".equalsIgnoreCase(compDiv)) {
            saveDistributorNotifyUrls(saved.getId(), notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4);
        }

        if (usesCommissionPolicyForCompDiv(compDivVal)) {
            applyCommissionPolicyForOrgCode(saved.getCode(), compDivVal, commissionFollowHq, hqPolicyScope,
                    perTxFee, cancelRate, voidFeePerTx, manualVoidFeePerTx, usageRate, failFee, payRate, refundRate, rollingPct, rollingDays,
                    feeSettlementPerTx, remittanceTransferFee, usdtTransferFeeUsd, feeUsdt, feeFx, fee3dsRate, chargebackFeePerTx, chargebackPolicyId,
                    voidSettlementMode, manualVoidSettlementMode, refundSettlementMode, forceRefundSettlementMode,
                    true);
            if ("MERCHANT".equalsIgnoreCase(compDivVal)) {
                applyMerchantIndependentChargebackPolicy(saved.getCode(), chargebackPolicyId);
            }
        }

        String rawPwdFinal = (pwd != null && !pwd.trim().isEmpty()) ? pwd.trim() : null;
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("로그인ID는 필수입니다.");
        }
        if (rawPwdFinal == null) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        if (loginId != null && !loginId.trim().isEmpty()) {
            String lid = loginId.trim();
            if (userRepository.findByUsername(lid).isPresent()) {
                throw new IllegalArgumentException("이미 사용 중인 로그인ID입니다: " + lid);
            }
            String encodedPwd = passwordEncoder.encode(rawPwdFinal);
            mp.setPwd(encodedPwd);
            merchantProfileRepository.save(mp);
            AppUser appUser = new AppUser();
            appUser.setUsername(lid);
            appUser.setPassword(encodedPwd);
            appUser.setName(ceoNm != null && !ceoNm.trim().isEmpty() ? ceoNm.trim()
                    : (name != null && !name.isEmpty() ? name : lid));
            appUser.setRole("USER");
            appUser.setEnabled(true);
            appUser.setOrgUnitCode(saved.getCode());
            appUser.setPermissionGroupNm("업체사용자");
            appUser.setOtpRegisteredYn("N");
            userRepository.save(appUser);
        }

        if (OrgUseYnUtil.S.equals(OrgUseYnUtil.normalize(useYn))) {
            applyOrgUseYnChange(saved, mp, useYn);
        }

        orgUnitChangeAuditService.appendIfChanged(saved.getId(), saved.getCode(), nz(saved.getName()),
                "[업체등록] 신규등록", "-",
                "등록 · " + childLevel.getNameKo() + " · 코드 " + saved.getCode() + " · 사용 " + ynDisplay(useYn));

        return saved;
    }

    public boolean isLoginIdAvailable(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) return false;
        String lid = loginId.trim();
        if (userRepository.findByUsername(lid).isPresent()) return false;
        return merchantProfileRepository.findByLoginId(lid).isEmpty();
    }

    private static String mergeRegionalSettings(String baseJson, String incomingJson,
                                                String assistantLoginId, String assistantPwd, String assistantRoleType, String brandingEditAllowedYn) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> rs = new java.util.LinkedHashMap<>();
            if (baseJson != null && !baseJson.isBlank()) {
                try {
                    java.util.Map<String, Object> old = om.readValue(baseJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    if (old != null) rs.putAll(old);
                } catch (Exception ignored) {}
            }
            if (incomingJson != null && !incomingJson.isBlank()) {
                try {
                    java.util.Map<String, Object> in = om.readValue(incomingJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    if (in != null) rs.putAll(in);
                } catch (Exception ignored) {}
            }
            if (assistantLoginId != null) rs.put("assistantLoginId", assistantLoginId.trim());
            if (assistantRoleType != null && !assistantRoleType.isBlank()) rs.put("assistantRoleType", normalizeAssistantRoleType(assistantRoleType));
            if (brandingEditAllowedYn != null && !brandingEditAllowedYn.isBlank()) {
                rs.put("brandingEditAllowedYn", "Y".equalsIgnoreCase(brandingEditAllowedYn.trim()) ? "Y" : "N");
            }
            return om.writeValueAsString(rs);
        } catch (Exception e) {
            return baseJson;
        }
    }

    private static Map<String, Object> parseRegionalSettings(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = om.readValue(json.trim(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private static boolean hasOwnHolidaySetting(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) return false;
        return hasText(rs.get("holidayProfileName"))
                || hasText(rs.get("holidayCountryCode"))
                || hasText(rs.get("holidayCountryCodes"))
                || hasText(rs.get("businessHolidayExtraDates"));
    }

    private static boolean incomingHasHolidaySetting(String incomingJson) {
        if (incomingJson == null || incomingJson.isBlank()) return false;
        Map<String, Object> rs = parseRegionalSettings(incomingJson);
        return hasOwnHolidaySetting(rs);
    }

    private static boolean isHolidayManagedByHeadquarters(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) return false;
        Object v = rs.get("holidayManagedByHeadquartersYn");
        return v != null && "Y".equalsIgnoreCase(String.valueOf(v).trim());
    }

    private boolean isParentRegionalHolidayManagedByHeadquarters(Long parentId) {
        Long cur = parentId;
        while (cur != null) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) break;
            OrgUnit org = opt.get();
            if (org.getOrgLevel() == OrgLevel.REGIONAL) {
                Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(org.getId());
                if (mpOpt.isEmpty()) return false;
                Map<String, Object> rs = parseRegionalSettings(mpOpt.get().getRegionalSettings());
                return isHolidayManagedByHeadquarters(rs);
            }
            cur = org.getParentId();
        }
        return false;
    }

    private Map<String, Object> resolveInheritedHolidaySettings(Long parentId) {
        Long cur = parentId;
        while (cur != null) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) break;
            OrgUnit org = opt.get();
            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(org.getId());
            if (mpOpt.isPresent()) {
                Map<String, Object> rs = parseRegionalSettings(mpOpt.get().getRegionalSettings());
                if (!rs.isEmpty() && org.getOrgLevel() == OrgLevel.REGIONAL) {
                    Map<String, Object> onlyHoliday = new LinkedHashMap<>();
                    copyIfPresent(rs, onlyHoliday, "holidayProfileName");
                    copyIfPresent(rs, onlyHoliday, "holidayProfileCountry");
                    copyIfPresent(rs, onlyHoliday, "holidayCountryCode");
                    copyIfPresent(rs, onlyHoliday, "holidayCountryCodes");
                    copyIfPresent(rs, onlyHoliday, "businessHolidayExtraDates");
                    copyIfPresent(rs, onlyHoliday, "businessHolidayRangesJson");
                    return onlyHoliday;
                }
            }
            cur = org.getParentId();
        }
        return new LinkedHashMap<>();
    }

    private static void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String key) {
        Object v = from.get(key);
        if (v != null && !String.valueOf(v).isBlank()) to.put(key, v);
    }

    private static void clearHolidayKeys(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) return;
        rs.remove("holidayProfileName");
        rs.remove("holidayProfileCountry");
        rs.remove("holidayCountryCode");
        rs.remove("holidayCountryCodes");
        rs.remove("businessHolidayExtraDates");
        rs.remove("businessHolidayRangesJson");
    }

    private static boolean hasText(Object v) {
        return v != null && !String.valueOf(v).isBlank();
    }

    private Optional<OrgLevel> resolveCurrentActorOrgLevel() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) return Optional.empty();
            String username = null;
            Object p = auth.getPrincipal();
            if (p instanceof AppUser u && u.getUsername() != null && !u.getUsername().isBlank()) {
                username = u.getUsername().trim();
            } else if (auth.getName() != null && !auth.getName().isBlank()) {
                username = auth.getName().trim();
            }
            if (username == null || username.isBlank()) return Optional.empty();
            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByLoginId(username);
            if (mpOpt.isPresent()) {
                return orgUnitRepository.findById(mpOpt.get().getOrgUnitId()).map(OrgUnit::getOrgLevel);
            }
            Optional<AppUser> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                AppUser u = userOpt.get();
                if ("ADMIN".equalsIgnoreCase(u.getRole())) return Optional.of(OrgLevel.HEADQUARTERS);
                if (u.getOrgUnitCode() != null && !u.getOrgUnitCode().isBlank()) {
                    return orgUnitRepository.findByCode(u.getOrgUnitCode().trim()).map(OrgUnit::getOrgLevel);
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static String normalizeAssistantRoleType(String roleType) {
        if (roleType == null || roleType.isBlank()) return "MANAGER";
        String v = roleType.trim().toUpperCase();
        if (SupervisorAssistantConstants.isSupervisorRoleType(v)) {
            return "MANAGER";
        }
        return switch (v) {
            case "MANAGER", "OPERATOR", "SETTLEMENT", "TECH", "CHATBOT_ADMIN" -> v;
            default -> "MANAGER";
        };
    }

    private static String permissionGroupByAssistantRole(String roleType) {
        if (SupervisorAssistantConstants.isSupervisorRoleType(roleType)) {
            return SupervisorAssistantConstants.PERMISSION_GROUP_NM;
        }
        if (ChatbotMerchantAdminConstants.ASSISTANT_ROLE_TYPE.equalsIgnoreCase(
                normalizeAssistantRoleType(roleType))) {
            return ChatbotMerchantAdminConstants.PERMISSION_GROUP_NM;
        }
        return switch (normalizeAssistantRoleType(roleType)) {
            case "OPERATOR" -> "운영담당";
            case "SETTLEMENT" -> "정산담당";
            case "TECH" -> "기술담당";
            default -> "관리담당";
        };
    }

    private record SettlementAuditSnap(
            String withdrawRestrictType,
            Integer withdrawLimitDays,
            String withdrawStartTime,
            String withdrawEndTime,
            String payLimitDefault,
            String payLimitExtra,
            String holdRate,
            Integer holdDays,
            String calcCycle,
            String calcCloseTime,
            String calcStartTime,
            Integer transferCycleDays,
            String calcProcType,
            String transferType,
            String autoTransferMin,
            String calcMinAmt,
            String transferExecTime,
            String payHoldYn,
            String calcExcludeYn,
            String calcExcludeTarget,
            String feeVatApplyYn,
            String feeVatRatePct) {
        static SettlementAuditSnap of(SettlementSetting s, OrgLevel orgLevel) {
            return new SettlementAuditSnap(
                    s.getWithdrawRestrictType() != null ? s.getWithdrawRestrictType().trim() : "",
                    s.getWithdrawLimitDays(),
                    s.getWithdrawStartTime() != null ? s.getWithdrawStartTime().toString() : "",
                    s.getWithdrawEndTime() != null ? s.getWithdrawEndTime().toString() : "",
                    s.getPayLimitDefault() != null ? s.getPayLimitDefault().toPlainString() : "",
                    s.getPayLimitExtra() != null ? s.getPayLimitExtra().toPlainString() : "",
                    s.getHoldRate() != null ? PercentDecimalHelper.toPlainOneDecimal(s.getHoldRate()) : "",
                    s.getHoldDays(),
                    orgLevel == OrgLevel.MERCHANT && s.getCalcCycle() != null ? s.getCalcCycle().trim() : "",
                    s.getCalcCloseTime() != null ? s.getCalcCloseTime().toString() : "",
                    s.getCalcStartTime() != null ? s.getCalcStartTime().toString() : "",
                    s.getTransferCycleDays(),
                    s.getCalcProcType() != null ? s.getCalcProcType().trim() : "",
                    s.getTransferType() != null ? s.getTransferType().trim() : "",
                    s.getAutoTransferMin() != null ? s.getAutoTransferMin().toPlainString() : "",
                    s.getCalcMinAmt() != null ? s.getCalcMinAmt().toPlainString() : "",
                    s.getTransferExecTime() != null ? s.getTransferExecTime().toString() : "",
                    s.getPayHoldYn() != null ? s.getPayHoldYn().trim() : "",
                    s.getCalcExcludeYn() != null ? s.getCalcExcludeYn().trim() : "",
                    s.getCalcExcludeTarget() != null ? s.getCalcExcludeTarget().trim() : "",
                    s.getFeeVatApplyYn() != null ? s.getFeeVatApplyYn().trim() : "",
                    s.getFeeVatRatePct() != null ? s.getFeeVatRatePct().stripTrailingZeros().toPlainString() : "");
        }

        void logDiff(OrgUnit ou, OrgUnitChangeAuditService audit, SettlementAuditSnap after) {
            if (after == null) {
                return;
            }
            String p = "[정산설정] ";
            long oid = ou.getId();
            String cid = ou.getCode() != null ? ou.getCode().trim() : "";
            String cnm = ou.getName() != null ? ou.getName().trim() : "";
            audit.appendIfChanged(oid, cid, cnm, p + "출금제한유형", withdrawRestrictType, after.withdrawRestrictType);
            audit.appendIfChanged(oid, cid, cnm, p + "출금제한일수",
                    withdrawLimitDays == null ? "" : String.valueOf(withdrawLimitDays),
                    after.withdrawLimitDays == null ? "" : String.valueOf(after.withdrawLimitDays));
            audit.appendIfChanged(oid, cid, cnm, p + "출금제한시작", withdrawStartTime, after.withdrawStartTime);
            audit.appendIfChanged(oid, cid, cnm, p + "출금제한종료", withdrawEndTime, after.withdrawEndTime);
            audit.appendIfChanged(oid, cid, cnm, p + "기본지급한도", payLimitDefault, after.payLimitDefault);
            audit.appendIfChanged(oid, cid, cnm, p + "추가지급한도", payLimitExtra, after.payLimitExtra);
            audit.appendIfChanged(oid, cid, cnm, p + "보류율", holdRate, after.holdRate);
            audit.appendIfChanged(oid, cid, cnm, p + "보류일수",
                    holdDays == null ? "" : String.valueOf(holdDays),
                    after.holdDays == null ? "" : String.valueOf(after.holdDays));
            audit.appendIfChanged(oid, cid, cnm, p + "정산주기", calcCycle, after.calcCycle);
            audit.appendIfChanged(oid, cid, cnm, p + "정산마감시각", calcCloseTime, after.calcCloseTime);
            audit.appendIfChanged(oid, cid, cnm, p + "정산개시시각", calcStartTime, after.calcStartTime);
            audit.appendIfChanged(oid, cid, cnm, p + "이체주기일수",
                    transferCycleDays == null ? "" : String.valueOf(transferCycleDays),
                    after.transferCycleDays == null ? "" : String.valueOf(after.transferCycleDays));
            audit.appendIfChanged(oid, cid, cnm, p + "정산구분", calcProcType, after.calcProcType);
            audit.appendIfChanged(oid, cid, cnm, p + "이체및송금구분", transferType, after.transferType);
            audit.appendIfChanged(oid, cid, cnm, p + "자동이체최소금액", autoTransferMin, after.autoTransferMin);
            audit.appendIfChanged(oid, cid, cnm, p + "정산최소금액", calcMinAmt, after.calcMinAmt);
            audit.appendIfChanged(oid, cid, cnm, p + "이체실행시각", transferExecTime, after.transferExecTime);
            audit.appendIfChanged(oid, cid, cnm, p + "지급보류", payHoldYn, after.payHoldYn);
            audit.appendIfChanged(oid, cid, cnm, p + "정산제외", calcExcludeYn, after.calcExcludeYn);
            audit.appendIfChanged(oid, cid, cnm, p + "정산제외대상", calcExcludeTarget, after.calcExcludeTarget);
            audit.appendIfChanged(oid, cid, cnm, p + "수수료VAT적용", feeVatApplyYn, after.feeVatApplyYn);
            audit.appendIfChanged(oid, cid, cnm, p + "수수료VAT율(%)", feeVatRatePct, after.feeVatRatePct);
        }
    }

    public Optional<Map<String, Object>> getSettlementSetting(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId())
                        .map(ss -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("compId", ou.getCode());
                            m.put("orgUnitId", ou.getId());
                            m.put("withdrawLimitDays", ss.getWithdrawLimitDays());
                            m.put("withdrawRestrictType", ss.getWithdrawRestrictType());
                            m.put("withdrawStartTime", ss.getWithdrawStartTime() != null ? ss.getWithdrawStartTime().toString() : null);
                            m.put("withdrawEndTime", ss.getWithdrawEndTime() != null ? ss.getWithdrawEndTime().toString() : null);
                            m.put("payLimitDefault", ss.getPayLimitDefault());
                            m.put("payLimitExtra", ss.getPayLimitExtra());
                            m.put("holdRate", ss.getHoldRate() != null ? PercentDecimalHelper.toPlainOneDecimal(ss.getHoldRate()) : null);
                            m.put("holdDays", ss.getHoldDays());
                            m.put("calcCycle", ou.getOrgLevel() == OrgLevel.MERCHANT ? ss.getCalcCycle() : null);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                m.put("pendingCalcCycle", ss.getPendingCalcCycle());
                                m.put("pendingCalcCycleAt", ss.getPendingCalcCycleAt() != null ? ss.getPendingCalcCycleAt().toString() : null);
                                String pend = ss.getPendingCalcCycle();
                                m.put("calcCycleTransitionMode",
                                        (pend != null && !pend.isBlank()) ? "NEXT_AFTER_RUN" : "IMMEDIATE");
                            } else {
                                m.put("pendingCalcCycle", null);
                                m.put("pendingCalcCycleAt", null);
                                m.put("calcCycleTransitionMode", "IMMEDIATE");
                            }
                            m.put("calcProcType", ss.getCalcProcType());
                            m.put("transferType", ss.getTransferType());
                            m.put("calcCloseTime", ss.getCalcCloseTime() != null ? ss.getCalcCloseTime().toString() : null);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                String normGs = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
                                String procGs = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
                                if ("MANUAL".equalsIgnoreCase(procGs) || !SettlementCycleTiming.isCalcStartTimeApplicableForAuto(normGs)) {
                                    m.put("calcStartTime", null);
                                } else {
                                    m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : null);
                                }
                            } else {
                                m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : null);
                            }
                            m.put("transferExecTime", ss.getTransferExecTime() != null ? ss.getTransferExecTime().toString() : null);
                            m.put("autoTransferMin", ss.getAutoTransferMin());
                            m.put("calcMinAmt", ss.getCalcMinAmt());
                            m.put("payHoldYn", ss.getPayHoldYn());
                            m.put("calcExcludeYn", ss.getCalcExcludeYn());
                            m.put("calcExcludeTarget", ss.getCalcExcludeTarget());
                            m.put("feeVatApplyYn", ss.getFeeVatApplyYn());
                            m.put("feeVatRatePct", ss.getFeeVatRatePct() != null ? ss.getFeeVatRatePct().stripTrailingZeros().toPlainString() : null);
                            return m;
                        }));
    }

    /** 가맹점 상세 저장 시 정산(tb_settlement_setting) 일괄 반영 */
    public boolean saveSettlementSetting(String compId, String withdrawRestrictType, Integer withdrawLimitDays,
                                         String withdrawStartTime, String withdrawEndTime,
                                         String payLimitDefault, String payLimitExtra,
                                         String holdRate, Integer holdDays, String calcCycle,
                                         String calcCloseTime, String calcStartTime, Integer transferCycleDays,
                                         String calcProcType, String transferType, String autoTransferMin, String payHoldYn,
                                         String calcExcludeYn, String calcExcludeTarget,
                                         String calcMinAmt, String transferExecTime,
                                         String feeVatApplyYn, String feeVatRatePct,
                                         String calcCycleTransitionMode, String calcCycleChangeRemark,
                                         String actorUsername) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId())
                        .map(ss -> {
                            SettlementAuditSnap beforeSnap = SettlementAuditSnap.of(ss, ou.getOrgLevel());
                            if (withdrawRestrictType != null) {
                                String w = withdrawRestrictType.trim();
                                ss.setWithdrawRestrictType(w.isEmpty() ? null : w);
                            }
                            if (withdrawLimitDays != null) ss.setWithdrawLimitDays(withdrawLimitDays);
                            if (parseTime(withdrawStartTime) != null) ss.setWithdrawStartTime(parseTime(withdrawStartTime));
                            if (parseTime(withdrawEndTime) != null) ss.setWithdrawEndTime(parseTime(withdrawEndTime));
                            if (payLimitDefault != null && !payLimitDefault.isEmpty()) try { ss.setPayLimitDefault(new BigDecimal(payLimitDefault.trim())); } catch (Exception ignored) {}
                            if (payLimitExtra != null && !payLimitExtra.isEmpty()) try { ss.setPayLimitExtra(new BigDecimal(payLimitExtra.trim())); } catch (Exception ignored) {}
                            if (holdRate != null && !holdRate.isEmpty()) {
                                ss.setHoldRate(PercentDecimalHelper.parsePercentOneDecimal(holdRate));
                            }
                            if (holdDays != null) ss.setHoldDays(holdDays);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                if (calcCycle != null && !calcCycle.isEmpty()) {
                                    masterDistSettlementCycleConfigService.validateMerchantCalcCycle(
                                            ou.getId(), calcCycle);
                                    String newNorm = SettlementPeriodResolver.normalizeCalcCycle(calcCycle.trim());
                                    String oldNorm = ss.getCalcCycle() != null
                                            ? SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle().trim())
                                            : "";
                                    boolean cycleChanged = !newNorm.equals(oldNorm);
                                    String modeRaw = calcCycleTransitionMode != null ? calcCycleTransitionMode.trim() : "";
                                    String mode = "NEXT_AFTER_RUN".equalsIgnoreCase(modeRaw)
                                            ? SettlementCalcCycleTransitionService.MODE_NEXT_AFTER_RUN
                                            : SettlementCalcCycleTransitionService.MODE_IMMEDIATE;
                                    String actor = (actorUsername != null && !actorUsername.isBlank())
                                            ? actorUsername.trim()
                                            : resolveActorUsernameFallback();
                                    String remark = calcCycleChangeRemark != null ? calcCycleChangeRemark.trim() : "";
                                    if (cycleChanged) {
                                        if (SettlementCalcCycleTransitionService.MODE_NEXT_AFTER_RUN.equals(mode)) {
                                            ss.setPendingCalcCycle(newNorm);
                                            ss.setPendingCalcCycleAt(LocalDateTime.now());
                                            settlementCalcCycleTransitionService.logChange(ou, oldNorm, newNorm, mode, remark, actor);
                                        } else {
                                            ss.setCalcCycle(newNorm);
                                            ss.setPendingCalcCycle(null);
                                            ss.setPendingCalcCycleAt(null);
                                            settlementCalcCycleTransitionService.logChange(ou, oldNorm, newNorm, mode, remark, actor);
                                        }
                                    }
                                }
                            } else {
                                ss.setCalcCycle(null);
                                ss.setPendingCalcCycle(null);
                                ss.setPendingCalcCycleAt(null);
                            }
                            if (transferCycleDays != null) ss.setTransferCycleDays(transferCycleDays);
                            if (calcProcType != null && !calcProcType.isEmpty()) ss.setCalcProcType(calcProcType.trim());
                            if (transferType != null && !transferType.isEmpty()) ss.setTransferType(transferType.trim());
                            if (autoTransferMin != null && !autoTransferMin.isEmpty()) try { ss.setAutoTransferMin(new BigDecimal(autoTransferMin.trim())); } catch (Exception ignored) {}
                            if (calcMinAmt != null && !calcMinAmt.isEmpty()) try { ss.setCalcMinAmt(new BigDecimal(calcMinAmt.trim())); } catch (Exception ignored) {}
                            if (parseTime(transferExecTime) != null) ss.setTransferExecTime(parseTime(transferExecTime));
                            if (payHoldYn != null && !payHoldYn.isEmpty()) ss.setPayHoldYn(payHoldYn);
                            if (calcExcludeYn != null && !calcExcludeYn.isEmpty()) ss.setCalcExcludeYn(calcExcludeYn);
                            if (calcExcludeTarget != null && !calcExcludeTarget.isEmpty()) ss.setCalcExcludeTarget(calcExcludeTarget);
                            if (feeVatApplyYn != null && !feeVatApplyYn.isBlank()) {
                                ss.setFeeVatApplyYn("Y".equalsIgnoreCase(feeVatApplyYn.trim()) ? "Y" : "N");
                            }
                            if (feeVatRatePct != null && !feeVatRatePct.isBlank()) {
                                try {
                                    ss.setFeeVatRatePct(new BigDecimal(feeVatRatePct.trim()));
                                } catch (Exception ignored) {
                                }
                            } else if (feeVatApplyYn != null && !feeVatApplyYn.isBlank()
                                    && "N".equalsIgnoreCase(feeVatApplyYn.trim())) {
                                ss.setFeeVatRatePct(BigDecimal.ZERO);
                            }
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                String normRt = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
                                if (SettlementCycleTiming.isRealtimeCode(normRt)) {
                                    String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
                                    if (proc.isEmpty() || "MANUAL".equalsIgnoreCase(proc)) {
                                        ss.setCalcProcType("AUTO");
                                    }
                                }
                                applyMerchantSettlementCloseStartFromForm(ss, calcCloseTime, calcStartTime);
                            }
                            settlementSettingRepository.save(ss);
                            SettlementAuditSnap afterSnap = SettlementAuditSnap.of(ss, ou.getOrgLevel());
                            beforeSnap.logDiff(ou, orgUnitChangeAuditService, afterSnap);
                            return true;
                        }))
                .orElse(false);
    }

    /** 레거시 등록 API 단일 transferType(구 이체구분) → 정산구분 + 이체및송금구분 */
    private static void applyLegacySettlementFields(SettlementSetting ss, String legacyTransferType) {
        if (legacyTransferType == null || legacyTransferType.isBlank()) {
            ss.setCalcProcType("MANUAL");
            ss.setTransferType("MANUAL");
            return;
        }
        switch (legacyTransferType.trim().toUpperCase()) {
            case "FUMBANKING" -> {
                ss.setCalcProcType("FUMBANKING");
                ss.setTransferType("AUTO");
            }
            case "AUTO" -> {
                ss.setCalcProcType("AUTO");
                ss.setTransferType("AUTO");
            }
            default -> {
                ss.setCalcProcType("MANUAL");
                ss.setTransferType("MANUAL");
            }
        }
    }

    /** 업체코드 10자리 자동 부여. 총본사=0000000000, 총판=0000000001부터(총판끼리 순번), 나머지=접두2자리+순번8자리. */
    private synchronized String generateNextCompCode(String compDiv) {
        String prefix = compCodePrefixFromCompDiv(compDiv);
        if ("00".equals(prefix)) {
            return "0000000000";
        }
        if ("20".equals(prefix)) {
            long max = 0;
            for (OrgUnit o : orgUnitRepository.findAll()) {
                if (o.getOrgLevel() != OrgLevel.MASTER_DIST) continue;
                String c = o.getCode();
                if (c != null && c.length() == 10 && c.matches("\\d{10}")) {
                    try {
                        long n = Long.parseLong(c);
                        if (n > max) max = n;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return String.format("%010d", max + 1);
        }
        List<OrgUnit> all = orgUnitRepository.findAll();
        long max = 0;
        for (OrgUnit o : all) {
            String c = o.getCode();
            if (c != null && c.startsWith(prefix) && c.length() == 10 && c.matches("\\d{10}")) {
                try {
                    long n = Long.parseLong(c.substring(2));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix + String.format("%08d", max + 1);
    }

    private static String optStr(Map<String, Object> m, String key) {
        Object v = m != null ? m.get(key) : null;
        return v != null ? v.toString().trim() : null;
    }

    private static boolean usesCommissionPolicyForCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isBlank()) return false;
        String d = compDiv.trim().toUpperCase();
        return "MERCHANT".equals(d) || "REGIONAL".equals(d) || "MASTER_DIST".equals(d);
    }

    private static boolean allCommissionParamsAbsent(String commissionFollowHq, String hqPolicyScope,
                                                     String perTxFee, String cancelRate, String voidFeePerTx, String manualVoidFeePerTx,
                                                     String usageRate,
                                                     String failFee, String payRate, String refundRate,
                                                     String rollingPct, String rollingDays,
                                                     String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
                                                     String fee3dsRate, String chargebackFeePerTx, String chargebackPolicyId,
                                                     String voidSettlementMode, String manualVoidSettlementMode, String refundSettlementMode, String forceRefundSettlementMode) {
        return commissionFollowHq == null && hqPolicyScope == null && perTxFee == null && cancelRate == null
                && voidFeePerTx == null && manualVoidFeePerTx == null
                && usageRate == null && failFee == null && payRate == null && refundRate == null
                && rollingPct == null && rollingDays == null && feeSettlementPerTx == null
                && remittanceTransferFee == null && usdtTransferFeeUsd == null
                && feeUsdt == null && feeFx == null
                && fee3dsRate == null && chargebackFeePerTx == null && chargebackPolicyId == null
                && voidSettlementMode == null && manualVoidSettlementMode == null && refundSettlementMode == null
                && forceRefundSettlementMode == null;
    }

    private static boolean allCommissionFeeScalarParamsAbsent(
            String perTxFee, String cancelRate, String voidFeePerTx, String manualVoidFeePerTx,
            String usageRate,
            String failFee, String payRate, String refundRate,
            String rollingPct, String rollingDays,
            String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
            String fee3dsRate, String chargebackFeePerTx, String chargebackPolicyId,
            String voidSettlementMode, String manualVoidSettlementMode, String refundSettlementMode, String forceRefundSettlementMode) {
        return perTxFee == null && cancelRate == null
                && voidFeePerTx == null && manualVoidFeePerTx == null
                && usageRate == null && failFee == null && payRate == null && refundRate == null
                && rollingPct == null && rollingDays == null && feeSettlementPerTx == null
                && remittanceTransferFee == null && usdtTransferFeeUsd == null
                && feeUsdt == null && feeFx == null
                && fee3dsRate == null && chargebackFeePerTx == null && chargebackPolicyId == null
                && voidSettlementMode == null && manualVoidSettlementMode == null && refundSettlementMode == null
                && forceRefundSettlementMode == null;
    }

    private void mergeCommissionUiIntoRegionalSettings(MerchantProfile mp, String commissionFollowHq, String hqPolicyScope) {
        Map<String, Object> rs = parseRegionalSettings(mp.getRegionalSettings());
        String cf = commissionFollowHq != null && !commissionFollowHq.isBlank() ? commissionFollowHq.trim() : "Y";
        rs.put("commissionFollowHq", "N".equalsIgnoreCase(cf) ? "N" : "Y");
        rs.put("hqPolicyScope", hqPolicyScope != null ? hqPolicyScope.trim() : "");
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            mp.setRegionalSettings(om.writeValueAsString(rs));
        } catch (Exception ignored) {
            mp.setRegionalSettings("{\"commissionFollowHq\":\"" + ("N".equalsIgnoreCase(cf) ? "N" : "Y")
                    + "\",\"hqPolicyScope\":\"" + (hqPolicyScope != null ? hqPolicyScope.trim().replace("\"", "\\\"") : "")
                    + "\"}");
        }
    }

    private void applyCommissionPolicyForOrgCode(String compCode, String compDiv,
                                                 String commissionFollowHq, String hqPolicyScope,
                                                 String perTxFee, String cancelRate, String voidFeePerTx, String manualVoidFeePerTx,
                                                 String usageRate,
                                                 String failFee, String payRate, String refundRate,
                                                 String rollingPct, String rollingDays,
                                                 String feeSettlementPerTx, String remittanceTransferFee, String usdtTransferFeeUsd, String feeUsdt, String feeFx,
                                                 String fee3dsRate, String chargebackFeePerTx, String chargebackPolicyId,
                                                 String voidSettlementMode, String manualVoidSettlementMode, String refundSettlementMode, String forceRefundSettlementMode,
                                                 boolean forceRedistributeFromTemplate) {
        if (!usesCommissionPolicyForCompDiv(compDiv) || compCode == null || compCode.isBlank()) {
            return;
        }
        boolean custom = "N".equalsIgnoreCase(commissionFollowHq != null ? commissionFollowHq.trim() : "");
        if (custom) {
            CommissionPolicy policy = commissionPolicyRepository.findByScope(compCode.trim()).orElseGet(CommissionPolicy::new);
            policy.setScope(compCode.trim());
            if (perTxFee != null && !perTxFee.trim().isEmpty()) {
                policy.setPerTxFee(PercentDecimalHelper.parseAmountOneDecimal(perTxFee.trim()));
            }
            if (cancelRate != null && !cancelRate.trim().isEmpty()) {
                policy.setCancelRate(PercentDecimalHelper.parseAmountOneDecimal(cancelRate.trim()));
            }
            if (voidFeePerTx != null && !voidFeePerTx.trim().isEmpty()) {
                policy.setVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(voidFeePerTx.trim()));
            }
            if (manualVoidFeePerTx != null && !manualVoidFeePerTx.trim().isEmpty()) {
                policy.setManualVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(manualVoidFeePerTx.trim()));
            }
            if (usageRate != null && !usageRate.trim().isEmpty()) {
                policy.setUsageRate(PercentDecimalHelper.parseAmountOneDecimal(usageRate.trim()));
            }
            if (failFee != null && !failFee.trim().isEmpty()) {
                policy.setFailFee(PercentDecimalHelper.parseAmountOneDecimal(failFee.trim()));
            }
            if (payRate != null && !payRate.trim().isEmpty()) {
                policy.setPayRate(PercentDecimalHelper.parsePercentOneDecimal(payRate));
            }
            if (refundRate != null && !refundRate.trim().isEmpty()) {
                policy.setRefundRate(PercentDecimalHelper.parseAmountOneDecimal(refundRate.trim()));
            }
            if (rollingPct != null && !rollingPct.trim().isEmpty()) {
                policy.setRollingPct(PercentDecimalHelper.parsePercentOneDecimal(rollingPct));
            }
            if (rollingDays != null && !rollingDays.trim().isEmpty()) try {
                policy.setRollingDays(Integer.parseInt(rollingDays.trim()));
            } catch (Exception ignored) {
            }
            if (feeSettlementPerTx != null && !feeSettlementPerTx.trim().isEmpty()) {
                policy.setFeeSettlementPerTx(PercentDecimalHelper.parseAmountOneDecimal(feeSettlementPerTx.trim()));
            }
            if (remittanceTransferFee != null && !remittanceTransferFee.trim().isEmpty()) {
                policy.setRemittanceTransferFee(PercentDecimalHelper.parseAmountOneDecimal(remittanceTransferFee.trim()));
            }
            if (usdtTransferFeeUsd != null && !usdtTransferFeeUsd.trim().isEmpty()) {
                policy.setUsdtTransferFeeUsd(PercentDecimalHelper.parseAmountOneDecimal(usdtTransferFeeUsd.trim()));
            }
            if (feeUsdt != null && !feeUsdt.trim().isEmpty()) {
                policy.setFeeUsdt(PercentDecimalHelper.parsePercentOneDecimal(feeUsdt));
            }
            if (feeFx != null && !feeFx.trim().isEmpty()) {
                policy.setFeeFx(PercentDecimalHelper.parsePercentOneDecimal(feeFx));
            }
            if (fee3dsRate != null && !fee3dsRate.trim().isEmpty()) {
                policy.setFee3dsRate(PercentDecimalHelper.parseAmountOneDecimal(fee3dsRate));
            }
            if (chargebackFeePerTx != null && !chargebackFeePerTx.trim().isEmpty()) {
                policy.setChargebackFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(chargebackFeePerTx.trim()));
            }
            if (chargebackPolicyId != null) {
                String cp = chargebackPolicyId.trim();
                if (cp.isEmpty()) {
                    policy.setChargebackPolicyId(null);
                } else {
                    try {
                        policy.setChargebackPolicyId(Long.parseLong(cp));
                    } catch (Exception ignored) {
                    }
                }
            }
            if (voidSettlementMode != null) {
                String t = voidSettlementMode.trim();
                policy.setVoidSettlementMode(t.isEmpty() || "FOLLOW".equalsIgnoreCase(t) ? null : VoidRefundSettlementModeUtil.normalize(t));
            }
            if (manualVoidSettlementMode != null) {
                String t = manualVoidSettlementMode.trim();
                policy.setManualVoidSettlementMode(t.isEmpty() || "FOLLOW".equalsIgnoreCase(t) ? null : VoidRefundSettlementModeUtil.normalize(t));
            }
            if (refundSettlementMode != null) {
                String t = refundSettlementMode.trim();
                policy.setRefundSettlementMode(t.isEmpty() || "FOLLOW".equalsIgnoreCase(t) ? null : VoidRefundSettlementModeUtil.normalize(t));
            }
            if (forceRefundSettlementMode != null) {
                String t = forceRefundSettlementMode.trim();
                policy.setForceRefundSettlementMode(t.isEmpty() || "FOLLOW".equalsIgnoreCase(t) ? null : VoidRefundSettlementModeUtil.normalize(t));
            }
            commissionPolicyRepository.save(policy);
        } else {
            String srcScope = (hqPolicyScope != null && !hqPolicyScope.trim().isEmpty()) ? hqPolicyScope.trim() : "DEFAULT";
            commissionPolicyRepository.findByScope(srcScope).ifPresent(src -> {
                CommissionPolicy policy = commissionPolicyRepository.findByScope(compCode.trim()).orElseGet(CommissionPolicy::new);
                policy.setScope(compCode.trim());
                policy.setPerTxFee(src.getPerTxFee());
                policy.setUsageRate(src.getUsageRate());
                policy.setFailFee(src.getFailFee());
                policy.setCancelRate(src.getCancelRate());
                policy.setVoidFeePerTx(src.getVoidFeePerTx());
                policy.setManualVoidFeePerTx(src.getManualVoidFeePerTx());
                policy.setRefundRate(src.getRefundRate());
                policy.setPayRate(src.getPayRate());
                policy.setFeeSettlementPerTx(src.getFeeSettlementPerTx());
                policy.setRemittanceTransferFee(src.getRemittanceTransferFee());
                policy.setUsdtTransferFeeUsd(src.getUsdtTransferFeeUsd());
                policy.setFeeUsdt(src.getFeeUsdt());
                policy.setFeeFx(src.getFeeFx());
                policy.setRollingPct(src.getRollingPct());
                policy.setRollingDays(src.getRollingDays());
                policy.setCurrencyCode(src.getCurrencyCode());
                policy.setPolicyRemark(src.getPolicyRemark());
                policy.setFee3dsRate(src.getFee3dsRate());
                policy.setChargebackFeePerTx(src.getChargebackFeePerTx());
                policy.setChargebackPolicyId(src.getChargebackPolicyId());
                policy.setVoidSettlementMode(src.getVoidSettlementMode());
                policy.setManualVoidSettlementMode(src.getManualVoidSettlementMode());
                policy.setRefundSettlementMode(src.getRefundSettlementMode());
                policy.setForceRefundSettlementMode(src.getForceRefundSettlementMode());
                policy.setExtraFee1Name(src.getExtraFee1Name());
                policy.setExtraFee1Mode(src.getExtraFee1Mode());
                policy.setExtraFee1Value(src.getExtraFee1Value());
                policy.setExtraFee2Name(src.getExtraFee2Name());
                policy.setExtraFee2Mode(src.getExtraFee2Mode());
                policy.setExtraFee2Value(src.getExtraFee2Value());
                policy.setExtraFee3Name(src.getExtraFee3Name());
                policy.setExtraFee3Mode(src.getExtraFee3Mode());
                policy.setExtraFee3Value(src.getExtraFee3Value());
                policy.setExtraFee4Name(src.getExtraFee4Name());
                policy.setExtraFee4Mode(src.getExtraFee4Mode());
                policy.setExtraFee4Value(src.getExtraFee4Value());
                policy.setTierCommissionJson(src.getTierCommissionJson());
                commissionPolicyRepository.save(policy);
                /*
                 * 배분(tb_distribution_fee_config)은 수수료관리에서 가맹별로 조정한다.
                 * HQ 템플릿 재적용은 신규·배분 비어 있음·따름/정책선택 변경 시에만 수행한다.
                 * (템플릿의 본사 요율 빈칸→0 으로 기존 1.7% 등이 초기화되는 사고 방지)
                 */
                boolean dfEmpty = distributionFeeConfigRepository.findByCompId(compCode.trim())
                        .map(CompService::isDistributionFeeConfigEffectivelyEmpty)
                        .orElse(true);
                if (forceRedistributeFromTemplate || dfEmpty) {
                    applyDistributionFromCommissionPolicyTemplate(src, compCode.trim());
                    try {
                        commissionService.recordHqTemplateApplyHistory(compCode.trim(), srcScope);
                    } catch (Exception ignored) {
                        /* 이력 실패해도 정책 반영은 유지 */
                    }
                } else {
                    /* 기존 배분 유지 시 결제율·건당 합계를 배분과 맞춤 */
                    distributionFeeConfigRepository.findByCompId(compCode.trim()).ifPresent(df -> {
                        java.math.BigDecimal totalRate = nzBd(df.getHqRate()).add(nzBd(df.getRegionalRate()))
                                .add(nzBd(df.getMasterRate())).add(nzBd(df.getBranchRate()))
                                .add(nzBd(df.getAgencyRate())).add(nzBd(df.getSalesOfficeRate()));
                        java.math.BigDecimal totalPerTx = nzBd(df.getHqPerTxFee()).add(nzBd(df.getRegionalPerTxFee()))
                                .add(nzBd(df.getMasterPerTxFee())).add(nzBd(df.getBranchPerTxFee()))
                                .add(nzBd(df.getAgencyPerTxFee())).add(nzBd(df.getSalesOfficePerTxFee()));
                        if (totalRate.signum() > 0) {
                            policy.setPayRate(totalRate);
                        }
                        if (totalPerTx.signum() > 0) {
                            policy.setPerTxFee(totalPerTx);
                        }
                        commissionPolicyRepository.save(policy);
                    });
                }
            });
        }
        orgUnitRepository.findByCode(compCode.trim()).ifPresent(ouSync ->
                settlementSettingRepository.findByOrgUnitId(ouSync.getId()).ifPresent(ssSync ->
                        syncVoidRefundSettlementModesToSettlementSetting(ouSync, ssSync, compDiv,
                                "N".equalsIgnoreCase(commissionFollowHq != null ? commissionFollowHq.trim() : ""))));
    }

    private static boolean isDistributionFeeConfigEffectivelyEmpty(DistributionFeeConfig df) {
        if (df == null) {
            return true;
        }
        return isZeroBd(df.getHqRate()) && isZeroBd(df.getRegionalRate()) && isZeroBd(df.getMasterRate())
                && isZeroBd(df.getBranchRate()) && isZeroBd(df.getAgencyRate()) && isZeroBd(df.getSalesOfficeRate())
                && isZeroBd(df.getHqPerTxFee()) && isZeroBd(df.getRegionalPerTxFee()) && isZeroBd(df.getMasterPerTxFee())
                && isZeroBd(df.getBranchPerTxFee()) && isZeroBd(df.getAgencyPerTxFee()) && isZeroBd(df.getSalesOfficePerTxFee());
    }

    private static boolean isZeroBd(java.math.BigDecimal v) {
        return v == null || v.compareTo(java.math.BigDecimal.ZERO) == 0;
    }

    private static java.math.BigDecimal nzBd(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }

    /**
     * 정산 실행 시 사용하는 무효·환불 방식(tb_settlement_setting)을 수수료정책 저장 결과와 맞춥니다.
     */
    private void syncVoidRefundSettlementModesToSettlementSetting(OrgUnit ou, SettlementSetting ss, String compDiv,
                                                                  boolean customCommission) {
        String div = compDiv != null ? compDiv.trim().toUpperCase(Locale.ROOT) : "";
        String code = ou.getCode() != null ? ou.getCode().trim() : "";
        if (code.isEmpty()) {
            return;
        }
        if ("MERCHANT".equals(div)) {
            commissionPolicyRepository.findByScope(code).ifPresent(p -> {
                boolean any = !VoidRefundSettlementModeResolutionService.isBlankOrFollow(p.getVoidSettlementMode())
                        || !VoidRefundSettlementModeResolutionService.isBlankOrFollow(p.getManualVoidSettlementMode())
                        || !VoidRefundSettlementModeResolutionService.isBlankOrFollow(p.getRefundSettlementMode())
                        || !VoidRefundSettlementModeResolutionService.isBlankOrFollow(p.getForceRefundSettlementMode());
                if (customCommission && any) {
                    ss.setVoidRefundSettlementOverrideYn("Y");
                    ss.setVoidSettlementMode(VoidRefundSettlementModeResolutionService.upperOrNull(p.getVoidSettlementMode()));
                    ss.setManualVoidSettlementMode(VoidRefundSettlementModeResolutionService.upperOrNull(p.getManualVoidSettlementMode()));
                    ss.setRefundSettlementMode(VoidRefundSettlementModeResolutionService.upperOrNull(p.getRefundSettlementMode()));
                    ss.setForceRefundSettlementMode(VoidRefundSettlementModeResolutionService.upperOrNull(p.getForceRefundSettlementMode()));
                } else {
                    ss.setVoidRefundSettlementOverrideYn("N");
                    ss.setVoidSettlementMode(null);
                    ss.setManualVoidSettlementMode(null);
                    ss.setRefundSettlementMode(null);
                    ss.setForceRefundSettlementMode(null);
                }
                settlementSettingRepository.save(ss);
            });
        } else if ("MASTER_DIST".equals(div) || "REGIONAL".equals(div)) {
            commissionPolicyRepository.findByScope(code).ifPresent(p -> {
                ss.setVoidRefundSettlementOverrideYn("N");
                ss.setVoidSettlementMode(normalizeVoidModeOrNull(p.getVoidSettlementMode()));
                ss.setManualVoidSettlementMode(normalizeVoidModeOrNull(p.getManualVoidSettlementMode()));
                ss.setRefundSettlementMode(normalizeVoidModeOrNull(p.getRefundSettlementMode()));
                ss.setForceRefundSettlementMode(normalizeVoidModeOrNull(p.getForceRefundSettlementMode()));
                settlementSettingRepository.save(ss);
            });
        }
    }

    private static String normalizeVoidModeOrNull(String raw) {
        if (VoidRefundSettlementModeResolutionService.isBlankOrFollow(raw)) {
            return null;
        }
        return VoidRefundSettlementModeUtil.normalize(raw);
    }

    /** 본사 템플릿 격자의 결제율·건당 열 → 가맹점 배분(tb_distribution_fee_config) */
    private void applyDistributionFromCommissionPolicyTemplate(CommissionPolicy src, String compCode) {
        if (compCode == null || compCode.isBlank() || src == null) {
            return;
        }
        String json = src.getTierCommissionJson();
        if (json == null || json.isBlank()) {
            json = CommissionTierJsonHelper.buildTierJsonFromPolicyScalars(src);
        }
        DistributionFeeConfig df = distributionFeeConfigRepository.findByCompId(compCode.trim()).orElseGet(() -> {
            DistributionFeeConfig x = new DistributionFeeConfig();
            x.setCompId(compCode.trim());
            return x;
        });
        CommissionTierJsonHelper.applyTierJsonToDistribution(json, df);
        distributionFeeConfigRepository.save(df);
    }

    /** 가맹 상세: 무효·환불 정산 방식은 tb_settlement_setting 상속 플래그에 맞춰 셀렉트 표시(FOLLOW=총판·본사 따름). */
    private static void applyMerchantVoidRefundModesDetailFromSettlement(SettlementSetting ss, Map<String, Object> m) {
        if (VoidRefundSettlementModeResolutionService.merchantOverridesVoidRefund(ss)) {
            m.put("voidSettlementMode", VoidRefundSettlementModeResolutionService.modeForDetailForm(ss.getVoidSettlementMode()));
            m.put("manualVoidSettlementMode", VoidRefundSettlementModeResolutionService.modeForDetailForm(ss.getManualVoidSettlementMode()));
            m.put("refundSettlementMode", VoidRefundSettlementModeResolutionService.modeForDetailForm(ss.getRefundSettlementMode()));
            m.put("forceRefundSettlementMode", VoidRefundSettlementModeResolutionService.modeForDetailForm(ss.getForceRefundSettlementMode()));
        } else {
            m.put("voidSettlementMode", "FOLLOW");
            m.put("manualVoidSettlementMode", "FOLLOW");
            m.put("refundSettlementMode", "FOLLOW");
            m.put("forceRefundSettlementMode", "FOLLOW");
        }
    }

    private void applyCommissionDetailToMap(Map<String, Object> m, OrgUnit ou) {
        OrgLevel ol = ou.getOrgLevel();
        if (ol != OrgLevel.MERCHANT && ol != OrgLevel.REGIONAL && ol != OrgLevel.MASTER_DIST) {
            return;
        }
        Object cf = m.get("commissionFollowHq");
        if (cf == null || String.valueOf(cf).isBlank()) {
            m.put("commissionFollowHq", "Y");
        }
        if (!m.containsKey("hqPolicyScope") || m.get("hqPolicyScope") == null) {
            m.put("hqPolicyScope", "");
        }
        String code = ou.getCode();
        if (code == null || code.isBlank()) {
            return;
        }
        commissionPolicyRepository.findByScope(code.trim()).ifPresentOrElse(p -> putCommissionPolicyScalars(m, p),
                () -> commissionPolicyRepository.findByScope("DEFAULT").ifPresent(def -> putCommissionPolicyScalars(m, def)));
    }

    private static void putCommissionPolicyScalars(Map<String, Object> m, CommissionPolicy p) {
        m.put("perTxFee", p.getPerTxFee() != null ? p.getPerTxFee().toPlainString() : "");
        m.put("failFee", p.getFailFee() != null ? p.getFailFee().toPlainString() : "");
        m.put("usageRate", p.getUsageRate() != null ? p.getUsageRate().toPlainString() : "");
        m.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "");
        m.put("cancelRate", p.getCancelRate() != null ? p.getCancelRate().toPlainString() : "");
        m.put("voidFeePerTx", p.getVoidFeePerTx() != null ? p.getVoidFeePerTx().toPlainString() : "");
        m.put("manualVoidFeePerTx", p.getManualVoidFeePerTx() != null ? p.getManualVoidFeePerTx().toPlainString() : "");
        m.put("refundRate", p.getRefundRate() != null ? p.getRefundRate().toPlainString() : "");
        m.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "");
        m.put("rollingDays", p.getRollingDays() != null ? String.valueOf(p.getRollingDays()) : "");
        m.put("feeSettlementPerTx", p.getFeeSettlementPerTx() != null ? p.getFeeSettlementPerTx().toPlainString() : "");
        m.put("remittanceTransferFee", p.getRemittanceTransferFee() != null ? p.getRemittanceTransferFee().toPlainString() : "");
        m.put("usdtTransferFeeUsd", p.getUsdtTransferFeeUsd() != null ? p.getUsdtTransferFeeUsd().toPlainString() : "");
        m.put("feeUsdt", p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "");
        m.put("feeFx", p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "");
        m.put("commissionMemo", p.getPolicyRemark() != null ? p.getPolicyRemark() : "");
        m.put("extraFee1Name", p.getExtraFee1Name() != null ? p.getExtraFee1Name() : "");
        m.put("extraFee1Mode", p.getExtraFee1Mode() != null ? p.getExtraFee1Mode() : "");
        m.put("extraFee1Value", extraFeeValuePlain(p.getExtraFee1Mode(), p.getExtraFee1Value()));
        m.put("extraFee2Name", p.getExtraFee2Name() != null ? p.getExtraFee2Name() : "");
        m.put("extraFee2Mode", p.getExtraFee2Mode() != null ? p.getExtraFee2Mode() : "");
        m.put("extraFee2Value", extraFeeValuePlain(p.getExtraFee2Mode(), p.getExtraFee2Value()));
        m.put("extraFee3Name", p.getExtraFee3Name() != null ? p.getExtraFee3Name() : "");
        m.put("extraFee3Mode", p.getExtraFee3Mode() != null ? p.getExtraFee3Mode() : "");
        m.put("extraFee3Value", extraFeeValuePlain(p.getExtraFee3Mode(), p.getExtraFee3Value()));
        m.put("extraFee4Name", p.getExtraFee4Name() != null ? p.getExtraFee4Name() : "");
        m.put("extraFee4Mode", p.getExtraFee4Mode() != null ? p.getExtraFee4Mode() : "");
        m.put("extraFee4Value", extraFeeValuePlain(p.getExtraFee4Mode(), p.getExtraFee4Value()));
        m.put("chargebackPolicyId", p.getChargebackPolicyId() != null ? String.valueOf(p.getChargebackPolicyId()) : "");
        m.put("fee3dsRate", p.getFee3dsRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFee3dsRate()) : "");
        m.put("chargebackFeePerTx", p.getChargebackFeePerTx() != null ? p.getChargebackFeePerTx().toPlainString() : "");
        m.put("voidSettlementMode", commissionSettlementModeForDetail(p.getVoidSettlementMode()));
        m.put("manualVoidSettlementMode", commissionSettlementModeForDetail(p.getManualVoidSettlementMode()));
        m.put("refundSettlementMode", commissionSettlementModeForDetail(p.getRefundSettlementMode()));
        m.put("forceRefundSettlementMode", commissionSettlementModeForDetail(p.getForceRefundSettlementMode()));
    }

    private static String commissionSettlementModeForDetail(String v) {
        if (v == null || v.isBlank()) {
            return "FOLLOW";
        }
        return VoidRefundSettlementModeUtil.normalize(v.trim());
    }

    private static String extraFeeValuePlain(String mode, BigDecimal v) {
        if (v == null) {
            return "";
        }
        if ("PCT".equalsIgnoreCase(mode)) {
            return PercentDecimalHelper.toPlainOneDecimal(v);
        }
        return v.toPlainString();
    }

    /** 목록·엑셀 노출용: 저장값 CORP|번호 / PERSONAL|번호 → 번호만 (구분 미노출) */
    private static String regNoForDisplay(String regNo) {
        if (regNo == null || regNo.isBlank()) return "-";
        int bar = regNo.indexOf('|');
        if (bar >= 0 && bar < regNo.length() - 1) {
            String num = regNo.substring(bar + 1).trim();
            return num.isEmpty() ? "-" : num;
        }
        return regNo.trim();
    }

    /**
     * 업체관리 그리드 「루트」: 결제대행사 설정(MID 연동)의 루트번호(root_no)만 표시. 비어 있지 않은 값만, 등록 순·중복 제거.
     */
    private static String siteRootFromPgBindings(List<MerchantPgBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return "-";
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MerchantPgBinding b : bindings) {
            if (RouteNoDisplayUtil.isAbsent(b.getRootNo())) {
                continue;
            }
            seen.add(b.getRootNo().trim());
        }
        if (seen.isEmpty()) {
            return "-";
        }
        return String.join(", ", seen);
    }

    /**
     * 업체관리 그리드 「카드」: 결제대행사 설정의 카드브랜드 코드만(괄호 설명 제외). 예: VM.
     * 운영(Y) 행이 있으면 그 값만, 없으면 등록된 전 행. 가맹이 아니거나 바인딩이 없으면 {@code -}.
     */
    private static String cardBrandScopeFromPgBindings(List<MerchantPgBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return "-";
        }
        List<String> operational = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (MerchantPgBinding b : bindings) {
            if (b == null) {
                continue;
            }
            all.add(b.getCardBrandScope());
            String op = b.getOperationalYn();
            if (op != null && "Y".equalsIgnoreCase(op.trim())) {
                operational.add(b.getCardBrandScope());
            }
        }
        return CardBrandScopeUtil.displayCodesJoined(operational.isEmpty() ? all : operational);
    }

    /**
     * 업체관리 목록 「통화」열: 총판·지사·대리점·영업점·가맹점만 기준통화 표시. 총본사·본사는 비움.
     */
    private static boolean compListRowShowsBaseCurrency(OrgLevel lvl) {
        if (lvl == null) {
            return false;
        }
        return switch (lvl) {
            case MASTER_DIST, BRANCH, AGENCY, SALES_OFFICE, MERCHANT -> true;
            case HEADQUARTERS, REGIONAL -> false;
        };
    }

    /**
     * 총판 행: 해당 조직 프로필의 기준통화. 지사·대리점·영업점·가맹: 상위 체인의 가장 가까운 총판과 동일 값.
     * {@code masterDistBaseCurrencyCache} 키는 통화를 읽어온 조직 ID(총판 org id).
     */
    private String resolveCompListBaseCurrencyDisplay(OrgUnit o, Map<Long, String> masterDistBaseCurrencyCache) {
        OrgLevel lvl = o.getOrgLevel();
        if (!compListRowShowsBaseCurrency(lvl)) {
            return "";
        }
        Long sourceOrgId;
        if (lvl == OrgLevel.MASTER_DIST) {
            sourceOrgId = o.getId();
        } else {
            Optional<Long> mid = findNearestMasterDistAncestorId(o.getId());
            if (mid.isEmpty()) {
                return "-";
            }
            sourceOrgId = mid.get();
        }
        return masterDistBaseCurrencyCache.computeIfAbsent(sourceOrgId, id ->
                merchantProfileRepository.findByOrgUnitId(id)
                        .map(MerchantProfile::getBaseCurrency)
                        .filter(bc -> bc != null && !bc.isBlank())
                        .map(String::trim)
                        .orElse("-"));
    }

    /** 업체관리 목록용 행 구성 (정산금, 미수금, 대표자명, 연락처, 은행, 계좌번호, 이체수수료, 정산주기, 이체구분 등) */
    private Map<String, Object> buildCompListItem(OrgUnit o, Map<Long, String> masterDistBaseCurrencyCache) {
        Map<String, Object> m = CompListItemDto.from(o);
        m.put("regNo", "-");
        m.put("ceoNm", "-");
        m.put("contact", "-");
        m.put("bankNm", "-");
        m.put("accountNo", "-");
        m.put("transferFee", "-");
        m.put("calcCycle", "-");
        m.put("calcProcType", "-");
        m.put("transferType", "사용안함");
        m.put("transferCycleHours", "-");
        m.put("calcExcludeYn", "-");
        m.put("calcExcludeTarget", "-");
        m.put("calcStartTime", "-");
        m.put("payHoldYn", "-");
        m.put("useYn", "-");
        m.put("terminalCountTerminal", "-");
        m.put("terminalCountWeb", "-");
        m.put("settlementAmt", "-");
        m.put("receivables", "-");
        List<MerchantPgBinding> pgBinds = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(o.getId());
        m.put("siteRoot", siteRootFromPgBindings(pgBinds));
        m.put("cardBrandScope", o.getOrgLevel() == OrgLevel.MERCHANT
                ? cardBrandScopeFromPgBindings(pgBinds)
                : "-");
        m.put("payIntegrationMode", resolvePayIntegrationModeDisplay(o));
        m.put("apiIntegrationChannel", resolveApiIntegrationChannelDisplay(o));
        m.put("urlPayInputModeLabel", resolveUrlPayInputModeDisplay(o));
        findNearestMasterDistAncestorId(o.getId()).ifPresentOrElse(
                mid -> m.put("masterDistScopeOrgId", mid),
                () -> m.put("masterDistScopeOrgId", null));
        merchantProfileRepository.findByOrgUnitId(o.getId()).ifPresent(mp -> {
            m.put("regNo", regNoForDisplay(mp.getRegNo()));
            m.put("ceoNm", mp.getCeoNm() != null ? mp.getCeoNm() : "-");
            String contact = mp.getCeoMobile() != null && !mp.getCeoMobile().isEmpty() ? mp.getCeoMobile() : (mp.getCompTel() != null ? mp.getCompTel() : "-");
            m.put("contact", contact);
            m.put("bankNm", bankCdToName(mp.getBankCd()));
            m.put("accountNo", mp.getAccountNo() != null ? mp.getAccountNo() : "-");
            m.put("transferFee", mp.getTransferFee() != null ? mp.getTransferFee() : "-");
            m.put("useYn", mp.getUseYn() != null ? mp.getUseYn() : "-");
            m.put("terminalCountTerminal", mp.getTerminalCountTerminal() != null ? String.valueOf(mp.getTerminalCountTerminal()) : "-");
            m.put("terminalCountWeb", mp.getTerminalCountWeb() != null ? String.valueOf(mp.getTerminalCountWeb()) : "-");
        });
        m.put("baseCurrency", resolveCompListBaseCurrencyDisplay(o, masterDistBaseCurrencyCache));
        settlementSettingRepository.findByOrgUnitId(o.getId()).ifPresent(ss -> {
            if (o.getOrgLevel() == OrgLevel.MERCHANT) {
                m.put("calcCycle", calcCycleToDisplay(ss.getCalcCycle()));
            } else {
                m.put("calcCycle", "-");
            }
            m.put("calcProcType", calcProcTypeToDisplay(ss.getCalcProcType()));
            m.put("transferType", transferRemitTypeToDisplay(ss.getTransferType()));
            m.put("transferCycleHours", ss.getTransferCycleDays() != null ? String.valueOf(ss.getTransferCycleDays()) : "-");
            m.put("calcExcludeYn", ss.getCalcExcludeYn() != null ? ss.getCalcExcludeYn() : "-");
            m.put("calcExcludeTarget", calcExcludeTargetToDisplay(ss.getCalcExcludeTarget()));
            if (o.getOrgLevel() == OrgLevel.MERCHANT) {
                String norm = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
                String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "";
                if ("MANUAL".equalsIgnoreCase(proc) || !SettlementCycleTiming.isCalcStartTimeApplicableForAuto(norm)) {
                    m.put("calcStartTime", "-");
                } else {
                    m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : "-");
                }
            }
            m.put("payHoldYn", payHoldYnToDisplay(ss.getPayHoldYn()));
        });
        return m;
    }

    /**
     * 가맹 결제 연동 방식 — 웹결제(Y) + 활성 브로커 시크릿이면 {@code API}, 그 외 가맹은 {@code URL}.
     * (비가맹 조직은 {@code -})
     */
    private String resolvePayIntegrationModeDisplay(OrgUnit o) {
        if (o == null || o.getOrgLevel() != OrgLevel.MERCHANT) {
            return "-";
        }
        return merchantApiDeploymentService.isMerchantApiIntegrationEligible(o.getId()) ? "API" : "URL";
    }

    /**
     * 가맹 API 연동 채널 — 본사 결제로직설정 × 가맹 프로필 교집합.
     * IN(INLINE)·RE(REDIRECT)·WO(WordPress), 복수 사용 시 {@code IN/RE} 형식. (비가맹은 {@code -})
     */
    private String resolveApiIntegrationChannelDisplay(OrgUnit o) {
        if (o == null || o.getOrgLevel() != OrgLevel.MERCHANT) {
            return "-";
        }
        return merchantApiIntegrationChannelService.buildEffectiveChannelDisplayCode(o.getId());
    }

    /** 가맹 웹결제 입력방식(온라인 URL 결제용) — 업체관리 목록 「타입」 컬럼. (비가맹은 {@code -}) */
    private String resolveUrlPayInputModeDisplay(OrgUnit o) {
        if (o == null || o.getOrgLevel() != OrgLevel.MERCHANT) {
            return "-";
        }
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> com.pg.urlpay.UrlPayInputModeUtil.formatCompListLabel(mp.getUrlPayInputMode()))
                .orElse("-");
    }

    private static String bankCdToName(String cd) {
        if (cd == null || cd.isEmpty()) return "-";
        return switch (cd) {
            case "04" -> "국민";
            case "20" -> "우리";
            case "81" -> "KEB하나";
            case "88" -> "신한";
            case "11" -> "NH농협";
            case "02" -> "산업은행";
            default -> cd;
        };
    }

    /**
     * 업체관리 목록: 미지정 시 기본 "사용(Y)"만 표시. "ALL"/"*"이면 필터 없음.
     */
    private String normalizeUseYnFilter(String useYn) {
        if (useYn == null || useYn.trim().isEmpty()) return "Y";
        String t = useYn.trim();
        if ("ALL".equalsIgnoreCase(t) || "*".equals(t)) return null;
        return t;
    }

    private boolean matchUseYn(OrgUnit o, String useYn) {
        String f = normalizeUseYnFilter(useYn);
        if (f == null) return true;
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> OrgUseYnUtil.normalize(mp.getUseYn()).equalsIgnoreCase(OrgUseYnUtil.normalize(f)))
                .orElseGet(() -> OrgUseYnUtil.Y.equalsIgnoreCase(OrgUseYnUtil.normalize(f)));
    }

    /**
     * 업체 사용여부 변경 — N/S 시 하위 연쇄, Y 복원 시 상위연쇄로 함께 미사용됐던 하위만 자동 복원.
     * <ul>
     *   <li>N/S — 현재 <b>사용(Y)</b> 인 하위만 함께 미사용/정지 처리하고 「상위연쇄」로 표시.
     *       이전에 이미 개별 미사용이던 하위는 그대로 둔다.</li>
     *   <li>Y — 「상위연쇄」로 표시된 하위만 다시 사용으로 복원(개별 미사용이던 하위는 유지).</li>
     * </ul>
     * S 는 연동 AppUser 영구정지·세션 무효화.
     */
    private void applyOrgUseYnChange(OrgUnit ou, MerchantProfile mp, String useYnRaw) {
        String prev = OrgUseYnUtil.normalize(mp.getUseYn());
        String normalized = OrgUseYnUtil.normalize(useYnRaw);
        mp.setUseYn(normalized);
        // 직접(관리자) 편집 대상 조직 자신은 상위연쇄가 아니므로 플래그를 항상 해제한다.
        mp.setParentCascadeDisabledYn("N");
        if (OrgUseYnUtil.S.equals(normalized)) {
            orgUserSuspensionService.suspendAllLinkedUsers(ou.getId());
            cascadeDisableToDescendants(ou.getId(), OrgUseYnUtil.S, true);
        } else if (OrgUseYnUtil.N.equals(normalized)) {
            cascadeDisableToDescendants(ou.getId(), OrgUseYnUtil.N, false);
        } else if (!OrgUseYnUtil.Y.equals(prev)) {
            // 미사용/정지 → 사용 으로 복원: 상위연쇄로 함께 꺼졌던 하위만 되살림
            restoreCascadeDisabledDescendants(ou.getId());
        }
        if (OrgUseYnUtil.S.equals(prev) && !OrgUseYnUtil.S.equals(normalized)) {
            orgUserSuspensionService.restoreAllLinkedUsers(ou.getId());
        }
    }

    /** 상위 미사용/정지 연쇄: 현재 사용(Y) 중인 하위만 함께 끄고 「상위연쇄」로 표시. */
    private void cascadeDisableToDescendants(Long rootOrgUnitId, String targetYn, boolean suspendUsers) {
        String target = OrgUseYnUtil.normalize(targetYn);
        for (Long did : collectDescendantIds(rootOrgUnitId)) {
            merchantProfileRepository.findByOrgUnitId(did).ifPresent(dmp -> {
                String prevUy = OrgUseYnUtil.normalize(dmp.getUseYn());
                // 이미 미사용/정지이던 하위(개별 설정)는 그대로 두어 복원 대상에서 제외한다.
                if (!OrgUseYnUtil.Y.equals(prevUy)) {
                    return;
                }
                dmp.setUseYn(target);
                dmp.setParentCascadeDisabledYn("Y");
                merchantProfileRepository.save(dmp);
                orgUnitRepository.findById(did).ifPresent(dchild ->
                        orgUnitChangeAuditService.appendIfChanged(
                                dchild.getId(),
                                nz(dchild.getCode()),
                                nz(dchild.getName()),
                                "[업체정보] 업체사용여부(상위연쇄)",
                                ynDisplay(prevUy),
                                ynDisplay(target)));
                if (suspendUsers) {
                    orgUserSuspensionService.suspendAllLinkedUsers(did);
                }
            });
        }
    }

    /**
     * 상위 사용(Y) 복원: 「상위연쇄」로 함께 미사용/정지됐던 하위만 다시 사용으로 되살린다.
     * 개별 미사용이던 하위는 유지. 직상위가 아직 미사용/정지면 정합성 위해 복원하지 않는다.
     * ({@link #collectDescendantIds}는 부모→자식 전위 순서라 부모가 먼저 복원된 뒤 자식이 판정된다.)
     */
    private void restoreCascadeDisabledDescendants(Long rootOrgUnitId) {
        for (Long did : collectDescendantIds(rootOrgUnitId)) {
            merchantProfileRepository.findByOrgUnitId(did).ifPresent(dmp -> {
                if (!"Y".equalsIgnoreCase(nz(dmp.getParentCascadeDisabledYn()))) {
                    return;
                }
                Optional<OrgUnit> ouOpt = orgUnitRepository.findById(did);
                if (ouOpt.isEmpty()) {
                    return;
                }
                Long parentId = ouOpt.get().getParentId();
                boolean parentActive = parentId == null
                        || parentId.equals(rootOrgUnitId)
                        || merchantProfileRepository.findByOrgUnitId(parentId)
                                .map(pmp -> OrgUseYnUtil.Y.equals(OrgUseYnUtil.normalize(pmp.getUseYn())))
                                .orElse(true);
                if (!parentActive) {
                    return;
                }
                String prevUy = OrgUseYnUtil.normalize(dmp.getUseYn());
                boolean wasSuspended = OrgUseYnUtil.S.equals(prevUy);
                dmp.setUseYn(OrgUseYnUtil.Y);
                dmp.setParentCascadeDisabledYn("N");
                merchantProfileRepository.save(dmp);
                orgUnitRepository.findById(did).ifPresent(dchild ->
                        orgUnitChangeAuditService.appendIfChanged(
                                dchild.getId(),
                                nz(dchild.getCode()),
                                nz(dchild.getName()),
                                "[업체정보] 업체사용여부(상위연쇄복원)",
                                ynDisplay(prevUy),
                                ynDisplay(OrgUseYnUtil.Y)));
                if (wasSuspended) {
                    orgUserSuspensionService.restoreAllLinkedUsers(did);
                }
            });
        }
    }

    private boolean matchPayHoldYn(OrgUnit o, String payHoldYn) {
        if (payHoldYn == null || payHoldYn.trim().isEmpty()) return true;
        return settlementSettingRepository.findByOrgUnitId(o.getId())
                .map(ss -> payHoldYn.trim().equals(ss.getPayHoldYn() != null ? ss.getPayHoldYn() : "")).orElse(true);
    }

    private boolean matchCeoNm(OrgUnit o, String ceoNm) {
        if (ceoNm == null || ceoNm.trim().isEmpty()) return true;
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> mp.getCeoNm() != null && mp.getCeoNm().contains(ceoNm.trim())).orElse(true);
    }

    private boolean matchCeoMobile(OrgUnit o, String ceoMobile) {
        if (ceoMobile == null || ceoMobile.trim().isEmpty()) return true;
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> (mp.getCeoMobile() != null && mp.getCeoMobile().contains(ceoMobile.trim()))
                        || (mp.getCompTel() != null && mp.getCompTel().contains(ceoMobile.trim()))).orElse(true);
    }

    private boolean matchRegNo(OrgUnit o, String regNo) {
        if (regNo == null || regNo.trim().isEmpty()) return true;
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> mp.getRegNo() != null && mp.getRegNo().contains(regNo.trim())).orElse(true);
    }

    /**
     * 상위 체인의 총판(MASTER_DIST)에 저장된 NOTIFY_1~4를 신규 가맹 조직에 복사합니다(노티 분기·동일 URL 정책).
     */
    private void copyNotifyUrlSlotsFromNearestMasterDistToMerchant(Long merchantOrgUnitId, Long startParentOrgUnitId) {
        if (merchantOrgUnitId == null || startParentOrgUnitId == null) {
            return;
        }
        java.util.Optional<Long> md = masterDistSettlementCycleConfigService.findNearestMasterDistOrgId(startParentOrgUnitId);
        if (md.isEmpty()) {
            return;
        }
        for (MerchantNotifyUrl u : merchantNotifyUrlRepository.findByOrgUnitIdOrderByUrlTypeAsc(md.get())) {
            String t = u.getUrlType();
            if (t == null) {
                continue;
            }
            if (!("NOTIFY_1".equals(t) || "NOTIFY_2".equals(t) || "NOTIFY_3".equals(t) || "NOTIFY_4".equals(t))) {
                continue;
            }
            if (u.getNotiUrl() == null || u.getNotiUrl().isBlank()) {
                continue;
            }
            MerchantNotifyUrl n = new MerchantNotifyUrl();
            n.setOrgUnitId(merchantOrgUnitId);
            n.setUrlType(t);
            n.setNotiUrl(u.getNotiUrl().trim());
            n.setUseYn("Y");
            merchantNotifyUrlRepository.save(n);
        }
        merchantNotifyUrlRepository.flush();
    }

    /** 총판 노티 URL 4개 저장 (NOTIFY_1~4). 필수 1·2는 본사 노티구성(tb_hq_notify_target) 연동 시 서버가 고정합니다. */
    private void saveDistributorNotifyUrls(Long orgUnitId, String url1, String url2, String url3, String url4) {
        String[] hqPair = hqNotifyTargetService.resolveMandatoryNotifyPairUrls(orgUnitId);
        String n1;
        String n2;
        if (hqPair[0] != null && !hqPair[0].isBlank() && hqPair[1] != null && !hqPair[1].isBlank()) {
            n1 = hqPair[0].trim();
            n2 = hqPair[1].trim();
        } else {
            n1 = url1 != null ? url1.trim() : "";
            n2 = url2 != null ? url2.trim() : "";
        }
        String n3 = url3 != null ? url3.trim() : "";
        String n4 = url4 != null ? url4.trim() : "";
        boolean hasAny = !n1.isEmpty() || !n2.isEmpty() || !n3.isEmpty() || !n4.isEmpty();
        if (hasAny && n1.isEmpty()) {
            throw new IllegalArgumentException("총판 노티 URL 1(기본)은 필수입니다.");
        }
        if (hasAny && n2.isEmpty()) {
            throw new IllegalArgumentException("총판 노티 URL 2(RESULT)는 필수입니다. CALLBACK(URL 1)과 함께 입력하세요.");
        }
        final int maxLen = 2048;
        for (String u : new String[] { n1, n2, n3, n4 }) {
            if (!u.isEmpty() && u.length() > maxLen) {
                throw new IllegalArgumentException("노티 URL은 " + maxLen + "자 이하여야 합니다. (현재 " + u.length() + "자)");
            }
        }
        merchantNotifyUrlRepository.deleteByOrgUnitIdAndUrlTypeIn(orgUnitId,
                java.util.List.of("NOTIFY_1", "NOTIFY_2", "NOTIFY_3", "NOTIFY_4"));
        merchantNotifyUrlRepository.flush();
        String[] urls = { n1, n2, n3, n4 };
        for (int i = 0; i < 4; i++) {
            if (urls[i] != null && !urls[i].trim().isEmpty()) {
                MerchantNotifyUrl n = new MerchantNotifyUrl();
                n.setOrgUnitId(orgUnitId);
                n.setUrlType("NOTIFY_" + (i + 1));
                n.setNotiUrl(urls[i].trim());
                n.setUseYn("Y");
                merchantNotifyUrlRepository.save(n);
            }
        }
        hqNotifyTargetService.replaceDistributorOrgLinks(orgUnitId, java.util.List.of(n1, n2, n3, n4));
    }

    /** 해당 조직의 모든 하위 조직 ID 수집 (대리점→영업점→가맹점 등 전체 하위 트리) */
    private List<Long> collectDescendantIds(Long rootId) {
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        List<Long> result = new ArrayList<>();
        collectDescendantIdsRec(rootId, byParent, result);
        return result;
    }

    private void collectDescendantIdsRec(Long id, Map<Long, List<OrgUnit>> byParent, List<Long> result) {
        for (OrgUnit child : byParent.getOrDefault(id, Collections.emptyList())) {
            result.add(child.getId());
            collectDescendantIdsRec(child.getId(), byParent, result);
        }
    }

    /**
     * viewer 업체코드 기준으로 target이 viewer 본인이거나 viewer의 직·간접 하위 조직인지(상세·수정 권한 판별).
     */
    public boolean isTargetUnderViewerOrg(String viewerCompCode, String targetCompCode) {
        if (viewerCompCode == null || targetCompCode == null) return false;
        String v = viewerCompCode.trim();
        String t = targetCompCode.trim();
        if (v.isEmpty() || t.isEmpty()) return false;
        if (v.equals(t)) return true;
        Optional<OrgUnit> cur = orgUnitRepository.findByCode(t);
        while (cur.isPresent()) {
            Long pid = cur.get().getParentId();
            if (pid == null) return false;
            Optional<OrgUnit> parent = orgUnitRepository.findById(pid);
            if (parent.isEmpty()) return false;
            String pc = parent.get().getCode();
            if (pc != null && v.equals(pc.trim())) return true;
            cur = parent;
        }
        return false;
    }

    /**
     * viewer 업체코드 기준 산하(직·간접) 가맹점 {@code org_unit.id} 목록. viewer가 가맹점이면 자기 조직만 포함.
     */
    @Transactional(readOnly = true)
    public List<Long> collectMerchantOrgUnitIdsInViewerSubtree(String viewerCompCode) {
        if (viewerCompCode == null || viewerCompCode.isBlank()) {
            return List.of();
        }
        Optional<OrgUnit> vu = orgUnitRepository.findByCode(viewerCompCode.trim());
        if (vu.isEmpty()) {
            return List.of();
        }
        List<Long> desc = collectDescendantIds(vu.get().getId());
        Set<Long> scope = new HashSet<>(desc);
        scope.add(vu.get().getId());
        return orgUnitRepository.findAll().stream()
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT && scope.contains(o.getId()))
                .map(OrgUnit::getId)
                .sorted()
                .toList();
    }

    /**
     * 업체관리(로그인 조직 하위만) 목록 검색 시 업체구분 필터: 본인·상위 단계는 무시하고 하위만 허용.
     * @return 허용 시 enum 이름, 무효/동급·상위면 null(=전체와 동일)
     */
    public String sanitizeSearchCompDivForSubtreeViewer(String viewerOrgLevelName, String requestedCompDiv) {
        if (requestedCompDiv == null || requestedCompDiv.isBlank()) return null;
        String t = requestedCompDiv.trim();
        OrgLevel viewer = parseOrgLevelName(viewerOrgLevelName);
        OrgLevel filter;
        try {
            filter = OrgLevel.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException e) {
            return t;
        }
        if (viewer == null) return filter.name();
        if (filter.getCode() <= viewer.getCode()) return null;
        return filter.name();
    }

    private static OrgLevel parseOrgLevelName(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return OrgLevel.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 목록·엑셀 등: 정산주기는 저장 코드(정규화) 그대로 표기 */
    private static String calcCycleToDisplay(String c) {
        if (c == null || c.isEmpty()) return "-";
        String norm = SettlementPeriodResolver.normalizeCalcCycle(c);
        return norm != null && !norm.isEmpty() ? norm : c.trim();
    }

    /** 정산구분(수동·자동·펌뱅킹) */
    private static String calcProcTypeToDisplay(String t) {
        if (t == null || t.isEmpty()) return "-";
        return switch (t.toUpperCase()) {
            case "MANUAL" -> "수동";
            case "AUTO" -> "자동";
            case "FUMBANKING" -> "펌뱅킹";
            default -> t;
        };
    }

    /** 이체및송금구분(수동·자동·사용안함) */
    private static String transferRemitTypeToDisplay(String t) {
        if (t == null || t.isEmpty()) return "사용안함";
        return switch (t.toUpperCase()) {
            case "MANUAL" -> "수동";
            case "AUTO" -> "자동";
            case "AUTO_NO_MANUAL" -> "자동(수동불가)";
            case "ARBITRARY" -> "임의출금";
            case "NONE" -> "사용안함";
            default -> t;
        };
    }

    private static String calcExcludeTargetToDisplay(String t) {
        if (t == null || t.isEmpty()) return "-";
        return switch (t.toUpperCase()) {
            case "NONE" -> "전체";
            case "WEB" -> "웹";
            case "OFFLINE" -> "오프라인";
            case "BOTH", "WEB_OFFLINE" -> "웹+오프라인";
            default -> t;
        };
    }

    private static String payHoldYnToDisplay(String y) {
        if (y == null || y.isEmpty()) return "-";
        return "Y".equalsIgnoreCase(y) ? "보류" : "지급";
    }

    /** 엑셀 업로드로 업체 일괄 등록. 1행=헤더, 2행~=데이터 */
    public Map<String, Object> importFromExcel(org.springframework.web.multipart.MultipartFile file) {
        List<String> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            List<Map<String, String>> rawRows = compExcelImportService.parseExcel(file);
            for (int i = 0; i < rawRows.size(); i++) {
                Map<String, String> row = compExcelImportService.toStandardRow(rawRows.get(i));
                String compNm = row.get("compNm");
                String compDiv = row.get("compDiv");
                if (compNm == null || compNm.isEmpty()) {
                    errors.add((i + 2) + "행: 업체명이 없습니다.");
                    continue;
                }
                if (compDiv == null || compDiv.isEmpty()) {
                    errors.add((i + 2) + "행: 업체구분이 없습니다.");
                    continue;
                }
                Long parentId = null;
                String parentComp = row.get("parentComp");
                if (parentComp != null && !parentComp.isEmpty()) {
                    parentId = orgUnitRepository.findByCode(parentComp.trim())
                            .map(OrgUnit::getId)
                            .orElse(null);
                    if (parentId == null) {
                        errors.add((i + 2) + "행: 상위코드 '" + parentComp + "'를 찾을 수 없습니다.");
                        continue;
                    }
                } else if (!"REGIONAL".equalsIgnoreCase(compDiv)) {
                    errors.add((i + 2) + "행: 본사 외 업체는 상위코드가 필요합니다.");
                    continue;
                }
                String pwd = row.get("pwd");
                if (pwd == null || pwd.isEmpty()) pwd = "test123!";
                String loginIdVal = row.get("loginId");
                if (loginIdVal == null || loginIdVal.isEmpty()) {
                    loginIdVal = "excel" + System.currentTimeMillis() + "_" + i;
                }
                try {
                    OrgUnit saved = registerWithExtra(
                            row.get("compId"),
                            compNm,
                            compDiv,
                            parentId,
                            row.get("compTel"), row.get("zipCode"), row.get("addr"), row.get("addrDetail"), null, null,
                            row.get("ceoNm"), row.get("ceoMobile"), row.getOrDefault("useYn", "Y"), loginIdVal, row.get("regNo"),
                            null, null, null, null, null, null, null, null, null, null, null,
                            row.get("email"), pwd,
                            row.get("bankCd"), row.get("transferFee"), null, row.get("accountNo"), row.get("accountHolder"),
                            null, null, null, null, null, null, null, null, null,
                            row.get("remark"),
                            null, null, null, null, null, null, null, null, null, null, row.get("calcCycle"), null,
                            row.get("transferType"), null, null, null, null, null, null, null, null, null,
                            null, null, null, null,
                            null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                            null, null,
                            null, null, null,
                            null, null,
                            null,
                            null, null, null, null, null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null, null, null, null, null);
                    if (loginIdVal != null && !loginIdVal.isEmpty() && userRepository.findByUsername(loginIdVal).isEmpty()) {
                        AppUser appUser = new AppUser();
                        appUser.setUsername(loginIdVal);
                        appUser.setPassword(passwordEncoder.encode(pwd));
                        appUser.setName(compNm);
                        appUser.setRole("USER");
                        appUser.setEnabled(true);
                        appUser.setOrgUnitCode(saved.getCode());
                        appUser.setPermissionGroupNm("업체사용자");
                        appUser.setOtpRegisteredYn("N");
                        appUser.setPasswordMustChangeYn("N");
                        userRepository.save(appUser);
                    }
                    created.add(saved.getCode() + " " + saved.getName());
                } catch (Exception e) {
                    errors.add((i + 2) + "행: " + (e.getMessage() != null ? e.getMessage() : "등록 실패"));
                }
            }
        } catch (Exception e) {
            errors.add("엑셀 파싱 오류: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("createdCount", created.size());
        result.put("created", created);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return result;
    }

    private static String pgBindingAuditLine(MerchantPgBinding b) {
        if (b == null) {
            return "";
        }
        return "PG=" + nz(b.getPgCd()) + "/" + nz(b.getPayMethod())
                + " MID=" + nz(b.getMid()) + " 활성=" + nz(b.getActivationYn())
                + " 운영=" + nz(b.getOperationalYn()) + " Route=" + nz(b.getRootNo())
                + " URL금액=" + nz(b.getUrlPayPricingMode())
                + " 카드=" + nz(b.getCardBrandScope())
                + " 할부=" + nz(b.getInstallmentYn())
                + " extSettle=" + nz(b.getExtSettleMode());
    }

    private static final ObjectMapper PG_BINDINGS_OBJECT_MAPPER = new ObjectMapper();

    /**
     * 가맹점이 선택할 수 있는 PG인지 검증(API연동설정에 등록·사용 Y).
     * 본사 화면의 「운영」체크는 가맹점 선택 가능 여부와 무관합니다.
     */
    private void requireSelectablePgAgencyForMerchant(String pgCd) {
        String pc = pgCd != null ? pgCd.trim() : "";
        if (pc.isEmpty()) {
            throw new IllegalArgumentException("결제대행사(PG) 코드가 비었습니다.");
        }
        PgAgency agency = pgAgencyRepository.findByPgCd(pc)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 PG사코드입니다. 배포설정 > API연동설정에서 먼저 등록하세요."));
        if (!"Y".equalsIgnoreCase(agency.getUseYn())) {
            throw new IllegalArgumentException("사용 중지된 결제대행사입니다: " + pc);
        }
    }

    /**
     * 연동용도가 노티만(Y)이고 URL·챗봇·API 연동이 없는 PG — 가맹 바인딩 카드브랜드 범위는 ALL 고정.
     */
    private boolean isPgAgencyNotifyOnlyIntegration(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        return pgAgencyRepository.findByPgCd(pgCd.trim())
                .map(a -> "Y".equalsIgnoreCase(nullToN(a.getIntegNotiYn()))
                        && !"Y".equalsIgnoreCase(nullToN(a.getIntegUrlPayYn()))
                        && !"Y".equalsIgnoreCase(nullToN(a.getIntegWebChatbotYn()))
                        && !"Y".equalsIgnoreCase(nullToN(a.getIntegApiYn())))
                .orElse(false);
    }

    private static String nullToN(String s) {
        return s != null ? s.trim() : "";
    }

    /** 노티 전용 PG면 ALL, 그 외에는 요청값 검증·정규화. */
    private String resolveMerchantPgCardBrandScopeForSave(String pgCd, String requestedCardBrandScope) {
        if (isPgAgencyNotifyOnlyIntegration(pgCd)) {
            return "ALL";
        }
        CardBrandScopeUtil.validateOrThrow(requestedCardBrandScope);
        return CardBrandScopeUtil.normalize(requestedCardBrandScope);
    }

    private String resolveMerchantPgCurrencyScopeForSave(String pgCd, String requestedCurrencyScope) {
        if (isPgAgencyNotifyOnlyIntegration(pgCd)) {
            return CurrencyScopeUtil.ALL;
        }
        CurrencyScopeUtil.validateOrThrow(requestedCurrencyScope);
        return CurrencyScopeUtil.normalize(requestedCurrencyScope);
    }

    private void validateMerchantPgBindingJsonRows(List<Map<String, Object>> list) {
        if (list == null) {
            return;
        }
        for (Map<String, Object> m : list) {
            String pc = m.get("pgCd") != null ? m.get("pgCd").toString().trim() : "";
            if (pc.isEmpty()) {
                continue;
            }
            requireSelectablePgAgencyForMerchant(pc);
            if (!isPgAgencyNotifyOnlyIntegration(pc)) {
                CardBrandScopeUtil.validateOrThrow(optStr(m, "cardBrandScope"));
                CurrencyScopeUtil.validateOrThrow(optStr(m, "currencyScope"));
            }
        }
    }

    /** 조회 응답: API Key·IV 원문 미노출(앞3자+*****). */
    private static void putMerchantPgBindingSecretFields(Map<String, Object> bm, MerchantPgBinding b) {
        String rawKey = b.getApiKey();
        String rawIv = b.getIvKey();
        boolean hasKey = rawKey != null && !rawKey.isBlank();
        boolean hasIv = rawIv != null && !rawIv.isBlank();
        bm.put("hasApiKey", hasKey ? "Y" : "N");
        bm.put("apiKeyMasked", hasKey ? MerchantPgCredentialUtil.maskSecretPreview(rawKey) : "");
        bm.put("apiKey", hasKey ? MerchantPgCredentialUtil.maskSecretPreview(rawKey) : "");
        bm.put("hasIvKey", hasIv ? "Y" : "N");
        bm.put("ivKeyMasked", hasIv ? MerchantPgCredentialUtil.maskSecretPreview(rawIv) : "");
        bm.put("ivKey", hasIv ? MerchantPgCredentialUtil.maskSecretPreview(rawIv) : "");
    }

    /** delete-recreate 시 기존 시크릿 유지용. key = pgCd\\0payMethod → [apiKey, ivKey] */
    private Map<String, String[]> snapshotMerchantPgBindingSecrets(Long orgUnitId) {
        Map<String, String[]> out = new HashMap<>();
        if (orgUnitId == null) {
            return out;
        }
        for (MerchantPgBinding b : merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId)) {
            String pc = b.getPgCd() != null ? b.getPgCd().trim() : "";
            if (pc.isEmpty()) {
                continue;
            }
            String pm = b.getPayMethod() != null && !b.getPayMethod().isBlank() ? b.getPayMethod().trim() : "WEB";
            String key = pc.toUpperCase(Locale.ROOT) + "\0" + pm.toUpperCase(Locale.ROOT);
            out.put(key, new String[]{b.getApiKey(), b.getIvKey()});
        }
        return out;
    }

    private void applyMerchantPgBindingCredentialsFromJson(
            MerchantPgBinding binding,
            String pgCd,
            String payMethod,
            Map<String, Object> m,
            Map<String, String[]> prevSecrets) {
        String pm = payMethod != null && !payMethod.isBlank() ? payMethod.trim() : "WEB";
        String key = (pgCd != null ? pgCd.trim().toUpperCase(Locale.ROOT) : "")
                + "\0" + pm.toUpperCase(Locale.ROOT);
        String[] prev = prevSecrets != null ? prevSecrets.get(key) : null;
        String prevAk = prev != null && prev.length > 0 ? prev[0] : null;
        String prevIv = prev != null && prev.length > 1 ? prev[1] : null;
        PgAgency agency = pgAgencyRepository.findByPgCd(pgCd != null ? pgCd.trim() : "").orElse(null);
        MerchantPgCredentialUtil.PersistCreds creds = MerchantPgCredentialUtil.normalizeForPersist(
                optStr(m, "mid"), optStr(m, "apiKey"), optStr(m, "ivKey"), prevAk, prevIv, agency);
        binding.setMid(creds.mid());
        binding.setApiKey(creds.apiKey());
        binding.setIvKey(creds.ivKey());
    }

    private void applyExtSettlementFromJsonMap(MerchantPgBinding binding, Map<String, Object> m) {
        if (m != null && m.containsKey("extSettleMode")) {
            applyMerchantPgBindingExtSettlementFields(binding,
                    optStr(m, "extSettleMode"), optStr(m, "extSettleLag"), optStr(m, "extSettleBatchTime"));
        }
    }

    /**
     * DB 유니크 (org_unit_id, pg_cd, pay_method) 에 맞춤. JSON에 동일 조합이 중복되면 뒤쪽 행이 앞을 덮어쓴다.
     */
    private List<Map<String, Object>> dedupeMerchantPgBindingJsonRows(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        java.util.Map<String, Integer> keyToIndex = new java.util.HashMap<>();
        for (Map<String, Object> m : list) {
            String pc = m.get("pgCd") != null ? m.get("pgCd").toString().trim() : "";
            if (pc.isEmpty()) {
                continue;
            }
            String pmRaw = optStr(m, "payMethod");
            String pm = (pmRaw != null && !pmRaw.isEmpty()) ? pmRaw.trim() : "WEB";
            String key = pc.toUpperCase(Locale.ROOT) + "\0" + pm.toUpperCase(Locale.ROOT);
            Integer idx = keyToIndex.get(key);
            if (idx != null) {
                out.set(idx, m);
            } else {
                keyToIndex.put(key, out.size());
                out.add(m);
            }
        }
        return out;
    }

    /**
     * 운영(Y) WEB 바인딩이면서 연동용도 URL결제인 행이 없으면 {@link MerchantProfile#getWebPaymentUseYn()} 을 미사용(N)으로 맞춘다.
     * (노티 전용 PG만 운영이면 URL·웹결제 설정을 동시에 켤 수 없음.)
     * @return 프로필의 웹결제여부 값이 바뀌었으면 true (호출측에서 {@link MerchantProfileRepository#save} 권장)
     */
    private boolean syncMerchantWebPaymentUseYnIfNoUrlPayBinding(Long merchantOrgUnitId, MerchantProfile mp) {
        if (merchantOrgUnitId == null || mp == null) {
            return false;
        }
        if (chillPayService.findOperationalWebBindingForUrlPay(merchantOrgUnitId).isPresent()) {
            return false;
        }
        String cur = mp.getWebPaymentUseYn();
        if (cur == null || !"N".equalsIgnoreCase(cur.trim())) {
            mp.setWebPaymentUseYn("N");
            return true;
        }
        return false;
    }

    private void applyMerchantPgBindingExtSettlementFields(MerchantPgBinding binding, String modeRaw, String lagStr, String batchHm) {
        String m0 = modeRaw != null ? modeRaw.trim().toUpperCase(Locale.ROOT) : "";
        if (m0.isEmpty() || "INHERIT".equals(m0)) {
            binding.setExtSettleMode(null);
            binding.setExtSettleLag(null);
            binding.setExtSettleBatchTime(null);
            return;
        }
        if ("OFF".equals(m0)) {
            binding.setExtSettleMode("OFF");
            binding.setExtSettleLag(null);
            binding.setExtSettleBatchTime(null);
            return;
        }
        if (!"T".equals(m0) && !"D".equals(m0)) {
            throw new IllegalArgumentException("PG 정산예정 모드는 INHERIT, OFF, T, D 중 하나입니다.");
        }
        if (lagStr == null || lagStr.isBlank()) {
            throw new IllegalArgumentException("PG 정산예정: T/D 모드일 때 N(1~10)이 필요합니다.");
        }
        int lag;
        try {
            lag = Integer.parseInt(lagStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PG 정산예정 N은 1~10 정수입니다.");
        }
        if (lag < 1 || lag > 10) {
            throw new IllegalArgumentException("PG 정산예정 N은 1~10만 허용됩니다.");
        }
        if ("D".equals(m0)) {
            LocalTime bt = parseTime(batchHm != null ? batchHm : "");
            if (bt == null) {
                throw new IllegalArgumentException("D 모드는 일괄 정산 시각(HH:mm)이 필요합니다.");
            }
            binding.setExtSettleMode("D");
            binding.setExtSettleLag(lag);
            binding.setExtSettleBatchTime(bt);
        } else {
            binding.setExtSettleMode("T");
            binding.setExtSettleLag(lag);
            binding.setExtSettleBatchTime(null);
        }
    }

    /** 가맹점 결제대행사 1건 저장 (업체정보 상세에서 행 단위 저장) */
    public Map<String, Object> saveMerchantPgBinding(String compId, Long bindingId, String pgCd, String payMethod,
                                                     String mid, String rootNo, String apiKey, String ivKey,
                                                     String activationYn, String operationalYn,
                                                     String installmentYn, String maxInstallmentMonthsStr,
                                                     String urlPayPricingMode,
                                                     String cardBrandScope,
                                                     String currencyScope,
                                                     boolean extSettlementFieldsPresent,
                                                     String extSettleMode, String extSettleLagStr, String extSettleBatchHm) {
        OrgUnit ou = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 결제대행사를 등록할 수 있습니다.");
        }
        String pc = pgCd != null ? pgCd.trim() : "";
        if (pc.isEmpty()) {
            throw new IllegalArgumentException("결제대행사(PG)를 선택하세요.");
        }
        requireSelectablePgAgencyForMerchant(pc);
        String pm = payMethod != null && !payMethod.isBlank() ? payMethod.trim() : "WEB";

        MerchantPgBinding binding;
        String beforeLine = "";
        if (bindingId != null) {
            binding = merchantPgBindingRepository.findByIdAndOrgUnitId(bindingId, ou.getId())
                    .orElseThrow(() -> new IllegalArgumentException("연동 정보를 찾을 수 없습니다."));
            beforeLine = pgBindingAuditLine(binding);
            if (merchantPgBindingRepository.existsByOrgUnitIdAndPgCdAndPayMethodAndIdNot(ou.getId(), pc, pm, bindingId)) {
                throw new IllegalArgumentException("동일 PG·결제구분 조합이 이미 있습니다.");
            }
        } else {
            if (merchantPgBindingRepository.existsByOrgUnitIdAndPgCdAndPayMethod(ou.getId(), pc, pm)) {
                throw new IllegalArgumentException("동일 PG·결제구분 조합이 이미 있습니다.");
            }
            binding = new MerchantPgBinding();
            binding.setOrgUnitId(ou.getId());
            int maxOrder = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId()).stream()
                    .map(MerchantPgBinding::getSortOrder)
                    .filter(o -> o != null)
                    .max(Integer::compareTo)
                    .orElse(-1);
            binding.setSortOrder(maxOrder + 1);
        }
        binding.setPgCd(pc);
        binding.setPayMethod(pm);
        binding.setRootNo(rootNo != null && !rootNo.isBlank() ? rootNo.trim() : null);
        String prevAk = binding.getId() != null ? binding.getApiKey() : null;
        String prevIv = binding.getId() != null ? binding.getIvKey() : null;
        PgAgency agencyForCred = pgAgencyRepository.findByPgCd(pc).orElse(null);
        MerchantPgCredentialUtil.PersistCreds creds = MerchantPgCredentialUtil.normalizeForPersist(
                mid, apiKey, ivKey, prevAk, prevIv, agencyForCred);
        binding.setMid(creds.mid());
        binding.setApiKey(creds.apiKey());
        binding.setIvKey(creds.ivKey());
        binding.setActivationYn("Y".equalsIgnoreCase(activationYn) ? "Y" : "N");
        binding.setInstallmentYn("Y".equalsIgnoreCase(installmentYn) ? "Y" : "N");
        if (maxInstallmentMonthsStr != null && !maxInstallmentMonthsStr.isBlank()) {
            try {
                binding.setMaxInstallmentMonths(Integer.parseInt(maxInstallmentMonthsStr.trim()));
            } catch (NumberFormatException ignored) {
                binding.setMaxInstallmentMonths(null);
            }
        } else {
            binding.setMaxInstallmentMonths(null);
        }
        boolean opY = "Y".equalsIgnoreCase(operationalYn);
        binding.setOperationalYn(opY ? "Y" : "N");
        if (binding.getId() == null) {
            if (urlPayPricingMode != null && "DISPLAY_FX_THB".equalsIgnoreCase(urlPayPricingMode.trim())) {
                binding.setUrlPayPricingMode("DISPLAY_FX_THB");
            } else {
                binding.setUrlPayPricingMode("CHECKOUT_CURRENCY");
            }
        } else if (urlPayPricingMode != null && !urlPayPricingMode.isBlank()) {
            binding.setUrlPayPricingMode("DISPLAY_FX_THB".equalsIgnoreCase(urlPayPricingMode.trim())
                    ? "DISPLAY_FX_THB" : "CHECKOUT_CURRENCY");
        }
        binding.setCardBrandScope(resolveMerchantPgCardBrandScopeForSave(pc, cardBrandScope));
        binding.setCurrencyScope(resolveMerchantPgCurrencyScopeForSave(pc, currencyScope));
        if (extSettlementFieldsPresent) {
            applyMerchantPgBindingExtSettlementFields(binding, extSettleMode, extSettleLagStr, extSettleBatchHm);
        }
        merchantPgBindingRepository.save(binding);
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), nz(ou.getCode()), nz(ou.getName()),
                "[PG연동] 결제대행(MID)", beforeLine, pgBindingAuditLine(binding));

        Map<String, Object> bm = new HashMap<>();
        bm.put("id", binding.getId());
        bm.put("pgCd", binding.getPgCd());
        bm.put("activationYn", binding.getActivationYn());
        bm.put("operationalYn", binding.getOperationalYn());
        bm.put("payMethod", binding.getPayMethod());
        bm.put("mid", binding.getMid() != null ? binding.getMid() : "");
        bm.put("rootNo", binding.getRootNo() != null ? binding.getRootNo() : "");
        putMerchantPgBindingSecretFields(bm, binding);
        bm.put("installmentYn", binding.getInstallmentYn());
        bm.put("maxInstallmentMonths", binding.getMaxInstallmentMonths() != null ? String.valueOf(binding.getMaxInstallmentMonths()) : "");
        bm.put("urlPayPricingMode", binding.getUrlPayPricingMode() != null ? binding.getUrlPayPricingMode() : "CHECKOUT_CURRENCY");
        bm.put("cardBrandScope", binding.getCardBrandScope() != null ? binding.getCardBrandScope() : "ALL");
        bm.put("currencyScope", binding.getCurrencyScope() != null ? binding.getCurrencyScope() : "ALL");
        bm.put("extSettleMode", binding.getExtSettleMode() != null ? binding.getExtSettleMode() : "");
        bm.put("extSettleLag", binding.getExtSettleLag() != null ? String.valueOf(binding.getExtSettleLag()) : "");
        bm.put("extSettleBatchTime", binding.getExtSettleBatchTime() != null ? binding.getExtSettleBatchTime().toString() : "");
        merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
            if (syncMerchantWebPaymentUseYnIfNoUrlPayBinding(ou.getId(), mp)) {
                merchantProfileRepository.save(mp);
            }
        });
        return bm;
    }

    public void deleteMerchantPgBinding(String compId, Long bindingId) {
        if (bindingId == null) throw new IllegalArgumentException("삭제할 연동이 없습니다.");
        OrgUnit ou = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 해당 기능을 사용할 수 있습니다.");
        }
        MerchantPgBinding b = merchantPgBindingRepository.findByIdAndOrgUnitId(bindingId, ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("연동 정보를 찾을 수 없습니다."));
        String line = pgBindingAuditLine(b);
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), nz(ou.getCode()), nz(ou.getName()),
                "[PG연동] 결제대행 삭제", line, "(삭제)");
        merchantPgBindingRepository.delete(b);
        merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
            if (syncMerchantWebPaymentUseYnIfNoUrlPayBinding(ou.getId(), mp)) {
                merchantProfileRepository.save(mp);
            }
        });
    }

    /**
     * TEMP_REMOVE_AFTER_DEV — 임시 개발용: 선택 조직 + 전체 하위의 {@code tb_merchant_profile.use_yn} 만 {@code N} (물리 삭제 없음).
     * 총본사 제외. {@code /api/comp/dev-tree-remove}·플래그·화면 [삭제(개발)] 와 함께 제거.
     */
    @Transactional
    public Map<String, Object> softDeactivateOrgSubtreeForDev(String compId, AppUser user, boolean featureEnabled) {
        if (!featureEnabled) {
            throw new IllegalArgumentException("개발용 기능이 비활성화되어 있습니다. app.features.comp-dev-tree-remove=true 인 프로파일에서만 사용할 수 있습니다.");
        }
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("관리자(ADMIN)만 사용할 수 있는 개발용 기능입니다.");
        }
        if (compId == null || compId.isBlank()) {
            throw new IllegalArgumentException("그리드에서 한 건을 체크한 뒤 진행하세요.");
        }
        OrgUnit root = orgUnitRepository.findByCode(compId.trim())
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (root.getOrgLevel() == OrgLevel.HEADQUARTERS) {
            throw new IllegalArgumentException("총본사는 처리 대상에서 제외됩니다.");
        }
        List<Long> subtree = new ArrayList<>();
        subtree.add(root.getId());
        subtree.addAll(collectDescendantIds(root.getId()));
        int profileUpdated = 0;
        for (Long ouId : subtree) {
            Optional<MerchantProfile> omp = merchantProfileRepository.findByOrgUnitId(ouId);
            if (omp.isPresent()) {
                MerchantProfile mp = omp.get();
                mp.setUseYn("N");
                merchantProfileRepository.save(mp);
                profileUpdated++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "선택 조직 및 하위 " + subtree.size() + "개 조직 중 프로필 " + profileUpdated + "건을 미사용(N) 처리했습니다. (DB 물리 삭제 없음)");
        out.put("orgCount", subtree.size());
        out.put("profileUpdated", profileUpdated);
        out.put("rootCompId", root.getCode());
        return out;
    }

    private static String merchantPgBindingStableKey(String pgCd, String payMethod) {
        String pc = pgCd != null ? pgCd.trim() : "";
        String pm = payMethod != null && !payMethod.isBlank() ? payMethod.trim() : "WEB";
        return pc + "\0" + pm;
    }

    /** 전체 교체 저장 직전: JSON에 urlPayPricingMode가 없을 때 기존 행과 동일 키(pgCd+payMethod)의 모드를 이어 받기 위함 */
    private Map<String, String> snapshotMerchantPgBindingPricingModes(Long orgUnitId) {
        Map<String, String> map = new HashMap<>();
        for (MerchantPgBinding ob : merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId)) {
            String mode = ob.getUrlPayPricingMode();
            if (mode != null && !mode.isBlank()) {
                map.put(merchantPgBindingStableKey(ob.getPgCd(), ob.getPayMethod()), mode.trim());
            }
        }
        return map;
    }

    private void applyUrlPayPricingModeFromJsonOrPrevious(MerchantPgBinding binding, String pgCd, String payMethod,
                                                          String upmFromJson, Map<String, String> previousModes) {
        if (upmFromJson != null && !upmFromJson.isBlank()) {
            binding.setUrlPayPricingMode("DISPLAY_FX_THB".equalsIgnoreCase(upmFromJson.trim())
                    ? "DISPLAY_FX_THB" : "CHECKOUT_CURRENCY");
            return;
        }
        if (previousModes != null) {
            String prev = previousModes.get(merchantPgBindingStableKey(pgCd, payMethod));
            if (prev != null) {
                binding.setUrlPayPricingMode("DISPLAY_FX_THB".equalsIgnoreCase(prev) ? "DISPLAY_FX_THB" : "CHECKOUT_CURRENCY");
                return;
            }
        }
        binding.setUrlPayPricingMode("CHECKOUT_CURRENCY");
    }
}
