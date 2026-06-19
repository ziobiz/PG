package com.pg.config;

import com.pg.middleware.notify.PgNotifyIngressHandler;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * NOTI·JPAY form POST 노티가 Spring Security(302 login)에 막히지 않도록
 * {@link ApiLoginBypassFilter} 와 동일한 우회 등록.
 */
@Configuration
public class ApiPgNotifyFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiPgNotifyBypassFilter> apiPgNotifyBypassFilter(
            PgNotifyIngressHandler ingressHandler) {
        FilterRegistrationBean<ApiPgNotifyBypassFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiPgNotifyBypassFilter(ingressHandler));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return bean;
    }
}
