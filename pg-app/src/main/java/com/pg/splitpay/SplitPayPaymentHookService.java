package com.pg.splitpay;

import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SplitPayPaymentHookService {

    private static final Logger log = LoggerFactory.getLogger(SplitPayPaymentHookService.class);
    private static final String STATUS_PAID = "10";

    private final SplitPayInstallmentRepository installmentRepository;
    private final SplitPayContractRepository contractRepository;
    private final SplitPayContractService splitPayContractService;

    public SplitPayPaymentHookService(SplitPayInstallmentRepository installmentRepository,
                                      SplitPayContractRepository contractRepository,
                                      SplitPayContractService splitPayContractService) {
        this.installmentRepository = installmentRepository;
        this.contractRepository = contractRepository;
        this.splitPayContractService = splitPayContractService;
    }

    @Transactional
    public void onTxnStatusChange(String orderNo, String txnStatus, String pgTrnId) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        installmentRepository.findByOrderNo(orderNo.trim()).ifPresent(inst -> {
            if (STATUS_PAID.equals(txnStatus)) {
                markPaid(inst, pgTrnId);
            } else if (isReversalStatus(txnStatus)) {
                splitPayContractService.stopContractOnFirstInstallmentReversal(orderNo);
            }
        });
    }

    private static boolean isReversalStatus(String txnStatus) {
        if (txnStatus == null || txnStatus.isBlank()) {
            return false;
        }
        return switch (txnStatus.trim()) {
            case "20", "21", "22", "30", "31" -> true;
            default -> false;
        };
    }

    private void markPaid(SplitPayInstallment inst, String pgTrnId) {
        if (SplitPayInstallment.STATUS_PAID.equals(inst.getStatus())) {
            return;
        }
        inst.setStatus(SplitPayInstallment.STATUS_PAID);
        inst.setPaidAt(LocalDateTime.now());
        if (pgTrnId != null && !pgTrnId.isBlank()) {
            inst.setPgTrnId(pgTrnId.length() > 32 ? pgTrnId.substring(0, 32) : pgTrnId.trim());
        }
        installmentRepository.save(inst);
        contractRepository.findById(inst.getContractId()).ifPresent(c -> {
            List<SplitPayInstallment> all = installmentRepository.findByContractIdOrderByInstallmentNoAsc(c.getId());
            boolean allPaid = all.stream().allMatch(i -> SplitPayInstallment.STATUS_PAID.equals(i.getStatus()));
            if (allPaid && SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())) {
                c.setStatus(SplitPayContract.STATUS_COMPLETED);
                contractRepository.save(c);
            }
        });
        log.info("분할결제 회차 결제완료 orderNo={} inst={}", inst.getOrderNo(), inst.getInstallmentNo());
    }
}
