package com.pg.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PG 아웃바운드 페이로드에서 가맹 사업장 상호·주소·쇼핑몰 URL·도메인·IP 식별을 제거한다.
 * 구매자 배송/청구 주소({@code payStreet*}, {@code shipping*}, {@code buyer.*})는 유지.
 *
 * <p>규칙: 결제사는 ICOPAY·NOTI 만 알 수 있다. 가맹 상호·홈페이지·도메인·IP 는 절대 전달 금지.
 * 주소가 필요하면 {@link IcopayPublicIdentity} 만 사용.
 */
public final class PgOutboundMerchantPrivacy {

    /** PG customerEmail 자리 — 가맹 도메인 이메일 유출 방지. */
    public static final String SAFE_PG_CUSTOMER_EMAIL = "noreply@icopay.co.kr";
    /** PG description 자리 — 상품명·상호·URL 금지. */
    public static final String SAFE_PG_DESCRIPTION = "Payment";

    private static final Pattern URL_OR_DOMAIN = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9][a-z0-9-]{0,61}\\.(com|net|org|io|shop|store|co\\.kr|kr|jp|th|cn)\\b)");
    private static final Pattern IPV4 = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b");

    private static final Set<String> BLOCKED_KEYS = Set.of(
            "merchantname", "merchant_name", "merchantnm", "compnm", "companynm", "companyname",
            "shopname", "storename", "sitename", "merchantaddr", "merchantaddress",
            "companyaddr", "companyaddress", "bizaddr", "businessaddress", "officeaddr",
            "officeaddress", "shopaddr", "shopaddress", "storeaddr", "storeaddress",
            "branchaddr", "branchaddress", "addrdetail", "addretc", "chatbotkbaddr",
            "homepage", "siteurl", "shopurl", "storeurl", "merchanturl", "mallurl",
            "merchantip", "shopip", "serverip", "clientip", "remoteip"
    );

    private PgOutboundMerchantPrivacy() {
    }

    /**
     * PG 전송 직전 Map 에서 가맹 사업장 식별 키를 제거.
     * 키 이름이 구매자 주소({@code pay_street_*}, {@code shipping_*}, {@code billto} 등)이면 유지.
     */
    public static void stripMerchantBusinessIdentity(Map<?, ?> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        Iterator<? extends Map.Entry<?, ?>> it = payload.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = it.next();
            Object k = e.getKey();
            if (k == null) {
                continue;
            }
            String key = String.valueOf(k).trim();
            if (key.isEmpty()) {
                continue;
            }
            if (isBuyerAddressKey(key)) {
                continue;
            }
            if (isBlockedMerchantKey(key)) {
                it.remove();
            }
        }
    }

    public static boolean isBlockedMerchantKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String n = normalizeKey(key);
        if (BLOCKED_KEYS.contains(n)) {
            return true;
        }
        /* merchantAddress / company_addr / shop-url 등 */
        if (n.contains("merchant") && (n.contains("addr") || n.contains("name") || n.contains("url"))) {
            return true;
        }
        if (n.contains("company") && (n.contains("addr") || n.contains("name"))) {
            return true;
        }
        return (n.contains("shop") || n.contains("store") || n.contains("mall"))
                && (n.contains("addr") || n.contains("url"));
    }

    /** 구매자 청구·배송 주소 필드 — 가맹 사업장 주소와 구분. */
    public static boolean isBuyerAddressKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String n = normalizeKey(key);
        if (n.startsWith("pay") || n.startsWith("shipping") || n.startsWith("buyer")
                || n.startsWith("billto") || n.startsWith("shipto") || n.startsWith("customer")) {
            return n.contains("addr") || n.contains("street") || n.contains("city")
                    || n.contains("zip") || n.contains("post") || n.contains("state")
                    || n.contains("country");
        }
        return n.contains("paystreet") || n.contains("shippingstreet")
                || n.contains("billingstreet") || n.contains("homeaddress");
    }

    /**
     * PG 로 보내는 customerEmail.
     * {@code sendRealBuyerEmail=false} 이면 항상 {@link #SAFE_PG_CUSTOMER_EMAIL}.
     * {@code true} 이면 실제 구매자 이메일을 보내고, 비어 있거나 '@' 없으면 중립 메일로 폴백.
     */
    public static String safePgCustomerEmail(String buyerEmail) {
        return safePgCustomerEmail(buyerEmail, false);
    }

    public static String safePgCustomerEmail(String buyerEmail, boolean sendRealBuyerEmail) {
        if (!sendRealBuyerEmail) {
            return SAFE_PG_CUSTOMER_EMAIL;
        }
        if (buyerEmail == null || buyerEmail.isBlank()) {
            return SAFE_PG_CUSTOMER_EMAIL;
        }
        String e = buyerEmail.trim();
        if (e.length() > 200 || e.indexOf('@') < 1 || e.indexOf('@') == e.length() - 1) {
            return SAFE_PG_CUSTOMER_EMAIL;
        }
        if (containsUrlDomainOrIp(e) && !e.toLowerCase(Locale.ROOT).contains("@")) {
            return SAFE_PG_CUSTOMER_EMAIL;
        }
        return e.length() > 100 ? e.substring(0, 100) : e;
    }

    /** PG description — URL·도메인·IP·주문패턴이면 {@link #SAFE_PG_DESCRIPTION}. */
    public static String safePgDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return SAFE_PG_DESCRIPTION;
        }
        String t = raw.trim();
        if (containsUrlDomainOrIp(t)) {
            return SAFE_PG_DESCRIPTION;
        }
        return t;
    }

    /** 문자열에 홈페이지·도메인·IPv4 흔적이 있으면 true. */
    public static boolean containsUrlDomainOrIp(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String t = text.trim();
        return URL_OR_DOMAIN.matcher(t).find() || IPV4.matcher(t).find();
    }

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
