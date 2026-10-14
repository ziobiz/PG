package com.pg.controller;

import com.pg.crypto.CryptoCheckoutService;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.urlpay.UrlPayCheckoutMoveService;
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
 *   <li>{@code /checkout-crypto/{compId}} — 크립토 결제 전용</li>
 *   <li>{@code /checkout-all/{compId}} — 카드+크립토 수단 선택(통합)</li>
 * </ul>
 */
@Controller
public class NeutralCheckoutRouteController {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ChillPayService chillPayService;
    private final UrlPayCheckoutMoveService urlPayCheckoutMoveService;
    private final CryptoCheckoutService cryptoCheckoutService;

    public NeutralCheckoutRouteController(OrgUnitRepository orgUnitRepository,
                                          MerchantProfileRepository merchantProfileRepository,
                                          ChillPayService chillPayService,
                                          UrlPayCheckoutMoveService urlPayCheckoutMoveService,
                                          CryptoCheckoutService cryptoCheckoutService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.chillPayService = chillPayService;
        this.urlPayCheckoutMoveService = urlPayCheckoutMoveService;
        this.cryptoCheckoutService = cryptoCheckoutService;
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

    /** 크립토 결제 전용. */
    @GetMapping("/checkout-crypto/{compId}")
    public String checkoutCrypto(@PathVariable("compId") String compId, HttpServletRequest request) {
        return "forward:" + resolveCryptoCheckoutPath(compId, request);
    }

    /** 통합(카드+크립토) 수단 선택. 한쪽만 활성이면 해당 창으로 직행. */
    @GetMapping("/checkout-all/{compId}")
    public String checkoutAll(@PathVariable("compId") String compId, HttpServletRequest request) {
        String mid = compId != null ? compId.trim() : "";
        String q = buildQuery(mid, request);
        try {
            Long orgUnitId = orgUnitRepository.findByCode(mid).map(OrgUnit::getId).orElse(null);
            if (orgUnitId != null) {
                boolean cryptoOn = cryptoCheckoutService.cryptoMeansAllowed(orgUnitId);
                boolean cardOn = cardUrlMeansAllowed(orgUnitId);
                if (cryptoOn && cardOn) {
                    return "forward:/checkout-means.html" + q;
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
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
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
        if (opPg == null || opPg.isBlank()) {
            return false;
        }
        return !PgVendor.isCryptoFamily(opPg);
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
        String session = nz(request.getParameter("session"));
        if (!session.isEmpty()) {
            q = q + (q.contains("?") ? "&" : "?") + "session=" + URLEncoder.encode(session, StandardCharsets.UTF_8);
        }
        String entry = nz(request.getParameter("entry"));
        if (!entry.isEmpty()) {
            q = q + (q.contains("?") ? "&" : "?") + "entry=" + URLEncoder.encode(entry, StandardCharsets.UTF_8);
        }
        String lang = nz(request.getParameter("lang"));
        if (lang.isEmpty()) {
            lang = nz(request.getParameter("langCode"));
        }
        if (!lang.isEmpty()) {
            q = q + (q.contains("?") ? "&" : "?") + "lang=" + URLEncoder.encode(lang, StandardCharsets.UTF_8);
        }
        return q;
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
