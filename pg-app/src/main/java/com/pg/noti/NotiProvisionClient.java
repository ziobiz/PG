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
 * NOTI Provision API 클라이언트 (JPAY · ElementPay).
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
        return getMerchant(baseUrl, apiKey, merchantId, "jpay", acceptLanguage);
    }

    public Map<String, Object> getMerchant(String baseUrl, String apiKey, String merchantId, String pgKind,
                                           String acceptLanguage) throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new NotiProvisionException("merchantId가 필요합니다.", "VALIDATION");
        }
        String kind = normalizePgKind(pgKind);
        String url = base + "/api/v1/icopay/merchants/" + encodePathSegment(mid) + "?pgKind=" + kind;
        return exchange(HttpMethod.GET, url, apiKey, null, UUID.randomUUID().toString(), acceptLanguage);
    }

    /** 가맹 수정 (NOTI 2차 API). */
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

    /** 가맹 삭제 (NOTI 2차 API). */
    public Map<String, Object> deleteMerchant(String baseUrl, String apiKey, String merchantId,
                                              boolean force, String acceptLanguage)
            throws NotiProvisionException {
        return deleteMerchant(baseUrl, apiKey, merchantId, force, "jpay", acceptLanguage);
    }

    public Map<String, Object> deleteMerchant(String baseUrl, String apiKey, String merchantId,
                                              boolean force, String pgKind, String acceptLanguage)
            throws NotiProvisionException {
        String base = normalizeBase(baseUrl);
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new NotiProvisionException("merchantId가 필요합니다.", "VALIDATION");
        }
        String kind = normalizePgKind(pgKind);
        String url = base + "/api/v1/icopay/merchants/" + encodePathSegment(mid) + "?pgKind=" + kind;
        if (force) {
            url += "&force=true";
        }
        return exchange(HttpMethod.DELETE, url, apiKey, null, UUID.randomUUID().toString(), acceptLanguage);
    }

    public static String normalizePgKind(String pgKind) {
        if (pgKind == null || pgKind.isBlank()) {
            return "jpay";
        }
        String k = pgKind.trim().toLowerCase(Locale.ROOT);
        if ("elementpay".equals(k) || "ep".equals(k) || "element".equals(k)) {
            return "elementpay";
        }
        return "jpay";
    }

    public static boolean isElementPay(String pgKind) {
        return "elementpay".equals(normalizePgKind(pgKind));
    }

    /** NOTI internal-targets 목록. 실패 시 예외 대신 상세 결과 Map 반환. */
    public Map<String, Object> listInternalTargetsDetailed(String baseUrl, String apiKey, String acceptLanguage) {
        Map<String, Object> out = new LinkedHashMap<>();
        String base = normalizeBase(baseUrl);
        String url = base + "/api/v1/icopay/internal-targets";
        out.put("endpoint", url);
        out.put("items", Collections.emptyList());
        if (apiKey == null || apiKey.isBlank()) {
            out.put("status", "NOT_CONFIGURED");
            out.put("httpStatus", 0);
            out.put("message", "Provision API 키가 없습니다. 위 「NOTI Provision API」에서 키를 저장하세요.");
            return out;
        }
        try {
            List<Map<String, Object>> items = exchangeList(
                    HttpMethod.GET, url, apiKey, UUID.randomUUID().toString(), acceptLanguage);
            out.put("items", items != null ? items : Collections.emptyList());
            out.put("httpStatus", 200);
            if (items == null || items.isEmpty()) {
                out.put("status", "EMPTY");
                out.put("message", "NOTI 전산 대상이 비어 있습니다. NOTI 관리화면에서 internal-targets를 등록하거나, 위 JPY/USD/THB 매핑에 ID를 직접 입력하세요.");
            } else {
                out.put("status", "OK");
                out.put("message", "");
            }
            return out;
        } catch (NotiProvisionException e) {
            int http = e.getHttpStatus();
            out.put("httpStatus", http);
            out.put("errorCode", e.getErrorCode() != null ? e.getErrorCode() : "");
            if (http == 404) {
                out.put("status", "NOTI_ENDPOINT_MISSING");
                out.put("message",
                        "NOTI에 목록 API(GET /api/v1/icopay/internal-targets)가 없거나 404입니다. "
                                + "목록은 참고용이며, 위 JPY/USD/THB 매핑에 NOTI 관리화면의 전산 대상 ID를 직접 입력하면 노티생성은 정상 동작합니다.");
            } else if (http == 401 || http == 403) {
                out.put("status", "AUTH_FAILED");
                out.put("message",
                        "Provision API 인증 실패(HTTP " + http + "). NOTI에서 발급한 Bearer 키·허용 IP를 확인하세요.");
            } else {
                out.put("status", "ERROR");
                String msg = e.getMessage() != null ? e.getMessage().trim() : "";
                out.put("message", msg.isEmpty()
                        ? ("NOTI 전산 대상 목록 조회 실패" + (http > 0 ? " (HTTP " + http + ")" : ""))
                        : msg);
            }
            return out;
        } catch (Exception e) {
            out.put("status", "ERROR");
            out.put("httpStatus", 0);
            out.put("message", e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "NOTI 전산 대상 목록 조회 중 오류가 발생했습니다.");
            return out;
        }
    }

    /** NOTI internal-targets 목록 (실패·미구현 시 빈 목록). */
    public List<Map<String, Object>> listInternalTargets(String baseUrl, String apiKey, String acceptLanguage) {
        Map<String, Object> detailed = listInternalTargetsDetailed(baseUrl, apiKey, acceptLanguage);
        Object items = detailed.get("items");
        if (items instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = (Map<String, Object>) m;
                    out.add(row);
                }
            }
            return out;
        }
        return Collections.emptyList();
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
