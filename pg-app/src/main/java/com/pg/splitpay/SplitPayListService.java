package com.pg.splitpay;

import com.pg.api.dto.PageResult;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SplitPayListService {

    private final SplitPayContractRepository contractRepository;
    private final SplitPayInstallmentRepository installmentRepository;

    public SplitPayListService(SplitPayContractRepository contractRepository,
                               SplitPayInstallmentRepository installmentRepository) {
        this.contractRepository = contractRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page1, int size,
                                                 String compId, String contractNo, String status,
                                                 LocalDate fromDate, LocalDate toDate) {
        int pageIdx = Math.max(page1, 1) - 1;
        int sz = size <= 0 ? 50 : Math.min(size, 300);
        Specification<SplitPayContract> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (compId != null && !compId.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("merchantCode")), compId.trim().toUpperCase(Locale.ROOT)));
            }
            if (contractNo != null && !contractNo.isBlank()) {
                ps.add(cb.like(cb.upper(root.get("contractNo")), "%" + contractNo.trim().toUpperCase(Locale.ROOT) + "%"));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
            }
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(fromDate, LocalTime.MIN)));
            }
            if (toDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(toDate, LocalTime.MAX)));
            }
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(Predicate[]::new));
        };
        Page<SplitPayContract> p = contractRepository.findAll(spec,
                PageRequest.of(pageIdx, sz, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(p, this::toRow);
    }

    private Map<String, Object> toRow(SplitPayContract c) {
        List<SplitPayInstallment> inst = installmentRepository.findByContractIdOrderByInstallmentNoAsc(c.getId());
        long paid = inst.stream().filter(i -> SplitPayInstallment.STATUS_PAID.equals(i.getStatus())).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNo", c.getContractNo());
        m.put("compId", c.getMerchantCode());
        m.put("customerEmail", c.getCustomerEmail());
        m.put("customerName", c.getCustomerName() != null ? c.getCustomerName() : "");
        m.put("totalAmount", c.getTotalAmount());
        m.put("currencyCode", c.getCurrencyCode());
        m.put("installmentCount", c.getInstallmentCount());
        m.put("paidCount", paid);
        m.put("intervalType", c.getIntervalType());
        m.put("intervalValue", c.getIntervalValue());
        m.put("status", c.getStatus());
        m.put("contractDate", c.getContractDate() != null ? c.getContractDate().toString() : "");
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString().replace('T', ' ') : "");
        return m;
    }
}
