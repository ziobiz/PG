package com.pg.config;

import com.pg.service.OrgHierarchyResetService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 기동 시 업체 트리 전체 삭제 후 OTL HQ(0000000000)만 남김.
 * {@code app.data.reset-org-hierarchy-on-startup=true} 일 때만 동작 (보통 1회 기동 후 false 권장).
 * <p>TEMP_REMOVE_AFTER_DEV — {@link com.pg.service.OrgHierarchyResetService} 제거 시 이 설정·Configuration 도 함께 정리.
 */
@Configuration
@ConditionalOnProperty(name = "app.data.reset-org-hierarchy-on-startup", havingValue = "true")
public class OrgHierarchyStartupReset {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CommandLineRunner orgHierarchyResetOnStartup(OrgHierarchyResetService orgHierarchyResetService) {
        return args -> orgHierarchyResetService.resetToHeadquartersOnly();
    }
}
