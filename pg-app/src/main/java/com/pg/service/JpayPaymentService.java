package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.util.JpaySignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * JPAY 샌드박스·운영 {@code pay_index} 직접 호출(서버 사이드).
 * 노티·콜백 URL은 {@link HqApiConfig#getPublicApiBaseUrl()} 또는 노티구성 {@code publicBaseUrl} + 노티 토큰으로 조합합니다.
 */
@Service
public class JpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(JpayPaymentService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final DateTimeFormatter APPLY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_SANDBOX_PAY_INDEX = "https://sandbox.j-pay.net/pay_index";
    private static final String DEFAULT_LIVE_PAY_INDEX = "https://www.j-pay.net/pay_index";
    private static final String DEFAULT_BANK_CODE = "901";

    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final JpaySaleRecordService jpaySaleRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public JpayPaymentService(MerchantPgBindingRepository merchantPgBindingRepository,
                              PgAgencyRepository pgAgencyRepository,
                              OrgServiceUseService orgServiceUseService,
                              HqNotifyEnvService hqNotifyEnvService,
                              HqApiConfigRepository hqApiConfigRepository,
                              JpaySaleRecordService jpaySaleRecordService,
                              OrgUnitRepository orgUnitRepository) {
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.jpaySaleRecordService = jpaySaleRecordService;
        this.orgUnitRepository = orgUnitRepository;
    }

    /**
     * @return success, status(0/1/2), msg, redirectUrl?, orderNo, memberId(마스킹), rawResponse
     */
    public Map<String, Object> executeDirectSale(Long orgUnitId,
                                                 Map<String, Object> body,
                                                 HttpServletRequest req,
                                                 String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            out.put("success", false);
            out.put("message", "가맹점을 찾을 수 없습니다.");
            return out;
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            out.put("success", false);
            out.put("message", "서비스가 중지된 업체입니다.");
            return out;
        }
        Optional<MerchantPgBinding> bindOpt = findOperationalJpayWebBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            out.put("success", false);
            out.put("message", "JPAY URL 결제(운영) 바인딩이 없습니다. 결제대행사에 JPAY·URL결제를 등록하세요.");
            return out;
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(binding.getPgCd() != null ? binding.getPgCd().trim() : "");
        if (agOpt.isEmpty()) {
            out.put("success", false);
            out.put("message", "PG사 연동(tb_pg_agency) 행을 찾을 수 없습니다.");
            return out;
        }
        PgAgency agency = agOpt.get();
        String mid = binding.getMid() != null ? binding.getMid().trim() : "";
        String apiKey = agency.getApiKey() != null ? agency.getApiKey().trim() : "";
        if (mid.isEmpty() || apiKey.isEmpty()) {
            out.put("success", false);
            out.put("message", "JPAY MID·API Key(tb_pg_agency)를 설정하세요.");
            return out;
        }
        int routeNo = parseRouteNo(binding.getRootNo());

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            out.put("success", false);
            out.put("message", "orderNo가 필요합니다.");
            return out;
        }
        if (orderNo.length() > 64) {
            orderNo = orderNo.substring(0, 64);
        }
        BigDecimal amountBd = parseAmount(body.get("amount"));
        if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
            out.put("success", false);
            out.put("message", "amount는 0보다 커야 합니다.");
            return out;
        }
        String currency = str(body.get("currency"));
        if (currency.isBlank()) {
            currency = "USD";
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);

        String payIndexUrl = resolvePayIndexUrl(agency);
        String bankCode = resolveBankCode(agency);
        String notifyTarget = resolveExtraStr(agency, "jpayNotifyTarget", "cbJpay");
        String resultTarget = resolveExtraStr(agency, "jpayResultTarget", "rsJpay");
        String publicBase = resolvePublicApiBase(req);
        if (publicBase.isBlank()) {
            out.put("success", false);
            out.put("message", "공개 API 베이스 URL이 없습니다. 배포설정에 publicApiBaseUrl 또는 노티 publicBaseUrl을 넣으세요.");
            return out;
        }
        String token = hqNotifyEnvService.getOrCreate().getIngressToken();
        String notifyPathPrefix = resolveJpayNotifyPathPrefix(agency);
        String notifyUrl = publicBase + notifyPathPrefix + token + "/" + notifyTarget;
        String callbackUrl = publicBase + notifyPathPrefix + token + "/" + resultTarget;

        String siteUrl = str(body.get("payUrl"));
        if (siteUrl.isBlank()) {
            siteUrl = publicBase;
        }

        String compCode = str(body.get("compId"));
        String attach = compCode.isBlank() ? "" : "icopayCompId=" + compCode.trim();

        String applyDate = LocalDateTime.now().format(APPLY_FMT);

        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", mid);
        signParams.put("pay_orderid", orderNo);
        signParams.put("pay_applydate", applyDate);
        signParams.put("pay_bankcode", bankCode);
        signParams.put("pay_notifyurl", notifyUrl);
        signParams.put("pay_callbackurl", callbackUrl);
        signParams.put("pay_amount", amountBd.stripTrailingZeros().toPlainString());

        String md5sign = JpaySignatureUtil.signRequestParams(signParams, apiKey);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("pay_md5sign", md5sign);
        form.add("pay_currency", currency);
        form.add("pay_url", siteUrl);
        if (!attach.isBlank()) {
            form.add("attach", attach);
        }

        addIfPresent(form, body, "pay_cardno", "payCardno");
        addIfPresent(form, body, "pay_cardmonth", "payCardmonth");
        addIfPresent(form, body, "pay_cardyear", "payCardyear");
        addIfPresent(form, body, "pay_cardcvv", "payCardcvv");
        addIfPresent(form, body, "pay_firstname", "payFirstname");
        addIfPresent(form, body, "pay_lastname", "payLastname");
        addIfPresent(form, body, "pay_street_address1", "payStreetAddress1");
        addIfPresent(form, body, "pay_street_address2", "payStreetAddress2");
        addIfPresent(form, body, "pay_city", "payCity");
        addIfPresent(form, body, "pay_postcode", "payPostcode");
        addIfPresent(form, body, "pay_state", "payState");
        addIfPresent(form, body, "pay_country_iso_code_2", "payCountryIsoCode2");
        addIfPresent(form, body, "pay_email_address", "payEmailAddress");
        addIfPresent(form, body, "pay_telephone", "payTelephone");
        addIfPresent(form, body, "pay_language", "payLanguage");
        addIfPresent(form, body, "system", "system");

        String productJson = str(body.get("payProductname"));
        if (productJson.isBlank()) {
            String item = str(body.get("item"));
            if (item.isBlank()) {
                item = "Order " + orderNo;
            }
            productJson = defaultProductJson(item, amountBd.stripTrailingZeros().toPlainString());
        }
        form.add("pay_productname", productJson);

        String ip = clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1";
        form.add("pay_ip", ip);
        String ua = req != null ? req.getHeader("User-Agent") : "";
        if (ua != null && !ua.isBlank()) {
            form.add("pay_useragent", ua.length() > 512 ? ua.substring(0, 512) : ua);
        }

        jpaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency, routeNo,
                str(body.get("payEmailAddress")),
                str(body.get("item")),
                resolveTxnOrigin(str(body.get("txnOrigin"))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        String raw;
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(payIndexUrl, entity, String.class);
            raw = resp.getBody() != null ? resp.getBody() : "";
        } catch (Exception e) {
            log.warn("JPAY pay_index 호출 실패: {}", e.getMessage());
            out.put("success", false);
            out.put("message", "JPAY 연동 호출 실패: " + e.getMessage());
            return out;
        }

        int status = -1;
        String msg = "";
        String url3ds = "";
        try {
            JsonNode n = OM.readTree(raw.trim().startsWith("{") ? raw : "{}");
            status = n.path("status").asInt(-1);
            msg = n.path("msg").asText("");
            url3ds = n.path("url").asText("");
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", "JPAY 응답 파싱 실패");
            out.put("rawResponse", raw);
            return out;
        }

        /* compId for sync outcome — resolve from org */
        String midCode = resolveMerchantCode(orgUnitId);

        if (status == 0 || status == 2) {
            jpaySaleRecordService.applySyncApiOutcome(midCode, orderNo, status, msg);
        }

        out.put("success", true);
        out.put("status", status);
        out.put("msg", msg);
        if (status == 1 && url3ds != null && !url3ds.isBlank()) {
            out.put("redirectUrl", url3ds);
        }
        out.put("orderNo", orderNo);
        out.put("payIndexUrl", payIndexUrl);
        out.put("rawResponse", raw);
        return out;
    }

    /** 가맹점 API 인라인 결제(jpay-pay.html) 등 — JPAY URL 결제 운영 바인딩 존재 여부 */
    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalJpayWebBinding(orgUnitId).isPresent();
    }

    private String resolveMerchantCode(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(o -> o.getCode() != null ? o.getCode().trim() : "")
                .orElse("");
    }

    private static String resolveTxnOrigin(String raw) {
        if (raw == null || raw.isBlank()) {
            return "URL";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("MERCHANT_API".equals(u)) {
            return "MERCHANT_API";
        }
        return "URL";
    }

    private Optional<MerchantPgBinding> findOperationalJpayWebBinding(Long orgUnitId) {
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && PgVendor.isJpayFamily(b.getPgCd()))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .min(Comparator.comparingInt(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE));
    }

    private String resolvePayIndexUrl(PgAgency agency) {
        String fromJson = resolveExtraStr(agency, "jpayPayIndexUrl", "");
        if (!fromJson.isBlank()) {
            return fromJson.trim();
        }
        boolean sand = agency.getSandboxYn() == null || "Y".equalsIgnoreCase(agency.getSandboxYn().trim());
        return sand ? DEFAULT_SANDBOX_PAY_INDEX : DEFAULT_LIVE_PAY_INDEX;
    }

    private String resolveBankCode(PgAgency agency) {
        String c = resolveExtraStr(agency, "jpayBankCode", "");
        return c.isBlank() ? DEFAULT_BANK_CODE : c.trim();
    }

    /**
     * {@code tb_pg_agency.credentials_extra_json} 의 {@code jpayNotifyIngressStyle}.
     * {@code OPEN} 이면 레거시 open 경로, 그 외(비우거나 {@code MIDDLEWARE})는 권장 미들웨어 경로.
     */
    private static String resolveJpayNotifyPathPrefix(PgAgency agency) {
        String style = resolveExtraStr(agency, "jpayNotifyIngressStyle", "").trim().toUpperCase(Locale.ROOT);
        if ("OPEN".equals(style)) {
            return PgNotifyIngressPaths.OPEN_PREFIX;
        }
        return PgNotifyIngressPaths.MIDDLEWARE_PREFIX;
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

    private String resolvePublicApiBase(HttpServletRequest req) {
        Optional<HqApiConfig> cfg = hqApiConfigRepository.findAll().stream().findFirst();
        if (cfg.isPresent() && cfg.get().getPublicApiBaseUrl() != null && !cfg.get().getPublicApiBaseUrl().isBlank()) {
            return trimSlash(cfg.get().getPublicApiBaseUrl().trim());
        }
        String pub = hqNotifyEnvService.getOrCreate().getPublicBaseUrl();
        if (pub != null && !pub.isBlank()) {
            return trimSlash(pub.trim());
        }
        if (req != null) {
            String scheme = req.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) {
                scheme = req.getScheme();
            }
            String host = req.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = req.getServerName();
                int port = req.getServerPort();
                if (("http".equalsIgnoreCase(scheme) && port != 80)
                        || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                    host = host + ":" + port;
                }
            }
            return trimSlash(scheme + "://" + host);
        }
        return "";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("/+$", "");
    }

    private static void addIfPresent(MultiValueMap<String, String> form, Map<String, Object> body, String formKey, String bodyKey) {
        String v = str(body.get(bodyKey));
        if (!v.isBlank()) {
            form.add(formKey, v);
        }
    }

    private static int parseRouteNo(String rootNo) {
        if (rootNo == null || rootNo.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rootNo.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            return null;
        }
        try {
            String s = o.toString().trim().replace(",", "");
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String defaultProductJson(String productName, String price) {
        try {
            List<Map<String, String>> one = List.of(Map.of(
                    "sku", "SKU1",
                    "productName", productName,
                    "productImage", "https://example.com/p.png",
                    "attributes", "",
                    "price", price,
                    "quantity", "1"));
            return OM.writeValueAsString(one);
        } catch (Exception e) {
            return "[{\"sku\":\"SKU1\",\"productName\":\"item\",\"productImage\":\"\",\"attributes\":\"\",\"price\":\"0.01\",\"quantity\":\"1\"}]";
        }
    }
}
