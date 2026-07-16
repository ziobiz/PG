package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * 일반결제 카드 인증(3DS/NONE3D) — URL결제·API 인라인 동일 실효값.
 */
@Service
public class CardAuthModeService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public CardAuthModeService(HqApiConfigRepository hqApiConfigRepository,
                               MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public String resolveHqDefault() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> CardAuthModeUtil.normalize(
                        c.getCardAuthModeDefault() != null
                                ? c.getCardAuthModeDefault()
                                : CardAuthModeUtil.THREE_DS))
                .orElse(CardAuthModeUtil.THREE_DS);
    }

    public String resolveEffective(Long orgUnitId) {
        if (orgUnitId == null) {
            return resolveHqDefault();
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        String stored = mp.map(MerchantProfile::getCardAuthMode)
                .orElse(CardAuthModeUtil.FOLLOW_HQ);
        return CardAuthModeUtil.resolveStored(stored, resolveHqDefault());
    }

    public void putEffectiveIntoMap(Map<String, Object> data, Long orgUnitId) {
        if (data == null) {
            return;
        }
        String effective = resolveEffective(orgUnitId);
        data.put("cardAuthMode", effective);
        data.put("cardAuthModeEffective", effective);
        data.put("hqCardAuthModeDefault", resolveHqDefault());
        if (orgUnitId != null) {
            merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
                String ui = CardAuthModeUtil.formatMerchantUiValue(mp.getCardAuthMode());
                if (!CardAuthModeUtil.FOLLOW_HQ.equals(ui)) {
                    data.put("cardAuthModeMerchantOverride", ui);
                }
            });
        }
    }
}
