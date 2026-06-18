package com.pg.merchantdeploy;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 API 연동 채널 — 본사 전역(결제로직설정) × 가맹 프로필(업체관리) 교집합.
 */
@Service
public class MerchantApiIntegrationChannelService {

    public static final String CODE_INTEGRATION_CHANNEL_DISABLED = "INTEGRATION_CHANNEL_DISABLED";

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public MerchantApiIntegrationChannelService(HqApiConfigRepository hqApiConfigRepository,
                                                MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public enum Channel {
        API_BROKER_INLINE,
        API_BROKER_REDIRECT,
        API_WORDPRESS
    }

    public HqApiConfig resolveHqConfig() {
        return hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
    }

    public Optional<MerchantProfile> resolveProfile(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId);
    }

    public boolean isInlineEffective(Long orgUnitId) {
        HqApiConfig hq = resolveHqConfig();
        MerchantProfile mp = resolveProfile(orgUnitId).orElse(null);
        return isYes(hq != null ? hq.getApiBrokerInlineEnabledYn() : "Y")
                && isYes(hq != null ? hq.getUrlPayInlineEnabledYn() : "Y")
                && isYes(mp != null ? mp.getApiBrokerInlineUseYn() : "Y");
    }

    public boolean isRedirectEffective(Long orgUnitId) {
        HqApiConfig hq = resolveHqConfig();
        MerchantProfile mp = resolveProfile(orgUnitId).orElse(null);
        return isYes(hq != null ? hq.getApiBrokerRedirectEnabledYn() : "Y")
                && isYes(hq != null ? hq.getUrlPayRedirectEnabledYn() : "Y")
                && isYes(mp != null ? mp.getApiBrokerRedirectUseYn() : "N");
    }

    public boolean isWordpressEffective(Long orgUnitId) {
        HqApiConfig hq = resolveHqConfig();
        MerchantProfile mp = resolveProfile(orgUnitId).orElse(null);
        return isYes(hq != null ? hq.getApiWordpressPluginEnabledYn() : "Y")
                && isYes(mp != null ? mp.getApiWordpressUseYn() : "N");
    }

    /** 업체관리 그리드 — IN(INLINE)·RE(REDIRECT)·WO(WordPress), 복수 시 {@code IN/RE} 형식. */
    public String buildEffectiveChannelDisplayCode(Long orgUnitId) {
        if (orgUnitId == null) {
            return "-";
        }
        List<String> parts = new ArrayList<>(3);
        if (isInlineEffective(orgUnitId)) {
            parts.add("IN");
        }
        if (isRedirectEffective(orgUnitId)) {
            parts.add("RE");
        }
        if (isWordpressEffective(orgUnitId)) {
            parts.add("WO");
        }
        return parts.isEmpty() ? "-" : String.join("/", parts);
    }

    public Optional<String> denyMessage(Long orgUnitId, Channel channel) {
        HqApiConfig hq = resolveHqConfig();
        MerchantProfile mp = resolveProfile(orgUnitId).orElse(null);
        return switch (channel) {
            case API_BROKER_INLINE -> {
                if (hq != null && !isYes(hq.getApiBrokerInlineEnabledYn())) {
                    yield Optional.of("본사 설정에서 API 중계형 INLINE 제공이 꺼져 있습니다.");
                }
                if (hq != null && !isYes(hq.getUrlPayInlineEnabledYn())) {
                    yield Optional.of("본사 설정에서 URL 결제형 INLINE 제공이 꺼져 있습니다.");
                }
                if (mp != null && !isYes(mp.getApiBrokerInlineUseYn())) {
                    yield Optional.of("이 가맹점은 API 인라인 연동이 미사용으로 설정되어 있습니다.");
                }
                yield Optional.empty();
            }
            case API_BROKER_REDIRECT -> {
                if (hq != null && !isYes(hq.getApiBrokerRedirectEnabledYn())) {
                    yield Optional.of("본사 설정에서 API 중계형 REDIRECT 제공이 꺼져 있습니다.");
                }
                if (hq != null && !isYes(hq.getUrlPayRedirectEnabledYn())) {
                    yield Optional.of("본사 설정에서 URL 결제형 REDIRECT 제공이 꺼져 있습니다.");
                }
                if (mp != null && !isYes(mp.getApiBrokerRedirectUseYn())) {
                    yield Optional.of("이 가맹점은 API 리다이렉트 연동이 미사용으로 설정되어 있습니다.");
                }
                yield Optional.empty();
            }
            case API_WORDPRESS -> {
                if (hq != null && !isYes(hq.getApiWordpressPluginEnabledYn())) {
                    yield Optional.of("본사 설정에서 WordPress 플러그인 연동이 꺼져 있습니다.");
                }
                if (mp != null && !isYes(mp.getApiWordpressUseYn())) {
                    yield Optional.of("이 가맹점은 WordPress/WooCommerce API 연동이 미사용으로 설정되어 있습니다.");
                }
                yield Optional.empty();
            }
        };
    }

    public Map<String, Object> buildStatusBlock(Long orgUnitId) {
        HqApiConfig hq = resolveHqConfig();
        MerchantProfile mp = resolveProfile(orgUnitId).orElse(null);
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("apiBrokerInlineUseYn", yn(mp != null ? mp.getApiBrokerInlineUseYn() : "Y"));
        block.put("apiBrokerRedirectUseYn", yn(mp != null ? mp.getApiBrokerRedirectUseYn() : "N"));
        block.put("apiWordpressUseYn", yn(mp != null ? mp.getApiWordpressUseYn() : "N"));
        block.put("hqApiBrokerInlineEnabledYn", yn(hq != null ? hq.getApiBrokerInlineEnabledYn() : "Y"));
        block.put("hqApiBrokerRedirectEnabledYn", yn(hq != null ? hq.getApiBrokerRedirectEnabledYn() : "Y"));
        block.put("hqUrlPayInlineEnabledYn", yn(hq != null ? hq.getUrlPayInlineEnabledYn() : "Y"));
        block.put("hqUrlPayRedirectEnabledYn", yn(hq != null ? hq.getUrlPayRedirectEnabledYn() : "Y"));
        block.put("hqWordpressPluginEnabledYn", yn(hq != null ? hq.getApiWordpressPluginEnabledYn() : "Y"));
        block.put("effectiveInline", isInlineEffective(orgUnitId));
        block.put("effectiveRedirect", isRedirectEffective(orgUnitId));
        block.put("effectiveWordpress", isWordpressEffective(orgUnitId));
        MerchantDeployL10n.putDescription(block, new MerchantDeployL10n.Bundle(
                "가맹 API 연동 채널 — 본사 결제로직설정 × 업체관리 가맹 프로필",
                "Merchant API integration channels — HQ payment orchestration × merchant profile",
                "加盟店 API 連携チャネル — 本社決済ロジック × 加盟店プロフィール",
                "商户 API 对接渠道 — 总部支付逻辑 × 商户资料",
                "ช่องเชื่อมต่อ Merchant API — HQ × โปรไฟล์ร้าน"
        ));
        MerchantDeployL10n.putTextFields(block, "merchantSettingNote", new MerchantDeployL10n.Bundle(
                "가맹별 채널은 업체관리 → 가맹 「가맹 API 연동 채널」에서 설정합니다.",
                "Per-merchant channels: Company management → merchant 「Merchant API integration channels」.",
                "加盟店別: 업체관리 → 「加盟店 API 連携チャネル」",
                "按商户: 업체관리 → 「商户 API 对接渠道」",
                "ตั้งค่าร้าน:  업체관리 → 「ช่อง Merchant API」"
        ));
        MerchantDeployL10n.putTextFields(block, "hqSettingNote", new MerchantDeployL10n.Bundle(
                "본사 전역 상한: 배포설정 → 결제로직설정 (API INLINE/REDIRECT·WordPress 제공)",
                "HQ global caps: Deployment → Payment orchestration (API INLINE/REDIRECT·WordPress).",
                "本社上限: デプロイ設定 → 決済ロジック設定",
                "总部上限: 部署设置 → 支付逻辑设置",
                "HQ: Deployment → Payment orchestration"
        ));
        return block;
    }

    public static void validateMerchantChannelCombination(String inlineYn, String redirectYn, String wordpressYn) {
        if (!isYes(wordpressYn)) {
            return;
        }
        if (!isYes(inlineYn) && !isYes(redirectYn)) {
            throw new IllegalArgumentException(
                    "WordPress/WooCommerce 연동을 사용하려면 API 인라인 또는 API 리다이렉트 중 하나 이상을 켜야 합니다.");
        }
    }

    public static String yn(String v) {
        return isYes(v) ? "Y" : "N";
    }

    public static boolean isYes(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }
}
