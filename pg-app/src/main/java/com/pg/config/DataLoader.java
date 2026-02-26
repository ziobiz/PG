package com.pg.config;

import com.pg.entity.*;
import com.pg.repository.*;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin1!"));
                admin.setName("관리자");
                admin.setRole("ADMIN");
                admin.setEnabled(true);
                userRepository.save(admin);
            }
        };
    }

    @Bean
    public CommandLineRunner seedNoticeAndTrnsctn(NoticeRepository noticeRepository,
                                                  PgTrnsctnRepository trnsctnRepository,
                                                  OrgUnitRepository orgUnitRepository,
                                                  MerchantProfileRepository merchantProfileRepository,
                                                  SettlementSettingRepository settlementSettingRepository,
                                                  MerchantCommissionExtraRepository merchantCommissionExtraRepository) {
        return args -> {
            if (noticeRepository.count() == 0) {
                String[] titles = { "PG 통합관리자 시스템 오픈 안내", "정산 주기 변경 안내", "점검 일정 안내" };
                for (String t : titles) {
                    Notice n = new Notice();
                    n.setTitle(t);
                    n.setContent(t + " 내용입니다.");
                    n.setHitCnt((int) (Math.random() * 100));
                    noticeRepository.save(n);
                }
            }
            if (orgUnitRepository.count() == 0) {
                OrgUnit hq = new OrgUnit();
                hq.setOrgLevel(OrgLevel.HEADQUARTERS);
                hq.setCode("HQ01");
                hq.setName("총본사");
                hq.setStatus("ACTIVE");
                orgUnitRepository.save(hq);
                OrgUnit m1 = new OrgUnit();
                m1.setOrgLevel(OrgLevel.MERCHANT);
                m1.setParentId(hq.getId());
                m1.setCode("M001");
                m1.setName("테스트가맹점1");
                m1.setStatus("ACTIVE");
                orgUnitRepository.save(m1);
                OrgUnit m2 = new OrgUnit();
                m2.setOrgLevel(OrgLevel.MERCHANT);
                m2.setParentId(hq.getId());
                m2.setCode("M002");
                m2.setName("테스트가맹점2");
                m2.setStatus("ACTIVE");
                orgUnitRepository.save(m2);
                for (OrgUnit ou : java.util.Arrays.asList(hq, m1, m2)) {
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
                        settlementSettingRepository.save(ss);
                    }
                    if (merchantCommissionExtraRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                        MerchantCommissionExtra ex = new MerchantCommissionExtra();
                        ex.setOrgUnitId(ou.getId());
                        merchantCommissionExtraRepository.save(ex);
                    }
                }
            }
            if (trnsctnRepository.count() == 0) {
                for (int i = 0; i < 15; i++) {
                    PgTrnsctn t = new PgTrnsctn();
                    t.setTrnId("T" + (System.currentTimeMillis() + i));
                    t.setMerchantId(i % 2 == 0 ? "M001" : "M002");
                    t.setServiceType("WEB");
                    t.setStatus(i % 5 == 0 ? "20" : "10");
                    t.setCurType("KRW");
                    t.setAmtKrw(BigDecimal.valueOf(10000 + i * 5000));
                    t.setPayNo("ORD" + (1000 + i));
                    t.setApprovalNo(String.format("%06d", 100000 + i));
                    t.setVan("INICIS");
                    trnsctnRepository.save(t);
                }
            }
        };
    }

    @Bean
    public CommandLineRunner seedDefaultCommissionPolicy(CommissionPolicyRepository commissionPolicyRepository) {
        return args -> {
            if (commissionPolicyRepository.findByScope("DEFAULT").isEmpty()) {
                CommissionPolicy p = new CommissionPolicy();
                p.setScope("DEFAULT");
                p.setPerTxFee(BigDecimal.ZERO);
                p.setCancelRate(BigDecimal.ZERO);
                p.setUsageRate(BigDecimal.ZERO);
                p.setFailFee(BigDecimal.ZERO);
                p.setPayRate(new BigDecimal("2.5"));
                p.setRefundRate(BigDecimal.ZERO);
                p.setRollingPct(new BigDecimal("5"));
                p.setRollingDays(180);
                commissionPolicyRepository.save(p);
            }
        };
    }
}
