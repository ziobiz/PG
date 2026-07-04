package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.service.MerchantChatbotProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 본사 mobileCheckoutModeDefault × 가맹 mobileCheckoutMode 오버라이드.
 */
@Service
public class MobileCheckoutModeService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantChatbotProductService productService;

    public MobileCheckoutModeService(HqApiConfigRepository hqApiConfigRepository,
                                     MerchantProfileRepository merchantProfileRepository,
                                     MerchantChatbotProductService productService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.productService = productService;
    }

    public String resolveHqDefault() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> MobileCheckoutModeUtil.normalize(c.getMobileCheckoutModeDefault()))
                .orElse(MobileCheckoutModeUtil.EMBED);
    }

    public String resolveEffective(Long orgUnitId) {
        String hq = resolveHqDefault();
        if (orgUnitId == null) {
            return hq;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty()) {
            return hq;
        }
        String override = MobileCheckoutModeUtil.normalizeMerchantOverride(mp.get().getMobileCheckoutMode());
        return override != null ? override : hq;
    }

    public void putEffectiveIntoMap(Map<String, Object> data, Long orgUnitId) {
        if (data == null) {
            return;
        }
        String effective = resolveEffective(orgUnitId);
        data.put("mobileCheckoutModeEffective", effective);
        data.put("hqMobileCheckoutModeDefault", resolveHqDefault());
        merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
            String ov = MobileCheckoutModeUtil.normalizeMerchantOverride(mp.getMobileCheckoutMode());
            if (ov != null) {
                data.put("mobileCheckoutMode", ov);
            }
        });
    }

    /**
     * inline session 응답 — embed 위젯 모바일 auto-redirect 용 payUrl.
     */
    public void enrichInlineSession(Map<String, Object> data, Long orgUnitId, HttpServletRequest request) {
        if (data == null || orgUnitId == null) {
            return;
        }
        putEffectiveIntoMap(data, orgUnitId);
        String compId = str(data.get("compId"));
        String sessionToken = str(data.get("sessionToken"));
        if (compId.isBlank()) {
            return;
        }
        String pgVendor = str(data.get("pgVendor"));
        if (pgVendor.isBlank()) {
            pgVendor = "CHILLPAY";
        }
        String payUrl = buildEmbedPayUrl(request, compId, pgVendor, sessionToken, str(data.get("langCode")));
        if (!payUrl.isBlank()) {
            data.put("payUrl", payUrl);
        }
    }

    public String buildEmbedPayUrl(HttpServletRequest request, String compId, String pgVendor,
                                   String sessionToken, String langCode) {
        if (compId == null || compId.isBlank()) {
            return "";
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        // PG 무관 중립 결제창 경로로 통일 — 가맹점·구매자에게 실제 PG(pgVendor)를 노출하지 않는다.
        return NeutralCheckoutRoute.buildPayUrl(base, compId, sessionToken, langCode, true);
    }

    public Map<String, Object> buildStatusBlock(Long orgUnitId) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("hqMobileCheckoutModeDefault", resolveHqDefault());
        block.put("mobileCheckoutModeEffective", resolveEffective(orgUnitId));
        merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
            String ov = MobileCheckoutModeUtil.normalizeMerchantOverride(mp.getMobileCheckoutMode());
            if (ov != null) {
                block.put("mobileCheckoutMode", ov);
            }
        });
        return block;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
