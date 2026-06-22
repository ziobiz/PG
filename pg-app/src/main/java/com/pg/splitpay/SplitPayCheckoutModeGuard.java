package com.pg.splitpay;

import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantProfileRepository;
import com.pg.urlpay.UrlPayCheckoutModeUtil;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** API URL 인라인 prepare — 분할결제 모드일 때 1회 결제 prepare 차단 */
@Component
public class SplitPayCheckoutModeGuard {

    private final MerchantProfileRepository merchantProfileRepository;

    public SplitPayCheckoutModeGuard(MerchantProfileRepository merchantProfileRepository) {
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public Optional<Map<String, Object>> denyInlineOneShotPrepare(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty() || !SplitPayMerchantUtil.isEnabled(mp.get())) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", "이 가맹점은 API URL 결제방식이 분할결제입니다. POST /api/pay/split/contracts 로 계약을 생성하세요.");
        out.put("errorCode", "SPLIT_PAY_MODE");
        out.put("urlPayCheckoutMode", UrlPayCheckoutModeUtil.SPLIT_PAY);
        return Optional.of(out);
    }
}
