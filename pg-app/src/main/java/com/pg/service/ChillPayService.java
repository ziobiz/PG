package com.pg.service;

import com.pg.integration.pg.PgVendor;
import com.pg.api.dto.PageResult;
import com.pg.config.ChillPayProperties;
import com.pg.dto.ChillPayDirectCreditRequest;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.dto.ChillPayPaymentSearchApiRequest;
import com.pg.dto.ChillPayPaymentSearchApiResponse;
import com.pg.dto.ChillPaySettlementSearchApiRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.dto.TxnDualLineSpec;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.PgAgency;
import com.pg.entity.SettlementSetting;
import com.pg.util.ChillPayDirectCreditUtil;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.TrnTimeDualZoneDisplay;
import com.pg.util.ViewDisplayTimezoneResolver;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    /** URL 결제 바인딩 범위 — STANDARD: 일반 URL·챗봇·API, REPAY: 저장 카드 재결제 URL 전용 */
    public enum UrlPayBindingScope {
        STANDARD, REPAY
    }

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
    /** Transaction Services — Search Settlement Transaction (Table 2.1~2.3) */
    private static final String TXN_SETTLEMENT_SEARCH_SB = "https://sandbox-api-transaction.chillpay.co/api/v1/settlement/search";
    private static final String TXN_SETTLEMENT_SEARCH_PR = "https://api-transaction.chillpay.co/api/v1/settlement/search";
    /** Transaction Services — Request Void (Table void/request) */
    private static final String TXN_VOID_REQUEST_SB = "https://sandbox-api-transaction.chillpay.co/api/v1/void/request";
    private static final String TXN_VOID_REQUEST_PR = "https://api-transaction.chillpay.co/api/v1/void/request";
    /** Transaction Services — Request Refund (Table refund/request) */
    private static final String TXN_REFUND_REQUEST_SB = "https://sandbox-api-transaction.chillpay.co/api/v1/refund/request";
    private static final String TXN_REFUND_REQUEST_PR = "https://api-transaction.chillpay.co/api/v1/refund/request";
    private static final DateTimeFormatter CHILLPAY_TXN_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    /** {@link PgExtSettlementExpectedService} 가 통합정산 행에 넣는 {@code icopayExpectedSettleAt} 표기 */
    private static final DateTimeFormatter ICO_EXPECTED_SETTLE_AT_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    /** 통합내역·통합정산 그리드 거래일 표시 — {@code yyyy-MM-dd} (로케일 비의존) */
    private static final DateTimeFormatter CHILL_TRN_DATE_GRID = DateTimeFormatter.ISO_LOCAL_DATE;
    /** 통합내역·일별통합: ChillPay 결제 검색 API 페이지당 상한(문서 기본 100, 상세·목록 500까지 요청) */
    /** ChillPay 결제 검색 API — PageSize 상한(초과 시 status 2005 Invalid PageSize Data). */
    private static final int CHILL_PAY_PAYMENT_PAGE_SIZE_MAX = 100;
    /** 통합내역 상단 요약: 최대 추가 ChillPay API 호출 페이지 수 */
    private static final int CHILL_STATUS_BAR_MAX_PAGES = 30;
    /** 일별통합 일자 집계 시 페이지 간 ChillPay 호출 간격(4004 완화). 0 이면 대기 없음. */
    private static final long CHILL_DAILY_SUMMARY_INTER_PAGE_MS = 80L;

    /** ChillPay 문서 예시처럼 JSON 에 명시적 {@code null} 필드를 포함한다(기본 Jackson 은 null 키를 생략함). */
    private static final ObjectMapper CHILLPAY_SETTLEMENT_JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    private final ChillPayProperties props;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PayListService payListService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final PgExtSettlementExpectedService pgExtSettlementExpectedService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;
    private final SettlementSettingRepository settlementSettingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public ChillPayService(ChillPayProperties props, HqApiConfigRepository hqApiConfigRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository,
                          PgAgencyRepository pgAgencyRepository,
                          OrgUnitRepository orgUnitRepository,
                          OrgServiceUseService orgServiceUseService,
                          PgTrnsctnRepository pgTrnsctnRepository,
                          PayListService payListService,
                          UrlPayDisplayFxService urlPayDisplayFxService,
                          PgExtSettlementExpectedService pgExtSettlementExpectedService,
                          HqLedgerSysSettingsService hqLedgerSysSettingsService,
                          MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,
                          SettlementSettingRepository settlementSettingRepository) {
        this.props = props;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.payListService = payListService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.pgExtSettlementExpectedService = pgExtSettlementExpectedService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    /** 가맹점 결제 조합 시 MID·샌드박스 보조 (Route 확정 전에 조회 가능) */
    private record HqFallbackRef(String merchantCode, boolean sandbox) {}

    /** 통합내역: TransactionId → 결제 DB(tb_pg_trnsctn) 역추적 시 가맹점 코드·업체 엔티티 */
    private record TxnMerchantLookup(String merchantId, Optional<OrgUnit> org) {}

    /** DirectCredit·거래 적재 시 Route 표시용 */
    public int resolveEffectiveRouteNo(Long merchantOrgUnitId) {
        return resolveConfig(merchantOrgUnitId).routeNo();
    }

    /**
     * 가맹점 ChillPay 바인딩이 있으면 자격은 바인딩; 샌드박스·route·ChillPay URL은 동일 {@code pg_cd}의 {@link PgAgency} 한 행만 사용(타 PG 행 URL 병합 없음).
     * 운영(Y) WEB 바인딩이면서 연동용도 URL결제인 PG만 사용합니다(노티 전용 운영 행만 있으면 URL 결제 불가).
     */
    private Config resolveConfig(Long merchantOrgUnitId) {
        return resolveConfig(merchantOrgUnitId, UrlPayBindingScope.STANDARD);
    }

    private Config resolveConfig(Long merchantOrgUnitId, UrlPayBindingScope scope) {
        ChillPayAgencyUrlOverrides globalUrlOv = loadChillPayAgencyUrlOverrides();
        if (merchantOrgUnitId == null) {
            return resolveConfigFromHq(globalUrlOv);
        }
        Optional<MerchantPgBinding> bindingOpt = findOperationalChillPayFamilyBinding(merchantOrgUnitId, scope);
        if (bindingOpt.isEmpty()) {
            throw urlPayOperationalBindingMissing(scope, true);
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
                            + "배포설정 > API연동설정에서 동일 pg_cd(" + pgc + ") 행에 API Key·MD5를 등록하세요. "
                            + "해당 행은 연동용도 「"
                            + (scope == UrlPayBindingScope.REPAY ? "URL재결제" : "URL결제")
                            + "」가 Y인 행이어야 합니다.");
        }
        HqFallbackRef hqRef = resolveHqFallbackRef();
        String mc = resolveMerchantMidForUrlPay(b, agencyForPgCd, hqRef);
        if (mc == null || mc.isEmpty()) {
            String pgc = b.getPgCd() != null ? b.getPgCd().trim() : "";
            throw new IllegalStateException(
                    "ChillPay MID(Merchant Code)가 비어 있습니다. 가맹점 등록 > 결제대행사에서 해당 PG(" + pgc + ")의 MID를 입력하거나, "
                            + "배포설정 > API연동설정 동일 pg_cd 행의 MID를 채워 주세요.");
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
     * (배포설정 &gt; API연동설정에 Route·MID만 CHILLPAY 행에 두고 가맹점 바인딩은 확장 코드만 쓰는 구성을 지원)
     */
    private Optional<PgAgency> findChillPayAgencyFallbackForMissingPgCd(String requestedPgCd) {
        return findChillPayAgencyFallbackForMissingPgCd(requestedPgCd, UrlPayBindingScope.STANDARD);
    }

    private Optional<PgAgency> findChillPayAgencyFallbackForMissingPgCd(String requestedPgCd, UrlPayBindingScope scope) {
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
            if (scope == UrlPayBindingScope.REPAY) {
                if (!"Y".equalsIgnoreCase(a.getIntegUrlPayRepayYn() != null ? a.getIntegUrlPayRepayYn().trim() : "")) {
                    continue;
                }
            } else if (!"Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : "")) {
                continue;
            }
            candidates.add(a);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .min(Comparator
                        .comparing((PgAgency a) -> PgVendor.CHILLPAY.equalsIgnoreCase(a.getPgCd().trim()) ? 0 : 1)
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
                            + ")의 루트번호를 입력하거나, 배포설정 > API연동설정에서 동일 결제대행사(pg_cd) 행의 Route 번호를 입력하세요.");
        }
        throw new IllegalStateException(
                "ChillPay 루트(Route) 번호가 설정되지 않았습니다. (1) 가맹점 결제대행사의 루트번호 또는 "
                        + "(2) 배포설정 > API연동설정에서 바인딩과 동일한 pg_cd(" + pgCd + ") 행을 등록하고 Route 번호를 입력하세요. "
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
        return PgVendor.isChillPayFamily(pgCd);
    }

    /**
     * 통합내역·통합정산(Transaction Services) 호출 시 {@code merchantOrgUnitId} 가 비어 있으면
     * 본사 {@link #resolveConfigFromHq} 만 타서, 결제(통합결제)와 달리 {@code tb_pg_agency.sandbox_yn} 이 Y 로 남은
     * 본사 행 때문에 샌드박스 URL로 프로덕션 자격이 섞이는 문제가 생길 수 있다.
     * 검색 필터의 가맹점 코드(업체코드) 또는 ChillPay MID 로 {@link OrgUnit} id 를 보강하면
     * 결제와 동일하게 {@link #resolveConfig(Long)}(가맹점 바인딩 + 동일 pg_cd 행)을 쓴다.
     */
    private Long resolveMerchantOrgUnitIdForChillPayTxnApi(Long merchantOrgUnitId, String merchantCodeFilter) {
        if (merchantOrgUnitId != null) {
            return merchantOrgUnitId;
        }
        if (merchantCodeFilter == null || merchantCodeFilter.isBlank() || "__NONE__".equals(merchantCodeFilter)) {
            return null;
        }
        String token = merchantCodeFilter.trim();
        Optional<OrgUnit> byCode = orgUnitRepository.findByCodeIgnoreCase(token);
        if (byCode.isPresent()) {
            return byCode.get().getId();
        }
        List<MerchantPgBinding> byMid = merchantPgBindingRepository.findByMidIgnoreCaseOrderByOperationalYnDescIdAsc(token);
        if (byMid.isEmpty()) {
            return null;
        }
        for (MerchantPgBinding b : byMid) {
            if (b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim())
                    && isChillPayFamilyPgCd(b.getPgCd())) {
                return b.getOrgUnitId();
            }
        }
        for (MerchantPgBinding b : byMid) {
            if (isChillPayFamilyPgCd(b.getPgCd())) {
                return b.getOrgUnitId();
            }
        }
        return byMid.get(0).getOrgUnitId();
    }

    /**
     * URL 결제 운영 WEB 바인딩의 금액 모드.
     * {@link UrlPayDisplayFxService#MODE_DISPLAY_FX_THB} 이면 표시통화(JPY/USD)→실결제 THB.
     */
    public String resolveUrlPayPricingMode(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return "CHECKOUT_CURRENCY";
        }
        Optional<MerchantPgBinding> web = findOperationalWebBindingForUrlPay(merchantOrgUnitId);
        if (web.isEmpty()) {
            return "CHECKOUT_CURRENCY";
        }
        MerchantPgBinding b = web.get();
        String pgCd = b.getPgCd() != null ? b.getPgCd().trim() : "";
        String legacy = b.getUrlPayPricingMode() != null ? b.getUrlPayPricingMode().trim() : "";
        return urlPayDisplayFxService.resolveUrlPayPricingMode(pgCd, legacy);
    }

    public Optional<MerchantPgBinding> findOperationalWebBindingForUrlPay(Long orgUnitId) {
        return listOperationalWebBindingsForUrlPay(orgUnitId).stream().findFirst();
    }

    /**
     * URL 결제 운영 WEB 바인딩 전체(정렬 우선순위). 멀티 PG 라우팅 시 카드브랜드·통화로 1건을 고릅니다.
     */
    public List<MerchantPgBinding> listOperationalWebBindingsForUrlPay(Long orgUnitId) {
        if (orgUnitId == null) {
            return List.of();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Map<String, Boolean> urlPayByPgCd = urlPayAgencyFlagByPgCd(list, UrlPayBindingScope.STANDARD);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && !b.getPgCd().isBlank())
                .filter(b -> Boolean.TRUE.equals(urlPayByPgCd.get(pgCdKey(b))))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .sorted(operationalWebBindingComparator())
                .toList();
    }

    /**
     * 인라인 카드 위젯 종류 — {@link com.pg.urlpay.UrlPayVendorCapabilityRegistry} (URL 결제 공통 플랫폼).
     */
    public static String resolveUrlPayInlineWidgetKind(String pgCd) {
        return com.pg.urlpay.UrlPayVendorCapabilityRegistry.resolveInlineWidgetKindLegacy(pgCd);
    }

    /**
     * URL 결제 시 운영 WEB 바인딩의 {@code pg_cd} — {@link #getUrlPayPresentationForCheckout} 의 {@code urlPayOperationalPgCd} 와 동일 규칙.
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
     * URL 재결제(저장 카드)용 운영 WEB 바인딩 — {@code integ_url_pay_repay_yn=Y} PG만 후보.
     */
    public Optional<MerchantPgBinding> findOperationalWebBindingForUrlPayRepay(Long orgUnitId) {
        return listOperationalWebBindingsForUrlPayRepay(orgUnitId).stream().findFirst();
    }

    public List<MerchantPgBinding> listOperationalWebBindingsForUrlPayRepay(Long orgUnitId) {
        if (orgUnitId == null || !isUrlPayRepayEnabledAtHq()) {
            return List.of();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Map<String, Boolean> repayByPgCd = urlPayAgencyFlagByPgCd(list, UrlPayBindingScope.REPAY);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && !b.getPgCd().isBlank())
                .filter(b -> Boolean.TRUE.equals(repayByPgCd.get(pgCdKey(b))))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .sorted(operationalWebBindingComparator())
                .toList();
    }

    private Comparator<MerchantPgBinding> operationalWebBindingComparator() {
        return Comparator
                .comparing((MerchantPgBinding b) -> isChillPayFamilyPgCd(b.getPgCd()) ? 0 : 1)
                .thenComparingInt((MerchantPgBinding b) -> UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equalsIgnoreCase(
                        urlPayDisplayFxService.resolveUrlPayPricingMode(
                                b.getPgCd() != null ? b.getPgCd().trim() : "",
                                b.getUrlPayPricingMode() != null ? b.getUrlPayPricingMode().trim() : ""))
                        ? 0 : 1)
                .thenComparing((MerchantPgBinding b) -> genericChillPayPgCd(b.getPgCd()) ? 1 : 0)
                .thenComparing(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(MerchantPgBinding::getId);
    }

    public String resolveUrlPayRepayOperationalPgCd(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return "";
        }
        Optional<MerchantPgBinding> webOp = findOperationalWebBindingForUrlPayRepay(merchantOrgUnitId);
        if (webOp.isPresent()) {
            String cd = webOp.get().getPgCd();
            return cd != null ? cd.trim() : "";
        }
        return "";
    }

    public boolean isUrlPayRepayEnabledAtHq() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> c.getUrlPayRepayEnabledYn() != null
                        && "Y".equalsIgnoreCase(c.getUrlPayRepayEnabledYn().trim()))
                .orElse(false);
    }

    public String resolveUrlPayRepayPathTemplate() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getUrlPayRepayPathTemplate)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse("/pay-repay/{compCode}");
    }

    /**
     * 운영(Y) ChillPay 계열 바인딩 — 배포설정 &gt; API연동설정에서 연동용도 URL결제인 {@code pg_cd} 를 최우선,
     * 다음 WEB(또는 결제구분 미입력), 그다음 기타 결제구분.
     */
    private Optional<MerchantPgBinding> findOperationalChillPayFamilyBinding(Long orgUnitId) {
        return findOperationalChillPayFamilyBinding(orgUnitId, UrlPayBindingScope.STANDARD);
    }

    private Optional<MerchantPgBinding> findOperationalChillPayFamilyBinding(Long orgUnitId, UrlPayBindingScope scope) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Map<String, Boolean> flagByPgCd = urlPayAgencyFlagByPgCd(list, scope);
        Optional<MerchantPgBinding> web = pickOperationalChillPayBindingRow(list, true, flagByPgCd);
        return web.isPresent() ? web : pickOperationalChillPayBindingRow(list, false, flagByPgCd);
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

    private static boolean isWebOrUnsetPayMethod(String payMethod) {
        if (payMethod == null || payMethod.isBlank()) {
            return true;
        }
        return "WEB".equalsIgnoreCase(payMethod.trim());
    }

    /** 바인딩 목록에 나온 {@code pg_cd} 별로, 사용 Y인 {@code tb_pg_agency} 행의 URL결제/URL재결제 연동 여부. */
    private Map<String, Boolean> urlPayAgencyFlagByPgCd(List<MerchantPgBinding> list) {
        return urlPayAgencyFlagByPgCd(list, UrlPayBindingScope.STANDARD);
    }

    private Map<String, Boolean> urlPayAgencyFlagByPgCd(List<MerchantPgBinding> list, UrlPayBindingScope scope) {
        Map<String, Boolean> m = new HashMap<>();
        for (MerchantPgBinding b : list) {
            String k = pgCdKey(b);
            if (k.isEmpty()) {
                continue;
            }
            m.computeIfAbsent(k, pgCd -> loadAgencyIntegUrlPayYn(pgCd, scope));
        }
        return m;
    }

    private boolean loadAgencyIntegUrlPayYn(String pgCd) {
        return loadAgencyIntegUrlPayYn(pgCd, UrlPayBindingScope.STANDARD);
    }

    private boolean loadAgencyIntegUrlPayYn(String pgCd, UrlPayBindingScope scope) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        String k = pgCd.trim();
        Optional<PgAgency> row = pgAgencyRepository.findByPgCd(k)
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()));
        if (row.isPresent()) {
            PgAgency a = row.get();
            if (scope == UrlPayBindingScope.REPAY) {
                return "Y".equalsIgnoreCase(a.getIntegUrlPayRepayYn() != null ? a.getIntegUrlPayRepayYn().trim() : "");
            }
            return "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : "");
        }
        if (!isChillPayFamilyPgCd(k)) {
            return false;
        }
        return findChillPayAgencyFallbackForMissingPgCd(k, scope).isPresent();
    }

    private static String pgCdKey(MerchantPgBinding b) {
        if (b == null || b.getPgCd() == null || b.getPgCd().isBlank()) {
            return "";
        }
        return b.getPgCd().trim();
    }

    /** {@code CHILLPAY} 단독 코드(확장 접미 없음) — URL결제 전용 {@code CHILLPAY_…} 행보다 뒤로 미루는 데 사용 */
    private static boolean genericChillPayPgCd(String pgCd) {
        return PgVendor.isChillPayBaseCode(pgCd);
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
     * 배포설정 &gt; API연동설정({@code tb_pg_agency}) — ChillPay 계열·사용(Y)·API Key+MD5 가 채워진 행을 {@code CHILLPAY} 코드 우선으로 한 건 선택.
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
                        .comparing((PgAgency a) -> PgVendor.CHILLPAY.equalsIgnoreCase(a.getPgCd().trim()) ? 0 : 1)
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
                    "ChillPay 루트(Route) 번호가 설정되지 않았습니다. 배포설정 > API연동설정에서 해당 ChillPay 행(pg_cd="
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
                        "ChillPay 루트(Route) 번호가 설정되지 않았습니다. 배포설정 > API연동설정(tb_pg_agency)에 ChillPay 행의 Route를 등록하거나, "
                                + "배포설정 > API배포설정의 ChillPay Route 번호를 입력하세요.");
            }
            int routeNo = c.getChillpayRouteNo();
            boolean sandbox = !"N".equalsIgnoreCase(c.getChillpaySandbox());
            return new Config(merchantCode, apiKey, md5Key, routeNo, sandbox, urlOv);
        }
        throw new IllegalStateException(
                "ChillPay 루트(Route) 번호를 설정할 수 없습니다. 배포설정 > API연동설정에서 ChillPay 결제대행사(Key·MD5·Route)를 등록하거나, "
                        + "배포설정 > API배포설정에 ChillPay Route를 입력하세요.");
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
            apiKey = apiKey != null ? apiKey.replace("\uFEFF", "").trim() : null;
            md5Key = md5Key != null ? md5Key.replace("\uFEFF", "").trim() : null;
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
        return requestPayment(orderNo, customerId, amount, directCreditToken, phoneNumber, description,
                ipAddress, custEmail, merchantOrgUnitId, langCode, checkoutCurrencyCode, browserReturnUrl,
                null, null, null, UrlPayBindingScope.STANDARD);
    }

    public ChillPayDirectPaymentResult requestPayment(
            String orderNo, String customerId, BigDecimal amount, String directCreditToken,
            String phoneNumber, String description, String ipAddress, String custEmail,
            Long merchantOrgUnitId, String langCode, String checkoutCurrencyCode,
            String browserReturnUrl, String saveCard, String creditToken, String tokenType,
            UrlPayBindingScope bindingScope) {

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            throw new IllegalStateException(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED);
        }

        UrlPayBindingScope scope = bindingScope != null ? bindingScope : UrlPayBindingScope.STANDARD;
        Config cfg = resolveConfig(merchantOrgUnitId, scope);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다. 배포설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다. 배포설정 > API배포설정 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }

        ChillPayDirectCreditRequest req = new ChillPayDirectCreditRequest();
        req.setOrderNo(ChillPayDirectCreditUtil.normalizeOrderNo(
                orderNo != null ? orderNo : "ORD" + System.currentTimeMillis()));
        req.setCustomerId(customerId != null ? customerId : "guest");
        req.setAmount(normalizeChillPayRequestAmount(amount, checkoutCurrencyCode));
        req.setDirectCreditToken(directCreditToken);
        if (creditToken != null && !creditToken.isBlank()) {
            req.setCreditToken(creditToken.trim());
            req.setTokenType(tokenType != null && !tokenType.isBlank() ? tokenType.trim().toUpperCase(Locale.ROOT) : "CT");
        } else if (tokenType != null && !tokenType.isBlank()) {
            req.setTokenType(tokenType.trim().toUpperCase(Locale.ROOT));
        }
        if (saveCard != null && !saveCard.isBlank()) {
            req.setSaveCard("Y".equalsIgnoreCase(saveCard.trim()) ? "Y" : "N");
        }
        /* NOTI /admin/test-pay/submit: PhoneNumber 기본값 */
        String phone = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber.trim() : "0911111111";
        req.setPhoneNumber(phone);
        req.setDescription(truncateChillPayDirectCreditDescription(description));
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
            // 가맹 개인정보 보호: PG 로 나가는 ReturnUrl 은 반드시 우리(신뢰) 도메인만 허용(이중 방어).
            HqApiConfig returnUrlCfg = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
            if (isTrustedPayResultHost(hostOfBase(browserReturnUrl), returnUrlCfg)) {
                req.setReturnUrl(browserReturnUrl.trim());
                log.info("ChillPay DirectCredit ReturnUrl set (browser return after hosted step if supported)");
            } else {
                log.warn("ChillPay ReturnUrl 이 신뢰 도메인이 아니어서 미전송 host={}", hostOfBase(browserReturnUrl));
            }
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
    /**
     * ChillPay 매뉴얼(결제 응답 Amount 등): 정수 금액의 <strong>마지막 2자리가 소수</strong>(예 THB {@code 55025} → 550.25).
     * JPY(392)·KRW(410)는 소수 단위 없이 정수만(엔·원 단위). 그 외 통화는 금액×100 후 정수로 보내 체크섬·게이트와 맞춤.
     */
    private static BigDecimal normalizeChillPayRequestAmount(BigDecimal amount, String checkoutCurrencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String num = toChillPayCurrencyNumeric(checkoutCurrencyCode);
        if ("392".equals(num) || "410".equals(num)) {
            return amount.setScale(0, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /** DirectCredit Description 상한(과도한 길이 시 칠리페이 측 검증·저장 불일치 방지). */
    private static final int CHILLPAY_DIRECT_CREDIT_DESCRIPTION_MAX = 255;

    private static String truncateChillPayDirectCreditDescription(String description) {
        String s = description != null ? description : "";
        if (s.length() <= CHILLPAY_DIRECT_CREDIT_DESCRIPTION_MAX) {
            return s;
        }
        return s.substring(0, CHILLPAY_DIRECT_CREDIT_DESCRIPTION_MAX);
    }

    public static String toChillPayCurrencyNumeric(String checkoutCurrencyCode) {
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
            case "SGD" -> "702";
            case "HKD" -> "344";
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
     * 운영 PG가 ChillPay 계열일 때만 {@link #resolveConfig(Long)} URL(ccd·DirectCredit·리다이렉트·appsrv).
     * JPAY 등 타 PG는 플랫폼 공통 필드만 채우고 ChillPay 전용 URL은 비웁니다.
     * {@code urlPayOperationalPgCd} / {@code urlPayInlineWidgetKind} 는 운영 WEB 바인딩 기준(연동용도 URL결제 PG 우선).
     */
    public Map<String, Object> getUrlPayPresentationForCheckout(Long merchantOrgUnitId) {
        String opPgCd = resolveUrlPayOperationalPgCd(merchantOrgUnitId);
        if (opPgCd.isEmpty()) {
            throw urlPayOperationalBindingMissing(UrlPayBindingScope.STANDARD, false);
        }
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
        String widgetKind = resolveUrlPayInlineWidgetKind(opPgCd);
        m.put("urlPayOperationalPgCd", opPgCd);
        m.put("urlPayInlineWidgetKind", widgetKind);
        m.put("urlPayPricingMode", resolveUrlPayPricingMode(merchantOrgUnitId));
        if (isChillPayFamilyPgCd(opPgCd)) {
            Config cfg = resolveConfig(merchantOrgUnitId);
            m.put("redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl());
            m.put("paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url());
            m.put("ccdScriptUrl", cfg.getCcdScriptUrl());
        } else {
            m.put("redirectPaymentPageUrl", "");
            m.put("paymentAppsrvV2Url", "");
            m.put("ccdScriptUrl", "");
        }
        return m;
    }

    /**
     * @param chillPayDirectCreditOnly true 이면 ChillPay DirectCredit·CCD 용(ChillPay 계열 바인딩만 후보)
     */
    private IllegalStateException urlPayOperationalBindingMissing(UrlPayBindingScope scope, boolean chillPayDirectCreditOnly) {
        String hint = scope == UrlPayBindingScope.REPAY
                ? "배포설정 > API연동설정에서 해당 pg_cd 행의 연동용도에 「URL재결제」를 켜야 합니다."
                : "배포설정 > API연동설정에서 해당 pg_cd 행의 연동용도에 「URL결제」를 켜야 합니다. "
                        + "(노티 전용 PG만 운영으로 두면 URL 결제·웹결제 설정을 동시에 쓸 수 없습니다.)";
        String head = scope == UrlPayBindingScope.REPAY
                ? "이 가맹점에 URL 재결제로 사용할 결제대행사(운영)가 없습니다. "
                : chillPayDirectCreditOnly
                ? "이 가맹점에 URL 결제로 사용할 ChillPay 계열 결제대행사(운영)가 없습니다. "
                : "이 가맹점에 URL 결제로 사용할 운영 결제대행사(WEB·연동용도 URL결제)가 없습니다. ";
        return new IllegalStateException(
                head
                        + "가맹점 등록에서 "
                        + (scope == UrlPayBindingScope.REPAY ? "URL재결제" : "URL결제")
                        + " 연동이 있는 결제대행사를 WEB(결제구분)으로 추가한 뒤, 해당 행에 운영(체크)을 켜세요. "
                        + hint);
    }

    public Map<String, Object> getConfigForFrontendUrlPayRepay(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId, UrlPayBindingScope.REPAY);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ccdScriptUrl", cfg.getCcdScriptUrl());
        m.put("directCreditApiUrl", cfg.getPaymentApiUrl());
        m.put("redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl());
        m.put("paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url());
        m.put("merchantCode", cfg.merchantCode() != null ? cfg.merchantCode() : "");
        m.put("apiKey", cfg.apiKey() != null ? cfg.apiKey() : "");
        m.put("routeNo", cfg.routeNo());
        m.put("sandbox", cfg.sandbox());
        m.put("urlPayVariant", "REPAY");
        return m;
    }

    public Map<String, Object> getUrlPayRepayPresentationForCheckout(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId, UrlPayBindingScope.REPAY);
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
        String opPgCd = resolveUrlPayRepayOperationalPgCd(merchantOrgUnitId);
        String widgetKind = resolveUrlPayInlineWidgetKind(opPgCd);
        m.put("urlPayVariant", "REPAY");
        m.put("urlPayOperationalPgCd", opPgCd);
        m.put("urlPayInlineWidgetKind", widgetKind);
        m.put("urlPayRepayPathTemplate", resolveUrlPayRepayPathTemplate());
        Optional<MerchantPgBinding> repayBinding = findOperationalWebBindingForUrlPayRepay(merchantOrgUnitId);
        if (repayBinding.isPresent()) {
            String legacy = repayBinding.get().getUrlPayPricingMode() != null
                    ? repayBinding.get().getUrlPayPricingMode().trim() : "";
            m.put("urlPayPricingMode", urlPayDisplayFxService.resolveUrlPayPricingMode(opPgCd, legacy));
        } else {
            m.put("urlPayPricingMode", "CHECKOUT_CURRENCY");
        }
        m.put("redirectPaymentPageUrl", cfg.getRedirectPaymentPageUrl());
        m.put("paymentAppsrvV2Url", cfg.getAppsrvPaymentV2Url());
        m.put("ccdScriptUrl", cfg.getCcdScriptUrl());
        return m;
    }

    /**
     * ChillPay Card Select / UseCreditToken 용 MerchantSecurityCheck (MD5).
     * Concat: MerchantCode + ApiKey + RequestExpireDate + CreditToken + MD5SecretKey
     */
    public String computeMerchantSecurityCheck(Long merchantOrgUnitId, UrlPayBindingScope scope,
                                               String requestExpireDate, String creditToken) {
        if (merchantOrgUnitId == null || requestExpireDate == null || requestExpireDate.isBlank()
                || creditToken == null || creditToken.isBlank()) {
            return "";
        }
        Config cfg = resolveConfig(merchantOrgUnitId, scope != null ? scope : UrlPayBindingScope.REPAY);
        String concat = (cfg.merchantCode() != null ? cfg.merchantCode() : "")
                + (cfg.apiKey() != null ? cfg.apiKey() : "")
                + requestExpireDate.trim()
                + creditToken.trim()
                + (cfg.md5Key() != null ? cfg.md5Key() : "");
        return md5(concat);
    }

    /** URL 결제 기본 방식과 INLINE/REDIRECT 제공 여부를 반영한 실효 방식 */
    public static String effectiveUrlPayFlow(HqApiConfig c) {
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

    public static String effectiveUrlPayFormMode(HqApiConfig c) {
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
     * @param merchantOrgUnitId null 이면 {@code merchantCodeFilter}(업체코드 또는 MID)로 가맹점을 역매핑해
     *                          통합결제와 동일한 바인딩 설정을 쓰고, 둘 다 없으면 본사(HQ) 설정만 사용
     * @param multiCurrency     총본사·본사 true 시 통화별 금액 나열, 총판·지사 이하는 {@code primaryCurrency} 만 집계
     * @param authentication    금액 요약(meta.payListFinancialSummary) 통화·기준통화 필터용, null 이면 기본 규칙만 적용
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
            String searchPayDivCd,
            boolean multiCurrency,
            String primaryCurrency,
            Authentication authentication) {

        Long effectiveMerchantOrgUnitId = resolveMerchantOrgUnitIdForChillPayTxnApi(merchantOrgUnitId, merchantCodeFilter);
        int ps = Math.min(CHILL_PAY_PAYMENT_PAGE_SIZE_MAX, Math.max(1, size));
        int pn = Math.max(1, page);
        String payDivFilter = searchPayDivCd != null ? searchPayDivCd.trim() : "";
        if (!payDivFilter.isEmpty()) {
            return searchChillPayPaymentTransactionsWithPayDivFilter(
                    effectiveMerchantOrgUnitId, pn, ps, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                    paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo,
                    payDivFilter, multiCurrency, primaryCurrency, authentication);
        }

        PageResult<Map<String, Object>> display = searchChillPayPaymentTransactionsPage(
                effectiveMerchantOrgUnitId, pn, ps, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo,
                null, null);

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        List<Map<String, Object>> rowsForFinancial = new ArrayList<>();
        int totalPages = display.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);
        for (int p = 1; p <= maxPages; p++) {
            PageResult<Map<String, Object>> slice = (p == pn)
                    ? display
                    : searchChillPayPaymentTransactionsPage(
                            effectiveMerchantOrgUnitId, p, display.getSize(), orderBy, orderDir, searchKeyword,
                            merchantCodeFilter, paymentChannel, routeNoFilter, orderNo, status,
                            transactionDateFrom, transactionDateTo, null, null);
            accumulateChillPayRowsIntoRollup(roll, slice.getList());
            if (slice.getList() != null) {
                rowsForFinancial.addAll(slice.getList());
            }
        }

        Map<String, Object> meta = display.getMeta() != null ? new LinkedHashMap<>(display.getMeta()) : new LinkedHashMap<>();
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, totalPages > maxPages));
        meta.put("payListFinancialSummary", payListService.buildChillPayFinancialSummary(rowsForFinancial, authentication));
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        display.setMeta(meta);
        return display;
    }

    /**
     * 일별통합 상단 일자별 집계 전용: 해당 거래일의 ChillPay 결제 검색을
     * {@link #CHILL_STATUS_BAR_MAX_PAGES} 페이지(페이지당 최대 {@link #CHILL_PAY_PAYMENT_PAGE_SIZE_MAX}건)까지 순회해 금액·상태 버킷을 합산합니다.
     * {@link #searchChillPayPaymentTransactions} 와 동일한 집계 범위이며, 그리드 목록은 반환하지 않습니다.
     * <p>{@code totalElements} 는 ChillPay TotalRecord(상태구분 필터 시 매칭 행 수)와 동일합니다.
     * 전체 건수가 스캔 상한을 넘으면 {@code meta.chillDailySummaryScanCapped} 가 true 입니다.
     */
    public PageResult<Map<String, Object>> searchChillPayPaymentTransactionsDailySummary(
            Long merchantOrgUnitId,
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
            String searchPayDivCd,
            boolean multiCurrency,
            String primaryCurrency,
            Authentication authentication) {

        Long effectiveMerchantOrgUnitId = resolveMerchantOrgUnitIdForChillPayTxnApi(merchantOrgUnitId, merchantCodeFilter);
        final int pageSize = CHILL_PAY_PAYMENT_PAGE_SIZE_MAX;
        String payDivStr = searchPayDivCd != null ? searchPayDivCd.trim() : "";
        boolean payDivClientFiltered = !payDivStr.isEmpty();

        PageResult<Map<String, Object>> first = searchChillPayPaymentTransactionsPage(
                effectiveMerchantOrgUnitId, 1, pageSize, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo, null, null);

        long totalEl = first.getTotalElements();
        int totalPages = first.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        List<Map<String, Object>> rowsForFinancial = new ArrayList<>();

        for (int p = 1; p <= maxPages; p++) {
            PageResult<Map<String, Object>> slice = (p == 1)
                    ? first
                    : searchChillPayPaymentTransactionsPage(
                            effectiveMerchantOrgUnitId, p, pageSize, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                            paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo,
                            null, null);
            List<Map<String, Object>> raw = slice.getList() != null ? slice.getList() : Collections.emptyList();
            if (payDivClientFiltered) {
                for (Map<String, Object> row : raw) {
                    if (rowMatchesSearchPayDivCd(row, payDivStr)) {
                        rowsForFinancial.add(row);
                    }
                }
            } else {
                accumulateChillPayRowsIntoRollup(roll, raw);
                rowsForFinancial.addAll(raw);
            }
            if (raw.size() < pageSize) {
                break;
            }
            if (p < maxPages) {
                chillDailySummaryInterPagePause();
            }
        }

        if (payDivClientFiltered) {
            accumulateChillPayRowsIntoRollup(roll, rowsForFinancial);
        }

        boolean scanCapped = totalPages > maxPages;
        long outTotal = payDivClientFiltered ? rowsForFinancial.size() : totalEl;

        Map<String, Object> meta = first.getMeta() != null ? new LinkedHashMap<>(first.getMeta()) : new LinkedHashMap<>();
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, scanCapped, true, null,
                PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER));
        meta.put("payListFinancialSummary", payListService.buildChillPayFinancialSummary(rowsForFinancial, authentication));
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        if (scanCapped) {
            meta.put("chillDailySummaryScanCapped", Boolean.TRUE);
            meta.put("chillDailySummaryTotalPages", totalPages);
        }
        if (payDivClientFiltered) {
            meta.put("chillDailySummaryPayDivClientFiltered", Boolean.TRUE);
            meta.put("chillDailySummaryUnfilteredTotalRecord", totalEl);
        }

        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setPage(1);
        out.setSize(pageSize);
        out.setTotalElements(outTotal);
        out.setTotalPages(Math.max(1, (int) Math.ceil(outTotal / (double) pageSize)));
        out.setList(Collections.emptyList());
        out.setMeta(meta);
        return out;
    }

    /**
     * 검증 리포트: 해당 거래일(TransactionDate) ChillPay 결제 검색 행 전체(페이지 상한까지).
     * {@link #searchChillPayPaymentTransactionsDailySummary} 와 동일 스캔 범위·필터.
     */
    public List<Map<String, Object>> listChillPayPaymentRowsForTransactionDate(
            Long merchantOrgUnitId,
            String orderBy,
            String orderDir,
            String searchKeyword,
            String merchantCodeFilter,
            String paymentChannel,
            Integer routeNoFilter,
            String orderNo,
            String status,
            LocalDate transactionDate,
            String searchPayDivCd,
            Authentication authentication) {

        if (transactionDate == null) {
            return List.of();
        }
        Long effectiveMerchantOrgUnitId = resolveMerchantOrgUnitIdForChillPayTxnApi(merchantOrgUnitId, merchantCodeFilter);
        final int pageSize = CHILL_PAY_PAYMENT_PAGE_SIZE_MAX;
        String payDivStr = searchPayDivCd != null ? searchPayDivCd.trim() : "";
        boolean payDivClientFiltered = !payDivStr.isEmpty();

        PageResult<Map<String, Object>> first = searchChillPayPaymentTransactionsPage(
                effectiveMerchantOrgUnitId, 1, pageSize, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status, transactionDate, transactionDate, null, null);

        int totalPages = first.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);
        List<Map<String, Object>> acc = new ArrayList<>();

        for (int p = 1; p <= maxPages; p++) {
            PageResult<Map<String, Object>> slice = (p == 1)
                    ? first
                    : searchChillPayPaymentTransactionsPage(
                            effectiveMerchantOrgUnitId, p, pageSize, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                            paymentChannel, routeNoFilter, orderNo, status, transactionDate, transactionDate, null, null);
            List<Map<String, Object>> raw = slice.getList() != null ? slice.getList() : Collections.emptyList();
            if (payDivClientFiltered) {
                for (Map<String, Object> row : raw) {
                    if (rowMatchesSearchPayDivCd(row, payDivStr)) {
                        acc.add(row);
                    }
                }
            } else {
                acc.addAll(raw);
            }
            if (raw.size() < pageSize) {
                break;
            }
            if (p < maxPages) {
                chillDailySummaryInterPagePause();
            }
        }
        return acc;
    }

    private static void chillDailySummaryInterPagePause() {
        if (CHILL_DAILY_SUMMARY_INTER_PAGE_MS <= 0L) {
            return;
        }
        try {
            Thread.sleep(CHILL_DAILY_SUMMARY_INTER_PAGE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 통합내역 상단 「상태구분」: 결제내역(tb_pg_trnsctn.status)과 동일 코드로, 노티 보강 후 칠페이 목록을 거릅니다.
     * 칠페이 결제 검색은 페이지당 최대 {@link #CHILL_PAY_PAYMENT_PAGE_SIZE_MAX}건까지 요청합니다. 필터 시 여러 API 페이지를 순회해 요청 페이지를 채웁니다(상한은 CHILL_STATUS_BAR_MAX_PAGES와 동일).
     */
    private PageResult<Map<String, Object>> searchChillPayPaymentTransactionsWithPayDivFilter(
            Long effectiveMerchantOrgUnitId,
            int pn,
            int ps,
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
            String payDivFilter,
            boolean multiCurrency,
            String primaryCurrency,
            Authentication authentication) {

        List<Map<String, Object>> acc = new ArrayList<>();
        Map<String, Object> baseMeta = new LinkedHashMap<>();
        int lastRawSize = 0;
        int scanEnd = 0;
        for (int scanCp = 1; scanCp <= CHILL_STATUS_BAR_MAX_PAGES && acc.size() < pn * ps; scanCp++) {
            PageResult<Map<String, Object>> slice = searchChillPayPaymentTransactionsPage(
                    effectiveMerchantOrgUnitId, scanCp, ps, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                    paymentChannel, routeNoFilter, orderNo, status, transactionDateFrom, transactionDateTo,
                    null, null);
            if (slice.getMeta() != null && baseMeta.isEmpty()) {
                baseMeta.putAll(slice.getMeta());
            }
            List<Map<String, Object>> rawList = slice.getList() != null ? slice.getList() : Collections.emptyList();
            lastRawSize = rawList.size();
            for (Map<String, Object> row : rawList) {
                if (rowMatchesSearchPayDivCd(row, payDivFilter)) {
                    acc.add(row);
                }
            }
            scanEnd = scanCp;
            if (rawList.size() < ps) {
                break;
            }
        }
        boolean cappedTotals = acc.size() < pn * ps && lastRawSize >= ps && scanEnd >= CHILL_STATUS_BAR_MAX_PAGES;

        int fromIdx = (pn - 1) * ps;
        List<Map<String, Object>> pageRows = new ArrayList<>();
        for (int i = fromIdx; i < Math.min(fromIdx + ps, acc.size()); i++) {
            Map<String, Object> src = acc.get(i);
            Map<String, Object> row = new LinkedHashMap<>(src);
            row.put("rowNo", i + 1);
            pageRows.add(row);
        }

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        accumulateChillPayRowsIntoRollup(roll, acc);

        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setList(pageRows);
        out.setPage(pn);
        out.setSize(ps);
        long totalFiltered = acc.size();
        out.setTotalElements(totalFiltered);
        out.setTotalPages(Math.max(1, (int) Math.ceil(totalFiltered / (double) ps)));

        Map<String, Object> meta = new LinkedHashMap<>(baseMeta);
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, cappedTotals));
        meta.put("payListFinancialSummary", payListService.buildChillPayFinancialSummary(acc, authentication));
        if (cappedTotals) {
            meta.put("chillPayPayDivFilterCapped", Boolean.TRUE);
        }
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        out.setMeta(meta);
        return out;
    }

    private static boolean rowMatchesSearchPayDivCd(Map<String, Object> row, String payDivFilter) {
        if (row == null || payDivFilter == null || payDivFilter.isBlank()) {
            return true;
        }
        String v = payDivFilter.trim();
        String st = firstNonBlankString(row, "status", "Status");
        if ("FAIL".equalsIgnoreCase(v)) {
            if ("F0".equalsIgnoreCase(st) || "99".equals(st)) {
                return true;
            }
            return PayListStatusBarBuckets.FAIL.equals(PayListStatusBarBuckets.bucketForChillStatus(st));
        }
        if ("10".equals(v)) {
            return "10".equals(st) || "0".equals(st)
                    || PayListStatusBarBuckets.SUCCESS.equals(PayListStatusBarBuckets.bucketForChillStatus(st));
        }
        if ("20".equals(v)) {
            return "20".equals(st) || "2".equals(st)
                    || PayListStatusBarBuckets.CANCEL.equals(PayListStatusBarBuckets.bucketForChillStatus(st));
        }
        if ("40".equals(v) || "41".equals(v) || "42".equals(v) || "31".equals(v)) {
            return v.equals(st);
        }
        return v.equalsIgnoreCase(st);
    }

    /**
     * ChillPay Transaction API — {@code /api/v1/settlement/search}(Search Settlement Transaction)로 조회합니다.
     * 문서 Table 2.3: 정산지급액·순액·환율·서비스비·이체일·컷오프·정산이체 여부 등 정산 중심 필드를 반환합니다.
     * 결제 상태 문자열은 응답에 없을 수 있어, 동일 승인번호가 ICOPAY {@code tb_pg_trnsctn}에 있으면 보강합니다.
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
            String primaryCurrency,
            Authentication authentication,
            String icopaySettlementStatusGroup) {

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

        Long effectiveMerchantOrgUnitId = resolveMerchantOrgUnitIdForChillPayTxnApi(merchantOrgUnitId, merchantCodeFilter);
        int ps = Math.min(100, Math.max(1, size));
        int pn = Math.max(1, page);
        String ob = normalizeChillPaySettlementOrderBy(orderBy);
        PageResult<Map<String, Object>> display = searchChillPaySettlementTransactionsPage(
                effectiveMerchantOrgUnitId, pn, ps, ob, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status,
                transactionDateFrom, transactionDateTo, payFrom, payTo);

        String postGroup = icopaySettlementStatusGroup != null
                ? icopaySettlementStatusGroup.trim().toUpperCase(Locale.ROOT) : "";
        boolean postFilter = needsIcopaySettlementPostFilter(postGroup);
        if (postFilter) {
            Map<String, String> icopayByChill = loadLatestIcopayStatusByChillTxnIds(display.getList());
            display.setList(filterSettlementRowsByIcopayStatusGroup(display.getList(), postGroup, icopayByChill));
        }

        pgExtSettlementExpectedService.enrichChillPaySettlementRows(display.getList());
        applyIcopayIntegratedSettlementSettledDisplayRules(display.getList());

        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        List<Map<String, Object>> rowsForFinancial = new ArrayList<>();
        int totalPages = display.getTotalPages();
        int maxPages = Math.min(Math.max(totalPages, 1), CHILL_STATUS_BAR_MAX_PAGES);
        if (postFilter) {
            accumulateChillPayRowsIntoRollup(roll, display.getList());
            if (display.getList() != null) {
                rowsForFinancial.addAll(display.getList());
            }
        } else {
            for (int p = 1; p <= maxPages; p++) {
                PageResult<Map<String, Object>> slice = (p == pn)
                        ? display
                        : searchChillPaySettlementTransactionsPage(
                                effectiveMerchantOrgUnitId, p, display.getSize(), ob, orderDir, searchKeyword,
                                merchantCodeFilter, paymentChannel, routeNoFilter, orderNo, status,
                                transactionDateFrom, transactionDateTo, payFrom, payTo);
                accumulateChillPayRowsIntoRollup(roll, slice.getList());
                if (slice.getList() != null) {
                    rowsForFinancial.addAll(slice.getList());
                }
            }
        }

        Map<String, Object> meta = display.getMeta() != null ? new LinkedHashMap<>(display.getMeta()) : new LinkedHashMap<>();
        meta.put("payListStatusBar", roll.toPayload(multiCurrency, primaryCurrency, totalPages > maxPages));
        Map<String, Object> settlementFinSummary = payListService.buildChillPayFinancialSummary(rowsForFinancial, authentication);
        settlementFinSummary.put("feeListSummary", true);
        meta.put("payListFinancialSummary", settlementFinSummary);
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payListService.putFeeCurrencyFormatMeta(meta);
        meta.put("chillPaySettlementMode", true);
        meta.put("chillPaySettlementApi", "SearchSettlementTransaction");
        meta.put("icopayExpectedSettleHelp",
                "예정(ICOPAY) 열은 배포설정 API연동설정(tb_pg_agency)의 T+N(주말 제외 영업일·결제와 동일 시각) 또는 "
                        + "D+N(달력일+N일·일괄 시각)으로 계산합니다. OFF·가맹 덮어쓰기 OFF·MID 미매칭이면 비웁니다. "
                        + "정산(이체) 열은 승인 성공 건만 ChillPay Settled 문구를 보이며, 실패·취소·환불·무효는 비웁니다. "
                        + "예정일이 채워진 경우 서울 기준 예정 시각 이전에는 미정산으로 표시합니다.");
        meta.put("paymentDateFrom", payFrom.toString());
        meta.put("paymentDateTo", payTo.toString());
        if (postFilter) {
            meta.put("chillPaySettlementPostFiltered", true);
            meta.put("chillPaySettlementPostFilterNote",
                    "상단 요약·건수는 ICOPAY 보조 필터(환불/강제환불 구분·성공 제외 등) 적용 시 현재 페이지 결과만 반영됩니다.");
        }
        display.setMeta(meta);
        return display;
    }

    private static boolean needsIcopaySettlementPostFilter(String g) {
        if (g == null || g.isEmpty() || "ALL".equals(g)) {
            return false;
        }
        return switch (g) {
            case "REFUND", "FORCE_REFUND", "VOID", "MANUAL_VOID", "EXCLUDE_SUCCESS" -> true;
            default -> false;
        };
    }

    private Map<String, String> loadLatestIcopayStatusByChillTxnIds(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> qids = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String raw = firstNonBlankString(row,
                    "transactionId", "TransactionId", "Transaction_Id", "transaction_id").trim();
            if (raw.isEmpty()) {
                continue;
            }
            qids.add(raw);
            String n = normalizeChillTxnIdForDbLookup(raw);
            if (!n.isEmpty() && !n.equalsIgnoreCase(raw)) {
                qids.add(n);
            }
        }
        if (qids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PgTrnsctn> list = pgTrnsctnRepository.findAllByChillTransactionIdIn(qids);
        Map<String, PgTrnsctn> best = new HashMap<>();
        for (PgTrnsctn t : list) {
            if (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank()) {
                continue;
            }
            String key = normalizeChillTxnIdForDbLookup(t.getChillTransactionId());
            if (key.isEmpty()) {
                key = t.getChillTransactionId().trim();
            }
            PgTrnsctn prev = best.get(key);
            if (prev == null || (t.getCreatedAt() != null && prev.getCreatedAt() != null
                    && t.getCreatedAt().isAfter(prev.getCreatedAt()))) {
                best.put(key, t);
            }
        }
        Map<String, String> out = new HashMap<>();
        for (PgTrnsctn t : best.values()) {
            if (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank()) {
                continue;
            }
            String rawId = t.getChillTransactionId().trim();
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            out.put(rawId, st);
            String nk = normalizeChillTxnIdForDbLookup(rawId);
            if (!nk.isEmpty() && !nk.equalsIgnoreCase(rawId)) {
                out.put(nk, st);
            }
        }
        return out;
    }

    private static List<Map<String, Object>> filterSettlementRowsByIcopayStatusGroup(
            List<Map<String, Object>> rows,
            String group,
            Map<String, String> icopayStatusByNormChillId) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String raw = firstNonBlankString(row,
                    "transactionId", "TransactionId", "Transaction_Id", "transaction_id").trim();
            String key = raw.isEmpty() ? "" : normalizeChillTxnIdForDbLookup(raw);
            String ist = "";
            if (!key.isEmpty()) {
                ist = icopayStatusByNormChillId.getOrDefault(key, "");
            }
            if (ist.isEmpty() && !raw.isEmpty()) {
                ist = icopayStatusByNormChillId.getOrDefault(raw, "");
            }
            String chillSt = firstNonBlankString(row, "status", "Status");
            if (passesIcopaySettlementGroupFilter(group, ist, chillSt)) {
                out.add(row);
            }
        }
        return out;
    }

    private static boolean passesIcopaySettlementGroupFilter(String group, String icopayStatus, String chillSt) {
        String low = chillSt != null ? chillSt.toLowerCase(Locale.ROOT) : "";
        return switch (group) {
            case "REFUND" -> "30".equals(icopayStatus) || "42".equals(icopayStatus)
                    || (icopayStatus.isEmpty() && low.contains("refund") && !low.contains("force"));
            case "FORCE_REFUND" -> "31".equals(icopayStatus);
            case "VOID" -> "21".equals(icopayStatus) || "40".equals(icopayStatus)
                    || (icopayStatus.isEmpty() && low.contains("void") && !low.contains("email"));
            case "MANUAL_VOID" -> "22".equals(icopayStatus) || "41".equals(icopayStatus)
                    || (icopayStatus.isEmpty() && low.contains("email"));
            case "EXCLUDE_SUCCESS" -> {
                if ("10".equals(icopayStatus)) {
                    yield false;
                }
                if (!icopayStatus.isEmpty()) {
                    yield true;
                }
                yield !PayListStatusBarBuckets.SUCCESS.equals(PayListStatusBarBuckets.bucketForChillStatus(chillSt));
            }
            default -> true;
        };
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
            BigDecimal lineAmt = PayListStatusBarBuckets.parseMoney(row.containsKey("amount") ? row.get("amount") : null);
            if (lineAmt.compareTo(BigDecimal.ZERO) == 0) {
                lineAmt = PayListStatusBarBuckets.parseMoney(row.get("Amount"));
            }
            if (lineAmt.compareTo(BigDecimal.ZERO) == 0) {
                lineAmt = PayListStatusBarBuckets.parseMoney(row.get("settleAmount"));
            }
            if (lineAmt.compareTo(BigDecimal.ZERO) == 0) {
                lineAmt = PayListStatusBarBuckets.parseMoney(row.get("netAmount"));
            }
            roll.add(bucket, cur, lineAmt, 1L);
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

    /**
     * 통합내역(Search Payment Transaction)과 동일한 필드·형식으로 {@link ChillPayPaymentSearchApiRequest} 를 채운다.
     * 10번째 체크섬 슬롯은 결제 API에서는 {@code Status}, 정산 검색에서는 {@code Settled} 값(동일 문자열)이 들어간다.
     */
    private static void populateChillPayPaymentSearchRequest(
            ChillPayPaymentSearchApiRequest req,
            int pageSize,
            int pageNumber,
            String orderBy,
            String orderDir,
            String searchKeyword,
            String merchantCodeFilter,
            String paymentChannel,
            Integer routeNoFilter,
            String orderNo,
            String statusOrSettledSlot10,
            LocalDate transactionDateFrom,
            LocalDate transactionDateTo,
            LocalDate paymentDateFrom,
            LocalDate paymentDateTo) {
        req.setOrderBy(trimOrDefault(orderBy, "TransactionId"));
        req.setOrderDir("ASC".equalsIgnoreCase(trimOrEmpty(orderDir)) ? "ASC" : "DESC");
        req.setPageSize(pageSize);
        req.setPageNumber(pageNumber);
        req.setSearchKeyword(trimOrEmpty(searchKeyword));
        req.setMerchantCode(trimOrEmpty(merchantCodeFilter));
        req.setPaymentChannel(trimOrEmpty(paymentChannel));
        req.setRouteNo(routeNoFilter);
        req.setOrderNo(trimOrEmpty(orderNo));
        req.setStatus(trimOrEmpty(statusOrSettledSlot10));
        req.setTransactionDateFrom(transactionDateFrom != null ? transactionDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : "");
        req.setTransactionDateTo(transactionDateTo != null ? transactionDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : "");
        req.setPaymentDateFrom(paymentDateFrom != null ? paymentDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : "");
        req.setPaymentDateTo(paymentDateTo != null ? paymentDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : "");
    }

    /**
     * 통합정산 UI는 정렬을 안 보내는 경우가 많다. ChillPay Transaction Services 기본값과 같이 {@code TransactionId} 를 쓴다.
     */
    private static String normalizeChillPaySettlementOrderBy(String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            return "TransactionId";
        }
        return orderBy.trim();
    }

    private static String nullIfBlank(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /**
     * Table 2.2 본문 + {@link ChillPaySettlementSearchApiRequest#toChecksumPlainString()} 18필드 MD5.
     * 날짜 문자열은 통합내역(결제 검색)과 동일하게 {@code LocalDate#atStartOfDay()} / {@code atTime(23,59,59)} 로 만든다.
     * 미사용 문자열 필드는 ChillPay 문서 예시처럼 {@code null}(JSON 에 명시)로 둔다.
     * {@code merchantCodeForWire} 는 보통 헤더 CHILLPAY-MerchantCode 와 동일한 ChillPay MID — 업체코드 등은 SearchKeyword 로만 넘긴다.
     */
    private static void populateChillPaySettlementSearchWireRequest(
            ChillPaySettlementSearchApiRequest req,
            int pageSize,
            int pageNumber,
            String orderBy,
            String orderDir,
            String searchKeywordForWire,
            String merchantCodeForWire,
            String paymentChannel,
            Integer routeNoFilter,
            String orderNo,
            String settledToken,
            LocalDate transactionDateFrom,
            LocalDate transactionDateTo,
            LocalDate paymentDateFrom,
            LocalDate paymentDateTo) {
        req.setOrderBy(trimOrDefault(orderBy, "TransactionId"));
        req.setOrderDir("ASC".equalsIgnoreCase(trimOrEmpty(orderDir)) ? "ASC" : "DESC");
        req.setPageSize(pageSize);
        req.setPageNumber(pageNumber);
        req.setSearchKeyword(nullIfBlank(searchKeywordForWire));
        req.setMerchantCode(nullIfBlank(merchantCodeForWire));
        req.setPaymentChannel(nullIfBlank(paymentChannel));
        req.setRouteNo(routeNoFilter);
        req.setOrderNo(nullIfBlank(orderNo));
        req.setSettled(nullIfBlank(settledToken));
        req.setTransactionDateFrom(transactionDateFrom != null ? transactionDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : null);
        req.setTransactionDateTo(transactionDateTo != null ? transactionDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : null);
        req.setPaymentDateFrom(paymentDateFrom != null ? paymentDateFrom.atStartOfDay().format(CHILLPAY_TXN_DT) : null);
        req.setPaymentDateTo(paymentDateTo != null ? paymentDateTo.atTime(23, 59, 59).format(CHILLPAY_TXN_DT) : null);
        req.setTransferDateFrom(null);
        req.setTransferDateTo(null);
        req.setCutOffTimeDateFrom(null);
        req.setCutOffTimeDateTo(null);
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
            throw new IllegalStateException(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED);
        }
        Config cfg = resolveConfig(merchantOrgUnitId);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다.");
        }

        int ps = Math.min(CHILL_PAY_PAYMENT_PAGE_SIZE_MAX, Math.max(1, size));
        int pn = Math.max(1, page);

        ChillPayPaymentSearchApiRequest req = new ChillPayPaymentSearchApiRequest();
        populateChillPayPaymentSearchRequest(req, ps, pn, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, trimOrEmpty(status),
                transactionDateFrom, transactionDateTo, paymentDateFrom, paymentDateTo);

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
                if (isChillPayNoTransactionsStatus(body.getStatus())) {
                    return emptyChillPayPaymentSearchPage(pn, ps, cfg, url, body.getStatus(), body.getMessage());
                }
                String msg = body.getMessage() != null ? body.getMessage() : ("상태코드 " + body.getStatus());
                throw new IllegalStateException(msg);
            }
            List<Map<String, Object>> raw = body.getData() != null ? body.getData() : Collections.emptyList();
            List<Map<String, Object>> list = new ArrayList<>();
            int startNo = (pn - 1) * ps + 1;
            Map<String, Optional<OrgUnit>> orgCache = new HashMap<>();
            Map<String, TxnMerchantLookup> txnOrgCache = new HashMap<>();
            Map<Long, String> calcCycleByOrgId = new HashMap<>();
            for (int i = 0; i < raw.size(); i++) {
                Map<String, Object> row = wrapChillPayRow(raw.get(i), startNo + i);
                enrichChillPayTrSearchRow(row, orgCache, txnOrgCache, calcCycleByOrgId);
                list.add(row);
            }
            enrichSettlementRowsTxnStatusForChillGrid(list);
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
            meta.put("chillPaySandbox", cfg.sandbox());
            meta.put("chillPayTxnApiEnv", cfg.sandbox() ? "SANDBOX" : "PRODUCTION");
            meta.put("chillPayPaymentSearchUrl", url);
            pr.setMeta(meta);
            return pr;
        } catch (HttpClientErrorException ex) {
            String detail = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode()) && isChillPayNoTransactionsHttpBody(detail)) {
                return emptyChillPayPaymentSearchPage(pn, ps, cfg, url, 3002, "Transaction Not Found");
            }
            log.error("ChillPay Search Payment Transaction HTTP {}: {}", ex.getStatusCode(), detail);
            throw new IllegalStateException("ChillPay 거래 검색 API 호출 실패: " + ex.getStatusCode() + " " + detail, ex);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("ChillPay Search Payment Transaction 실패: {}", e.getMessage());
            throw new IllegalStateException("ChillPay 거래 검색 API 호출 실패: " + e.getMessage(), e);
        }
    }

    /** ChillPay: 조건에 맞는 거래 없음(HTTP 400 + status 3002 등) — 오류가 아닌 빈 목록 */
    private static boolean isChillPayNoTransactionsStatus(Integer status) {
        return status != null && status == 3002;
    }

    private static boolean isChillPayNoTransactionsHttpBody(String detail) {
        if (detail == null || detail.isBlank()) {
            return false;
        }
        return detail.contains("\"status\":3002") || detail.contains("\"status\": 3002");
    }

    private PageResult<Map<String, Object>> emptyChillPayPaymentSearchPage(int page, int size, Config cfg, String url,
                                                                           Integer chillStatus, String chillMessage) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(List.of());
        pr.setPage(Math.max(1, page));
        pr.setSize(Math.min(CHILL_PAY_PAYMENT_PAGE_SIZE_MAX, Math.max(1, size)));
        pr.setTotalElements(0L);
        pr.setTotalPages(1);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("chillPayMessage", chillMessage != null ? chillMessage : "Transaction Not Found");
        meta.put("chillPayStatus", chillStatus != null ? chillStatus : 3002);
        meta.put("chillPaySandbox", cfg.sandbox());
        meta.put("chillPayTxnApiEnv", cfg.sandbox() ? "SANDBOX" : "PRODUCTION");
        meta.put("chillPayPaymentSearchUrl", url);
        meta.put("chillPayNoTransactions", true);
        pr.setMeta(meta);
        return pr;
    }

    /**
     * 칠페이 정산 검색이 {@code 3001 Search Failed} 만 반환하는 경우(엔드포인트·계정 조합), 동일 필터로
     * {@link #searchChillPayPaymentTransactionsPage} 를 호출해 목록을 채운다. 통합내역과 동일 API다.
     */
    private PageResult<Map<String, Object>> chillPaySettlementSearchFallbackToPaymentSearch(
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
            LocalDate paymentDateTo,
            String attemptedSettlementUrl) {
        log.warn("ChillPay /settlement/search returned 3001 — falling back to /payment/search (same filters as 통합내역)");
        PageResult<Map<String, Object>> fb = searchChillPayPaymentTransactionsPage(
                merchantOrgUnitId, page, size, orderBy, orderDir, searchKeyword, merchantCodeFilter,
                paymentChannel, routeNoFilter, orderNo, status,
                transactionDateFrom, transactionDateTo, paymentDateFrom, paymentDateTo);
        List<Map<String, Object>> rows = fb.getList();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                applyChillSettlementSearchAliases(row);
            }
            enrichSettlementRowsTxnStatusForChillGrid(rows);
        }
        Map<String, Object> meta = fb.getMeta() != null ? new LinkedHashMap<>(fb.getMeta()) : new LinkedHashMap<>();
        meta.put("chillPaySettlementFallback", Boolean.TRUE);
        /** 결제 검색으로는 내려오지 않는 칠페이 정산 전용 필드(참고·UI 툴팁 등). */
        meta.put("chillPaySettlementFallbackUnsettledApiFields",
                List.of(
                        "TransferDate", "CutOffTime", "SettleAmount", "NetAmount",
                        "ExchangeRate", "ServiceAmount", "ServiceVAT", "ServiceWHT"));
        meta.put("chillPaySettlementSearchUrl", attemptedSettlementUrl);
        meta.put("chillPaySettlementApi", "payment_search_fallback_after_3001");
        fb.setMeta(meta);
        return fb;
    }

    /**
     * ChillPay Search Settlement Transaction — {@code /api/v1/settlement/search}.
     * 문서 Table 2.2: {@link ChillPaySettlementSearchApiRequest} + 18필드 {@link ChillPaySettlementSearchApiRequest#toChecksumPlainString()} + MD5.
     * 결제 검색과 달리 이체·컷오프 필드가 포함된다. 본문 {@code MerchantCode} 는 헤더 MID 와 맞추고 업체코드 필터는 SearchKeyword 로 합친다.
     */
    private PageResult<Map<String, Object>> searchChillPaySettlementTransactionsPage(
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
            throw new IllegalStateException(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED);
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("CHILLPAY-MerchantCode", cfg.merchantCode());
        headers.set("CHILLPAY-ApiKey", cfg.apiKey());

        String url = cfg.sandbox() ? TXN_SETTLEMENT_SEARCH_SB : TXN_SETTLEMENT_SEARCH_PR;

        String headerMid = trimOrEmpty(cfg.merchantCode());
        String userMc = trimOrEmpty(merchantCodeFilter);
        String kwIn = trimOrEmpty(searchKeyword);
        String kwWire = kwIn;
        if (!userMc.isEmpty() && !userMc.equalsIgnoreCase(headerMid)) {
            kwWire = kwIn.isEmpty() ? userMc : (userMc + " " + kwIn).trim();
        }
        String orderByEff = normalizeChillPaySettlementOrderBy(orderBy);

        ChillPaySettlementSearchApiRequest wire = new ChillPaySettlementSearchApiRequest();
        String settledSlot10 = settledFilterTokenForSettlementSearch(status);
        populateChillPaySettlementSearchWireRequest(wire, ps, pn, orderByEff, orderDir, kwWire, headerMid,
                paymentChannel, routeNoFilter, orderNo, settledSlot10,
                transactionDateFrom, transactionDateTo, paymentDateFrom, paymentDateTo);
        wire.setChecksum(md5(wire.toChecksumPlainString() + cfg.md5Key()));
        final String jsonPayload;
        try {
            jsonPayload = CHILLPAY_SETTLEMENT_JSON.writeValueAsString(wire);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ChillPay 정산 검색 요청 JSON 직렬화 실패", e);
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        ChillPayPaymentSearchApiResponse body;
        try {
            ResponseEntity<ChillPayPaymentSearchApiResponse> res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChillPayPaymentSearchApiResponse.class);
            body = res.getBody();
        } catch (HttpClientErrorException ex) {
            String detail = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("ChillPay Search Settlement Transaction HTTP {}: {}", ex.getStatusCode(), detail);
            if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode()) && detail != null && detail.contains("\"status\":3001")) {
                return chillPaySettlementSearchFallbackToPaymentSearch(
                        merchantOrgUnitId, page, size, orderByEff, orderDir, searchKeyword, merchantCodeFilter,
                        paymentChannel, routeNoFilter, orderNo, status,
                        transactionDateFrom, transactionDateTo, paymentDateFrom, paymentDateTo, url);
            }
            if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode()) && isChillPayNoTransactionsHttpBody(detail)) {
                return emptyChillPayPaymentSearchPage(pn, ps, cfg, url, 3002, "Transaction Not Found");
            }
            throw new IllegalStateException("ChillPay 정산 검색 API 호출 실패: " + ex.getStatusCode() + " " + detail, ex);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("ChillPay Search Settlement Transaction 실패: {}", e.getMessage());
            throw new IllegalStateException("ChillPay 정산 검색 API 호출 실패: " + e.getMessage(), e);
        }
        if (body == null) {
            throw new IllegalStateException("ChillPay 응답 본문이 비어 있습니다.");
        }
        if (body.getStatus() != null && body.getStatus() != 200) {
            if (Integer.valueOf(3001).equals(body.getStatus())) {
                return chillPaySettlementSearchFallbackToPaymentSearch(
                        merchantOrgUnitId, page, size, orderByEff, orderDir, searchKeyword, merchantCodeFilter,
                        paymentChannel, routeNoFilter, orderNo, status,
                        transactionDateFrom, transactionDateTo, paymentDateFrom, paymentDateTo, url);
            }
            if (isChillPayNoTransactionsStatus(body.getStatus())) {
                return emptyChillPayPaymentSearchPage(pn, ps, cfg, url, body.getStatus(), body.getMessage());
            }
            String msg = body.getMessage() != null ? body.getMessage() : ("상태코드 " + body.getStatus());
            throw new IllegalStateException(msg);
        }
        List<Map<String, Object>> raw = body.getData() != null ? body.getData() : Collections.emptyList();
        List<Map<String, Object>> list = new ArrayList<>();
        int startNo = (pn - 1) * ps + 1;
        Map<String, Optional<OrgUnit>> orgCache = new HashMap<>();
        Map<String, TxnMerchantLookup> txnOrgCache = new HashMap<>();
        Map<Long, String> calcCycleByOrgId = new HashMap<>();
        for (int i = 0; i < raw.size(); i++) {
            Map<String, Object> row = wrapChillPayRow(raw.get(i), startNo + i);
            applyChillSettlementSearchAliases(row);
            enrichChillPayTrSearchRow(row, orgCache, txnOrgCache, calcCycleByOrgId);
            list.add(row);
        }
        enrichSettlementRowsTxnStatusForChillGrid(list);

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
        meta.put("chillPaySandbox", cfg.sandbox());
        meta.put("chillPayTxnApiEnv", cfg.sandbox() ? "SANDBOX" : "PRODUCTION");
        meta.put("chillPaySettlementSearchUrl", url);
        meta.put("chillPaySettlementChecksumNote",
                "SearchSettlementTransaction: Table 2.2 @JsonPropertyOrder + 18-field MD5; JSON via ObjectMapper(ALWAYS) with explicit nulls; MerchantCode=header MID; default OrderBy=TransactionId");
        pr.setMeta(meta);
        return pr;
    }

    /**
     * 결제 검색용 Status 문자열은 정산 API Checksum의 Settled 와 무관하므로 비웁니다.
     * ChillPay 문서: Settled 필터 시 Checksum 에 True/False.
     */
    private static String settledFilterTokenForSettlementSearch(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String t = status.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(t) || "y".equals(t) || "1".equals(t)) {
            return "True";
        }
        if ("false".equals(t) || "n".equals(t) || "0".equals(t)) {
            return "False";
        }
        return "";
    }

    private static void applyChillSettlementSearchAliases(Map<String, Object> m) {
        aliasIfMissing(m, "netAmount", "NetAmount");
        aliasIfMissing(m, "settleAmount", "SettleAmount");
        aliasIfMissing(m, "exchangeRate", "ExchangeRate");
        aliasIfMissing(m, "serviceAmount", "ServiceAmount");
        aliasIfMissing(m, "serviceVAT", "ServiceVAT");
        aliasIfMissing(m, "serviceWHT", "ServiceWHT");
        aliasIfMissing(m, "transferDate", "TransferDate");
        aliasIfMissing(m, "cutOffTime", "CutOffTime");
        Object rawSettled = m.get("Settled");
        if (rawSettled == null) {
            rawSettled = m.get("settled");
        }
        m.put("settled", formatChillSettledDisplay(rawSettled));
        /* 그리드·엑셀이 Pascal 키를 읽으면 false 그대로 노출되므로 원문 키는 제거한다. */
        m.remove("Settled");
    }

    /**
     * ChillPay 정산 API {@code Settled}: 칠페이 측 정산대금 **이체(지급) 완료** 여부(문서상 boolean).
     * ICOPAY 내부 정산 배치·정산실행 완료와는 별개이며, 샌드박스·주기 전에는 대부분 false 가 정상인 경우가 많다.
     */
    private static String formatChillSettledDisplay(Object settledRaw) {
        if (settledRaw == null) {
            return "";
        }
        if (settledRaw instanceof Boolean b) {
            return b ? "정산완료" : "미정산";
        }
        if (settledRaw instanceof Number n) {
            int v = n.intValue();
            if (v != 0) {
                return "정산완료";
            }
            return "미정산";
        }
        String t = String.valueOf(settledRaw).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(t) || "y".equals(t) || "1".equals(t) || "yes".equals(t)) {
            return "정산완료";
        }
        if ("false".equals(t) || "n".equals(t) || "0".equals(t) || "no".equals(t)) {
            return "미정산";
        }
        return String.valueOf(settledRaw).trim();
    }

    /**
     * 통합정산 「정산(이체)」열: <strong>승인 성공</strong> 건만 ChillPay {@code Settled} 문구를 보이고,
     * 실패·취소·환불·무효 등은 비웁니다. {@code icopayExpectedSettleAt}(예정 ICOPAY)이 있으면 서울 기준 그 시각
     * <strong>이전</strong>에는 아직 정산으로 보이지 않도록 {@code 미정산}으로 둡니다(도래 후에는 ChillPay 값 유지).
     */
    private static void applyIcopayIntegratedSettlementSettledDisplayRules(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(seoul).withSecond(0).withNano(0);
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            String st = firstNonBlankString(row, "status", "Status");
            String bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
            if (!PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                row.put("settled", "");
                continue;
            }
            String expectedStr = firstNonBlankString(row, "icopayExpectedSettleAt");
            if (expectedStr.isEmpty()) {
                continue;
            }
            try {
                LocalDateTime expectedAt = LocalDateTime.parse(expectedStr.trim(), ICO_EXPECTED_SETTLE_AT_DT);
                if (now.isBefore(expectedAt)) {
                    row.put("settled", "미정산");
                }
            } catch (DateTimeParseException ignored) {
                // 예정일 파싱 실패 시 ChillPay 표기 유지
            }
        }
    }

    /**
     * 정산 검색 API 응답에는 결제 Status 가 없을 수 있음 — ICOPAY 노티 적재 건이 있으면 동일 승인번호로 보강.
     */
    private void enrichSettlementRowsTxnStatusForChillGrid(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<String> qids = new HashSet<>();
        for (Map<String, Object> row : rows) {
            if (!firstNonBlankString(row, "status", "Status").isEmpty()) {
                continue;
            }
            String raw = firstNonBlankString(row, "transactionId", "TransactionId").trim();
            if (raw.isEmpty()) {
                continue;
            }
            qids.add(raw);
            String n = normalizeChillTxnIdForDbLookup(raw);
            if (!n.isEmpty() && !n.equalsIgnoreCase(raw)) {
                qids.add(n);
            }
        }
        if (qids.isEmpty()) {
            return;
        }
        List<PgTrnsctn> list = pgTrnsctnRepository.findAllByChillTransactionIdIn(qids);
        Map<String, PgTrnsctn> best = new HashMap<>();
        for (PgTrnsctn t : list) {
            if (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank()) {
                continue;
            }
            String key = normalizeChillTxnIdForDbLookup(t.getChillTransactionId().trim());
            if (key.isEmpty()) {
                key = t.getChillTransactionId().trim();
            }
            PgTrnsctn prev = best.get(key);
            if (prev == null || (t.getCreatedAt() != null && prev.getCreatedAt() != null
                    && t.getCreatedAt().isAfter(prev.getCreatedAt()))) {
                best.put(key, t);
            }
        }
        Map<String, String> byRaw = new HashMap<>();
        for (PgTrnsctn t : best.values()) {
            if (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank()) {
                continue;
            }
            String rid = t.getChillTransactionId().trim();
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            byRaw.put(rid, st);
            String nk = normalizeChillTxnIdForDbLookup(rid);
            if (!nk.isEmpty() && !nk.equalsIgnoreCase(rid)) {
                byRaw.putIfAbsent(nk, st);
            }
        }
        for (Map<String, Object> row : rows) {
            if (!firstNonBlankString(row, "status", "Status").isEmpty()) {
                continue;
            }
            String raw = firstNonBlankString(row, "transactionId", "TransactionId").trim();
            if (raw.isEmpty()) {
                continue;
            }
            String nk = normalizeChillTxnIdForDbLookup(raw);
            String st = byRaw.getOrDefault(raw, nk.isEmpty() ? "" : byRaw.getOrDefault(nk, ""));
            if (!st.isEmpty()) {
                row.put("status", st);
            }
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
        aliasIfMissing(m, "merchant", "MerchantCode");
        aliasIfMissing(m, "merchant", "merchantCode");
        aliasIfMissing(m, "merchant", "Mid");
        aliasIfMissing(m, "merchant", "MID");
        aliasIfMissing(m, "merchant", "MerchantID");
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
        aliasIfMissing(m, "routeNo", "Route");
        aliasIfMissing(m, "routeNo", "RootNo");
        aliasIfMissing(m, "routeNo", "rootNo");
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

    /**
     * 통합내역·통합정산 그리드: 결제내역과 동일 키(trnDate, trnTime, payCompletedAt) + 업체관리(MID·Route) 매핑(compNm, compId).
     * API naive 시각은 본사 전산설정 표준시간대({@code display_timezone})로 해석합니다.
     * {@code trnDate}는 {@code yyyy-MM-dd}(ISO 날짜), {@code trnTime}은 시각만 표준+JP 두 줄, {@code payCompletedAt}은 일시 두 줄.
     */
    private void enrichChillPayTrSearchRow(Map<String, Object> m,
                                           Map<String, Optional<OrgUnit>> orgCache,
                                           Map<String, TxnMerchantLookup> txnOrgCache,
                                           Map<Long, String> calcCycleByOrgId) {
        enrichChillPayTrRowOrg(m, orgCache, txnOrgCache, calcCycleByOrgId);
        enrichChillPayTrRowDatesAndZones(m);
        ensureChillPayTrCalcCycle(m, calcCycleByOrgId);
    }

    private void ensureChillPayTrCalcCycle(Map<String, Object> m, Map<Long, String> calcCycleByOrgId) {
        Object existing = m.get("calcCycle");
        if (existing != null && !String.valueOf(existing).trim().isEmpty() && !"-".equals(String.valueOf(existing).trim())) {
            return;
        }
        m.put("calcCycle", "-");
    }

    private void putChillPayTrCalcCycleForOrg(Map<String, Object> m, Long orgUnitId, Map<Long, String> calcCycleByOrgId) {
        if (orgUnitId == null) {
            m.put("calcCycle", "-");
            return;
        }
        String cycle = calcCycleByOrgId.computeIfAbsent(orgUnitId, id ->
                settlementSettingRepository.findByOrgUnitId(id)
                        .map(SettlementSetting::getCalcCycle)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .orElse(""));
        m.put("calcCycle", cycle.isEmpty() ? "-" : cycle);
    }

    private void enrichChillPayTrRowDatesAndZones(Map<String, Object> m) {
        LocalDateTime tx = parseChillPayApiDateTime(firstNonBlankString(m, "transactionDate", "TransactionDate"));
        LocalDateTime pay = parseChillPayApiDateTime(firstNonBlankString(m, "paymentDate", "PaymentDate"));
        ZoneId primary = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId operational = hqLedgerSysSettingsService.resolveOperationalDisplayZoneId();
        java.util.Optional<ZoneId> viewOverride = ViewDisplayTimezoneResolver.currentRequestOverride();
        java.util.Optional<TxnDualLineSpec> dualOpt = resolveTxnDualLineForChillRow(m)
                .flatMap(d -> ViewDisplayTimezoneResolver.effectiveDualSpec(d, primary, operational, viewOverride));
        if (tx != null) {
            m.put("trnDate", ViewDisplayTimezoneResolver.formatIsoDate(
                    ViewDisplayTimezoneResolver.trnDateInZone(tx, primary, viewOverride)));
            m.put("trnTime", dualOpt.map(d -> TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(tx, d))
                    .orElseGet(() -> formatLedgerDualLineTimeOnly(tx, primary)));
        } else {
            m.put("trnDate", "");
            m.put("trnTime", "");
        }
        if (pay != null) {
            m.put("payCompletedAt", dualOpt.map(d -> TrnTimeDualZoneDisplay.formatWithSpecDateTime(pay, d))
                    .orElseGet(() -> formatLedgerDualLineDateTime(pay, primary)));
        } else {
            m.put("payCompletedAt", "");
        }
    }

    private String formatLedgerDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId interpretAsZone) {
        ZoneId primary = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId operational = hqLedgerSysSettingsService.resolveOperationalDisplayZoneId();
        TxnDualLineSpec base = hqLedgerSysSettingsService.resolveLedgerTxnDualLineSpec();
        TxnDualLineSpec spec = ViewDisplayTimezoneResolver.effectiveDualSpec(
                base, primary, operational, ViewDisplayTimezoneResolver.currentRequestOverride()).orElse(base);
        return TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(naiveWallClock, spec);
    }

    private String formatLedgerDualLineDateTime(LocalDateTime naiveWallClock, ZoneId interpretAsZone) {
        ZoneId primary = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId operational = hqLedgerSysSettingsService.resolveOperationalDisplayZoneId();
        TxnDualLineSpec base = hqLedgerSysSettingsService.resolveLedgerTxnDualLineSpec();
        TxnDualLineSpec spec = ViewDisplayTimezoneResolver.effectiveDualSpec(
                base, primary, operational, ViewDisplayTimezoneResolver.currentRequestOverride()).orElse(base);
        return TrnTimeDualZoneDisplay.formatWithSpecDateTime(naiveWallClock, spec);
    }

    private Optional<TxnDualLineSpec> resolveTxnDualLineForChillRow(Map<String, Object> m) {
        String compId = firstNonBlankString(m, "compId", "merchant", "Merchant", "MerchantCode", "merchantCode", "MID", "Mid");
        if (compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        String c = compId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(c);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(c);
        }
        if (ou.isEmpty() || ou.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return Optional.empty();
        }
        return java.util.Optional.of(hqLedgerSysSettingsService.resolveLedgerTxnDualLineSpec());
    }

    private void enrichChillPayTrRowOrg(Map<String, Object> m,
                                        Map<String, Optional<OrgUnit>> orgCache,
                                        Map<String, TxnMerchantLookup> txnOrgCache,
                                        Map<Long, String> calcCycleByOrgId) {
        String mid = chillTrMerchantRaw(m);
        String routeKey = normalizeChillRouteNoKey(chillTrRouteRaw(m));
        if (mid.isEmpty()) {
            m.put("compNm", "");
            m.put("compId", "");
            m.put("merchantNm", "");
        } else {
            String cacheKey = mid + "\0" + routeKey;
            Optional<OrgUnit> ou = orgCache.computeIfAbsent(cacheKey, k -> resolveOrgUnitForChillMidAndRoute(mid, routeKey));
            if (ou.isPresent()) {
                OrgUnit o = ou.get();
                m.put("compNm", o.getName() != null ? o.getName() : "");
                m.put("compId", o.getCode() != null ? o.getCode() : "");
                m.put("merchantNm", o.getName() != null ? o.getName() : mid);
                putChillPayTrCalcCycleForOrg(m, o.getId(), calcCycleByOrgId);
            } else {
                m.put("compNm", "");
                m.put("compId", "");
                m.put("merchantNm", mid);
            }
        }
        if (isBlankStr(m.get("compId")) || isBlankStr(m.get("compNm"))) {
            enrichChillPayTrRowOrgFromTxn(m, txnOrgCache, calcCycleByOrgId);
        }
    }

    /**
     * 1순위(MID·Route→바인딩) 후에도 업체명·코드가 비어 있으면, 결제 DB에 저장된 Chill TransactionId로 역추적한다.
     */
    private void enrichChillPayTrRowOrgFromTxn(Map<String, Object> m, Map<String, TxnMerchantLookup> txnOrgCache,
                                               Map<Long, String> calcCycleByOrgId) {
        String chillTxn = firstNonBlankString(m,
                "transactionId", "TransactionId", "Transaction_Id", "transaction_id",
                "chillTransactionId", "ChillTransactionId", "CHILL_TRANSACTION_ID");
        String mid = chillTrMerchantRaw(m);
        String cacheKey = chillTxn.trim() + "\0" + mid + "\0" + firstNonBlankString(m, "orderNo", "OrderNo");
        TxnMerchantLookup lk = txnOrgCache.computeIfAbsent(cacheKey, k -> resolveTxnMerchantLookup(chillTxn.trim(), mid, m));
        if ((lk.merchantId() == null || lk.merchantId().isBlank()) && lk.org().isEmpty()) {
            return;
        }
        if (lk.org().isPresent()) {
            OrgUnit ou = lk.org().get();
            if (isBlankStr(m.get("compNm"))) {
                m.put("compNm", nvl(ou.getName()));
            }
            if (isBlankStr(m.get("compId"))) {
                m.put("compId", nvl(ou.getCode(), lk.merchantId()));
            }
            if (isBlankStr(m.get("merchantNm")) || mid.equals(String.valueOf(m.getOrDefault("merchantNm", "")).trim())) {
                String display = nvl(ou.getName(), lk.merchantId());
                if (!display.isEmpty()) {
                    m.put("merchantNm", display);
                }
            }
            putChillPayTrCalcCycleForOrg(m, ou.getId(), calcCycleByOrgId);
        } else {
            if (isBlankStr(m.get("compId"))) {
                m.put("compId", lk.merchantId());
            }
            if (isBlankStr(m.get("merchantNm"))) {
                m.put("merchantNm", !mid.isEmpty() ? mid : lk.merchantId());
            }
        }
    }

    /**
     * 통합내역 행 보강: Chill 승인번호(TransactionId) 정규화 후 pg_trnsctn 조회, 실패 시 주문번호+가맹(MID)으로 NOTI·URL 등 origin 순 조회.
     */
    private TxnMerchantLookup resolveTxnMerchantLookup(String chillTxnId, String mid, Map<String, Object> row) {
        Optional<PgTrnsctn> tOpt = Optional.empty();
        String norm = normalizeChillTxnIdForDbLookup(chillTxnId);
        if (!norm.isEmpty()) {
            tOpt = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(norm);
            if (tOpt.isEmpty() && chillTxnId != null && !chillTxnId.isBlank() && !chillTxnId.trim().equals(norm)) {
                tOpt = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(chillTxnId.trim());
            }
            if (tOpt.isEmpty() && mid != null && !mid.isBlank()) {
                tOpt = pgTrnsctnRepository.findFirstByChillTransactionIdAndMerchantId(norm, mid.trim());
            }
        }
        if (tOpt.isEmpty() && row != null) {
            String orderNo = firstNonBlankString(row, "orderNo", "OrderNo", "order_no");
            String mer = mid != null && !mid.isBlank() ? mid.trim() : chillTrMerchantRaw(row);
            if (!orderNo.isEmpty() && !mer.isEmpty()) {
                for (String origin : List.of("NOTI", "URL", "API", "CHATBOT")) {
                    tOpt = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(mer, orderNo, origin);
                    if (tOpt.isPresent()) {
                        break;
                    }
                }
            }
        }
        if (tOpt.isEmpty()) {
            return new TxnMerchantLookup("", Optional.empty());
        }
        String merId = tOpt.get().getMerchantId();
        if (merId == null || merId.isBlank()) {
            return new TxnMerchantLookup("", Optional.empty());
        }
        merId = merId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merId);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merId);
        }
        return new TxnMerchantLookup(merId, ou);
    }

    /** JSON 숫자·과학적 표기 등과 DB varchar 저장값을 맞추기 위한 Chill TransactionId 정규화(최대 64자). */
    private static String normalizeChillTxnIdForDbLookup(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return "";
        }
        try {
            if (s.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
                BigDecimal bd = new BigDecimal(s);
                s = bd.stripTrailingZeros().toPlainString();
            }
        } catch (Exception ignored) {
            // keep s
        }
        if (s.endsWith(".0")) {
            int dot = s.indexOf('.');
            if (dot > 0 && "0".equals(s.substring(dot + 1))) {
                s = s.substring(0, dot);
            }
        }
        if (s.length() > 64) {
            s = s.substring(0, 64);
        }
        return s;
    }

    private Optional<OrgUnit> resolveOrgUnitForChillMidAndRoute(String mid, String routeKey) {
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByMidOrderByOperationalYnDescIdAsc(mid);
        if (list.isEmpty()) {
            list = merchantPgBindingRepository.findByMidIgnoreCaseOrderByOperationalYnDescIdAsc(mid);
        }
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return selectBindingForChillRoute(list, routeKey)
                .flatMap(binding -> orgUnitRepository.findById(binding.getOrgUnitId()));
    }

    /**
     * 노티 {@link PgNotifyReceiveService#resolveBindingFromList} 와 동일: Route 일치 우선, 없으면 root 비어 있는 행, 마지막으로 첫 행.
     */
    private static Optional<MerchantPgBinding> selectBindingForChillRoute(List<MerchantPgBinding> list, String routeKey) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        if (routeKey.isEmpty()) {
            return Optional.of(list.get(0));
        }
        Optional<MerchantPgBinding> exact = list.stream()
                .filter(b -> b.getRootNo() != null && routeKeyEquals(routeKey, b.getRootNo().trim()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return list.stream()
                .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                .findFirst()
                .or(() -> Optional.of(list.get(0)));
    }

    private static boolean routeKeyEquals(String routeKey, String bindingRoot) {
        if (bindingRoot == null || bindingRoot.isEmpty()) {
            return false;
        }
        if (routeKey.equals(bindingRoot)) {
            return true;
        }
        try {
            return Integer.parseInt(routeKey) == Integer.parseInt(bindingRoot.trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** ChillPay Transaction Search 행에서 Route 번호 원문 (필드명 제각각 대응) */
    private static Object chillTrRouteRaw(Map<String, Object> m) {
        if (m == null) {
            return null;
        }
        for (String k : new String[]{
                "routeNo", "RouteNo", "Route", "route",
                "rootNo", "RootNo", "ROOT_NO", "RouteNumber", "routeNumber"
        }) {
            if (m.containsKey(k) && m.get(k) != null) {
                return m.get(k);
            }
        }
        return null;
    }

    private static String chillTrMerchantRaw(Map<String, Object> m) {
        return firstNonBlankString(m,
                "merchant", "Merchant", "merchantCode", "MerchantCode",
                "Mid", "MID", "MerchantID", "merchantID");
    }

    private static boolean isBlankStr(Object o) {
        if (o == null) {
            return true;
        }
        return String.valueOf(o).trim().isEmpty();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String nvl(String s, String fallback) {
        if (s != null && !s.trim().isEmpty()) {
            return s.trim();
        }
        return fallback != null ? fallback : "";
    }

    private static String normalizeChillRouteNoKey(Object routeNo) {
        if (routeNo == null) {
            return "";
        }
        String s = String.valueOf(routeNo).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return "";
        }
        if (s.endsWith(".0") && s.length() > 2) {
            try {
                double d = Double.parseDouble(s);
                if (d == Math.rint(d)) {
                    return String.valueOf((long) d);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return s;
    }

    private static LocalDateTime parseChillPayApiDateTime(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), CHILLPAY_TXN_DT);
        } catch (DateTimeParseException e) {
            return null;
        }
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

    /**
     * ChillPay Transaction API — Request Void (미정산).
     * 문서 §6 Table 6.1–6.2: URL {@code /api/v1/void/request}, 본문 {@code TransactionId}+{@code Checksum},
     * Checksum = {@code MD5(TransactionId + MD5 Secret Key)}.
     */
    public String requestChillPayVoid(long merchantOrgUnitId, long chillPayTransactionId) {
        return postChillPayTxnMutate(merchantOrgUnitId, chillPayTransactionId, false);
    }

    /**
     * ChillPay Transaction API — Request Refund (정산 후).
     * 문서 §7 Table 7.1–7.2: URL {@code /api/v1/refund/request}; 선택 {@code RefundAmount},{@code RequestNote} 미전송 시 전액 환불.
     * Checksum 은 표 7.2 비고와 같이 {@code TransactionId + MD5 Secret Key} 만 연결 후 MD5 (현재 구현은 전액 환불만).
     */
    public String requestChillPayRefund(long merchantOrgUnitId, long chillPayTransactionId) {
        return postChillPayTxnMutate(merchantOrgUnitId, chillPayTransactionId, true);
    }

    @SuppressWarnings("unchecked")
    private String postChillPayTxnMutate(long merchantOrgUnitId, long chillPayTransactionId, boolean refund) {
        Config cfg = resolveConfig(merchantOrgUnitId);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다.");
        }
        String checksum = md5(String.valueOf(chillPayTransactionId) + cfg.md5Key());
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("TransactionId", chillPayTransactionId);
        req.put("Checksum", checksum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("CHILLPAY-MerchantCode", cfg.merchantCode());
        headers.set("CHILLPAY-ApiKey", cfg.apiKey());

        String url = refund
                ? (cfg.sandbox() ? TXN_REFUND_REQUEST_SB : TXN_REFUND_REQUEST_PR)
                : (cfg.sandbox() ? TXN_VOID_REQUEST_SB : TXN_VOID_REQUEST_PR);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
        try {
            Map<String, Object> body = restTemplate.postForObject(url, entity, Map.class);
            assertChillPayTxnMutateOk(body);
            return chillPayTxnMutateMessage(body);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalStateException("ChillPay API 호출 실패: " + msg, e);
        }
    }

    private static String chillPayTxnMutateMessage(Map<String, Object> body) {
        if (body == null) {
            return "OK";
        }
        Object msg = body.get("message");
        if (msg == null) {
            msg = body.get("Message");
        }
        return msg != null && !String.valueOf(msg).isBlank() ? String.valueOf(msg).trim() : "OK";
    }

    private static void assertChillPayTxnMutateOk(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalStateException("ChillPay 응답 본문이 비어 있습니다.");
        }
        Object st = body.get("status");
        if (st == null) {
            st = body.get("Status");
        }
        Object cd = body.get("code");
        if (cd == null) {
            cd = body.get("Code");
        }
        /* 표 6.3·7.3·Appendix C: status 는 string, 코드 200 = Success (JSON 에 숫자로 올 수도 있음) */
        if (!chillPayTxnResponseSuccessStatus(st) && !chillPayTxnResponseSuccessStatus(cd)) {
            Object msg = body.get("message");
            if (msg == null) {
                msg = body.get("Message");
            }
            String m = msg != null ? String.valueOf(msg) : "ChillPay 처리에 실패했습니다.";
            throw new IllegalStateException(m);
        }
    }

    /** ChillPay-API-Transaction-Services-Document-EN v1.0.6 Appendix C — 성공 코드 200 */
    private static boolean chillPayTxnResponseSuccessStatus(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Number n) {
            return n.intValue() == 200;
        }
        String s = String.valueOf(o).trim();
        if ("200".equals(s)) {
            return true;
        }
        try {
            return Integer.parseInt(s) == 200;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
