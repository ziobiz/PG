package com.pg.urlpay;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Map;

import com.pg.util.PayerDeviceCategoryUtil;

/** 결제 요청 시 pg_trnsctn 적재용 고객 컨텍스트(IP·UA·국가·단말기). */
public final class PayerContextCapture {

    private PayerContextCapture() {
    }

    public static void enrichSaleBody(Map<String, Object> body, HttpServletRequest request, String clientIp) {
        if (body == null) {
            return;
        }
        String ip = clientIp != null && !clientIp.isBlank() ? clientIp.trim() : resolveIpFromRequest(request);
        if (!ip.isBlank()) {
            body.put("_payerClientIp", ip.length() > 64 ? ip.substring(0, 64) : ip);
        }
        String ua = request != null ? request.getHeader("User-Agent") : null;
        if (ua != null && !ua.isBlank()) {
            body.put("_payerUserAgent", ua.length() > 512 ? ua.substring(0, 512) : ua);
            body.put("_payerDeviceCategory", PayerDeviceCategoryUtil.fromUserAgent(ua));
        }
        String geo = VisitorCountryResolver.resolveIso2(request);
        if (geo.isBlank()) {
            geo = JpayBuyerPrefillUtil.canonicalCountryIso2(str(body.get("payCountryIsoCode2")));
        }
        if (!geo.isBlank()) {
            body.put("_payerCountryIso2", com.pg.util.PayerCountryIso2Util.normalize(geo));
        }
        String city = resolveCityFromBody(body);
        if (city.isBlank()) {
            city = resolveCityFromRequest(request);
        }
        if (!city.isBlank()) {
            body.put("_payerCity", city.length() > 128 ? city.substring(0, 128) : city);
        }
    }

    private static String resolveCityFromBody(Map<String, Object> body) {
        if (body == null) {
            return "";
        }
        String v = str(body.get("_payerCity"));
        if (v.isBlank()) {
            v = str(body.get("payCity"));
        }
        if (v.isBlank()) {
            v = str(body.get("pay_city"));
        }
        return v;
    }

    private static String resolveCityFromRequest(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] headers = {"CF-IPCity", "X-City", "X-Appengine-City"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String resolveIpFromRequest(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank()) {
                String first = v.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote.trim() : "";
    }

    private static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }
}
