package com.pg.service;

import com.pg.chatbot.ChatbotCatalogPolicy;
import com.pg.chatbot.ChatbotPromotionShelfMode;
import com.pg.chatbot.ChatbotListingType;
import com.pg.chatbot.ChatbotOperationMode;
import com.pg.chatbot.ChatbotReservationCollectMode;
import com.pg.entity.MerchantChatbotProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantChatbotProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.urlpay.UrlPayCheckoutModeUtil;
import com.pg.util.ChatbotProductPricingUtil;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MerchantChatbotProductService {

    private final MerchantChatbotProductRepository productRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final ChillPayService chillPayService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;

    public MerchantChatbotProductService(MerchantChatbotProductRepository productRepository,
                                        OrgUnitRepository orgUnitRepository,
                                        MerchantProfileRepository merchantProfileRepository,
                                        OrgBrandingRepository orgBrandingRepository,
                                        OrgServiceUseService orgServiceUseService,
                                        HqApiConfigRepository hqApiConfigRepository,
                                        HqNotifyEnvService hqNotifyEnvService,
                                        ChillPayService chillPayService,
                                        UrlPayDisplayFxService urlPayDisplayFxService,
                                        PaymentCurrencyScaleService paymentCurrencyScaleService) {
        this.productRepository = productRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.chillPayService = chillPayService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
    }

    public long countProductsForMerchant(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return 0;
        }
        return productRepository.countByOrgUnitId(merchantOrgUnitId);
    }

    /** 가맹이 판매 활성( use_yn=Y )으로 둔 상품 건수 */
    @Transactional(readOnly = true)
    public long countSaleActiveProductsForMerchant(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return 0;
        }
        return productRepository.countByOrgUnitIdAndUseYn(merchantOrgUnitId, "Y");
    }

    /**
     * 플랜 기준 총 등록(보관) 가능 건수: 판매 활성 상한 + {@link ChatbotProductPricingUtil#CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS}.
     * 플랜 미적용(0)이면 0.
     */
    @Transactional(readOnly = true)
    public int getEffectiveRegistrationCap(Long orgUnitId) {
        int sale = getEffectiveChatbotProductSlotCap(orgUnitId);
        return registrationCapFromSaleCap(sale);
    }

    private static int registrationCapFromSaleCap(int saleCap) {
        return saleCap > 0 ? saleCap + ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS : 0;
    }

    /**
     * 공개 챗봇 UI: 가맹점명 + 상단 로고(가맹 직접 URL 우선, 없으면 상위 본사·총판·총본사 브랜딩 순).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolveChatbotPublicUi(Long merchantOrgUnitId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (merchantOrgUnitId == null) {
            return meta;
        }
        OrgUnit merchant = orgUnitRepository.findById(merchantOrgUnitId).orElse(null);
        if (merchant == null) {
            return meta;
        }
        meta.put("merchantName", merchant.getName() != null ? merchant.getName() : "");
        meta.put("compId", merchant.getCode() != null ? merchant.getCode() : "");
        String logo = "";
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        if (mpOpt.isPresent()) {
            String own = mpOpt.get().getChatbotHeaderLogoUrl();
            if (own != null && !own.isBlank()) {
                logo = own.trim();
            }
        }
        if (logo.isEmpty()) {
            logo = resolveInheritedBrandingLogoUrl(merchantOrgUnitId).orElse("");
        }
        meta.put("headerLogoUrl", logo);
        boolean chatbotAdminConfigured = mpOpt
                .map(mp -> mp.getChatbotAdminUserId() != null && mp.getChatbotAdminUserId() > 0)
                .orElse(false);
        meta.put("chatbotAdminConfigured", chatbotAdminConfigured);
        ChatbotOperationMode opMode = mpOpt
                .map(mp -> ChatbotOperationMode.resolveStored(mp.getChatbotOperationMode()))
                .orElse(ChatbotOperationMode.SALE_PREPAID);
        meta.put("chatbotOperationMode", opMode.getCode());
        meta.put("chatbotOperationModeLabelKo", opMode.getLabelKo());
        meta.put("effectiveMaxProductImages", getEffectiveMaxProductImages(merchantOrgUnitId));
        meta.put("allowedListingTypes", new ArrayList<>(effectiveListingTypesOrdered(merchantOrgUnitId)));
        ChatbotPromotionShelfMode shelfMode = mpOpt
                .map(mp -> ChatbotPromotionShelfMode.resolveStored(mp.getChatbotPromotionShelfMode()))
                .orElse(ChatbotPromotionShelfMode.PROMOTION);
        meta.put("promotionShelfMode", shelfMode.name());
        int rotSec = mpOpt.map(MerchantProfile::getChatbotPromotionRotateSeconds).orElse(30);
        meta.put("promotionRotateSeconds", ChatbotPromotionShelfMode.normalizeRotateSeconds(rotSec));
        return meta;
    }

    /**
     * 브라우저가 열 결제 정적 페이지({@code /pay.html})용 공개 사이트 베이스 URL.
     * {@code tb_hq_api_config.public_api_base_url} → 노티 {@code public_base_url} → 요청 Host 순.
     */
    public String resolvePublicCustomerSiteBase(HttpServletRequest req) {
        String configured = hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> c.getPublicApiBaseUrl() != null ? c.getPublicApiBaseUrl().trim() : "")
                .orElse("");
        configured = trimSlash(configured);
        if (!configured.isBlank()) {
            return configured;
        }
        String notifyBase = trimSlash(hqNotifyEnvService.getOrCreate().getPublicBaseUrl());
        if (!notifyBase.isBlank()) {
            return notifyBase;
        }
        if (req != null) {
            return trimSlash(inferBaseFromRequest(req));
        }
        return "";
    }

    /**
     * 카탈로그 각 행에 챗봇·URL 진입용 프리필 결제 링크({@code /pay.html?m=…&item=&amount=&currency=&entry=chatbot})를 붙입니다.
     */
    public void enrichPublicCatalogItemsWithPayUrls(List<Map<String, Object>> items, String compCode,
                                                     HttpServletRequest req, Long orgUnitId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String cid = compCode != null ? compCode.trim() : "";
        if (cid.isEmpty()) {
            return;
        }
        boolean repayMode = isMerchantUrlPayCheckoutRepay(orgUnitId);
        String base = resolvePublicCustomerSiteBase(req);
        for (Map<String, Object> row : items) {
            if (row == null) {
                continue;
            }
            String title = row.get("title") != null ? String.valueOf(row.get("title")) : "";
            String amount = row.get("amount") != null ? String.valueOf(row.get("amount")) : "";
            String cur = row.get("currencyCode") != null ? String.valueOf(row.get("currencyCode")).trim().toUpperCase(Locale.ROOT) : "KRW";
            String lt = row.get("listingType") != null ? String.valueOf(row.get("listingType")).trim() : "SALE";
            boolean reservation = isReservationListingCode(lt);
            boolean placeStay = ChatbotListingType.RESERVATION_PLACE.getCode().equalsIgnoreCase(lt);
            row.put("urlPayPrefillUrl", buildPayHtmlPrefillUrl(base, cid, title, amount, cur, repayMode));
            if (placeStay) {
                row.put("payLinkVerbKo", "숙박 예약하기");
                row.put("payLinkVerbJa", "宿泊予約");
            } else {
                row.put("payLinkVerbKo", reservation ? "예약하기" : "구매하기");
                row.put("payLinkVerbJa", reservation ? "予約する" : "購入する");
            }
        }
    }

    /**
     * LLM이 금액·통화 안내를 할 때 사용할 URL 결제 체크아웃 요약(표시 통화 vs 청구 통화 등).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> urlPayFactsForChatbotLlm(Long merchantOrgUnitId) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (merchantOrgUnitId == null) {
            return data;
        }
        boolean repayMode = isMerchantUrlPayCheckoutRepay(merchantOrgUnitId);
        data.put("urlPayCheckoutMode", resolveUrlPayCheckoutModeForMerchant(merchantOrgUnitId));
        Map<String, Object> pres;
        try {
            pres = repayMode
                    ? new LinkedHashMap<>(chillPayService.getUrlPayRepayPresentationForCheckout(merchantOrgUnitId))
                    : new LinkedHashMap<>(chillPayService.getUrlPayPresentationForCheckout(merchantOrgUnitId));
        } catch (IllegalStateException ex) {
            data.put("urlPayConfigured", false);
            data.put("urlPayConfigurationHintKo", repayMode
                    ? "이 가맹점은 URL 재결제용 ChillPay 연동(운영·URL재결제)이 없으면 결제 페이지가 열리지 않을 수 있습니다."
                    : "이 가맹점은 URL 결제용 ChillPay 연동(운영·URL결제)이 없으면 결제 페이지가 열리지 않을 수 있습니다.");
            return data;
        }
        data.put("urlPayConfigured", true);
        data.putAll(pres);
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", ""));
        resolveUrlPayCheckoutCurrencyCode(merchantOrgUnitId).ifPresent(cur ->
                data.put("checkoutCurrencyCode", cur.trim().toUpperCase(Locale.ROOT)));
        String pricingMode = String.valueOf(data.getOrDefault("urlPayPricingMode", "CHECKOUT_CURRENCY"));
        boolean fxHq = urlPayDisplayFxService.isHqFeatureEnabled();
        data.put("urlPayDisplayFxHqEnabled", fxHq);
        if (UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equals(pricingMode) && fxHq) {
            data.put("urlPayDisplayFxActive", true);
            data.put("urlPayDisplayFxRefreshSeconds", urlPayDisplayFxService.refreshSeconds());
            String setCur = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
            data.put("urlPaySettlementCurrencyCode", setCur);
            data.put("urlPayDisplayFxDefaultDisplayCurrency", urlPayDisplayFxService.defaultDisplayCurrencyForPg(opPg));
            data.put("urlPayDisplayFxDisplayCurrencyMulti", urlPayDisplayFxService.isDisplayCurrencyMultiForPg(opPg));
            data.put("urlPayDisplayFxDisplayCurrencies", urlPayDisplayFxService.allowedDisplayCurrenciesForCheckout(opPg));
            data.put("urlPayFxUiBlind", urlPayDisplayFxService.isUrlPayFxUiBlind(opPg));
        } else {
            data.put("urlPayDisplayFxActive", false);
            data.put("urlPayFxUiBlind", false);
        }
        Object checkoutCurObj = data.get("checkoutCurrencyCode");
        String checkoutCur = checkoutCurObj instanceof String ? (String) checkoutCurObj : null;
        String scaleCur = checkoutCur;
        if (Boolean.TRUE.equals(data.get("urlPayDisplayFxActive"))) {
            Object scObj = data.get("urlPaySettlementCurrencyCode");
            scaleCur = scObj instanceof String && !((String) scObj).isBlank() ? (String) scObj : "THB";
        }
        String scaleMode = paymentCurrencyScaleService.resolveModeForUi(opPg,
                scaleCur != null && !scaleCur.isBlank() ? scaleCur : "");
        data.put("urlPayAmountScaleMode", scaleMode);
        data.put("urlPayCustomerCurrencyHintKo", buildUrlPayCustomerCurrencyHintKo(data));
        data.remove("redirectPaymentPageUrl");
        data.remove("paymentAppsrvV2Url");
        data.remove("ccdScriptUrl");
        return data;
    }

    private static String buildUrlPayCustomerCurrencyHintKo(Map<String, Object> data) {
        String checkout = data.get("checkoutCurrencyCode") instanceof String
                ? ((String) data.get("checkoutCurrencyCode")).trim().toUpperCase(Locale.ROOT) : "";
        boolean fx = Boolean.TRUE.equals(data.get("urlPayDisplayFxActive"));
        String settle = data.get("urlPaySettlementCurrencyCode") instanceof String
                ? ((String) data.get("urlPaySettlementCurrencyCode")).trim().toUpperCase(Locale.ROOT) : "THB";
        StringBuilder sb = new StringBuilder();
        if (!checkout.isBlank()) {
            sb.append("카탈로그 상품의 currencyCode는 고객에게 보여 주는 금액 단위 안내이며, 실제 카드 청구·결제 통화는 ")
                    .append("checkoutCurrencyCode(").append(checkout).append(") 설정을 따릅니다. ");
        }
        if (fx) {
            sb.append("본 가맹점 URL 결제는 표시 통화(예: JPY 등)로 금액을 입력·표시할 수 있으나, ")
                    .append("실제 청구·정산은 ").append(settle).append(" 기준으로 처리되는 모드입니다. ")
                    .append("최종 청구액은 결제 화면 단계에서 확인할 수 있습니다.");
        } else if (sb.length() == 0) {
            sb.append("상품 금액·통화는 카탈로그와 결제 화면을 동일하게 맞추어 안내하세요.");
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    public String resolveUrlPayCheckoutModeForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return UrlPayCheckoutModeUtil.STANDARD;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> UrlPayCheckoutModeUtil.normalize(mp.getChatbotUrlPayCheckoutMode()))
                .orElse(UrlPayCheckoutModeUtil.STANDARD);
    }

    @Transactional(readOnly = true)
    public boolean isMerchantUrlPayCheckoutRepay(Long orgUnitId) {
        return UrlPayCheckoutModeUtil.isRepay(resolveUrlPayCheckoutModeForMerchant(orgUnitId));
    }

    private static String buildPayHtmlPrefillUrl(String publicBaseTrimmed, String compCode,
                                                 String title, String amountPlain, String currencyIso,
                                                 boolean repayMode) {
        StringBuilder q = new StringBuilder();
        q.append("m=").append(urlEncode(compCode));
        q.append("&entry=chatbot");
        if (repayMode) {
            q.append("&variant=repay");
        }
        String t = title != null ? title.trim() : "";
        if (!t.isEmpty()) {
            q.append("&item=").append(urlEncode(t.length() > 500 ? t.substring(0, 500) : t));
        }
        String a = amountPlain != null ? amountPlain.trim() : "";
        if (!a.isEmpty()) {
            q.append("&amount=").append(urlEncode(a.length() > 40 ? a.substring(0, 40) : a));
        }
        String c = currencyIso != null ? currencyIso.trim().toUpperCase(Locale.ROOT) : "";
        if (!c.isEmpty()) {
            q.append("&currency=").append(urlEncode(c));
        }
        String path = "/pay.html?" + q;
        String base = trimSlash(publicBaseTrimmed);
        if (base.isBlank()) {
            return path;
        }
        return base + path;
    }

    private static String urlEncode(String raw) {
        return URLEncoder.encode(raw != null ? raw : "", StandardCharsets.UTF_8);
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
            if (("http".equalsIgnoreCase(scheme) && port != 80)
                    || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }

    /** {@link com.pg.controller.api.ApiPayController} 와 동일 규칙 — URL 결제 체크아웃 통화. */
    private Optional<String> resolveUrlPayCheckoutCurrencyCode(Long merchantOrgUnitId) {
        Optional<String> own = firstProfileBaseCurrencyToken(merchantOrgUnitId);
        if (own.isPresent()) {
            return own;
        }
        Long cur = merchantOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            Optional<OrgUnit> ou = orgUnitRepository.findById(cur);
            if (ou.isEmpty()) {
                break;
            }
            OrgUnit u = ou.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST) {
                Optional<String> distCur = firstProfileBaseCurrencyToken(u.getId());
                if (distCur.isPresent()) {
                    return distCur;
                }
            }
            cur = u.getParentId();
        }
        cur = merchantOrgUnitId;
        seen.clear();
        while (cur != null && seen.add(cur)) {
            Optional<OrgUnit> ou = orgUnitRepository.findById(cur);
            if (ou.isEmpty()) {
                break;
            }
            OrgUnit u = ou.get();
            if (u.getOrgLevel() == OrgLevel.REGIONAL) {
                return firstProfileBaseCurrencyToken(u.getId());
            }
            cur = u.getParentId();
        }
        return Optional.empty();
    }

    private Optional<String> firstProfileBaseCurrencyToken(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(MerchantProfile::getBaseCurrency)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.split(",")[0].trim())
                .filter(s -> !s.isEmpty());
    }

    private Optional<String> resolveInheritedBrandingLogoUrl(Long merchantOrgUnitId) {
        Long cur = merchantOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) {
                break;
            }
            OrgUnit u = opt.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST || u.getOrgLevel() == OrgLevel.REGIONAL
                    || u.getOrgLevel() == OrgLevel.HEADQUARTERS) {
                Optional<String> fromBrand = orgBrandingRepository.findByOrgUnitId(u.getId()).flatMap(b -> {
                    if (b.getFirstLogoImageUrl() != null && !b.getFirstLogoImageUrl().isBlank()) {
                        return Optional.of(b.getFirstLogoImageUrl().trim());
                    }
                    if (b.getLogoImageUrl() != null && !b.getLogoImageUrl().isBlank()) {
                        return Optional.of(b.getLogoImageUrl().trim());
                    }
                    if (b.getUrlPayImageUrl() != null && !b.getUrlPayImageUrl().isBlank()) {
                        return Optional.of(b.getUrlPayImageUrl().trim());
                    }
                    return Optional.empty();
                });
                if (fromBrand.isPresent()) {
                    return fromBrand;
                }
            }
            cur = u.getParentId();
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<OrgUnit> requireMerchantOrgByCode(String compId) {
        if (compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT);
    }

    /** 챗봇결제 사용(Y)·서비스 활성 가맹점만 공개 API 허용 */
    @Transactional(readOnly = true)
    public boolean isChatbotPaymentOpenForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return false;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> "Y".equalsIgnoreCase(mp.getChatbotPaymentUseYn() != null ? mp.getChatbotPaymentUseYn().trim() : ""))
                .orElse(false);
    }

    /** 상위 조직 「운영 보류」로 공개 카탈로그·주문 등 상업 기능이 막혀 있는지 */
    @Transactional(readOnly = true)
    public boolean isMerchantChatbotCommerceHold(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> "Y".equalsIgnoreCase(
                        mp.getChatbotCommerceHoldYn() != null ? mp.getChatbotCommerceHoldYn().trim() : ""))
                .orElse(false);
    }

    /** 챗봇결제 활성이면서 상업 기능(상품 노출·주문)이 허용된 상태 */
    @Transactional(readOnly = true)
    public boolean isChatbotCommercialFeaturesOpen(Long orgUnitId) {
        return isChatbotPaymentOpenForMerchant(orgUnitId) && !isMerchantChatbotCommerceHold(orgUnitId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllForOrg(Long orgUnitId) {
        return productRepository.findByOrgUnitIdOrderBySortOrderAscIdAsc(orgUnitId).stream()
                .map(this::toMap)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublicCatalog(Long orgUnitId) {
        return productRepository
                .findByOrgUnitIdAndUseYnAndHqCatalogBlockYnOrderBySortOrderAscIdAsc(orgUnitId, "Y", "N")
                .stream()
                .map(this::toPublicMap)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 상위 조직 상품관리: 산하 가맹점들의 상품을 한 목록으로 (가맹코드·명 포함).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProductsForMerchantOrgIds(List<Long> merchantOrgUnitIds) {
        if (merchantOrgUnitIds == null || merchantOrgUnitIds.isEmpty()) {
            return List.of();
        }
        List<MerchantChatbotProduct> rows = productRepository
                .findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAscIdAsc(merchantOrgUnitIds);
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> ouIds = rows.stream().map(MerchantChatbotProduct::getOrgUnitId).collect(Collectors.toSet());
        Map<Long, OrgUnit> ouById = orgUnitRepository.findAllById(ouIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (MerchantChatbotProduct p : rows) {
            Map<String, Object> m = toMap(p);
            OrgUnit ou = ouById.get(p.getOrgUnitId());
            m.put("compId", ou != null && ou.getCode() != null ? ou.getCode() : "");
            m.put("merchantName", ou != null && ou.getName() != null ? ou.getName() : "");
            if (ou != null) {
                m.put("merchantDefaultCurrency", normalizeBillingDefaultCurrency(resolveEffectiveBaseCurrencyIso(ou)));
            }
            out.add(m);
        }
        return out;
    }

    /**
     * 챗봇 상품 통화 UI: 청구 허용 통화 목록 + 가맹(상속 포함) 기준 기본 통화.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> currencyMetaForMerchantComp(String compId) {
        Optional<OrgUnit> ouOpt = requireMerchantOrgByCode(compId);
        if (ouOpt.isEmpty()) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allowedCurrencies", new ArrayList<>(ChatbotProductPricingUtil.BILLING_CURRENCIES));
        m.put("defaultCurrency", normalizeBillingDefaultCurrency(resolveEffectiveBaseCurrencyIso(ouOpt.get())));
        Long mid = ouOpt.get().getId();
        int maxImg = getEffectiveMaxProductImages(mid);
        m.put("effectiveMaxProductImages", maxImg);
        m.put("allowedListingTypes", new ArrayList<>(effectiveListingTypesOrdered(mid)));
        Optional<MerchantProfile> mpShelf = merchantProfileRepository.findByOrgUnitId(mid);
        ChatbotPromotionShelfMode shelfMode = mpShelf
                .map(mp -> ChatbotPromotionShelfMode.resolveStored(mp.getChatbotPromotionShelfMode()))
                .orElse(ChatbotPromotionShelfMode.PROMOTION);
        m.put("promotionShelfMode", shelfMode.name());
        int rotSec = mpShelf.map(MerchantProfile::getChatbotPromotionRotateSeconds).orElse(30);
        m.put("promotionRotateSeconds", ChatbotPromotionShelfMode.normalizeRotateSeconds(rotSec));
        return m;
    }

    /**
     * 챗봇-pay 상단 프로모션 표시 방식·순환 간격(가맹 프로필). 상품관리 화면에서 가맹·상위 조직이 저장.
     */
    @Transactional
    public Map<String, Object> savePromotionShelfSettingsForMerchantOrg(long merchantOrgUnitId,
                                                                        String modeRaw,
                                                                        Integer rotateSeconds) {
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        ChatbotPromotionShelfMode mode = ChatbotPromotionShelfMode.resolveStored(modeRaw);
        mp.setChatbotPromotionShelfMode(mode.name());
        mp.setChatbotPromotionRotateSeconds(ChatbotPromotionShelfMode.normalizeRotateSeconds(rotateSeconds));
        merchantProfileRepository.save(mp);
        if (mode == ChatbotPromotionShelfMode.HIDDEN) {
            productRepository.clearPromotionShelfYnForOrgUnit(merchantOrgUnitId);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("promotionShelfMode", mode.name());
        out.put("promotionRotateSeconds", mp.getChatbotPromotionRotateSeconds());
        return out;
    }

    /**
     * 조직 체인(총본사→…→가맹) 에서 명시된 이미지 상한 중 최소. 미지정 단계 무시. 전부 미지정이면 1.
     */
    @Transactional(readOnly = true)
    public int getEffectiveMaxProductImages(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return 4;
        }
        Integer min = null;
        for (Long oid : orgChainRootFirst(merchantOrgUnitId)) {
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(oid);
            if (mp.isEmpty()) {
                continue;
            }
            Integer raw = ChatbotCatalogPolicy.clampImageGrant(mp.get().getChatbotMaxProductImagesGrant());
            if (raw != null) {
                min = min == null ? raw : Math.min(min, raw);
            }
        }
        return min != null ? min : 4;
    }

    /** 상위 교집합 후 가맹 활성 교집합. */
    /** 가맹 실효 허용 카탈로그 유형 코드 집합 */
    @Transactional(readOnly = true)
    public LinkedHashSet<String> resolveEffectiveListingTypeCodes(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return new LinkedHashSet<>(ChatbotCatalogPolicy.orderedAllListingCodes());
        }
        List<Long> chain = orgChainRootFirst(merchantOrgUnitId);
        List<LinkedHashSet<String>> grants = new ArrayList<>();
        for (Long oid : chain) {
            merchantProfileRepository.findByOrgUnitId(oid).ifPresent(mp -> {
                LinkedHashSet<String> seg = ChatbotCatalogPolicy.parseListingCsvOrNull(mp.getChatbotCatalogListingGrant());
                if (seg != null) {
                    grants.add(seg);
                }
            });
        }
        LinkedHashSet<String> mask = ChatbotCatalogPolicy.intersectGrants(grants);
        LinkedHashSet<String> enabled = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .map(m -> ChatbotCatalogPolicy.parseListingCsvOrNull(m.getChatbotCatalogListingEnabled()))
                .orElse(null);
        return ChatbotCatalogPolicy.intersectEnabled(mask, enabled);
    }

    private List<String> effectiveListingTypesOrdered(Long merchantOrgUnitId) {
        LinkedHashSet<String> set = resolveEffectiveListingTypeCodes(merchantOrgUnitId);
        List<String> out = new ArrayList<>();
        for (String code : ChatbotCatalogPolicy.orderedAllListingCodes()) {
            if (set.contains(code)) {
                out.add(code);
            }
        }
        return out;
    }

    /** 루트(최상위) → 가맹 순 */
    private List<Long> orgChainRootFirst(Long orgUnitId) {
        ArrayList<Long> tailFirst = new ArrayList<>();
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            tailFirst.add(cur);
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            cur = ou != null ? ou.getParentId() : null;
        }
        Collections.reverse(tailFirst);
        return tailFirst;
    }

    private static boolean isReservationListingCode(String lt) {
        if (lt == null || lt.isBlank()) {
            return false;
        }
        Optional<ChatbotListingType> t = ChatbotListingType.fromCode(lt.trim());
        return t.filter(ChatbotListingType::needsReservationWindow).isPresent();
    }

    /** 프로필 첫 토큰 → 없으면 상위 조직 체인에서 상속 (총판 기준통화 정책과 동일). */
    private String resolveEffectiveBaseCurrencyIso(OrgUnit merchantOu) {
        if (merchantOu == null) {
            return null;
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(merchantOu.getId());
        String bc = mpOpt.map(MerchantProfile::getBaseCurrency).orElse(null);
        if (bc != null && !bc.trim().isEmpty()) {
            String tok = ChatbotProductPricingUtil.firstIsoCurrencyToken(bc);
            if (tok != null && !tok.isEmpty()) {
                return tok;
            }
        }
        Long cur = merchantOu.getParentId();
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) {
                break;
            }
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(cur);
            String pbc = mp.map(MerchantProfile::getBaseCurrency).orElse("");
            if (pbc != null && !pbc.trim().isEmpty()) {
                String[] parts = pbc.split(",\\s*");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    return ChatbotProductPricingUtil.firstIsoCurrencyToken(parts[0]);
                }
            }
            cur = ou.getParentId();
        }
        return null;
    }

    private static String normalizeBillingDefaultCurrency(String rawIso) {
        if (rawIso != null && ChatbotProductPricingUtil.isSupportedBillingCurrency(rawIso)) {
            return rawIso.trim().toUpperCase(Locale.ROOT);
        }
        return "KRW";
    }

    /** 가맹(org) 단위 중복 없는 자동 상품코드 — 신규 저장 시에만 사용 */
    private String allocUniqueProductCode(Long orgUnitId) {
        if (orgUnitId == null) {
            throw new IllegalArgumentException("orgUnitId가 필요합니다.");
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
            String candidate = "CB" + hex;
            if (candidate.length() > 64) {
                candidate = candidate.substring(0, 64);
            }
            if (!productRepository.existsByOrgUnitIdAndProductCode(orgUnitId, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("상품 코드 자동 생성에 실패했습니다. 잠시 후 다시 저장하세요.");
    }

    private Map<String, Object> toMap(MerchantChatbotProduct p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("productCode", p.getProductCode() != null ? p.getProductCode() : "");
        m.put("title", p.getTitle() != null ? p.getTitle() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("amount", p.getAmount() != null ? p.getAmount().stripTrailingZeros().toPlainString() : "0");
        m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode() : "KRW");
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("imageUrl2", nz(p.getImageUrl2()));
        m.put("imageUrl3", nz(p.getImageUrl3()));
        m.put("imageUrl4", nz(p.getImageUrl4()));
        m.put("sortOrder", p.getSortOrder() != null ? p.getSortOrder() : 0);
        m.put("useYn", yn(p.getUseYn()));
        m.put("hqCatalogBlockYn", yn(p.getHqCatalogBlockYn()));
        m.put("listingType", normalizeListingTypeStored(p.getListingType()));
        m.put("reservationSlotMinutes", p.getReservationSlotMinutes() != null ? p.getReservationSlotMinutes() : "");
        ChatbotReservationCollectMode cm = ChatbotReservationCollectMode.resolve(p.getReservationCollectMode());
        m.put("reservationCollectMode", cm.getCode());
        m.put("depositAmount", p.getDepositAmount() != null ? p.getDepositAmount().stripTrailingZeros().toPlainString() : "");
        m.put("promotionShelfYn", yn(p.getPromotionShelfYn()));
        m.put("itemNature", p.getItemNature() != null ? p.getItemNature() : "GOODS");
        m.put("imageUrls", imageUrlListForProduct(p));
        return m;
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static List<String> imageUrlListForProduct(MerchantChatbotProduct p) {
        List<String> urls = new ArrayList<>();
        // List.of(...) 는 null 허용 안 함 → 기존 데이터(image_url_2~4 NULL)에서 NPE 발생 가능
        for (String u : new String[]{p.getImageUrl(), p.getImageUrl2(), p.getImageUrl3(), p.getImageUrl4()}) {
            if (u != null && !u.isBlank()) {
                urls.add(u.trim());
            }
        }
        return urls;
    }

    private Map<String, Object> toPublicMap(MerchantChatbotProduct p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        if (p.getProductCode() != null && !p.getProductCode().isBlank()) {
            m.put("productCode", p.getProductCode().trim());
        }
        m.put("title", p.getTitle() != null ? p.getTitle() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("amount", p.getAmount() != null ? p.getAmount().stripTrailingZeros().toPlainString() : "0");
        m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "KRW");
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        List<String> imgs = imageUrlListForProduct(p);
        if (imgs.size() > 1) {
            m.put("imageUrls", imgs);
        }
        m.put("listingType", normalizeListingTypeStored(p.getListingType()));
        if (p.getReservationSlotMinutes() != null) {
            m.put("reservationSlotMinutes", p.getReservationSlotMinutes());
        }
        ChatbotReservationCollectMode cm = ChatbotReservationCollectMode.resolve(p.getReservationCollectMode());
        if (ChatbotListingType.needsReservationWindow(ChatbotListingType.fromCode(normalizeListingTypeStored(p.getListingType())).orElse(ChatbotListingType.SALE))) {
            m.put("reservationCollectMode", cm.getCode());
            if (cm == ChatbotReservationCollectMode.DEPOSIT && p.getDepositAmount() != null) {
                m.put("depositAmount", p.getDepositAmount().stripTrailingZeros().toPlainString());
                m.put("checkoutAmountHint", p.getDepositAmount().stripTrailingZeros().toPlainString());
            }
        }
        m.put("promotionShelfYn", yn(p.getPromotionShelfYn()));
        m.put("sortOrder", p.getSortOrder() != null ? p.getSortOrder() : 0);
        m.put("itemNature", p.getItemNature() != null ? p.getItemNature() : "GOODS");
        return m;
    }

    @Transactional
    public Map<String, Object> saveRow(Long orgUnitId, Map<String, Object> body) {
        return saveRow(orgUnitId, body, false);
    }

    /**
     * @param allowHqCatalogFields 가맹이 아닌 관리 계정만 true — {@code hqCatalogBlockYn} 갱신 허용
     */
    @Transactional
    public Map<String, Object> saveRow(Long orgUnitId, Map<String, Object> body, boolean allowHqCatalogFields) {
        MerchantChatbotProduct p;
        Object idObj = body != null ? body.get("id") : null;
        Long id = null;
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        } else if (idObj != null) {
            try {
                id = Long.parseLong(String.valueOf(idObj).trim());
            } catch (NumberFormatException ignored) {
                id = null;
            }
        }
        boolean isNew = id == null || id <= 0;
        int saleCap = resolveChatbotProductSlotCap(orgUnitId);
        int regCap = registrationCapFromSaleCap(saleCap);
        int extra = ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS;

        String previousUseYn = "N";
        if (!isNew) {
            p = productRepository.findById(id).filter(x -> orgUnitId.equals(x.getOrgUnitId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            previousUseYn = yn(p.getUseYn());
        } else {
            if (saleCap > 0) {
                long cnt = productRepository.countByOrgUnitId(orgUnitId);
                if (cnt >= regCap) {
                    throw new IllegalArgumentException(
                            "등록 가능한 상품은 최대 " + regCap + "건입니다.(판매 활성 최대 " + saleCap
                                    + "건 + 미판매 보관 " + extra + "건) 상품을 삭제하거나 플랜을 올린 뒤 등록하세요.");
                }
            }
            p = new MerchantChatbotProduct();
            p.setOrgUnitId(orgUnitId);
        }
        if (body != null) {
            if (isNew) {
                p.setProductCode(allocUniqueProductCode(orgUnitId));
            } else {
                String codeIn = str(body.get("productCode"));
                if (codeIn != null && !codeIn.isBlank()) {
                    p.setProductCode(codeIn.length() > 64 ? codeIn.substring(0, 64) : codeIn);
                }
            }
            String title = str(body.get("title"));
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("상품명은 필수입니다.");
            }
            p.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
            String desc = str(body.get("description"));
            if (desc != null && desc.length() > 8000) {
                desc = desc.substring(0, 8000);
            }
            p.setDescription(desc);
            p.setAmount(parseAmount(body.get("amount")));
            String cur = str(body.get("currencyCode"));
            if (cur == null || cur.isBlank()) {
                cur = normalizeBillingDefaultCurrency(resolveEffectiveBaseCurrencyIso(
                        orgUnitRepository.findById(orgUnitId).orElse(null)));
            }
            cur = cur.trim().toUpperCase(Locale.ROOT);
            if (!ChatbotProductPricingUtil.isSupportedBillingCurrency(cur)) {
                throw new IllegalArgumentException("허용되지 않은 통화입니다. 목록에서 선택하세요.");
            }
            p.setCurrencyCode(cur.length() > 10 ? cur.substring(0, 10) : cur);
            String img = str(body.get("imageUrl"));
            if (img != null && img.length() > 512) {
                img = img.substring(0, 512);
            }
            p.setImageUrl(img);
            Integer so = parseIntObj(body.get("sortOrder"));
            p.setSortOrder(so != null ? Math.max(1, so) : 1);
            p.setUseYn(yn(str(body.get("useYn"))));
            if (allowHqCatalogFields) {
                p.setHqCatalogBlockYn(yn(str(body.get("hqCatalogBlockYn"))));
            }
            if (body.containsKey("listingType")) {
                p.setListingType(normalizeListingType(body.get("listingType")));
            }
            if (body.containsKey("reservationSlotMinutes")) {
                Integer rsm = parseIntObj(body.get("reservationSlotMinutes"));
                if (rsm == null || rsm <= 0) {
                    p.setReservationSlotMinutes(null);
                } else {
                    int clamped = Math.min(24 * 60, Math.max(15, rsm));
                    ChatbotListingType ltNow = ChatbotListingType.fromCode(normalizeListingTypeStored(p.getListingType()))
                            .orElse(ChatbotListingType.SALE);
                    // 시간 예약은 30분 단위로만 허용
                    if (ltNow == ChatbotListingType.RESERVATION_TIME) {
                        clamped = Math.max(30, (clamped / 30) * 30);
                    }
                    p.setReservationSlotMinutes(clamped);
                }
            }
            if (body.containsKey("promotionShelfYn")) {
                p.setPromotionShelfYn(yn(str(body.get("promotionShelfYn"))));
            }
            if (body.containsKey("itemNature")) {
                String nat = str(body.get("itemNature"));
                if (nat == null || nat.isBlank()) {
                    p.setItemNature("GOODS");
                } else {
                    String u = nat.trim().toUpperCase(Locale.ROOT);
                    if (u.length() > 24) u = u.substring(0, 24);
                    p.setItemNature(u);
                }
            }
            if (body.containsKey("imageUrl2")) {
                p.setImageUrl2(clampStoredUrl(str(body.get("imageUrl2"))));
            }
            if (body.containsKey("imageUrl3")) {
                p.setImageUrl3(clampStoredUrl(str(body.get("imageUrl3"))));
            }
            if (body.containsKey("imageUrl4")) {
                p.setImageUrl4(clampStoredUrl(str(body.get("imageUrl4"))));
            }
            if (isNew || body.containsKey("listingType") || body.containsKey("reservationCollectMode")
                    || body.containsKey("depositAmount") || body.containsKey("amount")) {
                applyReservationCollectFromBody(p, body);
            }
        }

        String ltEff = normalizeListingTypeStored(p.getListingType());
        LinkedHashSet<String> allowedLt = resolveEffectiveListingTypeCodes(orgUnitId);
        if (!allowedLt.contains(ltEff)) {
            throw new IllegalArgumentException(
                    "상위·가맹 정책에서 허용되지 않는 상품 유형입니다: " + ltEff
                            + ". 허용: " + ChatbotCatalogPolicy.joinListingCsv(allowedLt));
        }
        int maxImg = getEffectiveMaxProductImages(orgUnitId);
        if (countFilledImageSlots(p) > maxImg) {
            throw new IllegalArgumentException(
                    "상품 이미지는 조직 설정 기준 최대 " + maxImg + "장까지 등록 가능합니다.");
        }

        if (saleCap > 0) {
            String newYn = yn(p.getUseYn());
            long activeDb = productRepository.countByOrgUnitIdAndUseYn(orgUnitId, "Y");
            long minusOld = (!isNew && "Y".equals(previousUseYn)) ? 1L : 0L;
            long plusNew = "Y".equals(newYn) ? 1L : 0L;
            long projectedActive = activeDb - minusOld + plusNew;
            if (projectedActive > saleCap) {
                throw new IllegalArgumentException(
                        "판매 활성(고객 챗봇·카탈로그에 노출 가능) 상품은 플랜 기준 최대 " + saleCap
                                + "개까지입니다. 다른 상품의 판매 활성을 끄거나(사용=N) 플랜을 변경하세요."
                                + " 미판매로 등록만 해 두는 상품은 총 " + regCap + "건까지 등록할 수 있습니다.");
            }
        }

        return toMap(productRepository.save(p));
    }

    private void applyReservationCollectFromBody(MerchantChatbotProduct p, Map<String, Object> body) {
        ChatbotListingType lt = ChatbotListingType.fromCode(normalizeListingTypeStored(p.getListingType()))
                .orElse(ChatbotListingType.SALE);
        if (!ChatbotListingType.needsReservationWindow(lt)) {
            p.setReservationCollectMode(ChatbotReservationCollectMode.FULL.getCode());
            p.setDepositAmount(null);
            return;
        }
        ChatbotReservationCollectMode cm = body != null && body.containsKey("reservationCollectMode")
                ? ChatbotReservationCollectMode.resolve(str(body.get("reservationCollectMode")))
                : ChatbotReservationCollectMode.resolve(p.getReservationCollectMode());
        if (cm != ChatbotReservationCollectMode.DEPOSIT) {
            p.setReservationCollectMode(ChatbotReservationCollectMode.FULL.getCode());
            p.setDepositAmount(null);
            return;
        }
        BigDecimal dep = parseDepositOptional(body != null ? body.get("depositAmount") : null, p.getDepositAmount());
        if (dep == null || dep.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("예약금(DEPOSIT) 모드에는 예약금액을 0보다 크게 입력하세요.");
        }
        BigDecimal full = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
        if (dep.compareTo(full) >= 0) {
            throw new IllegalArgumentException("예약금은 상품 금액(전체)보다 작아야 합니다.");
        }
        p.setReservationCollectMode(ChatbotReservationCollectMode.DEPOSIT.getCode());
        p.setDepositAmount(dep.setScale(4, RoundingMode.HALF_UP));
    }

    private static BigDecimal parseDepositOptional(Object bodyVal, BigDecimal fallback) {
        if (bodyVal == null) {
            return fallback;
        }
        String s = str(bodyVal);
        if (s == null) {
            return fallback;
        }
        try {
            return new BigDecimal(s.trim().replace(",", "")).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalArgumentException("예약금 형식이 올바르지 않습니다.");
        }
    }

    private static int countFilledImageSlots(MerchantChatbotProduct p) {
        int n = 0;
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            n++;
        }
        if (p.getImageUrl2() != null && !p.getImageUrl2().isBlank()) {
            n++;
        }
        if (p.getImageUrl3() != null && !p.getImageUrl3().isBlank()) {
            n++;
        }
        if (p.getImageUrl4() != null && !p.getImageUrl4().isBlank()) {
            n++;
        }
        return n;
    }

    private static String clampStoredUrl(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return t.length() > 512 ? t.substring(0, 512) : t;
    }

    /**
     * 챗봇결제 Y 가맹의 등록 건수 상한. 미설정·0 이하면 제한 없음.
     */
    private int resolveChatbotProductSlotCap(Long orgUnitId) {
        return getEffectiveChatbotProductSlotCap(orgUnitId);
    }

    /** 플랜 기준 「판매 활성(use_yn=Y)」 동시 허용 상한. 0이면 무제한(플랜 미설정 또는 챗봇 미사용). 총 등록 상한은 {@link #getEffectiveRegistrationCap(Long)}. */
    @Transactional(readOnly = true)
    public int getEffectiveChatbotProductSlotCap(Long orgUnitId) {
        if (orgUnitId == null) {
            return 0;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty()) {
            return 0;
        }
        MerchantProfile m = mp.get();
        if (!"Y".equalsIgnoreCase(m.getChatbotPaymentUseYn() != null ? m.getChatbotPaymentUseYn().trim() : "")) {
            return 0;
        }
        Integer slot = m.getChatbotProductSlotLimit();
        if (slot == null || slot <= 0) {
            return 0;
        }
        return ChatbotProductPricingUtil.isAllowedSlot(slot) ? slot : 0;
    }

    @Transactional
    public void deleteRow(Long orgUnitId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID가 필요합니다.");
        }
        MerchantChatbotProduct p = productRepository.findById(productId)
                .filter(x -> orgUnitId.equals(x.getOrgUnitId()))
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        productRepository.delete(p);
    }

    private static String normalizeListingTypeStored(String stored) {
        return ChatbotListingType.fromCode(stored != null ? stored : "")
                .map(ChatbotListingType::getCode)
                .orElse(ChatbotListingType.SALE.getCode());
    }

    private static String normalizeListingType(Object raw) {
        String s = str(raw);
        if (s == null) {
            return ChatbotListingType.SALE.getCode();
        }
        Optional<ChatbotListingType> t = ChatbotListingType.fromCode(s);
        return t.map(ChatbotListingType::getCode).orElse(ChatbotListingType.SALE.getCode());
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v != null ? v.trim() : "") ? "Y" : "N";
    }

    private static Integer parseIntObj(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("금액을 입력하세요.");
        }
        BigDecimal a;
        try {
            a = new BigDecimal(String.valueOf(o).trim().replace(",", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("금액 형식이 올바르지 않습니다.");
        }
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        return a.setScale(4, RoundingMode.HALF_UP);
    }
}
