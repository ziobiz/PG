package com.pg.util;

import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;

import java.util.List;
import java.util.Optional;

/**
 * 동일 주문번호·승인번호에 대한 기존 {@link PgTrnsctn} 조회 — URL/API 행 우선, NOTI guest 중복 방지.
 */
public final class PgTrnsctnOrderLookup {

    private PgTrnsctnOrderLookup() {
    }

    public static Optional<PgTrnsctn> findByMerchantChillTxnOrOrder(PgTrnsctnRepository repo,
                                                                    String merchantId,
                                                                    String chillTxnId,
                                                                    String orderNo) {
        if (repo == null || merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        String mid = merchantId.trim();
        if (chillTxnId != null && !chillTxnId.isBlank()) {
            String tid = chillTxnId.trim();
            Optional<PgTrnsctn> byChill = repo.findFirstByChillTransactionIdAndMerchantId(tid, mid);
            if (byChill.isPresent()) {
                return byChill;
            }
            Optional<PgTrnsctn> byChillGlobal = repo.findFirstByChillTransactionIdOrderByCreatedAtDesc(tid);
            if (byChillGlobal.isPresent()) {
                return byChillGlobal;
            }
        }
        if (orderNo != null && !orderNo.isBlank()) {
            return findPreferredByMerchantAndOrder(repo, mid, orderNo.trim());
        }
        return Optional.empty();
    }

    public static Optional<PgTrnsctn> findPreferredByMerchantAndOrder(PgTrnsctnRepository repo,
                                                                      String merchantId,
                                                                      String orderNo) {
        if (repo == null || merchantId == null || orderNo == null
                || merchantId.isBlank() || orderNo.isBlank()) {
            return Optional.empty();
        }
        List<PgTrnsctn> rows = repo.findByMerchantIdAndOrderNoOrderByCreatedAtAsc(
                merchantId.trim(), orderNo.trim());
        return PaidApprovalEvidenceGuard.pickPreferredOrderRowFromList(rows);
    }
}
