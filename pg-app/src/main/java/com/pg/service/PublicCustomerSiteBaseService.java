package com.pg.service;

import com.pg.repository.HqApiConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * 공개 결제 정적 페이지({@code /pay.html}, {@code /split-pay.html} 등) 베이스 URL.
 * {@code MerchantChatbotProductService} 와 분리해 분할결제·JPAY 빈 순환 참조를 방지합니다.
 */
@Service
public class PublicCustomerSiteBaseService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqNotifyEnvService hqNotifyEnvService;

    public PublicCustomerSiteBaseService(HqApiConfigRepository hqApiConfigRepository,
                                         HqNotifyEnvService hqNotifyEnvService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    /**
     * {@code tb_hq_api_config.public_api_base_url} → 노티 {@code public_base_url} → 요청 Host 순.
     */
    public String resolvePublicCustomerSiteBase(HttpServletRequest req) {
        String configured = hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> c.getPublicApiBaseUrl() != null ? c.getPublicApiBaseUrl().trim() : "")
                .orElse("");
        configured = trimSlash(configured);
        if (!configured.isBlank()) {
            return configured;
        }
        String notifyBase = trimSlash(hqNotifyEnvService.getOrCreate().getPublicBaseUrl());
        if (!notifyBase.isBlank()) {
            return notifyBase;
        }
        if (req != null) {
            return trimSlash(inferBaseFromRequest(req));
        }
        return "";
    }

    private static String trimSlash(String u) {
        if (u == null) {
            return "";
        }
        return u.trim().replaceAll("/+$", "");
    }

    private static String inferBaseFromRequest(HttpServletRequest req) {
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = req.getScheme();
        }
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = req.getServerName();
            int port = req.getServerPort();
            if (("http".equalsIgnoreCase(scheme) && port != 80)
                    || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }
}
