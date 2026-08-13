package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.NotiProvisionLog;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantProfile;
import com.pg.noti.NotiInternalTargetCatalogService;
import com.pg.noti.NotiProvisionClient;
import com.pg.noti.NotiProvisionException;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.NotiProvisionLogRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.HqNotifyEnvService;
import com.pg.service.HqNotifyTargetService;
import com.pg.service.HqNotiWebhookPartnerService;
import com.pg.service.OrgAccessService;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.MerchantJpayNotifyUrlSyncService;
import com.pg.util.PagePermissionCodes;
import com.pg.util.SupervisorAssistantConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 운영관리 — NOTI Provision (노티생성: JPAY · ElementPay).
 */
@Service
public class OpsNotiProvisionService {

    public static final String PAGE_URL = SupervisorAssistantConstants.NOTI_PROVISION_PAGE_URL;

    /** JPY 가맹 자동 슬롯 시작 — j200 */
    public static final int JPY_AUTO_SLOT_START = 200;
    /** USD 가맹 자동 슬롯 시작 — j55 */
    public static final int USD_AUTO_SLOT_START = 55;

    /** 노티생성 OTP 유효 시간(분) — 마지막 성공 등록 후 슬라이딩 */
    public static final int PROVISION_OTP_GRACE_MINUTES = 20;

    private static final long PROVISION_OTP_GRACE_MS = PROVISION_OTP_GRACE_MINUTES * 60L * 1000L;

    /** 사용자별 마지막 노티생성 OTP 통과 시각(ms) */
    private final ConcurrentHashMap<String, Long> provisionOtpPassedAtMs = new ConcurrentHashMap<>();

    private final HqNotifyEnvService hqNotifyEnvService;
    private final HqNotifyTargetService hqNotifyTargetService;
    private final HqNotiWebhookPartnerService hqNotiWebhookPartnerService;
    private final NotiProvisionClient notiProvisionClient;
    private final NotiInternalTargetCatalogService notiInternalTargetCatalogService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final NotiProvisionLogRepository notiProvisionLogRepository;
    private final MerchantJpayNotifyUrlSyncService merchantJpayNotifyUrlSyncService;
    private final AuthService authService;
    private final OrgPagePermissionService orgPagePermissionService;
    private final OrgAccessService orgAccessService;

    private static final DateTimeFormatter PROVISION_LOG_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter PROVISION_LOG_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter PROVISION_LOG_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public OpsNotiProvisionService(HqNotifyEnvService hqNotifyEnvService,
                                   HqNotifyTargetService hqNotifyTargetService,
                                   HqNotiWebhookPartnerService hqNotiWebhookPartnerService,
                                   NotiProvisionClient notiProvisionClient,
                                   NotiInternalTargetCatalogService notiInternalTargetCatalogService,
                                   OrgUnitRepository orgUnitRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                   NotiProvisionLogRepository notiProvisionLogRepository,
                                   MerchantJpayNotifyUrlSyncService merchantJpayNotifyUrlSyncService,
                                   AuthService authService,
                                   OrgPagePermissionService orgPagePermissionService,
                                   OrgAccessService orgAccessService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hqNotifyTargetService = hqNotifyTargetService;
        this.hqNotiWebhookPartnerService = hqNotiWebhookPartnerService;
        this.notiProvisionClient = notiProvisionClient;
        this.notiInternalTargetCatalogService = notiInternalTargetCatalogService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.notiProvisionLogRepository = notiProvisionLogRepository;
        this.merchantJpayNotifyUrlSyncService = merchantJpayNotifyUrlSyncService;
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.orgAccessService = orgAccessService;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        Map<String, Object> m = new LinkedHashMap<>();
        Optional<String> deny = accessDeniedReason(authentication);
        m.put("allowed", deny.isEmpty());
        m.put("denyReason", deny.orElse(null));
        m.put("canWrite", deny.isEmpty() && canWrite(authentication));
        m.put("pageUrl", PAGE_URL);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        m.put("provisionConfigured", isProvisionConfigured(cfg));
        m.put("notiProvisionEnabledYn", yn(cfg.getNotiProvisionEnabledYn()));
        m.put("notiProvisionDefaultInternalTargetId",
                nz(cfg.getNotiProvisionDefaultInternalTargetId()));
        m.put("notiProvisionInternalTargetJpy", nz(cfg.getNotiProvisionInternalTargetJpy()));
        m.put("notiProvisionInternalTargetUsd", nz(cfg.getNotiProvisionInternalTargetUsd()));
        m.put("notiProvisionInternalTargetThb", nz(cfg.getNotiProvisionInternalTargetThb()));
        m.put("notiProvisionDefaultDealmaiPartner", nz(cfg.getNotiProvisionDefaultDealmaiPartner()));
        m.put("notiProvisionBaseUrl", nz(cfg.getNotiProvisionBaseUrl()));
        AppUser user = resolveUser(authentication);
        if (user != null && user.getUsername() != null) {
            String uname = user.getUsername().trim();
            boolean otpRequired = requiresProvisionOtp(uname);
            m.put("otpRequiredForProvision", otpRequired);
            m.put("otpGraceMinutes", PROVISION_OTP_GRACE_MINUTES);
            if (!otpRequired) {
                Long at = provisionOtpPassedAtMs.get(uname);
                if (at != null) {
                    m.put("otpGraceExpiresAtMs", at + PROVISION_OTP_GRACE_MS);
                }
            }
        } else {
            m.put("otpRequiredForProvision", true);
            m.put("otpGraceMinutes", PROVISION_OTP_GRACE_MINUTES);
        }
        return m;
    }

    private static boolean isProvisionConfigured(HqNotifyEnvConfig cfg) {
        return "Y".equalsIgnoreCase(yn(cfg.getNotiProvisionEnabledYn()))
                && cfg.getNotiProvisionBaseUrl() != null && !cfg.getNotiProvisionBaseUrl().isBlank()
                && cfg.getNotiProvisionApiKey() != null && !cfg.getNotiProvisionApiKey().isBlank();
    }

    public Optional<String> accessDeniedReason(Authentication authentication) {
        AppUser user = resolveUser(authentication);
        if (user == null) {
            return Optional.of("로그인이 필요합니다.");
        }
        if (!orgPagePermissionService.isFullAccessAdmin(user)
                && !orgPagePermissionService.isHeadquartersAdmin(user)) {
            String ut = user.getUserType() != null ? user.getUserType().trim() : "";
            if (!"ASSISTANT".equalsIgnoreCase(ut)
                    || !SupervisorAssistantConstants.isSupervisorRoleType(user.getAssistantRoleType())) {
                return Optional.of("SUPERVISOR 역할 사용자만 노티생성을 사용할 수 있습니다.");
            }
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) {
            return Optional.of("조직 정보를 확인할 수 없습니다.");
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        if (!orgPagePermissionService.isHeadquartersAdmin(user)
                && !"HEADQUARTERS".equals(level) && !"REGIONAL".equals(level) && !"MASTER_DIST".equals(level)) {
            return Optional.of("총본사·본사·총판 조직의 SUPERVISOR만 사용할 수 있습니다.");
        }
        Map<String, String> perms = orgPagePermissionService.resolvePagePermissionsForUser(user);
        if (perms != null) {
            String p = perms.get(PAGE_URL);
            if (p != null) {
                String b = PagePermissionCodes.base(p);
                if (PagePermissionCodes.P_NONE.equals(b) || PagePermissionCodes.P_OBSERVER.equals(b)) {
                    return Optional.of("본사권한설정에서 이 메뉴 권한이 없습니다.");
                }
            }
        }
        return Optional.empty();
    }

    public boolean canWrite(Authentication authentication) {
        AppUser user = resolveUser(authentication);
        if (user == null) {
            return false;
        }
        if (orgPagePermissionService.isFullAccessAdmin(user)) {
            return true;
        }
        if (accessDeniedReason(authentication).isPresent()) {
            return false;
        }
        Map<String, String> perms = orgPagePermissionService.resolvePagePermissionsForUser(user);
        if (perms == null) {
            return true;
        }
        String p = perms.get(PAGE_URL);
        if (p == null) {
            return true;
        }
        return PagePermissionCodes.canWriteLike(p);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> merchantStatus(Authentication authentication, String compId) throws NotiProvisionException {
        assertAccess(authentication);
        OrgUnit merchant = resolveMerchant(authentication, compId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", merchant.getCode());
        out.put("compNm", merchant.getName() != null ? merchant.getName() : "");
        out.put("orgUnitId", merchant.getId());
        out.put("jpayNotifyUrl", findNotifyUrl(merchant.getId(), MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY));
        out.put("jpayCallbackUrl", findNotifyUrl(merchant.getId(), MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK));
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        if (isProvisionConfigured(cfg)) {
            try {
                Map<String, Object> remote = getMerchantPreferring(cfg, merchant.getCode(), "jpay", "ko");
                out.put("notiRemote", remote);
                out.put("pgKind", remote.get("pgKind") != null
                        ? NotiProvisionClient.normalizePgKind(String.valueOf(remote.get("pgKind")))
                        : "jpay");
            } catch (NotiProvisionException e) {
                out.put("notiRemoteError", e.getMessage());
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> merchantContext(Authentication authentication, String compId, String adminLang)
            throws NotiProvisionException {
        assertAccess(authentication);
        OrgUnit merchant = resolveMerchant(authentication, compId);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        String baseCurrency = resolveMerchantBaseCurrency(merchant.getId());
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(adminLang);
        List<Map<String, Object>> notiTargets = notiInternalTargetCatalogService.listFromNoti(cfg, acceptLang);
        String suggestedInternalRaw = resolveInternalTargetForCurrency(cfg, baseCurrency, Map.of());
        if (suggestedInternalRaw == null || suggestedInternalRaw.isBlank()) {
            suggestedInternalRaw = notiInternalTargetCatalogService.findIdByCurrency(notiTargets, baseCurrency);
        }
        String suggestedInternal = notiInternalTargetCatalogService.resolveCanonicalId(suggestedInternalRaw, notiTargets);
        if (suggestedInternal.isEmpty()) {
            suggestedInternal = suggestedInternalRaw;
        }
        MasterDistNotifyCtx mdCtx = resolveMasterDistNotifyContext(merchant.getId());
        int slotStart = autoSlotStartForCurrency(baseCurrency);
        int nextSlot = resolveNextAutoSlotNo(baseCurrency);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", merchant.getCode());
        out.put("compNm", merchant.getName() != null ? merchant.getName() : "");
        out.put("orgUnitId", merchant.getId());
        out.put("baseCurrency", baseCurrency);
        out.put("suggestedMerchantId", merchant.getCode());
        out.put("suggestedInternalTargetId", suggestedInternal);
        out.put("autoSlotStart", slotStart);
        out.put("suggestedAutoSlotNo", nextSlot);
        out.put("suggestedAutoRouteNo", "j" + nextSlot);
        out.put("internalTargetOptions", notiInternalTargetCatalogService.buildSelectOptions(notiTargets, cfg));
        out.put("notiInternalTargets", notiTargets);
        out.put("masterDistOrgId", mdCtx.masterDistOrgId());
        out.put("masterDistCode", mdCtx.masterDistCode());
        out.put("masterDistName", mdCtx.masterDistName());
        out.put("devNotifyCallbackUrl", nz(mdCtx.callbackUrl()));
        out.put("devNotifyResultUrl", nz(mdCtx.resultUrl()));
        out.put("merchantCallbackUrl", findNotifyUrl(merchant.getId(), "BACKGROUND"));
        out.put("merchantResultUrl", findNotifyUrl(merchant.getId(), "RESULT"));
        out.put("jpayNotifyUrl", findNotifyUrl(merchant.getId(), MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY));
        out.put("jpayCallbackUrl", findNotifyUrl(merchant.getId(), MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK));
        out.put("dealmaiPartners", hqNotiWebhookPartnerService.listActive());
        out.put("defaultDealmaiPartner", nz(cfg.getNotiProvisionDefaultDealmaiPartner()));
        out.put("notiBaseUrl", NotiProvisionClient.defaultBaseUrlIfBlank(cfg.getNotiProvisionBaseUrl()));

        if (isProvisionConfigured(cfg)) {
            try {
                Map<String, Object> remote = getMerchantPreferring(cfg, merchant.getCode(), "jpay", "ko");
                out.put("notiRemote", remote);
                out.put("merchantIdExists", true);
                out.put("pgKind", remote.get("pgKind") != null
                        ? NotiProvisionClient.normalizePgKind(String.valueOf(remote.get("pgKind")))
                        : "jpay");
            } catch (NotiProvisionException e) {
                if (isMerchantNotFound(e)) {
                    out.put("merchantIdExists", false);
                } else {
                    out.put("notiRemoteError", e.getMessage());
                }
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> nextAutoSlot(Authentication authentication, String baseCurrency)
            throws NotiProvisionException {
        assertAccess(authentication);
        String cur = normalizeBaseCurrency(baseCurrency);
        int start = autoSlotStartForCurrency(cur);
        int next = resolveNextAutoSlotNo(cur);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        String base = NotiProvisionClient.defaultBaseUrlIfBlank(cfg.getNotiProvisionBaseUrl());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseCurrency", cur);
        out.put("autoSlotStart", start);
        out.put("slotNo", next);
        out.put("routeNo", "j" + next);
        out.put("pgCallbackUrl", base + "/noti/callback/j" + next);
        out.put("pgResultUrl", base + "/noti/result/j" + next);
        out.put("message", cur.contains("USD")
                ? "USD 가맹 자동 슬롯 j" + next + " (시작 j" + USD_AUTO_SLOT_START + ")"
                : "JPY 가맹 자동 슬롯 j" + next + " (시작 j" + JPY_AUTO_SLOT_START + ")");
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkMerchantId(Authentication authentication, String merchantId, String compId,
                                               String pgKind) throws NotiProvisionException {
        assertAccess(authentication);
        if (compId != null && !compId.trim().isEmpty()) {
            resolveMerchant(authentication, compId);
        }
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new IllegalArgumentException("가맹점 ID를 입력하세요.");
        }
        String kind = NotiProvisionClient.normalizePgKind(pgKind);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merchantId", mid);
        out.put("pgKind", kind);
        out.put("available", true);
        try {
            Map<String, Object> remote = notiProvisionClient.getMerchant(
                    cfg.getNotiProvisionBaseUrl(), cfg.getNotiProvisionApiKey(), mid, kind, "ko");
            out.put("available", false);
            out.put("existing", remote);
            String existingComp = remote.get("merchantId") != null ? String.valueOf(remote.get("merchantId")) : mid;
            if (compId != null && !compId.trim().isEmpty() && compId.trim().equalsIgnoreCase(existingComp)) {
                out.put("sameComp", true);
                out.put("message", "동일 가맹점 ID가 NOTI에 이미 등록되어 있습니다(재생성·갱신 가능).");
            } else {
                out.put("sameComp", false);
                out.put("message", "NOTI에 동일 가맹점 ID가 이미 사용 중입니다.");
            }
        } catch (NotiProvisionException e) {
            if (isMerchantNotFound(e)) {
                out.put("message", "사용 가능한 가맹점 ID입니다.");
            } else {
                throw e;
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkJpaySlot(Authentication authentication, Integer slotNo, String merchantId)
            throws NotiProvisionException {
        assertAccess(authentication);
        if (slotNo == null || slotNo < 1 || slotNo > 999) {
            throw new IllegalArgumentException("PG 노티 슬롯 번호는 1~999 범위여야 합니다.");
        }
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        String base = NotiProvisionClient.defaultBaseUrlIfBlank(cfg.getNotiProvisionBaseUrl());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("slotNo", slotNo);
        out.put("routeNo", "j" + slotNo);
        out.put("pgCallbackUrl", base + "/noti/callback/j" + slotNo);
        out.put("pgResultUrl", base + "/noti/result/j" + slotNo);
        out.put("available", true);
        try {
            Map<String, Object> chk = notiProvisionClient.checkJpaySlot(
                    cfg.getNotiProvisionBaseUrl(), cfg.getNotiProvisionApiKey(), slotNo, merchantId, "ko");
            if (chk != null) {
                out.putAll(chk);
            }
        } catch (NotiProvisionException e) {
            if ("JPAY_ROUTE_CONFLICT".equalsIgnoreCase(e.getErrorCode())) {
                out.put("available", false);
                out.put("message", e.getMessage());
            } else if ("NOTI_HTTP".equalsIgnoreCase(e.getErrorCode()) && e.getHttpStatus() == 404) {
                out.put("message", "NOTI 슬롯 조회 API 미지원 — 생성 시 NOTI에서 중복 검증됩니다.");
            } else {
                throw e;
            }
        }
        return out;
    }

    @Transactional
    public Map<String, Object> provision(Authentication authentication, Map<String, Object> body)
            throws NotiProvisionException {
        assertWrite(authentication);
        AppUser user = resolveUser(authentication);
        if (user == null || user.getUsername() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = user.getUsername().trim();
        if (requiresProvisionOtp(username)) {
            String totp = str(body, "totpCode");
            if (totp.isEmpty() && body != null && body.get("otp") != null) {
                totp = String.valueOf(body.get("otp")).trim();
            }
            authService.verifyTotpOrThrow(user, totp);
        }
        String compId = str(body, "compId");
        if (compId.isEmpty()) {
            throw new IllegalArgumentException("가맹 업체코드를 입력하세요.");
        }
        OrgUnit merchant = resolveMerchant(authentication, compId);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        String baseCurrency = resolveMerchantBaseCurrency(merchant.getId());

        String merchantId = str(body, "merchantId");
        if (merchantId.isEmpty()) {
            merchantId = merchant.getCode();
        }

        String integrationMode = resolveIntegrationMode(body);
        boolean urlIntegrationMode = isPureUrlIntegrationMode(integrationMode);
        boolean urlHybridMode = isUrlHybridIntegrationMode(integrationMode);
        boolean urlFamilyMode = urlIntegrationMode || urlHybridMode;
        boolean enableRelay = urlIntegrationMode ? false
                : (urlHybridMode || !"N".equalsIgnoreCase(str(body, "enableRelayYn")));
        boolean enableInternal = urlFamilyMode ? false : "Y".equalsIgnoreCase(str(body, "enableInternalYn"));
        boolean enableDevInternal = urlFamilyMode || !"N".equalsIgnoreCase(str(body, "enableDevInternalYn"));
        boolean slotAuto = "Y".equalsIgnoreCase(str(body, "slotAutoYn"));
        String adminLang = str(body, "adminLang");
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(adminLang);
        List<Map<String, Object>> notiTargets = notiInternalTargetCatalogService.listFromNoti(cfg, acceptLang);

        String internalTargetId = str(body, "internalTargetId");
        if (internalTargetId.isEmpty()) {
            internalTargetId = resolveInternalTargetForCurrency(cfg, baseCurrency, body);
        }
        if (internalTargetId.isEmpty()) {
            internalTargetId = notiInternalTargetCatalogService.findIdByCurrency(notiTargets, baseCurrency);
        }
        String rawInternal = internalTargetId;
        internalTargetId = notiInternalTargetCatalogService.resolveCanonicalId(rawInternal, notiTargets);
        if (!rawInternal.isEmpty() && !notiTargets.isEmpty() && internalTargetId.isEmpty()) {
            throw new IllegalArgumentException("등록되지 않은 NOTI 전산 대상 ID입니다: " + rawInternal);
        }
        if (enableInternal && internalTargetId.isEmpty()) {
            throw new IllegalArgumentException("전산 노티 사용 시 NOTI 전산 대상 ID를 선택하세요.");
        }

        String callbackUrl = urlIntegrationMode ? "" : str(body, "callbackUrl");
        String resultUrl = urlIntegrationMode ? "" : str(body, "resultUrl");
        MasterDistNotifyCtx mdCtx = resolveMasterDistNotifyContext(merchant.getId());
        String relayOffDevCallbackUrl = "";
        String relayOffDevResultUrl = "";

        if (urlIntegrationMode) {
            /* URL 방식: 가맹점 callback/result 비움 + 대체송부=개발(가공) + 개발노티 전용사용 */
            callbackUrl = "";
            resultUrl = "";
            relayOffDevCallbackUrl = nz(mdCtx.callbackUrl());
            relayOffDevResultUrl = nz(mdCtx.resultUrl());
            if (relayOffDevCallbackUrl.isEmpty() || relayOffDevResultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 방식은 총판의 본사 노티 대상(개발 CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        } else if (urlHybridMode) {
            /* URL 하이브리드: URL 대체송부(개발) + 가맹점 노티(callback/result) 송부 */
            if (callbackUrl.isEmpty()) {
                callbackUrl = findNotifyUrl(merchant.getId(), "BACKGROUND");
            }
            if (resultUrl.isEmpty()) {
                resultUrl = findNotifyUrl(merchant.getId(), "RESULT");
            }
            if (callbackUrl.isEmpty() || resultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 하이브리드 방식은 가맹점 callback·result URL이 필요합니다. 업체관리 URL 또는 직접 입력하세요.");
            }
            relayOffDevCallbackUrl = nz(mdCtx.callbackUrl());
            relayOffDevResultUrl = nz(mdCtx.resultUrl());
            if (relayOffDevCallbackUrl.isEmpty() || relayOffDevResultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 하이브리드 방식은 총판의 본사 노티 대상(개발 CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        } else if (enableRelay) {
            if (callbackUrl.isEmpty()) {
                callbackUrl = findNotifyUrl(merchant.getId(), "BACKGROUND");
            }
            if (resultUrl.isEmpty()) {
                resultUrl = findNotifyUrl(merchant.getId(), "RESULT");
            }
            if (callbackUrl.isEmpty() || resultUrl.isEmpty()) {
                throw new IllegalArgumentException("가맹점 노티 사용 시 callback·result URL이 필요합니다. 업체관리 URL 또는 직접 입력하세요.");
            }
        } else if (enableDevInternal) {
            callbackUrl = nz(mdCtx.callbackUrl());
            resultUrl = nz(mdCtx.resultUrl());
            if (callbackUrl.isEmpty() || resultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "개발 노티 사용 시 총판의 본사 노티 대상(CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        } else {
            callbackUrl = "";
            resultUrl = "";
        }

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("enableRelay", enableRelay);
        options.put("enableInternal", enableInternal);
        options.put("enableDevInternal", enableDevInternal);
        options.put("relayFormat", mapRelayFormat(str(body, "relayFormat")));
        options.put("relayMode", mapRelayMode(str(body, "relayMode")));
        options.put("resultDeliveryMode", mapResultDeliveryMode(
                urlFamilyMode ? "AUTO" : str(body, "resultDeliveryMode")));
        if (urlFamilyMode) {
            options.put("relayOffForwardTarget", "dev_internal");
            options.put("relayOffDevCallbackUrl", relayOffDevCallbackUrl);
            options.put("relayOffDevResultUrl", relayOffDevResultUrl);
            /* URL=전용개발, 하이브리드=가맹 우선·끊길 때 대체 개발 */
            options.put("relayOffDevDedicatedUse", urlIntegrationMode);
        }
        String dealmai = resolveDealmaiPartner(cfg, str(body, "dealmaiPartnerCode"));
        /* URL·URL 하이브리드: Partner 코드가 있으면 DEALMAI 웹훅 강제 ON */
        boolean enableDealmaiWebhook = urlFamilyMode
                ? !dealmai.isEmpty()
                : !"N".equalsIgnoreCase(str(body, "enableDealmaiWebhookYn"));
        if (!dealmai.isEmpty() && enableDealmaiWebhook) {
            options.put("enableDealmaiWebhook", true);
            options.put("dealmaiPartnerCode", dealmai);
        } else if (!dealmai.isEmpty()) {
            options.put("dealmaiPartnerCode", dealmai);
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("merchantId", merchantId);
        String pgKind = NotiProvisionClient.normalizePgKind(str(body, "pgKind"));
        boolean elementPay = NotiProvisionClient.isElementPay(pgKind);
        req.put("pgKind", pgKind);
        if (!dealmai.isEmpty()) {
            req.put("dealmaiPartnerCode", dealmai);
            if (enableDealmaiWebhook) {
                req.put("enableDealmaiWebhook", true);
            }
        }
        if (urlFamilyMode) {
            req.put("relayOffForwardTarget", "dev_internal");
            req.put("relayOffDevCallbackUrl", relayOffDevCallbackUrl);
            req.put("relayOffDevResultUrl", relayOffDevResultUrl);
            req.put("relayOffDevDedicatedUse", urlIntegrationMode);
        }
        if (!internalTargetId.isEmpty()) {
            req.put("internalTargetId", internalTargetId);
        }
        Integer jpaySlotNo = null;
        if (!elementPay) {
            jpaySlotNo = parseOptionalInt(body.get("jpaySlotNo"));
            if (slotAuto) {
                jpaySlotNo = resolveNextAutoSlotNo(baseCurrency);
            }
            if (jpaySlotNo != null) {
                req.put("jpaySlotNo", jpaySlotNo);
                req.put("routeNo", "j" + jpaySlotNo);
            }
        }
        /* URL 방식은 가맹점 URL 필드를 명시적으로 비움. 하이브리드는 가맹 URL 송부 */
        if (urlIntegrationMode) {
            req.put("callbackUrl", "");
            req.put("resultUrl", "");
        } else {
            if (!callbackUrl.isEmpty()) {
                req.put("callbackUrl", callbackUrl);
            }
            if (!resultUrl.isEmpty()) {
                req.put("resultUrl", resultUrl);
            }
        }
        req.put("options", options);
        Map<String, Object> icopayMeta = buildIcopayProvisionMeta(
                merchant, integrationMode,
                user != null ? user.getUsername() : null);
        req.put("icopayMeta", icopayMeta);

        Map<String, Object> data = notiProvisionClient.provision(
                cfg.getNotiProvisionBaseUrl(),
                cfg.getNotiProvisionApiKey(),
                req,
                NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang")));

        String jpayNotify = "";
        String jpayCallback = "";
        if (!elementPay) {
            String[] jpayUrls = resolveProvisionedJpayUrls(data, cfg, jpaySlotNo);
            jpayNotify = jpayUrls[0];
            jpayCallback = jpayUrls[1];
            if (!jpayNotify.isEmpty() || !jpayCallback.isEmpty()) {
                merchantJpayNotifyUrlSyncService.persist(merchant.getId(), jpayNotify, jpayCallback);
            }
        } else {
            String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(cfg.getNotiProvisionBaseUrl());
            jpayNotify = firstNonBlank(data, "elementpayWebhookUrl");
            if (jpayNotify.isEmpty()) {
                jpayNotify = notiBase + "/noti/elementpay";
            }
            jpayCallback = firstNonBlank(data, "elementpayResultUrl");
            if (jpayCallback.isEmpty()) {
                jpayCallback = notiBase + "/noti/result/elementpay";
            }
            merchantJpayNotifyUrlSyncService.persist(merchant.getId(), jpayNotify, jpayCallback);
        }
        saveProvisionLog(merchant, merchantId, pgKind, baseCurrency, internalTargetId, dealmai, data, jpayNotify, jpayCallback, user,
                integrationMode);
        markProvisionOtpPassed(username);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", Boolean.TRUE.equals(data.get("created"))
                ? (elementPay ? "NOTI ElementPay 가맹이 생성되었습니다." : "NOTI JPAY 가맹이 생성되었습니다.")
                : (elementPay ? "기존 NOTI ElementPay 가맹과 동일합니다." : "기존 NOTI JPAY 가맹과 동일합니다."));
        out.put("provision", data);
        out.put("pgKind", pgKind);
        out.put("jpayNotifyUrl", jpayNotify);
        out.put("jpayCallbackUrl", jpayCallback);
        if (elementPay) {
            out.put("elementpayWebhookUrl", jpayNotify);
            out.put("elementpayResultUrl", jpayCallback);
        }
        out.put("compId", merchant.getCode());
        out.put("compNm", merchant.getName());
        out.put("merchantId", merchantId);
        out.put("dealmaiPartnerCode", dealmai);
        out.put("otpRequiredForProvision", requiresProvisionOtp(username));
        out.put("otpGraceMinutes", PROVISION_OTP_GRACE_MINUTES);
        return out;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> list(Authentication authentication,
                                                String searchCompId,
                                                int page,
                                                int size) {
        assertAccess(authentication);
        int p = Math.max(1, page);
        int s = Math.min(Math.max(size, 1), 200);
        String compQ = searchCompId != null ? searchCompId.trim() : "";
        Page<NotiProvisionLog> pg = notiProvisionLogRepository.search(
                compQ, PageRequest.of(p - 1, s));
        return PageResult.of(pg, this::toLogRow);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> logDetail(Authentication authentication, Long logId, String adminLang) throws NotiProvisionException {
        assertAccess(authentication);
        NotiProvisionLog log = requireLog(logId);
        assertLogInViewerScope(authentication, log);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(adminLang);
        List<Map<String, Object>> notiTargets = notiInternalTargetCatalogService.listFromNoti(cfg, acceptLang);
        Map<String, Object> out = toLogRow(log);
        out.put("merchantId", resolveLogMerchantId(log));
        out.put("internalTargetOptions", notiInternalTargetCatalogService.buildSelectOptions(notiTargets, cfg));
        out.put("dealmaiPartners", hqNotiWebhookPartnerService.listActive());
        out.put("defaultDealmaiPartner", nz(cfg.getNotiProvisionDefaultDealmaiPartner()));
        if (isProvisionConfigured(cfg)) {
            try {
                String logPgKind = resolveLogPgKind(log);
                out.put("notiRemote", notiProvisionClient.getMerchant(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        resolveLogMerchantId(log),
                        logPgKind,
                        "ko"));
            } catch (NotiProvisionException e) {
                if (!isMerchantNotFound(e)) {
                    out.put("notiRemoteError", e.getMessage());
                }
            }
        }
        return out;
    }

    @Transactional
    public Map<String, Object> updateLog(Authentication authentication, Map<String, Object> body)
            throws NotiProvisionException {
        assertWrite(authentication);
        verifyProvisionOtpIfNeeded(authentication, body);
        Long logId = parseLogId(body);
        NotiProvisionLog log = requireLog(logId);
        assertLogInViewerScope(authentication, log);
        OrgUnit merchant = orgUnitRepository.findById(log.getOrgUnitId())
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        String merchantId = resolveLogMerchantId(log);
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang"));
        List<Map<String, Object>> notiTargets = notiInternalTargetCatalogService.listFromNoti(cfg, acceptLang);

        String internalTargetId = str(body, "internalTargetId");
        if (internalTargetId.isEmpty()) {
            internalTargetId = nz(log.getInternalTargetId());
        }
        String rawInternal = internalTargetId;
        internalTargetId = notiInternalTargetCatalogService.resolveCanonicalId(rawInternal, notiTargets);
        if (!rawInternal.isEmpty() && !notiTargets.isEmpty() && internalTargetId.isEmpty()) {
            throw new IllegalArgumentException("등록되지 않은 NOTI 전산 대상 ID입니다: " + rawInternal);
        }

        String integrationMode = resolveIntegrationMode(body);
        boolean urlIntegrationMode = isPureUrlIntegrationMode(integrationMode);
        boolean urlHybridMode = isUrlHybridIntegrationMode(integrationMode);
        boolean urlFamilyMode = urlIntegrationMode || urlHybridMode;
        boolean enableRelay = urlIntegrationMode ? false
                : (urlHybridMode || !"N".equalsIgnoreCase(str(body, "enableRelayYn")));
        boolean enableInternal = urlFamilyMode ? false : "Y".equalsIgnoreCase(str(body, "enableInternalYn"));
        boolean enableDevInternal = urlFamilyMode || !"N".equalsIgnoreCase(str(body, "enableDevInternalYn"));
        String callbackUrl = urlIntegrationMode ? "" : str(body, "callbackUrl");
        String resultUrl = urlIntegrationMode ? "" : str(body, "resultUrl");
        MasterDistNotifyCtx mdCtx = resolveMasterDistNotifyContext(merchant.getId());
        String relayOffDevCallbackUrl = "";
        String relayOffDevResultUrl = "";
        if (urlIntegrationMode) {
            callbackUrl = "";
            resultUrl = "";
            relayOffDevCallbackUrl = nz(mdCtx.callbackUrl());
            relayOffDevResultUrl = nz(mdCtx.resultUrl());
            if (relayOffDevCallbackUrl.isEmpty() || relayOffDevResultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 방식은 총판의 본사 노티 대상(개발 CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        } else if (urlHybridMode) {
            if (callbackUrl.isEmpty()) {
                callbackUrl = findNotifyUrl(merchant.getId(), "BACKGROUND");
            }
            if (resultUrl.isEmpty()) {
                resultUrl = findNotifyUrl(merchant.getId(), "RESULT");
            }
            if (callbackUrl.isEmpty() || resultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 하이브리드 방식은 가맹점 callback·result URL이 필요합니다. 업체관리 URL 또는 직접 입력하세요.");
            }
            relayOffDevCallbackUrl = nz(mdCtx.callbackUrl());
            relayOffDevResultUrl = nz(mdCtx.resultUrl());
            if (relayOffDevCallbackUrl.isEmpty() || relayOffDevResultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "URL 하이브리드 방식은 총판의 본사 노티 대상(개발 CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        } else if (enableRelay) {
            if (callbackUrl.isEmpty()) {
                callbackUrl = findNotifyUrl(merchant.getId(), "BACKGROUND");
            }
            if (resultUrl.isEmpty()) {
                resultUrl = findNotifyUrl(merchant.getId(), "RESULT");
            }
        } else if (enableDevInternal) {
            callbackUrl = nz(mdCtx.callbackUrl());
            resultUrl = nz(mdCtx.resultUrl());
            if (callbackUrl.isEmpty() || resultUrl.isEmpty()) {
                throw new IllegalArgumentException(
                        "개발 노티 사용 시 총판의 본사 노티 대상(CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
            }
        }

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("enableRelay", enableRelay);
        options.put("enableInternal", enableInternal);
        options.put("enableDevInternal", enableDevInternal);
        options.put("relayFormat", mapRelayFormat(str(body, "relayFormat")));
        options.put("relayMode", mapRelayMode(str(body, "relayMode")));
        options.put("resultDeliveryMode", mapResultDeliveryMode(
                urlFamilyMode ? "AUTO" : str(body, "resultDeliveryMode")));
        if (urlFamilyMode) {
            options.put("relayOffForwardTarget", "dev_internal");
            options.put("relayOffDevCallbackUrl", relayOffDevCallbackUrl);
            options.put("relayOffDevResultUrl", relayOffDevResultUrl);
            options.put("relayOffDevDedicatedUse", urlIntegrationMode);
        }
        String dealmai = resolveDealmaiPartner(cfg, str(body, "dealmaiPartnerCode"));
        boolean enableDealmaiWebhook = urlFamilyMode
                ? !dealmai.isEmpty()
                : !"N".equalsIgnoreCase(str(body, "enableDealmaiWebhookYn"));
        if (!dealmai.isEmpty() && enableDealmaiWebhook) {
            options.put("enableDealmaiWebhook", true);
            options.put("dealmaiPartnerCode", dealmai);
        } else if (!dealmai.isEmpty()) {
            options.put("dealmaiPartnerCode", dealmai);
        }

        Map<String, Object> fallbackReq = buildProvisionFallbackReq(
                merchant, merchantId, log, internalTargetId, callbackUrl, resultUrl, options, dealmai, enableDealmaiWebhook);
        if (urlFamilyMode) {
            if (urlIntegrationMode) {
                fallbackReq.put("callbackUrl", "");
                fallbackReq.put("resultUrl", "");
            }
            fallbackReq.put("relayOffForwardTarget", "dev_internal");
            fallbackReq.put("relayOffDevCallbackUrl", relayOffDevCallbackUrl);
            fallbackReq.put("relayOffDevResultUrl", relayOffDevResultUrl);
            fallbackReq.put("relayOffDevDedicatedUse", urlIntegrationMode);
        }
        fallbackReq.put("icopayMeta", buildIcopayProvisionMeta(
                merchant, integrationMode, resolveUsername(authentication)));

        // ICOPAY에서 생성한 이력은 ICOPAY에서 수정해야 NOTI와 양방향이 유지된다.
        // 1) PUT(미들웨어 UI와 동일) → 2) 실패/미지원/설정충돌 시 동일 슬롯 강제 교체 → 3) 잔존 시 PUT 재시도
        Map<String, Object> data = upsertMerchantKeepingSlot(
                cfg, merchantId, fallbackReq, acceptLang);

        String logPgKind = resolveLogPgKind(log);
        boolean elementPay = NotiProvisionClient.isElementPay(logPgKind);
        String jpayNotify = "";
        String jpayCallback = "";
        if (!elementPay) {
            Integer updateSlot = extractSlotNo(data);
            if (updateSlot == null) {
                updateSlot = log.getSlotNo();
            }
            String[] jpayUrls = resolveProvisionedJpayUrls(data, cfg, updateSlot);
            jpayNotify = jpayUrls[0];
            jpayCallback = jpayUrls[1];
            if (jpayNotify.isEmpty() && log.getJpayNotifyUrl() != null) {
                jpayNotify = log.getJpayNotifyUrl().trim();
            }
            if (jpayCallback.isEmpty() && log.getJpayCallbackUrl() != null) {
                jpayCallback = log.getJpayCallbackUrl().trim();
            }
            if (!jpayNotify.isEmpty() || !jpayCallback.isEmpty()) {
                merchantJpayNotifyUrlSyncService.persist(merchant.getId(), jpayNotify, jpayCallback);
            }
        }
        applyLogUpdate(log, internalTargetId, dealmai, data, jpayNotify, jpayCallback,
                integrationMode);
        markProvisionOtpPassed(resolveUsername(authentication));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "노티 생성 이력이 수정되었습니다.");
        out.put("log", toLogRow(log));
        return out;
    }

    @Transactional
    public Map<String, Object> deleteLog(Authentication authentication, Map<String, Object> body)
            throws NotiProvisionException {
        assertWrite(authentication);
        verifyProvisionOtpIfNeeded(authentication, body);
        Long logId = parseLogId(body);
        boolean force = "Y".equalsIgnoreCase(str(body, "forceYn"));
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang"));
        deleteOneLog(authentication, logId, force, acceptLang);
        markProvisionOtpPassed(resolveUsername(authentication));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "노티 생성 이력이 삭제되었습니다. NOTI 미들웨어에서도 제거되었습니다.");
        out.put("id", logId);
        return out;
    }

    /**
     * 선택 이력 일괄 삭제 (OTP 1회 검증).
     * body.ids: number[] | comma-separated string
     */
    @Transactional
    public Map<String, Object> deleteLogs(Authentication authentication, Map<String, Object> body)
            throws NotiProvisionException {
        assertWrite(authentication);
        verifyProvisionOtpIfNeeded(authentication, body);
        List<Long> ids = parseLogIds(body);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 이력을 선택하세요.");
        }
        boolean force = !"N".equalsIgnoreCase(str(body, "forceYn"));
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang"));
        int deleted = 0;
        List<Long> deletedIds = new java.util.ArrayList<>();
        for (Long logId : ids) {
            deleteOneLog(authentication, logId, force, acceptLang);
            deleted++;
            deletedIds.add(logId);
        }
        markProvisionOtpPassed(resolveUsername(authentication));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "선택한 노티 생성 이력이 삭제되었습니다. NOTI 미들웨어에서도 제거되었습니다.");
        out.put("deletedCount", deleted);
        out.put("ids", deletedIds);
        return out;
    }

    private void deleteOneLog(Authentication authentication, Long logId, boolean force, String acceptLang)
            throws NotiProvisionException {
        NotiProvisionLog log = requireLog(logId);
        assertLogInViewerScope(authentication, log);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        String merchantId = resolveLogMerchantId(log);
        try {
            notiProvisionClient.deleteMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    force,
                    resolveLogPgKind(log),
                    acceptLang);
        } catch (NotiProvisionException e) {
            if (!isMerchantNotFound(e)) {
                throw e;
            }
        }
        if (!NotiProvisionClient.isElementPay(resolveLogPgKind(log))) {
            clearJpayUrls(log.getOrgUnitId());
        }
        notiProvisionLogRepository.delete(log);
    }

    /**
     * NOTI 가맹점 목록에 ICOPAY 업체코드+업체명을 함께 표시하기 위한 메타.
     * {@code icopayMeta.compName} → 미들웨어 {@code name}/{@code label}.
     */
    private static Map<String, Object> buildIcopayProvisionMeta(OrgUnit merchant, String integrationMode,
                                                                String provisionedBy) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (merchant != null) {
            if (merchant.getCode() != null && !merchant.getCode().isBlank()) {
                meta.put("compId", merchant.getCode().trim());
            }
            if (merchant.getName() != null && !merchant.getName().isBlank()) {
                meta.put("compName", merchant.getName().trim());
            }
            if (merchant.getId() != null) {
                meta.put("orgUnitId", merchant.getId());
            }
        }
        if (integrationMode != null && !integrationMode.isBlank()) {
            meta.put("integrationMode", integrationMode.trim());
        }
        if (provisionedBy != null && !provisionedBy.isBlank()) {
            meta.put("provisionedBy", provisionedBy.trim());
        }
        return meta;
    }

    private Map<String, Object> buildProvisionFallbackReq(OrgUnit merchant, String merchantId, NotiProvisionLog log,
                                                          String internalTargetId, String callbackUrl, String resultUrl,
                                                          Map<String, Object> options, String dealmai,
                                                          boolean enableDealmaiWebhook) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("merchantId", merchantId);
        String pgKind = resolveLogPgKind(log);
        req.put("pgKind", pgKind);
        if (!internalTargetId.isEmpty()) {
            req.put("internalTargetId", internalTargetId);
        }
        if (!NotiProvisionClient.isElementPay(pgKind) && log.getSlotNo() != null) {
            req.put("jpaySlotNo", log.getSlotNo());
            String route = nz(log.getRouteNo());
            if (route.isEmpty()) {
                route = "j" + log.getSlotNo();
            }
            req.put("routeNo", route);
        }
        if (!callbackUrl.isEmpty()) {
            req.put("callbackUrl", callbackUrl);
        }
        if (!resultUrl.isEmpty()) {
            req.put("resultUrl", resultUrl);
        }
        req.put("options", options);
        if (!dealmai.isEmpty()) {
            req.put("dealmaiPartnerCode", dealmai);
            if (enableDealmaiWebhook) {
                req.put("enableDealmaiWebhook", true);
            }
        }
        return req;
    }

    private void verifyProvisionOtpIfNeeded(Authentication authentication, Map<String, Object> body) {
        AppUser user = resolveUser(authentication);
        if (user == null || user.getUsername() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = user.getUsername().trim();
        if (requiresProvisionOtp(username)) {
            String totp = str(body, "totpCode");
            if (totp.isEmpty() && body != null && body.get("otp") != null) {
                totp = String.valueOf(body.get("otp")).trim();
            }
            authService.verifyTotpOrThrow(user, totp);
        }
    }

    private static Long parseLogId(Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null) {
            throw new IllegalArgumentException("이력 ID가 필요합니다.");
        }
        try {
            return Long.parseLong(String.valueOf(idObj).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("이력 ID가 올바르지 않습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Long> parseLogIds(Map<String, Object> body) {
        java.util.LinkedHashSet<Long> out = new java.util.LinkedHashSet<>();
        if (body == null) {
            return List.of();
        }
        Object idsObj = body.get("ids");
        if (idsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                try {
                    out.add(Long.parseLong(String.valueOf(item).trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (idsObj != null) {
            String raw = String.valueOf(idsObj).trim();
            if (!raw.isEmpty()) {
                for (String part : raw.split("[,\\s]+")) {
                    if (part.isBlank()) {
                        continue;
                    }
                    try {
                        out.add(Long.parseLong(part.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        Object idObj = body.get("id");
        if (idObj != null) {
            try {
                out.add(Long.parseLong(String.valueOf(idObj).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return new java.util.ArrayList<>(out);
    }

    private NotiProvisionLog requireLog(Long logId) {
        if (logId == null) {
            throw new IllegalArgumentException("이력 ID가 필요합니다.");
        }
        return notiProvisionLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("노티 생성 이력을 찾을 수 없습니다."));
    }

    private void assertLogInViewerScope(Authentication authentication, NotiProvisionLog log) {
        if (log == null || log.getCompId() == null) {
            return;
        }
        resolveMerchant(authentication, log.getCompId());
    }

    private static String resolveLogMerchantId(NotiProvisionLog log) {
        if (log == null) {
            return "";
        }
        String mid = log.getMerchantId() != null ? log.getMerchantId().trim() : "";
        if (!mid.isEmpty()) {
            return mid;
        }
        return log.getCompId() != null ? log.getCompId().trim() : "";
    }

    private void applyLogUpdate(NotiProvisionLog log, String internalTargetId, String dealmai,
                                Map<String, Object> data, String jpayNotify, String jpayCallback,
                                String integrationMode) {
        log.setInternalTargetId(internalTargetId);
        log.setDealmaiPartnerCode(dealmai != null ? dealmai : "");
        if (NotiProvisionClient.isElementPay(resolveLogPgKind(log))) {
            log.setRouteNo("elementpay");
            log.setSlotNo(null);
            log.setJpayNotifyUrl("");
            log.setJpayCallbackUrl("");
        } else {
            String route = extractRouteNo(data);
            if (!route.isEmpty()) {
                log.setRouteNo(route);
            }
            Integer slot = extractSlotNo(data);
            if (slot != null) {
                log.setSlotNo(slot);
            }
            log.setJpayNotifyUrl(jpayNotify);
            log.setJpayCallbackUrl(jpayCallback);
        }
        log.setIntegrationMode(normalizeIntegrationMode(integrationMode));
        notiProvisionLogRepository.save(log);
    }

    private void clearJpayUrls(long orgUnitId) {
        merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY)
                .ifPresent(merchantNotifyUrlRepository::delete);
        merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK)
                .ifPresent(merchantNotifyUrlRepository::delete);
    }

    private String resolveUsername(Authentication authentication) {
        AppUser user = resolveUser(authentication);
        return user != null && user.getUsername() != null ? user.getUsername().trim() : "";
    }

    private void saveProvisionLog(OrgUnit merchant,
                                  String merchantId,
                                  String pgKind,
                                  String baseCurrency,
                                  String internalTargetId,
                                  String dealmaiPartnerCode,
                                  Map<String, Object> data,
                                  String jpayNotify,
                                  String jpayCallback,
                                  AppUser user,
                                  String integrationMode) {
        NotiProvisionLog log = new NotiProvisionLog();
        log.setOrgUnitId(merchant.getId());
        log.setCompId(merchant.getCode());
        log.setMerchantId(merchantId != null && !merchantId.isBlank() ? merchantId.trim() : merchant.getCode());
        log.setPgKind(NotiProvisionClient.normalizePgKind(pgKind));
        log.setCompNm(merchant.getName());
        log.setBaseCurrency(normalizeBaseCurrency(baseCurrency));
        log.setInternalTargetId(internalTargetId);
        log.setDealmaiPartnerCode(dealmaiPartnerCode != null ? dealmaiPartnerCode : "");
        if (NotiProvisionClient.isElementPay(pgKind)) {
            log.setRouteNo("elementpay");
            log.setSlotNo(null);
        } else {
            log.setRouteNo(extractRouteNo(data));
            log.setSlotNo(extractSlotNo(data));
        }
        log.setJpayNotifyUrl(jpayNotify);
        log.setJpayCallbackUrl(jpayCallback);
        log.setIntegrationMode(normalizeIntegrationMode(integrationMode));
        log.setCreatedFlag(Boolean.TRUE.equals(data.get("created")) ? "Y" : "N");
        if (user != null && user.getUsername() != null) {
            log.setProvisionedBy(user.getUsername().trim());
        }
        notiProvisionLogRepository.save(log);
    }

    private Map<String, Object> toLogRow(NotiProvisionLog log) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", log.getId());
        m.put("orgUnitId", log.getOrgUnitId());
        m.put("compId", log.getCompId());
        m.put("merchantId", resolveLogMerchantId(log));
        m.put("compNm", log.getCompNm() != null ? log.getCompNm() : "");
        String pgKind = resolveLogPgKind(log);
        m.put("pgKind", pgKind);
        m.put("pgKindLabel", NotiProvisionClient.isElementPay(pgKind) ? "ElementPay" : "JPAY");
        m.put("internalTargetId", nz(log.getInternalTargetId()));
        m.put("integrationMode", normalizeIntegrationMode(log.getIntegrationMode()));
        m.put("routeNo", nz(log.getRouteNo()));
        m.put("slotNo", log.getSlotNo());
        m.put("jpayNotifyUrl", nz(log.getJpayNotifyUrl()));
        m.put("jpayCallbackUrl", nz(log.getJpayCallbackUrl()));
        m.put("dealmaiPartnerCode", nz(log.getDealmaiPartnerCode()));
        m.put("createdFlag", log.getCreatedFlag());
        m.put("createdLabel", "Y".equalsIgnoreCase(log.getCreatedFlag()) ? "신규 생성" : "기존 동일");
        m.put("provisionedBy", nz(log.getProvisionedBy()));
        if (log.getProvisionedAt() != null) {
            m.put("provisionedAt", log.getProvisionedAt().format(PROVISION_LOG_DT));
            m.put("provisionedDate", log.getProvisionedAt().format(PROVISION_LOG_DATE));
            m.put("provisionedTime", log.getProvisionedAt().format(PROVISION_LOG_TIME));
        } else {
            m.put("provisionedAt", "");
            m.put("provisionedDate", "");
            m.put("provisionedTime", "");
        }
        return m;
    }

    private static String resolveLogPgKind(NotiProvisionLog log) {
        if (log == null) {
            return "jpay";
        }
        String stored = log.getPgKind();
        if (stored != null && !stored.isBlank()) {
            return NotiProvisionClient.normalizePgKind(stored);
        }
        String route = nz(log.getRouteNo()).toLowerCase(Locale.ROOT);
        if ("elementpay".equals(route) || "ep".equals(route)) {
            return "elementpay";
        }
        if (log.getSlotNo() == null
                && (log.getJpayNotifyUrl() == null || log.getJpayNotifyUrl().isBlank())
                && (log.getJpayCallbackUrl() == null || log.getJpayCallbackUrl().isBlank())
                && route.isEmpty()) {
            /* 구 이력은 JPAY 기본. EP는 routeNo=elementpay 로 저장 */
            return "jpay";
        }
        return "jpay";
    }

    private static String extractRouteNo(Map<String, Object> data) {
        if (data == null) {
            return "";
        }
        String route = firstNonBlank(data, "routeNo");
        if (!route.isEmpty()) {
            return route;
        }
        Object slot = data.get("slot");
        if (slot != null && !(slot instanceof Map)) {
            return String.valueOf(slot).trim();
        }
        return "";
    }

    private static Integer extractSlotNo(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object n = data.get("slotNo");
        if (n == null) {
            n = data.get("slotNumber");
        }
        if (n == null && data.get("slot") instanceof Number slotNum) {
            n = slotNum;
        }
        if (n == null && data.get("slot") != null && !(data.get("slot") instanceof Map)) {
            n = data.get("slot");
        }
        if (n instanceof Number num) {
            return num.intValue();
        }
        if (n != null) {
            Integer parsed = parseSlotToken(String.valueOf(n));
            if (parsed != null) {
                return parsed;
            }
        }
        Integer fromRoute = parseSlotToken(firstNonBlank(data, "routeNo", "jpayRouteCallbackKey", "jpayRouteResultKey"));
        if (fromRoute != null) {
            return fromRoute;
        }
        return null;
    }

    /**
     * NOTI 응답의 ICOPAY JPAY ingress URL. 필드 누락 시 슬롯·베이스 URL로 조립해
     * 업체 {@code JPAY_NOTIFY}/{@code JPAY_CALLBACK} 에 반영한다.
     */
    private String[] resolveProvisionedJpayUrls(Map<String, Object> data, HqNotifyEnvConfig cfg, Integer requestedSlot) {
        String notify = firstNonBlank(data,
                "icopayJpayNotifyUrl", "pgCallbackUrl", "jpayNotifyUrl");
        String callback = firstNonBlank(data,
                "icopayJpayCallbackUrl", "pgResultUrl", "jpayCallbackUrl");
        // 가맹점 릴레이 callbackUrl/resultUrl 은 ingress 가 아님 — 위 키만 사용
        Integer slot = extractSlotNo(data);
        if (slot == null) {
            slot = requestedSlot;
        }
        String base = NotiProvisionClient.defaultBaseUrlIfBlank(
                cfg != null ? cfg.getNotiProvisionBaseUrl() : null);
        if (notify.isEmpty() && slot != null && slot > 0) {
            notify = base + "/noti/callback/j" + slot;
        }
        if (callback.isEmpty() && slot != null && slot > 0) {
            callback = base + "/noti/result/j" + slot;
        }
        if (notify.isEmpty()) {
            notify = buildIngressUrlFromRouteKey(base, firstNonBlank(data, "jpayRouteCallbackKey"), "callback");
        }
        if (callback.isEmpty()) {
            callback = buildIngressUrlFromRouteKey(base, firstNonBlank(data, "jpayRouteResultKey"), "result");
        }
        return new String[]{notify, callback};
    }

    private static String buildIngressUrlFromRouteKey(String base, String routeKey, String kind) {
        Integer slot = parseSlotToken(routeKey);
        if (slot == null || slot <= 0) {
            return "";
        }
        String k = kind != null && !kind.isBlank() ? kind.trim() : "callback";
        return base + "/noti/" + k + "/j" + slot;
    }

    /** {@code j20}, {@code jpay/callback/j20}, {@code 20} → 20 */
    private static Integer parseSlotToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:^|/)j(\\d+)\\s*$").matcher(t);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String findNotifyUrl(long orgUnitId, String urlType) {
        return merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType)
                .map(MerchantNotifyUrl::getNotiUrl)
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .orElse("");
    }

    private OrgUnit resolveMerchant(Authentication authentication, String compId) {
        String code = compId != null ? compId.trim() : "";
        if (code.isEmpty()) {
            throw new IllegalArgumentException("업체코드를 입력하세요.");
        }
        OrgUnit ou = orgUnitRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다: " + code));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점(MERCHANT) 업체코드만 NOTI Provision 대상입니다.");
        }
        assertMerchantInViewerScope(authentication, ou);
        return ou;
    }

    private void assertMerchantInViewerScope(Authentication authentication, OrgUnit merchant) {
        if (authentication == null || merchant == null || merchant.getCode() == null) {
            return;
        }
        Set<String> visible = orgAccessService.visibleMerchantCompCodes(authentication);
        if (visible == null) {
            return;
        }
        String code = merchant.getCode().trim();
        if (!visible.contains(code)) {
            throw new IllegalArgumentException("소속 조직(총판) 하위 가맹점만 조회·생성할 수 있습니다: " + code);
        }
    }

    private static int autoSlotStartForCurrency(String baseCurrency) {
        String cur = normalizeBaseCurrency(baseCurrency);
        return cur.contains("USD") ? USD_AUTO_SLOT_START : JPY_AUTO_SLOT_START;
    }

    private int resolveNextAutoSlotNo(String baseCurrency) {
        String cur = normalizeBaseCurrency(baseCurrency);
        int start = autoSlotStartForCurrency(cur);
        Integer max = notiProvisionLogRepository.findMaxSlotForCurrency(cur, start);
        int next = (max == null || max < start) ? start : max + 1;
        if (next > 999) {
            throw new IllegalStateException("자동 슬롯 번호가 상한(999)을 초과했습니다. 수동 슬롯을 지정하세요.");
        }
        return next;
    }

    private static String normalizeBaseCurrency(String baseCurrency) {
        String cur = baseCurrency != null ? baseCurrency.trim().toUpperCase(Locale.ROOT) : "";
        if (cur.contains("USD")) {
            return "USD";
        }
        return "JPY";
    }

    private void assertAccess(Authentication authentication) {
        accessDeniedReason(authentication).ifPresent(msg -> {
            throw new IllegalStateException(msg);
        });
    }

    private void assertWrite(Authentication authentication) {
        assertAccess(authentication);
        if (!canWrite(authentication)) {
            throw new IllegalStateException("노티생성 실행 권한이 없습니다(본사권한설정).");
        }
    }

    private static AppUser resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        return user;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key)).trim();
    }

    private static String resolveIntegrationMode(Map<String, Object> body) {
        String mode = str(body, "integrationMode");
        if (mode.isEmpty()) {
            mode = str(body, "payIntegrationMode");
        }
        return normalizeIntegrationMode(mode);
    }

    private static boolean isPureUrlIntegrationMode(String mode) {
        return "URL".equals(normalizeIntegrationMode(mode));
    }

    private static boolean isUrlHybridIntegrationMode(String mode) {
        return "URL_HYBRID".equals(normalizeIntegrationMode(mode));
    }

    private static String firstNonBlank(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (m.get(k) != null) {
                String v = String.valueOf(m.get(k)).trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return "";
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v) ? "Y" : "N";
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }

    private String resolveMerchantBaseCurrency(long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(MerchantProfile::getBaseCurrency)
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .orElse("JPY");
    }

    private String resolveInternalTargetForCurrency(HqNotifyEnvConfig cfg, String baseCurrency, Map<String, Object> body) {
        String manual = str(body, "internalTargetId");
        if (!manual.isEmpty()) {
            return manual;
        }
        String cur = baseCurrency != null ? baseCurrency.trim().toUpperCase(Locale.ROOT) : "";
        if (cur.contains("USD")) {
            String usd = nz(cfg.getNotiProvisionInternalTargetUsd());
            if (!usd.isEmpty()) {
                return usd;
            }
        } else if (cur.contains("THB")) {
            String thb = nz(cfg.getNotiProvisionInternalTargetThb());
            if (!thb.isEmpty()) {
                return thb;
            }
        } else if (cur.contains("JPY") || cur.isEmpty()) {
            String jpy = nz(cfg.getNotiProvisionInternalTargetJpy());
            if (!jpy.isEmpty()) {
                return jpy;
            }
        }
        return nz(cfg.getNotiProvisionDefaultInternalTargetId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listNotiInternalTargets(Authentication authentication, String adminLang) {
        assertAccess(authentication);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        return notiInternalTargetCatalogService.listFromNoti(cfg,
                NotiProvisionClient.acceptLanguageFromAdminLang(adminLang));
    }

    private MasterDistNotifyCtx resolveMasterDistNotifyContext(long merchantOrgUnitId) {
        Optional<Long> mdId = findNearestMasterDistAncestorId(merchantOrgUnitId);
        if (mdId.isEmpty()) {
            return new MasterDistNotifyCtx(null, "", "", null, null);
        }
        OrgUnit md = orgUnitRepository.findById(mdId.get()).orElse(null);
        String[] pair = hqNotifyTargetService.resolveMandatoryNotifyPairUrls(mdId.get());
        return new MasterDistNotifyCtx(
                mdId.get(),
                md != null && md.getCode() != null ? md.getCode() : "",
                md != null && md.getName() != null ? md.getName() : "",
                pair.length > 0 ? pair[0] : null,
                pair.length > 1 ? pair[1] : null);
    }

    private Optional<Long> findNearestMasterDistAncestorId(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) {
                break;
            }
            if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return Optional.of(ou.getId());
            }
            cur = ou.getParentId();
        }
        return Optional.empty();
    }

    private String resolveDealmaiPartner(HqNotifyEnvConfig cfg, String selected) {
        String code;
        if (selected == null || selected.isBlank() || "__DEFAULT__".equals(selected)) {
            code = nz(cfg.getNotiProvisionDefaultDealmaiPartner());
        } else {
            code = selected.trim();
        }
        if (code.isEmpty()) {
            return "";
        }
        if (!hqNotiWebhookPartnerService.isActivePartnerCode(code)) {
            return "";
        }
        return code;
    }

    private boolean requiresProvisionOtp(String username) {
        if (username == null || username.isBlank()) {
            return true;
        }
        Long at = provisionOtpPassedAtMs.get(username.trim());
        if (at == null) {
            return true;
        }
        return System.currentTimeMillis() - at > PROVISION_OTP_GRACE_MS;
    }

    private void markProvisionOtpPassed(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        provisionOtpPassedAtMs.put(username.trim(), System.currentTimeMillis());
    }

    private static String mapRelayFormat(String v) {
        if (v == null || v.isBlank()) {
            return "raw";
        }
        return switch (v.trim().toUpperCase(Locale.ROOT)) {
            case "JSON" -> "json";
            case "FORM" -> "form";
            default -> "raw";
        };
    }

    private static String mapRelayMode(String v) {
        if (v == null || v.isBlank()) {
            return "relay";
        }
        return switch (v.trim().toUpperCase(Locale.ROOT)) {
            case "ENHANCED", "ENHANCED_RELAY", "보강릴레이" -> "enhanced";
            default -> "relay";
        };
    }

    private static String mapResultDeliveryMode(String v) {
        if (v == null || v.isBlank()) {
            return "auto";
        }
        return switch (v.trim().toUpperCase(Locale.ROOT)) {
            case "AUTOT" -> "autot";
            case "POST" -> "no_browser_redirect";
            case "POST_302", "POST302" -> "post_force_redirect";
            default -> "auto";
        };
    }

    private static Integer parseOptionalInt(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PG 노티 슬롯 번호가 올바르지 않습니다.");
        }
    }

    private static boolean isMerchantNotFound(NotiProvisionException e) {
        return "MERCHANT_NOT_FOUND".equalsIgnoreCase(e.getErrorCode())
                || ("NOTI_HTTP".equalsIgnoreCase(e.getErrorCode()) && e.getHttpStatus() == 404);
    }

    /** NOTI 멱등: 동일 가맹 ID가 다른 설정으로 이미 존재(API↔URL 전환 등). */
    private static boolean isMerchantAlreadyExists(NotiProvisionException e) {
        if (e == null) {
            return false;
        }
        if ("MERCHANT_ALREADY_EXISTS".equalsIgnoreCase(e.getErrorCode())) {
            return true;
        }
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("다른 설정으로 이미 존재") || msg.contains("동일 가맹 ID가 다른 설정")) {
            return true;
        }
        return e.getHttpStatus() == 409 && (msg.contains("이미 존재") || msg.toUpperCase(Locale.ROOT).contains("ALREADY"));
    }

    /** PUT 수정 API 미구현·미지원. */
    private static boolean isUpdateEndpointUnavailable(NotiProvisionException e) {
        if (e == null) {
            return false;
        }
        int status = e.getHttpStatus();
        if (status == 404 || status == 405) {
            return true;
        }
        return "MERCHANT_NOT_FOUND".equalsIgnoreCase(e.getErrorCode());
    }

    /** 연동방식 정규화 (미저장 이력은 API). */
    private static String normalizeIntegrationMode(String mode) {
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "";
        if ("URL".equals(m)) {
            return "URL";
        }
        if ("URL_HYBRID".equals(m) || "URL-HYBRID".equals(m) || "HYBRID".equals(m) || "URLHYBRID".equals(m)) {
            return "URL_HYBRID";
        }
        return "API";
    }

    /**
     * ICOPAY 이력 수정 → NOTI 동기화.
     * NOTI 관리자 UI 수정과 같이 PUT을 우선하고, API↔URL 등 설정 충돌·미지원 시 동일 슬롯 교체.
     */
    private Map<String, Object> upsertMerchantKeepingSlot(HqNotifyEnvConfig cfg,
                                                          String merchantId,
                                                          Map<String, Object> provisionReq,
                                                          String acceptLang) throws NotiProvisionException {
        Map<String, Object> updateBody = toUpdateBody(provisionReq);
        try {
            return notiProvisionClient.updateMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    updateBody,
                    acceptLang);
        } catch (NotiProvisionException ue) {
            if (!(isUpdateEndpointUnavailable(ue)
                    || isMerchantNotFound(ue)
                    || isMerchantAlreadyExists(ue))) {
                throw ue;
            }
        }
        try {
            return replaceMerchantKeepingSlot(cfg, merchantId, provisionReq, acceptLang);
        } catch (NotiProvisionException re) {
            if (!isMerchantAlreadyExists(re)) {
                throw re;
            }
            // 삭제 거부·잔존 가맹이 있으면 PUT으로 덮어쓰기 (NOTI UI 수정과 동일 경로)
            try {
                return notiProvisionClient.updateMerchant(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        merchantId,
                        updateBody,
                        acceptLang);
            } catch (NotiProvisionException pe) {
                throw new NotiProvisionException(
                        "NOTI 가맹 설정을 갱신하지 못했습니다. ICOPAY에서 생성한 노티는 ICOPAY 이력 수정으로 바꿔야 양방향이 유지됩니다. "
                                + "잠시 후 다시 저장하거나 NOTI 미들웨어에서 해당 가맹 상태를 확인하세요.",
                        "MERCHANT_ALREADY_EXISTS",
                        pe.getHttpStatus() > 0 ? pe.getHttpStatus() : re.getHttpStatus());
            }
        }
    }

    /** PUT 본문: 슬롯·Route는 불변 — provision 요청에서 생성 전용 힌트만 유지. */
    private static Map<String, Object> toUpdateBody(Map<String, Object> provisionReq) {
        Map<String, Object> body = provisionReq != null
                ? new LinkedHashMap<>(provisionReq)
                : new LinkedHashMap<>();
        Object pk = body.get("pgKind");
        body.put("pgKind", NotiProvisionClient.normalizePgKind(pk != null ? String.valueOf(pk) : "jpay"));
        return body;
    }

    /**
     * NOTI 가맹 force 삭제 후 동일 슬롯으로 재 provision.
     * (API↔URL 전환 등 · 양방향 동기화)
     */
    private Map<String, Object> replaceMerchantKeepingSlot(HqNotifyEnvConfig cfg,
                                                           String merchantId,
                                                           Map<String, Object> provisionReq,
                                                           String acceptLang) throws NotiProvisionException {
        String pgKind = provisionReq != null && provisionReq.get("pgKind") != null
                ? NotiProvisionClient.normalizePgKind(String.valueOf(provisionReq.get("pgKind")))
                : "jpay";
        for (int attempt = 1; attempt <= 3; attempt++) {
            forceDeleteMerchantQuietly(cfg, merchantId, pgKind, acceptLang);
            sleepQuietly(200L * attempt);
            if (!merchantExistsOnNoti(cfg, merchantId, pgKind, acceptLang)) {
                break;
            }
        }
        try {
            return notiProvisionClient.provision(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    provisionReq,
                    acceptLang);
        } catch (NotiProvisionException e) {
            if (!isMerchantAlreadyExists(e)) {
                throw e;
            }
            forceDeleteMerchantQuietly(cfg, merchantId, pgKind, acceptLang);
            sleepQuietly(500L);
            try {
                return notiProvisionClient.provision(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        provisionReq,
                        acceptLang);
            } catch (NotiProvisionException pe) {
                if (isMerchantAlreadyExists(pe)) {
                    throw pe;
                }
                throw pe;
            }
        }
    }

    private boolean merchantExistsOnNoti(HqNotifyEnvConfig cfg, String merchantId, String acceptLang) {
        String pgKind = "jpay";
        return merchantExistsOnNoti(cfg, merchantId, pgKind, acceptLang);
    }

    private boolean merchantExistsOnNoti(HqNotifyEnvConfig cfg, String merchantId, String pgKind, String acceptLang) {
        try {
            notiProvisionClient.getMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    NotiProvisionClient.normalizePgKind(pgKind),
                    acceptLang);
            return true;
        } catch (NotiProvisionException e) {
            return !isMerchantNotFound(e);
        }
    }

    /**
     * JPAY 우선 조회 후 없으면 ElementPay 조회.
     */
    private Map<String, Object> getMerchantPreferring(HqNotifyEnvConfig cfg, String merchantId,
                                                      String preferPgKind, String acceptLang)
            throws NotiProvisionException {
        String prefer = NotiProvisionClient.normalizePgKind(preferPgKind);
        String other = NotiProvisionClient.isElementPay(prefer) ? "jpay" : "elementpay";
        try {
            return notiProvisionClient.getMerchant(
                    cfg.getNotiProvisionBaseUrl(), cfg.getNotiProvisionApiKey(), merchantId, prefer, acceptLang);
        } catch (NotiProvisionException e) {
            if (!isMerchantNotFound(e)) {
                throw e;
            }
            return notiProvisionClient.getMerchant(
                    cfg.getNotiProvisionBaseUrl(), cfg.getNotiProvisionApiKey(), merchantId, other, acceptLang);
        }
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void forceDeleteMerchantQuietly(HqNotifyEnvConfig cfg, String merchantId, String pgKind, String acceptLang)
            throws NotiProvisionException {
        try {
            notiProvisionClient.deleteMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    true,
                    NotiProvisionClient.normalizePgKind(pgKind),
                    acceptLang);
        } catch (NotiProvisionException de) {
            if (isMerchantNotFound(de)) {
                return;
            }
            if (de.getHttpStatus() == 401 || de.getHttpStatus() == 403
                    || "UNAUTHORIZED".equalsIgnoreCase(de.getErrorCode())
                    || "FORBIDDEN".equalsIgnoreCase(de.getErrorCode())) {
                throw de;
            }
            // 409 등: URL이 ICOPAY/PG에 묶여 삭제가 거절될 수 있음 → 상위에서 PUT 폴백
        }
    }

    private record MasterDistNotifyCtx(Long masterDistOrgId, String masterDistCode, String masterDistName,
                                       String callbackUrl, String resultUrl) {}
}
