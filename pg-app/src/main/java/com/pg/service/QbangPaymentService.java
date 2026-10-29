package com.pg.service;

import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.qbang.QbangApiClient;
import com.pg.integration.pg.qbang.QbangCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.MerchantPgBindingRouterService.RoutingHint;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.QrWalletScopeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** QBANG(큐뱅) QR — TK→QR→PL→OKAY. */
@Service
public class QbangPaymentService {

    private static final Logger log = LoggerFactory.getLogger(QbangPaymentService.class);

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantPgBindingRouterService pgBindingRouter;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final QbangApiClient apiClient;
    private final QbangSaleRecordService qbangSaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final QbangNotiMirrorService qbangNotiMirrorService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public QbangPaymentService(OrgUnitRepository orgUnitRepository,
                               PgAgencyRepository pgAgencyRepository,
                               PgTrnsctnRepository pgTrnsctnRepository,
                               MerchantPgBindingRouterService pgBindingRouter,
                               UrlPayChargeResolutionService urlPayChargeResolutionService,
                               QbangApiClient apiClient,
                               QbangSaleRecordService qbangSaleRecordService,
                               MerchantOutboundNotifyService merchantOutboundNotifyService,
                               HqNotifyEnvService hqNotifyEnvService,
                               QbangNotiMirrorService qbangNotiMirrorService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgBindingRouter = pgBindingRouter;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.qbangSaleRecordService = qbangSaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.qbangNotiMirrorService = qbangNotiMirrorService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.QBANG);
        m.put("notiWebhookPath", "/noti/qbang");
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.QBANG);
        return m;
    }

    public Optional<ResolvedQbang> resolveBinding(Long orgUnitId, String currency) {
        Optional<MerchantPgBinding> b = pgBindingRouter.resolveOperationalBinding(
                orgUnitId, RoutingHint.qr(QrWalletScopeUtil.QBANG, currency));
        if (b.isEmpty()) {
            return Optional.empty();
        }
        String pgCd = b.get().getPgCd() != null ? b.get().getPgCd().trim() : "";
        if (!PgVendor.isQbangFamily(pgCd)) {
            return Optional.empty();
        }
        Optional<PgAgency> agency = pgAgencyRepository.findByPgCd(pgCd);
        if (agency.isEmpty()) {
            return Optional.empty();
        }
        QbangCredentials creds = QbangCredentials.fromAgencyAndBinding(agency.get(), b.get());
        if (!creds.isUsable()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedQbang(pgCd, b.get(), creds));
    }

    public Map<String, Object> startQr(Long orgUnitId, Map<String, Object> body,
                                      HttpServletRequest request, String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        String currencyHint = str(body, "currency", "displayCurrency");
        Optional<ResolvedQbang> resolved = resolveBinding(orgUnitId, currencyHint);
        if (resolved.isEmpty()) {
            return fail(out, "QBANG_NOT_CONFIGURED",
                    "QBANG QR 운영 바인딩(posId)이 없습니다. 가맹 결제대행사에 QBANG·QR을 등록하세요.");
        }
        String opPg = resolved.get().pgCd();
        QbangCredentials creds = resolved.get().creds();

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
        } catch (IllegalArgumentException e) {
            return fail(out, "INVALID_AMOUNT", e.getMessage());
        }
        String orderNo = str(body, "orderNo", "order_no", "reference");
        if (orderNo.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }

        Map<String, Object> tk = apiClient.requestToken(creds);
        if (!Boolean.TRUE.equals(tk.get("ok"))) {
            return fail(out, strDef(tk.get("errorCode"), "QBANG_TK"),
                    strDef(tk.get("message"), "QBANG Token 요청에 실패했습니다."));
        }
        String key = str(tk.get("key"));
        String iv = str(tk.get("iv"));
        String txnNo = str(tk.get("transactionNo"));
        if (key.isBlank() || iv.isBlank() || txnNo.isBlank()) {
            return fail(out, "QBANG_TK", "Token 응답에 key/iv/transactionNo가 없습니다.");
        }

        BigDecimal vat = BigDecimal.ZERO;
        String vatRaw = str(body, "vat", "vatAmount");
        if (!vatRaw.isBlank()) {
            try {
                vat = new BigDecimal(vatRaw);
            } catch (Exception ignored) {
                vat = BigDecimal.ZERO;
            }
        } else if ("KRW".equalsIgnoreCase(charge.settlementCurrency())) {
            vat = charge.pgAmount().multiply(new BigDecimal("10"))
                    .divide(new BigDecimal("110"), 0, RoundingMode.HALF_UP);
        }

        String productName = str(body, "productName", "item");
        Map<String, Object> qr = apiClient.requestQr(creds, key, iv, txnNo,
                charge.settlementCurrency(), charge.pgAmount(), vat, productName);
        if (!Boolean.TRUE.equals(qr.get("ok"))) {
            return fail(out, strDef(qr.get("errorCode"), "QBANG_QR"),
                    strDef(qr.get("message"), "QBANG QR 요청에 실패했습니다."));
        }
        String qrCode = str(qr.get("qrCode"));
        if (qrCode.isBlank()) {
            return fail(out, "QBANG_QR", "qrCode가 없습니다.");
        }

        String buyerName = str(body, "customerNm", "buyerName", "buyer", "payerName", "name");
        String buyerEmail = str(body, "payEmailAddress", "buyerEmail", "email");
        String buyerTel = str(body, "payTelephone", "customerTel", "buyerTel", "phone");
        qbangSaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                productName, txnNo, str(body, "txnOrigin", "origin"),
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                buyerName, buyerEmail, buyerTel);

        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("transactionNo", txnNo);
        data.put("qrCode", qrCode);
        data.put("amount", charge.pgAmount());
        data.put("currency", charge.settlementCurrency());
        data.put("paymentStatus", "PENDING_QR");
        data.put("remainingTimeHint", 66);
        data.put("pgVendor", "ICOPAY");
        data.put("noti", publicNotiIngressUrls());
        out.put("data", data);
        log.info("QBANG QR started mid={} order={} txn={} ip={}",
                ouOpt.get().getCode(), orderNo, txnNo, clientIp);
        return out;
    }

    /**
     * PL 폴링. actionFlag=3 이고 승인 상태면 OKAY 후 거래 확정.
     */
    public Map<String, Object> pollAndMaybeConfirm(Long orgUnitId, String orderNo, String transactionNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null || transactionNo == null || transactionNo.isBlank()) {
            return fail(out, "INVALID", "transactionNo가 필요합니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<ResolvedQbang> resolved = resolveBinding(orgUnitId, "");
        if (resolved.isEmpty()) {
            return fail(out, "QBANG_NOT_CONFIGURED", "QBANG 설정이 없습니다.");
        }
        QbangCredentials creds = resolved.get().creds();
        Map<String, Object> pl = apiClient.pollStatus(creds, transactionNo.trim());
        if (!Boolean.TRUE.equals(pl.get("ok"))) {
            return fail(out, strDef(pl.get("errorCode"), "QBANG_PL"),
                    strDef(pl.get("message"), "상태 조회에 실패했습니다."));
        }

        int actionFlag = pl.get("actionFlag") instanceof Number n ? n.intValue() : 0;
        String state = str(pl.get("state"));
        int remaining = pl.get("remainingTime") instanceof Number n ? n.intValue() : 0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionNo", transactionNo.trim());
        data.put("orderNo", orderNo);
        data.put("state", state);
        data.put("stage", str(pl.get("stage")));
        data.put("actionFlag", actionFlag);
        data.put("remainingTime", remaining);
        data.put("paymentBrand", str(pl.get("paymentBrand")));
        data.put("approvalNo", str(pl.get("approvalNo")));

        /* 만료·실패 */
        if ("F".equalsIgnoreCase(state) || "E".equalsIgnoreCase(state)
                || (remaining <= 0 && actionFlag != 3)) {
            if (orderNo != null && !orderNo.isBlank()) {
                qbangSaleRecordService.applyFailedOutcome(orderNo, ouOpt.get().getCode(), transactionNo);
            }
            try {
                apiClient.abortNetwork(creds, transactionNo.trim());
            } catch (Exception ignored) {
                /* best effort */
            }
            data.put("paymentStatus", "FAILED");
            out.put("success", true);
            out.put("data", data);
            return out;
        }

        if (actionFlag == 3 || "C".equalsIgnoreCase(state)) {
            Map<String, Object> okay = apiClient.confirmOkay(creds, transactionNo.trim());
            if (!Boolean.TRUE.equals(okay.get("ok"))) {
                log.warn("QBANG OKAY soft-fail txn={} msg={}", transactionNo, okay.get("message"));
            }
            String appr = str(pl.get("approvalNo"));
            String brand = str(pl.get("paymentBrand"));
            String on = orderNo;
            if ((on == null || on.isBlank()) && transactionNo != null) {
                Optional<PgTrnsctn> byTxn = pgTrnsctnRepository
                        .findFirstByChillTransactionIdOrderByCreatedAtDesc(transactionNo.trim());
                if (byTxn.isPresent() && PgVendor.isQbangFamily(byTxn.get().getVan())) {
                    on = byTxn.get().getOrderNo();
                }
            }
            if (on != null && !on.isBlank()) {
                Optional<PgTrnsctn> paid = qbangSaleRecordService.applyPaidOutcome(
                        on, ouOpt.get().getCode(), transactionNo, appr, brand);
                paid.ifPresent(t -> {
                    try {
                        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "QBANG_PL");
                    } catch (Exception e) {
                        log.warn("QBANG merchant notify: {}", e.getMessage());
                    }
                    try {
                        qbangNotiMirrorService.schedulePaidMirrorAfterCommit(t);
                    } catch (Exception e) {
                        log.warn("QBANG NOTI mirror: {}", e.getMessage());
                    }
                });
            }
            data.put("paymentStatus", "PAID");
            data.put("orderNo", on);
            out.put("success", true);
            out.put("data", data);
            return out;
        }

        data.put("paymentStatus", "PENDING");
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public record ResolvedQbang(String pgCd, MerchantPgBinding binding, QbangCredentials creds) {
    }

    private static Map<String, Object> fail(Map<String, Object> out, String code, String message) {
        out.put("success", false);
        out.put("errorCode", code);
        out.put("message", message);
        return out;
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o).trim() : "";
    }

    private static String strDef(Object o, String fallback) {
        String s = str(o);
        return s.isBlank() ? (fallback != null ? fallback : "") : s;
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }
}
