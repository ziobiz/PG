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
 * URL·JPAY 공개 결제창 상단 로고·경고문구 — 가맹 {@code web_payment_header_*_mode} 우선,
 * 로고 DEFAULT 시 상위 총판 {@code url_pay_image_url}·{@code logo_image_url}.
 */
@Service
public class CheckoutHeaderLogoResolver {

    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgBrandingRepository orgBrandingRepository;

    public CheckoutHeaderLogoResolver(MerchantProfileRepository merchantProfileRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      OrgBrandingRepository orgBrandingRepository) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgBrandingRepository = orgBrandingRepository;
    }

    public record Resolved(String mode, Optional<String> url) {
    }

    public record SubtitleResolved(String mode, Optional<String> text) {
    }

    public Resolved resolve(Long merchantOrgUnitId) {
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        String mode = WebPaymentHeaderLogoModeUtil.normalize(
                prof.map(MerchantProfile::getWebPaymentHeaderLogoMode).orElse(WebPaymentHeaderLogoModeUtil.DEFAULT));
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
        String mode = WebPaymentHeaderLogoModeUtil.normalize(
                prof.map(MerchantProfile::getWebPaymentHeaderSubtitleMode).orElse(WebPaymentHeaderLogoModeUtil.DEFAULT));
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(mode)) {
            return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        if (WebPaymentHeaderLogoModeUtil.ACTIVE.equals(mode)) {
            String custom = prof.map(MerchantProfile::getWebPaymentHeaderSubtitleText).orElse("");
            if (custom == null || custom.isBlank()) {
                return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.empty());
            }
            return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.DEFAULT, Optional.empty());
    }

    public void applyToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        Resolved r = resolve(merchantOrgUnitId);
        data.put("checkoutHeaderLogoMode", r.mode());
        r.url().ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        SubtitleResolved st = resolveSubtitle(merchantOrgUnitId);
        data.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> data.put("checkoutHeaderSubtitleText", t));
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
        String mode = WebPaymentHeaderLogoModeUtil.normalize(
                prof.map(MerchantProfile::getSplitPayHeaderSubtitleMode).orElse(WebPaymentHeaderLogoModeUtil.DEFAULT));
        if (WebPaymentHeaderLogoModeUtil.DISABLED.equals(mode)) {
            return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.DISABLED, Optional.empty());
        }
        if (WebPaymentHeaderLogoModeUtil.ACTIVE.equals(mode)) {
            String custom = prof.map(MerchantProfile::getSplitPayHeaderSubtitleText).orElse("");
            if (custom == null || custom.isBlank()) {
                return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.empty());
            }
            return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.ACTIVE, Optional.of(custom.trim()));
        }
        return new SubtitleResolved(WebPaymentHeaderLogoModeUtil.DEFAULT, Optional.empty());
    }

    public void applySplitPayToCheckoutMap(Map<String, Object> data, Long merchantOrgUnitId) {
        if (data == null || merchantOrgUnitId == null) {
            return;
        }
        Resolved r = resolveSplitPay(merchantOrgUnitId);
        data.put("checkoutHeaderLogoMode", r.mode());
        r.url().ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        SubtitleResolved st = resolveSplitPaySubtitle(merchantOrgUnitId);
        data.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> data.put("checkoutHeaderSubtitleText", t));
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
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
        m.put("checkoutHeaderSubtitleMode", st.mode());
        st.text().ifPresent(t -> m.put("checkoutHeaderSubtitleText", t));
        return m;
    }
}
