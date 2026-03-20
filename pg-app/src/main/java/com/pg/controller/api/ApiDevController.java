package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.*;
import com.pg.repository.*;
import com.pg.service.OrgHierarchyResetService;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;

/**
 * 시드 데이터 수동 생성 API. dev 프로파일에서만 실제 생성.
 * dev가 아니면 안내 메시지 반환 (404 대신).
 */
@RestController
@RequestMapping(value = "/api/dev", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiDevController {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final Environment environment;
    private final OrgHierarchyResetService orgHierarchyResetService;

    public ApiDevController(OrgUnitRepository orgUnitRepository,
                            MerchantProfileRepository merchantProfileRepository,
                            SettlementSettingRepository settlementSettingRepository,
                            MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                            Environment environment,
                            OrgHierarchyResetService orgHierarchyResetService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.environment = environment;
        this.orgHierarchyResetService = orgHierarchyResetService;
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Object>> ping() {
        return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message", "pong")));
    }

    @GetMapping("/seed")
    public ResponseEntity<ApiResponse<Object>> seedOrg() {
        boolean isDev = Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
        long count = orgUnitRepository.count();
        if (!isDev && count > 0) {
            ApiResponse<Object> fail = ApiResponse.fail(
                "dev 프로파일로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.",
                "PROFILE_REQUIRED");
            return ResponseEntity.ok(fail);
        }
        if (count > 0) {
            return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message",
                    "이미 업체 데이터가 있습니다. 초기화: GET /api/dev/reset-org-hierarchy (dev 전용) 또는 app.data.reset-org-hierarchy-on-startup=true 로 기동.")));
        }
        OrgUnit hq = new OrgUnit();
        hq.setOrgLevel(OrgLevel.HEADQUARTERS);
        hq.setCode("0000000000");
        hq.setName("OTL HQ");
        hq.setStatus("ACTIVE");
        orgUnitRepository.save(hq);

        MerchantProfile mp = new MerchantProfile();
        mp.setOrgUnitId(hq.getId());
        mp.setCompDiv(OrgLevel.HEADQUARTERS.name());
        mp.setUseYn("Y");
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(hq.getId());
        ss.setCalcCycle("D7");
        ss.setTransferType("MANUAL");
        ss.setPayHoldYn("N");
        settlementSettingRepository.save(ss);

        MerchantCommissionExtra ex = new MerchantCommissionExtra();
        ex.setOrgUnitId(hq.getId());
        merchantCommissionExtraRepository.save(ex);

        return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message", "총본사(0000000000)만 생성했습니다. 하위 업체는 화면에서 등록하세요.")));
    }

    /**
     * 업체·결제·수수료 등 조직 연관 데이터 전부 삭제 후 0000000000만 재생성. dev 프로파일에서만 허용.
     */
    @GetMapping("/reset-org-hierarchy")
    public ResponseEntity<ApiResponse<Object>> resetOrgHierarchy() {
        boolean isDev = Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
        if (!isDev) {
            ApiResponse<Object> fail = ApiResponse.fail("dev 프로파일에서만 사용할 수 있습니다.", "PROFILE_REQUIRED");
            return ResponseEntity.ok(fail);
        }
        orgHierarchyResetService.resetToHeadquartersOnly();
        return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message",
                "모든 업체·연관 데이터를 삭제하고 OTL HQ(0000000000)만 남겼습니다. ADMIN 외 사용자·토큰은 삭제되었습니다. 다시 로그인하세요.")));
    }
}
