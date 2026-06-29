package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PaidApprovalEvidenceGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 동일 주문번호에 NOTI·guest 유령 행과 URL/API 실거래 행이 공존할 때 유령 행을 제거합니다.
 */
@Service
public class PgTrnsctnOrderDedupeService {

    private static final Logger log = LoggerFactory.getLogger(PgTrnsctnOrderDedupeService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;

    public PgTrnsctnOrderDedupeService(PgTrnsctnRepository pgTrnsctnRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    @Transactional
    public void purgeGuestNotiDuplicatesIfCanonicalExists(String merchantId, String orderNo) {
        if (merchantId == null || orderNo == null) {
            return;
        }
        String mid = merchantId.trim();
        String on = orderNo.trim();
        if (mid.isEmpty() || on.isEmpty()) {
            return;
        }
        List<PgTrnsctn> all = pgTrnsctnRepository.findByMerchantIdAndOrderNoOrderByCreatedAtAsc(mid, on);
        if (all.size() <= 1) {
            return;
        }
        for (PgTrnsctn candidate : all) {
            if (!PaidApprovalEvidenceGuard.isGuestNotiRow(candidate)) {
                continue;
            }
            boolean hasCanonicalSibling = all.stream()
                    .anyMatch(other -> other != null
                            && !candidate.getTrnId().equals(other.getTrnId())
                            && !PaidApprovalEvidenceGuard.isGuestNotiRow(other));
            if (!hasCanonicalSibling) {
                continue;
            }
            pgTrnsctnRepository.delete(candidate);
            log.info("중복 NOTI·guest 거래 삭제 trnId={} merchantId={} orderNo={}", candidate.getTrnId(), mid, on);
        }
    }
}
