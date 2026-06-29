package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * 본사 URL·API 입력방식 기본값 × 가맹 {@code urlPayInputMode} 오버라이드.
 * <p>가맹 {@link UrlPayInputModeUtil#FOLLOW_HQ} 이면 채널별 본사 기본을 적용하고,
 * 그 외 명시 타입은 가맹 값이 URL·API 모두에 우선합니다.</p>
 */
@Service
public class UrlPayInputModeService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public UrlPayInputModeService(HqApiConfigRepository hqApiConfigRepository,
                                  MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public String resolveHqUrlDefault() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> UrlPayInputModeUtil.normalize(
                        c.getUrlPayInputModeDefault() != null ? c.getUrlPayInputModeDefault() : UrlPayInputModeUtil.GENERAL))
                .orElse(UrlPayInputModeUtil.GENERAL);
    }

    public String resolveHqApiDefault() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> UrlPayInputModeUtil.normalize(
                        c.getApiUrlPayInputModeDefault() != null ? c.getApiUrlPayInputModeDefault() : UrlPayInputModeUtil.TYPE_BA))
                .orElse(UrlPayInputModeUtil.TYPE_BA);
    }

    public UrlPayInputModeUtil.Channel resolveChannel(HttpServletRequest request) {
        if (request == null) {
            return UrlPayInputModeUtil.Channel.URL;
        }
        String entry = request.getParameter("entry");
        if (entry != null && "merchant_api".equalsIgnoreCase(entry.trim())) {
            return UrlPayInputModeUtil.Channel.API;
        }
        return UrlPayInputModeUtil.Channel.URL;
    }

    public String resolveEffective(Long orgUnitId, UrlPayInputModeUtil.Channel channel) {
        String hqUrl = resolveHqUrlDefault();
        String hqApi = resolveHqApiDefault();
        if (orgUnitId == null) {
            return UrlPayInputModeUtil.resolve(null, hqUrl, hqApi, channel);
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        String stored = mp.map(MerchantProfile::getUrlPayInputMode).orElse(UrlPayInputModeUtil.GENERAL);
        return UrlPayInputModeUtil.resolve(stored, hqUrl, hqApi, channel);
    }

    public String resolveEffective(Long orgUnitId, HttpServletRequest request) {
        return resolveEffective(orgUnitId, resolveChannel(request));
    }

    public void putEffectiveIntoMap(Map<String, Object> data, Long orgUnitId, HttpServletRequest request) {
        if (data == null) {
            return;
        }
        UrlPayInputModeUtil.Channel ch = resolveChannel(request);
        String effective = resolveEffective(orgUnitId, ch);
        data.put("urlPayInputMode", effective);
        data.put("urlPayInputModeEffective", effective);
        data.put("urlPayInputModeChannel", ch.name());
        data.put("hqUrlPayInputModeDefault", resolveHqUrlDefault());
        data.put("hqApiUrlPayInputModeDefault", resolveHqApiDefault());
        if (orgUnitId != null) {
            merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
                String ui = UrlPayInputModeUtil.formatMerchantUiValue(mp.getUrlPayInputMode());
                if (!UrlPayInputModeUtil.FOLLOW_HQ.equals(ui)) {
                    data.put("urlPayInputModeMerchantOverride", ui);
                }
            });
        }
    }
}
