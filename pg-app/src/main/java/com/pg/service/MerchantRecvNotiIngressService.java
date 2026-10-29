package com.pg.service;

import com.pg.crypto.CryptoNotiMiddlewareMirrorService;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.NotiProvisionLog;
import com.pg.integration.pg.PgVendor;
import com.pg.noti.NotiProvisionClient;
import com.pg.repository.MerchantCryptoConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.NotiProvisionLogRepository;
import com.pg.util.QrWalletScopeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 가맹 상세 「수신통보」— 운영 바인딩·크립토·QR·노티생성이력 기준 PG별 NOTI 입구 목록.
 * 구매자 노출용이 아니라 본사/운영 UI용(PG명 표시 허용).
 */
@Service
public class MerchantRecvNotiIngressService {

    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final MerchantCryptoConfigRepository merchantCryptoConfigRepository;
    private final NotiProvisionLogRepository notiProvisionLogRepository;
    private final HqNotifyEnvService hqNotifyEnvService;

    public MerchantRecvNotiIngressService(MerchantPgBindingRepository merchantPgBindingRepository,
                                         MerchantCryptoConfigRepository merchantCryptoConfigRepository,
                                         NotiProvisionLogRepository notiProvisionLogRepository,
                                         HqNotifyEnvService hqNotifyEnvService) {
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantCryptoConfigRepository = merchantCryptoConfigRepository;
        this.notiProvisionLogRepository = notiProvisionLogRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForMerchant(Long orgUnitId, String storedNotifyUrl, String storedCallbackUrl) {
        if (orgUnitId == null) {
            return List.of();
        }
        String notiBase = resolveNotiBase();
        Set<String> kinds = new LinkedHashSet<>();
        Map<String, String> kindHints = new LinkedHashMap<>();

        List<MerchantPgBinding> bindings = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        for (MerchantPgBinding b : bindings) {
            if (b == null || !"Y".equalsIgnoreCase(nz(b.getOperationalYn()))) {
                continue;
            }
            if (b.getActivationYn() != null && "N".equalsIgnoreCase(b.getActivationYn().trim())) {
                continue;
            }
            String pm = nz(b.getPayMethod()).toUpperCase(Locale.ROOT);
            if (!pm.isEmpty() && !"WEB".equals(pm)) {
                continue;
            }
            String pgCd = nz(b.getPgCd());
            String kind = kindFromPgCd(pgCd);
            if (!kind.isEmpty()) {
                kinds.add(kind);
                kindHints.merge(kind, shortPgLabel(pgCd), (a, c) -> a.contains(c) ? a : a + ", " + c);
            }
            String qr = nz(b.getQrWalletScope()).toUpperCase(Locale.ROOT);
            if (!qr.isEmpty() && !"NONE".equals(qr) && !"-".equals(qr)) {
                if (qr.contains(QrWalletScopeUtil.QBANG) || qr.contains("QBANK")) {
                    kinds.add("qbang");
                    kindHints.putIfAbsent("qbang", "QBANG QR");
                }
                if (qr.contains(QrWalletScopeUtil.PAYPAY) || qr.contains("PAYPAY")) {
                    kinds.add("ilk");
                    kindHints.merge("ilk", "PayPay QR", (a, c) -> a.contains("PayPay") ? a : a + ", " + c);
                }
            }
        }

        merchantCryptoConfigRepository.findById(orgUnitId).ifPresent(cfg -> {
            if (cfg != null && "Y".equalsIgnoreCase(nz(cfg.getUseYn()))) {
                kinds.add("crypto");
                kindHints.putIfAbsent("crypto", "CRYPTO");
            }
        });

        Map<String, NotiProvisionLog> latestByKind = new LinkedHashMap<>();
        for (NotiProvisionLog pl : notiProvisionLogRepository.findByOrgUnitIdOrderByProvisionedAtDescIdDesc(orgUnitId)) {
            if (pl == null) {
                continue;
            }
            String k = NotiProvisionClient.normalizePgKind(pl.getPgKind());
            latestByKind.putIfAbsent(k, pl);
            kinds.add(k);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String kind : kinds) {
            rows.add(buildRow(kind, kindHints.get(kind), notiBase, latestByKind.get(kind),
                    storedNotifyUrl, storedCallbackUrl));
        }
        rows.sort((a, b) -> String.valueOf(a.get("label")).compareToIgnoreCase(String.valueOf(b.get("label"))));
        return rows;
    }

    private Map<String, Object> buildRow(String kind, String hint, String notiBase,
                                         NotiProvisionLog log,
                                         String storedNotify, String storedCallback) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pgKind", kind);
        m.put("label", labelForKind(kind));
        if (hint != null && !hint.isBlank()) {
            m.put("bindingHint", hint);
        }
        String[] fixed = fixedUrls(kind, notiBase);
        String webhook = fixed[0];
        String result = fixed[1];
        boolean provisioned = log != null;
        if (log != null) {
            if (log.getJpayNotifyUrl() != null && !log.getJpayNotifyUrl().isBlank()) {
                webhook = log.getJpayNotifyUrl().trim();
            }
            if (log.getJpayCallbackUrl() != null && !log.getJpayCallbackUrl().isBlank()) {
                result = log.getJpayCallbackUrl().trim();
            }
        }
        /* JPAY 슬롯: 가맹 저장 URL 우선 */
        if ("jpay".equals(kind)) {
            if (storedNotify != null && !storedNotify.isBlank()) {
                webhook = storedNotify.trim();
            }
            if (storedCallback != null && !storedCallback.isBlank()) {
                result = storedCallback.trim();
            }
            provisioned = provisioned || (webhook != null && !webhook.isBlank());
        }
        /* CRYPTO: 고정 입구 — 노티 가맹 생성 없이도 ICOPAY→NOTI 미러 */
        if ("crypto".equals(kind)) {
            provisioned = true;
            m.put("provisionMode", "FIXED_MIRROR");
        } else if (NotiProvisionClient.isFixedIngressPg(kind)) {
            m.put("provisionMode", "FIXED_INGRESS");
        } else {
            m.put("provisionMode", "SLOT");
        }
        m.put("webhookUrl", webhook != null ? webhook : "");
        m.put("resultUrl", result != null ? result : "");
        m.put("provisioned", provisioned);
        m.put("status", provisioned ? "READY" : "NEED_PROVISION");
        return m;
    }

    private static String[] fixedUrls(String kind, String notiBase) {
        String b = notiBase != null ? notiBase.replaceAll("/+$", "") : "https://noti.icopay.net";
        return switch (kind) {
            case "elementpay" -> new String[]{b + "/noti/elementpay", b + "/noti/result/elementpay"};
            case "ox" -> new String[]{b + "/noti/ox", b + "/noti/result/ox"};
            case "ilk" -> new String[]{b + "/noti/ilk", b + "/noti/result/ilk"};
            case "qxipay" -> new String[]{b + "/noti/qxipay", b + "/noti/result/qxipay"};
            case "omspay" -> new String[]{b + "/noti/omspay", b + "/noti/result/omspay"};
            case "qbang" -> new String[]{b + "/noti/qbang", b + "/noti/result/qbang"};
            case "crypto" -> {
                Map<String, String> u = CryptoNotiMiddlewareMirrorService.fixedNotiUrls(b);
                yield new String[]{u.get("cryptoWebhookUrl"), u.get("cryptoResultUrl")};
            }
            default -> new String[]{"", ""};
        };
    }

    private static String kindFromPgCd(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return "";
        }
        if (PgVendor.isCryptoFamily(pgCd)) {
            return "crypto";
        }
        if (PgVendor.isQbangFamily(pgCd)) {
            return "qbang";
        }
        if (PgVendor.isOmspayFamily(pgCd)) {
            return "omspay";
        }
        if (PgVendor.isQxipayFamily(pgCd)) {
            return "qxipay";
        }
        if (PgVendor.isIlkFamily(pgCd)) {
            return "ilk";
        }
        if (PgVendor.isOxFamily(pgCd)) {
            return "ox";
        }
        if (PgVendor.isElementPayFamily(pgCd)) {
            return "elementpay";
        }
        if (PgVendor.isJpayFamily(pgCd)) {
            return "jpay";
        }
        String u = pgCd.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("CHILL") || "CHILLPAY".equals(u)) {
            return "jpay";
        }
        return "";
    }

    private static String labelForKind(String kind) {
        return switch (kind) {
            case "elementpay" -> "ElementPay";
            case "ox" -> "ox";
            case "ilk" -> "ILK";
            case "qxipay" -> "QXIPAY";
            case "omspay" -> "OMSPAY";
            case "qbang" -> "QBANG";
            case "crypto" -> "CRYPTO";
            case "jpay" -> "JPAY";
            default -> kind != null ? kind.toUpperCase(Locale.ROOT) : "";
        };
    }

    private static String shortPgLabel(String pgCd) {
        return pgCd != null ? pgCd.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String resolveNotiBase() {
        try {
            HqNotifyEnvConfig c = hqNotifyEnvService.getOrCreate();
            if (c != null && c.getNotiProvisionBaseUrl() != null && !c.getNotiProvisionBaseUrl().isBlank()) {
                return c.getNotiProvisionBaseUrl().trim().replaceAll("/+$", "");
            }
        } catch (RuntimeException ignored) {
            /* fall through */
        }
        return NotiProvisionClient.defaultBaseUrlIfBlank(null);
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
