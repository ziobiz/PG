package com.pg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    /**
     * true: 모든 Origin 허용(패턴 {@code *}). API는 Bearer만 사용·쿠키 미사용이라 교차 출처 관리자 도메인에서 필수에 가깝다.
     * false: {@link #allowedOriginPatterns} 만 허용(보수적).
     */
    @Value("${app.cors.allow-all-origins:true}")
    private boolean allowAllOrigins;

    /**
     * allow-all 이 false 일 때만 사용.
     */
    @Value("${app.cors.allowed-origin-patterns:https://*.icopay.co.kr,https://jp.icopay.co.kr,https://icopay.co.kr,https://www.icopay.co.kr,http://localhost:*,http://127.0.0.1:*,https://*.cafe24.com,http://*.cafe24.com}")
    private String allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowAllOrigins) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (patterns.isEmpty()) {
                patterns = List.of("*");
            }
            config.setAllowedOriginPatterns(patterns);
        }
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        // Bearer 헤더만 쓰고 쿠키 기반 API 세션은 없음 — false 가 브라우저·프록시 조합에서 더 안정적
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
