package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * URL 결제(ChillPay·JPAY 공통) 체크아웃 실결제 통화 — 일반형 규칙.
 * <p>
 * 우선순위: 가맹 {@code base_currency} → 상위 총판(MASTER_DIST) → 상위 본사(REGIONAL) → 요청 body {@code currency}
 * → 최종 폴백({@link #DEFAULT_FALLBACK}).
 */
@Service
public class UrlPayCheckoutCurrencyService {

    /** ChillPay URL 일반형과 동일한 최종 폴백 */
    public static final String DEFAULT_FALLBACK = "JPY";

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public UrlPayCheckoutCurrencyService(OrgUnitRepository orgUnitRepository,
                                         MerchantProfileRepository merchantProfileRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    /**
     * 조직 체인만으로 통화를 해석합니다(요청 body 미사용).
     */
    public Optional<String> resolveFromOrgChain(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return Optional.empty();
        }
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

    /**
     * URL 결제 PG 송부 통화. {@code bodyCurrency}는 조직 체인이 모두 비었을 때만 사용됩니다.
     */
    public String resolveCheckoutCurrency(Long merchantOrgUnitId, String bodyCurrency) {
        return resolveFromOrgChain(merchantOrgUnitId)
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .orElse(normalizeBodyCurrency(bodyCurrency).orElse(DEFAULT_FALLBACK));
    }

    private Optional<String> normalizeBodyCurrency(String bodyCurrency) {
        if (bodyCurrency == null || bodyCurrency.isBlank()) {
            return Optional.empty();
        }
        String u = bodyCurrency.trim().toUpperCase(Locale.ROOT);
        return u.isEmpty() ? Optional.empty() : Optional.of(u);
    }

    /** 가맹점 프로필 {@code base_currency} 첫 토큰(본사 다통화 comma 구분 대응). */
    private Optional<String> firstProfileBaseCurrencyToken(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(MerchantProfile::getBaseCurrency)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.split(",")[0].trim())
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT));
    }
}
