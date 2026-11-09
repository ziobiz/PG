package com.pg.controller.api;

import com.pg.crypto.CryptoCheckoutService;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.CompService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.service.OrgServiceUseService;
import com.pg.urlpay.QrPaymentMeansService;
import com.pg.util.QrCodePngUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 공개 결제 진입 URL PNG QR.
 * <ul>
 *   <li>{@code /pay-qr} — {@code /checkout/{compId}} (카드 URL)</li>
 *   <li>{@code /qr-pay-qr} — {@code /checkout-qr/{compId}} (QR URL)</li>
 *   <li>{@code /unified-pay-qr} — {@code /checkout-all/{compId}} (통합)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pub/url-pay")
public class ApiPubUrlPayController {

    private static final Pattern COMP_CODE = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final CompService compService;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantChatbotProductService productService;
    private final QrPaymentMeansService qrPaymentMeansService;
    private final CryptoCheckoutService cryptoCheckoutService;
    private final ChillPayService chillPayService;
    private final MerchantPgBindingRouterService pgBindingRouter;

    public ApiPubUrlPayController(OrgUnitRepository orgUnitRepository,
                                  MerchantProfileRepository merchantProfileRepository,
                                  CompService compService,
                                  OrgServiceUseService orgServiceUseService,
                                  MerchantChatbotProductService productService,
                                  QrPaymentMeansService qrPaymentMeansService,
                                  CryptoCheckoutService cryptoCheckoutService,
                                  ChillPayService chillPayService,
                                  MerchantPgBindingRouterService pgBindingRouter) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.compService = compService;
        this.orgServiceUseService = orgServiceUseService;
        this.productService = productService;
        this.qrPaymentMeansService = qrPaymentMeansService;
        this.cryptoCheckoutService = cryptoCheckoutService;
        this.chillPayService = chillPayService;
        this.pgBindingRouter = pgBindingRouter;
    }

    /**
     * {@code GET /api/pub/url-pay/pay-qr?m=가맹점코드&size=280}
     */
    @GetMapping(value = "/pay-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> urlPayQr(@RequestParam(value = "m", required = false) String m,
                                           @RequestParam(value = "compId", required = false) String compIdParam,
                                           @RequestParam(value = "size", required = false, defaultValue = "280") int sizePx,
                                           HttpServletRequest request) {
        return pngForCheckoutPath(m, compIdParam, sizePx, request, "/checkout/",
                oid -> orgServiceUseService.isOrgServiceActive(oid) && compService.isWebPaymentQrEffective(oid));
    }

    /**
     * {@code GET /api/pub/url-pay/qr-pay-qr?m=가맹점코드&size=280} — 스캔 시 /checkout-qr
     */
    @GetMapping(value = "/qr-pay-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCheckoutPayQr(@RequestParam(value = "m", required = false) String m,
                                                  @RequestParam(value = "compId", required = false) String compIdParam,
                                                  @RequestParam(value = "size", required = false, defaultValue = "280") int sizePx,
                                                  HttpServletRequest request) {
        return pngForCheckoutPath(m, compIdParam, sizePx, request, "/checkout-qr/", this::qrCheckoutUrlAllowed);
    }

    /**
     * {@code GET /api/pub/url-pay/unified-pay-qr?m=가맹점코드&size=280} — 스캔 시 /checkout-all
     */
    @GetMapping(value = "/unified-pay-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> unifiedCheckoutPayQr(@RequestParam(value = "m", required = false) String m,
                                                       @RequestParam(value = "compId", required = false) String compIdParam,
                                                       @RequestParam(value = "size", required = false, defaultValue = "280") int sizePx,
                                                       HttpServletRequest request) {
        return pngForCheckoutPath(m, compIdParam, sizePx, request, "/checkout-all/", this::unifiedCheckoutUrlAllowed);
    }

    private ResponseEntity<byte[]> pngForCheckoutPath(String m, String compIdParam, int sizePx,
                                                      HttpServletRequest request, String pathPrefix,
                                                      java.util.function.Predicate<Long> allowed) {
        String code = firstNonBlank(m, compIdParam);
        if (code == null || !COMP_CODE.matcher(code).matches()) {
            return ResponseEntity.notFound().build();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(code.trim())
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT);
        if (ou.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Long oid = ou.get().getId();
        if (!allowed.test(oid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String base = productService.resolvePublicCustomerSiteBase(request);
        if (base == null || base.isBlank()) {
            base = inferPublicBase(request);
        }
        base = base.trim().replaceAll("/+$", "");
        String enc = java.net.URLEncoder.encode(code.trim(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        String url = base + pathPrefix + enc;
        int dim = Math.min(512, Math.max(128, sizePx));
        try {
            byte[] png = QrCodePngUtil.encodePng(url, dim);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .body(png);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** UI와 동일: 웹결제 ON + QR 수단 실효 */
    private boolean qrCheckoutUrlAllowed(Long orgUnitId) {
        if (orgUnitId == null || !orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return false;
        }
        if (!isWebPaymentUseOn(orgUnitId)) {
            return false;
        }
        return qrPaymentMeansService.qrMeansAllowed(orgUnitId);
    }

    /** 카드·QR·크립토 중 2개 이상 */
    private boolean unifiedCheckoutUrlAllowed(Long orgUnitId) {
        if (orgUnitId == null || !orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return false;
        }
        boolean cryptoOn = cryptoCheckoutService.cryptoMeansAllowed(orgUnitId);
        boolean cardOn = cardUrlMeansAllowed(orgUnitId);
        boolean qrOn = qrPaymentMeansService.qrMeansAllowed(orgUnitId);
        int means = (cryptoOn ? 1 : 0) + (cardOn ? 1 : 0) + (qrOn ? 1 : 0);
        return means >= 2;
    }

    private boolean isWebPaymentUseOn(Long orgUnitId) {
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (mp == null) {
            return false;
        }
        String web = mp.getWebPaymentUseYn() != null ? mp.getWebPaymentUseYn().trim() : "Y";
        return "Y".equalsIgnoreCase(web);
    }

    /** {@link com.pg.controller.NeutralCheckoutRouteController} 카드 수단 판정과 동일 */
    private boolean cardUrlMeansAllowed(Long orgUnitId) {
        if (orgUnitId == null || !cryptoCheckoutService.isMerchantServiceOpen(orgUnitId)) {
            return false;
        }
        if (!isWebPaymentUseOn(orgUnitId)) {
            return false;
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank() || PgVendor.isCryptoFamily(opPg)) {
            return false;
        }
        return pgBindingRouter.hasOperationalCardMeans(orgUnitId);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String inferPublicBase(HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
