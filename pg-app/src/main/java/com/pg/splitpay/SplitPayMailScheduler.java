package com.pg.splitpay;

import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import com.pg.service.PublicCustomerSiteBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SplitPayMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(SplitPayMailScheduler.class);

    private final SplitPayInstallmentRepository installmentRepository;
    private final SplitPayContractRepository contractRepository;
    private final SplitPayMailService mailService;
    private final PublicCustomerSiteBaseService publicCustomerSiteBaseService;

    public SplitPayMailScheduler(SplitPayInstallmentRepository installmentRepository,
                                 SplitPayContractRepository contractRepository,
                                 SplitPayMailService mailService,
                                 PublicCustomerSiteBaseService publicCustomerSiteBaseService) {
        this.installmentRepository = installmentRepository;
        this.contractRepository = contractRepository;
        this.mailService = mailService;
        this.publicCustomerSiteBaseService = publicCustomerSiteBaseService;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${app.splitPay.mailCron:0 10 8 * * *}", zone = "Asia/Seoul")
    public void runDailyMailJob() {
        LocalDate today = LocalDate.now();
        try {
            processDueMails(today);
        } catch (Exception e) {
            log.warn("분할결제 메일 스케줄 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public void processDueMails(LocalDate today) {
        LocalDate from = today.minusDays(1);
        LocalDate to = today.plusDays(3);
        List<SplitPayInstallment> pending = installmentRepository.findPendingDueBetween(from, to);
        String siteBase = publicCustomerSiteBaseService.resolvePublicCustomerSiteBase(null);
        for (SplitPayInstallment inst : pending) {
            if (!SplitPayInstallment.STATUS_PENDING.equals(inst.getStatus())) {
                continue;
            }
            SplitPayContract c = contractRepository.findById(inst.getContractId()).orElse(null);
            if (c == null || !SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())) {
                continue;
            }
            LocalDate due = inst.getDueDateAdjusted();
            if (due == null) {
                continue;
            }
            if (due.equals(today.plusDays(1)) && inst.getMailDMinus1Sent() == null) {
                mailService.sendInstallmentLink(c, inst, siteBase, "D_MINUS1");
                inst.setMailDMinus1Sent(LocalDateTime.now());
                installmentRepository.save(inst);
            } else if (due.equals(today) && inst.getMailD0Sent() == null) {
                mailService.sendInstallmentLink(c, inst, siteBase, "D0");
                inst.setMailD0Sent(LocalDateTime.now());
                installmentRepository.save(inst);
            } else if (due.plusDays(1).equals(today) && inst.getMailD1Sent() == null) {
                mailService.sendInstallmentLink(c, inst, siteBase, "D1");
                inst.setMailD1Sent(LocalDateTime.now());
                installmentRepository.save(inst);
            } else if (due.plusDays(2).equals(today) && inst.getMailD2Sent() == null) {
                mailService.sendInstallmentLink(c, inst, siteBase, "D2");
                inst.setMailD2Sent(LocalDateTime.now());
                installmentRepository.save(inst);
            } else if (due.plusDays(3).equals(today) && inst.getMailD3Sent() == null) {
                mailService.sendInstallmentLink(c, inst, siteBase, "D3");
                inst.setMailD3Sent(LocalDateTime.now());
                installmentRepository.save(inst);
            }
        }
    }
}
