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
 * 운영관리 — NOTI JPAY Provision (노티생성).
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
                Map<String, Object> remote = notiProvisionClient.getMerchant(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        merchant.getCode(),
                        "ko");
                out.put("notiRemote", remote);
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
                Map<String, Object> remote = notiProvisionClient.getMerchant(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        merchant.getCode(),
                        "ko");
                out.put("notiRemote", remote);
                out.put("merchantIdExists", true);
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
    public Map<String, Object> checkMerchantId(Authentication authentication, String merchantId, String compId)
            throws NotiProvisionException {
        assertAccess(authentication);
        if (compId != null && !compId.trim().isEmpty()) {
            resolveMerchant(authentication, compId);
        }
        String mid = merchantId != null ? merchantId.trim() : "";
        if (mid.isEmpty()) {
            throw new IllegalArgumentException("가맹점 ID를 입력하세요.");
        }
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merchantId", mid);
        out.put("available", true);
        try {
            Map<String, Object> remote = notiProvisionClient.getMerchant(
                    cfg.getNotiProvisionBaseUrl(), cfg.getNotiProvisionApiKey(), mid, "ko");
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
            throw new IllegalArgumentException("JPAY PG 노티 슬롯 번호는 1~999 범위여야 합니다.");
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

        boolean enableRelay = !"N".equalsIgnoreCase(str(body, "enableRelayYn"));
        boolean enableInternal = "Y".equalsIgnoreCase(str(body, "enableInternalYn"));
        boolean enableDevInternal = !"N".equalsIgnoreCase(str(body, "enableDevInternalYn"));
        boolean slotAuto = "Y".equalsIgnoreCase(str(body, "slotAutoYn"));
        String adminLang = str(body, "adminLang");
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(adminLang);
        List<Map<String, Object>> notiTargets = notiInternalTargetCatalogService.listFromNoti(cfg, acceptLang);

        String internalTargetId = str(body, "internalTargetId");
        if (internalTargetId.isEmpty()) {
            internalTargetId = resolveInternalTargetForCurrency(cfg, baseCurrency, body);
        }
        String rawInternal = internalTargetId;
        internalTargetId = notiInternalTargetCatalogService.resolveCanonicalId(rawInternal, notiTargets);
        if (!rawInternal.isEmpty() && !notiTargets.isEmpty() && internalTargetId.isEmpty()) {
            throw new IllegalArgumentException("등록되지 않은 NOTI 전산 대상 ID입니다: " + rawInternal);
        }
        if (enableInternal && internalTargetId.isEmpty()) {
            throw new IllegalArgumentException("전산 노티 사용 시 NOTI 전산 대상 ID를 선택하세요.");
        }

        String callbackUrl = str(body, "callbackUrl");
        String resultUrl = str(body, "resultUrl");
        MasterDistNotifyCtx mdCtx = resolveMasterDistNotifyContext(merchant.getId());

        if (enableRelay) {
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
                throw new IllegalArgumentException("개발 노티 사용 시 총판의 본사 노티 대상(CALLBACK·RESULT) 연결이 필요합니다. 본사설정 → 노티구성설정에서 총판 노티를 생성·연결하세요.");
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
        options.put("resultDeliveryMode", mapResultDeliveryMode(str(body, "resultDeliveryMode")));
        String dealmai = resolveDealmaiPartner(cfg, str(body, "dealmaiPartnerCode"));
        boolean enableDealmaiWebhook = !"N".equalsIgnoreCase(str(body, "enableDealmaiWebhookYn"));
        if (!dealmai.isEmpty() && enableDealmaiWebhook) {
            options.put("enableDealmaiWebhook", true);
            options.put("dealmaiPartnerCode", dealmai);
        } else if (!dealmai.isEmpty()) {
            options.put("dealmaiPartnerCode", dealmai);
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("merchantId", merchantId);
        req.put("pgKind", "jpay");
        if (!dealmai.isEmpty()) {
            req.put("dealmaiPartnerCode", dealmai);
            if (enableDealmaiWebhook) {
                req.put("enableDealmaiWebhook", true);
            }
        }
        if (!internalTargetId.isEmpty()) {
            req.put("internalTargetId", internalTargetId);
        }
        Integer jpaySlotNo = parseOptionalInt(body.get("jpaySlotNo"));
        if (slotAuto) {
            jpaySlotNo = resolveNextAutoSlotNo(baseCurrency);
        }
        if (jpaySlotNo != null) {
            req.put("jpaySlotNo", jpaySlotNo);
            req.put("routeNo", "j" + jpaySlotNo);
        }
        if (!callbackUrl.isEmpty()) {
            req.put("callbackUrl", callbackUrl);
        }
        if (!resultUrl.isEmpty()) {
            req.put("resultUrl", resultUrl);
        }
        req.put("options", options);
        Map<String, Object> icopayMeta = new LinkedHashMap<>();
        icopayMeta.put("compId", merchant.getCode());
        icopayMeta.put("orgUnitId", merchant.getId());
        if (user != null && user.getUsername() != null) {
            icopayMeta.put("provisionedBy", user.getUsername());
        }
        req.put("icopayMeta", icopayMeta);

        Map<String, Object> data = notiProvisionClient.provision(
                cfg.getNotiProvisionBaseUrl(),
                cfg.getNotiProvisionApiKey(),
                req,
                NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang")));

        String jpayNotify = firstNonBlank(data, "icopayJpayNotifyUrl", "pgCallbackUrl");
        String jpayCallback = firstNonBlank(data, "icopayJpayCallbackUrl", "pgResultUrl");
        if (!jpayNotify.isEmpty() || !jpayCallback.isEmpty()) {
            upsertJpayUrls(merchant.getId(), jpayNotify, jpayCallback);
        }
        saveProvisionLog(merchant, merchantId, baseCurrency, internalTargetId, dealmai, data, jpayNotify, jpayCallback, user);
        markProvisionOtpPassed(username);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", Boolean.TRUE.equals(data.get("created"))
                ? "NOTI JPAY 가맹이 생성되었습니다."
                : "기존 NOTI JPAY 가맹과 동일합니다.");
        out.put("provision", data);
        out.put("jpayNotifyUrl", jpayNotify);
        out.put("jpayCallbackUrl", jpayCallback);
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
                out.put("notiRemote", notiProvisionClient.getMerchant(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        resolveLogMerchantId(log),
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

        boolean enableRelay = !"N".equalsIgnoreCase(str(body, "enableRelayYn"));
        boolean enableInternal = "Y".equalsIgnoreCase(str(body, "enableInternalYn"));
        boolean enableDevInternal = !"N".equalsIgnoreCase(str(body, "enableDevInternalYn"));
        String callbackUrl = str(body, "callbackUrl");
        String resultUrl = str(body, "resultUrl");
        MasterDistNotifyCtx mdCtx = resolveMasterDistNotifyContext(merchant.getId());
        if (enableRelay) {
            if (callbackUrl.isEmpty()) {
                callbackUrl = findNotifyUrl(merchant.getId(), "BACKGROUND");
            }
            if (resultUrl.isEmpty()) {
                resultUrl = findNotifyUrl(merchant.getId(), "RESULT");
            }
        } else if (enableDevInternal) {
            callbackUrl = nz(mdCtx.callbackUrl());
            resultUrl = nz(mdCtx.resultUrl());
        }

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("enableRelay", enableRelay);
        options.put("enableInternal", enableInternal);
        options.put("enableDevInternal", enableDevInternal);
        options.put("relayFormat", mapRelayFormat(str(body, "relayFormat")));
        options.put("relayMode", mapRelayMode(str(body, "relayMode")));
        options.put("resultDeliveryMode", mapResultDeliveryMode(str(body, "resultDeliveryMode")));
        String dealmai = resolveDealmaiPartner(cfg, str(body, "dealmaiPartnerCode"));
        boolean enableDealmaiWebhook = !"N".equalsIgnoreCase(str(body, "enableDealmaiWebhookYn"));
        if (!dealmai.isEmpty() && enableDealmaiWebhook) {
            options.put("enableDealmaiWebhook", true);
            options.put("dealmaiPartnerCode", dealmai);
        } else if (!dealmai.isEmpty()) {
            options.put("dealmaiPartnerCode", dealmai);
        }

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("pgKind", "jpay");
        if (!internalTargetId.isEmpty()) {
            req.put("internalTargetId", internalTargetId);
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

        Map<String, Object> data;
        try {
            data = notiProvisionClient.updateMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    req,
                    acceptLang);
        } catch (NotiProvisionException e) {
            if ("NOTI_HTTP".equalsIgnoreCase(e.getErrorCode()) && e.getHttpStatus() == 404) {
                data = notiProvisionClient.provision(
                        cfg.getNotiProvisionBaseUrl(),
                        cfg.getNotiProvisionApiKey(),
                        buildProvisionFallbackReq(merchant, merchantId, log, internalTargetId, callbackUrl, resultUrl, options, dealmai, enableDealmaiWebhook),
                        acceptLang);
            } else {
                throw e;
            }
        }

        String jpayNotify = firstNonBlank(data, "icopayJpayNotifyUrl", "pgCallbackUrl");
        String jpayCallback = firstNonBlank(data, "icopayJpayCallbackUrl", "pgResultUrl");
        if (jpayNotify.isEmpty() && log.getJpayNotifyUrl() != null) {
            jpayNotify = log.getJpayNotifyUrl().trim();
        }
        if (jpayCallback.isEmpty() && log.getJpayCallbackUrl() != null) {
            jpayCallback = log.getJpayCallbackUrl().trim();
        }
        if (!jpayNotify.isEmpty() || !jpayCallback.isEmpty()) {
            upsertJpayUrls(merchant.getId(), jpayNotify, jpayCallback);
        }
        applyLogUpdate(log, internalTargetId, dealmai, data, jpayNotify, jpayCallback);
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
        NotiProvisionLog log = requireLog(logId);
        assertLogInViewerScope(authentication, log);
        HqNotifyEnvConfig cfg = hqNotifyEnvService.requireProvisionConfigReady();
        String merchantId = resolveLogMerchantId(log);
        boolean force = "Y".equalsIgnoreCase(str(body, "forceYn"));
        String acceptLang = NotiProvisionClient.acceptLanguageFromAdminLang(str(body, "adminLang"));
        try {
            notiProvisionClient.deleteMerchant(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    merchantId,
                    force,
                    acceptLang);
        } catch (NotiProvisionException e) {
            if (!isMerchantNotFound(e)) {
                throw e;
            }
        }
        clearJpayUrls(log.getOrgUnitId());
        notiProvisionLogRepository.delete(log);
        markProvisionOtpPassed(resolveUsername(authentication));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "노티 생성 이력이 삭제되었습니다. NOTI 미들웨어에서도 제거되었습니다.");
        out.put("id", logId);
        return out;
    }

    private Map<String, Object> buildProvisionFallbackReq(OrgUnit merchant, String merchantId, NotiProvisionLog log,
                                                          String internalTargetId, String callbackUrl, String resultUrl,
                                                          Map<String, Object> options, String dealmai,
                                                          boolean enableDealmaiWebhook) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("merchantId", merchantId);
        req.put("pgKind", "jpay");
        if (!internalTargetId.isEmpty()) {
            req.put("internalTargetId", internalTargetId);
        }
        if (log.getSlotNo() != null) {
            req.put("jpaySlotNo", log.getSlotNo());
            req.put("routeNo", nz(log.getRouteNo()));
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
                                Map<String, Object> data, String jpayNotify, String jpayCallback) {
        log.setInternalTargetId(internalTargetId);
        log.setDealmaiPartnerCode(dealmai != null ? dealmai : "");
        log.setRouteNo(extractRouteNo(data));
        Integer slot = extractSlotNo(data);
        if (slot != null) {
            log.setSlotNo(slot);
        }
        log.setJpayNotifyUrl(jpayNotify);
        log.setJpayCallbackUrl(jpayCallback);
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
                                  String baseCurrency,
                                  String internalTargetId,
                                  String dealmaiPartnerCode,
                                  Map<String, Object> data,
                                  String jpayNotify,
                                  String jpayCallback,
                                  AppUser user) {
        NotiProvisionLog log = new NotiProvisionLog();
        log.setOrgUnitId(merchant.getId());
        log.setCompId(merchant.getCode());
        log.setMerchantId(merchantId != null && !merchantId.isBlank() ? merchantId.trim() : merchant.getCode());
        log.setCompNm(merchant.getName());
        log.setBaseCurrency(normalizeBaseCurrency(baseCurrency));
        log.setInternalTargetId(internalTargetId);
        log.setDealmaiPartnerCode(dealmaiPartnerCode != null ? dealmaiPartnerCode : "");
        log.setRouteNo(extractRouteNo(data));
        log.setSlotNo(extractSlotNo(data));
        log.setJpayNotifyUrl(jpayNotify);
        log.setJpayCallbackUrl(jpayCallback);
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
        m.put("internalTargetId", nz(log.getInternalTargetId()));
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
        if (n instanceof Number num) {
            return num.intValue();
        }
        if (n != null) {
            try {
                return Integer.parseInt(String.valueOf(n).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void upsertJpayUrls(long orgUnitId, String notifyUrl, String callbackUrl) {
        if (notifyUrl != null && !notifyUrl.isBlank()) {
            saveOrUpdateUrl(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, notifyUrl.trim());
        }
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            saveOrUpdateUrl(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, callbackUrl.trim());
        }
    }

    private void saveOrUpdateUrl(long orgUnitId, String urlType, String url) {
        MerchantNotifyUrl row = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType)
                .orElseGet(MerchantNotifyUrl::new);
        row.setOrgUnitId(orgUnitId);
        row.setUrlType(urlType);
        row.setNotiUrl(url);
        row.setUseYn("Y");
        merchantNotifyUrlRepository.save(row);
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
        }
        String jpy = nz(cfg.getNotiProvisionInternalTargetJpy());
        if (!jpy.isEmpty()) {
            return jpy;
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
            throw new IllegalArgumentException("JPAY PG 노티 슬롯 번호가 올바르지 않습니다.");
        }
    }

    private static boolean isMerchantNotFound(NotiProvisionException e) {
        return "MERCHANT_NOT_FOUND".equalsIgnoreCase(e.getErrorCode())
                || ("NOTI_HTTP".equalsIgnoreCase(e.getErrorCode()) && e.getHttpStatus() == 404);
    }

    private record MasterDistNotifyCtx(Long masterDistOrgId, String masterDistCode, String masterDistName,
                                       String callbackUrl, String resultUrl) {}
}
