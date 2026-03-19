package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.*;
import com.pg.repository.*;
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

    public ApiDevController(OrgUnitRepository orgUnitRepository,
                            MerchantProfileRepository merchantProfileRepository,
                            SettlementSettingRepository settlementSettingRepository,
                            MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                            Environment environment) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.environment = environment;
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
            return ResponseEntity.ok(ApiResponse.fail(
                "dev 프로파일로 서버를 실행해주세요. 서버_재시작.bat 또는 로컬_실행.bat을 사용하세요.",
                "PROFILE_REQUIRED"));
        }
        if (count > 0) {
            return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message", "이미 데이터가 있습니다. 건수: " + orgUnitRepository.count())));
        }
        OrgUnit hq = new OrgUnit();
        hq.setOrgLevel(OrgLevel.HEADQUARTERS);
        hq.setCode("HQ01");
        hq.setName("총본사");
        hq.setStatus("ACTIVE");
        orgUnitRepository.save(hq);

        OrgUnit r01 = new OrgUnit();
        r01.setOrgLevel(OrgLevel.REGIONAL);
        r01.setParentId(hq.getId());
        r01.setCode("R01");
        r01.setName("본사1");
        r01.setStatus("ACTIVE");
        orgUnitRepository.save(r01);

        OrgUnit d01 = new OrgUnit();
        d01.setOrgLevel(OrgLevel.MASTER_DIST);
        d01.setParentId(r01.getId());
        d01.setCode("D01");
        d01.setName("총판1");
        d01.setStatus("ACTIVE");
        orgUnitRepository.save(d01);
        OrgUnit d02 = new OrgUnit();
        d02.setOrgLevel(OrgLevel.MASTER_DIST);
        d02.setParentId(r01.getId());
        d02.setCode("D02");
        d02.setName("총판2");
        d02.setStatus("ACTIVE");
        orgUnitRepository.save(d02);

        OrgUnit b01 = new OrgUnit();
        b01.setOrgLevel(OrgLevel.BRANCH);
        b01.setParentId(d01.getId());
        b01.setCode("B01");
        b01.setName("지사1");
        b01.setStatus("ACTIVE");
        orgUnitRepository.save(b01);
        OrgUnit b02 = new OrgUnit();
        b02.setOrgLevel(OrgLevel.BRANCH);
        b02.setParentId(d02.getId());
        b02.setCode("B02");
        b02.setName("지사2");
        b02.setStatus("ACTIVE");
        orgUnitRepository.save(b02);

        OrgUnit a01 = new OrgUnit();
        a01.setOrgLevel(OrgLevel.AGENCY);
        a01.setParentId(b01.getId());
        a01.setCode("A01");
        a01.setName("대리점1");
        a01.setStatus("ACTIVE");
        orgUnitRepository.save(a01);
        OrgUnit a02 = new OrgUnit();
        a02.setOrgLevel(OrgLevel.AGENCY);
        a02.setParentId(b02.getId());
        a02.setCode("A02");
        a02.setName("대리점2");
        a02.setStatus("ACTIVE");
        orgUnitRepository.save(a02);

        OrgUnit s01 = new OrgUnit();
        s01.setOrgLevel(OrgLevel.SALES_OFFICE);
        s01.setParentId(a01.getId());
        s01.setCode("S01");
        s01.setName("영업점1");
        s01.setStatus("ACTIVE");
        orgUnitRepository.save(s01);
        OrgUnit s02 = new OrgUnit();
        s02.setOrgLevel(OrgLevel.SALES_OFFICE);
        s02.setParentId(a02.getId());
        s02.setCode("S02");
        s02.setName("영업점2");
        s02.setStatus("ACTIVE");
        orgUnitRepository.save(s02);

        OrgUnit m1 = new OrgUnit();
        m1.setOrgLevel(OrgLevel.MERCHANT);
        m1.setParentId(s01.getId());
        m1.setCode("M001");
        m1.setName("가맹점1");
        m1.setStatus("ACTIVE");
        orgUnitRepository.save(m1);
        OrgUnit m2 = new OrgUnit();
        m2.setOrgLevel(OrgLevel.MERCHANT);
        m2.setParentId(s02.getId());
        m2.setCode("M002");
        m2.setName("가맹점2");
        m2.setStatus("ACTIVE");
        orgUnitRepository.save(m2);

        for (OrgUnit ou : Arrays.asList(hq, r01, d01, d02, b01, b02, a01, a02, s01, s02, m1, m2)) {
            if (merchantProfileRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                MerchantProfile mp = new MerchantProfile();
                mp.setOrgUnitId(ou.getId());
                mp.setCompDiv(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
                mp.setUseYn("Y");
                merchantProfileRepository.save(mp);
            }
            if (settlementSettingRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                SettlementSetting ss = new SettlementSetting();
                ss.setOrgUnitId(ou.getId());
                ss.setCalcCycle("D7");
                ss.setTransferType("MANUAL");
                ss.setPayHoldYn("N");
                settlementSettingRepository.save(ss);
            }
            if (merchantCommissionExtraRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                MerchantCommissionExtra ex = new MerchantCommissionExtra();
                ex.setOrgUnitId(ou.getId());
                merchantCommissionExtraRepository.save(ex);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("message", "시드 데이터 생성 완료. 업체관리에서 검색하세요.")));
    }
}
