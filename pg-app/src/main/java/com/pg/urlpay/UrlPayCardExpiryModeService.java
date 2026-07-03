package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class UrlPayCardExpiryModeService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public UrlPayCardExpiryModeService(HqApiConfigRepository hqApiConfigRepository,
                                       MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public String resolveHqDefault() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> UrlPayCardExpiryModeUtil.normalize(
                        c.getUrlPayCardExpiryModeDefault() != null
                                ? c.getUrlPayCardExpiryModeDefault()
                                : UrlPayCardExpiryModeUtil.DROPDOWN))
                .orElse(UrlPayCardExpiryModeUtil.DROPDOWN);
    }

    public String resolveEffective(Long orgUnitId) {
        if (orgUnitId == null) {
            return resolveHqDefault();
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        String stored = mp.map(MerchantProfile::getUrlPayCardExpiryMode)
                .orElse(UrlPayCardExpiryModeUtil.FOLLOW_HQ);
        return UrlPayCardExpiryModeUtil.resolveStored(stored, resolveHqDefault());
    }

    public void putEffectiveIntoMap(Map<String, Object> data, Long orgUnitId) {
        if (data == null) {
            return;
        }
        String effective = resolveEffective(orgUnitId);
        data.put("urlPayCardExpiryMode", effective);
        data.put("urlPayCardExpiryModeEffective", effective);
        data.put("hqUrlPayCardExpiryModeDefault", resolveHqDefault());
        if (orgUnitId != null) {
            merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
                String ui = UrlPayCardExpiryModeUtil.formatMerchantUiValue(mp.getUrlPayCardExpiryMode());
                if (!UrlPayCardExpiryModeUtil.FOLLOW_HQ.equals(ui)) {
                    data.put("urlPayCardExpiryModeMerchantOverride", ui);
                }
            });
        }
    }
}
