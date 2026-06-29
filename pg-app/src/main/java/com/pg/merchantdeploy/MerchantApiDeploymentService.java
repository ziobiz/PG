package com.pg.merchantdeploy;

import com.pg.api.dto.PageResult;
import com.pg.entity.HqApiConfig;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantIcopayBrokerCredential;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.HqNotifyEnvConfigRepository;
import com.pg.repository.MerchantIcopayBrokerCredentialRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.service.ChillPayService;
import com.pg.service.CompService;
import com.pg.service.HqNotifyEnvService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pg.merchantdeploy.MerchantDeployL10n.Bundle;

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
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantApiIntegrationChannelService integrationChannelService;
    private final ChillPayService chillPayService;

    public MerchantApiDeploymentService(OrgUnitRepository orgUnitRepository,
                                        MerchantProfileRepository merchantProfileRepository,
                                        MerchantPgBindingRepository merchantPgBindingRepository,
                                        MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                        MerchantIcopayBrokerCredentialRepository credentialRepository,
                                        HqApiConfigRepository hqApiConfigRepository,
                                        HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository,
                                        HqNotifyEnvService hqNotifyEnvService,
                                        MerchantPgBrokerCatalog brokerCatalog,
                                        CompService compService,
                                        PgAgencyRepository pgAgencyRepository,
                                        MerchantApiIntegrationChannelService integrationChannelService,
                                        ChillPayService chillPayService) {
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
        this.pgAgencyRepository = pgAgencyRepository;
        this.integrationChannelService = integrationChannelService;
        this.chillPayService = chillPayService;
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
        PageResult<Map<String, Object>> result = compService.search(searchCompId, searchCompNm, "MERCHANT", null, null, null,
                null, null, null, null, page, size, scopeCompId, scopeSubtreeBelow, false);
        enrichMerchantListRows(result.getList());
        return result;
    }

    private void enrichMerchantListRows(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> orgUnitIds = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Object id = row.get("id");
            if (id instanceof Number n) {
                orgUnitIds.add(n.longValue());
            }
        }
        if (orgUnitIds.isEmpty()) {
            return;
        }
        Map<Long, List<MerchantPgBinding>> bindingsByOrg = merchantPgBindingRepository
                .findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAsc(orgUnitIds)
                .stream()
                .collect(Collectors.groupingBy(MerchantPgBinding::getOrgUnitId));
        Map<String, String> pgNmByCd = new HashMap<>();
        for (PgAgency agency : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (agency.getPgCd() == null || agency.getPgCd().isBlank()) {
                continue;
            }
            String key = agency.getPgCd().trim().toUpperCase(Locale.ROOT);
            String label = agency.getPgNm() != null && !agency.getPgNm().isBlank()
                    ? agency.getPgNm().trim()
                    : agency.getPgCd().trim();
            pgNmByCd.put(key, label);
        }
        Map<Long, OrgUnit> orgById = orgUnitRepository.findAll().stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        Map<Long, MerchantIcopayBrokerCredential> brokerCredByOrg = pickLatestBrokerCredentialByOrg(
                credentialRepository.findByOrgUnitIdInAndUseYn(orgUnitIds, "Y"));
        for (Map<String, Object> row : list) {
            Object id = row.get("id");
            Long ouId = id instanceof Number n ? n.longValue() : null;
            List<MerchantPgBinding> bindings = ouId != null ? bindingsByOrg.get(ouId) : null;
            row.put("pgAgency", formatMerchantPgAgencyDisplay(bindings, pgNmByCd));
            row.put("masterDistNm", resolveNearestMasterDistName(ouId, orgById));
            MerchantIcopayBrokerCredential brokerCred = ouId != null ? brokerCredByOrg.get(ouId) : null;
            row.put("brokerSecretStatus", brokerSecretStatusCode(brokerCred));
            row.put("brokerIssuedDate", formatBrokerIssuedDate(brokerCred));
            row.put("brokerIssuedBy", brokerIssuedByDisplay(brokerCred));
            if (ouId != null) {
                row.put("apiIntegrationChannel", integrationChannelService.buildEffectiveChannelDisplayCode(ouId));
            } else {
                row.put("apiIntegrationChannel", "-");
            }
        }
    }

    /** org별 활성 브로커 시크릿 중 최신 발행(또는 재발행) 행 — 동률 시 ALL 범위 우선 */
    private static Map<Long, MerchantIcopayBrokerCredential> pickLatestBrokerCredentialByOrg(
            List<MerchantIcopayBrokerCredential> creds) {
        Map<Long, MerchantIcopayBrokerCredential> best = new HashMap<>();
        if (creds == null || creds.isEmpty()) {
            return best;
        }
        for (MerchantIcopayBrokerCredential c : creds) {
            if (c == null || c.getOrgUnitId() == null || !"Y".equalsIgnoreCase(c.getUseYn())) {
                continue;
            }
            Long ouId = c.getOrgUnitId();
            MerchantIcopayBrokerCredential prev = best.get(ouId);
            if (prev == null) {
                best.put(ouId, c);
                continue;
            }
            int cmp = compareBrokerIssuedAt(c, prev);
            if (cmp > 0) {
                best.put(ouId, c);
            } else if (cmp == 0 && preferBrokerScope(c.getVendorScope(), prev.getVendorScope())) {
                best.put(ouId, c);
            }
        }
        return best;
    }

    private static boolean preferBrokerScope(String a, String b) {
        if ("ALL".equalsIgnoreCase(a)) {
            return !"ALL".equalsIgnoreCase(b);
        }
        return false;
    }

    private static int compareBrokerIssuedAt(MerchantIcopayBrokerCredential a, MerchantIcopayBrokerCredential b) {
        LocalDateTime ta = latestBrokerIssuedAt(a);
        LocalDateTime tb = latestBrokerIssuedAt(b);
        if (ta == null && tb == null) {
            return 0;
        }
        if (ta == null) {
            return -1;
        }
        if (tb == null) {
            return 1;
        }
        return ta.compareTo(tb);
    }

    private static LocalDateTime latestBrokerIssuedAt(MerchantIcopayBrokerCredential c) {
        if (c == null) {
            return null;
        }
        LocalDateTime rotated = c.getRotatedAt();
        LocalDateTime created = c.getCreatedAt();
        if (rotated != null && created != null) {
            return rotated.isAfter(created) ? rotated : created;
        }
        return rotated != null ? rotated : created;
    }

    /** NOT_ISSUED | ISSUED | REISSUED */
    private static String brokerSecretStatusCode(MerchantIcopayBrokerCredential c) {
        if (c == null || !"Y".equalsIgnoreCase(c.getUseYn())) {
            return "NOT_ISSUED";
        }
        LocalDateTime rotated = c.getRotatedAt();
        if (rotated == null) {
            return "ISSUED";
        }
        LocalDateTime created = c.getCreatedAt();
        if (created != null && !rotated.isAfter(created.plusSeconds(10))) {
            return "ISSUED";
        }
        return "REISSUED";
    }

    private static final DateTimeFormatter BROKER_ISSUED_DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static String formatBrokerIssuedDate(MerchantIcopayBrokerCredential c) {
        LocalDateTime at = latestBrokerIssuedAt(c);
        if (at == null) {
            return "";
        }
        return at.format(BROKER_ISSUED_DATE_FMT);
    }

    private static String brokerIssuedByDisplay(MerchantIcopayBrokerCredential c) {
        if (c == null || c.getIssuedBy() == null || c.getIssuedBy().isBlank()) {
            return "";
        }
        return c.getIssuedBy().trim();
    }

    /** 상위 체인에서 가장 가까운 총판(MASTER_DIST) 업체명 */
    private static String resolveNearestMasterDistName(Long orgUnitId, Map<Long, OrgUnit> orgById) {
        if (orgUnitId == null || orgById == null || orgById.isEmpty()) {
            return "-";
        }
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgById.get(cur);
            if (ou == null) {
                break;
            }
            if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                String nm = ou.getName();
                return nm != null && !nm.isBlank() ? nm.trim() : "-";
            }
            cur = ou.getParentId();
        }
        return "-";
    }

    /** 가맹점 목록: 운영(operational Y) PG명 우선, 없으면 활성 바인딩 첫 PG */
    private static String formatMerchantPgAgencyDisplay(List<MerchantPgBinding> bindings,
                                                        Map<String, String> pgNmByCd) {
        if (bindings == null || bindings.isEmpty()) {
            return "-";
        }
        List<String> operational = collectPgAgencyLabels(bindings, pgNmByCd, true);
        if (!operational.isEmpty()) {
            return String.join(", ", operational);
        }
        List<String> active = collectPgAgencyLabels(bindings, pgNmByCd, false);
        return active.isEmpty() ? "-" : String.join(", ", active);
    }

    private static List<String> collectPgAgencyLabels(List<MerchantPgBinding> bindings,
                                                      Map<String, String> pgNmByCd,
                                                      boolean operationalOnly) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> labels = new ArrayList<>();
        for (MerchantPgBinding b : bindings) {
            if (operationalOnly && !"Y".equalsIgnoreCase(optYn(b.getOperationalYn()))) {
                continue;
            }
            if ("N".equalsIgnoreCase(optYn(b.getActivationYn()))) {
                continue;
            }
            String cd = b.getPgCd() != null ? b.getPgCd().trim().toUpperCase(Locale.ROOT) : "";
            if (cd.isEmpty() || seen.contains(cd)) {
                continue;
            }
            seen.add(cd);
            labels.add(pgNmByCd.getOrDefault(cd, b.getPgCd() != null ? b.getPgCd().trim() : cd));
        }
        return labels;
    }

    private static String optYn(String yn) {
        return yn != null ? yn.trim() : "";
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
        kit.put("apiIntegrationChannel", integrationChannelService.buildEffectiveChannelDisplayCode(ou.getId()));

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
            kit.put("merchantRedirectCheckoutChillPay", buildMerchantRedirectCheckoutBlock(publicApiBase, ou.getCode(), "chillpay"));
        }
        if (MerchantPgBrokerVendor.ALL.equals(vs) || MerchantPgBrokerVendor.JPAY.equals(vs)) {
            kit.put("merchantInlineCheckoutJpay", buildMerchantInlineCheckoutBlock(publicApiBase, ou.getCode(), "jpay"));
            kit.put("merchantRedirectCheckoutJpay", buildMerchantRedirectCheckoutBlock(publicApiBase, ou.getCode(), "jpay"));
            kit.put("merchantSubscriptionCheckoutJpay", buildMerchantSubscriptionCheckoutBlock(publicApiBase, ou.getCode()));
        }
        if (MerchantPgBrokerVendor.ALL.equals(vs)) {
            Map<String, Object> unifiedBlock = buildMerchantUnifiedCheckoutBlock(publicApiBase, ou.getCode());
            Map<String, Object> unifiedRedirectBlock = buildMerchantUnifiedRedirectCheckoutBlock(publicApiBase, ou.getCode());
            kit.put("merchantUnifiedCheckout", unifiedBlock);
            kit.put("merchantUnifiedRedirectCheckout", unifiedRedirectBlock);
            kit.put("merchantCheckoutApiParameterSpec",
                    MerchantCheckoutApiParameterSpec.build(publicApiBase, ou.getCode()));
            kit.put("integrationModes", buildIntegrationModes(publicApiBase, ou.getCode(), unifiedBlock, unifiedRedirectBlock));
        }

        kit.put("wordpressPlugins", buildWordPressPluginDeployBlock(publicApiBase));
        kit.put("paymentNotifyGuide", buildMerchantPaymentNotifyGuideBlock());

        kit.put("merchantIntegrationSamples", buildIntegrationSamples(publicApiBase));

        kit.put("credentialScopes", credentialSummaries(ou.getId()));

        kit.put("integrationChecklist", MerchantApiDeployChecklistI18n.build(publicApiBase, ou.getCode()));

        applyIntegrationChannelFilter(kit, ou.getId());

        return kit;
    }

    private void applyIntegrationChannelFilter(Map<String, Object> kit, Long orgUnitId) {
        kit.put("integrationChannels", integrationChannelService.buildStatusBlock(orgUnitId));
        if (!integrationChannelService.isInlineEffective(orgUnitId)) {
            kit.remove("merchantInlineCheckoutJpay");
            kit.remove("merchantInlineCheckoutChillPay");
            kit.remove("merchantUnifiedCheckout");
        }
        if (!integrationChannelService.isRedirectEffective(orgUnitId)) {
            kit.remove("merchantRedirectCheckoutJpay");
            kit.remove("merchantRedirectCheckoutChillPay");
            kit.remove("merchantUnifiedRedirectCheckout");
        }
        if (!integrationChannelService.isWordpressEffective(orgUnitId)) {
            kit.remove("wordpressPlugins");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checklist = (List<Map<String, Object>>) kit.get("integrationChecklist");
        if (checklist != null) {
            kit.put("integrationChecklist", filterChecklistByChannels(checklist, orgUnitId));
        }
    }

    private List<Map<String, Object>> filterChecklistByChannels(List<Map<String, Object>> list, Long orgUnitId) {
        boolean inline = integrationChannelService.isInlineEffective(orgUnitId);
        boolean redirect = integrationChannelService.isRedirectEffective(orgUnitId);
        boolean wp = integrationChannelService.isWordpressEffective(orgUnitId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : list) {
            String kr = row.get("textKr") != null ? row.get("textKr").toString() : "";
            if (!inline && (kr.contains("인라인") || kr.contains("inline") || kr.contains("embed") || kr.contains("Embed"))) {
                if (!kr.contains("구독") && !kr.contains("subscription")) {
                    continue;
                }
            }
            if (!redirect && (kr.contains("리다이렉트") || kr.contains("redirect") || kr.contains("REDIRECT") || kr.contains("returnUrl"))) {
                if (!kr.contains("챗봇") && !kr.contains("URL 결제") && !kr.contains("notifyIngress")) {
                    continue;
                }
            }
            if (!wp && kr.contains("WordPress")) {
                continue;
            }
            out.add(row);
        }
        return out.isEmpty() ? list : out;
    }

    /**
     * 가맹 배포 문서 — 운영 URL 결제 PG({@link ChillPayService#resolveUrlPayOperationalPgCd}) 기준으로
     * JPAY·ChillPay 전용 블록·체크리스트·샘플을 숨깁니다. 통합 checkout(인라인·리다이렉트)은 유지합니다.
     */
    private void applyMerchantDocPgVendorFilter(Map<String, Object> kit, Long orgUnitId) {
        Map<String, Object> scope = resolveMerchantApiDocPgScope(orgUnitId);
        kit.put("merchantApiDocPgScope", scope);
        boolean jpay = Boolean.TRUE.equals(scope.get("jpay"));
        boolean chillPay = Boolean.TRUE.equals(scope.get("chillPay"));

        if (!jpay) {
            kit.remove("merchantInlineCheckoutJpay");
            kit.remove("merchantRedirectCheckoutJpay");
            kit.remove("merchantSubscriptionCheckoutJpay");
        }
        if (!chillPay) {
            kit.remove("merchantInlineCheckoutChillPay");
            kit.remove("merchantRedirectCheckoutChillPay");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> brokerBlocks = (List<Map<String, Object>>) kit.get("pgBrokerBlocks");
        if (brokerBlocks != null) {
            List<Map<String, Object>> filteredBlocks = new ArrayList<>();
            for (Map<String, Object> block : brokerBlocks) {
                String vendorScope = block.get("vendorScope") != null ? block.get("vendorScope").toString() : "";
                if (matchesMerchantDocPgVendor(vendorScope, jpay, chillPay)) {
                    filteredBlocks.add(block);
                }
            }
            kit.put("pgBrokerBlocks", filteredBlocks);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checklist = (List<Map<String, Object>>) kit.get("integrationChecklist");
        if (checklist != null) {
            kit.put("integrationChecklist", filterChecklistByPgVendor(checklist, jpay, chillPay));
        }

        filterIntegrationSamplesByPgVendor(kit, jpay, chillPay);
        filterIntegrationModesByPgVendor(kit, jpay, chillPay);
        filterCredentialScopesByPgVendor(kit, jpay, chillPay);
    }

    private Map<String, Object> resolveMerchantApiDocPgScope(Long orgUnitId) {
        String opPg = orgUnitId != null ? chillPayService.resolveUrlPayOperationalPgCd(orgUnitId) : "";
        if (opPg == null || opPg.isBlank()) {
            opPg = resolveOperationalPgCdFromBindings(orgUnitId);
        }
        boolean jpay = PgVendor.isJpayFamily(opPg);
        boolean chillPay = PgVendor.isChillPayFamily(opPg);
        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("operationalPgCd", opPg != null ? opPg.trim() : "");
        scope.put("jpay", jpay);
        scope.put("chillPay", chillPay);
        if (jpay) {
            scope.put("primaryPgVendor", MerchantPgBrokerVendor.JPAY);
        } else if (chillPay) {
            scope.put("primaryPgVendor", MerchantPgBrokerVendor.CHILLPAY);
        } else {
            scope.put("primaryPgVendor", "");
        }
        return scope;
    }

    private String resolveOperationalPgCdFromBindings(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        for (MerchantPgBinding b : merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId)) {
            if (!"Y".equalsIgnoreCase(optYn(b.getOperationalYn()))) {
                continue;
            }
            if ("N".equalsIgnoreCase(optYn(b.getActivationYn()))) {
                continue;
            }
            String pgCd = b.getPgCd() != null ? b.getPgCd().trim() : "";
            if (pgCd.isEmpty() || !isAgencyUrlPayIntegration(pgCd)) {
                continue;
            }
            String pm = b.getPayMethod();
            if (pm != null && !pm.isBlank() && !"WEB".equalsIgnoreCase(pm.trim())) {
                continue;
            }
            return pgCd;
        }
        return "";
    }

    private boolean isAgencyUrlPayIntegration(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        return pgAgencyRepository.findByPgCd(pgCd.trim())
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .map(a -> "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : ""))
                .orElse(false);
    }

    private static boolean matchesMerchantDocPgVendor(String vendorScope, boolean jpay, boolean chillPay) {
        String vs = MerchantPgBrokerVendor.normalizeScope(vendorScope);
        if (MerchantPgBrokerVendor.ALL.equals(vs)) {
            return true;
        }
        if (MerchantPgBrokerVendor.JPAY.equals(vs) || PgVendor.isJpayFamily(vs)) {
            return jpay;
        }
        if (MerchantPgBrokerVendor.CHILLPAY.equals(vs) || PgVendor.isChillPayFamily(vs)) {
            return chillPay;
        }
        return true;
    }

    private static List<Map<String, Object>> filterChecklistByPgVendor(List<Map<String, Object>> list,
                                                                       boolean jpay, boolean chillPay) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : list) {
            String kr = row.get("textKr") != null ? row.get("textKr").toString() : "";
            if (kr.contains("WordPress")) {
                out.add(row);
                continue;
            }
            if (!chillPay && isChillPaySpecificChecklistItem(kr)) {
                continue;
            }
            if (!jpay && isJpaySpecificChecklistItem(kr)) {
                continue;
            }
            out.add(row);
        }
        return out.isEmpty() ? list : out;
    }

    private static boolean isChillPaySpecificChecklistItem(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("ChillPay 인라인") || text.contains("ChillPay 리다이렉트")
                || text.contains("ChillPay 콜백");
    }

    private static boolean isJpaySpecificChecklistItem(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("JPAY 인라인") || text.contains("JPAY 리다이렉트")
                || text.contains("JPAY 구독") || text.contains("JPAY pay_notifyurl");
    }

    @SuppressWarnings("unchecked")
    private static void filterIntegrationSamplesByPgVendor(Map<String, Object> kit, boolean jpay, boolean chillPay) {
        Object samplesObj = kit.get("merchantIntegrationSamples");
        if (!(samplesObj instanceof Map<?, ?> samplesRaw)) {
            return;
        }
        Map<String, Object> samples = new LinkedHashMap<>((Map<String, Object>) samplesRaw);
        Object phpObj = samples.get("php");
        if (phpObj instanceof Map<?, ?> phpRaw) {
            Map<String, Object> php = new LinkedHashMap<>((Map<String, Object>) phpRaw);
            if (!jpay) {
                php.remove("checkoutJpay");
            }
            if (!chillPay) {
                php.remove("checkoutChillpay");
            }
            samples.put("php", php);
        }
        Object jspObj = samples.get("jsp");
        if (jspObj instanceof Map<?, ?> jspRaw) {
            Map<String, Object> jsp = new LinkedHashMap<>((Map<String, Object>) jspRaw);
            if (!jpay) {
                jsp.remove("checkoutJpay");
            }
            if (!chillPay) {
                jsp.remove("checkoutChillpay");
            }
            samples.put("jsp", jsp);
        }
        kit.put("merchantIntegrationSamples", samples);
    }

    @SuppressWarnings("unchecked")
    private static void filterIntegrationModesByPgVendor(Map<String, Object> kit, boolean jpay, boolean chillPay) {
        Object modesObj = kit.get("integrationModes");
        if (!(modesObj instanceof Map<?, ?> modesRaw)) {
            return;
        }
        Map<String, Object> modes = new LinkedHashMap<>((Map<String, Object>) modesRaw);
        Object phpObj = modes.get("php");
        if (phpObj instanceof Map<?, ?> phpRaw) {
            Map<String, Object> php = new LinkedHashMap<>((Map<String, Object>) phpRaw);
            if (!jpay) {
                php.remove("checkoutLegacyJpay");
            }
            if (!chillPay) {
                php.remove("checkoutLegacyChillpay");
            }
            modes.put("php", php);
        }
        kit.put("integrationModes", modes);
    }

    @SuppressWarnings("unchecked")
    private static void filterCredentialScopesByPgVendor(Map<String, Object> kit, boolean jpay, boolean chillPay) {
        Object credsObj = kit.get("credentialScopes");
        if (!(credsObj instanceof List<?> credsRaw)) {
            return;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Object item : credsRaw) {
            if (!(item instanceof Map<?, ?> rowRaw)) {
                continue;
            }
            Map<String, Object> row = (Map<String, Object>) rowRaw;
            String scope = row.get("vendorScope") != null ? row.get("vendorScope").toString() : "";
            if (matchesMerchantDocPgVendor(scope, jpay, chillPay)) {
                filtered.add(row);
            }
        }
        kit.put("credentialScopes", filtered);
    }

    /**
     * 가맹점 전용 — 본사 API 배포 완료 가맹만 연동 키·문서 조회(평문 시크릿 포함). 발급·수정 없음.
     */
    public Map<String, Object> buildMerchantSelfPortal(HttpServletRequest req, String viewerCompId, String viewerOrgLevel) {
        if (viewerOrgLevel == null || !OrgLevel.MERCHANT.name().equalsIgnoreCase(viewerOrgLevel.trim())) {
            throw new IllegalArgumentException("가맹점 계정만 조회할 수 있습니다.");
        }
        String cid = viewerCompId != null ? viewerCompId.trim() : "";
        if (cid.isEmpty()) {
            throw new IllegalArgumentException("가맹 업체코드를 확인할 수 없습니다.");
        }
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("업체코드를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점(조직단계 MERCHANT)만 조회할 수 있습니다.");
        }

        boolean deployed = isMerchantApiIntegrationEligible(ou.getId());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", ou.getCode());
        out.put("merchantName", ou.getName());
        out.put("deployed", deployed);
        if (!deployed) {
            out.put("messageKey", "API_NOT_DEPLOYED");
            return out;
        }

        Map<String, Object> portal = buildDocsPortal(cid, req);
        out.put("portal", portal);

        List<MerchantIcopayBrokerCredential> creds =
                credentialRepository.findByOrgUnitIdAndUseYnOrderByIdDesc(ou.getId(), "Y");
        List<Map<String, Object>> credentialItems = new ArrayList<>();
        for (MerchantIcopayBrokerCredential c : creds) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("vendorScope", c.getVendorScope());
            item.put("brokerSecret", c.getBrokerSecret());
            item.put("brokerSecretMasked", maskBrokerSecretForDisplay(c.getBrokerSecret(), c.getSecretPrefix()));
            item.put("secretPrefix", c.getSecretPrefix());
            item.put("enforceYn", c.getEnforceYn());
            item.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            credentialItems.add(item);
        }
        out.put("credentials", credentialItems);
        out.put("brokerHeaderName", MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET);
        return out;
    }

    /** 배포설정 — API배포문서 화면용(다운로드·연동 파라미터 표). 시크릿 평문·재발급 기능은 포함하지 않음. */
    public Map<String, Object> buildDocsPortal(String compId, HttpServletRequest req) {
        Map<String, Object> kit = buildKit(compId, MerchantPgBrokerVendor.ALL, req);
        Object orgUnitIdObj = kit.get("merchantOrgUnitId");
        if (orgUnitIdObj instanceof Number orgUnitId) {
            applyMerchantDocPgVendorFilter(kit, orgUnitId.longValue());
        }
        Map<String, Object> portal = new LinkedHashMap<>();
        portal.put("compId", kit.get("compId"));
        portal.put("merchantName", kit.get("merchantName"));
        portal.put("merchantOrgUnitId", kit.get("merchantOrgUnitId"));
        portal.put("publicApiBaseUrl", kit.get("publicApiBaseUrl"));
        portal.put("baseCurrency", kit.getOrDefault("baseCurrency", ""));
        portal.put("apiIntegrationChannel", kit.getOrDefault("apiIntegrationChannel", "-"));
        portal.put("headersRecommended", kit.get("headersRecommended"));
        portal.put("notifyIngressUrlMiddleware", kit.get("notifyIngressUrlMiddleware"));
        portal.put("notifyIngressUrlOpen", kit.get("notifyIngressUrlOpen"));
        portal.put("merchantPgBindings", kit.get("merchantPgBindings"));
        portal.put("merchantNotifyUrls", kit.get("merchantNotifyUrls"));
        portal.put("merchantUnifiedCheckout", kit.get("merchantUnifiedCheckout"));
        portal.put("merchantUnifiedRedirectCheckout", kit.get("merchantUnifiedRedirectCheckout"));
        portal.put("merchantInlineCheckoutJpay", kit.get("merchantInlineCheckoutJpay"));
        portal.put("merchantInlineCheckoutChillPay", kit.get("merchantInlineCheckoutChillPay"));
        portal.put("merchantRedirectCheckoutJpay", kit.get("merchantRedirectCheckoutJpay"));
        portal.put("merchantRedirectCheckoutChillPay", kit.get("merchantRedirectCheckoutChillPay"));
        portal.put("wordpressPlugins", kit.get("wordpressPlugins"));
        portal.put("paymentNotifyGuide", kit.get("paymentNotifyGuide"));
        portal.put("integrationChannels", kit.get("integrationChannels"));
        portal.put("merchantApiDocPgScope", kit.get("merchantApiDocPgScope"));
        portal.put("merchantCheckoutApiParameterSpec", kit.get("merchantCheckoutApiParameterSpec"));
        portal.put("integrationModes", kit.get("integrationModes"));
        portal.put("merchantIntegrationSamples", kit.get("merchantIntegrationSamples"));
        portal.put("integrationChecklist", kit.get("integrationChecklist"));
        portal.put("credentialScopes", kit.get("credentialScopes"));
        return portal;
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
                "productName", "Sample product",
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

    private Map<String, Object> buildMerchantRedirectCheckoutBlock(String publicApiBase, String compId, String vendorKey) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        boolean jpay = "jpay".equalsIgnoreCase(vendorKey);
        String vendorScope = jpay ? MerchantPgBrokerVendor.JPAY : MerchantPgBrokerVendor.CHILLPAY;
        String apiPath = jpay
                ? "/api/middleware/v1/merchant/jpay/redirect-checkout"
                : "/api/middleware/v1/merchant/chillpay/redirect-checkout";
        String payPath = jpay ? "/jpay-pay/" : "/pay/";
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("integrationMode", "REDIRECT");
        block.put("pgVendor", vendorScope);
        MerchantDeployL10n.putDescription(block, new Bundle(
                jpay ? "JPAY 리다이렉트 URL 결제" : "ChillPay 리다이렉트 URL 결제",
                jpay ? "JPAY redirect URL checkout" : "ChillPay redirect URL checkout",
                jpay ? "JPAY リダイレクト URL 決済" : "ChillPay リダイレクト URL 決済",
                jpay ? "JPAY 重定向 URL 支付" : "ChillPay 重定向 URL 支付",
                jpay ? "JPAY redirect URL ชำระเงิน" : "ChillPay redirect URL ชำระเงิน"
        ));
        block.put("prepareUrl", base + apiPath + "/prepare");
        block.put("statusUrl", base + apiPath + "/status?compId=" + compId + "&orderNo={orderNo}");
        block.put("payPagePathTemplate", base + payPath + compId
                + "?entry=merchant_api&session={sessionToken}");
        block.put("prepareBodyExample", Map.of(
                "compId", compId,
                "orderNo", "ORD-001",
                "amount", jpay ? 100 : 10000,
                "currency", jpay ? "USD" : "JPY",
                "productName", "Sample product"
        ));
        MerchantDeployL10n.putTextFields(block, "redirectUsageHint", new Bundle(
                "prepare → data.payUrl 리다이렉트. 브라우저 복귀는 NOTI Result 경유(가맹 URL은 prepare·PG에 넣지 않음). PAID는 status·웹훅.",
                "prepare → redirect to data.payUrl. Browser return via NOTI Result (do not put merchant URL in prepare or PG). Confirm PAID via status or webhook.",
                "prepare → data.payUrl へ。ブラウ저復帰は NOTI Result 経由（prepare·PG に加盟店 URL を入れない）。PAID は status/Webhook。",
                "prepare → 跳转 data.payUrl。浏览器返回经 NOTI Result（勿在 prepare/PG 填商户 URL）。PAID 用 status/webhook。",
                "prepare → redirect ไป data.payUrl กลับเบราว์เซอร์ผ่าน NOTI Result (อย่าใส่ URL ร้านใน prepare/PG) ยืนยัน PAID ด้วย status/webhook"
        ));
        MerchantDeployL10n.putTextFields(block, "wordpressPluginNote", new Bundle(
                "WordPress: flow_mode=redirect — docs/WordPress_JPAY_플러그인_배포가이드.md",
                "WordPress: flow_mode=redirect — see docs/WordPress_JPAY_플러그인_배포가이드.md",
                "WordPress: flow_mode=redirect — docs/WordPress_JPAY_플러그인_배포가이드.md を参照",
                "WordPress：flow_mode=redirect — 见 docs/WordPress_JPAY_플러그인_배포가이드.md",
                "WordPress: flow_mode=redirect — ดู docs/WordPress_JPAY_플러그인_배포가이드.md"
        ));
        return block;
    }

    private Map<String, Object> buildMerchantUnifiedRedirectCheckoutBlock(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("integrationMode", "REDIRECT_UNIFIED");
        MerchantDeployL10n.putDescription(block, new Bundle(
                "PG 무관 통합 리다이렉트 — buyer 필수, returnUrl/cancelUrl body 금지(NOTI Result 경유)",
                "Unified redirect — buyer required; do not send returnUrl/cancelUrl in body (browser via NOTI Result)",
                "PG 非依存統合リダイレクト — buyer 必須、returnUrl/cancelUrl は body 禁止（NOTI Result 経由）",
                "PG 无关统一重定向 — buyer 必填，body 禁止 returnUrl/cancelUrl（经 NOTI Result）",
                "Unified redirect — ต้องมี buyer ห้าม returnUrl/cancelUrl ใน body (NOTI Result)"
        ));
        block.put("prepareUrl", base + "/api/middleware/v1/merchant/checkout/redirect/prepare");
        block.put("statusUrl", base + "/api/middleware/v1/merchant/checkout/redirect/status?compId=" + compId + "&orderNo={orderNo}");
        return block;
    }

    private Map<String, Object> buildWordPressPluginDeployBlock(String publicApiBase) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> wp = new LinkedHashMap<>();
        MerchantDeployL10n.putDescription(wp, new Bundle(
                "WordPress JPAY 플러그인 ZIP 배포 (WooCommerce·일반 WP)",
                "WordPress JPAY plugin ZIP (WooCommerce and general WP)",
                "WordPress JPAY プラグイン ZIP（WooCommerce・一般 WP）",
                "WordPress JPAY 插件 ZIP（WooCommerce 与一般 WP）",
                "WordPress JPAY plugin ZIP (WooCommerce และ WP ทั่วไป)"
        ));
        MerchantDeployL10n.putTextFields(wp, "inlineDefaultNote", new Bundle(
                "기본 flow_mode=inline — 기존 인라인 연동과 동일",
                "Default flow_mode=inline — same as legacy inline integration",
                "既定 flow_mode=inline — 従来インライン連携と同じ",
                "默认 flow_mode=inline — 与原有内联相同",
                "ค่าเริ่มต้น flow_mode=inline — เหมือน inline เดิม"
        ));
        MerchantDeployL10n.putTextFields(wp, "redirectDeployNote", new Bundle(
                "redirect 선택 시 HQ apiBrokerRedirectEnabledYn=Y 및 플러그인 flow_mode=redirect",
                "For redirect: HQ apiBrokerRedirectEnabledYn=Y and plugin flow_mode=redirect",
                "redirect 利用時は HQ apiBrokerRedirectEnabledYn=Y と flow_mode=redirect",
                "重定向需 HQ apiBrokerRedirectEnabledYn=Y 且 flow_mode=redirect",
                "redirect ต้อง HQ apiBrokerRedirectEnabledYn=Y และ flow_mode=redirect"
        ));
        wp.put("docPath", "docs/WordPress_JPAY_플러그인_배포가이드.md");
        wp.put("buildScript", "tools/build-wp-plugin-zips.ps1");
        wp.put("woocommercePluginZip", "woocommerce/icopay-woocommerce-1.1.0.zip");
        wp.put("generalWordPressPluginZip", "wordpress/icopay-jpay-1.0.0.zip");
        wp.put("flowModes", List.of("inline", "redirect"));
        wp.put("defaultFlowMode", "inline");
        wp.put("apiBaseUrlExample", base.isEmpty() ? "https://api.icopay.co.kr" : base);
        wp.put("woocommerceWebhookPath", "/wp-json/icopay/v1/webhook");
        wp.put("generalWordPressWebhookPath", "/wp-json/icopay-jpay/v1/webhook");
        MerchantDeployL10n.putTextFields(wp, "webhookRegisterNote", new Bundle(
                "WordPress 사용 시 본사 merchantNotifyUrls에 등록할 결제 통보(Webhook) URL 예: "
                        + "https://{가맹도메인}/wp-json/icopay/v1/webhook (WooCommerce) · "
                        + "https://{가맹도메인}/wp-json/icopay-jpay/v1/webhook (일반 WP). ICOPAY가 결제 확정 시 POST합니다.",
                "For WordPress, register merchant payment notify (webhook) URLs at HQ merchantNotifyUrls, e.g. "
                        + "https://{your-domain}/wp-json/icopay/v1/webhook (WooCommerce) · "
                        + "https://{your-domain}/wp-json/icopay-jpay/v1/webhook (general WP). ICOPAY POSTs on payment confirmation.",
                "WordPress 利用時は本社 merchantNotifyUrls に登録: "
                        + "https://{ドメイン}/wp-json/icopay/v1/webhook (WooCommerce) · "
                        + "https://{ドメイン}/wp-json/icopay-jpay/v1/webhook (一般 WP)。決済確定時に ICOPAY が POST。",
                "WordPress 请在总部 merchantNotifyUrls 登记: "
                        + "https://{域名}/wp-json/icopay/v1/webhook (WooCommerce) · "
                        + "https://{域名}/wp-json/icopay-jpay/v1/webhook (一般 WP)。ICOPAY 在支付确认时 POST。",
                "WordPress ลงทะเบียน merchantNotifyUrls ที่ HQ เช่น "
                        + "https://{โดเมน}/wp-json/icopay/v1/webhook (WooCommerce) · "
                        + "https://{โดเมน}/wp-json/icopay-jpay/v1/webhook (WP ทั่วไป)"
        ));
        return wp;
    }

    /** 가맹 결제 통보(Webhook) vs PG→ICOPAY 노티 vs returnUrl — 5개 언어. */
    private Map<String, Object> buildMerchantPaymentNotifyGuideBlock() {
        Map<String, Object> g = new LinkedHashMap<>();
        MerchantDeployL10n.putDescription(g, new Bundle(
                "결제 결과 통보(Webhook) — ICOPAY가 가맹 서버(merchantNotifyUrls)로 POST",
                "Payment result webhook — ICOPAY POSTs to the merchant server (merchantNotifyUrls)",
                "決済結果 Webhook — ICOPAY が加盟店サーバー(merchantNotifyUrls)へ POST",
                "支付结果 Webhook — ICOPAY 向商户服务器(merchantNotifyUrls) POST",
                "Webhook ผลการชำระ — ICOPAY POST ไปเซิร์ฟเวอร์ร้าน (merchantNotifyUrls)"
        ));
        MerchantDeployL10n.putTextFields(g, "overviewNote", new Bundle(
                "가맹 연동 문서의 「Webhook」은 ICOPAY → 가맹 서버 결제 확정 통보입니다. "
                        + "본사 업체관리 merchantNotifyUrls에 가맹 HTTPS URL을 등록합니다. "
                        + "WordPress 플러그인은 아래 REST 경로를 수신합니다.",
                "In merchant integration docs, 「webhook」 means ICOPAY → merchant server payment confirmation. "
                        + "Register the merchant HTTPS URL in HQ merchantNotifyUrls. WordPress plugins listen on the REST paths below.",
                "加盟店連携の「Webhook」は ICOPAY → 加盟店サーバーへの決済確定通知です。"
                        + "本社 merchantNotifyUrls に HTTPS URL を登録。WordPress は下記 REST を受信。",
                "商户对接文档中的「Webhook」指 ICOPAY → 商户服务器的支付确认通知。"
                        + "在总部 merchantNotifyUrls 登记 HTTPS URL。WordPress 插件接收下列 REST 路径。",
                "Webhook ในเอกสารร้าน = ICOPAY → เซิร์ฟเวอร์ร้าน ลงทะเบียน URL ที่ HQ merchantNotifyUrls"
        ));
        MerchantDeployL10n.putTextFields(g, "wordpressWooWebhookNote", new Bundle(
                "WooCommerce 플러그인 수신 URL: https://{가맹도메인}/wp-json/icopay/v1/webhook",
                "WooCommerce plugin receive URL: https://{your-domain}/wp-json/icopay/v1/webhook",
                "WooCommerce 受信 URL: https://{ドメイン}/wp-json/icopay/v1/webhook",
                "WooCommerce 接收 URL: https://{域名}/wp-json/icopay/v1/webhook",
                "WooCommerce: https://{โดเมน}/wp-json/icopay/v1/webhook"
        ));
        MerchantDeployL10n.putTextFields(g, "wordpressGeneralWebhookNote", new Bundle(
                "일반 WordPress 플러그인 수신 URL: https://{가맹도메인}/wp-json/icopay-jpay/v1/webhook",
                "General WordPress plugin receive URL: https://{your-domain}/wp-json/icopay-jpay/v1/webhook",
                "一般 WordPress 受信 URL: https://{ドメイン}/wp-json/icopay-jpay/v1/webhook",
                "一般 WordPress 接收 URL: https://{域名}/wp-json/icopay-jpay/v1/webhook",
                "WP ทั่วไป: https://{โดเมน}/wp-json/icopay-jpay/v1/webhook"
        ));
        MerchantDeployL10n.putTextFields(g, "pgIngressNote", new Bundle(
                "notifyIngressUrlMiddleware — JPAY/PG → ICOPAY 본사 수신 URL(본사·PG 설정). "
                        + "가맹 WordPress Webhook과 별개이며 가맹이 등록하지 않습니다.",
                "notifyIngressUrlMiddleware — JPAY/PG → ICOPAY HQ ingress (HQ/PG config). "
                        + "Not the merchant WordPress webhook; merchants do not register this.",
                "notifyIngressUrlMiddleware — JPAY/PG → ICOPAY 本社受信（本社・PG 設定）。"
                        + "加盟店 WordPress Webhook とは別。加盟店は登録しない。",
                "notifyIngressUrlMiddleware — JPAY/PG → ICOPAY 总部接收（总部/PG 配置）。"
                        + "与商户 WordPress Webhook 不同，商户无需登记。",
                "notifyIngressUrlMiddleware — JPAY/PG → ICOPAY HQ (ตั้งค่า HQ/PG) ไม่ใช่ Webhook ร้าน"
        ));
        MerchantDeployL10n.putTextFields(g, "returnUrlNote", new Bundle(
                "브라우저 복귀 URL은 prepare body·PG 전문에 넣지 않습니다. NOTI Result → 가맹 Result(브라우저), "
                        + "서버 Callback은 NOTI → 가맹 webhook. 결제 확정은 Status API·Webhook으로 서버에서 확인하세요.",
                "Do not put browser return URLs in prepare body or PG payloads. Browser: NOTI Result → merchant Result; "
                        + "server: NOTI → merchant webhook. Confirm payment on the server via Status API or webhook.",
                "ブラウザ復帰 URL は prepare body・PG 電文に入れません。NOTI Result → 加盟店、サーバーは webhook。"
                        + "確定は Status API/Webhook でサーバー確認。",
                "浏览器返回 URL 勿放入 prepare body 或 PG 报文。NOTI Result → 商户；服务器用 webhook。"
                        + "请在服务器通过 Status API/Webhook 确认。",
                "อย่าใส่ URL กลับเบราว์เซอร์ใน prepare/PG — NOTI Result → ร้าน, webhook ที่เซิร์ฟเวอร์"
        ));
        g.put("woocommerceWebhookPath", "/wp-json/icopay/v1/webhook");
        g.put("generalWordPressWebhookPath", "/wp-json/icopay-jpay/v1/webhook");
        return g;
    }

    private Map<String, Object> buildMerchantUnifiedCheckoutBlock(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("integrationMode", "INLINE_UNIFIED");
        block.put("descriptionKr", "PG 무관 통합 인라인 — buyer(email·phone·countryIso2) 필수, 운영 WEB PG 자동 분기");
        block.put("prepareUrl", base + "/api/middleware/v1/merchant/checkout/prepare");
        block.put("sessionUrl", base + "/api/middleware/v1/merchant/checkout/session?token={sessionToken}");
        block.put("statusUrl", base + "/api/middleware/v1/merchant/checkout/status?compId=" + compId + "&orderNo={orderNo}");
        block.put("embedScriptUrl", base + "/v1/embed-checkout/" + compId);
        block.put("flowDocHtml", base + "/merchant-api-samples/docs/unified-checkout-api-flow.html");
        block.put("flowDocHtmlKo", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ko.html");
        block.put("flowDocHtmlJa", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ja.html");
        block.put("flowDocHtmlCh", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ch.html");
        block.put("flowDocHtmlTh", base + "/merchant-api-samples/docs/unified-checkout-api-flow.th.html");
        block.put("flowDocText", base + "/merchant-api-samples/docs/unified-checkout-api-flow.txt");
        block.put("flowDocTextKo", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ko.txt");
        block.put("flowDocTextJa", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ja.txt");
        block.put("flowDocTextCh", base + "/merchant-api-samples/docs/unified-checkout-api-flow.ch.txt");
        block.put("flowDocTextTh", base + "/merchant-api-samples/docs/unified-checkout-api-flow.th.txt");
        block.put("prepareBodyExample", Map.of(
                "compId", compId,
                "orderNo", "ORD-001",
                "amount", 10000,
                "currency", "USD",
                "productName", "Sample product",
                "lang", "ENG",
                "buyer", Map.of(
                        "email", "buyer@example.com",
                        "phone", "1012345678",
                        "countryIso2", "KR"
                )
        ));
        block.put("embedScriptExample",
                "<div id=\"icopay-checkout\"></div>\n"
                        + "<script src=\"" + base + "/v1/embed-checkout/" + compId + "\"\n"
                        + "  data-session-token=\"{sessionToken}\"\n"
                        + "  data-target=\"icopay-checkout\"\n"
                        + "  data-lang=\"{langCode}\" async defer charset=\"utf-8\"></script>");
        block.put("postMessageEvent", "ICOPAY_INLINE_CHECKOUT");
        block.put("phpClientMethod", "prepareUnifiedCheckout / buildUnifiedEmbedHtml");
        block.put("jsonSampleFiles", List.of(
                "merchant-api-samples/json/unified-prepare-request.json",
                "merchant-api-samples/json/unified-prepare-response.example.json",
                "merchant-api-samples/json/README.txt"
        ));
        block.put("phpSampleFiles", List.of(
                "merchant-api-samples/php/IcopayMerchantApi.php",
                "merchant-api-samples/php/checkout_unified.php",
                "merchant-api-samples/php/icopay_config.example.php"
        ));
        return block;
    }

    /**
     * 가맹 배포용 연동 방식 — JSON(REST 직접 호출) · PHP(샘플 클라이언트) 두 패키지.
     */
    private Map<String, Object> buildIntegrationModes(String publicApiBase, String compId,
                                                      Map<String, Object> unifiedBlock,
                                                      Map<String, Object> unifiedRedirectBlock) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> modes = new LinkedHashMap<>();

        Map<String, Object> jsonMode = new LinkedHashMap<>();
        jsonMode.put("mode", "JSON");
        jsonMode.put("descriptionKr",
                "REST JSON 직접 호출 — Java·Node·Python·Go 등 모든 언어. 가맹 서버에서 prepare·status HTTP 호출.");
        jsonMode.put("contentType", "application/json");
        jsonMode.put("acceptHeader", "application/json");
        jsonMode.put("authHeader", MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET);
        jsonMode.put("authHeaderNoteKr", "브로커 시크릿 강제 시 필수. 레거시 /api/pay/... 는 예외.");
        jsonMode.put("recommendedFlowKr", List.of(
                "1) 가맹 서버: POST /api/middleware/v1/merchant/checkout/prepare (buyer 필수)",
                "2) 응답 data.sessionToken → 브라우저에 /v1/embed-checkout/{compId} 스크립트만 전달",
                "3) GET .../checkout/status 또는 merchantNotifyUrls 웹훅으로 PAID 확인"
        ));
        jsonMode.put("unifiedCheckout", unifiedBlock);
        jsonMode.put("unifiedRedirectCheckout", unifiedRedirectBlock);
        jsonMode.put("sampleFilesBaseUrl", base + "/merchant-api-samples/json/");
        jsonMode.put("prepareRequestExampleUrl", base + "/merchant-api-samples/json/unified-prepare-request.json");
        jsonMode.put("prepareResponseExampleUrl", base + "/merchant-api-samples/json/unified-prepare-response.example.json");
        jsonMode.put("curlPrepareExample", buildUnifiedPrepareCurlExample(base, compId));
        jsonMode.put("curlStatusExample",
                "curl -sS -G '" + base + "/api/middleware/v1/merchant/checkout/status' \\\n"
                        + "  --data-urlencode 'compId=" + compId + "' \\\n"
                        + "  --data-urlencode 'orderNo=ORD-001' \\\n"
                        + "  -H 'Accept: application/json' \\\n"
                        + "  -H '" + MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET + ": {brokerSecret}'");
        jsonMode.put("buyerSchema", Map.of(
                "email", "string, required",
                "phone", "string, required (국가번호 + 제외 로컬 번호)",
                "countryIso2", "string, required (ISO 3166-1 alpha-2, e.g. KR)"
        ));
        modes.put("json", jsonMode);

        Map<String, Object> phpMode = new LinkedHashMap<>();
        phpMode.put("mode", "PHP");
        phpMode.put("descriptionKr",
                "PHP 가맹 서버 — IcopayMerchantApi.php 클라이언트 + checkout_unified.php 샘플 페이지.");
        phpMode.put("phpVersionMin", "7.4");
        phpMode.put("downloadBaseUrl", base + "/merchant-api-samples/");
        phpMode.put("configExamplePath", "merchant-api-samples/php/icopay_config.example.php");
        phpMode.put("configDeployNoteKr",
                "icopay_config.example.php → icopay_config.php 복사 후 document root 밖에 두세요.");
        phpMode.put("configTemplate", Map.of(
                "api_base_url", base,
                "comp_id", compId,
                "broker_secret", "(브로커 시크릿 재발급 값)",
                "default_integration", "unified"
        ));
        phpMode.put("clientFile", "merchant-api-samples/php/IcopayMerchantApi.php");
        phpMode.put("checkoutUnified", "merchant-api-samples/php/checkout_unified.php");
        phpMode.put("checkoutLegacyChillpay", "merchant-api-samples/php/checkout_chillpay.php");
        phpMode.put("checkoutLegacyJpay", "merchant-api-samples/php/checkout_jpay.php");
        phpMode.put("notifyWebhook", "merchant-api-samples/php/notify_webhook.php");
        phpMode.put("postMessageJs", "merchant-api-samples/common/icopay-checkout.js");
        phpMode.put("recommendedMethods", List.of(
                "prepareUnifiedCheckout($orderNo, $amount, $buyer, ...)",
                "buildUnifiedEmbedHtml($sessionToken)",
                "getUnifiedPaymentStatus($orderNo)"
        ));
        phpMode.put("quickStartKr", List.of(
                "1) php/IcopayMerchantApi.php · icopay_config.php 배포",
                "2) checkout_unified.php 참고 — POST 시 buyer(email·phone·countryIso2) 전달",
                "3) prepareUnifiedCheckout → buildUnifiedEmbedHtml 출력",
                "4) common/icopay-checkout.js 로 postMessage 수신"
        ));
        phpMode.put("unifiedCheckout", unifiedBlock);
        phpMode.put("configPhpExample", buildPhpConfigExample(base, compId));
        modes.put("php", phpMode);

        return modes;
    }

    private static String buildUnifiedPrepareCurlExample(String base, String compId) {
        String url = base + "/api/middleware/v1/merchant/checkout/prepare";
        return "curl -sS -X POST '" + url + "' \\\n"
                + "  -H 'Content-Type: application/json' \\\n"
                + "  -H 'Accept: application/json' \\\n"
                + "  -H '" + MerchantBrokerAccessVerifier.HEADER_MERCHANT_BROKER_SECRET + ": {brokerSecret}' \\\n"
                + "  -d '{\n"
                + "    \"compId\": \"" + compId + "\",\n"
                + "    \"orderNo\": \"ORD-001\",\n"
                + "    \"amount\": 10000,\n"
                + "    \"currency\": \"USD\",\n"
                + "    \"productName\": \"Sample product\",\n"
                + "    \"lang\": \"ENG\",\n"
                + "    \"buyer\": {\n"
                + "      \"email\": \"buyer@example.com\",\n"
                + "      \"phone\": \"1012345678\",\n"
                + "      \"countryIso2\": \"KR\"\n"
                + "    }\n"
                + "  }'";
    }

    private static String buildPhpConfigExample(String base, String compId) {
        return "<?php\nreturn [\n"
                + "    'api_base_url'  => '" + base + "',\n"
                + "    'comp_id'       => '" + compId + "',\n"
                + "    'broker_secret' => 'YOUR_BROKER_SECRET',\n"
                + "    /** unified(권장) | chillpay | jpay */\n"
                + "    'default_integration' => 'unified',\n"
                + "];\n";
    }

    private Map<String, Object> buildMerchantSubscriptionCheckoutBlock(String publicApiBase, String compId) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("integrationMode", "INLINE");
        block.put("checkoutKind", "SUBSCRIPTION");
        block.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        block.put("descriptionKr", "JPAY API 구독 — jpay-subscribe.html(카드·3DS·정기) iframe 삽입");
        block.put("prepareUrl", base + "/api/middleware/v1/merchant/jpay/subscription/prepare");
        block.put("sessionUrl", base + "/api/middleware/v1/merchant/jpay/subscription/session?token={sessionToken}");
        block.put("statusUrl", base + "/api/middleware/v1/merchant/jpay/subscription/status?compId=" + compId + "&orderNo={orderNo}");
        block.put("cancelUrl", base + "/api/middleware/v1/merchant/jpay/subscription/cancel");
        block.put("embedScriptUrl", base + "/v1/embed-jpay-subscribe/" + compId);
        block.put("subscribePagePathTemplate", base + "/jpay-subscribe/" + compId + "?entry=merchant_api&embed=1&session={sessionToken}&lang={langCode}");
        block.put("prepareBodyExample", Map.of(
                "compId", compId,
                "orderNo", "SUB-001",
                "amount", 9.99,
                "currency", "USD",
                "productName", "Monthly Plan",
                "subscriptionPlan", Map.of(
                        "name", "Monthly Plan",
                        "plan_type", "monthly",
                        "description", "Monthly subscription",
                        "attempts", "3",
                        "interval_time", 3600,
                        "total_count", 12
                ),
                "lang", "ENG"
        ));
        block.put("embedScriptExample",
                "<div id=\"icopay-jpay-subscribe\"></div>\n"
                        + "<script src=\"" + base + "/v1/embed-jpay-subscribe/" + compId + "\"\n"
                        + "  data-session-token=\"{sessionToken}\"\n"
                        + "  data-target=\"icopay-jpay-subscribe\"\n"
                        + "  data-lang=\"{langCode}\" async defer charset=\"utf-8\"></script>");
        block.put("postMessageEvent", "ICOPAY_INLINE_CHECKOUT");
        return block;
    }

    private Map<String, Object> buildIntegrationSamples(String publicApiBase) {
        String base = publicApiBase != null ? publicApiBase.trim() : "";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("downloadBaseUrl", base + "/merchant-api-samples/");
        m.put("indexUrl", base + "/merchant-api-samples/index.html");
        m.put("readmeUrl", base + "/merchant-api-samples/README.txt");
        Map<String, String> jsonSamples = new LinkedHashMap<>();
        jsonSamples.put("readme", "merchant-api-samples/json/README.txt");
        jsonSamples.put("prepareRequest", "merchant-api-samples/json/unified-prepare-request.json");
        jsonSamples.put("prepareResponseExample", "merchant-api-samples/json/unified-prepare-response.example.json");
        jsonSamples.put("flowDocHtml", "merchant-api-samples/docs/unified-checkout-api-flow.html");
        jsonSamples.put("flowDocHtmlKo", "merchant-api-samples/docs/unified-checkout-api-flow.ko.html");
        jsonSamples.put("flowDocHtmlJa", "merchant-api-samples/docs/unified-checkout-api-flow.ja.html");
        jsonSamples.put("flowDocHtmlCh", "merchant-api-samples/docs/unified-checkout-api-flow.ch.html");
        jsonSamples.put("flowDocHtmlTh", "merchant-api-samples/docs/unified-checkout-api-flow.th.html");
        jsonSamples.put("flowDocText", "merchant-api-samples/docs/unified-checkout-api-flow.txt");
        jsonSamples.put("flowDocTextKo", "merchant-api-samples/docs/unified-checkout-api-flow.ko.txt");
        jsonSamples.put("flowDocTextJa", "merchant-api-samples/docs/unified-checkout-api-flow.ja.txt");
        jsonSamples.put("flowDocTextCh", "merchant-api-samples/docs/unified-checkout-api-flow.ch.txt");
        jsonSamples.put("flowDocTextTh", "merchant-api-samples/docs/unified-checkout-api-flow.th.txt");
        jsonSamples.put("parameterSpecHtml", "merchant-api-samples/docs/unified-checkout-api-parameters.html");
        jsonSamples.put("parameterSpecHtmlKo", "merchant-api-samples/docs/unified-checkout-api-parameters.ko.html");
        jsonSamples.put("parameterSpecHtmlJa", "merchant-api-samples/docs/unified-checkout-api-parameters.ja.html");
        jsonSamples.put("parameterSpecHtmlCh", "merchant-api-samples/docs/unified-checkout-api-parameters.ch.html");
        jsonSamples.put("parameterSpecHtmlTh", "merchant-api-samples/docs/unified-checkout-api-parameters.th.html");
        jsonSamples.put("parameterSpecText", "merchant-api-samples/docs/unified-checkout-api-parameters.txt");
        jsonSamples.put("parameterSpecTextKo", "merchant-api-samples/docs/unified-checkout-api-parameters.ko.txt");
        jsonSamples.put("parameterSpecTextJa", "merchant-api-samples/docs/unified-checkout-api-parameters.ja.txt");
        jsonSamples.put("parameterSpecTextCh", "merchant-api-samples/docs/unified-checkout-api-parameters.ch.txt");
        jsonSamples.put("parameterSpecTextTh", "merchant-api-samples/docs/unified-checkout-api-parameters.th.txt");
        m.put("json", jsonSamples);
        m.put("php", Map.of(
                "configExample", "merchant-api-samples/php/icopay_config.example.php",
                "client", "merchant-api-samples/php/IcopayMerchantApi.php",
                "checkoutUnified", "merchant-api-samples/php/checkout_unified.php",
                "checkoutChillpay", "merchant-api-samples/php/checkout_chillpay.php",
                "checkoutJpay", "merchant-api-samples/php/checkout_jpay.php",
                "notifyWebhook", "merchant-api-samples/php/notify_webhook.php"
        ));
        m.put("commonJsUrl", base + "/merchant-api-samples/common/icopay-checkout.js");
        m.put("jsp", Map.of(
                "configExample", "merchant-api-samples/jsp/icopay-config.example.properties",
                "clientSample", "merchant-api-samples/jsp/IcopayMerchantApi.sample.java",
                "checkoutChillpay", "merchant-api-samples/jsp/checkout-chillpay.jsp",
                "checkoutJpay", "merchant-api-samples/jsp/checkout-jpay.jsp",
                "notifyWebhook", "merchant-api-samples/jsp/notify-webhook.jsp"
        ));
        m.put("workflowKr",
                "1) 가맹 DB 주문(PENDING) 2) JSON: REST prepare / PHP: prepareUnifiedCheckout 3) sessionToken → embed "
                        + "4) postMessage 또는 웹훅으로 PAID 확인");
        m.put("integrationModesNoteKr",
                "키트 integrationModes.json — REST 직접 호출(curl·스키마). integrationModes.php — PHP 클라이언트·설정 템플릿.");
        m.put("securityNoteKr", "브로커 시크릿(X-Icopay-Merchant-Broker-Secret)은 가맹 서버(PHP/JSP)에만 두고 브라우저·앱에 노출하지 마세요.");
        return m;
    }

    /**
     * 가맹점API 메뉴·포털 노출 — 아래 <strong>둘 다</strong> 만족할 때만 true.
     * <ol>
     *   <li>가맹 프로필 {@code web_payment_use_yn=Y} (웹결제 사용)</li>
     *   <li>활성 브로커 시크릿 존재(배포설정 기준 발행·재발행, 미발행 제외)</li>
     * </ol>
     */
    public boolean isMerchantApiIntegrationEligible(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        if (!isWebPaymentEnabledForMerchant(orgUnitId)) {
            return false;
        }
        var creds = credentialRepository.findByOrgUnitIdAndUseYnOrderByIdDesc(orgUnitId, "Y");
        return creds != null && !creds.isEmpty();
    }

    private boolean isWebPaymentEnabledForMerchant(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> "Y".equalsIgnoreCase(trimNull(mp.getWebPaymentUseYn())))
                .orElse(false);
    }

    private static String trimNull(String s) {
        return s != null ? s.trim() : "";
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
    public Map<String, Object> rotateBrokerSecret(String compId, String vendorScope, String issuedBy) {
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
        String secret = MerchantBrokerSecretGenerator.newSecret(40);
        LocalDateTime now = LocalDateTime.now();
        /* uq_merchant_icopay_broker_vendor = UNIQUE(org_unit_id, vendor_scope) — use_yn 무관.
         * 재발급 시 행을 추가(insert)하면 기존 행(폐기 N 포함)과 충돌하므로, 동일 키가 있으면 UPDATE로 회전한다. */
        MerchantIcopayBrokerCredential cred = credentialRepository
                .findByOrgUnitIdAndVendorScope(ou.getId(), scope)
                .orElseGet(MerchantIcopayBrokerCredential::new);
        boolean isNew = cred.getId() == null;
        cred.setOrgUnitId(ou.getId());
        cred.setVendorScope(scope);
        cred.setBrokerSecret(secret);
        cred.setSecretPrefix(MerchantBrokerSecretGenerator.prefixOf(secret));
        cred.setUseYn("Y");
        if (isNew) {
            cred.setRotatedAt(null);
            cred.setEnforceYn("Y");
        } else {
            cred.setRotatedAt(now);
            if (cred.getEnforceYn() == null || cred.getEnforceYn().isBlank()) {
                cred.setEnforceYn("Y");
            }
        }
        if (issuedBy != null && !issuedBy.isBlank()) {
            cred.setIssuedBy(issuedBy.trim());
        }
        credentialRepository.save(cred);
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

    /** 가맹 화면 기본 표시용 — prefix + 마스킹 */
    static String maskBrokerSecretForDisplay(String secret, String prefix) {
        if (secret == null || secret.isBlank()) {
            return "••••••••••••";
        }
        String p = prefix != null ? prefix.trim() : "";
        if (!p.isEmpty()) {
            return p + "••••••••••••";
        }
        if (secret.length() <= 6) {
            return "••••••";
        }
        return secret.substring(0, 3) + "••••••••" + secret.substring(secret.length() - 2);
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
