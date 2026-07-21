package com.pg.urlpay;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * 결제창 표시 7항목 — 본사 기본 × 가맹 FOLLOW_HQ/개별 설정(가맹 우선).
 */
@Service
public class UrlPayCheckoutDisplayPolicyService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public UrlPayCheckoutDisplayPolicyService(HqApiConfigRepository hqApiConfigRepository,
                                              MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public HqApiConfig hqOrEmpty() {
        return hqApiConfigRepository.findAll().stream().findFirst().orElseGet(HqApiConfig::new);
    }

    public String effectiveProductNameUseYn(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getUrlPayProductNameUseYn),
                hqOrEmpty().getUrlPayProductNameUseDefaultYn(), "Y");
    }

    public String effectiveCompanyNameShowYn(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getUrlPayCompanyNameShowYn),
                hqOrEmpty().getUrlPayCompanyNameShowDefaultYn(), "Y");
    }

    public String effectiveLangMenuUseYn(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getUrlPayLangMenuUseYn),
                hqOrEmpty().getUrlPayLangMenuUseDefaultYn(), "Y");
    }

    public String effectiveShippingAddressUseYn(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getUrlPayShippingAddressUseYn),
                hqOrEmpty().getUrlPayShippingAddressUseDefaultYn(), "N");
    }

    public String effectiveRememberMode(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.normalizeStored(
                merchantField(orgUnitId, MerchantProfile::getCheckoutContactRememberMode));
    }

    /** 가맹이 상품명 사용을 본사설정 따름인지 */
    public boolean isProductNameUseFollowHq(Long orgUnitId) {
        return UrlPayFollowHqYnUtil.isFollowHq(merchantField(orgUnitId, MerchantProfile::getUrlPayProductNameUseYn));
    }

    public void putEffectiveDefaultProductIntoMap(Map<String, Object> data, Long orgUnitId,
                                                  Optional<MerchantDefaultProduct> merchantProduct) {
        if (data == null || orgUnitId == null) {
            return;
        }
        String useYn = effectiveProductNameUseYn(orgUnitId);
        data.put("urlPayProductNameUseYn", useYn);
        if (!"Y".equalsIgnoreCase(useYn)) {
            return;
        }
        if (isProductNameUseFollowHq(orgUnitId)) {
            HqApiConfig hq = hqOrEmpty();
            if (hq.getUrlPayDefaultProductName() != null && !hq.getUrlPayDefaultProductName().isBlank()) {
                data.put("defaultProductName", hq.getUrlPayDefaultProductName().trim());
            }
            if (hq.getUrlPayDefaultProductCode() != null && !hq.getUrlPayDefaultProductCode().isBlank()) {
                data.put("defaultProductCode", hq.getUrlPayDefaultProductCode().trim());
            }
            if (hq.getUrlPayDefaultProductDesc() != null && !hq.getUrlPayDefaultProductDesc().isBlank()) {
                data.put("defaultProductDesc", hq.getUrlPayDefaultProductDesc().trim());
            }
            /* 기본금액 — 결제창 프리필 없음(기존과 동일) */
            return;
        }
        if (merchantProduct != null && merchantProduct.isPresent()) {
            MerchantDefaultProduct p = merchantProduct.get();
            if (p.getProductName() != null && !p.getProductName().isBlank()) {
                data.put("defaultProductName", p.getProductName().trim());
            }
            if (p.getProductCode() != null && !p.getProductCode().isBlank()) {
                data.put("defaultProductCode", p.getProductCode().trim());
            }
            if (p.getProductDesc() != null && !p.getProductDesc().isBlank()) {
                data.put("defaultProductDesc", p.getProductDesc().trim());
            }
        }
    }

    public String effectiveLogoMode(Long orgUnitId) {
        return WebPaymentHeaderLogoModeUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getWebPaymentHeaderLogoMode),
                hqOrEmpty().getWebPaymentHeaderLogoModeDefault());
    }

    public String effectiveSubtitleMode(Long orgUnitId) {
        return CheckoutHeaderSubtitleModeUtil.resolveEffective(
                merchantField(orgUnitId, MerchantProfile::getWebPaymentHeaderSubtitleMode),
                hqOrEmpty().getWebPaymentHeaderSubtitleModeDefault());
    }

    public void putEffectiveYnIntoMap(Map<String, Object> data, Long orgUnitId) {
        if (data == null || orgUnitId == null) {
            return;
        }
        data.put("urlPayProductNameUseYn", effectiveProductNameUseYn(orgUnitId));
        data.put("urlPayCompanyNameShowYn", effectiveCompanyNameShowYn(orgUnitId));
        data.put("urlPayLangMenuUseYn", effectiveLangMenuUseYn(orgUnitId));
        data.put("urlPayShippingAddressUseYn", effectiveShippingAddressUseYn(orgUnitId));
        data.put("checkoutContactRememberMode", effectiveRememberMode(orgUnitId));
    }

    private String merchantField(Long orgUnitId, java.util.function.Function<MerchantProfile, String> getter) {
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        return mp.map(getter).orElse(null);
    }
}
