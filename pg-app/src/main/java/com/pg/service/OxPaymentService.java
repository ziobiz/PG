package com.pg.service;

import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ox.OxCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ox(OxPay Financial) — Merchant Hosted Create Payment 어댑터 골격.
 * <p>카드는 ICOPAY 인라인 1회 수집 → 서버 Create Payment. HPP 카드 UI 미사용.
 * Create Payment 요청/응답 필드는 OxPay 제공 API 문서(MID·TID 패키지)로 확정 후 완성.
 */
@Service
public class OxPaymentService {

    private static final Logger log = LoggerFactory.getLogger(OxPaymentService.class);

    public static final String NOTI_WEBHOOK_PATH = "/noti/ox";
    public static final String NOTI_RESULT_PATH = "/noti/result/ox";

    @Value("${app.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    @Value("${app.noti.public-base:https://noti.icopay.net}")
    private String notiPublicBase;

    /**
     * 운영 Webhook / Result 고정 URL (가맹 도메인 미등록).
     */
    public Map<String, String> publicNotiIngressUrls() {
        String base = notiPublicBase != null ? notiPublicBase.trim().replaceAll("/+$", "") : "https://noti.icopay.net";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("oxWebhookUrl", base + NOTI_WEBHOOK_PATH);
        m.put("oxResultUrl", base + NOTI_RESULT_PATH);
        m.put("pgVendor", PgVendor.OX);
        return m;
    }

    public String buildIcopayIngressUrl(String ingressToken) {
        return PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.OX;
    }

    /**
     * Merchant Hosted Create Payment — API 문서 확정 전 호출 시 명확한 오류.
     */
    public Map<String, Object> createPayment(OxCredentials creds, Map<String, Object> request) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (creds == null || !creds.isUsable()) {
            out.put("ok", false);
            out.put("errorCode", "OX_CREDENTIALS_MISSING");
            out.put("message", "ox API Key가 없습니다. 결제대행사 설정에서 등록하세요.");
            return out;
        }
        log.warn("ox Create Payment not fully implemented — need OxPay API pack. mid={} base={}",
                creds.mid(), creds.apiBase());
        out.put("ok", false);
        out.put("errorCode", "OX_API_DOC_REQUIRED");
        out.put("message",
                "ox Merchant Hosted Create Payment는 OxPay API 문서(엔드포인트·서명·3DS 필드) 확정 후 완성됩니다. NOTI 입구·provision은 준비되었습니다.");
        out.put("apiBase", creds.apiBase());
        out.put("requestEcho", request != null ? request.keySet() : null);
        out.put("noti", publicNotiIngressUrls());
        return out;
    }
}
