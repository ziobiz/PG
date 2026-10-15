package com.pg.crypto;

import com.pg.entity.*;
import com.pg.integration.pg.PgVendor;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.repository.*;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.CheckoutHeaderLogoResolver;
import com.pg.urlpay.CheckoutBuyerContactUtil;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.service.UrlPayCardCopyService;
import com.pg.urlpay.UrlPayCheckoutDisplayPolicyService;
import com.pg.util.OrgUseYnUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/** 크립토 입금 체크아웃 prepare / session / status / confirm. 청구·수수료·한도 기준통화는 USD. 표시통화는 가맹 설정. */
@Service
public class CryptoCheckoutService {

    /** 크립토 견적·수수료·한도·정산 기준 통화 (USDT/USDC 액면 = USD). */
    public static final String QUOTE_CURRENCY = "USD";

    private static final SecureRandom RND = new SecureRandom();
    private static final int EXPIRE_MINUTES = 60;

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantCryptoConfigRepository configRepository;
    private final CryptoDepositTxnRepository depositTxnRepository;
    private final CryptoCustodyAddressRepository custodyAddressRepository;
    private final CryptoCatalogService catalogService;
    private final CryptoPayoutService payoutService;
    private final CryptoCommissionChargeService commissionChargeService;
    private final CryptoNotiMiddlewareMirrorService cryptoNotiMiddlewareMirrorService;
    private final CryptoReserveResolver reserveResolver;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;
    private final UrlPayCheckoutDisplayPolicyService displayPolicy;
    private final UrlPayCardCopyService urlPayCardCopyService;
    private final CryptoFxService cryptoFxService;

    public CryptoCheckoutService(OrgUnitRepository orgUnitRepository,
                                 MerchantProfileRepository merchantProfileRepository,
                                 MerchantCryptoConfigRepository configRepository,
                                 CryptoDepositTxnRepository depositTxnRepository,
                                 CryptoCustodyAddressRepository custodyAddressRepository,
                                 CryptoCatalogService catalogService,
                                 CryptoPayoutService payoutService,
                                 CryptoCommissionChargeService commissionChargeService,
                                 CryptoNotiMiddlewareMirrorService cryptoNotiMiddlewareMirrorService,
                                 CryptoReserveResolver reserveResolver,
                                 CheckoutHeaderLogoResolver checkoutHeaderLogoResolver,
                                 UrlPayCheckoutDisplayPolicyService displayPolicy,
                                 UrlPayCardCopyService urlPayCardCopyService,
                                 CryptoFxService cryptoFxService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.configRepository = configRepository;
        this.depositTxnRepository = depositTxnRepository;
        this.custodyAddressRepository = custodyAddressRepository;
        this.catalogService = catalogService;
        this.payoutService = payoutService;
        this.commissionChargeService = commissionChargeService;
        this.cryptoNotiMiddlewareMirrorService = cryptoNotiMiddlewareMirrorService;
        this.reserveResolver = reserveResolver;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
        this.displayPolicy = displayPolicy;
        this.urlPayCardCopyService = urlPayCardCopyService;
        this.cryptoFxService = cryptoFxService;
    }

    /**
     * 크립토 활성: 가맹 크립토 useYn=Y + 업체등록 사용여부 Y.
     * CARD/CRYPTO/BOTH 는 게이트에 쓰지 않음(카드는 URL·PG 설정 우선).
     */
    public Optional<MerchantCryptoConfig> activeConfig(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        if (!isMerchantServiceOpen(orgUnitId)) {
            return Optional.empty();
        }
        return configRepository.findById(orgUnitId)
                .filter(c -> "Y".equalsIgnoreCase(c.getUseYn()));
    }

    /** 업체등록 사용(Y)만 결제 가능. 미사용(N)·영구정지(S) 차단. */
    public boolean isMerchantServiceOpen(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> OrgUseYnUtil.isServiceAllowed(mp.getUseYn()))
                .orElse(false);
    }

    public boolean cryptoMeansAllowed(Long orgUnitId) {
        return activeConfig(orgUnitId).isPresent();
    }

    @Transactional
    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null || ou.getOrgLevel() != OrgLevel.MERCHANT) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (!isMerchantServiceOpen(orgUnitId)) {
            return fail("가맹점이 미사용 또는 영구정지 상태입니다.", "MERCHANT_INACTIVE");
        }
        MerchantCryptoConfig cfg = activeConfig(orgUnitId).orElse(null);
        if (cfg == null) {
            return fail("크립토 결제가 활성화되지 않았습니다.", "CRYPTO_DISABLED");
        }

        String orderNo = str(body, "orderNo");
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal displayAmount = parseAmount(body != null ? body.get("amount") : null);
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String displayCcy = cryptoFxService.normalizeDisplayCurrency(
                firstNonBlank(str(body, "displayCurrency", "fiatCurrency"),
                        cfg.getDisplayCurrency() != null ? cfg.getDisplayCurrency() : QUOTE_CURRENCY));
        /* body에 quoteCurrency=USD 명시 시 표시=청구 동일 취급(레거시). USDT/USDC 힌트는 자산으로. */
        String fiatHint = str(body, "fiatCurrency", "quoteCurrency", "settleCurrency").toUpperCase(Locale.ROOT);
        if (!fiatHint.isBlank() && Set.of("USDT", "USDC", "EURC").contains(fiatHint)) {
            /* 자산 힌트 — 표시통화는 가맹 설정 유지 */
        } else if (!fiatHint.isBlank() && CryptoFxService.DISPLAY_CURRENCIES.contains(fiatHint)) {
            displayCcy = fiatHint;
        }
        Optional<BigDecimal> quoteOpt = cryptoFxService.toQuoteUsd(displayAmount, displayCcy);
        if (quoteOpt.isEmpty()) {
            return fail("표시통화→USD 환율을 가져올 수 없습니다.", "FX_UNAVAILABLE");
        }
        BigDecimal amount = quoteOpt.get();

        if (cfg.getAssetsCsv() == null || cfg.getAssetsCsv().isBlank()) {
            return fail("허용 자산이 없습니다. 자산을 활성으로 설정하세요.", "ASSET_NOT_ALLOWED");
        }
        String asset = str(body, "assetCode", "asset").toUpperCase(Locale.ROOT);
        if (asset.isBlank()) {
            String cur = str(body, "currency").toUpperCase(Locale.ROOT);
            if (Set.of("USDT", "USDC", "EURC").contains(cur)) {
                asset = cur;
            }
        }
        if (asset.isBlank() && Set.of("USDT", "USDC", "EURC").contains(fiatHint)) {
            asset = fiatHint;
        }
        if (asset.isBlank()) {
            asset = firstCsv(cfg.getAssetsCsv(), "");
        }
        if (asset.isBlank()) {
            return fail("허용 자산이 없습니다. 자산을 활성으로 설정하세요.", "ASSET_NOT_ALLOWED");
        }
        String network = str(body, "networkCode", "network").toUpperCase(Locale.ROOT);
        if (!csvContains(cfg.getAssetsCsv(), asset)) {
            return fail("가맹점에서 허용하지 않는 자산입니다.", "ASSET_NOT_ALLOWED");
        }
        if (network.isBlank()) {
            network = resolvePreferredNetwork(asset, cfg);
        }
        if (network.isBlank()) {
            return fail("허용 네트워크가 없거나, 해당 자산의 본사 수탁 주소가 없습니다. 본사 크립토설정을 확인하세요.",
                    "CUSTODY_MISSING");
        }
        if (!csvContains(cfg.getNetworksCsv(), network)) {
            return fail("가맹점에서 허용하지 않는 네트워크입니다.", "NETWORK_NOT_ALLOWED");
        }
        if (!catalogService.isValidPair(asset, network)) {
            return fail("자산·네트워크 조합이 올바르지 않습니다.", "INVALID_ASSET_NETWORK");
        }

        String custody = resolveCustody(network, asset);
        if (custody == null || custody.isBlank()) {
            /* 요청 네트워크에 수탁이 없으면 허용 목록 중 수탁 있는 네트워크로 자동 전환 */
            String fallbackNet = resolvePreferredNetwork(asset, cfg);
            if (fallbackNet != null && !fallbackNet.isBlank() && !fallbackNet.equalsIgnoreCase(network)) {
                String fbCustody = resolveCustody(fallbackNet, asset);
                if (fbCustody != null && !fbCustody.isBlank()) {
                    network = fallbackNet;
                    custody = fbCustody;
                }
            }
        }
        if (custody == null || custody.isBlank()) {
            return fail("수탁 주소가 설정되지 않았습니다. 본사 크립토설정에서 "
                            + asset + " × " + network + " 대표수탁(사용Y)을 등록하세요.",
                    "CUSTODY_MISSING");
        }

        Optional<CryptoDepositTxn> existing = depositTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo);
        if (existing.isPresent() && CryptoDepositTxn.ST_CONFIRMED.equals(existing.get().getStatus())) {
            return fail("이미 확정된 주문번호입니다.", "DUPLICATE_ORDER");
        }

        BuyerContact buyer = extractBuyerContact(body);
        Map<String, Object> buyerFail = validateBuyerContact(buyer, body, orgUnitId);
        if (buyerFail != null) {
            return buyerFail;
        }
        if (!CheckoutBuyerContactUtil.isYn(displayPolicy.effectiveShippingAddressUseYn(orgUnitId))) {
            buyer = new BuyerContact(buyer.email(), buyer.name(), buyer.phone(), buyer.countryIso2(), null);
        }

        BigDecimal assetAmount = cryptoFxService.toAssetAmount(amount, asset);

        CryptoDepositTxn txn = existing.orElseGet(CryptoDepositTxn::new);
        txn.setOrgUnitId(orgUnitId);
        txn.setCompId(ou.getCode());
        txn.setOrderNo(orderNo);
        txn.setAmount(amount);
        txn.setDisplayAmount(displayAmount);
        txn.setDisplayCurrency(displayCcy);
        txn.setAssetAmount(assetAmount);
        txn.setAssetCode(asset);
        txn.setNetworkCode(network);
        txn.setProductName(firstNonBlank(str(body, "productName"), str(body, "item")));
        applyBuyerToTxn(txn, buyer);
        txn.setCustodyAddress(custody);
        txn.setSessionToken("cr_" + hex(24));
        txn.setStatus(CryptoDepositTxn.ST_AWAITING);
        txn.setSettleMode(nzMode(cfg.getSettleMode(), MerchantCryptoConfig.SETTLE_SCHEDULED));
        txn.setPayoutMode(nzMode(cfg.getPayoutMode(), MerchantCryptoConfig.PAYOUT_MANUAL));
        txn.setDepositTxHash(null);
        txn.setConfirmedAt(null);
        txn.setExpireAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        depositTxnRepository.save(txn);

        String base = publicBase(request);
        String payUrl = NeutralCheckoutRoute.buildCryptoPayUrl(base, ou.getCode(), txn.getSessionToken(),
                str(body, "lang", "langCode"), false);

        Map<String, Object> data = publicTxn(txn, cfg);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("payUrl", payUrl);
        data.put("payMeans", "CRYPTO");
        data.put("cryptoEnabled", true);
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("catalog", checkoutCatalog(cfg));
        data.put("wallets", catalogService.wallets(network));
        checkoutHeaderLogoResolver.applyCryptoToCheckoutMap(data, orgUnitId);
        displayPolicy.putEffectiveYnIntoMap(data, orgUnitId);
        putHeadquartersFavicon(data);
        return ok(data);
    }

    /**
     * 자산·네트워크 선택 확정 — 수탁 주소 갱신 후 입금 안내 단계로.
     */
    @Transactional
    public Map<String, Object> bindSelection(String sessionToken, Map<String, Object> body) {
        CryptoDepositTxn txn = depositTxnRepository.findBySessionToken(nz(sessionToken)).orElse(null);
        if (txn == null) {
            return fail("세션을 찾을 수 없습니다.", "SESSION_NOT_FOUND");
        }
        expireIfNeeded(txn);
        if (CryptoDepositTxn.ST_EXPIRED.equals(txn.getStatus())
                || CryptoDepositTxn.ST_CONFIRMED.equals(txn.getStatus())
                || CryptoDepositTxn.ST_FAILED.equals(txn.getStatus())) {
            return fail("변경할 수 없는 상태입니다.", "INVALID_STATUS");
        }
        MerchantCryptoConfig cfg = configRepository.findById(txn.getOrgUnitId()).orElse(null);
        if (cfg == null || !"Y".equalsIgnoreCase(cfg.getUseYn())) {
            return fail("크립토 결제가 활성화되지 않았습니다.", "CRYPTO_DISABLED");
        }
        String asset = str(body, "assetCode", "asset").toUpperCase(Locale.ROOT);
        String network = str(body, "networkCode", "network").toUpperCase(Locale.ROOT);
        if (asset.isBlank()) {
            asset = txn.getAssetCode();
        }
        if (!csvContains(cfg.getAssetsCsv(), asset)) {
            return fail("가맹점에서 허용하지 않는 자산입니다.", "ASSET_NOT_ALLOWED");
        }
        if (network.isBlank()) {
            network = nz(txn.getNetworkCode()).toUpperCase(Locale.ROOT);
        }
        String existingCustody = network.isBlank() ? null : resolveCustody(network, asset);
        if (network.isBlank() || existingCustody == null || existingCustody.isBlank()) {
            String preferred = resolvePreferredNetwork(asset, cfg);
            if (preferred != null && !preferred.isBlank()) {
                network = preferred;
            }
        }
        if (network.isBlank()) {
            return fail("허용 네트워크가 없거나, 해당 자산의 본사 수탁 주소가 없습니다. 본사 크립토설정을 확인하세요.",
                    "CUSTODY_MISSING");
        }
        if (!csvContains(cfg.getNetworksCsv(), network)) {
            return fail("가맹점에서 허용하지 않는 네트워크입니다.", "NETWORK_NOT_ALLOWED");
        }
        if (!catalogService.isValidPair(asset, network)) {
            return fail("자산·네트워크 조합이 올바르지 않습니다.", "INVALID_ASSET_NETWORK");
        }
        String custody = resolveCustody(network, asset);
        if (custody == null || custody.isBlank()) {
            return fail("수탁 주소가 설정되지 않았습니다. 본사 크립토설정에서 "
                            + asset + " × " + network + " 대표수탁(사용Y)을 등록하세요.",
                    "CUSTODY_MISSING");
        }
        txn.setAssetCode(asset);
        txn.setNetworkCode(network);
        txn.setAssetAmount(cryptoFxService.toAssetAmount(txn.getAmount(), asset));
        txn.setCustodyAddress(custody);
        depositTxnRepository.save(txn);

        Map<String, Object> data = publicTxn(txn, cfg);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("cryptoEnabled", true);
        data.put("catalog", checkoutCatalog(cfg));
        data.put("wallets", catalogService.wallets(network));
        checkoutHeaderLogoResolver.applyCryptoToCheckoutMap(data, txn.getOrgUnitId());
        displayPolicy.putEffectiveYnIntoMap(data, txn.getOrgUnitId());
        putHeadquartersFavicon(data);
        return ok(data);
    }

    public Map<String, Object> session(String sessionToken) {
        CryptoDepositTxn txn = depositTxnRepository.findBySessionToken(nz(sessionToken))
                .orElse(null);
        if (txn == null) {
            return fail("세션을 찾을 수 없습니다.", "SESSION_NOT_FOUND");
        }
        expireIfNeeded(txn);
        MerchantCryptoConfig cfg = configRepository.findById(txn.getOrgUnitId()).orElse(null);
        Map<String, Object> data = publicTxn(txn, cfg);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("cryptoEnabled", cfg != null && "Y".equalsIgnoreCase(cfg.getUseYn())
                && isMerchantServiceOpen(txn.getOrgUnitId()));
        if (cfg != null) {
            data.put("catalog", checkoutCatalog(cfg));
        } else {
            data.put("catalog", catalogService.publicCatalog(List.of(txn.getAssetCode()), List.of(txn.getNetworkCode())));
        }
        data.put("wallets", catalogService.wallets(txn.getNetworkCode()));
        checkoutHeaderLogoResolver.applyCryptoToCheckoutMap(data, txn.getOrgUnitId());
        displayPolicy.putEffectiveYnIntoMap(data, txn.getOrgUnitId());
        putHeadquartersFavicon(data);
        return ok(data);
    }

    /**
     * 수단선택·세션 전 — 상단 로고·안내문구·구매자필드 Y/N(카드와 동일 정책).
     * @param channel {@code unified} 이면 통합 안내문구, 그 외 크립토 안내문구
     */
    public Map<String, Object> checkoutContext(Long orgUnitId, String channel) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("cryptoEnabled", cryptoMeansAllowed(orgUnitId));
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("displayCurrency", QUOTE_CURRENCY);
        if (orgUnitId != null) {
            orgUnitRepository.findById(orgUnitId).ifPresent(ou -> data.put("compId", ou.getCode()));
            configRepository.findById(orgUnitId).ifPresent(cfg ->
                    data.put("displayCurrency",
                            cryptoFxService.normalizeDisplayCurrency(cfg.getDisplayCurrency())));
            if (channel != null && "unified".equalsIgnoreCase(channel.trim())) {
                checkoutHeaderLogoResolver.applyUnifiedToCheckoutMap(data, orgUnitId);
            } else {
                checkoutHeaderLogoResolver.applyCryptoToCheckoutMap(data, orgUnitId);
            }
            displayPolicy.putEffectiveYnIntoMap(data, orgUnitId);
            displayPolicy.putEffectiveDefaultProductIntoMap(data, orgUnitId, Optional.empty());
        }
        putHeadquartersFavicon(data);
        return ok(data);
    }

    /** 시작폼 실시간 견적: 표시금액 → 청구(USD) → 자산 송금액. */
    public Map<String, Object> estimateFx(Long orgUnitId, Map<String, Object> body) {
        MerchantCryptoConfig cfg = orgUnitId != null
                ? configRepository.findById(orgUnitId).orElse(null) : null;
        String displayCcy = cryptoFxService.normalizeDisplayCurrency(
                firstNonBlank(str(body, "displayCurrency"),
                        cfg != null && cfg.getDisplayCurrency() != null ? cfg.getDisplayCurrency() : QUOTE_CURRENCY));
        BigDecimal displayAmount = parseAmount(body != null ? body.get("amount") : null);
        String asset = str(body, "assetCode", "asset").toUpperCase(Locale.ROOT);
        if (asset.isBlank() && cfg != null) {
            asset = firstCsv(cfg.getAssetsCsv(), "USDC");
        }
        Map<String, Object> est = cryptoFxService.estimate(displayAmount, displayCcy, asset);
        if (!Boolean.TRUE.equals(est.get("ok"))) {
            return fail("환율을 계산할 수 없습니다.", String.valueOf(est.getOrDefault("error", "FX_UNAVAILABLE")));
        }
        Map<String, Object> data = new LinkedHashMap<>(est);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        return ok(data);
    }

    /** 카드 URL결제와 동일 — 총본사 브랜드 파비콘 */
    private void putHeadquartersFavicon(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        Map<String, Object> withFav = urlPayCardCopyService.mergeHeadquartersBrandFavicon(data);
        Object fav = withFav.get(UrlPayCardCopyService.KEY_FAVICON_URL);
        if (fav != null && !fav.toString().isBlank()) {
            data.put(UrlPayCardCopyService.KEY_FAVICON_URL, fav.toString().trim());
        }
    }

    public Map<String, Object> status(String sessionToken, String orderNo, Long orgUnitId) {
        Optional<CryptoDepositTxn> opt = Optional.empty();
        if (sessionToken != null && !sessionToken.isBlank()) {
            opt = depositTxnRepository.findBySessionToken(sessionToken.trim());
        } else if (orgUnitId != null && orderNo != null && !orderNo.isBlank()) {
            opt = depositTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo.trim());
        }
        if (opt.isEmpty()) {
            return fail("거래를 찾을 수 없습니다.", "NOT_FOUND");
        }
        CryptoDepositTxn txn = opt.get();
        expireIfNeeded(txn);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", txn.getOrderNo());
        data.put("status", txn.getStatus());
        data.put("assetCode", txn.getAssetCode());
        data.put("networkCode", txn.getNetworkCode());
        data.put("amount", txn.getAmount());
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("depositTxHash", txn.getDepositTxHash());
        data.put("confirmedAt", txn.getConfirmedAt());
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        return ok(data);
    }

    /** 관리자·감지 워커: 입금 확정 후 출금 큐 생성. */
    @Transactional
    public Map<String, Object> confirmDeposit(Long depositId, String depositTxHash) {
        CryptoDepositTxn txn = depositTxnRepository.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("입금 건을 찾을 수 없습니다."));
        if (CryptoDepositTxn.ST_CONFIRMED.equals(txn.getStatus())) {
            return Map.of("success", true, "status", txn.getStatus(), "id", txn.getId());
        }
        if (depositTxHash == null || depositTxHash.isBlank()) {
            throw new IllegalArgumentException("depositTxHash가 필요합니다.");
        }
        txn.setDepositTxHash(depositTxHash.trim());
        txn.setStatus(CryptoDepositTxn.ST_CONFIRMED);
        txn.setConfirmedAt(LocalDateTime.now());
        depositTxnRepository.save(txn);

        /* 확정 입금 = 카드 승인 성공과 동일 — CRYPTO 수수료 과금 */
        BigDecimal feeCharged = commissionChargeService.chargeOnConfirmedDeposit(txn);

        MerchantCryptoConfig cfg = configRepository.findById(txn.getOrgUnitId()).orElse(null);
        String seller = extractSellerAddress(cfg, txn.getNetworkCode(), txn.getAssetCode());
        payoutService.enqueueAfterConfirm(txn, cfg, seller);

        /* ICOPAY → NOTI → 가맹 Callback (카드와 동일 경로; 결제통보 URL 직통 아님) */
        cryptoNotiMiddlewareMirrorService.scheduleConfirmMirrorAfterCommit(txn);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("status", txn.getStatus());
        out.put("id", txn.getId());
        out.put("feeAmt", feeCharged);
        out.put("feeChargedYn", txn.getFeeChargedYn());
        out.put("payTrnId", txn.getPayTrnId());
        out.put("notiMirrorScheduled", true);
        return out;
    }

    public Map<String, Object> meansForMerchant(Long orgUnitId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("quoteCurrency", QUOTE_CURRENCY);
        Optional<MerchantCryptoConfig> cfg = activeConfig(orgUnitId);
        if (cfg.isEmpty()) {
            m.put("cryptoEnabled", false);
            return m;
        }
        m.put("cryptoEnabled", true);
        m.put("settleMode", cfg.get().getSettleMode());
        m.put("payoutMode", cfg.get().getPayoutMode());
        String mid = orgUnitRepository.findById(orgUnitId).map(OrgUnit::getCode).orElse("");
        CryptoReserveResolver.Effective eff = reserveResolver.resolve(cfg.get(), mid);
        m.put("holdDays", eff.effectiveHoldDays());
        m.put("reservePct", eff.effectiveReservePct());
        m.put("reserveFollowHq", eff.followHq() ? "Y" : "N");
        m.put("assets", cfg.get().getAssetsCsv());
        m.put("networks", cfg.get().getNetworksCsv());
        return m;
    }

    private String resolveCustody(String network, String asset) {
        /* 입금은 자산×네트워크의 primary_yn=Y 대표 수탁 우선 */
        return custodyAddressRepository
                .findFirstByNetworkCodeAndAssetCodeAndUseYnAndPrimaryYnOrderByIdAsc(network, asset, "Y", "Y")
                .or(() -> custodyAddressRepository
                        .findFirstByNetworkCodeAndAssetCodeAndUseYnOrderByIdAsc(network, asset, "Y"))
                .or(() -> custodyAddressRepository
                        .findFirstByNetworkCodeAndAssetCodeIsNullAndUseYnAndPrimaryYnOrderByIdAsc(network, "Y", "Y"))
                .or(() -> custodyAddressRepository
                        .findFirstByNetworkCodeAndAssetCodeIsNullAndUseYnOrderByIdAsc(network, "Y"))
                .map(CryptoCustodyAddress::getAddress)
                .orElse(null);
    }

    /**
     * 구매자 결제창 카탈로그 — 가맹 허용 ∩ 카탈로그 페어 ∩ <b>본사 수탁(사용Y) 있는</b> 자산×네트워크만.
     * 수탁 없는 BASE 등이 같이 노출되면 안 된다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> checkoutCatalog(MerchantCryptoConfig cfg) {
        Map<String, Object> cat = catalogService.publicCatalog(
                Arrays.asList(cfg.getAssetsCsv().split(",")),
                Arrays.asList(cfg.getNetworksCsv().split(",")));
        List<Map<String, Object>> pairsIn = (List<Map<String, Object>>) cat.getOrDefault("pairs", List.of());
        List<Map<String, Object>> pairsOut = new ArrayList<>();
        Map<String, List<String>> custodyByAsset = new LinkedHashMap<>();
        Set<String> netCodes = new LinkedHashSet<>();
        Set<String> assetCodes = new LinkedHashSet<>();
        for (Map<String, Object> p : pairsIn) {
            if (p == null) {
                continue;
            }
            String a = String.valueOf(p.getOrDefault("assetCode", "")).trim().toUpperCase(Locale.ROOT);
            String n = String.valueOf(p.getOrDefault("networkCode", "")).trim().toUpperCase(Locale.ROOT);
            if (a.isEmpty() || n.isEmpty()) {
                continue;
            }
            String c = resolveCustody(n, a);
            if (c == null || c.isBlank()) {
                continue;
            }
            pairsOut.add(p);
            assetCodes.add(a);
            netCodes.add(n);
            custodyByAsset.computeIfAbsent(a, k -> new ArrayList<>());
            if (!custodyByAsset.get(a).contains(n)) {
                custodyByAsset.get(a).add(n);
            }
        }
        List<Map<String, Object>> assetsIn = (List<Map<String, Object>>) cat.getOrDefault("assets", List.of());
        List<Map<String, Object>> assetsOut = assetsIn.stream()
                .filter(m -> assetCodes.contains(String.valueOf(m.getOrDefault("code", "")).toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toList());
        List<Map<String, Object>> netsIn = (List<Map<String, Object>>) cat.getOrDefault("networks", List.of());
        List<Map<String, Object>> netsOut = netsIn.stream()
                .filter(m -> netCodes.contains(String.valueOf(m.getOrDefault("code", "")).toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>(cat);
        out.put("assets", assetsOut);
        out.put("networks", netsOut);
        out.put("pairs", pairsOut);
        out.put("custodyByAsset", custodyByAsset);
        return out;
    }

    /**
     * 가맹 허용 네트워크 중, 해당 자산의 본사 수탁(사용Y)이 있는 네트워크를 고른다.
     * CSV 순서만 따르면 BASE처럼 수탁 없는 네트워크가 먼저 잡혀 CUSTODY_MISSING 이 난다.
     */
    private String resolvePreferredNetwork(String asset, MerchantCryptoConfig cfg) {
        List<String> nets = splitCsvTokens(cfg.getNetworksCsv());
        if (nets.isEmpty()) {
            return "";
        }
        /* 자산별 관례 우선순위 (허용·수탁 모두 있을 때만) */
        List<String> preferredOrder = new ArrayList<>();
        if ("USDT".equalsIgnoreCase(asset)) {
            preferredOrder.addAll(List.of("TRC20", "BEP20", "ERC20", "BASE", "POLYGON", "SOL", "ARBITRUM"));
        } else if ("USDC".equalsIgnoreCase(asset)) {
            preferredOrder.addAll(List.of("BEP20", "BASE", "ERC20", "POLYGON", "SOL", "ARBITRUM", "TRC20"));
        } else if ("EURC".equalsIgnoreCase(asset)) {
            preferredOrder.addAll(List.of("BASE", "ERC20", "BEP20", "POLYGON", "SOL"));
        }
        for (String cand : preferredOrder) {
            if (nets.stream().anyMatch(n -> n.equalsIgnoreCase(cand))) {
                String c = resolveCustody(cand, asset);
                if (c != null && !c.isBlank()) {
                    return cand.toUpperCase(Locale.ROOT);
                }
            }
        }
        for (String n : nets) {
            String c = resolveCustody(n, asset);
            if (c != null && !c.isBlank()) {
                return n.toUpperCase(Locale.ROOT);
            }
        }
        /* 수탁이 전혀 없으면 빈 문자열 → 호출부에서 CUSTODY_MISSING */
        return "";
    }

    private static List<String> splitCsvTokens(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : csv.split(",")) {
            String t = p == null ? "" : p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private void expireIfNeeded(CryptoDepositTxn txn) {
        if (txn.getExpireAt() != null
                && txn.getExpireAt().isBefore(LocalDateTime.now())
                && (CryptoDepositTxn.ST_AWAITING.equals(txn.getStatus())
                || CryptoDepositTxn.ST_PENDING.equals(txn.getStatus()))) {
            txn.setStatus(CryptoDepositTxn.ST_EXPIRED);
            depositTxnRepository.save(txn);
        }
    }

    private Map<String, Object> publicTxn(CryptoDepositTxn txn, MerchantCryptoConfig cfg) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", txn.getCompId());
        data.put("orderNo", txn.getOrderNo());
        data.put("amount", txn.getAmount());
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("quoteAmount", txn.getAmount());
        String dispCcy = txn.getDisplayCurrency() != null && !txn.getDisplayCurrency().isBlank()
                ? txn.getDisplayCurrency() : QUOTE_CURRENCY;
        BigDecimal dispAmt = txn.getDisplayAmount() != null ? txn.getDisplayAmount() : txn.getAmount();
        data.put("displayCurrency", dispCcy);
        data.put("displayAmount", dispAmt);
        BigDecimal assetAmt = txn.getAssetAmount();
        if (assetAmt == null && txn.getAmount() != null && txn.getAssetCode() != null) {
            assetAmt = cryptoFxService.toAssetAmount(txn.getAmount(), txn.getAssetCode());
        }
        data.put("assetAmount", assetAmt);
        data.put("remittanceAmount", assetAmt);
        data.put("currency", txn.getAssetCode());
        data.put("assetCode", txn.getAssetCode());
        data.put("networkCode", txn.getNetworkCode());
        data.put("productName", txn.getProductName());
        data.put("custodyAddress", txn.getCustodyAddress());
        data.put("sessionToken", txn.getSessionToken());
        data.put("status", txn.getStatus());
        data.put("settleMode", txn.getSettleMode());
        data.put("payoutMode", txn.getPayoutMode());
        data.put("expireAt", txn.getExpireAt());
        data.put("depositTxHash", txn.getDepositTxHash());
        data.put("operationalPgCd", PgVendor.CRYPTO);
        if (txn.getBuyerEmail() != null && !txn.getBuyerEmail().isBlank()) {
            data.put("buyerEmail", txn.getBuyerEmail());
        }
        if (txn.getBuyerName() != null && !txn.getBuyerName().isBlank()) {
            data.put("buyerName", txn.getBuyerName());
        }
        if (txn.getBuyerPhone() != null && !txn.getBuyerPhone().isBlank()) {
            data.put("buyerPhone", txn.getBuyerPhone());
        }
        if (txn.getBuyerCountryIso2() != null && !txn.getBuyerCountryIso2().isBlank()) {
            data.put("buyerCountryIso2", txn.getBuyerCountryIso2());
        }
        if (cfg != null) {
            CryptoReserveResolver.Effective eff = reserveResolver.resolve(cfg, txn.getCompId());
            data.put("holdDays", eff.effectiveHoldDays());
            data.put("reservePct", eff.effectiveReservePct());
            data.put("reserveFollowHq", eff.followHq() ? "Y" : "N");
        }
        return data;
    }

    private record BuyerContact(String email, String name, String phone, String countryIso2, String shippingJson) {}

    /** @return 실패 시 fail map, 성공 시 null */
    private Map<String, Object> validateBuyerContact(BuyerContact buyer, Map<String, Object> body, Long orgUnitId) {
        String emailYn = displayPolicy.effectiveBuyerEmailUseYn(orgUnitId);
        String countryYn = displayPolicy.effectiveBuyerCountryUseYn(orgUnitId);
        String phoneYn = displayPolicy.effectiveBuyerPhoneUseYn(orgUnitId);

        /* API 통합 prepare 는 buyer 객체 필수 — 카드와 동일 email·phone·countryIso2 */
        boolean fromApiBuyer = body != null && (body.get("buyer") != null || body.get("buyerPrefill") != null);
        if (fromApiBuyer) {
            try {
                IcipayBuyerContactUtil.extractAndValidateRequired(body);
            } catch (IcipayBuyerContactUtil.BuyerRequiredException ex) {
                return ex.toFailMap();
            } catch (IllegalArgumentException ex) {
                return CheckoutFailI18n.buyerRequiredGeneric();
            }
            /* API도 영문·이메일·전화 숫자 규칙 적용 */
            BuyerContact apiBuyer = extractBuyerContact(body);
            Map<String, Object> formatFail = validateBuyerFormat(apiBuyer, emailYn, phoneYn);
            if (formatFail != null) {
                return formatFail;
            }
            return null;
        }
        if (buyer == null || buyer.name() == null || buyer.name().isBlank()) {
            return fail("성명이 필요합니다.", "BUYER_NAME_REQUIRED");
        }
        if (CheckoutBuyerContactUtil.isYn(emailYn) && (buyer.email() == null || buyer.email().isBlank())) {
            return CheckoutFailI18n.buyerEmailRequired();
        }
        if (CheckoutBuyerContactUtil.isYn(phoneYn) && (buyer.phone() == null || buyer.phone().isBlank())) {
            return CheckoutFailI18n.buyerPhoneRequired();
        }
        if (CheckoutBuyerContactUtil.isYn(countryYn)
                && (buyer.countryIso2() == null || buyer.countryIso2().length() != 2)) {
            return CheckoutFailI18n.buyerCountryRequired();
        }
        return validateBuyerFormat(buyer, emailYn, phoneYn);
    }

    /** 이름·이메일 영문(라틴)만, 이메일 형식, 전화 숫자만. */
    private Map<String, Object> validateBuyerFormat(BuyerContact buyer, String emailYn, String phoneYn) {
        if (buyer == null) {
            return null;
        }
        if (buyer.name() != null && !buyer.name().isBlank() && !isLatinName(buyer.name())) {
            return fail("성명은 영문만 입력할 수 있습니다.", "BUYER_NAME_LATIN_ONLY");
        }
        boolean emailRequired = CheckoutBuyerContactUtil.isYn(emailYn);
        if (emailRequired || (buyer.email() != null && !buyer.email().isBlank())) {
            String email = buyer.email() != null ? buyer.email().trim() : "";
            if (emailRequired && email.isBlank()) {
                return CheckoutFailI18n.buyerEmailRequired();
            }
            if (!email.isBlank()) {
                if (!isLatinText(email)) {
                    return fail("이메일은 영문만 입력할 수 있습니다.", "BUYER_EMAIL_LATIN_ONLY");
                }
                if (!isValidEmail(email)) {
                    return fail("이메일 형식이 올바르지 않습니다.", "BUYER_EMAIL_INVALID");
                }
            }
        }
        boolean phoneRequired = CheckoutBuyerContactUtil.isYn(phoneYn);
        if (phoneRequired || (buyer.phone() != null && !buyer.phone().isBlank())) {
            String phone = buyer.phone() != null ? buyer.phone().trim() : "";
            if (phoneRequired && phone.isBlank()) {
                return CheckoutFailI18n.buyerPhoneRequired();
            }
            if (!phone.isBlank() && !phone.matches("^[0-9]+$")) {
                return fail("전화번호는 숫자만 입력할 수 있습니다.", "BUYER_PHONE_DIGITS_ONLY");
            }
        }
        return null;
    }

    private static boolean isLatinName(String s) {
        /* 영문·공백·하이픈·아포스트로피 */
        return s != null && s.matches("^[A-Za-z][A-Za-z '\\-]*$");
    }

    private static boolean isLatinText(String s) {
        return s != null && s.chars().allMatch(c -> c <= 0x7F);
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    }

    private static void applyBuyerToTxn(CryptoDepositTxn txn, BuyerContact buyer) {
        if (buyer == null) {
            return;
        }
        txn.setBuyerEmail(blankToNull(buyer.email()));
        txn.setBuyerName(blankToNull(buyer.name()));
        txn.setBuyerPhone(blankToNull(buyer.phone()));
        txn.setBuyerCountryIso2(blankToNull(buyer.countryIso2()));
        txn.setBuyerShippingJson(blankToNull(buyer.shippingJson()));
    }

    private static BuyerContact extractBuyerContact(Map<String, Object> body) {
        String email = str(body, "buyerEmail", "email");
        String name = str(body, "buyerName", "name");
        String phone = str(body, "buyerPhone", "phone", "telephone");
        String country = str(body, "buyerCountryIso2", "countryIso2", "country").toUpperCase(Locale.ROOT);
        String shipping = str(body, "shippingAddress", "buyerShippingAddress", "address");

        Map<String, Object> nested = null;
        if (body != null) {
            Object raw = body.get("buyer");
            if (!(raw instanceof Map) && body.get("buyerPrefill") instanceof Map) {
                raw = body.get("buyerPrefill");
            }
            if (raw instanceof Map<?, ?> m) {
                nested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        nested.put(String.valueOf(e.getKey()), e.getValue());
                    }
                }
            }
        }
        if (nested != null) {
            if (email.isBlank()) email = str(nested, "email", "payEmailAddress");
            if (phone.isBlank()) phone = str(nested, "phone", "payTelephone", "telephone");
            if (country.isBlank()) {
                country = str(nested, "countryIso2", "payCountryIsoCode2", "country").toUpperCase(Locale.ROOT);
            }
            if (name.isBlank()) {
                name = str(nested, "name", "buyerName");
            }
            if (name.isBlank()) {
                String fn = str(nested, "firstName", "payFirstname", "firstname");
                String ln = str(nested, "lastName", "payLastname", "lastname");
                name = (fn + " " + ln).trim();
            }
            if (shipping.isBlank()) {
                shipping = str(nested, "shippingAddress", "address", "payStreetAddress1");
            }
        }
        if (country.length() > 2) {
            country = country.substring(0, 2);
        }
        String shippingJson = null;
        if (!shipping.isBlank()) {
            shippingJson = "{\"address\":\"" + shipping.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        }
        return new BuyerContact(
                email.isBlank() ? null : email,
                name.isBlank() ? null : name,
                phone.isBlank() ? null : phone,
                country.length() == 2 ? country : null,
                shippingJson);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String extractSellerAddress(MerchantCryptoConfig cfg, String network, String asset) {
        if (cfg == null || cfg.getSellerWalletJson() == null || cfg.getSellerWalletJson().isBlank()) {
            return null;
        }
        String json = cfg.getSellerWalletJson();
        // 단순 키 탐색: "TRC20":"addr" 또는 "USDT_TRC20":"addr"
        String[] keys = {
                "\"" + asset + "_" + network + "\"",
                "\"" + network + "\"",
                "\"" + asset + "\""
        };
        for (String key : keys) {
            int i = json.indexOf(key);
            if (i < 0) continue;
            int colon = json.indexOf(':', i + key.length());
            if (colon < 0) continue;
            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) continue;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) continue;
            String v = json.substring(q1 + 1, q2).trim();
            if (!v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static String publicBase(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            return "";
        }
        String p = (proto != null && !proto.isBlank()) ? proto : request.getScheme();
        return p + "://" + host;
    }

    private static Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("data", data);
        return r;
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        r.put("errorCode", code);
        return r;
    }

    private static BigDecimal parseAmount(Object raw) {
        if (raw == null) return null;
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null) return "";
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null && !v.toString().isBlank()) {
                return v.toString().trim();
            }
        }
        return "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b != null ? b : "";
    }

    private static String firstCsv(String csv, String def) {
        if (csv == null || csv.isBlank()) return def;
        for (String p : csv.split(",")) {
            if (p != null && !p.isBlank()) {
                return p.trim().toUpperCase(Locale.ROOT);
            }
        }
        return def;
    }

    private static boolean csvContains(String csv, String token) {
        if (csv == null || token == null) return false;
        for (String p : csv.split(",")) {
            if (token.equalsIgnoreCase(p.trim())) return true;
        }
        return false;
    }

    private static String nzMode(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim().toUpperCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String hex(int bytes) {
        byte[] b = new byte[bytes];
        RND.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }
}
