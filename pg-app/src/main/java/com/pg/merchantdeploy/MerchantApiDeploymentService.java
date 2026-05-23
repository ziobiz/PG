package com.pg.merchantdeploy;

import com.pg.api.dto.PageResult;
import com.pg.entity.HqApiConfig;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantIcopayBrokerCredential;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.HqNotifyEnvConfigRepository;
import com.pg.repository.MerchantIcopayBrokerCredentialRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.service.CompService;
import com.pg.service.HqNotifyEnvService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MerchantApiDeploymentService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantIcopayBrokerCredentialRepository credentialRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final MerchantPgBrokerCatalog brokerCatalog;
    private final CompService compService;

    public MerchantApiDeploymentService(OrgUnitRepository orgUnitRepository,
                                        MerchantProfileRepository merchantProfileRepository,
                                        MerchantPgBindingRepository merchantPgBindingRepository,
                                        MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                        MerchantIcopayBrokerCredentialRepository credentialRepository,
                                        HqApiConfigRepository hqApiConfigRepository,
                                        HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository,
                                        HqNotifyEnvService hqNotifyEnvService,
                                        MerchantPgBrokerCatalog brokerCatalog,
                                        CompService compService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.credentialRepository = credentialRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqNotifyEnvConfigRepository = hqNotifyEnvConfigRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.brokerCatalog = brokerCatalog;
        this.compService = compService;
    }

    public List<Map<String, Object>> listVendors() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MerchantPgBrokerRouteDefinition d : brokerCatalog.definitions()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vendorScope", d.vendorScope());
            m.put("displayNameKr", d.displayNameKr());
            m.put("pathSegment", d.pathSegment());
            out.add(m);
        }
        return out;
    }

    public PageResult<Map<String, Object>> searchMerchants(String searchCompId, String searchCompNm,
                                                           int page, int size,
                                                           String scopeCompId, boolean scopeSubtreeBelow) {
        /* compDiv=MERCHANT(조직단계). 넷째 인자는 useYn(Y/N/ALL) — 여기에 "MERCHANT"를 넣으면 사용여부와 비교되어 목록이 항상 0건이 됨 */
        return compService.search(searchCompId, searchCompNm, "MERCHANT", null, null, null,
                null, null, null, null, page, size, scopeCompId, scopeSubtreeBelow);
    }

    public Map<String, Object> buildKit(String compId, String vendorScope, HttpServletRequest req) {
        String cid = compId != null ? compId.trim() : "";
        if (cid.isEmpty()) {
            throw new IllegalArgumentException("compId가 필요합니다.");
        }
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("업체코드를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점(조직단계 MERCHANT)만 키트를 조회할 수 있습니다.");
        }
        String vs = MerchantPgBrokerVendor.normalizeScope(vendorScope);
        MerchantPgBrokerRouteDefinition routeDef = brokerCatalog.findByVendorScope(vs);
        if (routeDef == null && !MerchantPgBrokerVendor.ALL.equals(vs)) {
            throw new IllegalArgumentException("지원하지 않는 vendorScope 입니다: " + vs);
        }

        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        HqNotifyEnvConfig notifyCfg = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc().orElse(null);

        String publicApiBase = trimSlash(hq != null ? hq.getPublicApiBaseUrl() : null);
        if (publicApiBase == null || publicApiBase.isBlank()) {
            publicApiBase = trimSlash(hqNotifyEnvService.getOrCreate().getPublicBaseUrl());
        }
        if (publicApiBase == null || publicApiBase.isBlank()) {
            publicApiBase = inferBaseFromRequest(req);
        }

        Map<String, Object> kit = new LinkedHashMap<>();
        kit.put("compId", ou.getCode());
        kit.put("merchantOrgUnitId", ou.getId());
        kit.put("merchantName", ou.getName());
        kit.put("publicApiBaseUrl", publicApiBase);
        kit.put("vendorScopeRequested", vs);

        kit.put("headersRecommended", Map.of(
                MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET,
                "브로커 시크릿을 발급하고 「강제」로 두면 필수입니다.",
                "Content-Type", "application/json (POST 시)",
                "Accept", "application/json"
        ));

        if (notifyCfg != null) {
            String token = notifyCfg.getIngressToken() != null ? notifyCfg.getIngressToken() : "";
            kit.put("notifyIngressUrlOpen", publicApiBase + PgNotifyIngressPaths.OPEN_PREFIX + token);
            kit.put("notifyIngressUrlMiddleware", publicApiBase + PgNotifyIngressPaths.MIDDLEWARE_PREFIX + token);
        } else {
            kit.put("notifyIngressUrlOpen", "");
            kit.put("notifyIngressUrlMiddleware", "");
        }

        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.getId());
        mpOpt.ifPresent(mp -> kit.put("baseCurrency", mp.getBaseCurrency() != null ? mp.getBaseCurrency() : ""));

        List<Map<String, Object>> bindings = new ArrayList<>();
        for (MerchantPgBinding b : merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pgCd", b.getPgCd());
            row.put("payMethod", b.getPayMethod());
            row.put("mid", b.getMid());
            row.put("rootNo", b.getRootNo());
            row.put("operationalYn", b.getOperationalYn());
            row.put("activationYn", b.getActivationYn());
            row.put("apiKeyMasked", maskSecret(b.getApiKey()));
            row.put("ivKeyMasked", maskSecret(b.getIvKey()));
            bindings.add(row);
        }
        kit.put("merchantPgBindings", bindings);

        List<Map<String, Object>> notifyUrls = new ArrayList<>();
        merchantNotifyUrlRepository.findByOrgUnitIdOrderByUrlTypeAsc(ou.getId()).forEach(nu -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("urlType", nu.getUrlType());
            row.put("notiUrl", nu.getNotiUrl());
            notifyUrls.add(row);
        });
        kit.put("merchantNotifyUrls", notifyUrls);

        List<Map<String, Object>> brokerBlocks = new ArrayList<>();
        for (MerchantPgBrokerRouteDefinition def : brokerCatalog.definitions()) {
            if (!MerchantPgBrokerVendor.ALL.equals(vs) && !def.vendorScope().equalsIgnoreCase(vs)) {
                continue;
            }
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("vendorScope", def.vendorScope());
            block.put("displayNameKr", def.displayNameKr());
            List<Map<String, String>> endpoints = new ArrayList<>();
            for (int i = 0; i < def.brokerRelativePaths().size(); i++) {
                String br = def.brokerRelativePaths().get(i);
                String leg = i < def.legacyRelativePaths().size() ? def.legacyRelativePaths().get(i) : "";
                Map<String, String> ep = new LinkedHashMap<>();
                ep.put("label", methodHint(br));
                ep.put("brokerUrl", publicApiBase + br);
                ep.put("legacyUrl", publicApiBase + leg);
                endpoints.add(ep);
            }
            block.put("endpoints", endpoints);
            block.put("brokerBasePath", publicApiBase + "/api/middleware/v1/pg/" + def.pathSegment());
            block.put("legacyBasePath", publicApiBase + "/api/pay/" + def.pathSegment());
            brokerBlocks.add(block);
        }
        kit.put("pgBrokerBlocks", brokerBlocks);

        if (MerchantPgBrokerVendor.ALL.equals(vs) || MerchantPgBrokerVendor.CHILLPAY.equals(vs)) {
            kit.put("merchantInlineCheckoutChillPay", buildMerchantInlineCheckoutBlock(publicApiBase, ou.getCode(), "chillpay"));
        }
        if (MerchantPgBrokerVendor.ALL.equals(vs) || MerchantPgBrokerVendor.JPAY.equals(vs)) {
            kit.put("merchantInlineCheckoutJpay", buildMerchantInlineCheckoutBlock(publicApiBase, ou.getCode(), "jpay"));
        }

        kit.put("merchantIntegrationSamples", buildIntegrationSamples(publicApiBase));

        kit.put("credentialScopes", credentialSummaries(ou.getId()));

        kit.put("integrationChecklist", List.of(
                "API배포설정의 publicApiBaseUrl(또는 노티 publicBaseUrl)이 가맹점·PG사에 알려준 도메인과 일치하는지 확인",
                "PHP/JSP 샘플: " + publicApiBase + "/merchant-api-samples/ (README.txt · icopay_config · IcopayMerchantApi)",
                "ChillPay 인라인(가맹 API): 가맹 서버 POST "
                        + publicApiBase + "/api/middleware/v1/merchant/chillpay/inline-checkout/prepare → sessionToken → /v1/embed-pay/{compId}",
                "JPAY 인라인(가맹 API): 가맹 서버 POST "
                        + publicApiBase + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare → sessionToken → /v1/embed-jpay-pay/{compId}",
                "가맹 PHP/JSP: prepare 는 반드시 가맹 서버에서 호출(브로커 시크릿 노출 금지). 브라우저에는 sessionToken·embed 스크립트만 전달",
                "ChillPay 콜백·리다이렉트 URL은 본사 API연동설정·노티구성과 동일하게 유지",
                "JPAY pay_notifyurl·콜백은 기본적으로 notifyIngressUrlMiddleware 경로를 사용합니다. 레거시만 필요하면 tb_pg_agency credentials_extra_json 에 jpayNotifyIngressStyle=OPEN",
                "브로커 시크릿을 발급한 뒤 「강제」로 설정하면 /api/middleware/v1/pg/... 호출에 "
                        + MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET + " 헤더가 필수입니다",
                "레거시 /api/pay/... 경로는 시크릿 검증 없이 동작합니다(이행 기간용)"
        ));

        return kit;
    }

    private Map<String, Object> buildMerchantInlineCheckoutBlock(String publicApiBase, String compId, String vendorKey) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        boolean jpay = "jpay".equalsIgnoreCase(vendorKey);
        String vendorScope = jpay ? MerchantPgBrokerVendor.JPAY : MerchantPgBrokerVendor.CHILLPAY;
        String apiPath = jpay
                ? "/api/middleware/v1/merchant/jpay/inline-checkout"
                : "/api/middleware/v1/merchant/chillpay/inline-checkout";
        String embedPath = jpay ? "/v1/embed-jpay-pay/" : "/v1/embed-pay/";
        String payPath = jpay ? "/jpay-pay/" : "/pay/";
        String pageDesc = jpay
                ? "JPAY 인라인 결제 — ICOPAY jpay-pay.html(카드·3DS)을 iframe으로 삽입"
                : "ChillPay 인라인 결제 — ICOPAY pay.html(CCD·다중결제)을 iframe으로 삽입";
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("integrationMode", "INLINE");
        block.put("pgVendor", vendorScope);
        block.put("descriptionKr", pageDesc);
        block.put("prepareUrl", base + apiPath + "/prepare");
        block.put("sessionUrl", base + apiPath + "/session?token={sessionToken}");
        block.put("statusUrl", base + apiPath + "/status?compId=" + compId + "&orderNo={orderNo}");
        block.put("embedScriptUrl", base + embedPath + compId);
        block.put("payPagePathTemplate", base + payPath + compId + "?entry=merchant_api&embed=1&session={sessionToken}&lang={langCode}");
        block.put("prepareBodyExample", Map.of(
                "compId", compId,
                "orderNo", "ORD-001",
                "amount", jpay ? 100 : 10000,
                "currency", jpay ? "USD" : "JPY",
                "productName", "상품명",
                "lang", "ENG"
        ));
        block.put("langHintKr",
                "결제창 언어: prepare JSON lang/langCode/locale (KOR|ENG|JPN|CHN|THA). "
                        + "생략 시 embed 스크립트가 가맹 페이지 html[lang]·브라우저 언어를 자동 감지합니다. "
                        + "수동 지정: embed script data-lang=\"JPN\"");
        block.put("embedScriptExample",
                "<div id=\"" + (jpay ? "icopay-jpay-checkout" : "icopay-pay-checkout") + "\"></div>\n"
                        + "<script src=\"" + base + embedPath + compId + "\"\n"
                        + "  data-session-token=\"{sessionToken}\"\n"
                        + "  data-target=\"" + (jpay ? "icopay-jpay-checkout" : "icopay-pay-checkout") + "\"\n"
                        + "  data-lang=\"{langCode}\" async defer charset=\"utf-8\"></script>");
        block.put("postMessageEvent", "ICOPAY_INLINE_CHECKOUT");
        block.put("phpClientFile", "merchant-api-samples/php/IcopayMerchantApi.php");
        block.put("jspClientFile", "merchant-api-samples/jsp/IcopayMerchantApi.sample.java");
        block.put("phpCheckoutExample", jpay ? "merchant-api-samples/php/checkout_jpay.php" : "merchant-api-samples/php/checkout_chillpay.php");
        block.put("jspCheckoutExample", jpay ? "merchant-api-samples/jsp/checkout-jpay.jsp" : "merchant-api-samples/jsp/checkout-chillpay.jsp");
        return block;
    }

    private Map<String, Object> buildIntegrationSamples(String publicApiBase) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("downloadBaseUrl", base + "/merchant-api-samples/");
        m.put("indexUrl", base + "/merchant-api-samples/index.html");
        m.put("readmeUrl", base + "/merchant-api-samples/README.txt");
        m.put("commonJsUrl", base + "/merchant-api-samples/common/icopay-checkout.js");
        m.put("php", Map.of(
                "configExample", "merchant-api-samples/php/icopay_config.example.php",
                "client", "merchant-api-samples/php/IcopayMerchantApi.php",
                "checkoutChillpay", "merchant-api-samples/php/checkout_chillpay.php",
                "checkoutJpay", "merchant-api-samples/php/checkout_jpay.php",
                "notifyWebhook", "merchant-api-samples/php/notify_webhook.php"
        ));
        m.put("jsp", Map.of(
                "configExample", "merchant-api-samples/jsp/icopay-config.example.properties",
                "clientSample", "merchant-api-samples/jsp/IcopayMerchantApi.sample.java",
                "checkoutChillpay", "merchant-api-samples/jsp/checkout-chillpay.jsp",
                "checkoutJpay", "merchant-api-samples/jsp/checkout-jpay.jsp",
                "notifyWebhook", "merchant-api-samples/jsp/notify-webhook.jsp"
        ));
        m.put("workflowKr",
                "1) 가맹 DB 주문(PENDING) 2) PHP/JSP 서버에서 inline-checkout/prepare 3) sessionToken으로 embed HTML 출력 "
                        + "4) iframe postMessage 또는 merchantNotifyUrls 웹훅으로 PAID 확인");
        m.put("securityNoteKr", "브로커 시크릿(X-Icopay-Merchant-Broker-Secret)은 가맹 서버(PHP/JSP)에만 두고 브라우저·앱에 노출하지 마세요.");
        return m;
    }

    private List<Map<String, Object>> credentialSummaries(Long orgUnitId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MerchantIcopayBrokerCredential c : credentialRepository.findByOrgUnitIdAndUseYnOrderByIdDesc(orgUnitId, "Y")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vendorScope", c.getVendorScope());
            m.put("secretPrefix", c.getSecretPrefix());
            m.put("enforceYn", c.getEnforceYn());
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            rows.add(m);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> rotateBrokerSecret(String compId, String vendorScope) {
        String cid = compId != null ? compId.trim() : "";
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("업체코드를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 시크릿을 발급할 수 있습니다.");
        }
        String scope = MerchantPgBrokerVendor.normalizeScope(vendorScope);
        if (!MerchantPgBrokerVendor.isKnownVendorScope(scope)) {
            throw new IllegalArgumentException("vendorScope 가 올바르지 않습니다.");
        }
        credentialRepository.findByOrgUnitIdAndVendorScopeAndUseYn(ou.getId(), scope, "Y")
                .ifPresent(old -> {
                    old.setUseYn("N");
                    old.setRotatedAt(LocalDateTime.now());
                    credentialRepository.save(old);
                });
        String secret = MerchantBrokerSecretGenerator.newSecret(40);
        MerchantIcopayBrokerCredential n = new MerchantIcopayBrokerCredential();
        n.setOrgUnitId(ou.getId());
        n.setVendorScope(scope);
        n.setBrokerSecret(secret);
        n.setSecretPrefix(MerchantBrokerSecretGenerator.prefixOf(secret));
        n.setUseYn("Y");
        n.setEnforceYn("Y");
        credentialRepository.save(n);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", ou.getCode());
        out.put("vendorScope", scope);
        out.put("brokerSecretPlain", secret);
        out.put("message", "이 응답의 brokerSecretPlain 은 이번 한 번만 표시됩니다. 가맹점에 안전한 채널로 전달하세요.");
        return out;
    }

    @Transactional
    public Map<String, Object> setEnforce(String compId, String vendorScope, boolean enforce) {
        String cid = compId != null ? compId.trim() : "";
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("업체코드를 찾을 수 없습니다."));
        String scope = MerchantPgBrokerVendor.normalizeScope(vendorScope);
        MerchantIcopayBrokerCredential c = credentialRepository
                .findByOrgUnitIdAndVendorScopeAndUseYn(ou.getId(), scope, "Y")
                .orElseThrow(() -> new IllegalStateException("활성 시크릿이 없습니다. 먼저 발급하세요."));
        c.setEnforceYn(enforce ? "Y" : "N");
        credentialRepository.save(c);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compId", ou.getCode());
        m.put("vendorScope", scope);
        m.put("enforceYn", c.getEnforceYn());
        return m;
    }

    private static String methodHint(String path) {
        String u = path.toUpperCase(Locale.ROOT);
        if (u.endsWith("/REQUEST") || u.endsWith("/SALE")) {
            return "POST";
        }
        return "GET";
    }

    private static String maskSecret(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        if (s.length() <= 4) {
            return "****";
        }
        return s.substring(0, 2) + "…" + s.substring(s.length() - 2);
    }

    private static String trimSlash(String u) {
        if (u == null) {
            return "";
        }
        return u.trim().replaceAll("/+$", "");
    }

    private static String inferBaseFromRequest(HttpServletRequest req) {
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = req.getScheme();
        }
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = req.getServerName();
            int port = req.getServerPort();
            if (("http".equalsIgnoreCase(scheme) && port != 80) || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }
}
