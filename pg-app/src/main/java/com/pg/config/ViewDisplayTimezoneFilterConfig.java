package com.pg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ViewDisplayTimezoneFilterConfig {

    @Bean
    public FilterRegistrationBean<ViewDisplayTimezoneRequestFilter> viewDisplayTimezoneRequestFilter() {
        FilterRegistrationBean<ViewDisplayTimezoneRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ViewDisplayTimezoneRequestFilter());
        bean.addUrlPatterns("/api/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return bean;
    }
}
