package com.pg.util;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 본사 AI 설정 — 용도별 1~4순위의 제공자·모델 한 쌍. */
public record LlmProviderSlot(String provider, String model) {

    public LlmProviderSlot {
        provider = normalizeProvider(provider);
        model = model != null ? model.trim() : "";
    }

    public static String normalizeProvider(String raw) {
        if (raw == null) {
            return "";
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        if ("gemini".equals(p) || "groq".equals(p) || "anthropic".equals(p) || "openai".equals(p)) {
            return p;
        }
        return "";
    }

    public boolean isUsable() {
        return provider != null && !provider.isBlank();
    }

    public String resolvedModel(Map<String, Object> cfg) {
        if (model != null && !model.isBlank()) {
            return model.trim();
        }
        return defaultModelForProvider(provider);
    }

    public static String defaultModelForProvider(String provider) {
        String p = normalizeProvider(provider);
        if (p.isEmpty()) {
            return "";
        }
        return switch (p) {
            case "gemini" -> "gemini-3.5-flash";
            case "groq" -> "llama-3.1-8b-instant";
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "openai" -> "gpt-4o-mini";
            default -> "";
        };
    }

    /** @deprecated {@link #defaultModelForProvider(String)} */
    @Deprecated
    public static String defaultModelForProvider(String provider, Map<String, Object> cfg) {
        return defaultModelForProvider(provider);
    }

    public Map<String, String> toJsonMap() {
        return Map.of(
                "provider", provider != null ? provider : "",
                "model", model != null ? model : ""
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LlmProviderSlot that)) {
            return false;
        }
        return Objects.equals(provider, that.provider) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, model);
    }
}
