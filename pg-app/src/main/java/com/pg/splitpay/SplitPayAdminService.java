package com.pg.splitpay;

import com.pg.api.dto.PageResult;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import com.pg.service.OrgAccessService;
import com.pg.service.PublicCustomerSiteBaseService;
import com.pg.util.TxnOutcomeReasonApplier;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SplitPayAdminService {

    public static final String OUTCOME_CODE_SPLIT_CONTRACT_CANCEL = "SPLIT_CONTRACT_CANCEL";

    private final SplitPayInstallmentRepository installmentRepository;
    private final SplitPayContractRepository contractRepository;
    private final OrgAccessService orgAccessService;
    private final SplitPayMailService mailService;
    private final PublicCustomerSiteBaseService publicCustomerSiteBaseService;
    private final SplitPayContractCancelPermissionService cancelPermissionService;
    private final PgTrnsctnRepository pgTrnsctnRepository;

    public SplitPayAdminService(SplitPayInstallmentRepository installmentRepository,
                                SplitPayContractRepository contractRepository,
                                OrgAccessService orgAccessService,
                                SplitPayMailService mailService,
                                PublicCustomerSiteBaseService publicCustomerSiteBaseService,
                                SplitPayContractCancelPermissionService cancelPermissionService,
                                PgTrnsctnRepository pgTrnsctnRepository) {
        this.installmentRepository = installmentRepository;
        this.contractRepository = contractRepository;
        this.orgAccessService = orgAccessService;
        this.mailService = mailService;
        this.publicCustomerSiteBaseService = publicCustomerSiteBaseService;
        this.cancelPermissionService = cancelPermissionService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> searchProgress(int page1, int size,
                                                         String compId, String contractNo, String status,
                                                         LocalDate fromDate, LocalDate toDate,
                                                         Authentication authentication) {
        return searchInstallments(page1, size, compId, contractNo, status, fromDate, toDate, authentication, false);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> searchMail(int page1, int size,
                                                      String compId, String contractNo, String status,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      Authentication authentication) {
        return searchInstallments(page1, size, compId, contractNo, status, fromDate, toDate, authentication, true);
    }

    private PageResult<Map<String, Object>> searchInstallments(int page1, int size,
                                                               String compId, String contractNo, String status,
                                                               LocalDate fromDate, LocalDate toDate,
                                                               Authentication authentication, boolean mailView) {
        Set<String> visible = orgAccessService.visibleMerchantCompCodes(authentication);
        if (visible != null && visible.isEmpty()) {
            return PageResult.empty(page1, size);
        }
        int pageIdx = Math.max(page1, 1) - 1;
        int sz = size <= 0 ? 50 : Math.min(size, 300);
        Specification<SplitPayInstallment> spec = buildInstallmentSpec(visible, compId, contractNo, status, fromDate, toDate);
        Page<SplitPayInstallment> page = installmentRepository.findAll(spec,
                PageRequest.of(pageIdx, sz, Sort.by(Sort.Direction.DESC, "dueDateAdjusted", "installmentNo")));
        Map<Long, SplitPayContract> contracts = loadContracts(page.getContent());
        Map<Long, Long> paidCounts = loadPaidCounts(contracts.keySet());
        Function<SplitPayInstallment, Map<String, Object>> mapper = inst -> {
            SplitPayContract c = contracts.get(inst.getContractId());
            Map<String, Object> row = mailView ? toMailRow(inst, c, paidCounts) : toProgressRow(inst, c, paidCounts);
            boolean canCancel = c != null
                    && SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())
                    && cancelPermissionService.canCancelContract(c.getMerchantCode(), authentication);
            row.put("canCancelContract", canCancel);
            return row;
        };
        return PageResult.of(page, mapper);
    }

    @Transactional
    public Map<String, Object> cancelContract(String contractNo, String reason, Authentication authentication) {
        if (contractNo == null || contractNo.isBlank()) {
            throw new IllegalArgumentException("계약번호가 필요합니다.");
        }
        SplitPayContract c = contractRepository.findByContractNo(contractNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        assertMerchantAccessible(c.getMerchantCode(), authentication);
        if (!cancelPermissionService.canCancelContract(c.getMerchantCode(), authentication)) {
            throw new IllegalStateException("분할 계약 취소 권한이 없습니다.");
        }
        if (!SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())) {
            throw new IllegalStateException("진행중(ACTIVE) 계약만 취소할 수 있습니다.");
        }
        List<SplitPayInstallment> all = installmentRepository.findByContractIdOrderByInstallmentNoAsc(c.getId());
        List<SplitPayInstallment> paid = all.stream()
                .filter(i -> SplitPayInstallment.STATUS_PAID.equals(i.getStatus()))
                .collect(Collectors.toList());
        List<SplitPayInstallment> pending = all.stream()
                .filter(i -> SplitPayInstallment.STATUS_PENDING.equals(i.getStatus()))
                .collect(Collectors.toList());

        String actor = cancelPermissionService.actorUsername(authentication);
        String reasonTrim = reason != null ? reason.trim() : "";
        if (reasonTrim.length() > 500) {
            reasonTrim = reasonTrim.substring(0, 500);
        }
        String note = buildPaidOutcomeNote(c, paid);
        for (SplitPayInstallment inst : paid) {
            annotatePaidTxn(inst, note);
        }
        for (SplitPayInstallment inst : pending) {
            inst.setStatus(SplitPayInstallment.STATUS_CANCELLED);
            installmentRepository.save(inst);
        }
        c.setStatus(SplitPayContract.STATUS_CANCELLED);
        c.setCancelledAt(LocalDateTime.now());
        c.setCancelledBy(actor);
        c.setCancelReason(reasonTrim.isEmpty() ? null : reasonTrim);
        contractRepository.save(c);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractNo", c.getContractNo());
        out.put("status", c.getStatus());
        out.put("paidCount", paid.size());
        out.put("cancelledPendingCount", pending.size());
        out.put("cancelledAt", fmtTs(c.getCancelledAt()));
        out.put("cancelledBy", actor);
        return out;
    }

    private void annotatePaidTxn(SplitPayInstallment inst, String note) {
        PgTrnsctn t = null;
        if (inst.getPgTrnId() != null && !inst.getPgTrnId().isBlank()) {
            t = pgTrnsctnRepository.findById(inst.getPgTrnId().trim()).orElse(null);
        }
        if (t == null && inst.getOrderNo() != null && !inst.getOrderNo().isBlank()) {
            t = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(inst.getOrderNo().trim()).orElse(null);
        }
        if (t != null) {
            TxnOutcomeReasonApplier.annotateOperationalNote(t, note, OUTCOME_CODE_SPLIT_CONTRACT_CANCEL,
                    TxnOutcomeReasonApplier.SOURCE_ICOPAY);
            pgTrnsctnRepository.save(t);
        }
    }

    static String buildPaidOutcomeNote(SplitPayContract c, List<SplitPayInstallment> paid) {
        String cur = c.getCurrencyCode() != null ? c.getCurrencyCode() : "";
        String total = fmtAmt(c.getTotalAmount());
        if (paid == null || paid.isEmpty()) {
            return "분할계약취소: 총액 " + total + " " + cur
                    + " · 기납부 없음 · 잔여 회차 취소 (계약 " + c.getContractNo() + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("분할계약취소: 총액 ").append(total).append(' ').append(cur).append(" 중 ");
        for (int i = 0; i < paid.size(); i++) {
            SplitPayInstallment p = paid.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(p.getInstallmentNo()).append("회차 성공 ").append(fmtAmt(p.getAmount())).append(' ').append(cur);
        }
        sb.append(" 인정 · 잔여 회차 취소 (계약 ").append(c.getContractNo()).append(')');
        return sb.toString();
    }

    private static String fmtAmt(BigDecimal amt) {
        if (amt == null) {
            return "0";
        }
        return amt.stripTrailingZeros().toPlainString();
    }

    @Transactional
    public Map<String, Object> resendInstallmentMail(long installmentId, String phase, Authentication authentication) {
        SplitPayInstallment inst = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
        SplitPayContract contract = contractRepository.findById(inst.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        assertMerchantAccessible(contract.getMerchantCode(), authentication);
        if (!SplitPayInstallment.STATUS_PENDING.equals(inst.getStatus())) {
            throw new IllegalStateException("미납(PENDING) 회차만 메일을 재발송할 수 있습니다.");
        }
        String ph = normalizePhase(phase);
        String siteBase = publicCustomerSiteBaseService.resolvePublicCustomerSiteBase(null);
        mailService.sendInstallmentLink(contract, inst, siteBase, ph);
        LocalDateTime now = LocalDateTime.now();
        switch (ph) {
            case "D_MINUS1", "DM1" -> inst.setMailDMinus1Sent(now);
            case "D1" -> inst.setMailD1Sent(now);
            case "D2" -> inst.setMailD2Sent(now);
            case "D3" -> inst.setMailD3Sent(now);
            case "CREATE" -> { /* no dedicated column */ }
            default -> inst.setMailD0Sent(now);
        }
        installmentRepository.save(inst);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("installmentId", inst.getId());
        out.put("phase", ph);
        out.put("resentAt", now.toString().replace('T', ' '));
        return out;
    }

    private void assertMerchantAccessible(String merchantCode, Authentication authentication) {
        Set<String> visible = orgAccessService.visibleMerchantCompCodes(authentication);
        if (visible == null) {
            return;
        }
        String mc = merchantCode != null ? merchantCode.trim().toUpperCase(Locale.ROOT) : "";
        boolean hit = false;
        for (String v : visible) {
            if (v != null && v.trim().toUpperCase(Locale.ROOT).equals(mc)) {
                hit = true;
                break;
            }
        }
        if (mc.isEmpty() || !hit) {
            throw new IllegalArgumentException("조회 권한이 없는 가맹점입니다.");
        }
    }

    private static String normalizePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return "D0";
        }
        return phase.trim().toUpperCase(Locale.ROOT);
    }

    private Specification<SplitPayInstallment> buildInstallmentSpec(Set<String> visibleCodes,
                                                                    String compId, String contractNo, String status,
                                                                    LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Subquery<Long> contractFilter = query.subquery(Long.class);
            Root<SplitPayContract> croot = contractFilter.from(SplitPayContract.class);
            contractFilter.select(croot.get("id"));
            List<Predicate> cps = new ArrayList<>();
            if (visibleCodes != null) {
                Set<String> upper = visibleCodes.stream()
                        .filter(Objects::nonNull)
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                if (!upper.isEmpty()) {
                    cps.add(cb.upper(croot.get("merchantCode")).in(upper));
                }
            }
            if (compId != null && !compId.isBlank()) {
                cps.add(cb.equal(cb.upper(croot.get("merchantCode")), compId.trim().toUpperCase(Locale.ROOT)));
            }
            if (contractNo != null && !contractNo.isBlank()) {
                cps.add(cb.like(cb.upper(croot.get("contractNo")),
                        "%" + contractNo.trim().toUpperCase(Locale.ROOT) + "%"));
            }
            if (!cps.isEmpty()) {
                contractFilter.where(cb.and(cps.toArray(Predicate[]::new)));
                ps.add(root.get("contractId").in(contractFilter));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
            }
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("dueDateAdjusted"), fromDate));
            }
            if (toDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("dueDateAdjusted"), toDate));
            }
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private Map<Long, SplitPayContract> loadContracts(List<SplitPayInstallment> list) {
        Set<Long> ids = list.stream().map(SplitPayInstallment::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return contractRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SplitPayContract::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, Long> loadPaidCounts(Set<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> out = new LinkedHashMap<>();
        for (Long cid : contractIds) {
            long paid = installmentRepository.findByContractIdOrderByInstallmentNoAsc(cid).stream()
                    .filter(i -> SplitPayInstallment.STATUS_PAID.equals(i.getStatus()))
                    .count();
            out.put(cid, paid);
        }
        return out;
    }

    private Map<String, Object> toProgressRow(SplitPayInstallment inst, SplitPayContract c, Map<Long, Long> paidCounts) {
        return baseRow(inst, c, paidCounts);
    }

    private Map<String, Object> toMailRow(SplitPayInstallment inst, SplitPayContract c, Map<Long, Long> paidCounts) {
        Map<String, Object> m = baseRow(inst, c, paidCounts);
        m.put("mailDMinus1Sent", fmtTs(inst.getMailDMinus1Sent()));
        m.put("mailD0Sent", fmtTs(inst.getMailD0Sent()));
        m.put("mailD1Sent", fmtTs(inst.getMailD1Sent()));
        m.put("mailD2Sent", fmtTs(inst.getMailD2Sent()));
        m.put("mailD3Sent", fmtTs(inst.getMailD3Sent()));
        return m;
    }

    private Map<String, Object> baseRow(SplitPayInstallment inst, SplitPayContract c, Map<Long, Long> paidCounts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("installmentId", inst.getId());
        m.put("contractNo", c != null ? c.getContractNo() : "");
        m.put("compId", c != null ? c.getMerchantCode() : "");
        m.put("customerEmail", c != null ? c.getCustomerEmail() : "");
        m.put("customerName", c != null && c.getCustomerName() != null ? c.getCustomerName() : "");
        m.put("contractStatus", c != null ? c.getStatus() : "");
        m.put("installmentNo", inst.getInstallmentNo());
        int total = c != null && c.getInstallmentCount() != null ? c.getInstallmentCount() : 0;
        long paid = c != null ? paidCounts.getOrDefault(c.getId(), 0L) : 0L;
        m.put("installmentCount", total);
        m.put("paidCount", paid);
        m.put("progressPct", total > 0 ? (int) Math.round(paid * 100.0 / total) : 0);
        m.put("amount", inst.getAmount());
        m.put("currencyCode", c != null ? c.getCurrencyCode() : "");
        m.put("dueDate", inst.getDueDateAdjusted() != null ? inst.getDueDateAdjusted().toString() : "");
        m.put("status", inst.getStatus());
        String paidAt = fmtTs(inst.getPaidAt());
        String cancelledAt = c != null ? fmtTs(c.getCancelledAt()) : "";
        String cancelReason = c != null && c.getCancelReason() != null ? c.getCancelReason() : "";
        m.put("paidAt", paidAt);
        m.put("cancelledAt", cancelledAt);
        m.put("cancelReason", cancelReason);
        /* 이벤트일시: 납부완료→납부일시, 취소→계약취소일시 */
        String eventAt = "";
        String eventAtKind = "";
        if (SplitPayInstallment.STATUS_PAID.equals(inst.getStatus())) {
            eventAt = paidAt;
            eventAtKind = "PAID";
        } else if (SplitPayInstallment.STATUS_CANCELLED.equals(inst.getStatus())) {
            eventAt = cancelledAt;
            eventAtKind = "CANCELLED";
        }
        m.put("eventAt", eventAt);
        m.put("eventAtKind", eventAtKind);
        m.put("orderNo", inst.getOrderNo());
        m.put("pgTrnId", inst.getPgTrnId() != null ? inst.getPgTrnId() : "");
        return m;
    }

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 목록 표시용 — 소수초(나노) 없이 yyyy-MM-dd HH:mm:ss */
    private static String fmtTs(LocalDateTime dt) {
        return dt != null ? dt.format(TS_FMT) : "";
    }
}
