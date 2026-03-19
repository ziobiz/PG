package com.pg.config;

import com.pg.controller.api.ApiDevController;
import com.pg.service.CompService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration
public class ApiDevFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiDevBypassFilter> apiDevBypassFilter(ApiDevController apiDevController) {
        FilterRegistrationBean<ApiDevBypassFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiDevBypassFilter(apiDevController));
        bean.addUrlPatterns("/api/dev/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return bean;
    }

    /** dev 프로파일에서 /api/comp/list 인증 없이 처리 (검색 결과 미표시 우회) */
    @Bean
    public FilterRegistrationBean<ApiCompListBypassFilter> apiCompListBypassFilter(CompService compService, Environment environment) {
        FilterRegistrationBean<ApiCompListBypassFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiCompListBypassFilter(compService, environment));
        bean.addUrlPatterns("/api/comp/list");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return bean;
    }
}
