package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementSetting;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayListService {

    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementSettingRepository settlementSettingRepository;

    public PayListService(PgTrnsctnRepository trnsctnRepository,
                          OrgUnitRepository orgUnitRepository,
                          MerchantProfileRepository merchantProfileRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository,
                          DistributionFeeConfigRepository distributionFeeConfigRepository,
                          CommissionPolicyRepository commissionPolicyRepository,
                          SettlementSettingRepository settlementSettingRepository) {
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    public PageResult<Map<String, Object>> search(String merchantId, LocalDate fromDt, LocalDate toDt,
                                                  int page, int size, String payListVariant) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PgTrnsctn> spec = buildSpecification(merchantId, from, to, payListVariant);
        Page<PgTrnsctn> result = trnsctnRepository.findAll(spec, p);
        List<String> merchantCodes = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());

        Map<String, OrgUnit> ouByCode = new HashMap<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (merchantCodes.contains(ou.getCode())) {
                ouByCode.put(ou.getCode(), ou);
            }
        }
        Optional<CommissionPolicy> defaultPolicy = commissionPolicyRepository.findByScope("DEFAULT");

        Map<String, PayListRowContext> ctxByCode = new HashMap<>();
        for (String code : merchantCodes) {
            OrgUnit merchant = ouByCode.get(code);
            String compNm = merchant != null ? merchant.getName() : code;
            MerchantProfile profile = merchant != null
                    ? merchantProfileRepository.findByOrgUnitId(merchant.getId()).orElse(null)
                    : null;
            MerchantPgBinding binding = pickBinding(merchant);
            DistributionFeeConfig dist = distributionFeeConfigRepository.findByCompId(code).orElse(null);
            CommissionPolicy pol = commissionPolicyRepository.findByScope(code).or(() -> defaultPolicy).orElse(null);
            SettlementSetting ss = merchant != null
                    ? settlementSettingRepository.findByOrgUnitId(merchant.getId()).orElse(null)
                    : null;
            String[] hier = hierarchyNames(merchant);
            ctxByCode.put(code, new PayListRowContext(compNm, profile, binding, dist, pol, ss,
                    hier[0], hier[1], hier[2]));
        }

        List<Map<String, Object>> list = result.getContent().stream()
                .map(t -> PayListItemDto.from(t, ctxByCode.get(t.getMerchantId())))
                .collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        return pr;
    }

    private MerchantPgBinding pickBinding(OrgUnit merchant) {
        if (merchant == null) return null;
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(merchant.getId());
        if (list.isEmpty()) return null;
        return list.stream().filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst()
                .orElse(list.get(0));
    }

    /** [0]=총판, [1]=지사, [2]=대리점(영업점) */
    private String[] hierarchyNames(OrgUnit merchant) {
        String regional = "", master = "", branch = "";
        OrgUnit cur = merchant;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case MASTER_DIST -> regional = cur.getName();
                    case BRANCH -> master = cur.getName();
                    case AGENCY, SALES_OFFICE -> branch = cur.getName();
                    default -> { }
                }
            }
            Long pid = cur.getParentId();
            cur = pid != null ? orgUnitRepository.findById(pid).orElse(null) : null;
        }
        return new String[] { regional, master, branch };
    }

    private Specification<PgTrnsctn> buildSpecification(String merchantId, LocalDateTime fromDt, LocalDateTime toDt, String rawVariant) {
        String variant = rawVariant == null || rawVariant.isBlank() ? "INTEGRATED" : rawVariant.trim().toUpperCase();
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (merchantId != null && !merchantId.isBlank()) {
                parts.add(cb.equal(root.get("merchantId"), merchantId));
            }
            if (fromDt != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (toDt != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDt));
            }
            parts.add(variantPredicate(root, cb, variant));
            return cb.and(parts.toArray(new Predicate[0]));
        };
    }

    private Predicate variantPredicate(jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String variant) {
        return switch (variant) {
            case "INTEGRATED" -> cb.conjunction();
            case "SUCCESS" -> cb.equal(root.get("status"), "10");
            case "FAIL" -> root.get("status").in("F0", "99");
            case "REFUND" -> cb.equal(root.get("status"), "30");
            case "FORCE_REFUND" -> cb.equal(root.get("status"), "31");
            case "CANCEL" -> cb.equal(root.get("status"), "20");
            case "OFFSET_CANCEL" -> cb.or(cb.isNull(root.get("status")), cb.notEqual(root.get("status"), "10"));
            case "URL_PAY" -> cb.equal(root.get("origin"), "URL");
            case "NOTI" -> cb.equal(root.get("origin"), "NOTI");
            default -> cb.conjunction();
        };
    }
}
