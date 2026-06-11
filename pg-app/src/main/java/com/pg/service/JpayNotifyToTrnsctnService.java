package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.JpaySignatureUtil;
import com.pg.util.NotifyToTxnStatusMerge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPAY 비동기 노티({@code memberid},{@code orderid},{@code transaction_id},{@code returncode},{@code sign} 등)를
 * {@link PgTrnsctn}에 반영합니다. 서명 검증에 성공한 건만 갱신합니다.
 */
@Service
public class JpayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(JpayNotifyToTrnsctnService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ORIGIN_URL = "URL";
    private static final String ST_PAID = "10";
    private static final String ST_FAIL = "99";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final SettlementCalcService settlementCalcService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final JpaySubscriptionNotifyService jpaySubscriptionNotifyService;

    public JpayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                      PgAgencyRepository pgAgencyRepository,
                                      SettlementCalcService settlementCalcService,
                                      HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                      JpaySubscriptionNotifyService jpaySubscriptionNotifyService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.settlementCalcService = settlementCalcService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.jpaySubscriptionNotifyService = jpaySubscriptionNotifyService;
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("JPAY 노티 적재 예외: {}", e.getMessage());
            return true;
        }
    }

    private boolean doTry(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return false;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        Map<String, String> form = parseJpayNotifyFields(raw.trim());
        if (!looksLikeJpayServerNotify(form)) {
            return false;
        }
        String memberid = first(form, "memberid");
        String orderid = first(form, "orderid");
        String ret = first(form, "returncode");
        String paySt = first(form, "paymentstatus");
        String txnId = first(form, "transaction_id");
        if (memberid.isBlank() || orderid.isBlank() || (ret.isBlank() && paySt.isBlank())) {
            return false;
        }
        Optional<PgAgency> agOpt = findJpayAgencyByMerchantMid(memberid);
        if (agOpt.isEmpty()) {
            log.debug("JPAY 노티 MID에 해당하는 tb_pg_agency 없음 memberid={}", memberid);
            return false;
        }
        PgAgency ag = agOpt.get();
        String apiKey = ag.getApiKey() != null ? ag.getApiKey().trim() : "";
        String sign = first(form, "sign");
        String ch = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        boolean resultChannel = "RESULT".equals(ch);
        if (apiKey.isEmpty() || sign.isBlank()) {
            /* 3DS 동기 복귀(rsJpay)는 sign 없음 — ChillPay 노티매핑(RESULT·paymentStatus)으로 위임 */
            log.info("JPAY 노티 sign 없음 — 다음 핸들러(노티매핑)로 위임 orderid={} channel={}", orderid, ch);
            return false;
        }
        if (!JpaySignatureUtil.verifyNotifySign(form, apiKey, sign)) {
            log.warn("JPAY 노티 서명 불일치 orderid={} channel={}", orderid, ch);
            if (resultChannel) {
                return false;
            }
            return true;
        }
        String merchantId = in.getMerchantId();
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("JPAY 노티 가맹점 미해석 orderid={}", orderid);
            return true;
        }
        if (jpaySubscriptionNotifyService.isSubscriptionNotify(form)) {
            jpaySubscriptionNotifyService.applySubscriptionNotify(merchantId.trim(), form, notifyChannel);
            return true;
        }
        String on = orderid.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        Optional<PgTrnsctn> ex = findJpayTxn(merchantId.trim(), on);
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(merchantId.trim());
            x.setServiceType("URL_JPAY");
            x.setOrigin(ORIGIN_URL);
            return x;
        });
        t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        t.setOrderNo(on);
        t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
        if (txnId != null && !txnId.isBlank()) {
            String tid = txnId.trim();
            t.setChillTransactionId(tid.length() > 64 ? tid.substring(0, 64) : tid);
        }
        String amtStr = first(form, "true_amount");
        if (amtStr.isBlank()) {
            amtStr = first(form, "amount");
        }
        if (!amtStr.isBlank()) {
            try {
                BigDecimal a = new BigDecimal(amtStr.replace(",", "").trim());
                if (a.compareTo(BigDecimal.ZERO) > 0) {
                    t.setAmtKrw(a);
                }
            } catch (Exception ignored) {
                /* keep */
            }
        }
        String cur = first(form, "currency");
        if (!cur.isBlank()) {
            String u = cur.trim().toUpperCase(Locale.ROOT);
            t.setCurType(u.length() > 3 ? u.substring(0, 3) : u);
        }
        t.setNotifyChannelType(ch);

        boolean ok = "00".equals(ret.trim()) || isJpaySuccessPaymentStatus(paySt);
        String next = ok ? ST_PAID : ST_FAIL;
        String merged = NotifyToTxnStatusMerge.merge(t.getStatus(), next, ch);
        if (merged == null || merged.isBlank()) {
            merged = next;
        }
        t.setStatus(merged);
        t.setChillPaymentStatus(ok ? "JPAY_OK" : ("JPAY_FAIL " + ret).trim());
        if (ST_PAID.equals(merged)) {
            ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            t.setPaidAt(LocalDateTime.now(wall));
        } else {
            t.setPaidAt(null);
        }
        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
            t.setCustomerId("guest");
        }
        t.setPaymentChannel("CARD");
        if (in.getRootNo() != null && !in.getRootNo().isBlank()) {
            String r = in.getRootNo().trim();
            t.setRouteNo(r.length() > 32 ? r.substring(0, 32) : r);
        }

        pgTrnsctnRepository.save(t);
        if (ST_PAID.equals(merged) && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            } catch (Exception rtEx) {
                log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), rtEx.getMessage());
            }
        }
        log.info("JPAY 노티 반영 trnId={} merchantId={} orderNo={} returncode={}", t.getTrnId(), merchantId, on, ret);
        return true;
    }

    private Optional<PgTrnsctn> findJpayTxn(String merchantId, String orderNo) {
        Optional<PgTrnsctn> sub = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, "SUBSCRIPTION");
        if (sub.isPresent()) {
            return sub;
        }
        Optional<PgTrnsctn> api = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, "MERCHANT_API");
        if (api.isPresent()) {
            return api;
        }
        return pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, ORIGIN_URL);
    }

    private Optional<PgAgency> findJpayAgencyByMerchantMid(String memberid) {
        String m = memberid.trim();
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(m).stream()
                .filter(a -> PgVendor.isJpayFamily(a.getPgCd()))
                .findFirst();
    }

    private static boolean looksLikeJpayServerNotify(Map<String, String> f) {
        if (first(f, "memberid").isBlank() || first(f, "orderid").isBlank()) {
            return false;
        }
        if (!first(f, "transaction_id").isBlank()) {
            return !first(f, "returncode").isBlank() || isJpaySuccessPaymentStatus(first(f, "paymentstatus"));
        }
        return !first(f, "returncode").isBlank() || isJpaySuccessPaymentStatus(first(f, "paymentstatus"));
    }

    private static boolean isJpaySuccessPaymentStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        return "succeeded".equals(p) || "success".equals(p) || "paid".equals(p) || "00".equals(p);
    }

    private Map<String, String> parseJpayNotifyFields(String body) {
        if (body.startsWith("{")) {
            return parseJsonLowerKeys(body);
        }
        return parseFormLowerKeys(body);
    }

    private static Map<String, String> parseJsonLowerKeys(String json) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root != null && root.isObject()) {
                Iterator<String> it = root.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    JsonNode v = root.get(k);
                    if (v != null && !v.isNull() && v.isValueNode()) {
                        String val = v.asText();
                        if (val != null && !val.isBlank()) {
                            m.put(k.toLowerCase(Locale.ROOT), val.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private static Map<String, String> parseFormLowerKeys(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            for (String pair : body.split("&")) {
                int i = pair.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String k = URLDecoder.decode(pair.substring(0, i).trim(), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
                String v = URLDecoder.decode(pair.substring(i + 1).trim(), StandardCharsets.UTF_8);
                if (!v.isEmpty()) {
                    m.put(k, v);
                }
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }
}
