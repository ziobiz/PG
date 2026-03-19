package com.pg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RootRedirectFilterConfig {

    @Bean
    public FilterRegistrationBean<RootRedirectFilter> rootRedirectFilterRegistration() {
        FilterRegistrationBean<RootRedirectFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RootRedirectFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
