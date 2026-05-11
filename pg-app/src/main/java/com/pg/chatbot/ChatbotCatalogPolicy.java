package com.pg.chatbot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 산하 조직 교집합·가맹 활성화 교집합으로 허용 챗봇 상품 유형을 결정합니다.
 */
public final class ChatbotCatalogPolicy {

    private ChatbotCatalogPolicy() {
    }

    public static List<String> orderedAllListingCodes() {
        List<String> codes = new ArrayList<>();
        for (ChatbotListingType t : ChatbotListingType.values()) {
            codes.add(t.getCode());
        }
        return codes;
    }

    /**
     * CSV → 대문자 코드 집합. 알 수 없는 토큰이 있으면 null.
     */
    public static LinkedHashSet<String> parseListingCsvOrNull(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String p = part.trim().toUpperCase(Locale.ROOT);
            if (p.isEmpty()) {
                continue;
            }
            Optional<ChatbotListingType> t = ChatbotListingType.fromCode(p);
            if (t.isEmpty()) {
                return null;
            }
            out.add(t.get().getCode());
        }
        return out.isEmpty() ? null : out;
    }

    public static String joinListingCsv(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }
        List<String> order = orderedAllListingCodes();
        StringBuilder sb = new StringBuilder();
        for (String key : order) {
            if (codes.contains(key)) {
                if (!sb.isEmpty()) {
                    sb.append(',');
                }
                sb.append(key);
            }
        }
        return sb.toString();
    }

    /** 상위들이 모두 허용하는 교집합. grant 가 null 인 단계는 패스. */
    public static LinkedHashSet<String> intersectGrants(List<LinkedHashSet<String>> grantSets) {
        LinkedHashSet<String> acc = new LinkedHashSet<>(orderedAllListingCodes());
        if (grantSets == null) {
            return acc;
        }
        for (LinkedHashSet<String> g : grantSets) {
            if (g == null || g.isEmpty()) {
                continue;
            }
            acc.retainAll(g);
            if (acc.isEmpty()) {
                break;
            }
        }
        return acc;
    }

    public static LinkedHashSet<String> intersectEnabled(LinkedHashSet<String> grantMask,
                                                        LinkedHashSet<String> merchantEnabledOrNull) {
        Objects.requireNonNull(grantMask, "grantMask");
        LinkedHashSet<String> out = new LinkedHashSet<>(grantMask);
        if (merchantEnabledOrNull != null && !merchantEnabledOrNull.isEmpty()) {
            out.retainAll(merchantEnabledOrNull);
        }
        return out;
    }

    public static Integer clampImageGrant(Integer v) {
        if (v == null) {
            return null;
        }
        int n = v;
        if (n < 1) {
            return 1;
        }
        if (n > 4) {
            return 4;
        }
        return n;
    }
}
