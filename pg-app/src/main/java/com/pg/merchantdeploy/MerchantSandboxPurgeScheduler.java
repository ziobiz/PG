package com.pg.merchantdeploy;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantSandboxNotifyLogRepository;
import com.pg.repository.MerchantSandboxTxnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 샌드박스 결제·통보 이력 N일 보관 후 삭제. */
@Service
public class MerchantSandboxPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(MerchantSandboxPurgeScheduler.class);

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantSandboxTxnRepository sandboxTxnRepository;
    private final MerchantSandboxNotifyLogRepository notifyLogRepository;

    public MerchantSandboxPurgeScheduler(HqApiConfigRepository hqApiConfigRepository,
                                         MerchantSandboxTxnRepository sandboxTxnRepository,
                                         MerchantSandboxNotifyLogRepository notifyLogRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.sandboxTxnRepository = sandboxTxnRepository;
        this.notifyLogRepository = notifyLogRepository;
    }

    @Scheduled(cron = "${app.sandbox.purgeCron:0 30 4 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void purge() {
        int days = hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getSandboxRetainDays)
                .orElse(3);
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(1, days));
        long n1 = notifyLogRepository.deleteOlderThan(before);
        long n2 = sandboxTxnRepository.deleteOlderThan(before);
        if (n1 > 0 || n2 > 0) {
            log.info("sandbox purge: notify={}, txn={} (older than {} days)", n1, n2, days);
        }
    }
}
