package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.CompListItemDto;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantProfile;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.entity.SettlementSetting;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.AppUser;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.entity.PgAgency;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompExcelImportService compExcelImportService;

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
                       UserRepository userRepository, PasswordEncoder passwordEncoder,
                       CompExcelImportService compExcelImportService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.compExcelImportService = compExcelImportService;
    }

    /** scopeCompId: 로그인 사용자의 업체코드(본인 org만 조회, 업체정보조회용) */
    public PageResult<Map<String, Object>> search(String compId, String compNm, int page, int size, String scopeCompId) {
        return search(compId, compNm, null, null, null, null, null, null, null, null, page, size, scopeCompId, false);
    }

    /**
     * 업체관리 검색 - 확장 파라미터.
     * @param scopeCompId 로그인 사용자 업체코드 등
     * @param scopeSubtreeBelowLoginOrg true면 본인 조직 행은 제외하고 직·간접 하위만 목록에 포함(업체관리 트리용)
     */
    public PageResult<Map<String, Object>> search(String compId, String compNm,
            String compDiv, String useYn, String payHoldYn, String ceoNm, String terminalId, String ceoMobile, String regNo, Boolean includeSub,
            int page, int size, String scopeCompId, boolean scopeSubtreeBelowLoginOrg) {
        String cId = (compId != null && !compId.trim().isEmpty()) ? compId.trim() : null;
        String cNm = (compNm != null && !compNm.trim().isEmpty()) ? compNm.trim() : null;
        String cDiv = (compDiv != null && !compDiv.trim().isEmpty()) ? compDiv.trim() : null;
        List<OrgUnit> all = orgUnitRepository.findAll();
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
        java.util.Map<Long, String> idToSortKey = buildHierarchySortKeys(all);
        java.util.Map<Long, Integer> idToDepth = buildHierarchyDepth(all);
        boolean relativeDepth = scopeSubtreeBelowLoginOrg && scopeCompId != null && !scopeCompId.trim().isEmpty() && !filtered.isEmpty();
        int minDepthFiltered = 0;
        if (relativeDepth) {
            minDepthFiltered = filtered.stream().mapToInt(o -> idToDepth.getOrDefault(o.getId(), 0)).min().orElse(0);
        }
        filtered.sort((a, b) -> {
            String ka = idToSortKey.getOrDefault(a.getId(), "z");
            String kb = idToSortKey.getOrDefault(b.getId(), "z");
            return ka.compareTo(kb);
        });
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = new ArrayList<>();
        if (start < filtered.size()) {
            List<OrgUnit> pageList = filtered.subList(start, end);
            for (int i = 0; i < pageList.size(); i++) {
                OrgUnit ou = pageList.get(i);
                Map<String, Object> row = buildCompListItem(ou);
                row.put("rowNo", start + i + 1);
                row.put("parentId", ou.getParentId());
                int d = idToDepth.getOrDefault(ou.getId(), 0);
                row.put("depth", relativeDepth ? (d - minDepthFiltered) : d);
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

    public PageResult<Map<String, Object>> changeHistory(String searchCompId, String searchFromDate, String searchToDate, int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
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
                            m.put("commissionConfigAllowed", mp.getCommissionConfigAllowed());
                            m.put("webPaymentUseYn", mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn() : "Y");
                            m.put("baseCurrency", mp.getBaseCurrency());
                            m.put("orgUnitId", ou.getId());
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
                                        bm.put("apiKey", b.getApiKey() != null ? b.getApiKey() : "");
                                        bm.put("ivKey", b.getIvKey() != null ? b.getIvKey() : "");
                                        bm.put("installmentYn", b.getInstallmentYn() != null ? b.getInstallmentYn() : "N");
                                        bm.put("maxInstallmentMonths", b.getMaxInstallmentMonths() != null ? String.valueOf(b.getMaxInstallmentMonths()) : "");
                                        return bm;
                                    })
                                    .collect(Collectors.toList());
                            m.put("pgBindings", pgBindings);
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
                                } else {
                                    m.put("calcCycle", null);
                                }
                                m.put("calcProcType", ss.getCalcProcType());
                                m.put("transferType", ss.getTransferType());
                                m.put("holdRate", ss.getHoldRate());
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
                                m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : null);
                            });
                            return m;
                        }));
    }

    /** 지역 본사(업체) 정보 수정 */
    public boolean update(String compId, String compNm, String compDiv, Long parentId, String compTel,
                          String zipCode, String addr, String addrDetail, String addrEtc, String addrCountryCd, String ceoNm, String ceoMobile,
                          String useYn, String loginId, String pwd, String regNo, String bizType, String industry,
                          String bizNature, String product, String homepage, String settleName, String settleTelNo,
                          String fax, String email, String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                          String remark, String commissionConfigAllowed, String webPaymentUseYn, String baseCurrency,
                          String siteUrl, String siteSummary, String pgBindings, String regionalSettings,
                          String assistantLoginId, String assistantPwd, String assistantRoleType, String brandingEditAllowedYn,
                          String notifyUrl1, String notifyUrl2, String notifyUrl3, String notifyUrl4) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            if (compNm != null) ou.setName(compNm);
                            if (compDiv != null) ou.setOrgLevel(orgLevelFromCompDiv(compDiv));
                            OrgLevel childLevel = ou.getOrgLevel() != null ? ou.getOrgLevel() : orgLevelFromCompDiv(compDiv);
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
                            orgUnitRepository.save(ou);
                            if (commissionConfigAllowed != null) mp.setCommissionConfigAllowed(commissionConfigAllowed);
                            if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
                            if (baseCurrency != null && !baseCurrency.trim().isEmpty()) {
                                String divForVal = compDiv != null ? compDiv : (ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "");
                                validateBaseCurrency(divForVal, baseCurrency);
                                if ("MASTER_DIST".equalsIgnoreCase(divForVal)) {
                                    Long pid = parentId != null ? parentId : ou.getParentId();
                                    validateMasterDistBaseCurrencyAgainstRegionalParent(pid, baseCurrency.trim());
                                }
                                mp.setBaseCurrency(baseCurrency.trim());
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
                                mp.setUseYn(useYn);
                                if ("N".equalsIgnoreCase(useYn.trim())) {
                                    for (Long did : collectDescendantIds(ou.getId())) {
                                        merchantProfileRepository.findByOrgUnitId(did).ifPresent(dmp -> {
                                            dmp.setUseYn("N");
                                            merchantProfileRepository.save(dmp);
                                        });
                                    }
                                }
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
                            merchantProfileRepository.save(mp);
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
                                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                    java.util.List<Map<String, Object>> list = om.readValue(pgBindings.trim(),
                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Map<String, Object>>>() {});
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
                                        binding.setPayMethod(optStr(m, "payMethod") != null && !optStr(m, "payMethod").isEmpty() ? optStr(m, "payMethod") : "WEB");
                                        binding.setMid(optStr(m, "mid"));
                                        binding.setRootNo(optStr(m, "rootNo"));
                                        binding.setApiKey(optStr(m, "apiKey"));
                                        binding.setIvKey(optStr(m, "ivKey"));
                                        binding.setInstallmentYn("Y".equalsIgnoreCase(optStr(m, "installmentYn")) ? "Y" : "N");
                                        String maxMo = optStr(m, "maxInstallmentMonths");
                                        if (maxMo != null && !maxMo.isEmpty()) {
                                            try { binding.setMaxInstallmentMonths(Integer.parseInt(maxMo.trim())); } catch (NumberFormatException ignored) {}
                                        }
                                        binding.setSortOrder(order++);
                                        merchantPgBindingRepository.save(binding);
                                    }
                                } catch (Exception ignored) {}
                            }
                            return true;
                        }))
                .orElse(false);
    }

    /**
     * 업체 대표 계정 비밀번호 초기화 — 임시 비밀번호 {@code 로그인ID + "1!"} (MerchantProfile.pwd, AppUser 동기화).
     * @return 임시 평문 비밀번호, 실패 시 empty
     */
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
                            userRepository.findByUsername(lid).ifPresent(u -> {
                                u.setPassword(encoded);
                                u.setPasswordMustChangeYn("Y");
                                userRepository.save(u);
                            });
                            return java.util.Optional.of(tempPlain);
                        }));
    }

    /** 업체 로그인ID 변경 - MerchantProfile.loginId, AppUser.username 동시 변경 */
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
                null, null, null, null, null, null, null, null, null, null, null, /* settleType, commissionRate, limitAmt */ email, pwd,
                bankCd, transferFee, null, accountNo, accountHolder,
                null, null, null, null, null, null, null, null, null,
                remark,
                /* withdrawRestrictType + withdraw / pay limit / hold / calc */
                null, null, null, null, null, null, null, null, null, null, null, null,
                /* 55–64: transfer … calcStart … calcProc … */
                null, null, null, null, null, null, null, null, null, null,
                /* pgBindings … */
                null, null, null,
                /* 65–68 default product */
                null, null, null, null,
                /* 69–88: notify, commission, fees, regional (20 nulls) */
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);  /* +hqPolicyScope */
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
                                     String pgBindings, String webPaymentUseYn, String baseCurrency,
                                     String defaultProductName, String defaultProductCode, String defaultProductAmount, String defaultProductDesc,
                                     String notifyUrlBackground, String notifyUrlResult,
                                     String notifyUrl1, String notifyUrl2, String notifyUrl3, String notifyUrl4,
                                     String commissionFollowHq, String hqPolicyScope, String perTxFee, String cancelRate, String usageRate,
                                     String failFee, String payRate, String refundRate, String rollingPct, String rollingDays,
                                     String feeSettlementPerTx, String feeUsdt, String feeFx,
                                     String regionalSettings) {
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
        mp.setUseYn(useYn);
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
        if (baseCurrency != null && !baseCurrency.trim().isEmpty()) {
            validateBaseCurrency(compDiv != null ? compDiv : "AGENCY", baseCurrency);
            if ("MASTER_DIST".equalsIgnoreCase(compDivVal)) {
                validateMasterDistBaseCurrencyAgainstRegionalParent(effectiveParentId, baseCurrency.trim());
            }
            mp.setBaseCurrency(baseCurrency.trim());
        }
        if (("REGIONAL".equalsIgnoreCase(compDiv) || "MASTER_DIST".equalsIgnoreCase(compDiv))
                && regionalSettings != null && !regionalSettings.trim().isEmpty()) {
            mp.setRegionalSettings(regionalSettings.trim());
        }
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(saved.getId());
        /** 정산주기(calcCycle)는 가맹점에만 부여. 총본사~영업점은 미사용(null). */
        if (childLevel == OrgLevel.MERCHANT) {
            ss.setCalcCycle(calcCycle != null && !calcCycle.isEmpty() ? calcCycle : "D7");
        } else {
            ss.setCalcCycle(null);
        }
        if (calcProcType != null && !calcProcType.isBlank()) {
            ss.setCalcProcType(calcProcType.trim());
            ss.setTransferType(transferType != null && !transferType.isBlank() ? transferType.trim() : "MANUAL");
        } else {
            applyLegacySettlementFields(ss, transferType);
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
            if (holdRate != null && !holdRate.isEmpty()) try { ss.setHoldRate(new BigDecimal(holdRate.trim())); } catch (Exception ignored) {}
            if (holdDays != null) ss.setHoldDays(holdDays);
        }
        if (parseTime(calcCloseTime) != null) ss.setCalcCloseTime(parseTime(calcCloseTime));
        if (transferCycleDays != null) ss.setTransferCycleDays(transferCycleDays);
        if (autoTransferMin != null && !autoTransferMin.isEmpty()) try { ss.setAutoTransferMin(new BigDecimal(autoTransferMin.trim())); } catch (Exception ignored) {}
        if (payHoldYn != null && !payHoldYn.isEmpty()) ss.setPayHoldYn(payHoldYn);
        if (calcExcludeYn != null && !calcExcludeYn.isEmpty()) ss.setCalcExcludeYn(calcExcludeYn);
        if (calcExcludeTarget != null && !calcExcludeTarget.isEmpty()) ss.setCalcExcludeTarget(calcExcludeTarget);
        if (parseTime(calcStartTime) != null) ss.setCalcStartTime(parseTime(calcStartTime));
        if (calcMinAmt != null && !calcMinAmt.isEmpty()) try { ss.setCalcMinAmt(new BigDecimal(calcMinAmt.trim())); } catch (Exception ignored) {}
        if (parseTime(transferExecTime) != null) ss.setTransferExecTime(parseTime(transferExecTime));
        settlementSettingRepository.save(ss);

        MerchantCommissionExtra extra = new MerchantCommissionExtra();
        extra.setOrgUnitId(saved.getId());
        merchantCommissionExtraRepository.save(extra);

        if ("MERCHANT".equalsIgnoreCase(compDiv) && pgBindings != null && !pgBindings.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<Map<String, Object>> list = om.readValue(pgBindings.trim(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Map<String, Object>>>() {});
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
                    binding.setMid(optStr(m, "mid"));
                    binding.setRootNo(optStr(m, "rootNo"));
                    binding.setApiKey(optStr(m, "apiKey"));
                    binding.setIvKey(optStr(m, "ivKey"));
                    binding.setInstallmentYn("Y".equalsIgnoreCase(optStr(m, "installmentYn")) ? "Y" : "N");
                    String maxMo = optStr(m, "maxInstallmentMonths");
                    if (maxMo != null && !maxMo.isEmpty()) {
                        try { binding.setMaxInstallmentMonths(Integer.parseInt(maxMo.trim())); } catch (NumberFormatException ignored) {}
                    }
                    binding.setSortOrder(++order);
                    merchantPgBindingRepository.save(binding);
                }
            } catch (Exception ignored) {}
        }

        boolean hasDefaultProduct = (defaultProductName != null && !defaultProductName.trim().isEmpty())
                || (defaultProductCode != null && !defaultProductCode.trim().isEmpty())
                || (defaultProductAmount != null && !defaultProductAmount.trim().isEmpty())
                || (defaultProductDesc != null && !defaultProductDesc.trim().isEmpty());
        if ("MERCHANT".equalsIgnoreCase(compDiv) && hasDefaultProduct) {
            MerchantDefaultProduct dp = new MerchantDefaultProduct();
            dp.setOrgUnitId(saved.getId());
            dp.setProductName(defaultProductName != null ? defaultProductName.trim() : null);
            dp.setProductCode(defaultProductCode != null ? defaultProductCode.trim() : null);
            if (defaultProductAmount != null && !defaultProductAmount.trim().isEmpty()) {
                try { dp.setDefaultAmount(new BigDecimal(defaultProductAmount.trim())); } catch (NumberFormatException ignored) {}
            }
            dp.setProductDesc(defaultProductDesc != null ? defaultProductDesc.trim() : null);
            merchantDefaultProductRepository.save(dp);
        }

        if ("MERCHANT".equalsIgnoreCase(compDiv)) {
            if (notifyUrlBackground != null && !notifyUrlBackground.trim().isEmpty()) {
                MerchantNotifyUrl n1 = new MerchantNotifyUrl();
                n1.setOrgUnitId(saved.getId());
                n1.setUrlType("BACKGROUND");
                n1.setNotiUrl(notifyUrlBackground.trim());
                n1.setUseYn("Y");
                merchantNotifyUrlRepository.save(n1);
            }
            if (notifyUrlResult != null && !notifyUrlResult.trim().isEmpty()) {
                MerchantNotifyUrl n2 = new MerchantNotifyUrl();
                n2.setOrgUnitId(saved.getId());
                n2.setUrlType("RESULT");
                n2.setNotiUrl(notifyUrlResult.trim());
                n2.setUseYn("Y");
                merchantNotifyUrlRepository.save(n2);
            }
        }
        if ("MASTER_DIST".equalsIgnoreCase(compDiv)) {
            saveDistributorNotifyUrls(saved.getId(), notifyUrl1, notifyUrl2, notifyUrl3, notifyUrl4);
        }

        if ("MERCHANT".equalsIgnoreCase(compDiv) && "N".equalsIgnoreCase(commissionFollowHq != null ? commissionFollowHq.trim() : "")) {
            CommissionPolicy policy = new CommissionPolicy();
            policy.setScope(saved.getCode());
            if (perTxFee != null && !perTxFee.trim().isEmpty()) try { policy.setPerTxFee(new BigDecimal(perTxFee.trim())); } catch (Exception ignored) {}
            if (cancelRate != null && !cancelRate.trim().isEmpty()) try { policy.setCancelRate(new BigDecimal(cancelRate.trim())); } catch (Exception ignored) {}
            if (usageRate != null && !usageRate.trim().isEmpty()) try { policy.setUsageRate(new BigDecimal(usageRate.trim())); } catch (Exception ignored) {}
            if (failFee != null && !failFee.trim().isEmpty()) try { policy.setFailFee(new BigDecimal(failFee.trim())); } catch (Exception ignored) {}
            if (payRate != null && !payRate.trim().isEmpty()) try { policy.setPayRate(new BigDecimal(payRate.trim())); } catch (Exception ignored) {}
            if (refundRate != null && !refundRate.trim().isEmpty()) try { policy.setRefundRate(new BigDecimal(refundRate.trim())); } catch (Exception ignored) {}
            if (rollingPct != null && !rollingPct.trim().isEmpty()) try { policy.setRollingPct(new BigDecimal(rollingPct.trim())); } catch (Exception ignored) {}
            if (rollingDays != null && !rollingDays.trim().isEmpty()) try { policy.setRollingDays(Integer.parseInt(rollingDays.trim())); } catch (Exception ignored) {}
            if (feeSettlementPerTx != null && !feeSettlementPerTx.trim().isEmpty()) try { policy.setFeeSettlementPerTx(new BigDecimal(feeSettlementPerTx.trim())); } catch (Exception ignored) {}
            if (feeUsdt != null && !feeUsdt.trim().isEmpty()) try { policy.setFeeUsdt(new BigDecimal(feeUsdt.trim())); } catch (Exception ignored) {}
            if (feeFx != null && !feeFx.trim().isEmpty()) try { policy.setFeeFx(new BigDecimal(feeFx.trim())); } catch (Exception ignored) {}
            commissionPolicyRepository.save(policy);
        } else if ("MERCHANT".equalsIgnoreCase(compDiv)) {
            String srcScope = (hqPolicyScope != null && !hqPolicyScope.trim().isEmpty()) ? hqPolicyScope.trim() : "DEFAULT";
            commissionPolicyRepository.findByScope(srcScope).ifPresent(src -> {
                CommissionPolicy policy = new CommissionPolicy();
                policy.setScope(saved.getCode());
                policy.setPerTxFee(src.getPerTxFee());
                policy.setUsageRate(src.getUsageRate());
                policy.setFailFee(src.getFailFee());
                policy.setCancelRate(src.getCancelRate());
                policy.setRefundRate(src.getRefundRate());
                policy.setPayRate(src.getPayRate());
                policy.setFeeSettlementPerTx(src.getFeeSettlementPerTx());
                policy.setFeeUsdt(src.getFeeUsdt());
                policy.setFeeFx(src.getFeeFx());
                policy.setRollingPct(src.getRollingPct());
                policy.setRollingDays(src.getRollingDays());
                commissionPolicyRepository.save(policy);
            });
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
        return switch (v) {
            case "MANAGER", "OPERATOR", "SETTLEMENT", "TECH" -> v;
            default -> "MANAGER";
        };
    }

    private static String permissionGroupByAssistantRole(String roleType) {
        return switch (normalizeAssistantRoleType(roleType)) {
            case "OPERATOR" -> "운영담당";
            case "SETTLEMENT" -> "정산담당";
            case "TECH" -> "기술담당";
            default -> "관리담당";
        };
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
                            m.put("holdRate", ss.getHoldRate());
                            m.put("holdDays", ss.getHoldDays());
                            m.put("calcCycle", ou.getOrgLevel() == OrgLevel.MERCHANT ? ss.getCalcCycle() : null);
                            m.put("calcProcType", ss.getCalcProcType());
                            m.put("transferType", ss.getTransferType());
                            m.put("calcCloseTime", ss.getCalcCloseTime() != null ? ss.getCalcCloseTime().toString() : null);
                            m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : null);
                            m.put("transferExecTime", ss.getTransferExecTime() != null ? ss.getTransferExecTime().toString() : null);
                            m.put("autoTransferMin", ss.getAutoTransferMin());
                            m.put("calcMinAmt", ss.getCalcMinAmt());
                            m.put("payHoldYn", ss.getPayHoldYn());
                            m.put("calcExcludeYn", ss.getCalcExcludeYn());
                            m.put("calcExcludeTarget", ss.getCalcExcludeTarget());
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
                                         String calcMinAmt, String transferExecTime) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId())
                        .map(ss -> {
                            if (withdrawRestrictType != null) {
                                String w = withdrawRestrictType.trim();
                                ss.setWithdrawRestrictType(w.isEmpty() ? null : w);
                            }
                            if (withdrawLimitDays != null) ss.setWithdrawLimitDays(withdrawLimitDays);
                            if (parseTime(withdrawStartTime) != null) ss.setWithdrawStartTime(parseTime(withdrawStartTime));
                            if (parseTime(withdrawEndTime) != null) ss.setWithdrawEndTime(parseTime(withdrawEndTime));
                            if (payLimitDefault != null && !payLimitDefault.isEmpty()) try { ss.setPayLimitDefault(new BigDecimal(payLimitDefault.trim())); } catch (Exception ignored) {}
                            if (payLimitExtra != null && !payLimitExtra.isEmpty()) try { ss.setPayLimitExtra(new BigDecimal(payLimitExtra.trim())); } catch (Exception ignored) {}
                            if (holdRate != null && !holdRate.isEmpty()) try { ss.setHoldRate(new BigDecimal(holdRate.trim())); } catch (Exception ignored) {}
                            if (holdDays != null) ss.setHoldDays(holdDays);
                            if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
                                if (calcCycle != null && !calcCycle.isEmpty()) ss.setCalcCycle(calcCycle);
                            } else {
                                ss.setCalcCycle(null);
                            }
                            if (parseTime(calcCloseTime) != null) ss.setCalcCloseTime(parseTime(calcCloseTime));
                            if (parseTime(calcStartTime) != null) ss.setCalcStartTime(parseTime(calcStartTime));
                            if (transferCycleDays != null) ss.setTransferCycleDays(transferCycleDays);
                            if (calcProcType != null && !calcProcType.isEmpty()) ss.setCalcProcType(calcProcType.trim());
                            if (transferType != null && !transferType.isEmpty()) ss.setTransferType(transferType.trim());
                            if (autoTransferMin != null && !autoTransferMin.isEmpty()) try { ss.setAutoTransferMin(new BigDecimal(autoTransferMin.trim())); } catch (Exception ignored) {}
                            if (calcMinAmt != null && !calcMinAmt.isEmpty()) try { ss.setCalcMinAmt(new BigDecimal(calcMinAmt.trim())); } catch (Exception ignored) {}
                            if (parseTime(transferExecTime) != null) ss.setTransferExecTime(parseTime(transferExecTime));
                            if (payHoldYn != null && !payHoldYn.isEmpty()) ss.setPayHoldYn(payHoldYn);
                            if (calcExcludeYn != null && !calcExcludeYn.isEmpty()) ss.setCalcExcludeYn(calcExcludeYn);
                            if (calcExcludeTarget != null && !calcExcludeTarget.isEmpty()) ss.setCalcExcludeTarget(calcExcludeTarget);
                            settlementSettingRepository.save(ss);
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

    /** 업체관리 목록용 행 구성 (정산금, 미수금, 대표자명, 연락처, 은행, 계좌번호, 이체수수료, 정산주기, 이체구분 등) */
    private Map<String, Object> buildCompListItem(OrgUnit o) {
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
            m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().toString() : "-");
            m.put("payHoldYn", payHoldYnToDisplay(ss.getPayHoldYn()));
        });
        return m;
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
                .map(mp -> {
                    String v = mp.getUseYn() != null ? mp.getUseYn().trim() : "Y";
                    return f.equalsIgnoreCase(v);
                })
                .orElseGet(() -> "Y".equalsIgnoreCase(f));
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

    /** 총판 노티 URL 4개 저장 (NOTIFY_1~4). 본사/총판 생성 시 총본사 설정에서 설정 */
    private void saveDistributorNotifyUrls(Long orgUnitId, String url1, String url2, String url3, String url4) {
        String n1 = url1 != null ? url1.trim() : "";
        String n2 = url2 != null ? url2.trim() : "";
        String n3 = url3 != null ? url3.trim() : "";
        String n4 = url4 != null ? url4.trim() : "";
        boolean hasAny = !n1.isEmpty() || !n2.isEmpty() || !n3.isEmpty() || !n4.isEmpty();
        if (hasAny && n1.isEmpty()) {
            throw new IllegalArgumentException("총판 노티 URL 1(기본)은 필수입니다.");
        }
        merchantNotifyUrlRepository.deleteByOrgUnitIdAndUrlTypeIn(orgUnitId,
                java.util.List.of("NOTIFY_1", "NOTIFY_2", "NOTIFY_3", "NOTIFY_4"));
        String[] urls = { n1, n2, n3, n4 };
        for (int i = 0; i < 4; i++) {
            if (urls[i] != null && !urls[i].trim().isEmpty()) {
                MerchantNotifyUrl n = new MerchantNotifyUrl();
                n.setOrgUnitId(orgUnitId);
                n.setUrlType("NOTIFY_" + (i + 1));
                n.setNotiUrl(urls[i].trim());
                n.setUseYn(i == 0 ? "Y" : "N");
                merchantNotifyUrlRepository.save(n);
            }
        }
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

    /** 계층 정렬용 키 생성 (레그 구조: 부모→자식 순) */
    private java.util.Map<Long, String> buildHierarchySortKeys(List<OrgUnit> all) {
        java.util.Map<Long, java.util.List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        java.util.Map<Long, String> result = new java.util.HashMap<>();
        int[] counter = { 0 };
        for (OrgUnit root : all.stream().filter(o -> o.getParentId() == null)
                .sorted((a, b) -> (a.getCode() != null ? a.getCode() : "").compareTo(b.getCode() != null ? b.getCode() : "")).toList()) {
            dfsAssignSortKey(root.getId(), byParent, result, String.valueOf(++counter[0]));
        }
        return result;
    }

    private void dfsAssignSortKey(Long id, java.util.Map<Long, java.util.List<OrgUnit>> byParent,
            java.util.Map<Long, String> result, String prefix) {
        result.put(id, prefix);
        java.util.List<OrgUnit> children = new java.util.ArrayList<>(byParent.getOrDefault(id, java.util.Collections.emptyList()));
        children.sort((a, b) -> (a.getCode() != null ? a.getCode() : "").compareTo(b.getCode() != null ? b.getCode() : ""));
        for (int i = 0; i < children.size(); i++) {
            dfsAssignSortKey(children.get(i).getId(), byParent, result, prefix + "." + (i + 1));
        }
    }

    /** 계층 깊이 (0=루트, 1=1단계 하위, ...) */
    private java.util.Map<Long, Integer> buildHierarchyDepth(List<OrgUnit> all) {
        java.util.Map<Long, Integer> depth = new java.util.HashMap<>();
        for (OrgUnit o : all) {
            if (o.getParentId() == null) {
                depth.put(o.getId(), 0);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (OrgUnit o : all) {
                if (o.getParentId() != null && depth.containsKey(o.getParentId()) && !depth.containsKey(o.getId())) {
                    depth.put(o.getId(), depth.get(o.getParentId()) + 1);
                    changed = true;
                }
            }
        }
        for (OrgUnit o : all) {
            depth.putIfAbsent(o.getId(), 0);
        }
        return depth;
    }

    private static String calcCycleToDisplay(String c) {
        if (c == null || c.isEmpty()) return "-";
        String u = c.trim().toUpperCase();
        return switch (u) {
            case "NONE" -> "정산안함";
            case "RT", "REALTIME" -> "실시간";
            case "M5" -> "5분";
            case "M10" -> "10분";
            case "H1" -> "1시간";
            case "H2" -> "2시간";
            case "H4" -> "4시간";
            case "W3" -> "W+3";
            case "W5" -> "W+5";
            case "W7" -> "W+7";
            case "W10" -> "W+10";
            case "W14" -> "W+14";
            case "WK1W" -> "WK+1W";
            case "WK2W" -> "WK+2W";
            case "WK1WT" -> "WK+1WT";
            case "WK2WT" -> "WK+2WT";
            case "WEEKLY" -> "Weekly(구)";
            case "WEEKLY2" -> "Weekly2(구)";
            default -> {
                if (u.matches("D\\d+")) yield "D+" + u.substring(1);
                yield c;
            }
        };
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
                            row.get("compTel"),
                            row.get("zipCode"), row.get("addr"), row.get("addrDetail"), null, null,
                            row.get("ceoNm"),
                            row.get("ceoMobile"),
                            row.getOrDefault("useYn", "Y"),
                            loginIdVal,
                            row.get("regNo"),
                            null, null, null, null, null, null, null, null, null, null,
                            null,
                            row.get("email"),
                            pwd,
                            row.get("bankCd"),
                            row.get("transferFee"),
                            null,
                            row.get("accountNo"),
                            row.get("accountHolder"),
                            null, null, null, null, null, null, null, null, null,
                            row.get("remark"),
                            null, null, null, null, null, null, null, null, null, null, row.get("calcCycle"), null,
                            row.get("transferType"), null, null, null, null, null, null, null, null, null,
                            null, null, null,
                            null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null);
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

    /** 가맹점 결제대행사 1건 저장 (업체정보 상세에서 행 단위 저장) */
    public Map<String, Object> saveMerchantPgBinding(String compId, Long bindingId, String pgCd, String payMethod,
                                                     String mid, String rootNo, String apiKey, String ivKey,
                                                     String activationYn, String operationalYn,
                                                     String installmentYn, String maxInstallmentMonthsStr) {
        OrgUnit ou = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 결제대행사를 등록할 수 있습니다.");
        }
        String pc = pgCd != null ? pgCd.trim() : "";
        if (pc.isEmpty()) {
            throw new IllegalArgumentException("결제대행사(PG)를 선택하세요.");
        }
        PgAgency agency = pgAgencyRepository.findByPgCd(pc)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 PG사코드입니다. 본사설정 > PG사 API 연동에서 먼저 등록하세요."));
        if (!"Y".equalsIgnoreCase(agency.getUseYn())) {
            throw new IllegalArgumentException("사용 중지된 결제대행사입니다.");
        }
        String pm = payMethod != null && !payMethod.isBlank() ? payMethod.trim() : "WEB";

        MerchantPgBinding binding;
        if (bindingId != null) {
            binding = merchantPgBindingRepository.findByIdAndOrgUnitId(bindingId, ou.getId())
                    .orElseThrow(() -> new IllegalArgumentException("연동 정보를 찾을 수 없습니다."));
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
        binding.setMid(mid != null ? mid.trim() : null);
        binding.setRootNo(rootNo != null && !rootNo.isBlank() ? rootNo.trim() : null);
        binding.setApiKey(apiKey != null ? apiKey.trim() : null);
        binding.setIvKey(ivKey != null ? ivKey.trim() : null);
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
        if (opY) {
            for (MerchantPgBinding other : merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId())) {
                if (binding.getId() != null && other.getId().equals(binding.getId())) continue;
                other.setOperationalYn("N");
                merchantPgBindingRepository.save(other);
            }
        }
        binding.setOperationalYn(opY ? "Y" : "N");
        merchantPgBindingRepository.save(binding);

        Map<String, Object> bm = new HashMap<>();
        bm.put("id", binding.getId());
        bm.put("pgCd", binding.getPgCd());
        bm.put("activationYn", binding.getActivationYn());
        bm.put("operationalYn", binding.getOperationalYn());
        bm.put("payMethod", binding.getPayMethod());
        bm.put("mid", binding.getMid() != null ? binding.getMid() : "");
        bm.put("rootNo", binding.getRootNo() != null ? binding.getRootNo() : "");
        bm.put("apiKey", binding.getApiKey() != null ? binding.getApiKey() : "");
        bm.put("ivKey", binding.getIvKey() != null ? binding.getIvKey() : "");
        bm.put("installmentYn", binding.getInstallmentYn());
        bm.put("maxInstallmentMonths", binding.getMaxInstallmentMonths() != null ? String.valueOf(binding.getMaxInstallmentMonths()) : "");
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
        merchantPgBindingRepository.delete(b);
    }
}
