package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 서버통보(웹훅) HMAC 서명 — 본사 Y/N(기본 N) + 가맹 FOLLOW_HQ|Y|N(가맹 우선).
 * 활성 시 {@code X-Icopay-Signature: v1=} + HMAC-SHA256(body, secret).
 */
@Service
public class MerchantOutboundWebhookSignService {

    private static final SecureRandom RNG = new SecureRandom();

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public MerchantOutboundWebhookSignService(HqApiConfigRepository hqApiConfigRepository,
                                              MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    @Transactional(readOnly = true)
    public String hqDefaultYn() {
        return hqApiConfigRepository.findAll().stream()
                .findFirst()
                .map(HqApiConfig::getMerchantOutboundWebhookSignYn)
                .map(v -> UrlPayFollowHqYnUtil.normalizeHqDefault(v, UrlPayFollowHqYnUtil.N))
                .orElse(UrlPayFollowHqYnUtil.N);
    }

    @Transactional(readOnly = true)
    public boolean isSigningEnabled(Long orgUnitId) {
        String hq = hqDefaultYn();
        if (orgUnitId == null) {
            return UrlPayFollowHqYnUtil.Y.equals(hq);
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        String merchant = mp != null ? mp.getOutboundWebhookSignYn() : UrlPayFollowHqYnUtil.FOLLOW_HQ;
        String eff = UrlPayFollowHqYnUtil.resolveEffective(merchant, hq, UrlPayFollowHqYnUtil.N);
        return UrlPayFollowHqYnUtil.Y.equals(eff);
    }

    /**
     * 서명이 활성이면 시크릿을 보장·반환. 비활성이면 empty.
     */
    @Transactional
    public Optional<String> resolveSigningSecret(Long orgUnitId) {
        if (!isSigningEnabled(orgUnitId) || orgUnitId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ensureSecret(orgUnitId));
    }

    @Transactional
    public String ensureSecret(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (mp == null) {
            return "";
        }
        String existing = mp.getOutboundWebhookSignSecret();
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        String created = newSecret();
        mp.setOutboundWebhookSignSecret(created);
        merchantProfileRepository.save(mp);
        return created;
    }

    /** 재발급 — 평문은 이번 응답에만. */
    @Transactional
    public Map<String, Object> rotateSecret(Long orgUnitId) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            out.put("success", false);
            out.put("errorCode", "NOT_FOUND");
            return out;
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (mp == null) {
            out.put("success", false);
            out.put("errorCode", "NOT_FOUND");
            return out;
        }
        String created = newSecret();
        mp.setOutboundWebhookSignSecret(created);
        merchantProfileRepository.save(mp);
        out.put("success", true);
        out.put("outboundWebhookSignSecretPlain", created);
        out.put("outboundWebhookSignSecretMasked", maskSecret(created));
        out.put("outboundWebhookSignHasSecret", true);
        out.put("message", "서명 시크릿이 발급·재발급되었습니다. 평문은 이번 한 번만 표시됩니다. 화면을 벗어나면 다시 볼 수 없습니다.");
        return out;
    }

    @Transactional(readOnly = true)
    public void putStatusFields(Long orgUnitId, Map<String, Object> m) {
        if (m == null) {
            return;
        }
        String hq = hqDefaultYn();
        m.put("hqOutboundWebhookSignYn", hq);
        if (orgUnitId == null) {
            m.put("outboundWebhookSignYn", UrlPayFollowHqYnUtil.FOLLOW_HQ);
            m.put("outboundWebhookSignEffectiveYn", hq);
            m.put("outboundWebhookSignSecretMasked", "");
            m.put("outboundWebhookSignHasSecret", false);
            return;
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        String stored = mp != null ? mp.getOutboundWebhookSignYn() : UrlPayFollowHqYnUtil.FOLLOW_HQ;
        String eff = UrlPayFollowHqYnUtil.resolveEffective(stored, hq, UrlPayFollowHqYnUtil.N);
        String sec = mp != null ? mp.getOutboundWebhookSignSecret() : null;
        boolean has = sec != null && !sec.isBlank();
        m.put("outboundWebhookSignYn", UrlPayFollowHqYnUtil.normalizeStored(stored));
        m.put("outboundWebhookSignEffectiveYn", eff);
        m.put("outboundWebhookSignHasSecret", has);
        m.put("outboundWebhookSignSecretMasked", has ? maskSecret(sec) : "");
    }

    /** 표시용: 앞 5자리 + 마스킹. 전체 값은 발급/재발급 직후 응답에만. */
    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        String s = secret.trim();
        if (s.length() <= 5) {
            return "••••••••";
        }
        return s.substring(0, 5) + "••••••••••••";
    }

    private static String newSecret() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }
}
