package com.pg.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ElementPay {@code getMethods} 카드 alias.
 * 라이브·샌드박스 모두 캐비닛에 실제로 열린 수단만 쓴다.
 * 라이브에서 {@code kCards} 를 {@code thCardsCheckout} 로 바꾸지 않는다
 * (라이브 getMethods 에 kCards 만 있는 가맹이 있다).
 */
public final class ElementPayCardServiceAlias {

    private ElementPayCardServiceAlias() {
    }

    public static String resolveConfigured(String configured, boolean sandbox) {
        if (configured == null || configured.isBlank() || "card".equalsIgnoreCase(configured.trim())) {
            return sandbox ? "kCards" : "thCardsCheckout";
        }
        return configured.trim();
    }

    public static boolean catalogContains(List<Map<String, Object>> methods, String alias) {
        if (methods == null || alias == null || alias.isBlank()) {
            return false;
        }
        String want = alias.trim();
        for (Map<String, Object> m : methods) {
            if (m == null) {
                continue;
            }
            if (want.equalsIgnoreCase(str(m.get("alias"))) || want.equals(str(m.get("id")))) {
                return true;
            }
        }
        return false;
    }

    /**
     * HQ/기본 alias 가 getMethods 목록에 없으면 목록의 카드 수단으로 교체한다.
     * 목록이 비면 요청 alias 를 유지한다.
     */
    public static String resolveAgainstCatalog(String wanted, List<Map<String, Object>> methods) {
        String use = (wanted == null || wanted.isBlank()) ? "kCards" : wanted.trim();
        if (methods == null || methods.isEmpty()) {
            return use;
        }
        if (catalogContains(methods, use)) {
            return use;
        }
        String suggested = suggestCard(methods);
        return suggested != null && !suggested.isBlank() ? suggested : use;
    }

    public static String suggestCard(List<Map<String, Object>> methods) {
        if (methods == null || methods.isEmpty()) {
            return "kCards";
        }
        String hosted = firstMatching(methods, true);
        if (hosted != null) {
            return hosted;
        }
        String kCards = firstMatching(methods, false);
        return kCards != null ? kCards : str(methods.get(0).get("alias"));
    }

    private static String firstMatching(List<Map<String, Object>> methods, boolean hostedCheckoutOnly) {
        for (Map<String, Object> m : methods) {
            String alias = str(m.get("alias"));
            String aliasL = alias.toLowerCase(Locale.ROOT);
            String name = str(m.get("name")).toLowerCase(Locale.ROOT);
            boolean hosted = aliasL.contains("cardscheckout") || aliasL.equals("thcardscheckout");
            boolean kOrGeneric = aliasL.equals("kcards") || aliasL.equals("card")
                    || (name.contains("visa") && name.contains("master"))
                    || name.contains("jcb") || name.contains("unionpay") || name.contains("amex")
                    || (name.contains("credit") && name.contains("card"));
            if (hostedCheckoutOnly) {
                if (hosted) {
                    return alias.isBlank() ? str(m.get("id")) : alias;
                }
            } else if (hosted || kOrGeneric || aliasL.contains("card") || name.contains("card")) {
                return alias.isBlank() ? str(m.get("id")) : alias;
            }
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
