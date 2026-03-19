package com.pg.config;

import com.pg.service.AuthService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApiLoginFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiLoginBypassFilter> apiLoginBypassFilter(AuthService authService) {
        FilterRegistrationBean<ApiLoginBypassFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiLoginBypassFilter(authService));
        bean.addUrlPatterns("/api/auth/login");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return bean;
    }
}
