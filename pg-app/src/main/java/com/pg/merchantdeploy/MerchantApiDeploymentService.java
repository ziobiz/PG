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
            kit.put("notifyIngressUrlOpen", publicApiBase + "/api/open/pg-notify/" + token);
            kit.put("notifyIngressUrlMiddleware", publicApiBase + "/api/middleware/notify/v1/pg-notify/" + token);
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

        kit.put("credentialScopes", credentialSummaries(ou.getId()));

        kit.put("integrationChecklist", List.of(
                "API배포설정의 publicApiBaseUrl(또는 노티 publicBaseUrl)이 가맹점·PG사에 알려준 도메인과 일치하는지 확인",
                "ChillPay 콜백·리다이렉트 URL은 본사 API연동설정·노티구성과 동일하게 유지",
                "브로커 시크릿을 발급한 뒤 「강제」로 설정하면 /api/middleware/v1/pg/... 호출에 "
                        + MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET + " 헤더가 필수입니다",
                "레거시 /api/pay/... 경로는 시크릿 검증 없이 동작합니다(이행 기간용)"
        ));

        return kit;
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
