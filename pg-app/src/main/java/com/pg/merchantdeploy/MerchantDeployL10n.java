package com.pg.merchantdeploy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 가맹 API 배포 문서·키트용 5개 언어(KO/EN/JP/CH/TH) 텍스트 번들.
 */
public final class MerchantDeployL10n {

    public record Bundle(String kr, String en, String jp, String ch, String th) {
    }

    private MerchantDeployL10n() {
    }

    public static Map<String, Object> textMap(Bundle b) {
        Map<String, Object> m = new LinkedHashMap<>();
        putTextFields(m, "", b);
        return m;
    }

    public static void putTextFields(Map<String, Object> m, String prefix, Bundle b) {
        String p = prefix == null ? "" : prefix;
        m.put(p + "Kr", b.kr());
        m.put(p + "En", b.en());
        m.put(p + "Jp", b.jp());
        m.put(p + "Ch", b.ch());
        m.put(p + "Th", b.th());
    }

    public static void putDescription(Map<String, Object> m, Bundle b) {
        putTextFields(m, "description", b);
    }

    public static void putRemark(Map<String, Object> m, Bundle b) {
        putTextFields(m, "remark", b);
    }

    public static void putMeaning(Map<String, Object> m, Bundle b) {
        putTextFields(m, "meaning", b);
    }
}
