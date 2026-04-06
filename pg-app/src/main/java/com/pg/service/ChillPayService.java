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
import com.pg.util.PayListStatusBarBuckets;
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
import java.net.URI;
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
 * <strong>MID</strong>는 가맹점 바인딩 → 동일 {@code pg_cd}의 {@code tb_pg_agency} 순이다(자세한 규칙은 {@link #resolveMerchantMidForUrlPay}).
 * 동일 PG 행에 MID가 없을 때 <strong>다른</strong> PG 행·본사 설정에서 가져온 MID를 바인딩 Api-Key와 섞지 않는다(ChillPay 1002 Invalid MerchantCode 방지).
 * {@code pg_cd} 매칭 행이 없을 때만 본사 {@link #resolveHqFallbackRef} 의 MID·샌드박스를 쓴다.
 * <p><strong>ChillPay Route (결제 요청):</strong> (1) 가맹점 결제대행사 등록 {@code root_no} 우선, (2) 없으면 동일 {@code pg_cd} 의 {@code tb_pg_agency.route_no}. 둘 다 없으면 {@link IllegalStateException} — {@code application.yml} 로 대체하지 않는다.
 * 본사 전용 경로({@link #resolveConfigFromHq})는 tb_pg_agency 자격(키) 행의 {@code route_no} 또는 {@code tb_hq_api_config.chillpayRouteNo} 가 필수이며, 없으면 예외.
 * 운영 ChillPay 바인딩이 여럿이면 URL결제(Y) 행을 먼저 고르고, 그중에서도 {@code CHILLPAY_…} 확장 코드를 {@code CHILLPAY} 일반보다 우선한다.
 * 바인딩만 있고 동일 {@code pg_cd}의 PG사 API 행이 없거나 ChillPay가 아니면, 본사 ChillPay 계열·{@code tb_hq_api_config} 순으로 자격(MID·키)만 폴백한다.
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
    /** 통합내역 상단 요약: 최대 추가 ChillPay API 호출 페이지 수(페이지당 최대 100건) */
    private static final int CHILL_STATUS_BAR_MAX_PAGES = 30;

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

    /** 가맹점 결제 조합 시 MID·샌드박스 보조 (Route 확정 전에 조회 가능) */
    private record HqFallbackRef(String merchantCode, boolean sandbox) {}

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
            throw new IllegalStateException(
                    "이 가맹점에 URL 결제로 사용할 ChillPay 계열 결제대행사(운영)가 없습니다. "
                            + "가맹점 등록에서 웹(WEB) 운영 PG를 지정하고, API연동설정에서 해당 pg_cd 행의 연동용도에 「URL결제」를 반드시 켜야 합니다. "
                            + "(연동용도가 노티만 Y인 PG로는 URL 결제를 진행할 수 없습니다.)");
        }
        MerchantPgBinding b = bindingOpt.get();
        Optional<PgAgency> agencyForPgCd = resolvePgAgencyForMerchantBinding(b);
        ChillPayAgencyUrlOverrides perPgCdUrls = agencyForPgCd.map(this::urlOverridesFromSinglePgAgency)
                .orElse(ChillPayAgencyUrlOverrides.empty());
        /* URL 결제: 동일 pg_cd의 API연동 행에만 정의된 URL 사용. 타 ChillPay PG 행과 병합하지 않음(미파싱 항목은 샌드박스별 기본 URL). */
        ChillPayAgencyUrlOverrides urlOv = agencyForPgCd.isPresent() ? perPgCdUrls : globalUrlOv;

        String ak = trimOrNull(b.getApiKey());
        String mk = trimOrNull(b.getIvKey());
        if (ak == null || mk == null) {
            if (agencyForPgCd.isPresent()) {
                PgAgency ag = agencyForPgCd.get();
                String akAg = trimOrNull(ag.getApiKey());
                String mkAg = trimOrNull(ag.getMd5SecretKey());
                if (akAg != null && mkAg != null) {
                    ak = akAg;
                    mk = mkAg;
                }
            }
        }
        if (ak == null || mk == null) {
            String pgc = b.getPgCd() != null ? b.getPgCd().trim() : "";
            throw new IllegalStateException(
                    "ChillPay API Key·MD5(또는 가맹점 IV)가 비어 있습니다. 가맹점 결제대행사 행에 입력하거나, "
                            + "API연동설정에서 동일 pg_cd(" + pgc + ") 행에 API Key·MD5를 등록하세요. "
                            + "해당 행은 연동용도 「URL결제」가 Y인 행이어야 합니다.");
        }
        HqFallbackRef hqRef = resolveHqFallbackRef();
        String mc = resolveMerchantMidForUrlPay(b, agencyForPgCd, hqRef);
        if (mc == null || mc.isEmpty()) {
            String pgc = b.getPgCd() != null ? b.getPgCd().trim() : "";
            throw new IllegalStateException(
                    "ChillPay MID(Merchant Code)가 비어 있습니다. 가맹점 등록 > 결제대행사에서 해당 PG(" + pgc + ")의 MID를 입력하거나, "
                            + "API연동설정 동일 pg_cd 행의 MID를 채워 주세요.");
        }
        int routeNo = resolveChillPayRouteNoUrlPayContract(merchantOrgUnitId, b, agencyForPgCd);
        boolean sandbox = agencyForPgCd
                .map(a -> a.getSandboxYn() == null || !"N".equalsIgnoreCase(a.getSandboxYn().trim()))
                .orElse(hqRef.sandbox());
        return new Config(mc, ak, mk, routeNo, sandbox, urlOv);
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 바인딩 {@code pg_cd} 와 일치하고 사용 Y인 ChillPay 계열 {@link PgAgency} (가맹점이 선택한 PG 연동 정의). */
    private Optional<PgAgency> resolvePgAgencyForMerchantBinding(MerchantPgBinding b) {
        if (b.getPgCd() == null || b.getPgCd().isBlank()) {
            return Optional.empty();
        }
        String cd = b.getPgCd().trim();
        Optional<PgAgency> direct = pgAgencyRepository.findByPgCd(cd)
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .filter(a -> isChillPayFamilyPgCd(a.getPgCd()));
        if (direct.isPresent()) {
            return direct;
        }
        /* URL 전용·확장 pg_cd만 바인딩에 있고 tb_pg_agency 행이 없을 때: 공통 CHILLPAY(URL결제) 행의 Route·엔드포인트가 적용되도록 (yml 4 하드 폴백 방지) */
        if (!isChillPayFamilyPgCd(cd)) {
            return Optional.empty();
        }
        return findChillPayAgencyFallbackForMissingPgCd(cd);
    }

    /**
     * {@code tb_pg_agency} 에 바인딩 {@code pg_cd} 와 동일한 행이 없을 때,
     * 사용(Y)·연동용도 URL결제(Y)인 ChillPay 계열 행 중 <strong>일반 {@code CHILLPAY}</strong> 를 우선한다.
     * (API연동설정에 Route·MID만 CHILLPAY 행에 두고 가맹점 바인딩은 확장 코드만 쓰는 구성을 지원)
     */
    private Optional<PgAgency> findChillPayAgencyFallbackForMissingPgCd(String requestedPgCd) {
        if (requestedPgCd == null || requestedPgCd.isBlank() || !isChillPayFamilyPgCd(requestedPgCd)) {
            return Optional.empty();
        }
        List<PgAgency> candidates = new ArrayList<>();
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (a.getPgCd() == null || !isChillPayFamilyPgCd(a.getPgCd())) {
                continue;
            }
            if (a.getUseYn() == null || !"Y".equalsIgnoreCase(a.getUseYn().trim())) {
                continue;
            }
            if (!"Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : "")) {
                continue;
            }
            candidates.add(a);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .min(Comparator
                        .comparing((PgAgency a) -> "CHILLPAY".equalsIgnoreCase(a.getPgCd().trim()) ? 0 : 1)
                        .thenComparing(PgAgency::getPgCd, String.CASE_INSENSITIVE_ORDER));
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
     * URL 결제 ChillPay Route: (1) 가맹점 바인딩 {@code root_no}, (2) 동일 {@code pg_cd} 의 {@code tb_pg_agency.route_no}.
     * 둘 다 없으면 {@link IllegalStateException} — yml·임의 기본값 없음.
     */
    private static int resolveChillPayRouteNoUrlPayContract(
            Long merchantOrgUnitId,
            MerchantPgBinding b,
            Optional<PgAgency> agencyForPgCd) {
        Optional<Integer> routeFromRoot = parseBindingRouteNo(b.getRootNo());
        Optional<Integer> routeFromAgency = agencyForPgCd.flatMap(a -> Optional.ofNullable(a.getRouteNo()));
        if (routeFromRoot.isPresent()) {
            return routeFromRoot.get();
        }
        if (routeFromAgency.isPresent()) {
            return routeFromAgency.get();
        }
        String pgCd = b.getPgCd() != null ? b.getPgCd().trim() : "";
        log.warn(
                "orgUnitId={}: ChillPay Route unset — merchant binding root_no empty and (no tb_pg_agency route or no matching agency) pg_cd={}",
                merchantOrgUnitId, pgCd);
        if (agencyForPgCd.isPresent()) {
            throw new IllegalStateException(
                    "ChillPay 루트(Route) 번호가 설정되지 않았습니다. 가맹점 등록 > 결제대행사 설정에서 해당 PG("
                            + pgCd
                            + ")의 루트번호를 입력하거나, API연동설정에서 동일 결제대행사(pg_cd) 행의 Route 번호를 입력하세요.");
        }
        throw new IllegalStateException(
                "ChillPay 루트(Route) 번호가 설정되지 않았습니다. (1) 가맹점 결제대행사의 루트번호 또는 "
                        + "(2) API연동설정에서 바인딩과 동일한 pg_cd(" + pgCd + ") 행을 등록하고 Route 번호를 입력하세요. "
                        + "tb_pg_agency에 해당 PG코드가 없으면 행을 추가하세요.");
    }

    /**
     * ChillPay Merchant-Code: 가맹점 바인딩 MID → (동일 {@code pg_cd} 행이 있으면) 그 행의 {@link PgAgency#merchantMid} 만.
     * 그 행이 없을 때만 본사 {@link HqFallbackRef#merchantCode()} — 다른 PG 코드 행의 MID는 바인딩 키와 섞지 않는다.
     */
    private static String resolveMerchantMidForUrlPay(
            MerchantPgBinding b, Optional<PgAgency> agencyForPgCd, HqFallbackRef hqRef) {
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
            return hqRef.merchantCode().trim();
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
                .filter(b -> Boolean.TRUE.equals(urlPayByPgCd.get(pgCdKey(b))))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .min(Comparator
                        .comparing((MerchantPgBinding b) -> isChillPayFamilyPgCd(b.getPgCd()) ? 0 : 1)
                        .thenComparing((MerchantPgBinding b) -> genericChillPayPgCd(b.getPgCd()) ? 1 : 0)
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
        /* URL 결제는 가맹점에 세팅된 운영 WEB·URL결제 연동 PG만 사용. 본사 일반 CHILLPAY로 대체하지 않음 */
        return "";
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
                .filter(b -> Boolean.TRUE.equals(urlPayByPgCd.get(pgCdKey(b))))
                .filter(b -> !webOnly || isWebOrUnsetPayMethod(b.getPayMethod()))
                .min(Comparator
                        .comparing((MerchantPgBinding b) -> genericChillPayPgCd(b.getPgCd()) ? 1 : 0)
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
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        String k = pgCd.trim();
        Optional<PgAgency> row = pgAgencyRepository.findByPgCd(k)
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()));
        if (row.isPresent()) {
            PgAgency a = row.get();
            return "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : "");
        }
        if (!isChillPayFamilyPgCd(k)) {
            return false;
        }
        return findChillPayAgencyFallbackForMissingPgCd(k).isPresent();
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

    /** {@code CHILLPAY} 단독 코드(확장 접미 없음) — URL결제 전용 {@code CHILLPAY_…} 행보다 뒤로 미루는 데 사용 */
    private static boolean genericChillPayPgCd(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return true;
        }
        return "CHILLPAY".equalsIgnoreCase(pgCd.trim());
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

    /**
     * 본사 API연동({@code tb_pg_agency}) — ChillPay 계열·사용(Y)·API Key+MD5 가 채워진 행을 {@code CHILLPAY} 코드 우선으로 한 건 선택.
     */
    /**
     * 본사 단독(가맹점 없음) ChillPay 자격 후보. <strong>연동용도 URL결제(Y)</strong>인 행만 — 노티 전용 행으로 URL 결제를 열지 않음.
     */
    private Optional<PgAgency> pickPrimaryChillPayAgencyWithKeys() {
        return pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(a -> a.getPgCd() != null && isChillPayFamilyPgCd(a.getPgCd()))
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .filter(a -> "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : ""))
                .filter(a -> a.getApiKey() != null && !a.getApiKey().isBlank()
                        && a.getMd5SecretKey() != null && !a.getMd5SecretKey().isBlank())
                .min(Comparator
                        .comparing((PgAgency a) -> "CHILLPAY".equalsIgnoreCase(a.getPgCd().trim()) ? 0 : 1)
                        .thenComparing(PgAgency::getPgCd, Comparator.nullsLast(String::compareToIgnoreCase)));
    }

    /**
     * 가맹점 ChillPay 조합 시 MID·샌드박스 참조용(Route 확정과 무관).
     */
    private HqFallbackRef resolveHqFallbackRef() {
        Optional<PgAgency> pick = pickPrimaryChillPayAgencyWithKeys();
        if (pick.isPresent()) {
            PgAgency a = pick.get();
            String mc = (a.getMerchantMid() != null && !a.getMerchantMid().isBlank())
                    ? a.getMerchantMid().trim()
                    : props.getMerchantCode();
            boolean sandbox = a.getSandboxYn() == null || !"N".equalsIgnoreCase(a.getSandboxYn().trim());
            return new HqFallbackRef(mc, sandbox);
        }
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        if (opt.isPresent()) {
            HqApiConfig c = opt.get();
            String mc = (c.getChillpayMerchantCode() != null && !c.getChillpayMerchantCode().isEmpty())
                    ? c.getChillpayMerchantCode().trim()
                    : props.getMerchantCode();
            boolean sandbox = !"N".equalsIgnoreCase(c.getChillpaySandbox());
            return new HqFallbackRef(mc, sandbox);
        }
        return new HqFallbackRef(props.getMerchantCode(), props.isSandbox());
    }

    /** ChillPay 계열 자격 행 — {@code CHILLPAY} 우선. 선택 행의 {@code route_no} 필수(없으면 예외). */
    private Optional<Config> configFromPgAgencyChillPay(ChillPayAgencyUrlOverrides urlOv) {
        Optional<PgAgency> pick = pickPrimaryChillPayAgencyWithKeys();
        if (pick.isEmpty()) {
            return Optional.empty();
        }
        PgAgency a = pick.get();
        if (a.getRouteNo() == null) {
            String pgc = a.getPgCd() != null ? a.getPgCd().trim() : "";
            throw new IllegalStateException(
                    "ChillPay 루트(Route) 번호가 설정되지 않았습니다. API연동설정에서 해당 ChillPay 행(pg_cd="
                            + pgc + ")의 Route 번호를 입력하세요.");
        }
        int routeNo = a.getRouteNo();
        String mc = (a.getMerchantMid() != null && !a.getMerchantMid().isBlank())
                ? a.getMerchantMid().trim()
                : props.getMerchantCode();
        boolean sandbox = a.getSandboxYn() == null || !"N".equalsIgnoreCase(a.getSandboxYn().trim());
        return Optional.of(new Config(mc, a.getApiKey().trim(), a.getMd5SecretKey().trim(), routeNo, sandbox, urlOv));
    }

    /**
     * 본사 전용 ChillPay 자격: (1) {@code tb_pg_agency} 자격 행의 {@code route_no} 또는
     * (2) {@code tb_hq_api_config.chillpayRouteNo}. 둘 다 없으면 예외 — {@code application.yml} 로 대체하지 않음.
     */
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
            if (c.getChillpayRouteNo() == null) {
                throw new IllegalStateException(
                        "ChillPay 루트(Route) 번호가 설정되지 않았습니다. API연동설정(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, "
                                + "본사설정 > API배포설정의 ChillPay Route 번호를 입력하세요.");
            }
            int routeNo = c.getChillpayRouteNo();
            boolean sandbox = !"N".equalsIgnoreCase(c.getChillpaySandbox());
            return new Config(merchantCode, apiKey, md5Key, routeNo, sandbox, urlOv);
        }
        throw new IllegalStateException(
                "ChillPay 루트(Route) 번호를 설정할 수 없습니다. API연동설정에서 ChillPay 결제대행사(Key·MD5·Route)를 등록하거나, "
                        + "본사 API배포설정에 ChillPay Route를 입력하세요.");
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
     * DirectCredit 호출 결과와, 실제 요청에 사용한 ChillPay Route(전산 적재·표시용 — {@link #resolveEffectiveRouteNo}와 동일 스냅샷).
     */
    public record ChillPayDirectPaymentResult(ChillPayDirectCreditResponse response, int routeUsed) {}

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
    public ChillPayDirectPaymentResult requestPayment(
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
            return new ChillPayDirectPaymentResult(body, cfg.routeNo());
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
     * 우선순위: {@code public_admin_site_url} → {@code public_api_base_url} → (신뢰된) {@code Origin}/{@code Referer} 호스트
     * → 현재 요청의 scheme/host.
     * 결제 폼이 {@code icopay.co.kr}·API가 {@code api.icopay.co.kr} 인 경우, DB에 API URL만 있어도 브라우저 Origin 으로
     * 결과 페이지를 같은 도메인에 두어 로그인 페이지로 잘못 보내지 않도록 합니다.
     */
    public String resolveUrlPayResultAbsolute(HttpServletRequest request, String compId) {
        if (request == null) {
            return "";
        }
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        HqApiConfig cfg = opt.orElse(null);
        String base = "";
        if (cfg != null && cfg.getPublicAdminSiteUrl() != null && !cfg.getPublicAdminSiteUrl().isBlank()) {
            base = preferTrustedOriginIfApiSubdomainMisconfigured(cfg.getPublicAdminSiteUrl().trim(), request, cfg);
        } else if (cfg != null && cfg.getPublicApiBaseUrl() != null && !cfg.getPublicApiBaseUrl().isBlank()) {
            base = cfg.getPublicApiBaseUrl().trim();
            String fromBrowser = resolveTrustedPayPageOriginBase(request, cfg);
            if (fromBrowser != null && !fromBrowser.isBlank()
                    && hostOfBase(fromBrowser) != null
                    && !hostOfBase(fromBrowser).equalsIgnoreCase(hostOfBase(base))) {
                base = fromBrowser;
            }
        } else {
            String fromBrowser = resolveTrustedPayPageOriginBase(request, cfg);
            if (fromBrowser != null && !fromBrowser.isBlank()) {
                base = fromBrowser;
            } else {
                base = request.getScheme() + "://" + request.getServerName();
                int port = request.getServerPort();
                if (port != 80 && port != 443 && port > 0) {
                    base += ":" + port;
                }
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
     * DB 에 {@code public_admin_site_url} 이 {@code api.*} 로만 잡혀 있고, 실제 결제 폼은 {@code www}/루트 도메인인 경우
     * 신뢰된 Origin·Referer 로 베이스를 바꿉니다.
     */
    private static String preferTrustedOriginIfApiSubdomainMisconfigured(String base, HttpServletRequest request, HqApiConfig cfg) {
        String bh = hostOfBase(base);
        if (bh == null || !bh.toLowerCase(Locale.ROOT).startsWith("api.")) {
            return base;
        }
        String fromBrowser = resolveTrustedPayPageOriginBase(request, cfg);
        if (fromBrowser == null || fromBrowser.isBlank()) {
            return base;
        }
        String oh = hostOfBase(fromBrowser);
        if (oh == null || oh.equalsIgnoreCase(bh)) {
            return base;
        }
        return fromBrowser;
    }

    /**
     * 결제 요청 시 브라우저가 보낸 Origin 또는 Referer 에서 scheme+host(+비표준 포트)만 추출합니다.
     * 오픈 리다이렉트 방지를 위해 {@link #isTrustedPayResultHost} 로 화이트리스트합니다.
     */
    private static String resolveTrustedPayPageOriginBase(HttpServletRequest request, HqApiConfig cfg) {
        if (request == null) {
            return null;
        }
        String origin = trimToNull(request.getHeader("Origin"));
        String candidate = origin;
        if (candidate == null) {
            String ref = trimToNull(request.getHeader("Referer"));
            if (ref != null) {
                try {
                    URI u = URI.create(ref);
                    if (u.getScheme() != null && u.getHost() != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(u.getScheme()).append("://").append(u.getHost());
                        int port = u.getPort();
                        if (port > 0 && port != 80 && port != 443) {
                            sb.append(':').append(port);
                        }
                        candidate = sb.toString();
                    }
                } catch (IllegalArgumentException ignored) {
                    /* ignore */
                }
            }
        }
        if (candidate == null) {
            return null;
        }
        try {
            URI u = URI.create(candidate);
            if (!isTrustedPayResultHost(u.getHost(), cfg)) {
                return null;
            }
            return stripTrailingSlashes(candidate.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String stripTrailingSlashes(String b) {
        String x = b;
        while (x.endsWith("/")) {
            x = x.substring(0, x.length() - 1);
        }
        return x;
    }

    private static String hostOfBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            return URI.create(baseUrl.trim()).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isTrustedPayResultHost(String host, HqApiConfig cfg) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(h) || "127.0.0.1".equals(h)) {
            return true;
        }
        if ("icopay.co.kr".equals(h) || "www.icopay.co.kr".equals(h) || h.endsWith(".icopay.co.kr")) {
            return true;
        }
        if (cfg != null) {
            for (String urlStr : List.of(cfg.getPublicAdminSiteUrl(), cfg.getPublicApiBaseUrl())) {
                if (urlStr == null || urlStr.isBlank()) {
                    continue;
                }
                try {
                    String uh = URI.create(urlStr.trim()).getHost();
                    if (uh != null && uh.equalsIgnoreCase(h)) {
                        return true;
                    }
                } catch (Exception ignored) {
                    /* ignore */
                }
            }
        }
        return false;
    }

    /**
     * ChillPay Transaction API — Search Payment Transaction (실시간 칠페이 결제 거래 목록).
     * 본사·가맹 {@link #resolveConfig(Long)} 자격(MerchantCode·ApiKey·MD5)으로 호출합니다.
     *
     * @param merchantOrgUnitId null이면 본사(HQ) 설정만 사용
     * @param multiCurrency     총본사·본사·총판 true 시 통화별 금액 나열, 지사 이하는 {@code primaryCurrency} 만 집계
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
            LocalDate transactionDateTo,
            boolean multiCurrency,
            String primaryCurrency) {

        int ps = Math.min(100, Math.max(1, size));
        int pn = Math.max(1, page);
        PageResult<Map<String, Object>> display = searchChillPayPaymentTransactionsPage(
                merchantOrgUnitId, pn, ps, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo,
                null, null);

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        int totalPages = display.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);
        for (int p = 1; p <= maxPages; p++) {
            PageResult<Map<String, Object>> slice = (p == pn)
                    ? display
                    : searchChillPayPaymentTransactionsPage(
                            merchantOrgUnitId, p, display.getSize(), orderBy, orderDir, searchKeyword,
                            merchantCodeFilter, paymentChannel, routeNoFilter, orderNo, status,
                            transactionDateFrom, transactionDateTo, null, null);
            accumulateChillPayRowsIntoRollup(roll, slice.getList());
        }

        Map<String, Object> meta = display.getMeta() != null ? new LinkedHashMap<>(display.getMeta()) : new LinkedHashMap<>();
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, totalPages > maxPages));
        display.setMeta(meta);
        return display;
    }

    /**
     * ChillPay Transaction API — 동일 {@code /api/v1/payment/search} 로
     * <strong>PaymentDate</strong> 구간·정렬을 중심으로 조회합니다.
     * ICOPAY 정산 테이블이 아니라 칠페이가 내려주는 Settled·Fee·TotalAmount 등 원문을 그대로 표시합니다.
     * 문서 Table 1.2 OrderBy 에 {@code Settled}·{@code PaymentDate} 등 사용 가능.
     *
     * @param paymentDateFrom/to null·둘 다 null 이면 최근 30일(오늘 기준) 결제일 구간
     * @param transactionDateFrom/to 선택 — 거래생성일 필터(비우면 미전달)
     */
    public PageResult<Map<String, Object>> searchChillPaySettlementTransactions(
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
            LocalDate paymentDateFrom,
            LocalDate paymentDateTo,
            LocalDate transactionDateFrom,
            LocalDate transactionDateTo,
            boolean multiCurrency,
            String primaryCurrency) {

        LocalDate payFrom = paymentDateFrom;
        LocalDate payTo = paymentDateTo;
        if (payFrom == null && payTo == null) {
            payTo = LocalDate.now();
            payFrom = payTo.minusDays(30);
        } else if (payFrom == null) {
            payTo = payTo != null ? payTo : LocalDate.now();
            payFrom = payTo.minusDays(30);
        } else if (payTo == null) {
            payTo = payFrom.plusDays(30);
        }

        int ps = Math.min(100, Math.max(1, size));
        int pn = Math.max(1, page);
        String ob = trimOrDefault(orderBy, "Settled");
        PageResult<Map<String, Object>> display = searchChillPayPaymentTransactionsPage(
                merchantOrgUnitId, pn, ps, ob, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status,
                transactionDateFrom, transactionDateTo, payFrom, payTo);

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        int totalPages = display.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);
        for (int p = 1; p <= maxPages; p++) {
            PageResult<Map<String, Object>> slice = (p == pn)
                    ? display
                    : searchChillPayPaymentTransactionsPage(
                            merchantOrgUnitId, p, display.getSize(), ob, orderDir, searchKeyword,
                            merchantCodeFilter, paymentChannel, routeNoFilter, orderNo, status,
                            transactionDateFrom, transactionDateTo, payFrom, payTo);
            accumulateChillPayRowsIntoRollup(roll, slice.getList());
        }

        Map<String, Object> meta = display.getMeta() != null ? new LinkedHashMap<>(display.getMeta()) : new LinkedHashMap<>();
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, totalPages > maxPages));
        meta.put("chillPaySettlementMode", true);
        meta.put("paymentDateFrom", payFrom.toString());
        meta.put("paymentDateTo", payTo.toString());
        display.setMeta(meta);
        return display;
    }

    private static void accumulateChillPayRowsIntoRollup(PayListStatusBarBuckets.MutableRollup roll,
                                                         List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            String st = firstNonBlankString(row, "status", "Status");
            String bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
            String cur = PayListStatusBarBuckets.normalizeCurrency(firstNonBlankString(row, "currency", "Currency"));
            Object amtObj = row.containsKey("amount") ? row.get("amount") : row.get("Amount");
            roll.add(bucket, cur, PayListStatusBarBuckets.parseMoney(amtObj), 1L);
        }
    }

    private static String firstNonBlankString(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String k : keys) {
            Object v = row.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    private PageResult<Map<String, Object>> searchChillPayPaymentTransactionsPage(
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
            LocalDate transactionDateTo,
            LocalDate paymentDateFrom,
            LocalDate paymentDateTo) {

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
        req.setPaymentDateFrom(paymentDateFrom != null ? paymentDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : "");
        req.setPaymentDateTo(paymentDateTo != null ? paymentDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : "");

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
        if (!m.containsKey("icopay")) {
            if (m.containsKey("IcoPay")) {
                m.put("icopay", m.get("IcoPay"));
            } else if (m.containsKey("Icopay")) {
                m.put("icopay", m.get("Icopay"));
            }
        }
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
