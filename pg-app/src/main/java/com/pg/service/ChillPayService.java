package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.config.ChillPayProperties;
import com.pg.dto.ChillPayDirectCreditRequest;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.dto.ChillPayPaymentSearchApiRequest;
import com.pg.dto.ChillPayPaymentSearchApiResponse;
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

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ChillPay DirectCredit API 연동 서비스.
 * <p>
 * <strong>URL 결제</strong>는 연동용도가 URL결제({@code integ_url_pay_yn=Y})인 {@code tb_pg_agency} 행과 같은 {@code pg_cd}의
 * 가맹점 바인딩을 운영 행 후보 중에서 <strong>최우선</strong>으로 고른 뒤, 그 행에 입력된 엔드포인트·샌드박스·루트를 따른다(다른 PG 코드 URL과 병합하지 않음).
 * <p>
 * <strong>ApiKey·MD5(IV)</strong>는 가맹점 {@code tb_merchant_pg_binding} 행에서 쓴다.
 * <strong>MID·Route</strong>는 가맹점 바인딩 → 동일 {@code pg_cd}의 {@code tb_pg_agency} 순이다.
 * 동일 PG 행에 MID가 없을 때 <strong>다른</strong> PG 행·본사 설정에서 가져온 MID를 바인딩 Api-Key와 섞지 않는다(ChillPay 1002 Invalid MerchantCode 방지).
 * {@code pg_cd} 매칭 행이 없을 때만 본사 {@code resolveConfigFromHq}의 MID를 쓴다. Route는 동일 PG 행이 있으나 비어 있으면 {@code application.yml} 기본 route를 쓴다.
 * 바인딩만 있고 동일 {@code pg_cd}의 PG사 API 행이 없거나 ChillPay가 아니면, 본사 ChillPay 계열 PG 병합·{@code tb_hq_api_config}·yml 순으로 폴백한다.
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
    /** Transaction Services — Search Payment Transaction (Table 1.1~1.3) */
    private static final String TXN_PAYMENT_SEARCH_SB = "https://sandbox-api-transaction.chillpay.co/api/v1/payment/search";
    private static final String TXN_PAYMENT_SEARCH_PR = "https://api-transaction.chillpay.co/api/v1/payment/search";
    private static final DateTimeFormatter CHILLPAY_TXN_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

    /**
     * 가맹점 ChillPay 바인딩이 있으면 자격은 바인딩; 샌드박스·route·ChillPay URL은 동일 {@code pg_cd}의 {@link PgAgency} 한 행만 사용(타 PG 행 URL 병합 없음).
     */
    private Config resolveConfig(Long merchantOrgUnitId) {
        ChillPayAgencyUrlOverrides globalUrlOv = loadChillPayAgencyUrlOverrides();
        if (merchantOrgUnitId == null) {
            return resolveConfigFromHq(globalUrlOv);
        }
        Optional<MerchantPgBinding> bindingOpt = findOperationalChillPayFamilyBinding(merchantOrgUnitId);
        if (bindingOpt.isEmpty()) {
            return resolveConfigFromHq(globalUrlOv);
        }
        MerchantPgBinding b = bindingOpt.get();
        Optional<PgAgency> agencyForPgCd = resolvePgAgencyForMerchantBinding(b);
        ChillPayAgencyUrlOverrides perPgCdUrls = agencyForPgCd.map(this::urlOverridesFromSinglePgAgency)
                .orElse(ChillPayAgencyUrlOverrides.empty());
        /* URL 결제: 동일 pg_cd의 API연동 행에만 정의된 URL 사용. 타 ChillPay PG 행과 병합하지 않음(미파싱 항목은 샌드박스별 기본 URL). */
        ChillPayAgencyUrlOverrides urlOv = agencyForPgCd.isPresent() ? perPgCdUrls : globalUrlOv;

        String ak = (b.getApiKey() != null && !b.getApiKey().isEmpty()) ? b.getApiKey().trim() : null;
        String mk = (b.getIvKey() != null && !b.getIvKey().isEmpty()) ? b.getIvKey().trim() : null;
        if (ak == null || mk == null) {
            return resolveConfigFromHq(globalUrlOv);
        }
        Config hqRef = resolveConfigFromHq(globalUrlOv);
        String mc = resolveMerchantMidForUrlPay(b, agencyForPgCd, hqRef);
        if (mc == null || mc.isEmpty()) {
            if (agencyForPgCd.isPresent()) {
                log.warn(
                        "orgUnitId={}: ChillPay MID empty on merchant binding and on tb_pg_agency pg_cd={}. "
                                + "Fill MID on one of them (do not pair merchant Api-Key with another row's MID). Using full HQ ChillPay credentials.",
                        merchantOrgUnitId, b.getPgCd());
            } else {
                log.warn(
                        "orgUnitId={}: No matching tb_pg_agency for pg_cd={}; ChillPay MID missing on binding. Using HQ ChillPay credentials only.",
                        merchantOrgUnitId, b.getPgCd());
            }
            return resolveConfigFromHq(globalUrlOv);
        }
        int routeNo = parseBindingRouteNo(b.getRootNo())
                .orElseGet(() -> agencyForPgCd.flatMap(a -> Optional.ofNullable(a.getRouteNo()))
                        .orElseGet(() -> agencyForPgCd.isPresent() ? props.getRouteNo() : hqRef.routeNo()));
        boolean sandbox = agencyForPgCd
                .map(a -> a.getSandboxYn() == null || !"N".equalsIgnoreCase(a.getSandboxYn().trim()))
                .orElse(hqRef.sandbox());
        return new Config(mc, ak, mk, routeNo, sandbox, urlOv);
    }

    /** 바인딩 {@code pg_cd} 와 일치하고 사용 Y인 ChillPay 계열 {@link PgAgency} (가맹점이 선택한 PG 연동 정의). */
    private Optional<PgAgency> resolvePgAgencyForMerchantBinding(MerchantPgBinding b) {
        if (b.getPgCd() == null || b.getPgCd().isBlank()) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(b.getPgCd().trim())
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .filter(a -> isChillPayFamilyPgCd(a.getPgCd()));
    }

    /**
     * 한 건 PG사 API 행만 파싱. 연동용도가 URL결제이면 {@code endpoint_url_pay} 등 URL 필드를 먼저 적용해
     * API 전용 필드와 겹칠 때 URL 결제 연동에 맞는 값이 우선한다.
     */
    private ChillPayAgencyUrlOverrides urlOverridesFromSinglePgAgency(PgAgency a) {
        Objects.requireNonNull(a);
        boolean urlPayRow = a.getIntegUrlPayYn() != null && "Y".equalsIgnoreCase(a.getIntegUrlPayYn().trim());
        String[] fields = urlPayRow
                ? new String[] { a.getEndpointUrlPay(), a.getApiEndpoint(), a.getEndpointNoti(), a.getEndpointApi() }
                : new String[] { a.getEndpointApi(), a.getApiEndpoint(), a.getEndpointUrlPay(), a.getEndpointNoti() };
        ChillPayAgencyUrlOverrides acc = ChillPayAgencyUrlOverrides.empty();
        for (String raw : fields) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            acc = acc.merge(parseChillPayApiEndpoint(raw.trim()));
        }
        return acc;
    }

    private static Optional<Integer> parseBindingRouteNo(String rootNo) {
        if (rootNo == null || rootNo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(rootNo.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * ChillPay Merchant-Code: 가맹점 바인딩 MID → (동일 {@code pg_cd} 행이 있으면) 그 행의 {@link PgAgency#merchantMid} 만.
     * 그 행이 없을 때만 본사 {@link Config#merchantCode()} — 다른 PG 코드 행의 MID는 바인딩 키와 섞지 않는다.
     */
    private static String resolveMerchantMidForUrlPay(
            MerchantPgBinding b, Optional<PgAgency> agencyForPgCd, Config hqRef) {
        if (b.getMid() != null && !b.getMid().isBlank()) {
            return b.getMid().trim();
        }
        if (agencyForPgCd.isPresent()) {
            return agencyForPgCd
                    .map(PgAgency::getMerchantMid)
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .orElse(null);
        }
        if (hqRef.merchantCode() != null && !hqRef.merchantCode().isBlank()) {
            return hqRef.merchantCode();
        }
        return null;
    }

    public static boolean isChillPayFamilyPgCd(String pgCd) {
        if (pgCd == null) {
            return false;
        }
        String u = pgCd.trim().toUpperCase(Locale.ROOT);
        return u.equals("CHILLPAY") || u.startsWith("CHILLPAY");
    }

    /**
     * 공개 URL 결제: 웹·운영(Y) PG 바인딩 1건.
     * {@code tb_pg_agency} 에서 연동용도 URL결제(Y)인 {@code pg_cd} 를 먼저 고르고, 그다음 ChillPay 계열 우선.
     */
    public Optional<MerchantPgBinding> findOperationalWebBindingForUrlPay(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Map<String, Boolean> urlPayByPgCd = urlPayAgencyFlagByPgCd(list);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && !b.getPgCd().isBlank())
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .min(Comparator
                        .comparing((MerchantPgBinding b) -> Boolean.TRUE.equals(urlPayByPgCd.get(pgCdKey(b))) ? 0 : 1)
                        .thenComparing((MerchantPgBinding b) -> isChillPayFamilyPgCd(b.getPgCd()) ? 0 : 1)
                        .thenComparing(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE)
                        .thenComparing(MerchantPgBinding::getId));
    }

    /**
     * 인라인 카드 위젯 종류(다중 PG 확장용). ChillPay만 CCD 연동 완료, 그 외는 프론트에서 위젯 슬롯만 비우고 안내.
     */
    public static String resolveUrlPayInlineWidgetKind(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return "UNSUPPORTED_INLINE";
        }
        return isChillPayFamilyPgCd(pgCd) ? "CHILLPAY_CCD" : "UNSUPPORTED_INLINE";
    }

    /**
     * URL 결제 시 운영 PG 코드({@code pg_cd}) — {@link #getUrlPayPresentationForCheckout} 의 {@code urlPayOperationalPgCd} 와 동일 규칙.
     * 결제통화 스케일 규칙 매칭·API 스케일링에 사용.
     */
    public String resolveUrlPayOperationalPgCd(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return "";
        }
        Optional<MerchantPgBinding> webOp = findOperationalWebBindingForUrlPay(merchantOrgUnitId);
        if (webOp.isPresent()) {
            String cd = webOp.get().getPgCd();
            return cd != null ? cd.trim() : "";
        }
        Config cfg = resolveConfig(merchantOrgUnitId);
        boolean canHqChillPay = cfg.apiKey() != null && !cfg.apiKey().isBlank()
                && cfg.merchantCode() != null && !cfg.merchantCode().isBlank();
        return canHqChillPay ? "CHILLPAY" : "";
    }

    /**
     * 운영(Y) ChillPay 계열 바인딩 — API연동설정에서 연동용도 URL결제인 {@code pg_cd} 를 최우선, 다음 WEB(또는 결제구분 미입력), 그다음 기타 결제구분.
     */
    private Optional<MerchantPgBinding> findOperationalChillPayFamilyBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Map<String, Boolean> urlPayByPgCd = urlPayAgencyFlagByPgCd(list);
        Optional<MerchantPgBinding> web = pickOperationalChillPayBindingRow(list, true, urlPayByPgCd);
        return web.isPresent() ? web : pickOperationalChillPayBindingRow(list, false, urlPayByPgCd);
    }

    private Optional<MerchantPgBinding> pickOperationalChillPayBindingRow(
            List<MerchantPgBinding> list, boolean webOnly, Map<String, Boolean> urlPayByPgCd) {
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && isChillPayFamilyPgCd(b.getPgCd()))
                .filter(b -> !webOnly || isWebOrUnsetPayMethod(b.getPayMethod()))
                .min(Comparator
                        .comparing((MerchantPgBinding b) -> Boolean.TRUE.equals(urlPayByPgCd.get(pgCdKey(b))) ? 0 : 1)
                        .thenComparing((MerchantPgBinding b) -> "CHILLPAY".equalsIgnoreCase(b.getPgCd().trim()) ? 0 : 1)
                        .thenComparing(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE)
                        .thenComparing(MerchantPgBinding::getId));
    }

    /** 바인딩 목록에 나온 {@code pg_cd} 별로, 사용 Y인 {@code tb_pg_agency} 행의 URL결제 연동 여부. */
    private Map<String, Boolean> urlPayAgencyFlagByPgCd(List<MerchantPgBinding> list) {
        Map<String, Boolean> m = new HashMap<>();
        for (MerchantPgBinding b : list) {
            String k = pgCdKey(b);
            if (k.isEmpty()) {
                continue;
            }
            m.computeIfAbsent(k, this::loadAgencyIntegUrlPayYn);
        }
        return m;
    }

    private boolean loadAgencyIntegUrlPayYn(String pgCd) {
        return pgAgencyRepository.findByPgCd(pgCd)
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .map(a -> "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : ""))
                .orElse(false);
    }

    private static String pgCdKey(MerchantPgBinding b) {
        if (b == null || b.getPgCd() == null || b.getPgCd().isBlank()) {
            return "";
        }
        return b.getPgCd().trim();
    }

    private static boolean isWebOrUnsetPayMethod(String payMethod) {
        if (payMethod == null || payMethod.isBlank()) {
            return true;
        }
        return "WEB".equalsIgnoreCase(payMethod.trim());
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

    /**
     * ChillPay CCD 번들은 {@code document.querySelector('script[src^="…"]')} 로 자기 script 태그를 찾는다.
     * 프로덕션: {@code https://cdn.chill.credit} · 샌드박스: {@code https://sandbox-bankdemo3.chillpay.co}.
     * PG사 엔드포인트에 잘못된 {@code ccdpayment.js} URL이 있으면 초기화가 되지 않아 iframe이 비어 보인다.
     */
    private static String normalizeChillPayCcdScriptUrl(String url, boolean sandbox) {
        if (url == null || url.isBlank()) {
            return sandbox ? CCD_SCRIPT_SANDBOX : CCD_SCRIPT_PROD;
        }
        String u = url.trim();
        if (!u.toLowerCase(Locale.ROOT).contains("ccdpayment.js")) {
            return u;
        }
        if (sandbox) {
            if (u.startsWith("https://sandbox-bankdemo3.chillpay.co")) {
                return u;
            }
            log.warn("ChillPay CCD script URL does not match sandbox bundle prefix; using {}. Configured: {}",
                    CCD_SCRIPT_SANDBOX, url);
            return CCD_SCRIPT_SANDBOX;
        }
        if (u.startsWith("https://cdn.chill.credit")) {
            return u;
        }
        log.warn("ChillPay CCD script URL does not match production bundle prefix (cdn.chill.credit); using {}. Configured: {}",
                CCD_SCRIPT_PROD, url);
        return CCD_SCRIPT_PROD;
    }

    private record Config(String merchantCode, String apiKey, String md5Key, int routeNo, boolean sandbox,
                          ChillPayAgencyUrlOverrides urlOv) {
        Config {
            urlOv = urlOv != null ? urlOv : ChillPayAgencyUrlOverrides.empty();
            merchantCode = merchantCode != null ? merchantCode.trim() : null;
            apiKey = apiKey != null ? apiKey.trim() : null;
            md5Key = md5Key != null ? md5Key.trim() : null;
        }

        String getCcdScriptUrl() {
            if (notBlank(urlOv.ccdScriptUrl())) {
                return normalizeChillPayCcdScriptUrl(urlOv.ccdScriptUrl().trim(), sandbox);
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
     * @param langCode UI 언어(KOR/ENG/CHN/JPN/THA 등) → ChillPay LangCode(한국어는 EN, 나머지 EN/TH/JA/ZH)로 매핑
     */
    /**
     * @param checkoutCurrencyCode 가맹점 checkout 통화(예: JPY, THB) 또는 ISO 4217 숫자(392). null/공백이면 JPY(392).
     */
    /**
     * @param browserReturnUrl ChillPay 호스티드(WaitAuthorize) 단계 후 브라우저 복귀 URL. 비우면 미전송.
     */
    public ChillPayDirectCreditResponse requestPayment(
            String orderNo, String customerId, BigDecimal amount, String directCreditToken,
            String phoneNumber, String description, String ipAddress, String custEmail,
            Long merchantOrgUnitId, String langCode, String checkoutCurrencyCode,
            String browserReturnUrl) {

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
        req.setAmount(amount != null ? amount : BigDecimal.ZERO);
        req.setDirectCreditToken(directCreditToken);
        /* NOTI /admin/test-pay/submit: PhoneNumber 기본값 */
        String phone = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber.trim() : "0911111111";
        req.setPhoneNumber(phone);
        req.setDescription(description != null ? description : "");
        req.setRouteNo(cfg.routeNo());
        req.setIPAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        /* NOTI /admin/test-pay/submit: CustEmail 기본값 */
        String email = (custEmail != null && !custEmail.isBlank()) ? custEmail.trim() : "test@sample.com";
        req.setCustEmail(email);
        req.setLangCode(toChillPayLangCode(langCode));
        req.setCurrency(toChillPayCurrencyNumeric(checkoutCurrencyCode));

        String concat = req.toConcatString();
        String checkSum = md5(concat + cfg.md5Key());
        req.setCheckSum(checkSum);

        if (browserReturnUrl != null && !browserReturnUrl.isBlank()) {
            req.setReturnUrl(browserReturnUrl.trim());
            log.info("ChillPay DirectCredit ReturnUrl set (browser return after hosted step if supported)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        /* ziobiz/NOTI ChillPay DirectCredit 호출과 동일 헤더명 */
        headers.set("CHILLPAY-MerchantCode", cfg.merchantCode());
        headers.set("CHILLPAY-ApiKey", cfg.apiKey());

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

    /**
     * NOTI 테스트 설정·ChillPay Table 1.3 의 Currency 문자열 코드(392=JPY, 764=THB 등).
     * 알파벳 통화(JPY, THB, …)는 숫자로 치환하고, 이미 숫자만이면 그대로 둔다.
     */
    static String toChillPayCurrencyNumeric(String checkoutCurrencyCode) {
        if (checkoutCurrencyCode == null || checkoutCurrencyCode.isBlank()) {
            return "392";
        }
        String s = checkoutCurrencyCode.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            return s;
        }
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "JPY" -> "392";
            case "THB" -> "764";
            case "USD" -> "840";
            case "KRW", "KOR", "WON" -> "410";
            case "CNY", "RMB" -> "156";
            case "EUR" -> "978";
            case "GBP" -> "826";
            case "VND" -> "704";
            default -> "392";
        };
    }

    /**
     * ChillPay DirectCredit Table 1.3 LangCode.
     * 운영에서 {@code KO}(한국어) 전달 시 응답 {@code 1010 Invalid Language Code} 가 나는 경우가 있어,
     * 한국어 UI는 그대로 두고 API에는 {@code EN}으로 보냅니다(결제 페이지 문구는 자사 pay.html 담당).
     */
    static String toChillPayLangCode(String uiLang) {
        if (uiLang == null || uiLang.isBlank()) {
            return "EN";
        }
        String u = uiLang.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "KOR", "KO", "KR" -> "EN";
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

    /**
     * 결제 페이지용 설정. URL·샌드박스는 {@link #resolveConfig(Long)}(가맹점 {@code pg_cd} → {@code tb_pg_agency}) 기준.
     * {@code apiKey}는 CCD 스크립트 {@code data-api-key}용으로 브라우저에 내려감.
     */
    public Map<String, Object> getConfigForFrontend(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ccdScriptUrl", cfg.getCcdScriptUrl());
        m.put("directCreditApiUrl", cfg.getPaymentApiUrl());
        m.put("redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl());
        m.put("paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url());
        m.put("merchantCode", cfg.merchantCode() != null ? cfg.merchantCode() : "");
        m.put("apiKey", cfg.apiKey() != null ? cfg.apiKey() : "");
        m.put("routeNo", cfg.routeNo());
        m.put("sandbox", cfg.sandbox());
        return m;
    }

    /**
     * 공개 URL 결제 페이지용: {@link HqApiConfig}의 인라인/리다이렉트·폼 모드 플래그와,
     * {@link #resolveConfig(Long)}에 따른 ChillPay URL(ccd·DirectCredit·리다이렉트·appsrv) — 가맹점 바인딩 {@code pg_cd}의 {@code tb_pg_agency} 내용.
     * {@code urlPayOperationalPgCd} / {@code urlPayInlineWidgetKind} 는 운영 WEB 바인딩 기준(연동용도 URL결제 PG 우선).
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
        String opPgCd = resolveUrlPayOperationalPgCd(merchantOrgUnitId);
        String widgetKind = resolveUrlPayInlineWidgetKind(opPgCd);
        m.put("urlPayOperationalPgCd", opPgCd);
        m.put("urlPayInlineWidgetKind", widgetKind);
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

    /**
     * URL 결제 복귀(결과) 페이지 전체 URL. ChillPay 머천트 포털의 Result URL·DirectCredit ReturnUrl에 동일하게 등록 가능.
     * 우선순위: {@code public_api_base_url} → {@code public_admin_site_url} → 현재 요청의 scheme/host.
     */
    public String resolveUrlPayResultAbsolute(HttpServletRequest request, String compId) {
        if (request == null) {
            return "";
        }
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        HqApiConfig cfg = opt.orElse(null);
        String base = "";
        if (cfg != null && cfg.getPublicApiBaseUrl() != null && !cfg.getPublicApiBaseUrl().isBlank()) {
            base = cfg.getPublicApiBaseUrl().trim();
        } else if (cfg != null && cfg.getPublicAdminSiteUrl() != null && !cfg.getPublicAdminSiteUrl().isBlank()) {
            base = cfg.getPublicAdminSiteUrl().trim();
        } else {
            base = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443 && port > 0) {
                base += ":" + port;
            }
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = (cfg != null && cfg.getChillpayUrlResultPath() != null && !cfg.getChillpayUrlResultPath().isBlank())
                ? cfg.getChillpayUrlResultPath().trim()
                : "/pay-result.html";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String url = base + path;
        if (compId != null && !compId.isBlank()) {
            String enc = URLEncoder.encode(compId.trim(), StandardCharsets.UTF_8);
            url += (url.contains("?") ? "&" : "?") + "m=" + enc;
        }
        return url;
    }

    /**
     * ChillPay Transaction API — Search Payment Transaction (실시간 칠페이 결제 거래 목록).
     * 본사·가맹 {@link #resolveConfig(Long)} 자격(MerchantCode·ApiKey·MD5)으로 호출합니다.
     *
     * @param merchantOrgUnitId null이면 본사(HQ) 설정만 사용
     */
    public PageResult<Map<String, Object>> searchChillPayPaymentTransactions(
            Long merchantOrgUnitId,
            int page,
            int size,
            String orderBy,
            String orderDir,
            String searchKeyword,
            String merchantCodeFilter,
            String paymentChannel,
            Integer routeNoFilter,
            String orderNo,
            String status,
            LocalDate transactionDateFrom,
            LocalDate transactionDateTo) {

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            throw new IllegalStateException("서비스가 중지된 업체입니다.");
        }
        Config cfg = resolveConfig(merchantOrgUnitId);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다.");
        }

        int ps = Math.min(100, Math.max(1, size));
        int pn = Math.max(1, page);

        ChillPayPaymentSearchApiRequest req = new ChillPayPaymentSearchApiRequest();
        req.setOrderBy(trimOrDefault(orderBy, "TransactionId"));
        req.setOrderDir("ASC".equalsIgnoreCase(trimOrEmpty(orderDir)) ? "ASC" : "DESC");
        req.setPageSize(ps);
        req.setPageNumber(pn);
        req.setSearchKeyword(trimOrEmpty(searchKeyword));
        req.setMerchantCode(trimOrEmpty(merchantCodeFilter));
        req.setPaymentChannel(trimOrEmpty(paymentChannel));
        req.setRouteNo(routeNoFilter);
        req.setOrderNo(trimOrEmpty(orderNo));
        req.setStatus(trimOrEmpty(status));
        req.setTransactionDateFrom(transactionDateFrom != null ? transactionDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : "");
        req.setTransactionDateTo(transactionDateTo != null ? transactionDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : "");
        req.setPaymentDateFrom("");
        req.setPaymentDateTo("");

        req.setChecksum(md5(req.toChecksumPlainString() + cfg.md5Key()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("CHILLPAY-MerchantCode", cfg.merchantCode());
        headers.set("CHILLPAY-ApiKey", cfg.apiKey());

        String url = cfg.sandbox() ? TXN_PAYMENT_SEARCH_SB : TXN_PAYMENT_SEARCH_PR;
        HttpEntity<ChillPayPaymentSearchApiRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<ChillPayPaymentSearchApiResponse> res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChillPayPaymentSearchApiResponse.class);
            ChillPayPaymentSearchApiResponse body = res.getBody();
            if (body == null) {
                throw new IllegalStateException("ChillPay 응답 본문이 비어 있습니다.");
            }
            if (body.getStatus() != null && body.getStatus() != 200) {
                String msg = body.getMessage() != null ? body.getMessage() : ("상태코드 " + body.getStatus());
                throw new IllegalStateException(msg);
            }
            List<Map<String, Object>> raw = body.getData() != null ? body.getData() : Collections.emptyList();
            List<Map<String, Object>> list = new ArrayList<>();
            int startNo = (pn - 1) * ps + 1;
            for (int i = 0; i < raw.size(); i++) {
                list.add(wrapChillPayRow(raw.get(i), startNo + i));
            }
            long total = body.getTotalRecord() != null ? body.getTotalRecord() : 0L;
            int totalPages = total <= 0 ? 1 : (int) Math.ceil((double) total / (double) ps);

            PageResult<Map<String, Object>> pr = new PageResult<>();
            pr.setList(list);
            pr.setPage(pn);
            pr.setSize(ps);
            pr.setTotalElements(total);
            pr.setTotalPages(totalPages);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("chillPayMessage", body.getMessage());
            meta.put("chillPayStatus", body.getStatus());
            pr.setMeta(meta);
            return pr;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("ChillPay Search Payment Transaction 실패: {}", e.getMessage());
            throw new IllegalStateException("ChillPay 거래 검색 API 호출 실패: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> wrapChillPayRow(Map<String, Object> raw, int rowNo) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw != null) {
            m.putAll(raw);
        }
        m.put("rowNo", rowNo);
        aliasIfMissing(m, "transactionId", "TransactionId");
        aliasIfMissing(m, "transactionDate", "TransactionDate");
        aliasIfMissing(m, "merchant", "Merchant");
        aliasIfMissing(m, "customer", "Customer");
        aliasIfMissing(m, "orderNo", "OrderNo");
        aliasIfMissing(m, "paymentChannel", "PaymentChannel");
        aliasIfMissing(m, "paymentDate", "PaymentDate");
        aliasIfMissing(m, "amount", "Amount");
        aliasIfMissing(m, "refundAmount", "RefundAmount");
        aliasIfMissing(m, "fee", "Fee");
        aliasIfMissing(m, "discount", "Discount");
        aliasIfMissing(m, "totalAmount", "TotalAmount");
        aliasIfMissing(m, "currency", "Currency");
        aliasIfMissing(m, "routeNo", "RouteNo");
        aliasIfMissing(m, "status", "Status");
        aliasIfMissing(m, "settled", "Settled");
        aliasIfMissing(m, "description", "Description");
        return m;
    }

    private static void aliasIfMissing(Map<String, Object> m, String lowerKey, String pascalKey) {
        if (m.containsKey(lowerKey)) {
            return;
        }
        if (m.containsKey(pascalKey)) {
            m.put(lowerKey, m.get(pascalKey));
        }
    }

    private static String trimOrEmpty(String s) {
        return s != null ? s.trim() : "";
    }

    private static String trimOrDefault(String s, String def) {
        String t = trimOrEmpty(s);
        return t.isEmpty() ? def : t;
    }
}
