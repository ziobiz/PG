package com.pg.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.OrgLevel;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 링크 미리보기(Open Graph) JSON·상속·HTML 이스케이프.
 * 총본사는 항상 CUSTOM. 본사 FOLLOW_HQ → 총본사. 총판 FOLLOW_HQ → 본사 CUSTOM 아니면 총본사.
 */
public final class LinkPreviewOgSupport {

    public static final String MODE_FOLLOW_HQ = "FOLLOW_HQ";
    public static final String MODE_CUSTOM = "CUSTOM";
    public static final String DEFAULT_TITLE = "ICOPAY";

    public static final String[] LANGS = { "KO", "EN", "JP", "CH", "TH" };

    public enum OgSource {
        SELF, PARENT, HQ, DEFAULT
    }

    private static final ObjectMapper OM = new ObjectMapper();

    private static final Map<String, String> DEFAULT_DESC = Map.of(
            "KO", "결제·정산을 위한 ICOPAY 관리자입니다.",
            "EN", "ICOPAY payment and settlement platform.",
            "JP", "決済・精算のための ICOPAY 管理画面です。",
            "CH", "ICOPAY 支付与结算管理平台。",
            "TH", "แพลตฟอร์มชำระเงินและชำระยอด ICOPAY"
    );

    private LinkPreviewOgSupport() {
    }

    public static String normalizeMode(String mode, OrgLevel level) {
        if (level == OrgLevel.HEADQUARTERS) {
            return MODE_CUSTOM;
        }
        if (mode != null && MODE_CUSTOM.equalsIgnoreCase(mode.trim())) {
            return MODE_CUSTOM;
        }
        return MODE_FOLLOW_HQ;
    }

    public static OgSource cascade(OrgLevel level, String ownMode, OrgLevel parentLevel, String parentMode) {
        if (level == OrgLevel.HEADQUARTERS) {
            return OgSource.SELF;
        }
        if (MODE_CUSTOM.equals(normalizeMode(ownMode, level))) {
            return OgSource.SELF;
        }
        if (level == OrgLevel.MASTER_DIST
                && parentLevel == OrgLevel.REGIONAL
                && MODE_CUSTOM.equals(normalizeMode(parentMode, OrgLevel.REGIONAL))) {
            return OgSource.PARENT;
        }
        if (level == OrgLevel.REGIONAL || level == OrgLevel.MASTER_DIST) {
            return OgSource.HQ;
        }
        return OgSource.DEFAULT;
    }

    public static Map<String, String> parseLangMap(String json) {
        Map<String, String> out = emptyLangMap();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            Map<String, Object> raw = OM.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (raw == null) {
                return out;
            }
            for (String lang : LANGS) {
                Object v = raw.get(lang);
                if (v == null && "CH".equals(lang)) {
                    v = raw.get("ZH");
                }
                if (v != null) {
                    String s = String.valueOf(v).trim();
                    if (!s.isEmpty()) {
                        out.put(lang, s);
                    }
                }
            }
        } catch (Exception ignored) {
            // keep empty
        }
        return out;
    }

    public static String toJson(Map<String, String> map) {
        Map<String, String> clean = emptyLangMap();
        if (map != null) {
            for (String lang : LANGS) {
                String v = trimToNull(map.get(lang));
                if (v != null) {
                    clean.put(lang, v);
                }
            }
        }
        try {
            return OM.writeValueAsString(clean);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Map<String, String> emptyLangMap() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String lang : LANGS) {
            m.put(lang, "");
        }
        return m;
    }

    public static String pick(Map<String, String> map, String lang) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        String key = lang == null ? "KO" : lang.trim().toUpperCase(Locale.ROOT);
        String v = trimToEmpty(map.get(key));
        if (!v.isEmpty()) {
            return v;
        }
        v = trimToEmpty(map.get("EN"));
        if (!v.isEmpty()) {
            return v;
        }
        return trimToEmpty(map.get("KO"));
    }

    public static String defaultDescription(String lang) {
        String key = lang == null ? "KO" : lang.trim().toUpperCase(Locale.ROOT);
        String v = DEFAULT_DESC.get(key);
        return v != null ? v : DEFAULT_DESC.get("EN");
    }

    public static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public static String escapeAttr(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static boolean isHqLoginHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.trim().toLowerCase(Locale.ROOT);
        int colon = h.indexOf(':');
        if (colon > 0) {
            h = h.substring(0, colon);
        }
        return "api.icopay.co.kr".equals(h)
                || "www.api.icopay.co.kr".equals(h)
                || "icopay.co.kr".equals(h)
                || "www.icopay.co.kr".equals(h);
    }

    public static String toAbsoluteUrl(String maybeRelative, String publicBaseUrl) {
        String u = trimToEmpty(maybeRelative);
        if (u.isEmpty()) {
            return "";
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        String base = trimToEmpty(publicBaseUrl);
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "https://api.icopay.co.kr";
        }
        if (!u.startsWith("/")) {
            u = "/" + u;
        }
        return base + u;
    }

    public static String buildOgBlock(String title, String description, String imageAbs, String pageUrl) {
        String t = escapeAttr(title == null || title.isBlank() ? DEFAULT_TITLE : title);
        String d = escapeAttr(description == null ? "" : description);
        StringBuilder sb = new StringBuilder(512);
        sb.append("<!--PG_OG_START-->\n");
        sb.append("<meta name=\"description\" content=\"").append(d).append("\">\n");
        sb.append("<meta property=\"og:type\" content=\"website\">\n");
        sb.append("<meta property=\"og:title\" content=\"").append(t).append("\">\n");
        sb.append("<meta property=\"og:description\" content=\"").append(d).append("\">\n");
        if (pageUrl != null && !pageUrl.isBlank()) {
            sb.append("<meta property=\"og:url\" content=\"").append(escapeAttr(pageUrl)).append("\">\n");
        }
        if (imageAbs != null && !imageAbs.isBlank()) {
            sb.append("<meta property=\"og:image\" content=\"").append(escapeAttr(imageAbs)).append("\">\n");
        }
        sb.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        sb.append("<meta name=\"twitter:title\" content=\"").append(t).append("\">\n");
        sb.append("<meta name=\"twitter:description\" content=\"").append(d).append("\">\n");
        if (imageAbs != null && !imageAbs.isBlank()) {
            sb.append("<meta name=\"twitter:image\" content=\"").append(escapeAttr(imageAbs)).append("\">\n");
        }
        sb.append("<!--PG_OG_END-->");
        return sb.toString();
    }

    public static String injectOgBlock(String html, String ogBlock, String title) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String out = html;
        int start = out.indexOf("<!--PG_OG_START-->");
        int end = out.indexOf("<!--PG_OG_END-->");
        if (start >= 0 && end > start) {
            out = out.substring(0, start) + ogBlock + out.substring(end + "<!--PG_OG_END-->".length());
        } else {
            int head = indexOfIgnoreCase(out, "<head>");
            if (head >= 0) {
                int insert = head + 6;
                out = out.substring(0, insert) + "\n" + ogBlock + out.substring(insert);
            }
        }
        if (title != null && !title.isBlank()) {
            out = replaceTitle(out, title);
        }
        return out;
    }

    private static String replaceTitle(String html, String title) {
        int open = indexOfIgnoreCase(html, "<title");
        if (open < 0) {
            return html;
        }
        int gt = html.indexOf('>', open);
        int close = indexOfIgnoreCase(html, "</title>");
        if (gt < 0 || close < 0 || close < gt) {
            return html;
        }
        return html.substring(0, gt + 1) + escapeAttr(title) + html.substring(close);
    }

    private static int indexOfIgnoreCase(String hay, String needle) {
        return hay.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
    }
}
