package com.pg.merchantdeploy;

import java.security.SecureRandom;
import java.util.Locale;

final class MerchantBrokerSecretGenerator {

    private static final char[] ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghijkmnpqrstuvwxyz".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    private MerchantBrokerSecretGenerator() {
    }

    static String newSecret(int length) {
        StringBuilder sb = new StringBuilder(length + 4);
        sb.append("ic_");
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM[RND.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }

    static String prefixOf(String secret) {
        if (secret == null) {
            return "";
        }
        String t = secret.trim();
        if (t.length() <= 6) {
            return t.toLowerCase(Locale.ROOT);
        }
        return t.substring(0, 6).toLowerCase(Locale.ROOT) + "…";
    }
}
