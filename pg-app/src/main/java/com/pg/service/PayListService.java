package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public PageResult<Map<String, Object>> search(PayListSearchRequest req) {
        if (req == null) {
            req = new PayListSearchRequest();
        }
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        int page = Math.max(1, req.getPage());
        int size = Math.min(100, Math.max(1, req.getSize()));
        Pageable p = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to);
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

    private Specification<PgTrnsctn> buildSpecification(PayListSearchRequest req, LocalDateTime fromDt, LocalDateTime toDt) {
        Set<String> merchantCodes = resolveMerchantFilterCodes(req);
        if (merchantCodes != null && merchantCodes.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        String variant = req.getPayListVariant() == null || req.getPayListVariant().isBlank()
                ? "INTEGRATED" : req.getPayListVariant().trim().toUpperCase(Locale.ROOT);
        final Set<String> mcsFinal = merchantCodes;
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (mcsFinal != null) {
                parts.add(root.get("merchantId").in(mcsFinal));
            }
            if (fromDt != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (toDt != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDt));
            }
            parts.add(variantPredicate(root, cb, variant));
            addPayDivPredicate(parts, root, cb, req.getSearchPayDivCd());
            addPayProcPredicate(parts, root, cb, req.getSearchPayProcCd());
            addTranFactorColumnPredicates(parts, root, cb, req.getSearchTranFactor(), req.getSearchTranValue());
            addPgAgencyPredicate(parts, root, cb, req.getSearchPgCd());
            addKeywordPredicate(parts, root, cb, req.getSearchKeyword());
            addLike(parts, root, cb, "chillTransactionId", req.getSearchChillTxnId());
            addLike(parts, root, cb, "approvalNo", req.getSearchCardAprvNo());
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    /**
     * null = 가맹점 제한 없음, 비어 있지 않은 Set = 해당 코드만, empty Set = 조건 불충족(결과 0건).
     */
    private Set<String> resolveMerchantFilterCodes(PayListSearchRequest req) {
        Set<String> mcs = null;
        mcs = intersectCodes(mcs, codesFromCompField(req.getSearchCompField(), req.getSearchCompQ()));
        mcs = intersectCodes(mcs, codesFromRegNo(req.getSearchRegNo()));
        mcs = intersectCodes(mcs, codesFromCalcCycle(req.getSearchCycle()));
        mcs = intersectCodes(mcs, codesFromMid(req.getSearchMid()));

        String tf = req.getSearchTranFactor();
        String tv = req.getSearchTranValue();
        if (tf != null && !tf.isBlank() && tv != null && !tv.isBlank()) {
            String f = tf.trim().toUpperCase(Locale.ROOT);
            if ("MERCHANT".equals(f)) {
                mcs = intersectCodes(mcs, codesFromCompField("BOTH", tv));
            } else if ("MID".equals(f)) {
                mcs = intersectCodes(mcs, codesFromMid(tv.trim()));
            }
        }
        return mcs;
    }

    private static Set<String> intersectCodes(Set<String> current, Set<String> next) {
        if (next == null) {
            return current;
        }
        if (next.isEmpty()) {
            return Set.of();
        }
        if (current == null) {
            return new HashSet<>(next);
        }
        HashSet<String> h = new HashSet<>(current);
        h.retainAll(next);
        return h;
    }

    private Set<String> codesFromCompField(String field, String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String trimmed = q.trim();
        Set<String> set = new HashSet<>();
        String f = field == null ? "" : field.trim().toUpperCase(Locale.ROOT);
        if (f.isEmpty() || "BOTH".equals(f)) {
            orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
            orgUnitRepository.findByOrgLevelAndCodeContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else if ("NM".equals(f)) {
            orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else if ("CODE".equals(f)) {
            orgUnitRepository.findByOrgLevelAndCodeContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else {
            return null;
        }
        return set.isEmpty() ? Set.of() : set;
    }

    private Set<String> codesFromRegNo(String regNo) {
        if (regNo == null || regNo.isBlank()) {
            return null;
        }
        List<Long> ids = merchantProfileRepository.findOrgUnitIdsByRegNoContainingIgnoreCase(regNo.trim());
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        for (Long id : ids) {
            orgUnitRepository.findById(id).ifPresent(o -> codes.add(o.getCode()));
        }
        return codes.isEmpty() ? Set.of() : codes;
    }

    private Set<String> codesFromCalcCycle(String cycle) {
        if (cycle == null || cycle.isBlank()) {
            return null;
        }
        List<SettlementSetting> list = settlementSettingRepository.findByCalcCycle(cycle.trim());
        if (list.isEmpty()) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        for (SettlementSetting ss : list) {
            orgUnitRepository.findById(ss.getOrgUnitId()).ifPresent(o -> codes.add(o.getCode()));
        }
        return codes.isEmpty() ? Set.of() : codes;
    }

    private Set<String> codesFromMid(String mid) {
        if (mid == null || mid.isBlank()) {
            return null;
        }
        List<String> list = merchantPgBindingRepository.findMerchantCodesByMidContaining(mid.trim());
        if (list.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(list);
    }

    private void addPayDivPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                    jakarta.persistence.criteria.CriteriaBuilder cb, String cd) {
        if (cd == null || cd.isBlank()) {
            return;
        }
        String v = cd.trim();
        if ("FAIL".equalsIgnoreCase(v)) {
            parts.add(root.get("status").in("F0", "99"));
            return;
        }
        parts.add(cb.equal(root.get("status"), v));
    }

    private void addPayProcPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String cd) {
        if (cd == null || cd.isBlank()) {
            return;
        }
        switch (cd.trim()) {
            case "10" -> parts.add(cb.and(
                    unsettled(root, cb),
                    cb.equal(root.get("status"), "10")
            ));
            case "20" -> parts.add(cb.equal(cb.upper(root.get("settledYn")), "Y"));
            case "30" -> parts.add(cb.equal(root.get("status"), "20"));
            case "40" -> parts.add(cb.and(
                    unsettled(root, cb),
                    root.get("status").in("30", "31", "40", "41", "42")
            ));
            default -> { }
        }
    }

    private static Predicate unsettled(jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
        return cb.or(
                cb.isNull(root.get("settledYn")),
                cb.notEqual(cb.upper(root.get("settledYn")), "Y")
        );
    }

    private void addTranFactorColumnPredicates(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               String factorRaw, String valueRaw) {
        if (valueRaw == null || valueRaw.isBlank() || factorRaw == null || factorRaw.isBlank()) {
            return;
        }
        String factor = factorRaw.trim().toUpperCase(Locale.ROOT);
        if ("MERCHANT".equals(factor) || "MID".equals(factor)) {
            return;
        }
        String v = valueRaw.trim();
        switch (factor) {
            case "ORDER_NO" -> addLike(parts, root, cb, "orderNo", v);
            case "CUSTOMER_ID" -> addLike(parts, root, cb, "customerId", v);
            case "TRN_ID" -> addLike(parts, root, cb, "trnId", v);
            case "ROUTE" -> addLike(parts, root, cb, "routeNo", v);
            case "AMT" -> {
                try {
                    String digits = v.replace(",", "").trim();
                    BigDecimal amt = new BigDecimal(digits);
                    parts.add(cb.equal(root.get("amtKrw"), amt));
                } catch (Exception ignored) {
                    parts.add(cb.disjunction());
                }
            }
            default -> { }
        }
    }

    private void addPgAgencyPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                      jakarta.persistence.criteria.CriteriaBuilder cb, String pgCdRaw) {
        if (pgCdRaw == null || pgCdRaw.isBlank()) {
            return;
        }
        String pgCd = pgCdRaw.trim();
        List<String> byBinding = merchantPgBindingRepository.findMerchantCodesByPgCd(pgCd);
        List<Predicate> ors = new ArrayList<>();
        ors.add(cb.equal(root.get("van"), pgCd));
        if (!byBinding.isEmpty()) {
            ors.add(root.get("merchantId").in(byBinding));
        }
        parts.add(cb.or(ors.toArray(Predicate[]::new)));
    }

    private void addKeywordPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                     jakarta.persistence.criteria.CriteriaBuilder cb, String kw) {
        if (kw == null || kw.isBlank()) {
            return;
        }
        String pat = "%" + kw.trim().toLowerCase(Locale.ROOT) + "%";
        List<Predicate> ors = new ArrayList<>();
        orFieldLike(ors, root, cb, "orderNo", pat);
        orFieldLike(ors, root, cb, "trnId", pat);
        orFieldLike(ors, root, cb, "customerId", pat);
        orFieldLike(ors, root, cb, "chillTransactionId", pat);
        orFieldLike(ors, root, cb, "payNo", pat);
        if (!ors.isEmpty()) {
            parts.add(cb.or(ors.toArray(Predicate[]::new)));
        }
    }

    private static void orFieldLike(List<Predicate> ors, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                    jakarta.persistence.criteria.CriteriaBuilder cb, String field, String pattern) {
        jakarta.persistence.criteria.Path<String> path = root.get(field);
        ors.add(cb.and(cb.isNotNull(path), cb.like(cb.lower(path), pattern)));
    }

    private static void addLike(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                jakarta.persistence.criteria.CriteriaBuilder cb, String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String pat = "%" + raw.trim().toLowerCase(Locale.ROOT) + "%";
        jakarta.persistence.criteria.Path<String> path = root.get(field);
        parts.add(cb.and(cb.isNotNull(path), cb.like(cb.lower(path), pat)));
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
            case "CHATBOT_PAY" -> cb.equal(root.get("origin"), "CHATBOT");
            case "NOTI" -> cb.equal(root.get("origin"), "NOTI");
            default -> cb.conjunction();
        };
    }
}
