package com.pg.merchantdeploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantIcopayBrokerCredential;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantIcopayBrokerCredentialRepository;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /api/middleware/v1/pg/**} 호출 시 가맹점 브로커 시크릿 검증.
 * 해당 가맹점에 활성 시크릿 행이 없으면 통과(레거시 호환).
 */
@Component
public class MerchantBrokerAccessVerifier {

    public static final String HEADER_MERCHANT_BROKER_SECRET = "X-Icopay-Merchant-Broker-Secret";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantIcopayBrokerCredentialRepository credentialRepository;

    public MerchantBrokerAccessVerifier(OrgUnitRepository orgUnitRepository,
                                        MerchantIcopayBrokerCredentialRepository credentialRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.credentialRepository = credentialRepository;
    }

    public void verify(HttpServletRequest request, Map<String, Object> jsonBody) {
        String vendorPath = extractVendorSegment(request.getRequestURI());
        String vendorScope = MerchantPgBrokerVendor.fromBrokerPathSegment(vendorPath);
        enforceBrokerSecret(request, jsonBody, vendorScope);
    }

    /**
     * {@code /api/middleware/v1/merchant/{vendor}/...} 가맹점 통합 API용 브로커 시크릿 검증.
     */
    public void verifyMerchantApi(HttpServletRequest request, Map<String, Object> jsonBody, String vendorScope) {
        enforceBrokerSecret(request, jsonBody, MerchantPgBrokerVendor.normalizeScope(vendorScope));
    }

    private void enforceBrokerSecret(HttpServletRequest request, Map<String, Object> jsonBody, String vendorScope) {
        Long orgUnitId = resolveOrgUnitId(request, jsonBody);
        if (orgUnitId == null) {
            return;
        }
        Optional<MerchantIcopayBrokerCredential> credOpt = resolveCredential(orgUnitId, vendorScope);
        if (credOpt.isEmpty()) {
            return;
        }
        MerchantIcopayBrokerCredential cred = credOpt.get();
        if (!"Y".equalsIgnoreCase(cred.getUseYn())) {
            return;
        }
        if (!"Y".equalsIgnoreCase(cred.getEnforceYn())) {
            return;
        }
        String presented = request.getHeader(HEADER_MERCHANT_BROKER_SECRET);
        if (presented == null || presented.isBlank()) {
            throw new SecurityException("브로커 시크릿이 필요합니다. 헤더 " + HEADER_MERCHANT_BROKER_SECRET + " 를 설정하세요.");
        }
        if (!constantTimeEquals(presented.trim(), cred.getBrokerSecret())) {
            throw new SecurityException("브로커 시크릿이 올바르지 않습니다.");
        }
    }

    private Optional<MerchantIcopayBrokerCredential> resolveCredential(Long orgUnitId, String vendorScope) {
        String v = vendorScope != null ? vendorScope.toUpperCase(Locale.ROOT) : MerchantPgBrokerVendor.ALL;
        Optional<MerchantIcopayBrokerCredential> specific =
                credentialRepository.findByOrgUnitIdAndVendorScopeAndUseYn(orgUnitId, v, "Y");
        if (specific.isPresent()) {
            return specific;
        }
        if (!MerchantPgBrokerVendor.ALL.equals(v)) {
            return credentialRepository.findByOrgUnitIdAndVendorScopeAndUseYn(orgUnitId, MerchantPgBrokerVendor.ALL, "Y");
        }
        List<MerchantIcopayBrokerCredential> any =
                credentialRepository.findByOrgUnitIdAndUseYnOrderByIdDesc(orgUnitId, "Y");
        return any.stream().findFirst();
    }

    private static String extractVendorSegment(String uri) {
        if (uri == null) {
            return "";
        }
        int idx = uri.indexOf("/api/middleware/v1/pg/");
        if (idx < 0) {
            return "";
        }
        String tail = uri.substring(idx + "/api/middleware/v1/pg/".length());
        int slash = tail.indexOf('/');
        return slash > 0 ? tail.substring(0, slash) : tail;
    }

    private Long resolveOrgUnitId(HttpServletRequest request, Map<String, Object> jsonBody) {
        String compId = firstNonBlank(request.getParameter("compId"),
                jsonBody != null ? str(jsonBody.get("compId")) : null);
        Long merchantId = parseLong(request.getParameter("merchantId"));
        if (merchantId == null && jsonBody != null) {
            merchantId = parseLong(jsonBody.get("merchantId"));
        }
        if (merchantId != null) {
            return merchantId;
        }
        if (compId != null && !compId.isBlank()) {
            Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId.trim());
            return ou.map(OrgUnit::getId).orElse(null);
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }

    private static Long parseLong(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(x, y);
    }

    /** POST 본문에서 compId 추출(컨트롤러에서 한 번 파싱한 맵 전달) */
    public static Map<String, Object> parseJsonBodyMap(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = MAPPER.readValue(raw, Map.class);
            return m != null ? m : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }
}
