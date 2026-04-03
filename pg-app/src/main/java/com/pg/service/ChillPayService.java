package com.pg.service;

import com.pg.config.ChillPayProperties;
import com.pg.dto.ChillPayDirectCreditRequest;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgAgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ChillPay DirectCredit API 연동 서비스.
 * 본사 ChillPay 자격: PG사 API 연동(tb_pg_agency, CHILLPAY) → API 구성 세팅(tb_hq_api_config) → application.yml 순.
 * <p>
 * {@code tb_pg_agency} 에서 PG코드가 {@code CHILLPAY} 로 시작하고 사용 Y인 행들의 엔드포인트를 병합합니다.
 * (용도별로 {@code CHILLPAY_API}, {@code CHILLPAY_URL} 등 분리 등록 가능.)
 */
@Service
public class ChillPayService {

    private static final Logger log = LoggerFactory.getLogger(ChillPayService.class);
    private static final String CCD_SCRIPT_SANDBOX = "https://sandbox-bankdemo3.chillpay.co/js/ccdpayment.js";
    private static final String CCD_SCRIPT_PROD = "https://cdn.chill.credit/js/ccdpayment.js";
    private static final String DIRECT_CREDIT_SANDBOX = "https://sandbox-api-directcredit.chillpay.co";
    private static final String DIRECT_CREDIT_PROD = "https://api-directcredit.chillpay.co";

    /**
     * 브라우저가 POST로 이동하는 ChillPay 호스티드 결제 페이지 (HTML Form / Code Template).
     * Merchant Integration Manual §2.1, 2.2
     */
    private static final String REDIRECT_PAYMENT_SANDBOX = "https://sandbox-cdnv3.chillpay.co/Payment/";
    private static final String REDIRECT_PAYMENT_PROD = "https://cdn.chillpay.co/Payment/";
    /**
     * 서버 간 연동용 Payment API v2 (application/x-www-form-urlencoded).
     * Manual §2.3
     */
    private static final String APPSRV_PAYMENT_V2_SANDBOX = "https://sandbox-appsrv2.chillpay.co/api/v2/Payment/";
    private static final String APPSRV_PAYMENT_V2_PROD = "https://appsrv.chillpay.co/api/v2/Payment/";

    private final ChillPayProperties props;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final RestTemplate restTemplate = new RestTemplate();

    public ChillPayService(ChillPayProperties props, HqApiConfigRepository hqApiConfigRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository,
                          PgAgencyRepository pgAgencyRepository,
                          OrgServiceUseService orgServiceUseService) {
        this.props = props;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    /** DirectCredit·거래 적재 시 Route 표시용 */
    public int resolveEffectiveRouteNo(Long merchantOrgUnitId) {
        return resolveConfig(merchantOrgUnitId).routeNo();
    }

    /** 가맹점 orgUnitId가 있으면 해당 가맹점의 ChillPay 계열(pg_cd가 CHILLPAY로 시작) 운영 행 우선, 없으면 본사 설정 */
    private Config resolveConfig(Long merchantOrgUnitId) {
        ChillPayAgencyUrlOverrides urlOv = loadChillPayAgencyUrlOverrides();
        if (merchantOrgUnitId != null) {
            Optional<MerchantPgBinding> binding = findOperationalChillPayFamilyBinding(merchantOrgUnitId);
            if (binding.isPresent()) {
                MerchantPgBinding b = binding.get();
                String mc = (b.getMid() != null && !b.getMid().isEmpty()) ? b.getMid() : null;
                String ak = (b.getApiKey() != null && !b.getApiKey().isEmpty()) ? b.getApiKey() : null;
                String mk = (b.getIvKey() != null && !b.getIvKey().isEmpty()) ? b.getIvKey() : null;
                if (ak != null && mk != null) {
                    Config base = resolveConfigFromHq(urlOv);
                    return new Config(mc != null ? mc : base.merchantCode(), ak, mk, base.routeNo(), base.sandbox(), urlOv);
                }
            }
        }
        return resolveConfigFromHq(urlOv);
    }

    private static boolean isChillPayFamilyPgCd(String pgCd) {
        if (pgCd == null) {
            return false;
        }
        String u = pgCd.trim().toUpperCase(Locale.ROOT);
        return u.equals("CHILLPAY") || u.startsWith("CHILLPAY");
    }

    /** 운영(Y)인 결제대행사 행 중 ChillPay 계열 첫 행 — 정확히 CHILLPAY 코드 우선 */
    private Optional<MerchantPgBinding> findOperationalChillPayFamilyBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getPgCd() != null && isChillPayFamilyPgCd(b.getPgCd()))
                .min(Comparator
                        .comparing((MerchantPgBinding b) -> "CHILLPAY".equalsIgnoreCase(b.getPgCd().trim()) ? 0 : 1)
                        .thenComparing(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE)
                        .thenComparing(MerchantPgBinding::getId));
    }

    /**
     * 사용 Y인 ChillPay 계열({@code pg_cd}가 CHILLPAY로 시작) 행 전부의 엔드포인트를 파싱해 URL 오버라이드를 병합한다.
     */
    private ChillPayAgencyUrlOverrides loadChillPayAgencyUrlOverrides() {
        ChillPayAgencyUrlOverrides acc = ChillPayAgencyUrlOverrides.empty();
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (a.getPgCd() == null || !isChillPayFamilyPgCd(a.getPgCd())) {
                continue;
            }
            if (a.getUseYn() == null || !"Y".equalsIgnoreCase(a.getUseYn().trim())) {
                continue;
            }
            for (String raw : new String[] {
                    a.getEndpointApi(),
                    a.getApiEndpoint(),
                    a.getEndpointUrlPay(),
                    a.getEndpointNoti()
            }) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                acc = acc.merge(parseChillPayApiEndpoint(raw.trim()));
            }
        }
        if (acc.hasAny()) {
            log.debug("ChillPay URL overrides merged from tb_pg_agency (CHILLPAY* rows)");
        }
        return acc;
    }

    /**
     * 한 필드에 하나의 URL만 저장한다는 전제로 유형을 추정합니다.
     * <ul>
     *   <li>{@code …ccdpayment.js} → CCD 인라인 스크립트</li>
     *   <li>{@code …appsrv…/api/v2…} → Payment API v2 베이스</li>
     *   <li>{@code …directcredit…} 또는 {@code …/api/v1/payment} → DirectCredit(JSON) 결제 API</li>
     *   <li>{@code …cdnv3…/Payment…}, {@code …cdn.chillpay.co/…/Payment…} 또는 {@code https…/…/payment…}(v1 제외) → 호스티드 REDIRECT</li>
     *   <li>그 외 {@code https://…} → DirectCredit 결제 URL로 그대로 사용</li>
     * </ul>
     */
    static ChillPayAgencyUrlOverrides parseChillPayApiEndpoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChillPayAgencyUrlOverrides.empty();
        }
        String t = raw.trim();
        String lower = t.toLowerCase(Locale.ROOT);

        if (lower.contains("ccdpayment.js")) {
            return new ChillPayAgencyUrlOverrides(null, null, null, t);
        }
        if (lower.contains("appsrv") && lower.contains("/api/v2")) {
            String base = t.endsWith("/") ? t : t + "/";
            return new ChillPayAgencyUrlOverrides(null, null, base, null);
        }
        if (lower.contains("directcredit") || lower.contains("/api/v1/payment")) {
            String p = t.replaceAll("/+$", "");
            if (!lower.contains("/api/v1/payment")) {
                p = p + "/api/v1/payment";
            }
            return new ChillPayAgencyUrlOverrides(p, null, null, null);
        }
        if (lower.contains("cdnv3.chillpay")
                || (lower.contains("cdn.chillpay.co") && lower.contains("/payment"))) {
            String r = t.endsWith("/") ? t : t + "/";
            return new ChillPayAgencyUrlOverrides(null, r, null, null);
        }
        if ((lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.contains("/payment/") || lower.endsWith("/payment"))
                && !lower.contains("/api/v1/")) {
            String r = t.endsWith("/") ? t : t + "/";
            return new ChillPayAgencyUrlOverrides(null, r, null, null);
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new ChillPayAgencyUrlOverrides(t, null, null, null);
        }
        return ChillPayAgencyUrlOverrides.empty();
    }

    private record ChillPayAgencyUrlOverrides(
            String paymentApiUrl,
            String redirectPaymentPageUrl,
            String appsrvPaymentV2Url,
            String ccdScriptUrl
    ) {
        static ChillPayAgencyUrlOverrides empty() {
            return new ChillPayAgencyUrlOverrides(null, null, null, null);
        }

        boolean hasAny() {
            return notBlank(paymentApiUrl) || notBlank(redirectPaymentPageUrl)
                    || notBlank(appsrvPaymentV2Url) || notBlank(ccdScriptUrl);
        }

        /** 이미 채워진 항목은 유지하고, 비어 있는 항목만 {@code o}로 채운다. */
        ChillPayAgencyUrlOverrides merge(ChillPayAgencyUrlOverrides o) {
            if (o == null) {
                return this;
            }
            return new ChillPayAgencyUrlOverrides(
                    firstNonBlank(paymentApiUrl, o.paymentApiUrl),
                    firstNonBlank(redirectPaymentPageUrl, o.redirectPaymentPageUrl),
                    firstNonBlank(appsrvPaymentV2Url, o.appsrvPaymentV2Url),
                    firstNonBlank(ccdScriptUrl, o.ccdScriptUrl)
            );
        }

        private static String firstNonBlank(String a, String b) {
            return notBlank(a) ? a : b;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** ChillPay 계열 행 중 자격이 있는 행 — 코드 {@code CHILLPAY} 우선, 없으면 키가 채워진 첫 행 */
    private Optional<Config> configFromPgAgencyChillPay(ChillPayAgencyUrlOverrides urlOv) {
        List<PgAgency> all = pgAgencyRepository.findAllByOrderByPgCdAsc();
        Optional<PgAgency> pick = all.stream()
                .filter(a -> a.getPgCd() != null && isChillPayFamilyPgCd(a.getPgCd()))
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .filter(a -> a.getApiKey() != null && !a.getApiKey().isBlank()
                        && a.getMd5SecretKey() != null && !a.getMd5SecretKey().isBlank())
                .min(Comparator
                        .comparing((PgAgency a) -> "CHILLPAY".equalsIgnoreCase(a.getPgCd().trim()) ? 0 : 1)
                        .thenComparing(PgAgency::getPgCd, Comparator.nullsLast(String::compareToIgnoreCase)));
        if (pick.isEmpty()) {
            return Optional.empty();
        }
        PgAgency a = pick.get();
        String mc = (a.getMerchantMid() != null && !a.getMerchantMid().isBlank())
                ? a.getMerchantMid().trim()
                : props.getMerchantCode();
        int routeNo = a.getRouteNo() != null ? a.getRouteNo() : props.getRouteNo();
        boolean sandbox = a.getSandboxYn() == null || !"N".equalsIgnoreCase(a.getSandboxYn().trim());
        return Optional.of(new Config(mc, a.getApiKey().trim(), a.getMd5SecretKey().trim(), routeNo, sandbox, urlOv));
    }

    /** 본사설정(API 구성 세팅) 또는 application.yml에서 ChillPay 설정 조회 */
    private Config resolveConfigFromHq(ChillPayAgencyUrlOverrides urlOv) {
        Optional<Config> fromAgency = configFromPgAgencyChillPay(urlOv);
        if (fromAgency.isPresent()) {
            return fromAgency.get();
        }
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        if (opt.isPresent()) {
            HqApiConfig c = opt.get();
            String apiKey = (c.getChillpayApiKey() != null && !c.getChillpayApiKey().isEmpty()) ? c.getChillpayApiKey() : props.getApiKey();
            String md5Key = (c.getChillpayMd5Key() != null && !c.getChillpayMd5Key().isEmpty()) ? c.getChillpayMd5Key() : props.getMd5Key();
            String merchantCode = (c.getChillpayMerchantCode() != null && !c.getChillpayMerchantCode().isEmpty()) ? c.getChillpayMerchantCode() : props.getMerchantCode();
            int routeNo = (c.getChillpayRouteNo() != null) ? c.getChillpayRouteNo() : props.getRouteNo();
            boolean sandbox = !"N".equalsIgnoreCase(c.getChillpaySandbox());
            return new Config(merchantCode, apiKey, md5Key, routeNo, sandbox, urlOv);
        }
        return new Config(props.getMerchantCode(), props.getApiKey(), props.getMd5Key(), props.getRouteNo(), props.isSandbox(), urlOv);
    }

    private record Config(String merchantCode, String apiKey, String md5Key, int routeNo, boolean sandbox,
                          ChillPayAgencyUrlOverrides urlOv) {
        Config {
            urlOv = urlOv != null ? urlOv : ChillPayAgencyUrlOverrides.empty();
        }

        String getCcdScriptUrl() {
            if (notBlank(urlOv.ccdScriptUrl())) {
                return urlOv.ccdScriptUrl().trim();
            }
            return sandbox ? CCD_SCRIPT_SANDBOX : CCD_SCRIPT_PROD;
        }

        String getPaymentApiUrl() {
            if (notBlank(urlOv.paymentApiUrl())) {
                return urlOv.paymentApiUrl().trim();
            }
            return (sandbox ? DIRECT_CREDIT_SANDBOX : DIRECT_CREDIT_PROD) + "/api/v1/payment";
        }

        String getRedirectPaymentPageUrl() {
            if (notBlank(urlOv.redirectPaymentPageUrl())) {
                String r = urlOv.redirectPaymentPageUrl().trim();
                return r.endsWith("/") ? r : r + "/";
            }
            return sandbox ? REDIRECT_PAYMENT_SANDBOX : REDIRECT_PAYMENT_PROD;
        }

        String getAppsrvPaymentV2Url() {
            if (notBlank(urlOv.appsrvPaymentV2Url())) {
                String b = urlOv.appsrvPaymentV2Url().trim();
                return b.endsWith("/") ? b : b + "/";
            }
            return sandbox ? APPSRV_PAYMENT_V2_SANDBOX : APPSRV_PAYMENT_V2_PROD;
        }
    }

    /**
     * ChillPay DirectCredit 결제 API 호출.
     */
    /** merchantOrgUnitId: 가맹점 등록 시 결제대행사 설정에 ChillPay를 운영대상으로 등록한 경우 해당 가맹점 설정 사용 */
    /**
     * @param langCode UI 언어(KOR/ENG/CHN/JPN/THA 등) → ChillPay LangCode(KO/EN/ZH/JA/TH 등)로 매핑
     */
    public ChillPayDirectCreditResponse requestPayment(
            String orderNo, String customerId, Long amount, String directCreditToken,
            String phoneNumber, String description, String ipAddress, String custEmail,
            Long merchantOrgUnitId, String langCode) {

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            throw new IllegalStateException("서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)");
        }

        Config cfg = resolveConfig(merchantOrgUnitId);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다. 본사설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다. 본사설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }

        ChillPayDirectCreditRequest req = new ChillPayDirectCreditRequest();
        req.setOrderNo(orderNo != null ? orderNo : "ORD" + System.currentTimeMillis());
        req.setCustomerId(customerId != null ? customerId : "guest");
        req.setAmount(amount != null ? amount : 0L);
        req.setDirectCreditToken(directCreditToken);
        req.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        req.setDescription(description != null ? description : "");
        req.setRouteNo(cfg.routeNo());
        req.setIPAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        req.setCustEmail(custEmail != null ? custEmail : "");
        req.setLangCode(toChillPayLangCode(langCode));

        String concat = req.toConcatString();
        String checkSum = md5(concat + cfg.md5Key());
        req.setCheckSum(checkSum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", cfg.apiKey());
        headers.set("Merchant-Code", cfg.merchantCode());

        HttpEntity<ChillPayDirectCreditRequest> entity = new HttpEntity<>(req, headers);
        String url = cfg.getPaymentApiUrl();

        log.info("ChillPay 요청: {} orderNo={} amount={}", url, req.getOrderNo(), req.getAmount());

        try {
            ResponseEntity<ChillPayDirectCreditResponse> res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChillPayDirectCreditResponse.class
            );
            ChillPayDirectCreditResponse body = res.getBody();
            if (body != null && body.getData() != null) {
                log.info("ChillPay 응답: status={} paymentStatus={}", body.getStatus(), body.getData().getPaymentStatus());
            }
            return body;
        } catch (Exception e) {
            log.error("ChillPay API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("ChillPay 결제 요청 실패: " + e.getMessage(), e);
        }
    }

    /** ChillPay DirectCredit Table 1.3 LangCode (EN/KO/JA/ZH 등 매뉴얼 기준) */
    static String toChillPayLangCode(String uiLang) {
        if (uiLang == null || uiLang.isBlank()) {
            return "EN";
        }
        String u = uiLang.trim().toUpperCase();
        return switch (u) {
            case "KOR", "KO", "KR" -> "KO";
            case "CHN", "ZH", "CN" -> "ZH";
            case "JPN", "JA", "JP", "JPY" -> "JA";
            case "THA", "TH", "THAI" -> "TH";
            case "ENG", "EN" -> "EN";
            default -> "EN";
        };
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    /** 결제 페이지용 설정. merchantOrgUnitId 있으면 해당 가맹점 ChillPay 설정 사용 */
    public Map<String, Object> getConfigForFrontend(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId);
        return Map.of(
                "ccdScriptUrl", cfg.getCcdScriptUrl(),
                "directCreditApiUrl", cfg.getPaymentApiUrl(),
                "redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl(),
                "paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url(),
                "merchantCode", cfg.merchantCode(),
                "routeNo", cfg.routeNo(),
                "sandbox", cfg.sandbox()
        );
    }

    /**
     * 공개 URL 결제 페이지용: 본사 {@link HqApiConfig} 기준 인라인/리다이렉트·폼 모드 및 ChillPay URL 안내.
     * (가맹점별 오버라이드 없음 — URL 결제 정책은 본사 단일 설정.)
     */
    public Map<String, Object> getUrlPayPresentationForCheckout(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId);
        Map<String, Object> m = new LinkedHashMap<>();
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        if (opt.isEmpty()) {
            m.put("urlPayFlow", "INLINE");
            m.put("urlPayFormMode", "FULL");
        } else {
            HqApiConfig c = opt.get();
            m.put("urlPayFlow", effectiveUrlPayFlow(c));
            m.put("urlPayFormMode", effectiveUrlPayFormMode(c));
        }
        m.put("redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl());
        m.put("paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url());
        m.put("ccdScriptUrl", cfg.getCcdScriptUrl());
        return m;
    }

    /** URL 결제 기본 방식과 INLINE/REDIRECT 제공 여부를 반영한 실효 방식 */
    static String effectiveUrlPayFlow(HqApiConfig c) {
        String def = c.getUrlPayDefaultFlowType() != null ? c.getUrlPayDefaultFlowType().trim() : "REDIRECT";
        boolean inlineOk = !"N".equalsIgnoreCase(c.getUrlPayInlineEnabledYn());
        boolean redirectOk = !"N".equalsIgnoreCase(c.getUrlPayRedirectEnabledYn());
        if ("INLINE".equalsIgnoreCase(def)) {
            if (inlineOk) {
                return "INLINE";
            }
            return redirectOk ? "REDIRECT" : "INLINE";
        }
        if ("REDIRECT".equalsIgnoreCase(def)) {
            if (redirectOk) {
                return "REDIRECT";
            }
            return inlineOk ? "INLINE" : "REDIRECT";
        }
        return "INLINE";
    }

    static String effectiveUrlPayFormMode(HqApiConfig c) {
        String fm = c.getUrlPayFormMode();
        if (fm == null || fm.isBlank()) {
            return "FULL";
        }
        return "SIMPLE".equalsIgnoreCase(fm.trim()) ? "SIMPLE" : "FULL";
    }
}
