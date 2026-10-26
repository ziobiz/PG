package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.repository.HqApiConfigRepository;
import com.pg.util.CardBrandScopeUtil;
import com.pg.util.CurrencyScopeUtil;
import com.pg.util.MultiPgRoutingModeUtil;
import com.pg.util.QrWalletScopeUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 가맹 URL 결제 운영 PG 선택 — 본사 멀티 PG 스위치·라우팅 차원(브랜드/통화/혼합)·힌트 기반.
 * <p>본사 사용불가브랜드가 등록된 PG로는 해당 브랜드를 라우팅하지 않습니다(행 스코프와 AND).
 */
@Service
public class MerchantPgBindingRouterService {

    public record RoutingHint(String cardBrand, String currency, boolean repayScope, String qrWallet) {
        public RoutingHint(String cardBrand, String currency, boolean repayScope) {
            this(cardBrand, currency, repayScope, "");
        }

        public static RoutingHint standard(String cardBrand, String currency) {
            return new RoutingHint(cardBrand, currency, false, "");
        }

        public static RoutingHint repay(String cardBrand, String currency) {
            return new RoutingHint(cardBrand, currency, true, "");
        }

        /** QR 지갑 결제. 카드 브랜드는 보지 않고 지갑·통화로 행을 고릅니다. */
        public static RoutingHint qr(String qrWallet, String currency) {
            return new RoutingHint("", currency, false, qrWallet != null ? qrWallet : "");
        }

        public boolean hasQrWallet() {
            return qrWallet != null && !qrWallet.isBlank();
        }
    }

    private final HqApiConfigRepository hqApiConfigRepository;
    private final ChillPayService chillPayService;
    private final PayCardPolicyService payCardPolicyService;

    public MerchantPgBindingRouterService(HqApiConfigRepository hqApiConfigRepository,
                                          ChillPayService chillPayService,
                                          @Lazy PayCardPolicyService payCardPolicyService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.chillPayService = chillPayService;
        this.payCardPolicyService = payCardPolicyService;
    }

    private Optional<HqApiConfig> hqConfig() {
        return hqApiConfigRepository.findAll().stream().findFirst();
    }

    public boolean isMultiPgRoutingEnabled() {
        return hqConfig()
                .map(HqApiConfig::getMultiPgRoutingEnabledYn)
                .map(v -> !"N".equalsIgnoreCase(v.trim()))
                .orElse(true);
    }

    public String resolveMultiPgRoutingMode() {
        return hqConfig()
                .map(HqApiConfig::getMultiPgRoutingMode)
                .map(MultiPgRoutingModeUtil::normalize)
                .orElse(MultiPgRoutingModeUtil.BRAND_AND_CURRENCY);
    }

    public Optional<MerchantPgBinding> resolveOperationalBinding(Long orgUnitId, RoutingHint hint) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        boolean repay = hint != null && hint.repayScope();
        List<MerchantPgBinding> candidates = repay
                ? chillPayService.listOperationalWebBindingsForUrlPayRepay(orgUnitId)
                : chillPayService.listOperationalWebBindingsForUrlPay(orgUnitId);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (hint != null && hint.hasQrWallet()) {
            return resolveQrBinding(candidates, hint);
        }
        List<MerchantPgBinding> cardCandidates = candidates.stream()
                .filter(b -> !CardBrandScopeUtil.isNone(b.getCardBrandScope()))
                .collect(Collectors.toList());
        if (cardCandidates.isEmpty()) {
            return Optional.empty();
        }
        if (!isMultiPgRoutingEnabled() || hint == null) {
            return Optional.of(cardCandidates.get(0));
        }
        String mode = resolveMultiPgRoutingMode();
        String brandLetter = CardBrandScopeUtil.toScopeLetter(hint.cardBrand());
        String currency = hint.currency() != null ? hint.currency().trim().toUpperCase(Locale.ROOT) : "";
        List<MerchantPgBinding> filtered = cardCandidates.stream()
                .filter(b -> matchesBindingForMode(b, mode, brandLetter, currency))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        return filtered.stream().max(routingComparator(mode));
    }

    public String resolveOperationalPgCd(Long orgUnitId, RoutingHint hint) {
        return resolveOperationalBinding(orgUnitId, hint)
                .map(MerchantPgBinding::getPgCd)
                .map(String::trim)
                .orElse("");
    }

    public String resolveOperationalPgCd(Long orgUnitId, String cardBrand, String currency, boolean repay) {
        RoutingHint hint = repay ? RoutingHint.repay(cardBrand, currency) : RoutingHint.standard(cardBrand, currency);
        return resolveOperationalPgCd(orgUnitId, hint);
    }

    /** checkout-context·관리 UI용 운영 PG 라우트 요약 */
    public List<Map<String, Object>> listOperationalRouteSummaries(Long orgUnitId, boolean repay) {
        List<MerchantPgBinding> list = repay
                ? chillPayService.listOperationalWebBindingsForUrlPayRepay(orgUnitId)
                : chillPayService.listOperationalWebBindingsForUrlPay(orgUnitId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MerchantPgBinding b : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pgCd", b.getPgCd());
            row.put("cardBrandScope", b.getCardBrandScope() != null ? b.getCardBrandScope() : "ALL");
            row.put("currencyScope", b.getCurrencyScope() != null ? b.getCurrencyScope() : "ALL");
            row.put("qrWalletScope", b.getQrWalletScope() != null ? b.getQrWalletScope() : "");
            row.put("urlPayPricingMode", b.getUrlPayPricingMode());
            row.put("sortOrder", b.getSortOrder());
            out.add(row);
        }
        return out;
    }

    /** 운영 URL 결제 행 중 카드를 받는 행이 있는지. QR 전용(카드 없음) 행은 제외합니다. */
    public boolean hasOperationalCardMeans(Long orgUnitId) {
        return chillPayService.listOperationalWebBindingsForUrlPay(orgUnitId).stream()
                .anyMatch(b -> !CardBrandScopeUtil.isNone(b.getCardBrandScope()));
    }

    /** 운영 URL 결제 행에 설정된 QR 지갑. 카탈로그 순. */
    public List<String> listOperationalQrWallets(Long orgUnitId) {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        for (MerchantPgBinding b : chillPayService.listOperationalWebBindingsForUrlPay(orgUnitId)) {
            String scope = QrWalletScopeUtil.normalize(b.getQrWalletScope());
            if (scope.isEmpty()) {
                continue;
            }
            for (String part : scope.split(",")) {
                if (!part.isBlank()) {
                    found.add(part);
                }
            }
        }
        return new ArrayList<>(found);
    }

    private Optional<MerchantPgBinding> resolveQrBinding(List<MerchantPgBinding> candidates, RoutingHint hint) {
        String wallet = QrWalletScopeUtil.canonicalWallet(hint.qrWallet());
        if (wallet.isEmpty()) {
            return Optional.empty();
        }
        String currency = hint.currency() != null ? hint.currency().trim().toUpperCase(Locale.ROOT) : "";
        List<MerchantPgBinding> filtered = candidates.stream()
                .filter(b -> QrWalletScopeUtil.matches(b.getQrWalletScope(), wallet))
                .filter(b -> currency.isEmpty() || CurrencyScopeUtil.matchesScope(b.getCurrencyScope(), currency))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        return filtered.stream().max(Comparator
                .comparingInt((MerchantPgBinding b) -> QrWalletScopeUtil.specificityScore(b.getQrWalletScope()))
                .thenComparingInt((MerchantPgBinding b) -> CurrencyScopeUtil.specificityScore(b.getCurrencyScope()))
                .thenComparing(b -> b.getSortOrder() != null ? -b.getSortOrder() : Integer.MIN_VALUE)
                .thenComparing(b -> b.getId() != null ? -b.getId() : 0L));
    }

    private boolean matchesBindingForMode(MerchantPgBinding binding, String mode, String brandLetter, String currency) {
        if (CardBrandScopeUtil.isNone(binding.getCardBrandScope())) {
            return false;
        }
        boolean brandOk = true;
        boolean currencyOk = true;
        if (MultiPgRoutingModeUtil.BRAND.equals(mode) || MultiPgRoutingModeUtil.BRAND_AND_CURRENCY.equals(mode)) {
            if (!brandLetter.isEmpty()) {
                brandOk = CardBrandScopeUtil.matchesScope(binding.getCardBrandScope(), brandLetter);
                if (brandOk && payCardPolicyService != null && binding.getPgCd() != null) {
                    /* 본사 사용불가브랜드: 해당 PG에는 이 브랜드를 보내지 않음 */
                    brandOk = !payCardPolicyService.isBrandBlockedByHq(binding.getPgCd(), brandLetter);
                }
            }
        }
        if (MultiPgRoutingModeUtil.CURRENCY.equals(mode) || MultiPgRoutingModeUtil.BRAND_AND_CURRENCY.equals(mode)) {
            if (!currency.isEmpty()) {
                currencyOk = CurrencyScopeUtil.matchesScope(binding.getCurrencyScope(), currency);
            }
        }
        return brandOk && currencyOk;
    }

    private Comparator<MerchantPgBinding> routingComparator(String mode) {
        return Comparator
                .comparingInt((MerchantPgBinding b) -> routingSpecificityScore(b, mode))
                .thenComparing(b -> b.getSortOrder() != null ? -b.getSortOrder() : Integer.MIN_VALUE)
                .thenComparing(b -> b.getId() != null ? -b.getId() : 0L);
    }

    private int routingSpecificityScore(MerchantPgBinding binding, String mode) {
        int score = 0;
        if (MultiPgRoutingModeUtil.BRAND.equals(mode) || MultiPgRoutingModeUtil.BRAND_AND_CURRENCY.equals(mode)) {
            score += CardBrandScopeUtil.specificityScore(binding.getCardBrandScope());
        }
        if (MultiPgRoutingModeUtil.CURRENCY.equals(mode) || MultiPgRoutingModeUtil.BRAND_AND_CURRENCY.equals(mode)) {
            score += CurrencyScopeUtil.specificityScore(binding.getCurrencyScope());
        }
        return score;
    }
}
