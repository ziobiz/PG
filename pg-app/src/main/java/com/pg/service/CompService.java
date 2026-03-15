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
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.CommissionPolicyRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;

    private static LocalTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String t = s.trim();
            if (t.matches("\\d{1,2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm"));
            if (t.matches("\\d{1,2}:\\d{2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm:ss"));
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) { return null; }
    }

    /** 업체구분(compDiv) 문자열 → OrgLevel 매핑 (총본사/본사/총판/지사/대리점/가맹점) */
    private static OrgLevel orgLevelFromCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return OrgLevel.AGENCY;
        return switch (compDiv.toUpperCase()) {
            case "REGIONAL" -> OrgLevel.REGIONAL;
            case "MASTER_DIST" -> OrgLevel.MASTER_DIST;
            case "BRANCH" -> OrgLevel.BRANCH;
            case "AGENCY" -> OrgLevel.AGENCY;
            case "MERCHANT" -> OrgLevel.MERCHANT;
            default -> OrgLevel.AGENCY;
        };
    }

    public CompService(OrgUnitRepository orgUnitRepository, MerchantProfileRepository merchantProfileRepository,
                       SettlementSettingRepository settlementSettingRepository,
                       MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                       MerchantPgBindingRepository merchantPgBindingRepository,
                       MerchantDefaultProductRepository merchantDefaultProductRepository,
                       MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                       CommissionPolicyRepository commissionPolicyRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
    }

    /** scopeCompId: 로그인 사용자의 업체코드(본인 org만 조회, 업체정보조회용) */
    public PageResult<Map<String, Object>> search(String compId, String compNm, int page, int size, String scopeCompId) {
        return search(compId, compNm, null, null, null, null, null, null, null, null, null, page, size, scopeCompId);
    }

    /** scopeCompId 또는 compCode 기준 업체 + 모든 하위 업체 ID 수집 */
    private java.util.Set<Long> collectScopeAndDescendantIds(String compCode) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (compCode == null || compCode.trim().isEmpty()) return ids;
        orgUnitRepository.findByCode(compCode.trim()).ifPresent(root -> {
            ids.add(root.getId());
            collectDescendantIds(root.getId(), ids);
        });
        return ids;
    }

    private void collectDescendantIds(Long parentId, java.util.Set<Long> ids) {
        for (OrgUnit child : orgUnitRepository.findByParentIdOrderByCodeAsc(parentId)) {
            ids.add(child.getId());
            collectDescendantIds(child.getId(), ids);
        }
    }

    /** 업체관리 검색 - 확장 파라미터. searchParentCompId: 상위 조직 선택 시 해당 조직+하위 전체 조회 */
    public PageResult<Map<String, Object>> search(String compId, String compNm,
            String compDiv, String useYn, String payHoldYn, String ceoNm, String terminalId, String ceoMobile, String regNo, Boolean includeSub,
            String searchParentCompId, int page, int size, String scopeCompId) {
        String cId = (compId != null && !compId.trim().isEmpty()) ? compId.trim() : null;
        String cNm = (compNm != null && !compNm.trim().isEmpty()) ? compNm.trim() : null;
        String cDiv = (compDiv != null && !compDiv.trim().isEmpty()) ? compDiv.trim() : null;
        String searchParentCode = (searchParentCompId != null && searchParentCompId.contains(" ("))
                ? searchParentCompId.split(" \\(")[0].trim() : searchParentCompId;
        java.util.Set<Long> scopeIds = collectScopeAndDescendantIds(scopeCompId);
        java.util.Set<Long> parentScopeIds = collectScopeAndDescendantIds(searchParentCode);
        if (searchParentCode != null && !searchParentCode.trim().isEmpty()) {
            if (parentScopeIds.isEmpty()) scopeIds = new java.util.HashSet<>();
            else if (scopeIds.isEmpty()) scopeIds = parentScopeIds;
            else { scopeIds = new java.util.HashSet<>(scopeIds); scopeIds.retainAll(parentScopeIds); }
        }
        final java.util.Set<Long> finalScopeIds = scopeIds;
        List<OrgUnit> all = orgUnitRepository.findAll();
        List<OrgUnit> filtered = all.stream()
                .filter(o -> ((scopeCompId == null || scopeCompId.trim().isEmpty()) && (searchParentCode == null || searchParentCode.trim().isEmpty())) || finalScopeIds.contains(o.getId()))
                .filter(o -> (cId == null || (o.getCode() != null && o.getCode().contains(cId))))
                .filter(o -> (cNm == null || (o.getName() != null && o.getName().contains(cNm))))
                .filter(o -> (cDiv == null || (o.getOrgLevel() != null && o.getOrgLevel().name().equals(cDiv))))
                .filter(o -> matchUseYn(o, useYn))
                .filter(o -> matchPayHoldYn(o, payHoldYn))
                .filter(o -> matchCeoNm(o, ceoNm))
                .filter(o -> matchCeoMobile(o, ceoMobile))
                .filter(o -> matchRegNo(o, regNo))
                .collect(Collectors.toList());
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = new ArrayList<>();
        if (start < filtered.size()) {
            List<OrgUnit> pageList = filtered.subList(start, end);
            for (int i = 0; i < pageList.size(); i++) {
                Map<String, Object> row = buildCompListItem(pageList.get(i));
                row.put("rowNo", start + i + 1);
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

    /** compDiv에 맞는 상위 조직 레벨인지 검증 (총본사→본사→총판→지사→대리점→가맹점) */
    private boolean isValidParentLevel(String compDiv, OrgLevel parentLevel) {
        if (compDiv == null || parentLevel == null) return false;
        return switch (compDiv.toUpperCase()) {
            case "REGIONAL" -> parentLevel == OrgLevel.HEADQUARTERS;
            case "MASTER_DIST" -> parentLevel == OrgLevel.REGIONAL;
            case "BRANCH" -> parentLevel == OrgLevel.MASTER_DIST;
            case "AGENCY" -> parentLevel == OrgLevel.BRANCH;
            case "MERCHANT" -> parentLevel == OrgLevel.AGENCY;
            default -> false;
        };
    }

    /** parentId가 targetId 또는 그 하위 조직이면 순환 참조 (이동 불가) */
    private boolean wouldCreateCycle(Long targetOrgUnitId, Long newParentId) {
        if (targetOrgUnitId == null || newParentId == null) return false;
        if (targetOrgUnitId.equals(newParentId)) return true;
        OrgUnit parent = orgUnitRepository.findById(newParentId).orElse(null);
        while (parent != null) {
            if (parent.getId().equals(targetOrgUnitId)) return true;
            parent = parent.getParentId() != null ? orgUnitRepository.findById(parent.getParentId()).orElse(null) : null;
        }
        return false;
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
                            m.put("parentId", ou.getParentId());
                            if (ou.getParentId() != null) {
                                orgUnitRepository.findById(ou.getParentId())
                                        .ifPresent(p -> m.put("parentComp", p.getCode() + " (" + (p.getName() != null ? p.getName() : "") + ")"));
                            }
                            m.put("compTel", mp.getCompTel());
                            m.put("zipCode", mp.getZipCode());
                            m.put("addr", mp.getAddr());
                            m.put("addrDetail", mp.getAddrDetail());
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
                            m.put("bankCd", mp.getBankCd());
                            m.put("transferFee", mp.getTransferFee());
                            m.put("accountNo", mp.getAccountNo());
                            m.put("accountHolder", mp.getAccountHolder());
                            m.put("remark", mp.getRemark());
                            m.put("commissionConfigAllowed", mp.getCommissionConfigAllowed());
                            m.put("webPaymentUseYn", mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn() : "Y");
                            m.put("baseCurrency", mp.getBaseCurrency());
                            m.put("orgUnitId", ou.getId());
                            settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                                m.put("calcCycle", ss.getCalcCycle());
                                m.put("transferType", ss.getTransferType());
                                m.put("holdRate", ss.getHoldRate());
                                m.put("holdDays", ss.getHoldDays());
                                m.put("payLimitDefault", ss.getPayLimitDefault());
                            });
                            return m;
                        }));
    }

    /** 지역 본사(업체) 정보 수정 - parentId 변경 시 상위 조직 검증 및 순환 참조 방지 */
    public boolean update(String compId, String compNm, String compDiv, Long parentId,
                          String compTel, String zipCode, String addr, String addrDetail, String ceoNm, String ceoMobile,
                          String useYn, String loginId, String regNo, String bizType, String industry,
                          String bizNature, String product, String homepage, String settleName, String settleTelNo,
                          String fax, String email, String bankCd, String transferFee, String accountNo, String accountHolder,
                          String remark, String commissionConfigAllowed, String webPaymentUseYn, String baseCurrency) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            String effectiveCompDiv = compDiv != null ? compDiv : (ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
                            if (parentId != null) {
                                OrgUnit parent = orgUnitRepository.findById(parentId).orElse(null);
                                if (parent == null) throw new IllegalArgumentException("상위 업체를 찾을 수 없습니다.");
                                if (!isValidParentLevel(effectiveCompDiv, parent.getOrgLevel())) {
                                    throw new IllegalArgumentException("업체구분에 맞는 상위 조직이 아닙니다. (본사→총본사, 총판→본사, 지사→총판, 대리점→지사, 가맹점→대리점)");
                                }
                                if (wouldCreateCycle(ou.getId(), parentId)) {
                                    throw new IllegalArgumentException("자기 자신 또는 하위 조직을 상위로 지정할 수 없습니다.");
                                }
                                ou.setParentId(parentId);
                            }
                            if (compNm != null) ou.setName(compNm);
                            if (compDiv != null) ou.setOrgLevel(orgLevelFromCompDiv(compDiv));
                            orgUnitRepository.save(ou);
                            if (commissionConfigAllowed != null) mp.setCommissionConfigAllowed(commissionConfigAllowed);
                            if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
                            if (baseCurrency != null && !baseCurrency.trim().isEmpty()) mp.setBaseCurrency(baseCurrency.trim());
                            mp.setCompTel(compTel);
                            mp.setZipCode(zipCode);
                            mp.setAddr(addr);
                            mp.setAddrDetail(addrDetail);
                            mp.setCeoNm(ceoNm);
                            mp.setCeoMobile(ceoMobile);
                            mp.setUseYn(useYn);
                            mp.setLoginId(loginId);
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
                            merchantProfileRepository.save(mp);
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
        return registerWithExtra(code, name, compDiv, parentId, compTel, zipCode, addr, addrDetail,
                ceoNm, ceoMobile, useYn, loginId, regNo,
                null, null, null, null, null, null, null, null, null, null, null, /* settleType, commissionRate, limitAmt */ email, pwd,
                bankCd, transferFee, accountNo, accountHolder,
                null, null, null, null, null, null, null, null, remark,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                null,
                null, null, null, null, null, null);  /* pgBindings, webPaymentUseYn, baseCurrency, defaultProduct*, notifyUrl*, commission*, fee* */
    }

    public OrgUnit registerWithExtra(String code, String name, String compDiv, Long parentId,
                                     String compTel, String zipCode, String addr, String addrDetail,
                                     String ceoNm, String ceoMobile, String useYn, String loginId,
                                     String regNo, String bizType, String industry,
                                     String bizNature, String product, String homepage, String settleName, String settleTelNo,
                                     String settleType, String commissionRate, String limitAmt,
                                     String fax, String email, String pwd,
                                     String bankCd, String transferFee, String accountNo, String accountHolder,
                                     String countryCd, String swift, String branchName, String branchAddr,
                                     String contactTel, String walletAddress, String networkName, String siteUrl,
                                     String remark,
                                     Integer withdrawLimitDays, String withdrawStartTime, String withdrawEndTime,
                                     String payLimitDefault, String payLimitExtra, String payLimitAlertSms,
                                     String holdRateFollowHq, String holdRate, Integer holdDays, String calcCycle, String calcCloseTime,
                                     String transferType, Integer transferCycleDays, String autoTransferMin, String payHoldYn,
                                     String calcExcludeYn, String calcExcludeTarget, String calcStartTime,
                                     String pgBindings, String webPaymentUseYn, String baseCurrency,
                                     String defaultProductName, String defaultProductCode, String defaultProductAmount, String defaultProductDesc,
                                     String notifyUrlBackground, String notifyUrlResult,
                                     String commissionFollowHq, String perTxFee, String cancelRate, String usageRate,
                                     String failFee, String payRate, String refundRate, String rollingPct, String rollingDays,
                                     String feeSettlementPerTx, String feeUsdt, String feeFx) {
        String effectiveCompDiv = compDiv != null ? compDiv.toUpperCase() : "AGENCY";
        if ("MASTER_DIST".equals(effectiveCompDiv) || "BRANCH".equals(effectiveCompDiv) || "AGENCY".equals(effectiveCompDiv) || "MERCHANT".equals(effectiveCompDiv)) {
            if (parentId == null) {
                throw new IllegalArgumentException("총판·지사·대리점·가맹점은 개설 시 상위 지점을 반드시 선택해야 합니다.");
            }
            OrgUnit parent = orgUnitRepository.findById(parentId).orElse(null);
            if (parent == null) {
                throw new IllegalArgumentException("선택한 상위 지점을 찾을 수 없습니다.");
            }
            if (!isValidParentLevel(effectiveCompDiv, parent.getOrgLevel())) {
                throw new IllegalArgumentException("업체구분에 맞는 상위 지점이 아닙니다. (총판→본사, 지사→총판, 대리점→지사, 가맹점→대리점)");
            }
        }
        OrgUnit o = new OrgUnit();
        o.setCode(code != null ? code : "C" + System.currentTimeMillis());
        o.setName(name != null ? name : "");
        o.setOrgLevel(orgLevelFromCompDiv(compDiv != null ? compDiv : "AGENCY"));
        o.setParentId(parentId);
        o.setStatus("ACTIVE");
        OrgUnit saved = orgUnitRepository.save(o);

        MerchantProfile mp = new MerchantProfile();
        mp.setOrgUnitId(saved.getId());
        mp.setCompDiv(compDiv);
        mp.setCompTel(compTel);
        mp.setZipCode(zipCode);
        mp.setAddr(addr);
        mp.setAddrDetail(addrDetail);
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
        mp.setPwd(pwd);
        mp.setBankCd(bankCd);
        mp.setTransferFee(transferFee);
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
        mp.setRemark(remark);
        if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
        if (baseCurrency != null && !baseCurrency.trim().isEmpty()) mp.setBaseCurrency(baseCurrency.trim());
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(saved.getId());
        ss.setCalcCycle(calcCycle != null && !calcCycle.isEmpty() ? calcCycle : "D7");
        ss.setTransferType(transferType != null && !transferType.isEmpty() ? transferType : "MANUAL");
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
        }

        return saved;
    }

    public Optional<Map<String, Object>> getSettlementSetting(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId())
                        .map(ss -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("compId", ou.getCode());
                            m.put("orgUnitId", ou.getId());
                            m.put("withdrawLimitDays", ss.getWithdrawLimitDays());
                            m.put("payLimitDefault", ss.getPayLimitDefault());
                            m.put("payLimitExtra", ss.getPayLimitExtra());
                            m.put("holdRate", ss.getHoldRate());
                            m.put("holdDays", ss.getHoldDays());
                            m.put("calcCycle", ss.getCalcCycle());
                            m.put("transferType", ss.getTransferType());
                            m.put("autoTransferMin", ss.getAutoTransferMin());
                            m.put("payHoldYn", ss.getPayHoldYn());
                            return m;
                        }));
    }

    public boolean saveSettlementSetting(String compId, Integer withdrawLimitDays, String payLimitDefault, String payLimitExtra,
                                         String holdRate, Integer holdDays, String calcCycle, String transferType,
                                         String autoTransferMin, String payHoldYn) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId())
                        .map(ss -> {
                            if (withdrawLimitDays != null) ss.setWithdrawLimitDays(withdrawLimitDays);
                            if (payLimitDefault != null && !payLimitDefault.isEmpty()) try { ss.setPayLimitDefault(new BigDecimal(payLimitDefault.trim())); } catch (Exception ignored) {}
                            if (payLimitExtra != null && !payLimitExtra.isEmpty()) try { ss.setPayLimitExtra(new BigDecimal(payLimitExtra.trim())); } catch (Exception ignored) {}
                            if (holdRate != null && !holdRate.isEmpty()) try { ss.setHoldRate(new BigDecimal(holdRate.trim())); } catch (Exception ignored) {}
                            if (holdDays != null) ss.setHoldDays(holdDays);
                            if (calcCycle != null && !calcCycle.isEmpty()) ss.setCalcCycle(calcCycle);
                            if (transferType != null && !transferType.isEmpty()) ss.setTransferType(transferType);
                            if (autoTransferMin != null && !autoTransferMin.isEmpty()) try { ss.setAutoTransferMin(new BigDecimal(autoTransferMin.trim())); } catch (Exception ignored) {}
                            if (payHoldYn != null && !payHoldYn.isEmpty()) ss.setPayHoldYn(payHoldYn);
                            settlementSettingRepository.save(ss);
                            return true;
                        }))
                .orElse(false);
    }

    private static String optStr(Map<String, Object> m, String key) {
        Object v = m != null ? m.get(key) : null;
        return v != null ? v.toString().trim() : null;
    }

    /** 업체관리 목록용 행 구성 (엑셀 다운로드 컬럼과 동일) */
    private Map<String, Object> buildCompListItem(OrgUnit o) {
        Map<String, Object> m = CompListItemDto.from(o);
        m.put("regNo", "-");
        m.put("ceoNm", "-");
        m.put("industry", "-");
        m.put("bizType", "-");
        m.put("email", "-");
        m.put("contact", "-");
        m.put("zipCode", "-");
        m.put("addr", "-");
        m.put("bankNm", "-");
        m.put("accountNo", "-");
        m.put("accountHolder", "-");
        m.put("calcCycle", "-");
        m.put("calcCloseTime", "-");
        m.put("transferFee", "-");
        m.put("transferType", "사용안함");
        m.put("transferCycleHours", "-");
        m.put("calcExcludeYn", "-");
        m.put("calcExcludeTarget", "-");
        m.put("calcStartTime", "-");
        m.put("payHoldYnDisplay", "-");
        m.put("payLimitDefault", "-");
        m.put("payLimitExtra", "-");
        m.put("payLimitMonth", "-");
        m.put("payLimitYear", "-");
        m.put("withdrawLimitHour", "-");
        m.put("payAmountInTime", "-");
        m.put("sameCardLimitDayWeb", "-");
        m.put("sameCardLimitCntWeb", "-");
        m.put("sameCardLimitAmtWeb", "-");
        m.put("sameCardLimitDayTerminal", "-");
        m.put("sameCardLimitCntTerminal", "-");
        m.put("sameCardLimitAmtTerminal", "-");
        m.put("payLimitDaily", "-");
        m.put("terminalCountTerminal", "-");
        m.put("terminalCountWeb", "-");
        m.put("useYnDisplay", "-");
        m.put("settlementAmt", "-");
        m.put("receivables", "-");
        merchantProfileRepository.findByOrgUnitId(o.getId()).ifPresent(mp -> {
            m.put("regNo", mp.getRegNo() != null ? mp.getRegNo() : "-");
            m.put("ceoNm", mp.getCeoNm() != null ? mp.getCeoNm() : "-");
            m.put("industry", mp.getIndustry() != null ? mp.getIndustry() : "-");
            m.put("bizType", mp.getBizType() != null ? mp.getBizType() : "-");
            m.put("email", mp.getEmail() != null ? mp.getEmail() : "-");
            String contact = mp.getCeoMobile() != null && !mp.getCeoMobile().isEmpty() ? mp.getCeoMobile() : (mp.getCompTel() != null ? mp.getCompTel() : "-");
            m.put("contact", contact);
            m.put("zipCode", mp.getZipCode() != null ? mp.getZipCode() : "-");
            String fullAddr = (mp.getAddr() != null ? mp.getAddr() : "") + (mp.getAddrDetail() != null ? " " + mp.getAddrDetail() : "");
            m.put("addr", fullAddr.trim().isEmpty() ? "-" : fullAddr.trim());
            m.put("bankNm", bankCdToName(mp.getBankCd()));
            m.put("accountNo", mp.getAccountNo() != null ? mp.getAccountNo() : "-");
            m.put("accountHolder", mp.getAccountHolder() != null ? mp.getAccountHolder() : "-");
            m.put("transferFee", mp.getTransferFee() != null ? mp.getTransferFee() : "-");
            m.put("useYnDisplay", "Y".equals(mp.getUseYn()) ? "사용" : "미사용");
            m.put("terminalCountTerminal", mp.getTerminalCountTerminal() != null ? mp.getTerminalCountTerminal().toString() : "-");
            m.put("terminalCountWeb", mp.getTerminalCountWeb() != null ? mp.getTerminalCountWeb().toString() : "-");
        });
        settlementSettingRepository.findByOrgUnitId(o.getId()).ifPresent(ss -> {
            m.put("calcCycle", calcCycleToDisplay(ss.getCalcCycle()));
            m.put("calcCloseTime", ss.getCalcCloseTime() != null ? ss.getCalcCloseTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-");
            m.put("transferType", transferTypeToDisplay(ss.getTransferType()));
            m.put("transferCycleHours", ss.getTransferCycleDays() != null ? String.valueOf(ss.getTransferCycleDays()) : "-");
            m.put("calcExcludeYn", "Y".equals(ss.getCalcExcludeYn()) ? "Y" : "N");
            m.put("calcExcludeTarget", ss.getCalcExcludeTarget() != null ? ss.getCalcExcludeTarget() : "-");
            m.put("calcStartTime", ss.getCalcStartTime() != null ? ss.getCalcStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-");
            m.put("payHoldYnDisplay", "Y".equals(ss.getPayHoldYn()) ? "보류" : "지급");
            m.put("payLimitDefault", ss.getPayLimitDefault() != null ? ss.getPayLimitDefault().toString() : "-");
            m.put("payLimitExtra", ss.getPayLimitExtra() != null ? ss.getPayLimitExtra().toString() : "-");
            m.put("payLimitMonth", ss.getPayLimitMonth() != null ? ss.getPayLimitMonth().toString() : "-");
            m.put("payLimitYear", ss.getPayLimitYear() != null ? ss.getPayLimitYear().toString() : "-");
            m.put("withdrawLimitHour", ss.getWithdrawLimitHour() != null ? ss.getWithdrawLimitHour().toString() : "-");
            m.put("payAmountInTime", ss.getPayAmountInTime() != null ? ss.getPayAmountInTime().toString() : "-");
            m.put("sameCardLimitDayWeb", ss.getSameCardLimitDayWeb() != null ? ss.getSameCardLimitDayWeb().toString() : "-");
            m.put("sameCardLimitCntWeb", ss.getSameCardLimitCntWeb() != null ? ss.getSameCardLimitCntWeb().toString() : "-");
            m.put("sameCardLimitAmtWeb", ss.getSameCardLimitAmtWeb() != null ? ss.getSameCardLimitAmtWeb().toString() : "-");
            m.put("sameCardLimitDayTerminal", ss.getSameCardLimitDayTerminal() != null ? ss.getSameCardLimitDayTerminal().toString() : "-");
            m.put("sameCardLimitCntTerminal", ss.getSameCardLimitCntTerminal() != null ? ss.getSameCardLimitCntTerminal().toString() : "-");
            m.put("sameCardLimitAmtTerminal", ss.getSameCardLimitAmtTerminal() != null ? ss.getSameCardLimitAmtTerminal().toString() : "-");
            m.put("payLimitDaily", ss.getPayLimitDaily() != null ? ss.getPayLimitDaily().toString() : "-");
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

    private boolean matchUseYn(OrgUnit o, String useYn) {
        if (useYn == null || useYn.trim().isEmpty()) return true;
        return merchantProfileRepository.findByOrgUnitId(o.getId())
                .map(mp -> useYn.trim().equals(mp.getUseYn() != null ? mp.getUseYn() : "")).orElse(true);
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

    private static String calcCycleToDisplay(String c) {
        if (c == null || c.isEmpty()) return "-";
        if (c.matches("D\\d+")) return "D+" + c.substring(1);
        return c;
    }

    private static String transferTypeToDisplay(String t) {
        if (t == null || t.isEmpty()) return "사용안함";
        return switch (t.toUpperCase()) {
            case "MANUAL" -> "수동";
            case "AUTO" -> "자동";
            case "FUMBANKING" -> "펌뱅킹";
            default -> t;
        };
    }
}
