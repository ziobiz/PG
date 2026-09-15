package com.pg.urlpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.HqPayCopyTranslationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 구 결제 URL → 신 주소 이동. 표시 문구는 저장 시 1회 번역·DB 저장(표시마다 재번역 금지).
 */
@Service
public class UrlPayCheckoutMoveService {

    private static final Logger log = LoggerFactory.getLogger(UrlPayCheckoutMoveService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    public static final String HOP_PARAM = "cm";

    public static final String DEFAULT_MESSAGE_KO = """
            고객님, 안녕하세요.
            기존에 안내해 드린 결제 링크가 변경되어 번거로움을 드린 점 진심으로 사과드립니다.
            
            원활한 결제 처리를 위해 아래의 변경된 새 주소로 접속하시어 결제를 진행해 주시기를 부탁드립니다.
            아래의 확인을 누르면 자동으로 변경된 새 주소로 이동합니다.
            
            불편을 드려 다시 한번 대단히 죄송합니다.""";

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqPayCopyTranslationService hqPayCopyTranslationService;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;

    public UrlPayCheckoutMoveService(OrgUnitRepository orgUnitRepository,
                                     MerchantProfileRepository merchantProfileRepository,
                                     HqApiConfigRepository hqApiConfigRepository,
                                     HqPayCopyTranslationService hqPayCopyTranslationService,
                                     CheckoutHeaderLogoResolver checkoutHeaderLogoResolver) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqPayCopyTranslationService = hqPayCopyTranslationService;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
    }

    public record Resolved(String mode, String targetUrl, String message, Map<String, String> messageI18n,
                           Long merchantOrgUnitId) {
        public boolean auto() {
            return UrlPayCheckoutMoveModeUtil.AUTO.equals(mode);
        }

        public boolean direct() {
            return UrlPayCheckoutMoveModeUtil.DIRECT.equals(mode);
        }
    }

    public boolean skipBecauseHop(HttpServletRequest req) {
        if (req == null) {
            return false;
        }
        String hop = req.getParameter(HOP_PARAM);
        return hop != null && ("1".equals(hop.trim()) || "true".equalsIgnoreCase(hop.trim()));
    }

    public Optional<Resolved> resolve(String compId, HttpServletRequest req) {
        if (compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        if (!UrlPayCheckoutChannelUtil.isPublicUrlPayCheckout(req)) {
            return Optional.empty();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId.trim());
        if (ou.isEmpty()) {
            return Optional.empty();
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.get().getId());
        if (mpOpt.isEmpty()) {
            return Optional.empty();
        }
        MerchantProfile mp = mpOpt.get();
        HqApiConfig hq = hqOrEmpty();
        String merchantMode = UrlPayCheckoutMoveModeUtil.normalizeMerchant(mp.getUrlPayCheckoutMoveMode());
        if (!UrlPayCheckoutMoveModeUtil.isMoveActive(merchantMode)) {
            return Optional.empty();
        }
        String type = UrlPayCheckoutMoveTargetTypeUtil.normalize(mp.getUrlPayCheckoutMoveTargetType());
        String targetRaw = mp.getUrlPayCheckoutMoveTarget() != null ? mp.getUrlPayCheckoutMoveTarget().trim() : "";
        if (targetRaw.isEmpty()) {
            return Optional.empty();
        }
        String dest = buildTargetUrl(compId.trim(), type, targetRaw, req);
        if (dest == null || dest.isBlank()) {
            return Optional.empty();
        }
        if (isSameCheckout(compId.trim(), dest, req)) {
            return Optional.empty();
        }
        dest = appendHop(dest);
        Map<String, String> i18n;
        String message;
        if (UrlPayCheckoutMoveModeUtil.AUTO.equals(merchantMode)) {
            message = "";
            i18n = Map.of();
        } else {
            message = firstNonBlank(mp.getUrlPayCheckoutMoveMessage(), hqMessage(hq));
            i18n = parseI18n(mp.getUrlPayCheckoutMoveMessageI18n());
            if (i18n.isEmpty()) {
                i18n = parseI18n(hq.getUrlPayCheckoutMoveMessageDefaultI18n());
            }
            if (i18n.isEmpty()) {
                i18n = defaultI18n(message);
            }
        }
        return Optional.of(new Resolved(merchantMode, dest, message, i18n, ou.get().getId()));
    }

    public Map<String, Object> toPublicMap(Resolved r) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (r == null) {
            m.put("active", false);
            return m;
        }
        m.put("active", true);
        m.put("mode", r.mode());
        m.put("targetUrl", r.targetUrl());
        m.put("message", r.message() != null ? r.message() : "");
        if (r.messageI18n() != null && !r.messageI18n().isEmpty()) {
            m.put("messageI18n", r.messageI18n());
        }
        m.put("confirmI18n", confirmI18n());
        if (r.merchantOrgUnitId() != null) {
            Map<String, Object> logo = checkoutHeaderLogoResolver.asMap(r.merchantOrgUnitId());
            if (logo != null) {
                Object logoMode = logo.get("checkoutHeaderLogoMode");
                if (logoMode != null) {
                    m.put("checkoutHeaderLogoMode", logoMode);
                }
                Object logoUrl = logo.get("checkoutHeaderLogoUrl");
                if (logoUrl != null) {
                    m.put("checkoutHeaderLogoUrl", logoUrl);
                }
                Object htmlTitle = logo.get("checkoutHeaderHtmlTitle");
                if (htmlTitle != null) {
                    m.put("checkoutHeaderHtmlTitle", htmlTitle);
                }
            }
        }
        return m;
    }

    public void applyToMerchantProfile(MerchantProfile mp, String modeRaw, String typeRaw,
                                       String targetRaw, String messageRaw) {
        if (mp == null) {
            return;
        }
        String mode = UrlPayCheckoutMoveModeUtil.normalizeMerchant(modeRaw);
        mp.setUrlPayCheckoutMoveMode(mode);
        mp.setUrlPayCheckoutMoveTargetType(UrlPayCheckoutMoveTargetTypeUtil.normalize(typeRaw));
        String target = targetRaw != null ? targetRaw.trim() : "";
        if (target.length() > 500) {
            target = target.substring(0, 500);
        }
        mp.setUrlPayCheckoutMoveTarget(target.isEmpty() ? null : target);
        if (!UrlPayCheckoutMoveModeUtil.DIRECT.equals(mode)) {
            return;
        }
        String text = messageRaw != null ? messageRaw.trim() : "";
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        if (text.isEmpty()) {
            mp.setUrlPayCheckoutMoveMessage(null);
            mp.setUrlPayCheckoutMoveMessageI18n(null);
            return;
        }
        String prev = mp.getUrlPayCheckoutMoveMessage() != null ? mp.getUrlPayCheckoutMoveMessage().trim() : "";
        String prevI18n = mp.getUrlPayCheckoutMoveMessageI18n();
        mp.setUrlPayCheckoutMoveMessage(text);
        if (text.equals(prev) && prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        mp.setUrlPayCheckoutMoveMessageI18n(translateJson(text));
    }

    public void applyHqMessage(HqApiConfig c, String modeRaw, String messageRaw) {
        if (c == null) {
            return;
        }
        c.setUrlPayCheckoutMoveModeDefault(UrlPayCheckoutMoveModeUtil.normalizeHq(modeRaw));
        String text = messageRaw != null ? messageRaw.trim() : "";
        if (text.isEmpty()) {
            text = DEFAULT_MESSAGE_KO;
        }
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        String prev = c.getUrlPayCheckoutMoveMessageDefault() != null ? c.getUrlPayCheckoutMoveMessageDefault().trim() : "";
        String prevI18n = c.getUrlPayCheckoutMoveMessageDefaultI18n();
        c.setUrlPayCheckoutMoveMessageDefault(text);
        if (text.equals(prev) && prevI18n != null && !prevI18n.isBlank() && korMatches(prevI18n, text)) {
            return;
        }
        if (DEFAULT_MESSAGE_KO.equals(text) && (prevI18n == null || prevI18n.isBlank())) {
            try {
                c.setUrlPayCheckoutMoveMessageDefaultI18n(OM.writeValueAsString(builtinDefaultI18n()));
                return;
            } catch (Exception ignored) {
                /* fall through to translate */
            }
        }
        c.setUrlPayCheckoutMoveMessageDefaultI18n(translateJson(text));
    }

    public static Map<String, String> builtinDefaultI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_MESSAGE_KO);
        m.put("ENG", """
                Dear customer,
                We sincerely apologize for the inconvenience caused by the change of the payment link we previously provided.
                
                To complete your payment smoothly, please continue using the new address.
                Click Confirm to go to the new payment address automatically.
                
                We apologize again for the inconvenience.""");
        m.put("JPN", """
                お客様へ
                以前ご案内した決済リンクが変更となり、ご不便をおかけしたこと心よりお詫び申し上げます。
                
                円滑な決済のため、新しいアドレスからお手続きをお願いいたします。
                「確認」を押すと、新しい決済アドレスへ自動で移動します。
                
                ご不便をおかけし、重ねてお詫び申し上げます。""");
        m.put("CHN", """
                尊敬的顾客：
                此前提供的支付链接已变更，给您带来不便，我们深表歉意。
                
                为顺利完成支付，请通过新地址继续办理。
                点击「确认」将自动跳转至新的支付地址。
                
                再次为给您带来的不便致歉。""");
        m.put("THA", """
                เรียนลูกค้า
                ขออภัยอย่างยิ่งที่ลิงก์ชำระเงินที่แจ้งไว้เดิมมีการเปลี่ยนแปลง และอาจทำให้ท่านไม่สะดวก
                
                เพื่อให้การชำระเงินเป็นไปอย่างราบรื่น กรุณาใช้ที่อยู่ใหม่
                กด「ยืนยัน」เพื่อไปยังที่อยู่ชำระเงินใหม่โดยอัตโนมัติ
                
                ขออภัยอีกครั้งในความไม่สะดวก""");
        return m;
    }

    public static Map<String, String> confirmI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "확인");
        m.put("ENG", "Confirm");
        m.put("JPN", "確認");
        m.put("CHN", "确认");
        m.put("THA", "ยืนยัน");
        return m;
    }

    private String buildTargetUrl(String currentCompId, String type, String targetRaw, HttpServletRequest req) {
        if (UrlPayCheckoutMoveTargetTypeUtil.URL.equals(type)) {
            return sanitizeExternalUrl(targetRaw);
        }
        String code = targetRaw.trim();
        if (code.toLowerCase(Locale.ROOT).startsWith("http://")
                || code.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return sanitizeExternalUrl(code);
        }
        if (!code.matches("[A-Za-z0-9._-]{1,32}")) {
            return null;
        }
        if (code.equalsIgnoreCase(currentCompId)) {
            return null;
        }
        String base = requestBase(req);
        String path = NeutralCheckoutRoute.PATH + code;
        String qs = mergeQuery(req, code);
        return (base.isEmpty() ? path : base + path) + qs;
    }

    private static String sanitizeExternalUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String u = raw.trim();
        if (u.isEmpty()) {
            return null;
        }
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            return null;
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            if (u.startsWith("/")) {
                return null;
            }
            u = "https://" + u;
            lower = u.toLowerCase(Locale.ROOT);
        }
        try {
            URI uri = URI.create(u);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return uri.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSameCheckout(String currentCompId, String dest, HttpServletRequest req) {
        try {
            URI uri = URI.create(dest);
            String path = uri.getPath() != null ? uri.getPath() : "";
            String prefix = NeutralCheckoutRoute.PATH;
            if (path.startsWith(prefix)) {
                String code = path.substring(prefix.length());
                int slash = code.indexOf('/');
                if (slash >= 0) {
                    code = code.substring(0, slash);
                }
                if (currentCompId.equalsIgnoreCase(code)) {
                    String destHost = uri.getHost();
                    String curHost = req != null ? req.getServerName() : null;
                    String xf = req != null ? req.getHeader("X-Forwarded-Host") : null;
                    if (xf != null && xf.contains(",")) {
                        xf = xf.split(",")[0].trim();
                    }
                    if (xf != null && xf.contains(":")) {
                        xf = xf.split(":")[0].trim();
                    }
                    String host = xf != null && !xf.isBlank() ? xf : curHost;
                    return destHost == null || host == null || destHost.equalsIgnoreCase(host);
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static String appendHop(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        try {
            URI uri = URI.create(url);
            String q = uri.getRawQuery();
            if (q != null && (q.contains(HOP_PARAM + "=1") || q.contains(HOP_PARAM + "=true"))) {
                return url;
            }
            String add = HOP_PARAM + "=1";
            if (q == null || q.isBlank()) {
                return url + (url.contains("?") ? "&" : "?") + add;
            }
            return url + "&" + add;
        } catch (Exception e) {
            return url.contains("?") ? url + "&" + HOP_PARAM + "=1" : url + "?" + HOP_PARAM + "=1";
        }
    }

    private static String mergeQuery(HttpServletRequest req, String newCode) {
        if (req == null) {
            return "?" + HOP_PARAM + "=1&m=" + enc(newCode);
        }
        StringBuilder q = new StringBuilder();
        var names = req.getParameterMap();
        if (names != null) {
            names.forEach((k, vals) -> {
                if (k == null) {
                    return;
                }
                String key = k.trim();
                if (HOP_PARAM.equalsIgnoreCase(key) || "m".equalsIgnoreCase(key)) {
                    return;
                }
                if (vals == null) {
                    return;
                }
                for (String v : vals) {
                    if (q.length() > 0) {
                        q.append('&');
                    }
                    q.append(enc(key)).append('=').append(enc(v != null ? v : ""));
                }
            });
        }
        if (q.length() > 0) {
            q.append('&');
        }
        q.append(HOP_PARAM).append("=1&m=").append(enc(newCode));
        return "?" + q;
    }

    private static String requestBase(HttpServletRequest req) {
        if (req == null) {
            return "";
        }
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = req.getScheme();
        }
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = req.getServerName();
            int port = req.getServerPort();
            if (("http".equalsIgnoreCase(scheme) && port != 80)
                    || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        } else if (host.contains(",")) {
            host = host.split(",")[0].trim();
        }
        return scheme + "://" + host;
    }

    private HqApiConfig hqOrEmpty() {
        return hqApiConfigRepository.findAll().stream().findFirst().orElseGet(HqApiConfig::new);
    }

    private static String hqMessage(HqApiConfig hq) {
        if (hq != null && hq.getUrlPayCheckoutMoveMessageDefault() != null
                && !hq.getUrlPayCheckoutMoveMessageDefault().isBlank()) {
            return hq.getUrlPayCheckoutMoveMessageDefault().trim();
        }
        return DEFAULT_MESSAGE_KO;
    }

    private String translateJson(String text) {
        try {
            Map<String, String> langs = hqPayCopyTranslationService.translateLineFromKo(text);
            if (langs == null || langs.isEmpty()) {
                langs = fallbackAll(text);
            } else if (!langs.containsKey("KOR")) {
                langs.put("KOR", text);
            }
            return OM.writeValueAsString(langs);
        } catch (Exception e) {
            log.warn("Checkout-move i18n failed: {}", e.getMessage());
            try {
                return OM.writeValueAsString(fallbackAll(text));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static boolean korMatches(String i18nJson, String text) {
        Map<String, String> m = parseI18n(i18nJson);
        String kor = m.get("KOR");
        return kor != null && kor.trim().equals(text.trim());
    }

    private static Map<String, String> parseI18n(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> raw = OM.readValue(json, new TypeReference<>() {});
            Map<String, String> out = new LinkedHashMap<>();
            if (raw != null) {
                raw.forEach((k, v) -> {
                    if (k != null && v != null && !v.isBlank()) {
                        out.put(k.trim().toUpperCase(Locale.ROOT), v.trim());
                    }
                });
            }
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, String> defaultI18n(String text) {
        if (text != null && text.trim().equals(DEFAULT_MESSAGE_KO.trim())) {
            return builtinDefaultI18n();
        }
        return fallbackAll(text);
    }

    private static Map<String, String> fallbackAll(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        String t = text != null ? text : "";
        m.put("KOR", t);
        m.put("ENG", t);
        m.put("JPN", t);
        m.put("CHN", t);
        m.put("THA", t);
        return m;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b != null ? b.trim() : "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }
}
