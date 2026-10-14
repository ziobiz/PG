package com.pg.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.CryptoCustodyAddress;
import com.pg.entity.CryptoDepositTxn;
import com.pg.entity.MerchantCryptoConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CryptoCustodyAddressRepository;
import com.pg.repository.CryptoDepositTxnRepository;
import com.pg.repository.MerchantCryptoConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.CommissionFeeChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CryptoAdminService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CryptoCatalogService catalogService;
    private final CryptoCustodyAddressRepository custodyAddressRepository;
    private final CryptoDepositTxnRepository depositTxnRepository;
    private final MerchantCryptoConfigRepository configRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final CryptoCheckoutService checkoutService;
    private final CryptoPayoutService payoutService;

    public CryptoAdminService(CryptoCatalogService catalogService,
                              CryptoCustodyAddressRepository custodyAddressRepository,
                              CryptoDepositTxnRepository depositTxnRepository,
                              MerchantCryptoConfigRepository configRepository,
                              MerchantProfileRepository merchantProfileRepository,
                              OrgUnitRepository orgUnitRepository,
                              CryptoCheckoutService checkoutService,
                              CryptoPayoutService payoutService) {
        this.catalogService = catalogService;
        this.custodyAddressRepository = custodyAddressRepository;
        this.depositTxnRepository = depositTxnRepository;
        this.configRepository = configRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.checkoutService = checkoutService;
        this.payoutService = payoutService;
    }

    /** HQ 화면 자산 카드 순서: USDC → USDT → EURC */
    public static final List<String> CUSTODY_ASSET_ORDER = List.of("USDC", "USDT", "EURC");

    public Map<String, Object> hqSettings() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("catalog", catalogService.publicCatalog(null, null));
        out.put("custodyAssetOrder", CUSTODY_ASSET_ORDER);
        List<Map<String, Object>> custody = new ArrayList<>();
        for (CryptoCustodyAddress a : custodyAddressRepository.findByUseYnOrderByIdAsc("Y")) {
            custody.add(toCustodyMap(a));
        }
        custody.sort(Comparator
                .comparing((Map<String, Object> m) -> assetOrderIndex(String.valueOf(m.get("assetCode"))))
                .thenComparing(m -> String.valueOf(m.get("networkCode")), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(m -> "Y".equalsIgnoreCase(String.valueOf(m.get("primaryYn"))) ? 0 : 1)
                .thenComparing(m -> String.valueOf(m.get("id"))));
        out.put("custodyAddresses", custody);
        Map<String, List<Map<String, Object>>> byAsset = new LinkedHashMap<>();
        for (String code : CUSTODY_ASSET_ORDER) {
            byAsset.put(code, new ArrayList<>());
        }
        for (Map<String, Object> m : custody) {
            String ac = m.get("assetCode") != null ? String.valueOf(m.get("assetCode")).toUpperCase(Locale.ROOT) : "";
            byAsset.computeIfAbsent(ac.isBlank() ? "_" : ac, k -> new ArrayList<>()).add(m);
        }
        out.put("custodyByAsset", byAsset);
        out.put("merchantConfigs", listMerchantConfigs());
        return out;
    }

    private static int assetOrderIndex(String assetCode) {
        String a = assetCode == null || "null".equalsIgnoreCase(assetCode) ? "" : assetCode.toUpperCase(Locale.ROOT);
        int i = CUSTODY_ASSET_ORDER.indexOf(a);
        return i >= 0 ? i : 99;
    }

    private static Map<String, Object> toCustodyMap(CryptoCustodyAddress a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("networkCode", a.getNetworkCode());
        m.put("assetCode", a.getAssetCode());
        m.put("address", a.getAddress());
        m.put("label", a.getLabel());
        m.put("useYn", a.getUseYn());
        m.put("primaryYn", a.getPrimaryYn() == null ? "N" : a.getPrimaryYn());
        return m;
    }

    /** 등록된 가맹 크립토설정 전체 목록(본사 화면용). */
    public List<Map<String, Object>> listMerchantConfigs() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MerchantCryptoConfig cfg : configRepository.findAll()) {
            OrgUnit ou = orgUnitRepository.findById(cfg.getOrgUnitId()).orElse(null);
            if (ou == null || ou.getOrgLevel() != OrgLevel.MERCHANT) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("compId", ou.getCode());
            m.put("compNm", ou.getName());
            m.put("orgUnitId", ou.getId());
            m.put("useYn", cfg.getUseYn());
            m.put("checkoutMeans", cfg.getCheckoutMeans());
            m.put("settleMode", cfg.getSettleMode());
            m.put("payoutMode", cfg.getPayoutMode());
            m.put("holdDays", cfg.getHoldDays());
            m.put("reservePct", cfg.getReservePct());
            m.put("assetsCsv", cfg.getAssetsCsv());
            m.put("networksCsv", cfg.getNetworksCsv());
            m.put("sellerWalletJson", cfg.getSellerWalletJson());
            Map<String, String> wallets = parseSellerWallets(cfg.getSellerWalletJson());
            m.put("usdtTrc20", wallets.getOrDefault("USDT_TRC20", wallets.getOrDefault("TRC20", "")));
            m.put("erc20", wallets.getOrDefault("ERC20", ""));
            rows.add(m);
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("compId")), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private static Map<String, String> parseSellerWallets(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return out;
        try {
            Map<String, Object> o = JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (o == null) return out;
            for (Map.Entry<String, Object> e : o.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    String v = String.valueOf(e.getValue()).trim();
                    if (!v.isEmpty()) out.put(e.getKey().toUpperCase(Locale.ROOT), v);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    @Transactional
    public Map<String, Object> saveCustody(Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        CryptoCustodyAddress a = id != null
                ? custodyAddressRepository.findById(id).orElse(new CryptoCustodyAddress())
                : new CryptoCustodyAddress();
        a.setNetworkCode(str(body.get("networkCode")).toUpperCase(Locale.ROOT));
        String asset = str(body.get("assetCode"));
        a.setAssetCode(asset.isBlank() ? null : asset.toUpperCase(Locale.ROOT));
        a.setAddress(str(body.get("address")));
        a.setLabel(str(body.get("label")));
        a.setUseYn("N".equalsIgnoreCase(str(body.get("useYn"))) ? "N" : "Y");
        if (a.getNetworkCode().isBlank() || a.getAddress().isBlank()) {
            throw new IllegalArgumentException("networkCode와 address가 필요합니다.");
        }
        if (a.getAssetCode() == null || a.getAssetCode().isBlank()) {
            throw new IllegalArgumentException("assetCode(자산)가 필요합니다.");
        }
        boolean makePrimary = "Y".equalsIgnoreCase(str(body.get("primaryYn")))
                || "true".equalsIgnoreCase(str(body.get("primaryYn")));
        if (a.getPrimaryYn() == null || a.getPrimaryYn().isBlank()) {
            a.setPrimaryYn("N");
        }
        custodyAddressRepository.save(a);
        if (makePrimary) {
            promotePrimary(a);
        } else if (!hasPrimaryForPair(a.getNetworkCode(), a.getAssetCode())) {
            /* 해당 자산×네트워크에 대표가 없으면 이번 주소를 자동 대표 */
            promotePrimary(a);
        }
        return hqSettings();
    }

    /** 동일 자산×네트워크의 입금 대표 수탁으로 지정. OTP는 컨트롤러에서 검증. */
    @Transactional
    public Map<String, Object> setCustodyPrimary(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("대표로 지정할 수탁 지갑 ID가 필요합니다.");
        }
        CryptoCustodyAddress a = custodyAddressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대표수탁지갑을 찾을 수 없습니다."));
        if (!"Y".equalsIgnoreCase(a.getUseYn())) {
            throw new IllegalArgumentException("삭제(비활성)된 주소는 대표로 지정할 수 없습니다.");
        }
        promotePrimary(a);
        return hqSettings();
    }

    /** 대표수탁지갑 삭제(사용 N). OTP는 컨트롤러에서 검증. */
    @Transactional
    public Map<String, Object> deleteCustody(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("삭제할 대표수탁지갑 ID가 필요합니다.");
        }
        CryptoCustodyAddress a = custodyAddressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대표수탁지갑을 찾을 수 없습니다."));
        boolean wasPrimary = "Y".equalsIgnoreCase(a.getPrimaryYn());
        String net = a.getNetworkCode();
        String asset = a.getAssetCode();
        a.setUseYn("N");
        a.setPrimaryYn("N");
        custodyAddressRepository.save(a);
        if (wasPrimary) {
            promoteFirstActive(net, asset);
        }
        return hqSettings();
    }

    private void promotePrimary(CryptoCustodyAddress target) {
        clearPrimaryForPair(target.getNetworkCode(), target.getAssetCode(), target.getId());
        target.setPrimaryYn("Y");
        target.setUseYn("Y");
        custodyAddressRepository.save(target);
    }

    private void clearPrimaryForPair(String networkCode, String assetCode, Long excludeId) {
        List<CryptoCustodyAddress> peers = assetCode == null || assetCode.isBlank()
                ? custodyAddressRepository.findByNetworkCodeAndAssetCodeIsNullAndUseYn(networkCode, "Y")
                : custodyAddressRepository.findByNetworkCodeAndAssetCodeAndUseYn(networkCode, assetCode, "Y");
        for (CryptoCustodyAddress p : peers) {
            if (excludeId != null && excludeId.equals(p.getId())) continue;
            if ("Y".equalsIgnoreCase(p.getPrimaryYn())) {
                p.setPrimaryYn("N");
                custodyAddressRepository.save(p);
            }
        }
    }

    private boolean hasPrimaryForPair(String networkCode, String assetCode) {
        List<CryptoCustodyAddress> peers = assetCode == null || assetCode.isBlank()
                ? custodyAddressRepository.findByNetworkCodeAndAssetCodeIsNullAndUseYn(networkCode, "Y")
                : custodyAddressRepository.findByNetworkCodeAndAssetCodeAndUseYn(networkCode, assetCode, "Y");
        return peers.stream().anyMatch(p -> "Y".equalsIgnoreCase(p.getPrimaryYn()));
    }

    private void promoteFirstActive(String networkCode, String assetCode) {
        List<CryptoCustodyAddress> peers = assetCode == null || assetCode.isBlank()
                ? custodyAddressRepository.findByNetworkCodeAndAssetCodeIsNullAndUseYn(networkCode, "Y")
                : custodyAddressRepository.findByNetworkCodeAndAssetCodeAndUseYn(networkCode, assetCode, "Y");
        if (peers.isEmpty()) return;
        peers.sort(Comparator.comparing(CryptoCustodyAddress::getId));
        promotePrimary(peers.get(0));
    }

    public Map<String, Object> merchantConfig(String compId) {
        OrgUnit ou = orgUnitRepository.findByCode(compId.trim())
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 설정할 수 있습니다.");
        }
        boolean exists = configRepository.existsById(ou.getId());
        MerchantCryptoConfig cfg = exists
                ? configRepository.findById(ou.getId()).orElseThrow()
                : newUnusedMerchantCryptoConfig(ou.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compId", ou.getCode());
        m.put("compNm", ou.getName());
        m.put("orgUnitId", ou.getId());
        /* 미등록 가맹 = 기본 미사용(N) · 허용자산 비활성 */
        m.put("useYn", exists ? cfg.getUseYn() : "N");
        m.put("checkoutMeans", exists ? cfg.getCheckoutMeans() : MerchantCryptoConfig.MEANS_CARD);
        m.put("settleMode", cfg.getSettleMode());
        m.put("payoutMode", cfg.getPayoutMode());
        m.put("holdDays", cfg.getHoldDays());
        m.put("reservePct", cfg.getReservePct());
        m.put("assetsCsv", exists ? cfg.getAssetsCsv() : "");
        m.put("networksCsv", exists ? cfg.getNetworksCsv() : "");
        m.put("sellerWalletJson", exists ? cfg.getSellerWalletJson() : null);
        m.put("catalog", catalogService.publicCatalog(null, null));
        return m;
    }

    /** 가맹 크립토 미등록 시 기본값 — 사용=미사용, 자산·네트워크 비활성. */
    private static MerchantCryptoConfig newUnusedMerchantCryptoConfig(Long orgUnitId) {
        MerchantCryptoConfig c = new MerchantCryptoConfig();
        c.setOrgUnitId(orgUnitId);
        c.setUseYn("N");
        c.setCheckoutMeans(MerchantCryptoConfig.MEANS_CARD);
        c.setSettleMode(MerchantCryptoConfig.SETTLE_SCHEDULED);
        c.setPayoutMode(MerchantCryptoConfig.PAYOUT_MANUAL);
        c.setHoldDays(3);
        c.setReservePct(java.math.BigDecimal.ZERO);
        c.setAssetsCsv("");
        c.setNetworksCsv("");
        c.setSellerWalletJson(null);
        return c;
    }

    @Transactional
    public Map<String, Object> saveMerchantConfig(Map<String, Object> body) {
        String compId = str(body.get("compId"));
        OrgUnit ou = orgUnitRepository.findByCode(compId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        MerchantCryptoConfig cfg = configRepository.findById(ou.getId())
                .orElseGet(() -> newUnusedMerchantCryptoConfig(ou.getId()));
        cfg.setUseYn("Y".equalsIgnoreCase(str(body.get("useYn"))) ? "Y" : "N");
        /* 결제수단 CARD/CRYPTO/BOTH 는 UI·게이트에서 제거. 레거시 컬럼은 useYn 에 맞춤(카드는 URL·PG 설정 우선). */
        cfg.setCheckoutMeans("Y".equalsIgnoreCase(cfg.getUseYn())
                ? MerchantCryptoConfig.MEANS_CRYPTO
                : MerchantCryptoConfig.MEANS_CARD);
        String settle = str(body.get("settleMode")).toUpperCase(Locale.ROOT);
        cfg.setSettleMode(MerchantCryptoConfig.SETTLE_IMMEDIATE.equals(settle)
                ? MerchantCryptoConfig.SETTLE_IMMEDIATE : MerchantCryptoConfig.SETTLE_SCHEDULED);
        String payout = str(body.get("payoutMode")).toUpperCase(Locale.ROOT);
        cfg.setPayoutMode(MerchantCryptoConfig.PAYOUT_AUTO.equals(payout)
                ? MerchantCryptoConfig.PAYOUT_AUTO : MerchantCryptoConfig.PAYOUT_MANUAL);
        try {
            cfg.setHoldDays(Integer.parseInt(str(body.get("holdDays"))));
        } catch (Exception e) {
            cfg.setHoldDays(3);
        }
        if (cfg.getHoldDays() < 0) {
            cfg.setHoldDays(0);
        }
        try {
            java.math.BigDecimal rp = new java.math.BigDecimal(str(body.get("reservePct")).isBlank()
                    ? "0" : str(body.get("reservePct")));
            if (rp.compareTo(java.math.BigDecimal.ZERO) < 0) {
                rp = java.math.BigDecimal.ZERO;
            }
            if (rp.compareTo(new java.math.BigDecimal("100")) > 0) {
                rp = new java.math.BigDecimal("100");
            }
            cfg.setReservePct(rp);
        } catch (Exception e) {
            cfg.setReservePct(java.math.BigDecimal.ZERO);
        }
        String assets = str(body.get("assetsCsv"));
        /* 빈값=전 자산 비활성. 활성 자산만 CSV. */
        cfg.setAssetsCsv(assets.isBlank() ? "" : assets.toUpperCase(Locale.ROOT));
        String nets = str(body.get("networksCsv"));
        /* 빈값=네트워크 미선택(기본 미사용). 예전 기본 TRC20,ERC20 자동채움 제거 */
        cfg.setNetworksCsv(nets.isBlank() ? "" : nets.toUpperCase(Locale.ROOT));
        if (body.containsKey("sellerWalletJson")) {
            cfg.setSellerWalletJson(str(body.get("sellerWalletJson")));
        }
        configRepository.save(cfg);
        /* CRYPTO 수수료 슬롯: 활성 시 DEFAULT_CRYPTO 자동 부여. 크립토전용(checkoutMeans=CRYPTO)이면 FIAT 슬롯 비활성 */
        boolean cryptoOn = "Y".equalsIgnoreCase(cfg.getUseYn());
        boolean cryptoOnly = cryptoOn && MerchantCryptoConfig.MEANS_CRYPTO.equalsIgnoreCase(cfg.getCheckoutMeans());
        assignCryptoCommissionSlot(ou.getId(), cryptoOn, cryptoOnly);
        return merchantConfig(ou.getCode());
    }

    public Map<String, Object> searchDeposits(String compId, String status, int page, int size) {
        Page<CryptoDepositTxn> p = depositTxnRepository.search(
                nz(compId), nz(status),
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size))));
        List<Map<String, Object>> rows = new ArrayList<>();
        int base = (int) p.getPageable().getOffset();
        int i = 0;
        for (CryptoDepositTxn t : p.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rowNo", base + (++i));
            m.put("id", t.getId());
            m.put("compId", t.getCompId());
            m.put("orderNo", t.getOrderNo());
            m.put("amount", t.getAmount());
            m.put("assetCode", t.getAssetCode());
            m.put("networkCode", t.getNetworkCode());
            m.put("productName", t.getProductName());
            m.put("status", t.getStatus());
            m.put("statusNm", depositStatusNm(t.getStatus()));
            m.put("settleMode", t.getSettleMode());
            m.put("payoutMode", t.getPayoutMode());
            m.put("custodyAddress", t.getCustodyAddress());
            m.put("depositTxHash", t.getDepositTxHash());
            m.put("confirmedAt", t.getConfirmedAt());
            m.put("expireAt", t.getExpireAt());
            m.put("createdAt", t.getCreatedAt());
            rows.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", rows);
        out.put("total", p.getTotalElements());
        return out;
    }

    public Map<String, Object> confirmDeposit(Long id, String txHash) {
        return checkoutService.confirmDeposit(id, txHash);
    }

    public CryptoPayoutService payouts() {
        return payoutService;
    }

    private static String depositStatusNm(String st) {
        if (st == null) return "";
        return switch (st) {
            case CryptoDepositTxn.ST_PENDING -> "대기";
            case CryptoDepositTxn.ST_AWAITING -> "입금대기";
            case CryptoDepositTxn.ST_CONFIRMED -> "입금확정";
            case CryptoDepositTxn.ST_EXPIRED -> "만료";
            case CryptoDepositTxn.ST_FAILED -> "실패";
            default -> st;
        };
    }

    private void assignCryptoCommissionSlot(Long orgUnitId, boolean cryptoOn, boolean cryptoOnly) {
        if (orgUnitId == null) {
            return;
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mpOpt.isEmpty()) {
            return;
        }
        MerchantProfile mp = mpOpt.get();
        Map<String, Object> rs = new LinkedHashMap<>();
        String raw = mp.getRegionalSettings();
        if (raw != null && !raw.isBlank()) {
            try {
                rs.putAll(JSON.readValue(raw, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ignored) {
            }
        }
        if (cryptoOn) {
            if (!rs.containsKey("commissionCryptoFollowHq")
                    || String.valueOf(rs.getOrDefault("commissionCryptoFollowHq", "")).isBlank()) {
                rs.put("commissionCryptoFollowHq", "Y");
            }
            String hqCrypto = String.valueOf(rs.getOrDefault("hqCryptoPolicyScope", "")).trim();
            if (hqCrypto.isEmpty() || "null".equalsIgnoreCase(hqCrypto)) {
                rs.put("hqCryptoPolicyScope", CommissionFeeChannel.DEFAULT_SCOPE_CRYPTO);
            }
            rs.put("fiatCommissionSlotYn", cryptoOnly ? "N" : "Y");
        } else if (!rs.containsKey("fiatCommissionSlotYn")) {
            rs.put("fiatCommissionSlotYn", "Y");
        }
        try {
            mp.setRegionalSettings(JSON.writeValueAsString(rs));
            merchantProfileRepository.save(mp);
        } catch (Exception ignored) {
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
