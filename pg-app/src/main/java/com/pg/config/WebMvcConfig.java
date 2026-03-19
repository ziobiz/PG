package com.pg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * /uploads/** : 본사/총판 브랜딩 이미지 서빙
 * <p>
 * 콘텐츠 협상은 Spring 기본값을 사용합니다.
 * {@code ignoreAcceptHeader(true) + defaultContentType(TEXT_HTML)} 는
 * {@code produces = application/json} 인 REST API 에 대해 406 Not Acceptable 을 유발할 수 있습니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 기본 협상 유지 (@RestController produces 와 일치)
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(System.getProperty("user.dir")).resolve(uploadDir).normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + basePath.toAbsolutePath() + "/");
    }
}
