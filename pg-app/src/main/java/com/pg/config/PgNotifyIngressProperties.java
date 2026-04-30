package com.pg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PG 노티 수신 URL({@code /api/open/pg-notify/…} 및 {@code /api/middleware/notify/v1/pg-notify/…}) —
 * ziobiz/NOTI 등 송신원 제한용. 값이 비어 있으면 해당 검증은 생략(기존 동작).
 */
@Component
@ConfigurationProperties(prefix = "app.pg-notify")
public class PgNotifyIngressProperties {

    /**
     * 허용 송신 IP 또는 CIDR, 쉼표 구분. 예: 203.0.113.10,198.51.100.0/24
     * 비어 있으면 IP 제한 없음.
     */
    private String allowedClientCidrs = "";

    /**
     * NOTI 미들웨어와 공유 비밀. 비어 있으면 HMAC 검증 안 함.
     * 본문은 컨트롤러에서 읽은 RAW 문자열(UTF-8 디코딩 결과) 기준 HMAC-SHA256 → 소문자 hex.
     */
    private String hmacSecret = "";

    /** HMAC hex 값이 실릴 HTTP 헤더명 (NOTI 측과 동일하게 맞출 것) */
    private String hmacHeader = "X-PG-Notify-Hmac";

    public String getAllowedClientCidrs() {
        return allowedClientCidrs;
    }

    public void setAllowedClientCidrs(String allowedClientCidrs) {
        this.allowedClientCidrs = allowedClientCidrs != null ? allowedClientCidrs : "";
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret != null ? hmacSecret : "";
    }

    public String getHmacHeader() {
        return hmacHeader;
    }

    public void setHmacHeader(String hmacHeader) {
        this.hmacHeader = (hmacHeader != null && !hmacHeader.isBlank()) ? hmacHeader.trim() : "X-PG-Notify-Hmac";
    }
}
