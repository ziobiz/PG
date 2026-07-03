package com.pg.merchantdeploy;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.JpayTradeApiService;
import com.pg.util.JpayCheckoutMinAmountUtil;
import com.pg.util.JpayOrderDuplicateUtil;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PgNotifyInternalStatusMapper;
import com.pg.util.PgTrnsctnOrderLookup;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * JPAY 인라인 prepare — 최소금액·기존 시도(orderNo) 검증(A안: 새 orderNo 필요).
 */
@Service
public class JpayInlineCheckoutPrepareGuard {

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final JpayTradeApiService jpayTradeApiService;

    public JpayInlineCheckoutPrepareGuard(PgTrnsctnRepository pgTrnsctnRepository,
                                          OrgUnitRepository orgUnitRepository,
                                          JpayTradeApiService jpayTradeApiService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.jpayTradeApiService = jpayTradeApiService;
    }

    /**
     * @return empty if OK, else fail map (success=false, errorCode, messages, …)
     */
    public Optional<Map<String, Object>> validatePrepare(Long orgUnitId, String orderNo,
                                                         BigDecimal amount, String currency) {
        Optional<Map<String, Object>> minDeny = JpayCheckoutMinAmountUtil.validate(amount, currency);
        if (minDeny.isPresent()) {
            return minDeny;
        }
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return Optional.empty();
        }
        String mid = ou.get().getCode();
        Optional<PgTrnsctn> local = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, mid, orderNo.trim());
        if (local.isPresent() && PgVendor.isJpayFamily(local.get().getVan())) {
            Optional<Map<String, Object>> deny = denyFromLocalTxn(local.get(), orderNo.trim());
            if (deny.isPresent()) {
                return deny;
            }
        }
        Optional<JpayTradeApiService.TradeQuerySnapshot> snap =
                jpayTradeApiService.tryQueryTradeForOrgUnit(orgUnitId, orderNo.trim());
        if (snap.isEmpty()) {
            return Optional.empty();
        }
        return denyFromTradeSnapshot(snap.get(), orderNo.trim());
    }

    private static Optional<Map<String, Object>> denyFromLocalTxn(PgTrnsctn t, String orderNo) {
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        if ("10".equals(st) || "00".equals(st)) {
            return Optional.empty();
        }
        if ("08".equals(st)) {
            return Optional.of(JpayOrderDuplicateUtil.orderPendingFailPayload(orderNo));
        }
        if (NotifyToTxnStatusMerge.isTerminalOutcome(st)) {
            return Optional.of(JpayOrderDuplicateUtil.orderAlreadyAttemptedFailPayload(orderNo));
        }
        return Optional.empty();
    }

    private static Optional<Map<String, Object>> denyFromTradeSnapshot(
            JpayTradeApiService.TradeQuerySnapshot snap, String orderNo) {
        String mapped = snap.mappedInternalStatus();
        if (PgNotifyInternalStatusMapper.ST_PAID.equals(mapped)) {
            return Optional.empty();
        }
        if (PgNotifyInternalStatusMapper.ST_FAIL.equals(mapped)) {
            return Optional.of(JpayOrderDuplicateUtil.orderAlreadyAttemptedFailPayload(orderNo));
        }
        String ts = snap.tradeState() != null ? snap.tradeState().trim().toUpperCase() : "";
        if ("UNPAID".equals(ts) || PgNotifyInternalStatusMapper.ST_CANCEL.equals(mapped)) {
            return Optional.of(JpayOrderDuplicateUtil.orderPendingFailPayload(orderNo));
        }
        return Optional.empty();
    }
}
