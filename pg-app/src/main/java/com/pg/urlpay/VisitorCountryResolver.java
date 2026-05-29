package com.pg.urlpay;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** URL 공개 결제 — 접속(방문) 국가 ISO2 추정. */
public final class VisitorCountryResolver {

    private static final Pattern LOCALE_REGION = Pattern.compile("^[a-zA-Z]{2,3}[-_]([a-zA-Z]{2})$");

    private VisitorCountryResolver() {
    }

    public static String resolveIso2(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] geoHeaders = {
                "CF-IPCountry",
                "CloudFront-Viewer-Country",
                "X-Country-Code",
                "X-Geo-Country"
        };
        for (String h : geoHeaders) {
            String iso = PhoneDialCodeCatalog.canonicalIso2(request.getHeader(h));
            if (!iso.isEmpty() && !"XX".equals(iso)) {
                return iso;
            }
        }
        return regionFromAcceptLanguage(request.getHeader("Accept-Language"));
    }

    static String regionFromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "";
        }
        String[] parts = acceptLanguage.split(",");
        for (String part : parts) {
            String tag = part.trim();
            int semi = tag.indexOf(';');
            if (semi >= 0) {
                tag = tag.substring(0, semi).trim();
            }
            Matcher m = LOCALE_REGION.matcher(tag);
            if (m.matches()) {
                return PhoneDialCodeCatalog.canonicalIso2(m.group(1));
            }
            if (tag.length() == 2) {
                return PhoneDialCodeCatalog.canonicalIso2(tag);
            }
        }
        return "";
    }
}
