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
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static LocalTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String t = s.trim();
            if (t.matches("\\d{1,2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm"));
            if (t.matches("\\d{1,2}:\\d{2}:\\d{2}")) return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm:ss"));
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) { return null; }
    }

    /** 업체구분(compDiv) 문자열 → OrgLevel 매핑 (총본사/본사/총판/지사/대리점/영업점/가맹점) */
    private static OrgLevel orgLevelFromCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return OrgLevel.AGENCY;
        return switch (compDiv.toUpperCase()) {
            case "REGIONAL" -> OrgLevel.REGIONAL;
            case "MASTER_DIST" -> OrgLevel.MASTER_DIST;
            case "BRANCH" -> OrgLevel.BRANCH;
            case "AGENCY" -> OrgLevel.AGENCY;
            case "SALES_OFFICE" -> OrgLevel.SALES_OFFICE;
            case "MERCHANT" -> OrgLevel.MERCHANT;
            default -> OrgLevel.AGENCY;
        };
    }

    /** 업체구분별 접두사: 10=총본사, 11=본사, 12=총판, 13=지사, 14=대리점, 15=영업점, 20=가맹점. 업체코드는 자동 부여되며 조직 생성 시 순서대로 번호가 부여됨. 업체코드는 유일한 구별 정보. */
    private static String compCodePrefixFromCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return "14";
        return switch (compDiv.toUpperCase()) {
            case "HEADQUARTERS" -> "10";
            case "REGIONAL" -> "11";
            case "MASTER_DIST" -> "12";
            case "BRANCH" -> "13";
            case "AGENCY" -> "14";
            case "SALES_OFFICE" -> "15";
            case "MERCHANT" -> "20";
            default -> "14";
        };
    }

    public CompService(OrgUnitRepository orgUnitRepository, MerchantProfileRepository merchantProfileRepository,
                       SettlementSettingRepository settlementSettingRepository,
                       MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                       MerchantPgBindingRepository merchantPgBindingRepository,
                       MerchantDefaultProductRepository merchantDefaultProductRepository,
                       MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                       CommissionPolicyRepository commissionPolicyRepository,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** scopeCompId: 로그인 사용자의 업체코드(본인 org만 조회, 업체정보조회용) */
    public PageResult<Map<String, Object>> search(String compId, String compNm, int page, int size, String scopeCompId) {
        return search(compId, compNm, null, null, null, null, null, null, null, null, page, size, scopeCompId);
    }

    /** 업체관리 검색 - 확장 파라미터 (업체구분, 사용상태, 지급보류, 대표자명, 터미널ID, 휴대폰, 사업자번호, 하위업체포함) */
    public PageResult<Map<String, Object>> search(String compId, String compNm,
            String compDiv, String useYn, String payHoldYn, String ceoNm, String terminalId, String ceoMobile, String regNo, Boolean includeSub,
            int page, int size, String scopeCompId) {
        String cId = (compId != null && !compId.trim().isEmpty()) ? compId.trim() : null;
        String cNm = (compNm != null && !compNm.trim().isEmpty()) ? compNm.trim() : null;
        String cDiv = (compDiv != null && !compDiv.trim().isEmpty()) ? compDiv.trim() : null;
        List<OrgUnit> all = orgUnitRepository.findAll();
        List<OrgUnit> filtered = all.stream()
                .filter(o -> (scopeCompId == null || scopeCompId.trim().isEmpty() || (o.getCode() != null && o.getCode().equals(scopeCompId))))
                .filter(o -> (cId == null || (o.getCode() != null && o.getCode().contains(cId))))
                .filter(o -> (cNm == null || (o.getName() != null && o.getName().contains(cNm))))
                .filter(o -> (cDiv == null || (o.getOrgLevel() != null && o.getOrgLevel().name().equals(cDiv))))
                .filter(o -> matchUseYn(o, useYn))
                .filter(o -> matchPayHoldYn(o, payHoldYn))
                .filter(o -> matchCeoNm(o, ceoNm))
                .filter(o -> matchCeoMobile(o, ceoMobile))
                .filter(o -> matchRegNo(o, regNo))
                .collect(Collectors.toList());
        java.util.Map<Long, String> idToSortKey = buildHierarchySortKeys(all);
        java.util.Map<Long, Integer> idToDepth = buildHierarchyDepth(all);
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
                row.put("depth", idToDepth.getOrDefault(ou.getId(), 0));
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
                            if (ou.getParentId() != null) {
                                orgUnitRepository.findById(ou.getParentId())
                                        .ifPresent(p -> m.put("parentComp", p.getCode() + (p.getName() != null ? " (" + p.getName() + ")" : "")));
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
                            List<Map<String, Object>> pgBindings = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId()).stream()
                                    .map(b -> Map.<String, Object>of(
                                            "pgCd", b.getPgCd() != null ? b.getPgCd() : "",
                                            "activationYn", b.getActivationYn() != null ? b.getActivationYn() : "Y",
                                            "operationalYn", b.getOperationalYn() != null ? b.getOperationalYn() : "N",
                                            "payMethod", b.getPayMethod() != null ? b.getPayMethod() : "WEB",
                                            "mid", b.getMid() != null ? b.getMid() : "",
                                            "apiKey", b.getApiKey() != null ? b.getApiKey() : "",
                                            "ivKey", b.getIvKey() != null ? b.getIvKey() : "",
                                            "installmentYn", b.getInstallmentYn() != null ? b.getInstallmentYn() : "N",
                                            "maxInstallmentMonths", b.getMaxInstallmentMonths() != null ? String.valueOf(b.getMaxInstallmentMonths()) : ""
                                    ))
                                    .collect(Collectors.toList());
                            m.put("pgBindings", pgBindings);
                            settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                                m.put("calcCycle", ss.getCalcCycle());
                                m.put("transferType", ss.getTransferType());
                                m.put("holdRate", ss.getHoldRate());
                                m.put("holdDays", ss.getHoldDays());
                                m.put("payLimitDefault", ss.getPayLimitDefault());
                                m.put("withdrawLimitDays", ss.getWithdrawLimitDays());
                                m.put("withdrawStartTime", ss.getWithdrawStartTime() != null ? ss.getWithdrawStartTime().toString() : null);
                                m.put("withdrawEndTime", ss.getWithdrawEndTime() != null ? ss.getWithdrawEndTime().toString() : null);
                                m.put("payLimitExtra", ss.getPayLimitExtra());
                                m.put("payLimitAlertSms", ss.getPayLimitAlertSms());
                                m.put("holdRateFollowHq", ss.getHoldRateFollowHq());
                                m.put("calcCloseTime", ss.getCalcCloseTime() != null ? ss.getCalcCloseTime().toString() : null);
                                m.put("transferCycleDays", ss.getTransferCycleDays());
                                m.put("autoTransferMin", ss.getAutoTransferMin());
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
                          String zipCode, String addr, String addrDetail, String ceoNm, String ceoMobile,
                          String useYn, String loginId, String regNo, String bizType, String industry,
                          String bizNature, String product, String homepage, String settleName, String settleTelNo,
                          String fax, String email, String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                          String remark, String commissionConfigAllowed, String webPaymentUseYn, String baseCurrency,
                          String siteUrl, String siteSummary, String pgBindings) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            if (compNm != null) ou.setName(compNm);
                            if (compDiv != null) ou.setOrgLevel(orgLevelFromCompDiv(compDiv));
                            if (parentId != null) ou.setParentId(parentId);
                            orgUnitRepository.save(ou);
                            if (commissionConfigAllowed != null) mp.setCommissionConfigAllowed(commissionConfigAllowed);
                            if (webPaymentUseYn != null && !webPaymentUseYn.trim().isEmpty()) mp.setWebPaymentUseYn(webPaymentUseYn.trim());
                            if (baseCurrency != null && !baseCurrency.trim().isEmpty()) mp.setBaseCurrency(baseCurrency.trim());
                            if (siteUrl != null) mp.setSiteUrl(siteUrl.trim());
                            if (siteSummary != null) mp.setSiteSummary(siteSummary.trim());
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

    /** 업체 비밀번호 초기화 - 기본 비밀번호로 재설정 (MerchantProfile.pwd, AppUser.password) */
    public boolean resetPassword(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            String defaultPwd = "test123!";
                            String encoded = passwordEncoder.encode(defaultPwd);
                            mp.setPwd(encoded);
                            merchantProfileRepository.save(mp);
                            String loginId = mp.getLoginId();
                            if (loginId != null && !loginId.isEmpty()) {
                                userRepository.findByUsername(loginId).ifPresent(u -> {
                                    u.setPassword(encoded);
                                    userRepository.save(u);
                                });
                            }
                            return true;
                        }))
                .orElse(false);
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
                                appUser.setPassword(passwordEncoder.encode("test123!"));
                                appUser.setName(mp.getCeoNm() != null ? mp.getCeoNm() : trimmed);
                                appUser.setRole("USER");
                                appUser.setEnabled(true);
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
        return registerWithExtra(code, name, compDiv, parentId, compTel, zipCode, addr, addrDetail,
                ceoNm, ceoMobile, useYn, loginId, regNo,
                null, null, null, null, null, null, null, null, null, null, null, /* settleType, commissionRate, limitAmt */ email, pwd,
                bankCd, transferFee, null, accountNo, accountHolder,
                null, null, null, null, null, null, null, null, null, remark,
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
                                     String bankCd, String transferFee, String cryptoTransferFee, String accountNo, String accountHolder,
                                     String countryCd, String swift, String branchName, String branchAddr,
                                     String contactTel, String walletAddress, String networkName, String siteUrl, String siteSummary,
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
        OrgUnit o = new OrgUnit();
        String compDivVal = compDiv != null ? compDiv.trim() : "AGENCY";
        String finalCode = (code != null && !code.trim().isEmpty()) ? code.trim() : generateNextCompCode(compDivVal);
        if (orgUnitRepository.findByCode(finalCode).isPresent()) {
            throw new IllegalArgumentException("업체코드가 이미 존재합니다: " + finalCode);
        }
        o.setCode(finalCode);
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

    /** 업체코드 10자리 자동 부여: 업체구분별 접두사(11=본사, 12=총판, 13=지사, 14=대리점, 15=영업점, 20=가맹점) + 8자리 순번, 전 업체 유일 */
    private synchronized String generateNextCompCode(String compDiv) {
        String prefix = compCodePrefixFromCompDiv(compDiv);
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
        merchantProfileRepository.findByOrgUnitId(o.getId()).ifPresent(mp -> {
            m.put("regNo", mp.getRegNo() != null ? mp.getRegNo() : "-");
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
            m.put("calcCycle", calcCycleToDisplay(ss.getCalcCycle()));
            m.put("transferType", transferTypeToDisplay(ss.getTransferType()));
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

    private static String calcExcludeTargetToDisplay(String t) {
        if (t == null || t.isEmpty()) return "-";
        return switch (t.toUpperCase()) {
            case "NONE" -> "전체";
            case "WEB" -> "웹";
            case "OFFLINE" -> "오프라인";
            default -> t;
        };
    }

    private static String payHoldYnToDisplay(String y) {
        if (y == null || y.isEmpty()) return "-";
        return "Y".equalsIgnoreCase(y) ? "보류" : "정상";
    }
}
