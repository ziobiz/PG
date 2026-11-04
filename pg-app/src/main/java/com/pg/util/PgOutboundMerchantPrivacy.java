package com.pg.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PG 아웃바운드 페이로드에서 가맹 사업장 상호·주소·쇼핑몰 URL 키를 제거한다.
 * 구매자 배송/청구 주소({@code payStreet*}, {@code shipping*}, {@code buyer.*})는 유지.
 *
 * <p>규칙: 가맹 주소는 ICOPAY 주소({@link IcopayPublicIdentity}) 이외로 나가면 안 된다.
 */
public final class PgOutboundMerchantPrivacy {

    private static final Set<String> BLOCKED_KEYS = Set.of(
            "merchantname", "merchant_name", "merchantnm", "compnm", "companynm", "companyname",
            "shopname", "storename", "sitename", "merchantaddr", "merchantaddress",
            "companyaddr", "companyaddress", "bizaddr", "businessaddress", "officeaddr",
            "officeaddress", "shopaddr", "shopaddress", "storeaddr", "storeaddress",
            "branchaddr", "branchaddress", "addrdetail", "addretc", "chatbotkbaddr",
            "homepage", "siteurl", "shopurl", "storeurl", "merchanturl", "mallurl"
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

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
