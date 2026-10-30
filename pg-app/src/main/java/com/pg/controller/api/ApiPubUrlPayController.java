package com.pg.controller.api;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.CompService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
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
 * 공개 URL 결제용 웹 QR PNG — 스캔 시 {@code /checkout/{compId}}.
 */
@RestController
@RequestMapping("/api/pub/url-pay")
public class ApiPubUrlPayController {

    private static final Pattern COMP_CODE = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");

    private final OrgUnitRepository orgUnitRepository;
    private final CompService compService;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantChatbotProductService productService;

    public ApiPubUrlPayController(OrgUnitRepository orgUnitRepository,
                                  CompService compService,
                                  OrgServiceUseService orgServiceUseService,
                                  MerchantChatbotProductService productService) {
        this.orgUnitRepository = orgUnitRepository;
        this.compService = compService;
        this.orgServiceUseService = orgServiceUseService;
        this.productService = productService;
    }

    /**
     * {@code GET /api/pub/url-pay/pay-qr?m=가맹점코드&size=280}
     */
    @GetMapping(value = "/pay-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> urlPayQr(@RequestParam(value = "m", required = false) String m,
                                           @RequestParam(value = "compId", required = false) String compIdParam,
                                           @RequestParam(value = "size", required = false, defaultValue = "280") int sizePx,
                                           HttpServletRequest request) {
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
        if (!orgServiceUseService.isOrgServiceActive(oid) || !compService.isWebPaymentQrEffective(oid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String base = productService.resolvePublicCustomerSiteBase(request);
        if (base == null || base.isBlank()) {
            base = inferPublicBase(request);
        }
        base = base.trim().replaceAll("/+$", "");
        String enc = java.net.URLEncoder.encode(code.trim(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        String url = base + "/checkout/" + enc;
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
