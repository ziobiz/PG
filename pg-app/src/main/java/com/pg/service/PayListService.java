package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PayListService {

    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public PayListService(PgTrnsctnRepository trnsctnRepository, OrgUnitRepository orgUnitRepository) {
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public PageResult<Map<String, Object>> search(String merchantId, LocalDate fromDt, LocalDate toDt,
                                                  int page, int size, String payListVariant) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PgTrnsctn> spec = buildSpecification(merchantId, from, to, payListVariant);
        Page<PgTrnsctn> result = trnsctnRepository.findAll(spec, p);
        List<String> merchantIds = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        Map<String, String> compNames = orgUnitRepository.findAll().stream()
                .filter(o -> merchantIds.contains(o.getCode()))
                .collect(Collectors.toMap(OrgUnit::getCode, OrgUnit::getName, (a, b) -> a));
        List<Map<String, Object>> list = result.getContent().stream()
                .map(t -> PayListItemDto.from(t, compNames.get(t.getMerchantId())))
                .collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        return pr;
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

    /**
     * 결제관리 화면별 필터 — docs/결제관리_기획_NOTI참고.md 참고.
     * OFFSET_CANCEL: 승인성공(10) 제외 전 건(상계·정산 판단용). NULL status 포함.
     * URL_PAY: 플랫폼 URL직접결제 출처만, 상태는 전체.
     */
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
