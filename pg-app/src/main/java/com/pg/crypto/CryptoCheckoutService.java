package com.pg.crypto;

import com.pg.entity.*;
import com.pg.integration.pg.PgVendor;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.repository.*;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.util.OrgUseYnUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/** 크립토 입금 체크아웃 prepare / session / status / confirm. 금액·수수료·한도 기준통화는 USD. */
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

    public CryptoCheckoutService(OrgUnitRepository orgUnitRepository,
                                 MerchantProfileRepository merchantProfileRepository,
                                 MerchantCryptoConfigRepository configRepository,
                                 CryptoDepositTxnRepository depositTxnRepository,
                                 CryptoCustodyAddressRepository custodyAddressRepository,
                                 CryptoCatalogService catalogService,
                                 CryptoPayoutService payoutService,
                                 CryptoCommissionChargeService commissionChargeService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.configRepository = configRepository;
        this.depositTxnRepository = depositTxnRepository;
        this.custodyAddressRepository = custodyAddressRepository;
        this.catalogService = catalogService;
        this.payoutService = payoutService;
        this.commissionChargeService = commissionChargeService;
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
        BigDecimal amount = parseAmount(body != null ? body.get("amount") : null);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount(USD)가 필요합니다.", "INVALID_AMOUNT");
        }
        String fiatHint = str(body, "fiatCurrency", "quoteCurrency", "settleCurrency").toUpperCase(Locale.ROOT);
        /* 견적·수수료·한도는 USD. USDT/USDC 액면≈USD 허용. EURC·기타 법정통화 견적 거부. */
        if (!fiatHint.isBlank() && !QUOTE_CURRENCY.equals(fiatHint)
                && !Set.of("USDT", "USDC").contains(fiatHint)) {
            return fail("크립토 결제는 USD 기준입니다.", "USD_REQUIRED");
        }

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
        if (asset.isBlank()) {
            asset = firstCsv(cfg.getAssetsCsv(), "");
        }
        if (asset.isBlank()) {
            return fail("허용 자산이 없습니다. 자산을 활성으로 설정하세요.", "ASSET_NOT_ALLOWED");
        }
        String network = str(body, "networkCode", "network").toUpperCase(Locale.ROOT);
        if (network.isBlank()) {
            network = defaultNetworkForAsset(asset, cfg);
        }
        if (!csvContains(cfg.getAssetsCsv(), asset)) {
            return fail("가맹점에서 허용하지 않는 자산입니다.", "ASSET_NOT_ALLOWED");
        }
        if (!csvContains(cfg.getNetworksCsv(), network)) {
            return fail("가맹점에서 허용하지 않는 네트워크입니다.", "NETWORK_NOT_ALLOWED");
        }
        if (!catalogService.isValidPair(asset, network)) {
            return fail("자산·네트워크 조합이 올바르지 않습니다.", "INVALID_ASSET_NETWORK");
        }

        String custody = resolveCustody(network, asset);
        if (custody == null || custody.isBlank()) {
            return fail("수탁 주소가 설정되지 않았습니다. 본사 크립토설정을 확인하세요.", "CUSTODY_MISSING");
        }

        Optional<CryptoDepositTxn> existing = depositTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo);
        if (existing.isPresent() && CryptoDepositTxn.ST_CONFIRMED.equals(existing.get().getStatus())) {
            return fail("이미 확정된 주문번호입니다.", "DUPLICATE_ORDER");
        }

        CryptoDepositTxn txn = existing.orElseGet(CryptoDepositTxn::new);
        txn.setOrgUnitId(orgUnitId);
        txn.setCompId(ou.getCode());
        txn.setOrderNo(orderNo);
        txn.setAmount(amount);
        txn.setAssetCode(asset);
        txn.setNetworkCode(network);
        txn.setProductName(firstNonBlank(str(body, "productName"), str(body, "item")));
        txn.setBuyerEmail(str(body, "buyerEmail", "email"));
        txn.setBuyerName(str(body, "buyerName", "name"));
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
        String payUrl = NeutralCheckoutRoute.buildPayUrl(base, ou.getCode(), txn.getSessionToken(),
                str(body, "lang", "langCode"), false);

        Map<String, Object> data = publicTxn(txn, cfg);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("payUrl", payUrl);
        data.put("payMeans", "CRYPTO");
        data.put("cryptoEnabled", true);
        data.put("quoteCurrency", QUOTE_CURRENCY);
        data.put("catalog", catalogService.publicCatalog(
                Arrays.asList(cfg.getAssetsCsv().split(",")),
                Arrays.asList(cfg.getNetworksCsv().split(","))));
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
            data.put("catalog", catalogService.publicCatalog(
                    Arrays.asList(cfg.getAssetsCsv().split(",")),
                    Arrays.asList(cfg.getNetworksCsv().split(","))));
        } else {
            data.put("catalog", catalogService.publicCatalog(List.of(txn.getAssetCode()), List.of(txn.getNetworkCode())));
        }
        data.put("wallets", catalogService.wallets(txn.getNetworkCode()));
        return ok(data);
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
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("status", txn.getStatus());
        out.put("id", txn.getId());
        out.put("feeAmt", feeCharged);
        out.put("feeChargedYn", txn.getFeeChargedYn());
        out.put("payTrnId", txn.getPayTrnId());
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
        m.put("holdDays", cfg.get().getHoldDays());
        m.put("reservePct", cfg.get().getReservePct());
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

    private String defaultNetworkForAsset(String asset, MerchantCryptoConfig cfg) {
        if ("USDT".equalsIgnoreCase(asset) && csvContains(cfg.getNetworksCsv(), "TRC20")) {
            return "TRC20";
        }
        if (csvContains(cfg.getNetworksCsv(), "ERC20")) {
            return "ERC20";
        }
        return firstCsv(cfg.getNetworksCsv(), "TRC20");
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
        if (cfg != null) {
            data.put("holdDays", cfg.getHoldDays());
            data.put("reservePct", cfg.getReservePct());
        }
        return data;
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
