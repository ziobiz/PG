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
    public CommandLineRunner seedPgAgency(PgAgencyRepository pgAgencyRepository) {
        return args -> {
            if (pgAgencyRepository.count() == 0) {
                String[][] agencies = {
                    {"PG01", "이니시스(INICIS)"}, {"PG02", "다날"}, {"PG03", "KG이니시스"}, {"PG04", "나이스페이"},
                    {"CHILLPAY", "ChillPay(칠리페이)"}
                };
                for (String[] a : agencies) {
                    PgAgency pa = new PgAgency();
                    pa.setPgCd(a[0]);
                    pa.setPgNm(a[1]);
                    pa.setApiEndpoint("CHILLPAY".equals(a[0]) ? "https://api-directcredit.chillpay.co" : "https://api.example.com/" + a[0].toLowerCase());
                    pa.setUseYn("Y");
                    pgAgencyRepository.save(pa);
                }
            } else if (pgAgencyRepository.findByPgCd("CHILLPAY").isEmpty()) {
                PgAgency pa = new PgAgency();
                pa.setPgCd("CHILLPAY");
                pa.setPgNm("ChillPay(칠리페이)");
                pa.setApiEndpoint("https://api-directcredit.chillpay.co");
                pa.setUseYn("Y");
                pgAgencyRepository.save(pa);
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

    /** a본사, b총판, c지사, d대리점, e가맹점 계층 생성. e가맹점은 ChillPay 사용 */
    @Bean
    public CommandLineRunner seedOrgHierarchy(OrgUnitRepository orgUnitRepository,
                                              MerchantProfileRepository merchantProfileRepository,
                                              SettlementSettingRepository settlementSettingRepository,
                                              MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                                              MerchantPgBindingRepository merchantPgBindingRepository,
                                              PgAgencyRepository pgAgencyRepository,
                                              UserRepository userRepository,
                                              PasswordEncoder passwordEncoder) {
        return args -> {
            if (orgUnitRepository.findByCode("A01").isPresent()) return;
            OrgUnit hq = orgUnitRepository.findAll().stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS)
                    .findFirst()
                    .orElseGet(() -> {
                        OrgUnit x = new OrgUnit();
                        x.setOrgLevel(OrgLevel.HEADQUARTERS);
                        x.setCode("HQ01");
                        x.setName("총본사");
                        x.setStatus("ACTIVE");
                        return orgUnitRepository.save(x);
                    });

            OrgUnit a = new OrgUnit();
            a.setOrgLevel(OrgLevel.REGIONAL);
            a.setParentId(hq.getId());
            a.setCode("A01");
            a.setName("a본사");
            a.setStatus("ACTIVE");
            orgUnitRepository.save(a);

            OrgUnit b = new OrgUnit();
            b.setOrgLevel(OrgLevel.MASTER_DIST);
            b.setParentId(a.getId());
            b.setCode("B01");
            b.setName("b총판");
            b.setStatus("ACTIVE");
            orgUnitRepository.save(b);

            OrgUnit c = new OrgUnit();
            c.setOrgLevel(OrgLevel.BRANCH);
            c.setParentId(b.getId());
            c.setCode("C01");
            c.setName("c지사");
            c.setStatus("ACTIVE");
            orgUnitRepository.save(c);

            OrgUnit d = new OrgUnit();
            d.setOrgLevel(OrgLevel.AGENCY);
            d.setParentId(c.getId());
            d.setCode("D01");
            d.setName("d대리점");
            d.setStatus("ACTIVE");
            orgUnitRepository.save(d);

            OrgUnit e = new OrgUnit();
            e.setOrgLevel(OrgLevel.MERCHANT);
            e.setParentId(d.getId());
            e.setCode("E01");
            e.setName("e가맹점");
            e.setStatus("ACTIVE");
            orgUnitRepository.save(e);

            for (OrgUnit ou : java.util.Arrays.asList(a, b, c, d, e)) {
                if (merchantProfileRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                    MerchantProfile mp = new MerchantProfile();
                    mp.setOrgUnitId(ou.getId());
                    mp.setCompDiv(ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
                    mp.setUseYn("Y");
                    mp.setLoginId(ou.getCode().toLowerCase());
                    mp.setRegNo("123-45-67890");
                    mp.setCeoNm(ou.getName() + " 대표");
                    mp.setCeoMobile("010-0000-0000");
                    mp.setCompTel("02-0000-0000");
                    mp.setZipCode("00000");
                    mp.setAddr("서울시");
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

            if (pgAgencyRepository.findByPgCd("CHILLPAY").isPresent() && merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(e.getId()).stream().noneMatch(mb -> "CHILLPAY".equals(mb.getPgCd()))) {
                MerchantPgBinding binding = new MerchantPgBinding();
                binding.setOrgUnitId(e.getId());
                binding.setPgCd("CHILLPAY");
                binding.setActivationYn("Y");
                binding.setOperationalYn("Y");
                binding.setPayMethod("WEB");
                binding.setMid("M035594");
                binding.setApiKey("");
                binding.setIvKey("");
                binding.setSortOrder(1);
                merchantPgBindingRepository.save(binding);
            }

            String defaultPwd = "test123!";
            String[][] users = new String[][] {
                {"a01", "a본사"}, {"b01", "b총판"}, {"c01", "c지사"}, {"d01", "d대리점"}, {"e01", "e가맹점"}
            };
            for (String[] u : users) {
                if (userRepository.findByUsername(u[0]).isEmpty()) {
                    AppUser appUser = new AppUser();
                    appUser.setUsername(u[0]);
                    appUser.setPassword(passwordEncoder.encode(defaultPwd));
                    appUser.setName(u[1]);
                    appUser.setRole("USER");
                    appUser.setEnabled(true);
                    userRepository.save(appUser);
                }
            }
        };
    }
}
