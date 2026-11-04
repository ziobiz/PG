package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.IcopayPublicIdentity;
import com.pg.entity.OrgLevel;
import com.pg.util.PgOutboundUrlPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 공개·PG 방향 ICOPAY 상호·주소·사이트. 가맹 {@link MerchantProfile} 주소는 쓰지 않는다.
 * 주소는 설정값 → 본사(총본사) 프로필 → {@link IcopayPublicIdentity#FALLBACK_ADDRESS}.
 */
@Service
public class IcopayPublicIdentityService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    @Value("${pg.icopay.public-name:ICOPAY}")
    private String configuredName;

    @Value("${pg.icopay.public-site-url:https://icopay.co.kr}")
    private String configuredSiteUrl;

    @Value("${pg.icopay.public-address:}")
    private String configuredAddress;

    public IcopayPublicIdentityService(OrgUnitRepository orgUnitRepository,
                                       MerchantProfileRepository merchantProfileRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public String displayName() {
        return IcopayPublicIdentity.displayName(configuredName);
    }

    public String siteUrl() {
        return IcopayPublicIdentity.siteUrl(configuredSiteUrl);
    }

    /** PG·공개 표면에 넣을 유일한 사업장 주소(ICOPAY). */
    public String addressLine() {
        if (configuredAddress != null && !configuredAddress.isBlank()) {
            return configuredAddress.trim();
        }
        OrgUnit hq = resolveHeadquarters();
        if (hq != null) {
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(hq.getId());
            if (mp.isPresent()) {
                String joined = joinAddr(mp.get());
                if (!joined.isBlank()) {
                    return joined;
                }
            }
        }
        return IcopayPublicIdentity.FALLBACK_ADDRESS;
    }

    /** checkout-context 등 공개 JSON — 가맹 addr 대신 플랫폼 정체성. */
    public void putPlatformBrandInto(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        data.put("platformBrandName", displayName());
        data.put("platformBrandAddress", addressLine());
        data.put("platformBrandUrl", siteUrl());
        /* 실수로 가맹 주소 키가 있으면 제거 */
        data.remove("merchantAddr");
        data.remove("merchantAddress");
        data.remove("addr");
        data.remove("addrDetail");
        data.remove("addrEtc");
        data.remove("branchAddr");
        data.remove("chatbotKbAddr");
    }

    public Map<String, String> asMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", displayName());
        m.put("address", addressLine());
        m.put("siteUrl", siteUrl());
        m.put("apiBaseUrl", PgOutboundUrlPolicy.enforceOwnDomain(
                IcopayPublicIdentity.API_BASE_URL, IcopayPublicIdentity.API_BASE_URL,
                IcopayPublicIdentity.API_BASE_URL, siteUrl()));
        return m;
    }

    private OrgUnit resolveHeadquarters() {
        Optional<OrgUnit> byCode = orgUnitRepository.findByCode("0000000000");
        if (byCode.isPresent() && byCode.get().getOrgLevel() == OrgLevel.HEADQUARTERS) {
            return byCode.get();
        }
        List<OrgUnit> list = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.HEADQUARTERS);
        return list.isEmpty() ? null : list.get(0);
    }

    private static String joinAddr(MerchantProfile mp) {
        StringBuilder sb = new StringBuilder();
        if (mp.getZipCode() != null && !mp.getZipCode().isBlank()) {
            sb.append('(').append(mp.getZipCode().trim()).append(") ");
        }
        appendPart(sb, mp.getAddr());
        appendPart(sb, mp.getAddrDetail());
        appendPart(sb, mp.getAddrEtc());
        return sb.toString().trim();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
            sb.append(' ');
        }
        sb.append(part.trim());
    }
}
