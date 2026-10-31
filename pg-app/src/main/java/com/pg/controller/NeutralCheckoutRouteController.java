package com.pg.controller;

import com.pg.crypto.CryptoCheckoutService;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.urlpay.QrPaymentMeansService;
import com.pg.urlpay.UrlPayCheckoutMoveService;
import com.pg.util.QrWalletScopeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 중립 결제 라우트.
 * <ul>
 *   <li>{@code /checkout/{compId}} — 카드(URL) 결제 전용</li>
 *   <li>{@code /checkout-qr/{compId}} — QR 결제 전용 (PayPay·QBANG)</li>
 *   <li>{@code /checkout-crypto/{compId}} — 크립토 결제 전용</li>
 *   <li>{@code /checkout-all/{compId}} — 카드·QR·크립토 중 2개 이상이면 수단 선택(통합)</li>
 * </ul>
 */
@Controller
public class NeutralCheckoutRouteController {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ChillPayService chillPayService;
    private final UrlPayCheckoutMoveService urlPayCheckoutMoveService;
    private final CryptoCheckoutService cryptoCheckoutService;
    private final MerchantPgBindingRouterService pgBindingRouter;
    private final QrPaymentMeansService qrPaymentMeansService;

    public NeutralCheckoutRouteController(OrgUnitRepository orgUnitRepository,
                                          MerchantProfileRepository merchantProfileRepository,
                                          ChillPayService chillPayService,
                                          UrlPayCheckoutMoveService urlPayCheckoutMoveService,
                                          CryptoCheckoutService cryptoCheckoutService,
                                          MerchantPgBindingRouterService pgBindingRouter,
                                          QrPaymentMeansService qrPaymentMeansService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.chillPayService = chillPayService;
        this.urlPayCheckoutMoveService = urlPayCheckoutMoveService;
        this.cryptoCheckoutService = cryptoCheckoutService;
        this.pgBindingRouter = pgBindingRouter;
        this.qrPaymentMeansService = qrPaymentMeansService;
    }

    /** 카드(URL) 결제 — 수단 선택 없음. */
    @GetMapping("/checkout/{compId}")
    public String checkout(@PathVariable("compId") String compId, HttpServletRequest request) {
        if (!urlPayCheckoutMoveService.skipBecauseHop(request)) {
            var move = urlPayCheckoutMoveService.resolve(compId, request);
            if (move.isPresent()) {
                var r = move.get();
                if (r.auto()) {
                    return "redirect:" + r.targetUrl();
                }
                if (r.direct()) {
                    return "forward:/checkout-move.html";
                }
            }
        }
        return "forward:" + resolveCardCheckoutPath(compId, request);
    }

    /** QR 결제 전용 (크립토 /checkout-crypto 와 대칭). */
    @GetMapping("/checkout-qr/{compId}")
    public String checkoutQr(@PathVariable("compId") String compId, HttpServletRequest request) {
        String mid = compId != null ? compId.trim() : "";
        String q = buildQuery(mid, request);
        try {
            Long orgUnitId = orgUnitRepository.findByCode(mid).map(OrgUnit::getId).orElse(null);
            if (orgUnitId != null && qrPaymentMeansService.qrMeansAllowed(orgUnitId)) {
                return "forward:" + resolveQrPagePath(orgUnitId, q);
            }
        } catch (RuntimeException ignore) {
            // fall through
        }
        return "forward:/checkout-means.html" + q;
    }

    /** 크립토 결제 전용. */
    @GetMapping("/checkout-crypto/{compId}")
    public String checkoutCrypto(@PathVariable("compId") String compId, HttpServletRequest request) {
        return "forward:" + resolveCryptoCheckoutPath(compId, request);
    }

    /** 통합(카드·QR·크립토) 수단 선택. 한쪽만 활성이면 해당 창으로 직행. */
    @GetMapping("/checkout-all/{compId}")
    public String checkoutAll(@PathVariable("compId") String compId, HttpServletRequest request) {
        String mid = compId != null ? compId.trim() : "";
        String q = buildQuery(mid, request);
        try {
            Long orgUnitId = orgUnitRepository.findByCode(mid).map(OrgUnit::getId).orElse(null);
            if (orgUnitId != null) {
                boolean cryptoOn = cryptoCheckoutService.cryptoMeansAllowed(orgUnitId);
                boolean cardOn = cardUrlMeansAllowed(orgUnitId);
                boolean qrOn = qrPaymentMeansService.qrMeansAllowed(orgUnitId);
                int means = (cryptoOn ? 1 : 0) + (cardOn ? 1 : 0) + (qrOn ? 1 : 0);
                if (means >= 2) {
                    return "forward:/checkout-means.html" + q;
                }
                if (qrOn) {
                    return "forward:" + resolveQrPagePath(orgUnitId, q);
                }
                if (cryptoOn) {
                    return "forward:/crypto-pay.html" + q;
                }
                if (cardOn) {
                    return "forward:" + resolveCardPagePath(orgUnitId, q);
                }
            }
        } catch (RuntimeException ignore) {
            // fall through
        }
        return "forward:/checkout-means.html" + q;
    }

    private String resolveCardCheckoutPath(String compId, HttpServletRequest request) {
        String mid = compId != null ? compId.trim() : "";
        String q = buildQuery(mid, request);
        String session = request != null ? nz(request.getParameter("session")) : "";
        try {
            Long orgUnitId = orgUnitRepository.findByCode(mid).map(OrgUnit::getId).orElse(null);
            if (orgUnitId != null) {
                if (session.startsWith("sb_")) {
                    return "/sandbox-pay.html" + q;
                }
                /* API prepare 크립토 세션·레거시 payMeans=CRYPTO 호환 */
                if (session.startsWith("cr_")
                        || wantsCryptoMeans(request)
                        || isCryptoSession(session)) {
                    return "/crypto-pay.html" + q;
                }
                if (wantsQrMeans(request)) {
                    return resolveQrPagePath(orgUnitId, q);
                }
                return resolveCardPagePath(orgUnitId, q);
            }
        } catch (RuntimeException ignore) {
            // 조회 실패 시 기본 결제창
        }
        return "/pay.html" + (q.isEmpty() ? "?pgCheckoutFwd=1" : (q + "&pgCheckoutFwd=1"));
    }

    private String resolveCryptoCheckoutPath(String compId, HttpServletRequest request) {
        String mid = compId != null ? compId.trim() : "";
        return "/crypto-pay.html" + buildQuery(mid, request);
    }

    private String resolveCardPagePath(Long orgUnitId, String q) {
        String opPg = pgBindingRouter.resolveOperationalPgCd(
                orgUnitId, MerchantPgBindingRouterService.RoutingHint.standard("", ""));
        if (opPg == null || opPg.isBlank()) {
            if (qrPaymentMeansService.qrMeansAllowed(orgUnitId)) {
                return resolveQrPagePath(orgUnitId, q);
            }
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }
        if (opPg != null && !opPg.isBlank()) {
            if (PgVendor.isJpayFamily(opPg)) {
                return "/jpay-pay.html" + q;
            }
            if (PgVendor.isEximbayFamily(opPg)) {
                return "/eximbay-pay.html" + q;
            }
            if (PgVendor.isElementPayFamily(opPg)) {
                return "/elementpay-pay.html" + q;
            }
            if (PgVendor.isIlkFamily(opPg)) {
                return "/ilk-pay.html" + q;
            }
            if (PgVendor.isOxFamily(opPg)) {
                return "/ox-pay.html" + q;
            }
            if (PgVendor.isQxipayFamily(opPg)) {
                return "/qxipay-pay.html" + q;
            }
            if (PgVendor.isOmspayFamily(opPg)) {
                return "/omspay-pay.html" + q;
            }
            if (PgVendor.isPaysoFamily(opPg)) {
                return "/payso-pay.html" + q;
            }
            if (PgVendor.isQbangFamily(opPg)) {
                return "/qbang-pay.html" + withParam(q, "payMeans", "QR");
            }
            if (PgVendor.isCryptoFamily(opPg)) {
                /* 운영 PG가 CRYPTO 만이면 카드 URL에서도 크립토창 — 전용은 /checkout-crypto */
                return "/crypto-pay.html" + q;
            }
        }
        return "/pay.html" + (q.isEmpty() ? "?pgCheckoutFwd=1" : (q + "&pgCheckoutFwd=1"));
    }

    private boolean cardUrlMeansAllowed(Long orgUnitId) {
        if (orgUnitId == null || !cryptoCheckoutService.isMerchantServiceOpen(orgUnitId)) {
            return false;
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (mp == null) {
            return false;
        }
        String web = mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn().trim() : "Y";
        if (!"Y".equalsIgnoreCase(web)) {
            return false;
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank() || PgVendor.isCryptoFamily(opPg)) {
            return false;
        }
        return pgBindingRouter.hasOperationalCardMeans(orgUnitId);
    }

    /** Prefer QBANG QR page when a QBANG wallet is bound; otherwise ILK PayPay QR. */
    private String resolveQrPagePath(Long orgUnitId, String q) {
        String withMeans = withParam(q, "payMeans", "QR");
        if (orgUnitId != null) {
            for (String w : qrPaymentMeansService.listOperationalQrWalletsIfAllowed(orgUnitId)) {
                if (QrWalletScopeUtil.QBANG.equalsIgnoreCase(w)) {
                    return "/qbang-pay.html" + withMeans;
                }
            }
        }
        return "/ilk-pay.html" + withMeans;
    }

    private boolean wantsQrMeans(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String means = nz(request.getParameter("payMeans"));
        if (means.isEmpty()) {
            means = nz(request.getParameter("means"));
        }
        return "QR".equalsIgnoreCase(means);
    }

    private boolean wantsCryptoMeans(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String means = nz(request.getParameter("payMeans"));
        if (means.isEmpty()) {
            means = nz(request.getParameter("means"));
        }
        return "CRYPTO".equalsIgnoreCase(means);
    }

    private boolean isCryptoSession(String session) {
        if (session == null || session.isBlank()) {
            return false;
        }
        Map<String, Object> r = cryptoCheckoutService.session(session);
        Object ok = r.get("success");
        return ok instanceof Boolean && (Boolean) ok;
    }

    private static String buildQuery(String mid, HttpServletRequest request) {
        String q = mid.isEmpty() ? "" : ("?m=" + URLEncoder.encode(mid, StandardCharsets.UTF_8));
        if (request == null) {
            return q;
        }
        /* 결제 복귀(OMSPAY return_url 등) 시 done·orderNo 가 빠지면 결제창이 다시 열려 결제완료 UI가 안 보임 */
        String[] passKeys = {
                "session", "entry", "lang", "langCode", "payMeans", "means",
                "orderNo", "OrderNo", "amount", "currency",
                "done", "omspay", "omspayReturn", "payso", "paysoReturn", "outcome", "status", "icopay_status"
        };
        for (String key : passKeys) {
            String v = nz(request.getParameter(key));
            if (v.isEmpty()) {
                continue;
            }
            String encKey = "langCode".equals(key) ? "lang" : key;
            if (q.contains(encKey + "=")) {
                continue;
            }
            q = q + (q.contains("?") ? "&" : "?") + encKey + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8);
        }
        return q;
    }

    private static String withParam(String q, String key, String value) {
        String base = q != null ? q : "";
        if (base.contains(key + "=")) {
            return base;
        }
        return base + (base.contains("?") ? "&" : "?") + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
