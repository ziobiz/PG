package com.pg.splitpay;

import com.pg.entity.AppUser;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.OrgAccessService;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 분할결제 계약 취소 권한 — 총본사 항상 / 본사·총판은 HQ orgOp / 가맹은 FOLLOW_HQ·Y/N 실효값.
 */
@Service
public class SplitPayContractCancelPermissionService {

    private final OrgAccessService orgAccessService;
    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;

    public SplitPayContractCancelPermissionService(OrgAccessService orgAccessService,
                                                   AuthService authService,
                                                   OrgUnitRepository orgUnitRepository,
                                                   MerchantProfileRepository merchantProfileRepository,
                                                   HqApiConfigRepository hqApiConfigRepository) {
        this.orgAccessService = orgAccessService;
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public boolean canCancelContract(String merchantCode, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return false;
        }
        String mc = merchantCode != null ? merchantCode.trim() : "";
        if (mc.isEmpty()) {
            return false;
        }
        Set<String> visible = orgAccessService.visibleMerchantCompCodes(authentication);
        /* ADMIN·총본사(HEADQUARTERS)는 가맹 조회 제한 없음 → 계약취소 항상 허용 */
        if (visible == null) {
            return true;
        }
        boolean ok = false;
        for (String v : visible) {
            if (v != null && v.trim().equalsIgnoreCase(mc)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        OrgLevel actorLevel = resolveActorOrgLevel(user);
        if (actorLevel == OrgLevel.HEADQUARTERS) {
            return true;
        }
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (actorLevel == OrgLevel.REGIONAL || actorLevel == OrgLevel.MASTER_DIST) {
            String orgOp = hq != null ? hq.getSplitPayContractCancelOrgOpYn() : "N";
            return "Y".equalsIgnoreCase(UrlPayFollowHqYnUtil.normalizeHqDefault(orgOp, "N"));
        }
        if (actorLevel == OrgLevel.MERCHANT) {
            return isMerchantCancelEffective(mc, hq);
        }
        return false;
    }

    /**
     * 가맹 실효 취소 권한.
     * 명시 N 이면 불가. Y 이면 허용.
     * FOLLOW_HQ(또는 미설정)이면 본사 기본값을 따르되, 본사 기본이 비어 있으면 허용(Y) — 가맹이 본인 계약을 취소할 수 있게 함.
     */
    public boolean isMerchantCancelEffective(String merchantCode, HqApiConfig hqOpt) {
        Optional<OrgUnit> ou = orgUnitRepository.findByCodeIgnoreCase(merchantCode != null ? merchantCode.trim() : "");
        if (ou.isEmpty()) {
            return false;
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.get().getId()).orElse(null);
        String stored = mp != null ? mp.getSplitPayContractCancelYn() : UrlPayFollowHqYnUtil.FOLLOW_HQ;
        String norm = UrlPayFollowHqYnUtil.normalizeStored(stored);
        if ("N".equalsIgnoreCase(norm)) {
            return false;
        }
        if ("Y".equalsIgnoreCase(norm)) {
            return true;
        }
        HqApiConfig hq = hqOpt != null ? hqOpt : hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        String hqDefault = hq != null ? hq.getSplitPayContractCancelDefaultYn() : "Y";
        return "Y".equalsIgnoreCase(UrlPayFollowHqYnUtil.resolveEffective(UrlPayFollowHqYnUtil.FOLLOW_HQ, hqDefault, "Y"));
    }

    private OrgLevel resolveActorOrgLevel(AppUser user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return OrgLevel.HEADQUARTERS;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String code = null;
        if (org != null && org.get("compId") != null) {
            code = org.get("compId").toString().trim();
        }
        if ((code == null || code.isEmpty()) && user.getOrgUnitCode() != null) {
            code = user.getOrgUnitCode().trim();
        }
        if (code == null || code.isEmpty()) {
            return null;
        }
        return orgUnitRepository.findByCodeIgnoreCase(code)
                .map(OrgUnit::getOrgLevel)
                .orElse(null);
    }

    public String actorUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUser u) {
            return u.getUsername() != null ? u.getUsername() : "";
        }
        return "";
    }

    public static String normalizeMerchantCancelStored(String raw) {
        return UrlPayFollowHqYnUtil.normalizeStored(raw);
    }
}
