package com.pg.urlpay;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.service.MerchantPgBindingRouterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * QR 수단 실효 여부 — 가맹 사용(FOLLOW_HQ|Y|N) + 운영 WEB QR 바인딩.
 */
@Service
public class QrPaymentMeansService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantPgBindingRouterService pgBindingRouter;

    public QrPaymentMeansService(MerchantProfileRepository merchantProfileRepository,
                                 HqApiConfigRepository hqApiConfigRepository,
                                 MerchantPgBindingRouterService pgBindingRouter) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.pgBindingRouter = pgBindingRouter;
    }

    public boolean qrMeansAllowed(Long orgUnitId) {
        if (orgUnitId == null || !isEffectiveUseOn(orgUnitId)) {
            return false;
        }
        List<String> wallets = pgBindingRouter.listOperationalQrWallets(orgUnitId);
        return wallets != null && !wallets.isEmpty();
    }

    public boolean isEffectiveUseOn(Long orgUnitId) {
        return "Y".equalsIgnoreCase(resolveEffectiveUseYn(orgUnitId));
    }

    public String resolveEffectiveUseYn(Long orgUnitId) {
        if (orgUnitId == null) {
            return "N";
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        String raw = mp != null && mp.getQrPaymentUseYn() != null ? mp.getQrPaymentUseYn().trim() : "FOLLOW_HQ";
        String u = raw.toUpperCase(Locale.ROOT);
        if ("N".equals(u)) {
            return "N";
        }
        if ("Y".equals(u)) {
            return "Y";
        }
        return hqQrUseDefaultYn();
    }

    public String hqQrUseDefaultYn() {
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (hq == null || hq.getQrPaymentUseDefaultYn() == null || hq.getQrPaymentUseDefaultYn().isBlank()) {
            return "Y";
        }
        return "N".equalsIgnoreCase(hq.getQrPaymentUseDefaultYn().trim()) ? "N" : "Y";
    }

    public List<String> listOperationalQrWalletsIfAllowed(Long orgUnitId) {
        if (!qrMeansAllowed(orgUnitId)) {
            return List.of();
        }
        return pgBindingRouter.listOperationalQrWallets(orgUnitId);
    }
}
