package com.pg.service;

import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.noti.NotiProvisionClient;
import com.pg.noti.NotiProvisionException;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ICOPAY 웹훅서명 설정 → NOTI 가맹 릴레이 동기.
 * Provision 미설정·NOTI 미등록 가맹은 실패해도 ICOPAY 저장은 유지(경고 로그만).
 */
@Service
public class NotiWebhookSignSyncService {

    private static final Logger log = LoggerFactory.getLogger(NotiWebhookSignSyncService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final NotiProvisionClient notiProvisionClient;
    private final MerchantOutboundWebhookSignService merchantOutboundWebhookSignService;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;

    public NotiWebhookSignSyncService(HqNotifyEnvService hqNotifyEnvService,
                                      NotiProvisionClient notiProvisionClient,
                                      MerchantOutboundWebhookSignService merchantOutboundWebhookSignService,
                                      MerchantProfileRepository merchantProfileRepository,
                                      OrgUnitRepository orgUnitRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.notiProvisionClient = notiProvisionClient;
        this.merchantOutboundWebhookSignService = merchantOutboundWebhookSignService;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> syncOrgUnit(Long orgUnitId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        if (orgUnitId == null) {
            out.put("errorCode", "NO_ORG");
            return out;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty() || ou.get().getCode() == null || ou.get().getCode().isBlank()) {
            out.put("errorCode", "NO_COMP");
            return out;
        }
        return syncMerchantCode(ou.get().getCode().trim(), orgUnitId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> syncMerchantCode(String merchantCode, Long orgUnitId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        String mid = merchantCode != null ? merchantCode.trim() : "";
        if (mid.isEmpty()) {
            out.put("errorCode", "NO_COMP");
            return out;
        }
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        if (!isProvisionConfigured(cfg)) {
            out.put("errorCode", "NOTI_PROVISION_NOT_CONFIGURED");
            out.put("skipped", true);
            log.info("NOTI 웹훅서명 동기 스킵(Provision 미설정) mid={}", mid);
            return out;
        }
        boolean enabled = merchantOutboundWebhookSignService.isSigningEnabled(orgUnitId);
        String yn = enabled ? UrlPayFollowHqYnUtil.Y : UrlPayFollowHqYnUtil.N;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("outboundWebhookSignYn", yn);
        if (enabled && orgUnitId != null) {
            String secret = merchantOutboundWebhookSignService.ensureSecret(orgUnitId);
            if (secret != null && !secret.isBlank()) {
                body.put("outboundWebhookSignSecret", secret);
            } else {
                body.put("clearSecret", true);
            }
        } else {
            body.put("clearSecret", true);
        }
        try {
            Map<String, Object> data = notiProvisionClient.syncWebhookSign(
                    cfg.getNotiProvisionBaseUrl(),
                    cfg.getNotiProvisionApiKey(),
                    mid,
                    body,
                    "ko");
            out.put("success", true);
            out.put("noti", data);
            out.put("outboundWebhookSignYn", yn);
            log.info("NOTI 웹훅서명 동기 완료 mid={} yn={}", mid, yn);
            return out;
        } catch (NotiProvisionException e) {
            out.put("errorCode", e.getErrorCode() != null ? e.getErrorCode() : "NOTI_SYNC_FAIL");
            out.put("message", e.getMessage());
            out.put("httpStatus", e.getHttpStatus());
            log.warn("NOTI 웹훅서명 동기 실패 mid={} code={} http={}: {}",
                    mid, e.getErrorCode(), e.getHttpStatus(), e.getMessage());
            return out;
        } catch (Exception e) {
            out.put("errorCode", "NOTI_SYNC_ERROR");
            out.put("message", e.getMessage());
            log.warn("NOTI 웹훅서명 동기 오류 mid={}: {}", mid, e.getMessage());
            return out;
        }
    }

    /** 본사 기본값 변경 후 FOLLOW_HQ 가맹 재동기(실패 무시). */
    @Transactional(readOnly = true)
    public void syncAllFollowHqMerchantsBestEffort() {
        HqNotifyEnvConfig cfg = hqNotifyEnvService.getOrCreate();
        if (!isProvisionConfigured(cfg)) {
            return;
        }
        for (MerchantProfile mp : merchantProfileRepository.findAll()) {
            if (mp == null || mp.getOrgUnitId() == null) {
                continue;
            }
            String stored = UrlPayFollowHqYnUtil.normalizeStored(mp.getOutboundWebhookSignYn());
            if (!UrlPayFollowHqYnUtil.FOLLOW_HQ.equals(stored)) {
                continue;
            }
            try {
                syncOrgUnit(mp.getOrgUnitId());
            } catch (Exception e) {
                log.warn("FOLLOW_HQ 웹훅서명 동기 스킵 orgUnitId={}: {}", mp.getOrgUnitId(), e.getMessage());
            }
        }
    }

    private static boolean isProvisionConfigured(HqNotifyEnvConfig cfg) {
        return cfg != null
                && "Y".equalsIgnoreCase(String.valueOf(cfg.getNotiProvisionEnabledYn() != null
                        ? cfg.getNotiProvisionEnabledYn() : "").trim())
                && cfg.getNotiProvisionBaseUrl() != null && !cfg.getNotiProvisionBaseUrl().isBlank()
                && cfg.getNotiProvisionApiKey() != null && !cfg.getNotiProvisionApiKey().isBlank();
    }
}
