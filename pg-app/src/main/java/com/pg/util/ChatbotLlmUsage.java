package com.pg.util;

/**
 * 본사 AI챗봇설정 — 용도별 LLM 프로바이더·모델 순위({@code config_json} 키).
 */
public enum ChatbotLlmUsage {

    /** 상품·카탈로그 안내(공개 챗봇 대화 — 상품 중심) */
    CATALOG("report_provider_order_catalog"),
    /** 챗봇 짧은 문장 응답 */
    SHORT("report_provider_order_short"),
    /** 챗봇 결제 안내·기타(환영 문구·로고 LLM 등) */
    GENERAL("report_provider_order_general"),
    /** ICOPAY 챗봇결제 외 서비스(처리사유 번역·로그인 공지 다국어 등) */
    PLATFORM("report_provider_order_platform");

    public static final String LEGACY_ORDER_KEY = "report_provider_order";

    private final String configKey;

    ChatbotLlmUsage(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
