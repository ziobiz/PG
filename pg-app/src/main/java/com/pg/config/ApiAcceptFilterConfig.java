package com.pg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * /api/ 로 시작하는 요청에서 Accept 가 비었거나 전체 와일드카드인 경우
 * application/json 으로 보정하여 WebMvcConfig 기본 TEXT_HTML 과의 406 을 방지합니다.
 */
@Configuration
public class ApiAcceptFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiAcceptFilter> apiAcceptFilterRegistration() {
        FilterRegistrationBean<ApiAcceptFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiAcceptFilter());
        bean.addUrlPatterns("/*");
        // RootRedirectFilter 가 먼저 (HIGHEST_PRECEDENCE), 그 다음 Accept 보정
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }
}
