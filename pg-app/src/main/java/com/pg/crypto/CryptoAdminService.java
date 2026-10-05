package com.pg.crypto;

import com.pg.entity.CryptoCustodyAddress;
import com.pg.entity.CryptoDepositTxn;
import com.pg.entity.MerchantCryptoConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CryptoCustodyAddressRepository;
import com.pg.repository.CryptoDepositTxnRepository;
import com.pg.repository.MerchantCryptoConfigRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CryptoAdminService {

    private final CryptoCatalogService catalogService;
    private final CryptoCustodyAddressRepository custodyAddressRepository;
    private final CryptoDepositTxnRepository depositTxnRepository;
    private final MerchantCryptoConfigRepository configRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final CryptoCheckoutService checkoutService;
    private final CryptoPayoutService payoutService;

    public CryptoAdminService(CryptoCatalogService catalogService,
                              CryptoCustodyAddressRepository custodyAddressRepository,
                              CryptoDepositTxnRepository depositTxnRepository,
                              MerchantCryptoConfigRepository configRepository,
                              OrgUnitRepository orgUnitRepository,
                              CryptoCheckoutService checkoutService,
                              CryptoPayoutService payoutService) {
        this.catalogService = catalogService;
        this.custodyAddressRepository = custodyAddressRepository;
        this.depositTxnRepository = depositTxnRepository;
        this.configRepository = configRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.checkoutService = checkoutService;
        this.payoutService = payoutService;
    }

    public Map<String, Object> hqSettings() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("catalog", catalogService.publicCatalog(null, null));
        List<Map<String, Object>> custody = new ArrayList<>();
        for (CryptoCustodyAddress a : custodyAddressRepository.findByUseYnOrderByIdAsc("Y")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("networkCode", a.getNetworkCode());
            m.put("assetCode", a.getAssetCode());
            m.put("address", a.getAddress());
            m.put("label", a.getLabel());
            m.put("useYn", a.getUseYn());
            custody.add(m);
        }
        out.put("custodyAddresses", custody);
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
        custodyAddressRepository.save(a);
        return hqSettings();
    }

    public Map<String, Object> merchantConfig(String compId) {
        OrgUnit ou = orgUnitRepository.findByCode(compId.trim())
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 설정할 수 있습니다.");
        }
        MerchantCryptoConfig cfg = configRepository.findById(ou.getId()).orElseGet(() -> {
            MerchantCryptoConfig c = new MerchantCryptoConfig();
            c.setOrgUnitId(ou.getId());
            return c;
        });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compId", ou.getCode());
        m.put("compNm", ou.getName());
        m.put("orgUnitId", ou.getId());
        m.put("useYn", cfg.getUseYn());
        m.put("checkoutMeans", cfg.getCheckoutMeans());
        m.put("settleMode", cfg.getSettleMode());
        m.put("payoutMode", cfg.getPayoutMode());
        m.put("holdDays", cfg.getHoldDays());
        m.put("assetsCsv", cfg.getAssetsCsv());
        m.put("networksCsv", cfg.getNetworksCsv());
        m.put("sellerWalletJson", cfg.getSellerWalletJson());
        m.put("catalog", catalogService.publicCatalog(null, null));
        return m;
    }

    @Transactional
    public Map<String, Object> saveMerchantConfig(Map<String, Object> body) {
        String compId = str(body.get("compId"));
        OrgUnit ou = orgUnitRepository.findByCode(compId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        MerchantCryptoConfig cfg = configRepository.findById(ou.getId()).orElseGet(() -> {
            MerchantCryptoConfig c = new MerchantCryptoConfig();
            c.setOrgUnitId(ou.getId());
            return c;
        });
        cfg.setUseYn("Y".equalsIgnoreCase(str(body.get("useYn"))) ? "Y" : "N");
        String means = str(body.get("checkoutMeans")).toUpperCase(Locale.ROOT);
        if (!Set.of("CARD", "CRYPTO", "BOTH").contains(means)) {
            means = MerchantCryptoConfig.MEANS_CARD;
        }
        cfg.setCheckoutMeans(means);
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
        String assets = str(body.get("assetsCsv"));
        cfg.setAssetsCsv(assets.isBlank() ? "USDT" : assets.toUpperCase(Locale.ROOT));
        String nets = str(body.get("networksCsv"));
        cfg.setNetworksCsv(nets.isBlank() ? "TRC20,ERC20" : nets.toUpperCase(Locale.ROOT));
        if (body.containsKey("sellerWalletJson")) {
            cfg.setSellerWalletJson(str(body.get("sellerWalletJson")));
        }
        configRepository.save(cfg);
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

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
