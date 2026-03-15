package com.pg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 406 Not Acceptable 방지: Accept 헤더 무시, 컨트롤러 produces 우선
 * - API: produces=application/json → JSON 반환
 * - View: 기본 text/html → Thymeleaf HTML 반환
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .ignoreAcceptHeader(true)
                .defaultContentType(MediaType.TEXT_HTML);
    }
}
