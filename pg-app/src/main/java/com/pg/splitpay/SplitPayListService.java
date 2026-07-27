package com.pg.splitpay;

import com.pg.api.dto.PageResult;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import jakarta.persistence.criteria.Predicate;
import com.pg.service.OrgAccessService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.util.ViewDisplayTimezoneResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SplitPayListService {

    private final SplitPayContractRepository contractRepository;
    private final SplitPayInstallmentRepository installmentRepository;
    private final OrgAccessService orgAccessService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final SplitPayContractCancelPermissionService cancelPermissionService;

    public SplitPayListService(SplitPayContractRepository contractRepository,
                               SplitPayInstallmentRepository installmentRepository,
                               OrgAccessService orgAccessService,
                               HqLedgerSysSettingsService hqLedgerSysSettingsService,
                               SplitPayContractCancelPermissionService cancelPermissionService) {
        this.contractRepository = contractRepository;
        this.installmentRepository = installmentRepository;
        this.orgAccessService = orgAccessService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.cancelPermissionService = cancelPermissionService;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page1, int size,
                                                 String compId, String contractNo, String status,
                                                 LocalDate fromDate, LocalDate toDate,
                                                 Authentication authentication) {
        Set<String> visible = orgAccessService.visibleMerchantCompCodes(authentication);
        if (visible != null && visible.isEmpty()) {
            return PageResult.empty(page1, size);
        }
        int pageIdx = Math.max(page1, 1) - 1;
        int sz = size <= 0 ? 50 : Math.min(size, 300);
        Specification<SplitPayContract> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (visible != null) {
                Set<String> upper = visible.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toSet());
                if (!upper.isEmpty()) {
                    ps.add(cb.upper(root.get("merchantCode")).in(upper));
                }
            }
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
        return PageResult.of(p, c -> toRow(c, authentication));
    }

    private Map<String, Object> toRow(SplitPayContract c, Authentication authentication) {
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
        m.put("createdAt", formatSplitPayDateTime(c.getCreatedAt()));
        m.put("cancelledAt", formatSplitPayDateTime(c.getCancelledAt()));
        m.put("cancelledBy", c.getCancelledBy() != null ? c.getCancelledBy() : "");
        m.put("cancelReason", c.getCancelReason() != null ? c.getCancelReason() : "");
        boolean canCancel = SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())
                && cancelPermissionService.canCancelContract(c.getMerchantCode(), authentication);
        m.put("canCancelContract", canCancel);
        return m;
    }

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String formatSplitPayDateTime(LocalDateTime naive) {
        if (naive == null) {
            return "";
        }
        ZoneId standard = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        Optional<ZoneId> view = ViewDisplayTimezoneResolver.currentRequestOverride();
        if (view.isEmpty()) {
            return naive.format(TS_FMT);
        }
        return ViewDisplayTimezoneResolver.formatNaiveAsWallDateTime(naive, standard, view);
    }
}
