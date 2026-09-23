package com.pg.merchantdeploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantIcopayBrokerCredential;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantIcopayBrokerCredentialRepository;
import com.pg.repository.MerchantProfileRepository;
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
 * {@code /api/middleware/v1/pg/**} · 가맹 통합 API 브로커 시크릿 검증.
 * LIVE / SANDBOX 시크릿을 구분해 요청 속성 {@link #REQ_ATTR_SANDBOX} 에 기록한다.
 */
@Component
public class MerchantBrokerAccessVerifier {

    public static final String HEADER_MERCHANT_BROKER_SECRET = "X-Icopay-Merchant-Broker-Secret";
    public static final String REQ_ATTR_SANDBOX = "pg.broker.sandbox";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantIcopayBrokerCredentialRepository credentialRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public MerchantBrokerAccessVerifier(OrgUnitRepository orgUnitRepository,
                                        MerchantIcopayBrokerCredentialRepository credentialRepository,
                                        MerchantProfileRepository merchantProfileRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.credentialRepository = credentialRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public void verify(HttpServletRequest request, Map<String, Object> jsonBody) {
        String vendorPath = extractVendorSegment(request.getRequestURI());
        String vendorScope = MerchantPgBrokerVendor.fromBrokerPathSegment(vendorPath);
        enforceBrokerSecret(request, jsonBody, vendorScope);
    }

    public void verifyMerchantApi(HttpServletRequest request, Map<String, Object> jsonBody, String vendorScope) {
        enforceBrokerSecret(request, jsonBody, MerchantPgBrokerVendor.normalizeScope(vendorScope));
    }

    public static boolean isSandboxRequest(HttpServletRequest request) {
        return request != null && Boolean.TRUE.equals(request.getAttribute(REQ_ATTR_SANDBOX));
    }

    private void enforceBrokerSecret(HttpServletRequest request, Map<String, Object> jsonBody, String vendorScope) {
        if (request != null) {
            request.setAttribute(REQ_ATTR_SANDBOX, Boolean.FALSE);
        }
        Long orgUnitId = resolveOrgUnitId(request, jsonBody);
        if (orgUnitId == null) {
            return;
        }
        String presented = request != null ? request.getHeader(HEADER_MERCHANT_BROKER_SECRET) : null;
        if (presented != null && !presented.isBlank()) {
            Optional<MerchantIcopayBrokerCredential> bySecret =
                    credentialRepository.findByBrokerSecretAndUseYn(presented.trim(), "Y");
            if (bySecret.isPresent()) {
                MerchantIcopayBrokerCredential cred = bySecret.get();
                if (!orgUnitId.equals(cred.getOrgUnitId())) {
                    throw new SecurityException("브로커 시크릿이 올바르지 않습니다.");
                }
                if (cred.isSandbox()) {
                    assertSandboxEnabled(orgUnitId);
                    if (request != null) {
                        request.setAttribute(REQ_ATTR_SANDBOX, Boolean.TRUE);
                    }
                    return;
                }
                if (!"Y".equalsIgnoreCase(cred.getEnforceYn())) {
                    return;
                }
                return;
            }
        }
        Optional<MerchantIcopayBrokerCredential> credOpt = resolveLiveCredential(orgUnitId, vendorScope);
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
        if (presented == null || presented.isBlank()) {
            throw new SecurityException("브로커 시크릿이 필요합니다. 헤더 " + HEADER_MERCHANT_BROKER_SECRET + " 를 설정하세요.");
        }
        if (!constantTimeEquals(presented.trim(), cred.getBrokerSecret())) {
            throw new SecurityException("브로커 시크릿이 올바르지 않습니다.");
        }
    }

    private void assertSandboxEnabled(Long orgUnitId) {
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty() || !"Y".equalsIgnoreCase(mp.get().getSandboxUseYn())) {
            throw new SecurityException("샌드박스가 비활성입니다. 관리자에게 활성화를 요청하세요.");
        }
    }

    private Optional<MerchantIcopayBrokerCredential> resolveLiveCredential(Long orgUnitId, String vendorScope) {
        String v = vendorScope != null ? vendorScope.toUpperCase(Locale.ROOT) : MerchantPgBrokerVendor.ALL;
        Optional<MerchantIcopayBrokerCredential> specific =
                credentialRepository.findByOrgUnitIdAndVendorScopeAndEnvModeAndUseYn(
                        orgUnitId, v, MerchantIcopayBrokerCredential.ENV_LIVE, "Y");
        if (specific.isPresent()) {
            return specific;
        }
        if (!MerchantPgBrokerVendor.ALL.equals(v)) {
            Optional<MerchantIcopayBrokerCredential> all =
                    credentialRepository.findByOrgUnitIdAndVendorScopeAndEnvModeAndUseYn(
                            orgUnitId, MerchantPgBrokerVendor.ALL, MerchantIcopayBrokerCredential.ENV_LIVE, "Y");
            if (all.isPresent()) {
                return all;
            }
        }
        /* 마이그레이션 전 env_mode 없는 행 호환: vendor만으로 LIVE 우선 */
        Optional<MerchantIcopayBrokerCredential> legacy =
                credentialRepository.findByOrgUnitIdAndVendorScopeAndUseYn(orgUnitId, v, "Y");
        if (legacy.isPresent() && !legacy.get().isSandbox()) {
            return legacy;
        }
        List<MerchantIcopayBrokerCredential> any =
                credentialRepository.findByOrgUnitIdAndUseYnOrderByIdDesc(orgUnitId, "Y");
        return any.stream().filter(c -> !c.isSandbox()).findFirst();
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
        String compId = firstNonBlank(request != null ? request.getParameter("compId") : null,
                jsonBody != null ? str(jsonBody.get("compId")) : null);
        Long merchantId = parseLong(request != null ? request.getParameter("merchantId") : null);
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
