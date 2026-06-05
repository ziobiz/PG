package com.pg.merchantdeploy;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 관리 화면 — 가맹점 API 샘플·연동 문서(classpath static) 조회. 화이트리스트 경로만 허용.
 */
@Service
public class MerchantApiSampleDocService {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "merchant-api-samples/docs/",
            "merchant-api-samples/json/",
            "merchant-api-samples/php/",
            "merchant-api-samples/jsp/",
            "merchant-api-samples/common/",
            "merchant-api-samples/index.html",
            "merchant-api-samples/README.txt"
    );

    public record SampleDoc(String body, MediaType contentType) {
    }

    public Optional<SampleDoc> load(String rawPath) {
        String path = normalizePath(rawPath);
        if (path == null || !isAllowed(path)) {
            return Optional.empty();
        }
        ClassPathResource resource = new ClassPathResource("static/" + path);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try {
            String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return Optional.of(new SampleDoc(body, mediaTypeFor(path)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String flowDocPath(String lang, boolean text) {
        String l = lang != null ? lang.trim().toUpperCase(Locale.ROOT) : "";
        String suffix = switch (l) {
            case "KO", "KR" -> ".ko";
            case "JP", "JA" -> ".ja";
            case "CH", "ZH" -> ".ch";
            case "TH" -> ".th";
            default -> "";
        };
        String ext = text ? ".txt" : ".html";
        return "merchant-api-samples/docs/unified-checkout-api-flow" + suffix + ext;
    }

    private static String normalizePath(String raw) {
        if (raw == null) {
            return null;
        }
        String p = raw.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.isEmpty() || p.contains("..")) {
            return null;
        }
        for (String seg : p.split("/")) {
            if (seg.isEmpty() || !SAFE_SEGMENT.matcher(seg).matches()) {
                return null;
            }
        }
        return p;
    }

    private static boolean isAllowed(String path) {
        if ("merchant-api-samples/index.html".equals(path) || "merchant-api-samples/README.txt".equals(path)) {
            return true;
        }
        return ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static MediaType mediaTypeFor(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) {
            return MediaType.parseMediaType("text/html;charset=UTF-8");
        }
        if (lower.endsWith(".json")) {
            return MediaType.parseMediaType("application/json;charset=UTF-8");
        }
        if (lower.endsWith(".php")) {
            return MediaType.parseMediaType("application/x-php;charset=UTF-8");
        }
        if (lower.endsWith(".java") || lower.endsWith(".properties")) {
            return MediaType.parseMediaType("text/plain;charset=UTF-8");
        }
        return MediaType.parseMediaType("text/plain;charset=UTF-8");
    }
}
