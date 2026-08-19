package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 일본 우편번호 → 주소 (zipcloud). 업체등록 기본정보 주소 검색용.
 */
@Service
public class JapanZipLookupService {

    private static final Logger log = LoggerFactory.getLogger(JapanZipLookupService.class);
    private static final String ZIPCLOUD = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public JapanZipLookupService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static String digitsOnly(String zip) {
        if (zip == null) {
            return "";
        }
        return zip.replaceAll("\\D", "");
    }

    public static boolean isValidJpZip(String digits) {
        return digits != null && digits.matches("\\d{7}");
    }

    public static String formatJpZip(String digits) {
        String d = digitsOnly(digits);
        if (!isValidJpZip(d)) {
            return d;
        }
        return d.substring(0, 3) + "-" + d.substring(3);
    }

    public List<Map<String, String>> lookup(String zipRaw) {
        String digits = digitsOnly(zipRaw);
        if (!isValidJpZip(digits)) {
            throw new IllegalArgumentException("JP_ZIP_INVALID");
        }
        String url = ZIPCLOUD + URLEncoder.encode(digits, StandardCharsets.UTF_8);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("Japan zip lookup HTTP {}", res.statusCode());
                throw new IllegalStateException("JP_ZIP_LOOKUP_FAILED");
            }
            JsonNode root = objectMapper.readTree(res.body() != null ? res.body() : "{}");
            int status = root.path("status").asInt(0);
            if (status != 200) {
                log.warn("Japan zip lookup status={} msg={}", status, root.path("message").asText(""));
                throw new IllegalStateException("JP_ZIP_LOOKUP_FAILED");
            }
            JsonNode results = root.get("results");
            List<Map<String, String>> out = new ArrayList<>();
            if (results == null || !results.isArray() || results.isEmpty()) {
                return out;
            }
            for (JsonNode n : results) {
                Map<String, String> row = new LinkedHashMap<>();
                String zip = n.path("zipcode").asText(digits);
                String a1 = n.path("address1").asText("");
                String a2 = n.path("address2").asText("");
                String a3 = n.path("address3").asText("");
                row.put("zip", formatJpZip(zip));
                row.put("pref", a1);
                row.put("city", a2);
                row.put("town", a3);
                row.put("address", (a1 + a2 + a3).trim());
                out.add(row);
            }
            return out;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Japan zip lookup interrupted");
            throw new IllegalStateException("JP_ZIP_LOOKUP_FAILED");
        } catch (Exception e) {
            log.warn("Japan zip lookup failed: {}", e.getMessage());
            throw new IllegalStateException("JP_ZIP_LOOKUP_FAILED");
        }
    }
}
