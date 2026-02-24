package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.CompListItemDto;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantProfile;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    public CompService(OrgUnitRepository orgUnitRepository, MerchantProfileRepository merchantProfileRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public PageResult<Map<String, Object>> search(String compId, String compNm, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.ASC, "code"));
        List<OrgUnit> all = orgUnitRepository.findAll();
        List<OrgUnit> filtered = all.stream()
                .filter(o -> (compId == null || compId.isEmpty() || (o.getCode() != null && o.getCode().contains(compId))))
                .filter(o -> (compNm == null || compNm.isEmpty() || (o.getName() != null && o.getName().contains(compNm))))
                .collect(Collectors.toList());
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = filtered.subList(start, end).stream()
                .map(o -> {
                    Map<String, Object> m = CompListItemDto.from(o);
                    merchantProfileRepository.findByOrgUnitId(o.getId())
                            .ifPresent(mp -> m.put("regNo", mp.getRegNo()));
                    return m;
                })
                .collect(Collectors.toList());
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
                            m.put("parentId", ou.getParentId());
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
                            m.put("fax", mp.getFax());
                            m.put("email", mp.getEmail());
                            m.put("bankCd", mp.getBankCd());
                            m.put("transferFee", mp.getTransferFee());
                            m.put("accountNo", mp.getAccountNo());
                            m.put("accountHolder", mp.getAccountHolder());
                            m.put("remark", mp.getRemark());
                            return m;
                        }));
    }

    /** 지역 본사(업체) 정보 수정 */
    public boolean update(String compId, String compNm, String compDiv, String compTel,
                          String zipCode, String addr, String addrDetail, String ceoNm, String ceoMobile,
                          String useYn, String loginId, String regNo, String bizType, String industry, String fax,
                          String email, String bankCd, String transferFee, String accountNo, String accountHolder,
                          String remark) {
        return orgUnitRepository.findByCode(compId != null ? compId : "")
                .flatMap(ou -> merchantProfileRepository.findByOrgUnitId(ou.getId())
                        .map(mp -> {
                            if (compNm != null) ou.setName(compNm);
                            if (compDiv != null) ou.setOrgLevel("MERCHANT".equals(compDiv) ? OrgLevel.MERCHANT : OrgLevel.AGENCY);
                            orgUnitRepository.save(ou);
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
                ceoNm, ceoMobile, useYn, loginId, regNo, null, null, null, email, pwd,
                bankCd, transferFee, accountNo, accountHolder, remark);
    }

    public OrgUnit registerWithExtra(String code, String name, String compDiv, Long parentId,
                                     String compTel, String zipCode, String addr, String addrDetail,
                                     String ceoNm, String ceoMobile, String useYn, String loginId,
                                     String regNo, String bizType, String industry, String fax,
                                     String email, String pwd,
                                     String bankCd, String transferFee, String accountNo, String accountHolder,
                                     String remark) {
        OrgUnit o = new OrgUnit();
        o.setCode(code != null ? code : "C" + System.currentTimeMillis());
        o.setName(name != null ? name : "");
        o.setOrgLevel(compDiv != null && compDiv.equals("MERCHANT") ? OrgLevel.MERCHANT : OrgLevel.AGENCY);
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
        mp.setFax(fax);
        mp.setEmail(email);
        mp.setPwd(pwd);
        mp.setBankCd(bankCd);
        mp.setTransferFee(transferFee);
        mp.setAccountNo(accountNo);
        mp.setAccountHolder(accountHolder);
        mp.setRemark(remark);
        merchantProfileRepository.save(mp);

        return saved;
    }
}
