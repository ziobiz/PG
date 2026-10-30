package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.urlpay.WebPayQrBrandLabelModeUtil;
import com.pg.urlpay.WebPaymentQrModeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 웹용 QR 열기 HTML — 상단·하단 문구 해석 */
@Service
public class WebPayQrBrandViewService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final OrgServiceUseService orgServiceUseService;

    public WebPayQrBrandViewService(OrgUnitRepository orgUnitRepository,
                                    MerchantProfileRepository merchantProfileRepository,
                                    HqApiConfigRepository hqApiConfigRepository,
                                    OrgServiceUseService orgServiceUseService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> resolvePublicView(String compCode) {
        if (compCode == null || compCode.isBlank()) {
            return Optional.empty();
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compCode.trim())
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT);
        if (ouOpt.isEmpty()) {
            return Optional.empty();
        }
        OrgUnit ou = ouOpt.get();
        if (!orgServiceUseService.isOrgServiceActive(ou.getId())) {
            return Optional.empty();
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.getId());
        if (mpOpt.isEmpty() || !WebPaymentQrModeUtil.isEffective(mpOpt.get())) {
            return Optional.empty();
        }
        MerchantProfile mp = mpOpt.get();
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        String hqTitleMode = hq != null ? hq.getWebPayQrTitleModeDefault() : WebPayQrBrandLabelModeUtil.ACTIVE;
        String hqFooterMode = hq != null ? hq.getWebPayQrFooterModeDefault() : WebPayQrBrandLabelModeUtil.ACTIVE;
        String hqTitleText = hq != null ? hq.getWebPayQrTitleTextDefault() : null;
        String hqFooterText = hq != null ? hq.getWebPayQrFooterTextDefault() : null;

        String titleMode = WebPayQrBrandLabelModeUtil.resolveEffective(mp.getWebPayQrTitleMode(), hqTitleMode);
        String footerMode = WebPayQrBrandLabelModeUtil.resolveEffective(mp.getWebPayQrFooterMode(), hqFooterMode);

        String companyName = firstNonBlank(ou.getName(), ou.getCode());
        String titleText = null;
        if (WebPayQrBrandLabelModeUtil.ACTIVE.equals(titleMode)) {
            boolean follow = WebPayQrBrandLabelModeUtil.FOLLOW_HQ.equals(
                    WebPayQrBrandLabelModeUtil.normalizeMerchantStored(mp.getWebPayQrTitleMode()));
            String custom = follow ? hqTitleText : mp.getWebPayQrTitleText();
            titleText = firstNonBlank(custom, companyName);
        }

        String footerText = null;
        if (WebPayQrBrandLabelModeUtil.ACTIVE.equals(footerMode)) {
            boolean follow = WebPayQrBrandLabelModeUtil.FOLLOW_HQ.equals(
                    WebPayQrBrandLabelModeUtil.normalizeMerchantStored(mp.getWebPayQrFooterMode()));
            String custom = follow ? hqFooterText : mp.getWebPayQrFooterText();
            footerText = firstNonBlank(custom, WebPayQrBrandLabelModeUtil.DEFAULT_FOOTER);
        }

        Map<String, Object> m = new HashMap<>();
        m.put("compId", ou.getCode());
        m.put("companyName", companyName);
        m.put("titleText", titleText);
        m.put("footerText", footerText);
        m.put("showTitle", titleText != null && !titleText.isBlank());
        m.put("showFooter", footerText != null && !footerText.isBlank());
        return Optional.of(m);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }
}
