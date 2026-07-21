package com.pg.noti;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * NOTI JPAY Provision API 클라이언트.
 */
@Component
public class NotiProvisionClient {

    private static final String DEFAULT_BASE = "https://noti.icopay.net";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public NotiProvisionClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> provision(String baseUrl, String apiKey, Map<String, Object> body, String acceptLanguage)
            throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String url = base + "/api/v1/icopay/merchants/provision";
        String requestId = UUID.randomUUID().toString();
        return exchange(HttpMethod.POST, url, apiKey, body, requestId, acceptLanguage);
    }

    public Map<String, Object> getMerchant(String baseUrl, String apiKey, String merchantId, String acceptLanguage)
            throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new NotiProvisionException("merchantId가 필요합니다.", "VALIDATION");
        }
        String url = base + "/api/v1/icopay/merchants/" + encodePathSegment(mid) + "?pgKind=jpay";
        return exchange(HttpMethod.GET, url, apiKey, null, UUID.randomUUID().toString(), acceptLanguage);
    }

    /** JPAY 가맹 수정 (NOTI 2차 API). */
    public Map<String, Object> updateMerchant(String baseUrl, String apiKey, String merchantId,
                                              Map<String, Object> body, String acceptLanguage)
            throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new NotiProvisionException("merchantId가 필요합니다.", "VALIDATION");
        }
        String url = base + "/api/v1/icopay/merchants/" + encodePathSegment(mid);
        Map<String, Object> req = body != null ? new LinkedHashMap<>(body) : new LinkedHashMap<>();
        req.putIfAbsent("pgKind", "jpay");
        return exchange(HttpMethod.PUT, url, apiKey, req, UUID.randomUUID().toString(), acceptLanguage);
    }

    /** JPAY 가맹 삭제 (NOTI 2차 API). */
    public Map<String, Object> deleteMerchant(String baseUrl, String apiKey, String merchantId,
                                              boolean force, String acceptLanguage)
            throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new NotiProvisionException("merchantId가 필요합니다.", "VALIDATION");
        }
        String url = base + "/api/v1/icopay/merchants/" + encodePathSegment(mid) + "?pgKind=jpay";
        if (force) {
            url += "&force=true";
        }
        return exchange(HttpMethod.DELETE, url, apiKey, null, UUID.randomUUID().toString(), acceptLanguage);
    }

    /** NOTI internal-targets 목록 (미구현 시 빈 목록). */
    public List<Map<String, Object>> listInternalTargets(String baseUrl, String apiKey, String acceptLanguage) {
        String base = normalizeBase(baseUrl);
        String url = base + "/api/v1/icopay/internal-targets";
        try {
            return exchangeList(HttpMethod.GET, url, apiKey, UUID.randomUUID().toString(), acceptLanguage);
        } catch (NotiProvisionException e) {
            if (e.getHttpStatus() == 404) {
                return Collections.emptyList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** JPAY 슬롯 사용 가능 여부 (NOTI API 미구현 시 404). */
    public Map<String, Object> checkJpaySlot(String baseUrl, String apiKey, int slotNo, String excludeMerchantId,
                                             String acceptLanguage) throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String url = base + "/api/v1/icopay/jpay/slots/" + slotNo + "?pgKind=jpay";
        if (excludeMerchantId != null && !excludeMerchantId.isBlank()) {
            url += "&excludeMerchantId=" + encodePathSegment(excludeMerchantId.trim());
        }
        return exchange(HttpMethod.GET, url, apiKey, null, UUID.randomUUID().toString(), acceptLanguage);
    }

    public static String defaultBaseUrlIfBlank(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE;
        }
        return normalizeBase(baseUrl);
    }

    private Map<String, Object> exchange(HttpMethod method, String url, String apiKey, Map<String, Object> body,
                                         String requestId, String acceptLanguage) throws NotiProvisionException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey != null ? apiKey.trim() : "");
        headers.set("X-Icopay-Request-Id", requestId != null ? requestId : UUID.randomUUID().toString());
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage.trim());
        }
        HttpEntity<?> entity;
        if (body != null && method != HttpMethod.GET) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            entity = new HttpEntity<>(body, headers);
        } else {
            entity = new HttpEntity<>(headers);
        }
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            if (method == HttpMethod.DELETE && (resp.getBody() == null || resp.getBody().isBlank())) {
                Map<String, Object> ok = new LinkedHashMap<>();
                ok.put("deleted", true);
                return ok;
            }
            return parseSuccessBody(resp.getBody());
        } catch (HttpStatusCodeException ex) {
            throw toHttpException(ex);
        } catch (NotiProvisionException e) {
            throw e;
        } catch (Exception e) {
            throw new NotiProvisionException(
                    e.getMessage() != null ? e.getMessage() : "NOTI Provision API 호출 실패", "NOTI_ERROR", 0);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> exchangeList(HttpMethod method, String url, String apiKey,
                                                     String requestId, String acceptLanguage)
            throws NotiProvisionException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey != null ? apiKey.trim() : "");
        headers.set("X-Icopay-Request-Id", requestId != null ? requestId : UUID.randomUUID().toString());
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage.trim());
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            return parseSuccessListBody(resp.getBody());
        } catch (HttpStatusCodeException ex) {
            throw toHttpException(ex);
        }
    }

    private NotiProvisionException toHttpException(HttpStatusCodeException ex) {
        ParsedNotiError parsed = parseErrorBody(ex.getResponseBodyAsString());
        String code = parsed.errorCode != null && !parsed.errorCode.isBlank() ? parsed.errorCode : "NOTI_HTTP";
        return new NotiProvisionException(parsed.message, code, ex.getStatusCode().value());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSuccessListBody(String raw) throws NotiProvisionException {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            if (root == null) {
                return Collections.emptyList();
            }
            Object success = root.get("success");
            if (success instanceof Boolean b && !b) {
                String msg = root.get("message") != null ? String.valueOf(root.get("message")) : "NOTI 오류";
                String code = root.get("errorCode") != null ? String.valueOf(root.get("errorCode")) : "NOTI_FAIL";
                throw new NotiProvisionException(msg, code, 0);
            }
            Object data = root.get("data");
            if (data instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        out.add(new LinkedHashMap<>((Map<String, Object>) m));
                    }
                }
                return out;
            }
            if (data instanceof Map<?, ?> dm) {
                Object targets = dm.get("targets");
                if (targets instanceof List<?> list) {
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m) {
                            out.add(new LinkedHashMap<>((Map<String, Object>) m));
                        }
                    }
                    return out;
                }
            }
            return Collections.emptyList();
        } catch (NotiProvisionException e) {
            throw e;
        } catch (Exception e) {
            throw new NotiProvisionException("NOTI 응답 JSON 파싱 실패: " + e.getMessage(), "NOTI_PARSE", 0);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSuccessBody(String raw) throws NotiProvisionException {
        if (raw == null || raw.isBlank()) {
            throw new NotiProvisionException("NOTI 응답 본문이 비어 있습니다.", "NOTI_EMPTY", 0);
        }
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            if (root == null) {
                throw new NotiProvisionException("NOTI 응답을 해석할 수 없습니다.", "NOTI_PARSE", 0);
            }
            Object success = root.get("success");
            if (success instanceof Boolean b && !b) {
                String msg = root.get("message") != null ? String.valueOf(root.get("message")) : "NOTI 오류";
                String code = root.get("errorCode") != null ? String.valueOf(root.get("errorCode")) : "NOTI_FAIL";
                throw new NotiProvisionException(msg, code, 0);
            }
            Object data = root.get("data");
            if (data instanceof Map<?, ?> dm) {
                return new LinkedHashMap<>((Map<String, Object>) dm);
            }
            return root;
        } catch (NotiProvisionException e) {
            throw e;
        } catch (Exception e) {
            throw new NotiProvisionException("NOTI 응답 JSON 파싱 실패: " + e.getMessage(), "NOTI_PARSE", 0);
        }
    }

    private ParsedNotiError parseErrorBody(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedNotiError("NOTI Provision API 오류", null);
        }
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            String message = null;
            if (root.get("message") != null) {
                message = String.valueOf(root.get("message"));
            } else if (root.get("error") != null) {
                message = String.valueOf(root.get("error"));
            }
            String errorCode = root.get("errorCode") != null ? String.valueOf(root.get("errorCode")).trim() : null;
            if (message == null || message.isBlank()) {
                message = raw.length() > 500 ? raw.substring(0, 500) : raw;
            }
            return new ParsedNotiError(message, errorCode);
        } catch (Exception ignored) {
            return new ParsedNotiError(raw.length() > 500 ? raw.substring(0, 500) : raw, null);
        }
    }

    private record ParsedNotiError(String message, String errorCode) {}

    private static String normalizeBase(String baseUrl) {
        String b = baseUrl != null ? baseUrl.trim() : DEFAULT_BASE;
        if (b.isEmpty()) {
            b = DEFAULT_BASE;
        }
        return b.replaceAll("/+$", "");
    }

    private static String encodePathSegment(String s) {
        return s.replace(" ", "%20");
    }

    public static String acceptLanguageFromAdminLang(String adminLang) {
        if (adminLang == null || adminLang.isBlank()) {
            return "ko";
        }
        return switch (adminLang.trim().toUpperCase(Locale.ROOT)) {
            case "EN" -> "en";
            case "JP", "JA" -> "ja";
            case "CH", "ZH" -> "zh";
            case "TH" -> "th";
            default -> "ko";
        };
    }
}
