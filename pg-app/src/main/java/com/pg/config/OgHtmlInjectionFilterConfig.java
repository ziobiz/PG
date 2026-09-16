package com.pg.config;

import com.pg.service.LinkPreviewService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class OgHtmlInjectionFilterConfig {

    @Bean
    public FilterRegistrationBean<OgHtmlInjectionFilter> ogHtmlInjectionFilterRegistration(LinkPreviewService linkPreviewService) {
        FilterRegistrationBean<OgHtmlInjectionFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OgHtmlInjectionFilter(linkPreviewService));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
