package com.pg.merchantdeploy;

import java.util.List;

/**
 * 단일 PG 벤더에 대한 브로커·레거시 공개 API 경로 정의.
 */
public record MerchantPgBrokerRouteDefinition(
        String vendorScope,
        String displayNameKr,
        String pathSegment,
        List<String> brokerRelativePaths,
        List<String> legacyRelativePaths
) {
}
