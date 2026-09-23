package com.pg.merchantdeploy;

import com.pg.api.dto.PageResult;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantIcopayBrokerCredential;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantSandboxNotifyLog;
import com.pg.entity.MerchantSandboxTxn;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantIcopayBrokerCredentialRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.MerchantSandboxNotifyLogRepository;
import com.pg.repository.MerchantSandboxTxnRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 샌드박스 관리자 조회·설정·재송부 (가맹 수정 불가 필드는 호출측에서 권한 검사).
 */
@Service
public class MerchantSandboxAdminService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantSandboxTxnRepository sandboxTxnRepository;
    private final MerchantSandboxNotifyLogRepository notifyLogRepository;
    private final MerchantIcopayBrokerCredentialRepository credentialRepository;
    private final MerchantSandboxNotifyService sandboxNotifyService;
    private final HqApiConfigRepository hqApiConfigRepository;

    public MerchantSandboxAdminService(OrgUnitRepository orgUnitRepository,
                                       MerchantProfileRepository merchantProfileRepository,
                                       MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                       MerchantSandboxTxnRepository sandboxTxnRepository,
                                       MerchantSandboxNotifyLogRepository notifyLogRepository,
                                       MerchantIcopayBrokerCredentialRepository credentialRepository,
                                       MerchantSandboxNotifyService sandboxNotifyService,
                                       HqApiConfigRepository hqApiConfigRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.sandboxTxnRepository = sandboxTxnRepository;
        this.notifyLogRepository = notifyLogRepository;
        this.credentialRepository = credentialRepository;
        this.sandboxNotifyService = sandboxNotifyService;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings(String compId) {
        OrgUnit ou = requireMerchant(compId);
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", ou.getCode());
        out.put("compNm", ou.getName());
        out.put("sandboxUseYn", mp.getSandboxUseYn() != null ? mp.getSandboxUseYn() : "N");
        out.put("notifyUrlBackgroundSandbox", urlOf(ou.getId(), MerchantNotifyUrl.URL_TYPE_BACKGROUND_SANDBOX));
        out.put("notifyUrlResultSandbox", urlOf(ou.getId(), MerchantNotifyUrl.URL_TYPE_RESULT_SANDBOX));
        Optional<MerchantIcopayBrokerCredential> sb = credentialRepository
                .findByOrgUnitIdAndVendorScopeAndEnvModeAndUseYn(
                        ou.getId(), MerchantPgBrokerVendor.ALL, MerchantIcopayBrokerCredential.ENV_SANDBOX, "Y");
        out.put("sandboxSecretIssued", sb.isPresent() ? "Y" : "N");
        out.put("sandboxSecretPrefix", sb.map(MerchantIcopayBrokerCredential::getSecretPrefix).orElse(""));
        out.put("sandboxCardActive", "Y".equalsIgnoreCase(mp.getSandboxUseYn()) && sb.isPresent() ? "Y" : "N");
        return out;
    }

    @Transactional
    public Map<String, Object> saveSettings(String compId, String sandboxUseYn,
                                           String backgroundSandbox, String resultSandbox) {
        OrgUnit ou = requireMerchant(compId);
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        if (sandboxUseYn != null) {
            mp.setSandboxUseYn(sandboxUseYn);
            merchantProfileRepository.save(mp);
        }
        boolean enabled = "Y".equalsIgnoreCase(mp.getSandboxUseYn());
        Optional<MerchantIcopayBrokerCredential> sb = credentialRepository
                .findByOrgUnitIdAndVendorScopeAndEnvModeAndUseYn(
                        ou.getId(), MerchantPgBrokerVendor.ALL, MerchantIcopayBrokerCredential.ENV_SANDBOX, "Y");
        if (!enabled || sb.isEmpty()) {
            /* 비활성·키 미발급: URL은 유지하되 useYn=N */
            upsertSandboxUrl(ou.getId(), MerchantNotifyUrl.URL_TYPE_BACKGROUND_SANDBOX, backgroundSandbox, false);
            upsertSandboxUrl(ou.getId(), MerchantNotifyUrl.URL_TYPE_RESULT_SANDBOX, resultSandbox, false);
        } else {
            upsertSandboxUrl(ou.getId(), MerchantNotifyUrl.URL_TYPE_BACKGROUND_SANDBOX, backgroundSandbox, true);
            upsertSandboxUrl(ou.getId(), MerchantNotifyUrl.URL_TYPE_RESULT_SANDBOX, resultSandbox, true);
        }
        return getSettings(compId);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> searchTxns(String searchCompId, String searchStatus, int page, int size) {
        List<MerchantSandboxTxn> all = sandboxTxnRepository.search(
                blankToNull(searchCompId), blankToNull(searchStatus));
        return pageMaps(all, page, size, this::txnRow);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> searchNotifies(String searchCompId, int page, int size) {
        List<MerchantSandboxNotifyLog> all = notifyLogRepository.search(blankToNull(searchCompId));
        return pageMaps(all, page, size, this::notifyRow);
    }

    @Transactional
    public Map<String, Object> resendNotify(Long logId) {
        if (logId == null) {
            throw new IllegalArgumentException("logId가 필요합니다.");
        }
        return sandboxNotifyService.resend(logId);
    }

    @Transactional(readOnly = true)
    public int getRetainDays() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getSandboxRetainDays)
                .orElse(3);
    }

    @Transactional
    public Map<String, Object> saveRetainDays(Integer days) {
        HqApiConfig cfg = hqApiConfigRepository.findAll().stream().findFirst()
                .orElseGet(HqApiConfig::new);
        cfg.setSandboxRetainDays(days);
        hqApiConfigRepository.save(cfg);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sandboxRetainDays", cfg.getSandboxRetainDays());
        return out;
    }

    private OrgUnit requireMerchant(String compId) {
        String cid = compId != null ? compId.trim() : "";
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("업체코드를 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
            throw new IllegalArgumentException("가맹점만 샌드박스 설정이 가능합니다.");
        }
        return ou;
    }

    private String urlOf(Long orgUnitId, String type) {
        return merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, type)
                .map(MerchantNotifyUrl::getNotiUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse("");
    }

    private void upsertSandboxUrl(Long orgUnitId, String urlType, String raw, boolean active) {
        String url = raw != null ? raw.trim() : "";
        if (url.length() > 2048) {
            throw new IllegalArgumentException("SANDBOX URL은 2048자 이하여야 합니다.");
        }
        Optional<MerchantNotifyUrl> existing =
                merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType);
        if (url.isEmpty()) {
            existing.ifPresent(merchantNotifyUrlRepository::delete);
            return;
        }
        MerchantNotifyUrl n = existing.orElseGet(MerchantNotifyUrl::new);
        n.setOrgUnitId(orgUnitId);
        n.setUrlType(urlType);
        n.setNotiUrl(url);
        n.setUseYn(active ? "Y" : "N");
        merchantNotifyUrlRepository.save(n);
    }

    private Map<String, Object> txnRow(MerchantSandboxTxn t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("compId", t.getCompId());
        m.put("orderNo", t.getOrderNo());
        m.put("amount", t.getAmount());
        m.put("currency", t.getCurrency());
        m.put("productName", t.getProductName());
        m.put("status", t.getStatus());
        m.put("statusNm", statusNm(t.getStatus()));
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        m.put("completedAt", t.getCompletedAt() != null ? t.getCompletedAt().toString() : "");
        return m;
    }

    private Map<String, Object> notifyRow(MerchantSandboxNotifyLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("compId", l.getCompId());
        m.put("orderNo", l.getOrderNo());
        m.put("urlType", l.getUrlType());
        m.put("targetUrl", l.getTargetUrl());
        m.put("result", l.getResultStatus());
        m.put("httpStatus", l.getHttpStatus());
        m.put("retryCnt", l.getRetryCnt());
        m.put("sendDt", l.getSentAt() != null ? l.getSentAt().toString() : "");
        m.put("errorMessage", l.getErrorMessage());
        String payload = l.getPayloadBody();
        m.put("webhookPayload", payload);
        m.put("webhookPayloadPreview", payload != null && payload.length() > 180
                ? payload.substring(0, 180) + "…" : payload);
        return m;
    }

    private static String statusNm(String st) {
        if (st == null) {
            return "";
        }
        return switch (st.toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> "승인";
            case "FAILED" -> "실패";
            case "PENDING" -> "대기";
            default -> st;
        };
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private <T> PageResult<Map<String, Object>> pageMaps(List<T> all, int page, int size,
                                                         java.util.function.Function<T, Map<String, Object>> mapFn) {
        int p = Math.max(1, page);
        int sz = Math.min(200, Math.max(1, size));
        int from = (p - 1) * sz;
        List<Map<String, Object>> slice = new ArrayList<>();
        for (int i = from; i < all.size() && slice.size() < sz; i++) {
            slice.add(mapFn.apply(all.get(i)));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(slice);
        pr.setPage(p);
        pr.setSize(sz);
        pr.setTotalElements(all.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil(all.size() / (double) sz)));
        return pr;
    }
}
