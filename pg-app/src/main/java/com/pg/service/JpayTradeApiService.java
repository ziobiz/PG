package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.JpayNotifyStatusResolver;
import com.pg.util.JpaySignatureUtil;
import com.pg.util.JpayTradeStatusMapper;
import com.pg.util.PgNotifyInternalStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * JPAY Trade API — {@code /pay/trade/query}, {@code /pay/trade/refund}.
 */
@Service
public class JpayTradeApiService {

    private static final Logger log = LoggerFactory.getLogger(JpayTradeApiService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final String DEFAULT_SANDBOX_PAY_INDEX = "https://sandbox.j-pay.net/pay_index";
    private static final String DEFAULT_LIVE_PAY_INDEX = "https://api.j-pay.net/pay_index";

    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final RestTemplate restTemplate = createRestTemplate();

    public JpayTradeApiService(MerchantPgBindingRepository merchantPgBindingRepository,
                               PgAgencyRepository pgAgencyRepository,
                               OrgUnitRepository orgUnitRepository,
                               PgTrnsctnRepository pgTrnsctnRepository) {
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    public Map<String, Object> queryAndApplyToTxn(String trnId) {
        PgTrnsctn t = pgTrnsctnRepository.findById(trnId.trim())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));
        requireJpayTxn(t);
        TradeCtx ctx = resolveTradeCtx(t);
        JsonNode body = postTradeQuery(ctx, t.getOrderNo());
        String tradeState = body.path("trade_state").asText("");
        String returnCode = body.path("returncode").asText("");
        String mapped = JpayTradeStatusMapper.fromTradeState(tradeState);
        if (mapped == null) {
            mapped = JpayNotifyStatusResolver.fromReturnCode(returnCode);
        }
        String oldStatus = nz(t.getStatus());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trnId", t.getTrnId());
        out.put("orderNo", t.getOrderNo());
        out.put("transactionId", t.getChillTransactionId());
        out.put("tradeState", tradeState);
        out.put("returncode", returnCode);
        out.put("oldStatus", oldStatus);
        Map<String, Object> apiMap = new LinkedHashMap<>();
        body.fields().forEachRemaining(e -> apiMap.put(e.getKey(),
                e.getValue().isNull() ? "" : e.getValue().asText("")));
        out.put("apiResponse", apiMap);
        if (mapped != null && !mapped.equals(oldStatus)) {
            t.setStatus(mapped);
            String rcLabel = returnCode.isBlank() ? tradeState : returnCode;
            t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(mapped, rcLabel));
            if (!PgNotifyInternalStatusMapper.ST_PAID.equals(mapped)) {
                t.setPaidAt(null);
            }
            String txnId = body.path("transaction_id").asText("");
            if (!txnId.isBlank() && (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank())) {
                t.setChillTransactionId(txnId);
            }
            pgTrnsctnRepository.save(t);
            out.put("newStatus", mapped);
            out.put("updated", true);
            out.put("message", "JPAY 조회 결과로 결제내역 상태를 갱신했습니다.");
        } else {
            out.put("newStatus", oldStatus);
            out.put("updated", false);
            out.put("message", mapped == null ? "JPAY 조회는 성공했으나 상태 매핑이 없습니다." : "이미 동일 상태입니다.");
        }
        return out;
    }

    public void requestRefund(PgTrnsctn t, BigDecimal refundAmount, String reason) {
        requireJpayTxn(t);
        if (t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            throw new IllegalStateException("주문번호가 없어 JPAY 환불 API를 호출할 수 없습니다.");
        }
        String txnId = t.getChillTransactionId();
        if (txnId == null || txnId.isBlank()) {
            throw new IllegalStateException("Transaction ID가 없어 JPAY 환불 API를 호출할 수 없습니다.");
        }
        TradeCtx ctx = resolveTradeCtx(t);
        BigDecimal payAmt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        if (payAmt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("결제금액이 없어 JPAY 환불 API를 호출할 수 없습니다.");
        }
        BigDecimal refund = refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0
                ? refundAmount : payAmt;
        if (refund.compareTo(payAmt) > 0) {
            refund = payAmt;
        }
        String currency = t.getCurType() != null && !t.getCurType().isBlank()
                ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "JPY";
        String refundReason = reason != null && !reason.isBlank() ? reason.trim() : "icopay refund";

        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", ctx.mid());
        signParams.put("transaction_id", txnId.trim());
        signParams.put("pay_orderid", t.getOrderNo().trim());
        signParams.put("currency", currency);
        signParams.put("pay_amount", amountPlain(payAmt));
        signParams.put("refund_amount", amountPlain(refund));
        signParams.put("refund_reason", refundReason);
        String md5sign = JpaySignatureUtil.signRequestParams(signParams, ctx.apiKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("pay_md5sign", md5sign);

        String url = ctx.tradeBase() + "/refund";
        JsonNode body = postForm(url, form);
        String rc = body.path("returncode").asText("");
        if (!"00".equals(rc) && !"0".equals(rc)) {
            String msg = body.path("refund_message").asText(body.path("msg").asText("JPAY 환불 실패"));
            throw new IllegalStateException(msg);
        }
        log.info("JPAY refund OK orderNo={} transactionId={} refund={}", t.getOrderNo(), txnId, refund);
    }

    private JsonNode postTradeQuery(TradeCtx ctx, String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("주문번호가 필요합니다.");
        }
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", ctx.mid());
        signParams.put("pay_orderid", orderNo.trim());
        String md5sign = JpaySignatureUtil.signRequestParams(signParams, ctx.apiKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pay_memberid", ctx.mid());
        form.add("pay_orderid", orderNo.trim());
        form.add("pay_md5sign", md5sign);

        String url = ctx.tradeBase() + "/query";
        JsonNode body = postForm(url, form);
        String rc = body.path("returncode").asText("");
        if (!"00".equals(rc) && !"0".equals(rc) && body.path("trade_state").asText("").isBlank()) {
            throw new IllegalStateException(body.path("msg").asText("JPAY 조회 실패"));
        }
        return body;
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String raw = resp.getBody() != null ? resp.getBody().trim() : "";
            if (raw.isBlank()) {
                throw new IllegalStateException("JPAY 응답 본문이 비어 있습니다.");
            }
            if (raw.startsWith("{")) {
                return OM.readTree(raw);
            }
            throw new IllegalStateException("JPAY 응답 형식이 올바르지 않습니다.");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalStateException("JPAY API 호출 실패: " + msg, e);
        }
    }

    private TradeCtx resolveTradeCtx(PgTrnsctn t) {
        long ouId = orgUnitRepository.findByCodeIgnoreCase(nz(t.getMerchantId()))
                .orElseThrow(() -> new IllegalStateException("가맹점 조직을 찾을 수 없습니다: " + t.getMerchantId()))
                .getId();
        MerchantPgBinding binding = findOperationalJpayBinding(ouId, t.getVan())
                .orElseThrow(() -> new IllegalStateException("JPAY 운영 PG 바인딩이 없습니다."));
        PgAgency agency = pgAgencyRepository.findByPgCd(nz(binding.getPgCd()))
                .orElseThrow(() -> new IllegalStateException("PG사 연동 행을 찾을 수 없습니다."));
        String mid = nz(binding.getMid());
        String apiKey = agency.getApiKey() != null ? agency.getApiKey().trim() : "";
        if (mid.isBlank() || apiKey.isBlank()) {
            throw new IllegalStateException("JPAY MID·API Key를 설정하세요.");
        }
        String tradeBase = resolveTradeApiBase(agency);
        return new TradeCtx(mid, apiKey, tradeBase);
    }

    private Optional<MerchantPgBinding> findOperationalJpayBinding(long orgUnitId, String pgCdHint) {
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        if (pgCdHint != null && !pgCdHint.isBlank()) {
            Optional<MerchantPgBinding> byPg = list.stream()
                    .filter(b -> pgCdHint.equalsIgnoreCase(nz(b.getPgCd())))
                    .filter(this::isOperationalJpayBinding)
                    .findFirst();
            if (byPg.isPresent()) {
                return byPg;
            }
        }
        return list.stream()
                .filter(this::isOperationalJpayBinding)
                .min(Comparator.comparingInt(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE));
    }

    private boolean isOperationalJpayBinding(MerchantPgBinding b) {
        if (b == null || b.getPgCd() == null || !PgVendor.isJpayFamily(b.getPgCd())) {
            return false;
        }
        if (b.getOperationalYn() == null || !"Y".equalsIgnoreCase(b.getOperationalYn().trim())) {
            return false;
        }
        return b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim());
    }

    private static String resolveTradeApiBase(PgAgency agency) {
        String payIndex = resolvePayIndexUrl(agency);
        if (payIndex.contains("/pay_index")) {
            return payIndex.replace("/pay_index", "/pay/trade");
        }
        if (payIndex.endsWith("/")) {
            return payIndex + "pay/trade";
        }
        return payIndex + "/pay/trade";
    }

    private static String resolvePayIndexUrl(PgAgency agency) {
        String fromJson = resolveExtraStr(agency, "jpayPayIndexUrl", "");
        if (!fromJson.isBlank()) {
            return normalizePayIndex(fromJson.trim(), agency);
        }
        String epPay = agency.getEndpointUrlPay();
        if (epPay != null && !epPay.isBlank()) {
            return normalizePayIndex(epPay.trim(), agency);
        }
        String legacyEp = agency.getApiEndpoint();
        if (legacyEp != null && !legacyEp.isBlank()) {
            return normalizePayIndex(legacyEp.trim(), agency);
        }
        boolean sand = agency.getSandboxYn() == null || "Y".equalsIgnoreCase(agency.getSandboxYn().trim());
        return sand ? DEFAULT_SANDBOX_PAY_INDEX : DEFAULT_LIVE_PAY_INDEX;
    }

    private static String normalizePayIndex(String url, PgAgency agency) {
        if (url == null || url.isBlank()) {
            boolean sand = agency == null || agency.getSandboxYn() == null
                    || "Y".equalsIgnoreCase(agency.getSandboxYn().trim());
            return sand ? DEFAULT_SANDBOX_PAY_INDEX : DEFAULT_LIVE_PAY_INDEX;
        }
        String u = url.trim();
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.contains("sandbox.j-pay.net") && lower.startsWith("http://")) {
            return "https://" + u.substring(7);
        }
        if (lower.matches("https?://(www\\.)?j-pay\\.net/pay_index/?")) {
            return DEFAULT_LIVE_PAY_INDEX;
        }
        if (lower.startsWith("http://") && lower.contains("j-pay.net")) {
            return "https://" + u.substring(7);
        }
        return u;
    }

    private static String resolveExtraStr(PgAgency agency, String key, String def) {
        if (agency == null || agency.getCredentialsExtraJson() == null || agency.getCredentialsExtraJson().isBlank()) {
            return def != null ? def : "";
        }
        try {
            JsonNode n = OM.readTree(agency.getCredentialsExtraJson());
            String v = n.path(key).asText("");
            return v.isBlank() && def != null ? def : v;
        } catch (Exception e) {
            return def != null ? def : "";
        }
    }

    private static void requireJpayTxn(PgTrnsctn t) {
        if (t == null || !PgVendor.isJpayFamily(t.getVan())) {
            throw new IllegalStateException("JPAY 거래만 지원합니다.");
        }
    }

    private static String amountPlain(BigDecimal amt) {
        return amt.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o).trim() : "";
    }

    private static RestTemplate createRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        StringHttpMessageConverter utf8 = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        utf8.setWriteAcceptCharset(false);
        rt.getMessageConverters().add(0, utf8);
        return rt;
    }

    private record TradeCtx(String mid, String apiKey, String tradeBase) {
    }
}
