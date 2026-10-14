package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * URL·JPAY 공개 결제창 상단 로고·경고문구 — 가맹 설정 우선, FOLLOW_HQ 시 본사 기본.
 */
@Service
public class CheckoutHeaderLogoResolver {

    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final UrlPayCheckoutDisplayPolicyService checkoutDisplayPolicyService;
    private final UrlPayCardInputCopyService urlPayCardInputCopyService;
    private final CheckoutHeaderSubtitleCopyService checkoutHeaderSubtitleCopyService;

    public CheckoutHeaderLogoResolver(MerchantProfileRepository merchantProfileRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      OrgBrandingRepository orgBrandingRepository,
                                      UrlPayCheckoutDisplayPolicyService checkoutDisplayPolicyService,
                                      UrlPayCardInputCopyService urlPayCardInputCopyService,
                                      CheckoutHeaderSubtitleCopyService checkoutHeaderSubtitleCopyService) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.checkoutDisplayPolicyService = checkoutDisplayPolicyService;
        this.urlPayCardInputCopyService = urlPayCardInputCopyService;
        this.checkoutHeaderSubtitleCopyService = checkoutHeaderSubtitleCopyService;
    }

    public record Resolved(String mode, Optional<String> url) {
    }

    public record SubtitleResolved(String mode, Optional<String> text) {
    }

    public Resolved resolve(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String mode = checkoutDisplayPolicyService.effectiveLogoMode(merchantOrgUnitId);
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        if (WebPaymentHeaderLogoModeUtil.ACTIVE.equals(mode)) {
            String custom = prof.map(MerchantProfile::getWebPaymentHeaderLogoUrl).orElse("");
            if (custom == null || custom.isBlank()) {
                return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.empty());
            }
            return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        if (WebPaymentHeaderLogoModeUtil.HTML.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.HTML, Optional.empty());
        }
        return new Resolved(WebPaymentHeaderLogoModeUtil.DEFAULT, resolveMasterDistBrandingLogo(merchantOrgUnitId));
    }

    public SubtitleResolved resolveSubtitle(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String mode = checkoutDisplayPolicyService.effectiveSubtitleMode(merchantOrgUnitId);
        if (CheckoutHeaderSubtitleModeUtil.DISABLED.equals(mode)) {
            return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.DISABLED, Optional.empty());
        }
        if (CheckoutHeaderSubtitleModeUtil.isDirectActive(mode)) {
            String custom = prof.map(MerchantProfile::getWebPaymentHeaderSubtitleText).orElse("");
            if (custom == null || custom.isBlank()) {
                return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.ACTIVE, Optional.empty());
            }
            return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        if (CheckoutHeaderSubtitleModeUtil.isPreset(mode)) {
            return new SubtitleResolved(mode, Optional.empty());
        }
        return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.DEFAULT, Optional.empty());
    }

    public void applyToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        applyToCheckoutMap(data, merchantOrgUnitId, null);
    }

    /**
     * @param checkoutEntry {@code merchant_api} 이면 API 로고설정, 그 외·null 이면 웹결제(URL) 로고설정
     */
    public void applyToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId, String checkoutEntry) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        boolean apiEntry = checkoutEntry != null
                && "merchant_api".equalsIgnoreCase(checkoutEntry.trim());
        Resolved r = apiEntry ? resolveApi(merchantOrgUnitId) : resolve(merchantOrgUnitId);
        data.put("checkoutHeaderLogoMode", r.mode());
        data.put("checkoutHeaderLogoChannel", apiEntry ? "API" : "WEB");
        r.url().ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        if (apiEntry) {
            String apiModeRaw = prof.map(MerchantProfile::getApiPaymentHeaderLogoMode).orElse("DISABLED");
            data.put("apiPaymentHeaderLogoMode", apiModeRaw != null ? apiModeRaw : "DISABLED");
            boolean followWeb = apiModeRaw != null
                    && (WebPaymentHeaderLogoModeUtil.FOLLOW_HQ.equalsIgnoreCase(apiModeRaw.trim())
                    || "HQ".equalsIgnoreCase(apiModeRaw.trim()));
            if (followWeb) {
                prof.map(MerchantProfile::getWebPaymentHeaderHtmlTitle)
                        .filter(t -> t != null && !t.isBlank())
                        .ifPresent(t -> data.put("checkoutHeaderHtmlTitle", t.trim()));
            } else {
                prof.map(MerchantProfile::getApiPaymentHeaderHtmlTitle)
                        .filter(t -> t != null && !t.isBlank())
                        .ifPresent(t -> data.put("checkoutHeaderHtmlTitle", t.trim()));
            }
        } else {
            prof.map(MerchantProfile::getWebPaymentHeaderHtmlTitle)
                    .filter(t -> t != null && !t.isBlank())
                    .ifPresent(t -> data.put("checkoutHeaderHtmlTitle", t.trim()));
        }
        SubtitleResolved st = resolveSubtitle(merchantOrgUnitId);
        data.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> data.put("checkoutHeaderSubtitleText", t));
        prof.ifPresent(mp -> {
            urlPayCardInputCopyService.putCheckoutFields(data, mp);
            checkoutHeaderSubtitleCopyService.putCheckoutFields(data, mp);
        });
    }

    /**
     * API/embed 로고. 기본 DISABLED. FOLLOW_HQ 이면 웹결제 로고({@link #resolve})를 따름.
     */
    public Resolved resolveApi(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String stored = prof.map(MerchantProfile::getApiPaymentHeaderLogoMode).orElse("DISABLED");
        if (stored == null || stored.isBlank()) {
            stored = WebPaymentHeaderLogoModeUtil.DISABLED;
        }
        String u = stored.trim().toUpperCase(java.util.Locale.ROOT);
        if (WebPaymentHeaderLogoModeUtil.FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return resolve(merchantOrgUnitId);
        }
        String mode = WebPaymentHeaderLogoModeUtil.normalize(u);
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        if (WebPaymentHeaderLogoModeUtil.ACTIVE.equals(mode)) {
            String custom = prof.map(MerchantProfile::getApiPaymentHeaderLogoUrl).orElse("");
            if (custom == null || custom.isBlank()) {
                return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.empty());
            }
            return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        if (WebPaymentHeaderLogoModeUtil.HTML.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.HTML, Optional.empty());
        }
        return new Resolved(WebPaymentHeaderLogoModeUtil.DEFAULT, resolveMasterDistBrandingLogo(merchantOrgUnitId));
    }

    /**
     * 크립토 결제·수단선택 상단 로고(A형).
     * {@code DISABLED} → 미노출. {@code FOLLOW_HQ}(기본) → 웹결제 로고({@link #resolve})와 동일.
     */
    public Resolved resolveCrypto(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String stored = prof.map(MerchantProfile::getCryptoPaymentHeaderLogoMode).orElse("FOLLOW_HQ");
        if (stored == null || stored.isBlank()) {
            stored = "FOLLOW_HQ";
        }
        String u = stored.trim().toUpperCase(java.util.Locale.ROOT);
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(u)
                || "N".equals(u) || "OFF".equals(u) || "INACTIVE".equals(u)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        return resolve(merchantOrgUnitId);
    }

    /** 크립토 prepare/session/checkout-context 용 — 웹결제 HTML 표시명·크립토 안내문구. */
    public void applyCryptoToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String stored = prof.map(MerchantProfile::getCryptoPaymentHeaderLogoMode).orElse("FOLLOW_HQ");
        data.put("cryptoPaymentHeaderLogoMode", stored != null && !stored.isBlank() ? stored : "FOLLOW_HQ");
        Resolved r = resolveCrypto(merchantOrgUnitId);
        data.put("checkoutHeaderLogoMode", r.mode());
        data.put("checkoutHeaderLogoChannel", "CRYPTO");
        r.url().ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        if (!WebPaymentHeaderLogoModeUtil.DISABLED.equals(r.mode())) {
            prof.map(MerchantProfile::getWebPaymentHeaderHtmlTitle)
                    .filter(t -> t != null && !t.isBlank())
                    .ifPresent(t -> data.put("checkoutHeaderHtmlTitle", t.trim()));
        }
        prof.ifPresent(mp -> checkoutHeaderSubtitleCopyService.putCryptoCheckoutFields(data, mp));
    }

    /** 통합(수단선택) checkout-context — 로고는 크립토 A형, 안내문구는 통합 전용. */
    public void applyUnifiedToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        applyCryptoToCheckoutMap(data, merchantOrgUnitId);
        data.put("checkoutHeaderLogoChannel", "UNIFIED");
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        data.remove("checkoutHeaderSubtitleText");
        data.remove("checkoutHeaderSubtitleTextI18n");
        prof.ifPresent(mp -> checkoutHeaderSubtitleCopyService.putUnifiedCheckoutFields(data, mp));
    }

    public Resolved resolveSplitPay(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String mode = WebPaymentHeaderLogoModeUtil.normalize(
                prof.map(MerchantProfile::getSplitPayHeaderLogoMode).orElse(WebPaymentHeaderLogoModeUtil.HTML));
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        if (WebPaymentHeaderLogoModeUtil.ACTIVE.equals(mode)) {
            String custom = prof.map(MerchantProfile::getSplitPayHeaderLogoUrl).orElse("");
            if (custom == null || custom.isBlank()) {
                return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.empty());
            }
            return new Resolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        if (WebPaymentHeaderLogoModeUtil.HTML.equals(mode)) {
            return new Resolved(WebPaymentHeaderLogoModeUtil.HTML, Optional.empty());
        }
        return new Resolved(WebPaymentHeaderLogoModeUtil.DEFAULT, resolveMasterDistBrandingLogo(merchantOrgUnitId));
    }

    public SubtitleResolved resolveSplitPaySubtitle(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String mode = CheckoutHeaderSubtitleModeUtil.normalize(
                prof.map(MerchantProfile::getSplitPayHeaderSubtitleMode).orElse(CheckoutHeaderSubtitleModeUtil.DEFAULT));
        if (CheckoutHeaderSubtitleModeUtil.DISABLED.equals(mode)) {
            return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.DISABLED, Optional.empty());
        }
        if (CheckoutHeaderSubtitleModeUtil.isDirectActive(mode)) {
            String custom = prof.map(MerchantProfile::getSplitPayHeaderSubtitleText).orElse("");
            if (custom == null || custom.isBlank()) {
                return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.ACTIVE, Optional.empty());
            }
            return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        if (CheckoutHeaderSubtitleModeUtil.isPreset(mode)) {
            return new SubtitleResolved(mode, Optional.empty());
        }
        return new SubtitleResolved(CheckoutHeaderSubtitleModeUtil.DEFAULT, Optional.empty());
    }

    public void applySplitPayToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        Resolved r = resolveSplitPay(merchantOrgUnitId);
        data.put("checkoutHeaderLogoMode", r.mode());
        r.url().ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        prof.map(MerchantProfile::getSplitPayHeaderHtmlTitle)
                .filter(t -> t != null && !t.isBlank())
                .ifPresent(t -> data.put("checkoutHeaderHtmlTitle", t.trim()));
        SubtitleResolved st = resolveSplitPaySubtitle(merchantOrgUnitId);
        data.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> data.put("checkoutHeaderSubtitleText", t));
        String langMenu = prof.map(MerchantProfile::getSplitPayLangMenuUseYn).orElse("Y");
        data.put("splitPayLangMenuUseYn", langMenu != null && "Y".equalsIgnoreCase(langMenu.trim()) ? "Y" : "N");
    }

    private Optional<String> resolveMasterDistBrandingLogo(Long merchantOrgUnitId) {
        Long cur = merchantOrgUnitId;
        while (cur != null) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) {
                break;
            }
            OrgUnit u = opt.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return orgBrandingRepository.findByOrgUnitId(u.getId()).flatMap(b -> {
                    String up = b.getUrlPayImageUrl();
                    if (up != null && !up.isBlank()) {
                        return Optional.of(up.trim());
                    }
                    String lg = b.getLogoImageUrl();
                    if (lg != null && !lg.isBlank()) {
                        return Optional.of(lg.trim());
                    }
                    return Optional.empty();
                });
            }
            cur = u.getParentId();
        }
        return Optional.empty();
    }

    /** checkout-context 응답용 — mode·url만 필요할 때 */
    public Map<String, Object> asMap(Long merchantOrgUnitId) {
        Resolved r = resolve(merchantOrgUnitId);
        SubtitleResolved st = resolveSubtitle(merchantOrgUnitId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("checkoutHeaderLogoMode", r.mode());
        r.url().ifPresent(u -> m.put("checkoutHeaderLogoUrl", u));
        merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .map(MerchantProfile::getWebPaymentHeaderHtmlTitle)
                .filter(t -> t != null && !t.isBlank())
                .ifPresent(t -> m.put("checkoutHeaderHtmlTitle", t.trim()));
        m.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> m.put("checkoutHeaderSubtitleText", t));
        return m;
    }
}
