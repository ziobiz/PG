package com.pg.config;

import com.pg.catalog.PageMenuCatalog;
import com.pg.entity.*;
import com.pg.repository.*;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.HqNotifyEnvService;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner ensureHqNotifyEnv(HqNotifyEnvService hqNotifyEnvService) {
        return args -> hqNotifyEnvService.getOrCreate();
    }

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder, Environment env) {
        return args -> {
            String defaultPwd = "admin1!";
            Optional<AppUser> adminOpt = userRepository.findByUsername("admin");
            if (adminOpt.isEmpty()) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(defaultPwd));
                admin.setName("관리자");
                admin.setRole("ADMIN");
                admin.setEnabled(true);
                admin.setOrgUnitCode("0000000000");
                admin.setPermissionGroupNm("시스템관리자");
                admin.setOtpRegisteredYn("Y");
                userRepository.save(admin);
            } else if (java.util.Arrays.stream(env.getActiveProfiles()).anyMatch("dev"::equals)) {
                AppUser admin = adminOpt.get();
                admin.setPassword(passwordEncoder.encode(defaultPwd));
                admin.setEnabled(true);
                if (admin.getOrgUnitCode() == null || admin.getOrgUnitCode().isBlank()) {
                    admin.setOrgUnitCode("0000000000");
                }
                if (admin.getPermissionGroupNm() == null || admin.getPermissionGroupNm().isBlank()) {
                    admin.setPermissionGroupNm("시스템관리자");
                }
                admin.setOtpRegisteredYn("Y");
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
                hq.setCode("0000000000");
                hq.setName("OTL HQ");
                hq.setStatus("ACTIVE");
                orgUnitRepository.save(hq);
                for (OrgUnit ou : java.util.Collections.singletonList(hq)) {
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
                        ss.setCalcProcType("MANUAL");
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
            }
            if (trnsctnRepository.count() == 0 && orgUnitRepository.findByCode("M001").isPresent()) {
                for (int i = 0; i < 30; i++) {
                    PgTrnsctn t = new PgTrnsctn();
                    t.setTrnId("T" + (System.currentTimeMillis() + i));
                    t.setMerchantId(i % 2 == 0 ? "M001" : "M002");
                    t.setServiceType("WEB");
                    switch (i % 8) {
                        case 0:
                            t.setStatus("10");
                            break;
                        case 1:
                            t.setStatus("20");
                            break;
                        case 2:
                            t.setStatus("F0");
                            break;
                        case 3:
                            t.setStatus("30");
                            break;
                        case 4:
                            t.setStatus("31");
                            break;
                        case 5:
                            t.setStatus("99");
                            break;
                        default:
                            t.setStatus("10");
                    }
                    if (i % 9 == 0) {
                        t.setOrigin("NOTI");
                    } else if (i % 7 == 0) {
                        t.setOrigin("URL");
                    } else if (i % 13 == 0) {
                        t.setOrigin("CHATBOT");
                    } else {
                        t.setOrigin(null);
                    }
                    t.setCurType("KRW");
                    t.setAmtKrw(BigDecimal.valueOf(10000 + i * 5000L));
                    t.setPayNo("ORD" + (1000 + i));
                    t.setOrderNo("ORD" + (1000 + i));
                    t.setChillTransactionId(String.valueOf(8_000_000L + i));
                    t.setCustomerId("guest_" + i);
                    t.setCustomerNm("시드고객" + i);
                    t.setPaymentChannel(i % 3 == 0 ? "CARD" : (i % 3 == 1 ? "BANK" : "CHILLPAY"));
                    t.setPaidAt(LocalDateTime.now().minusMinutes(i));
                    t.setIcopayAmt(BigDecimal.valueOf(50L + i));
                    t.setChillFeeAmt(BigDecimal.valueOf(30L + i % 5));
                    t.setTotalAmt(t.getAmtKrw().add(t.getChillFeeAmt()));
                    t.setRouteNo(String.valueOf(4 + (i % 3)));
                    t.setChillPaymentStatus("10".equals(t.getStatus()) ? "Paid" : ("20".equals(t.getStatus()) ? "Cancelled" : "WaitAuthorize"));
                    t.setSettledYn(i % 5 == 0 ? "Y" : "N");
                    t.setApprovalNo(String.format("%06d", 100000 + i));
                    t.setVan("CHILLPAY");
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

    /** 레거시 데모 계층 (기본 비활성). 프로퍼티 app.data.seed-legacy-demo-hierarchy 가 true 일 때만 실행 */
    @Bean
    @ConditionalOnProperty(prefix = "app.data", name = "seed-legacy-demo-hierarchy", havingValue = "true")
    public CommandLineRunner seedOrgHierarchy(OrgUnitRepository orgUnitRepository,
                                              MerchantProfileRepository merchantProfileRepository,
                                              SettlementSettingRepository settlementSettingRepository,
                                              MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                                              MerchantPgBindingRepository merchantPgBindingRepository,
                                              PgAgencyRepository pgAgencyRepository,
                                              UserRepository userRepository,
                                              PasswordEncoder passwordEncoder) {
        return args -> {
            if (orgUnitRepository.findByCode("M100").isPresent()) return;

            if (orgUnitRepository.findByCode("A01").isPresent()) {
                OrgUnit d = orgUnitRepository.findByCode("D01").orElse(null);
                if (d == null) return;
                OrgUnit e2 = new OrgUnit();
                e2.setOrgLevel(OrgLevel.MERCHANT);
                e2.setParentId(d.getId());
                e2.setCode("M100");
                e2.setName("테스트가맹점");
                e2.setStatus("ACTIVE");
                orgUnitRepository.save(e2);
                for (OrgUnit ou : java.util.Collections.singletonList(e2)) {
                    MerchantProfile mp = new MerchantProfile();
                    mp.setOrgUnitId(ou.getId());
                    mp.setCompDiv("MERCHANT");
                    mp.setUseYn("Y");
                    mp.setLoginId("m100");
                    mp.setRegNo("123-45-67890");
                    mp.setCeoNm("홍길동");
                    mp.setCeoMobile("010-1234-5678");
                    mp.setCompTel("02-1234-5678");
                    mp.setZipCode("00000");
                    mp.setAddr("서울시 강남구");
                    mp.setAddrDetail("테헤란로 123");
                    mp.setEmail("merchant@test.com");
                    mp.setBankCd("04");
                    mp.setAccountNo("123-456-789");
                    mp.setAccountHolder("홍길동");
                    merchantProfileRepository.save(mp);
                    SettlementSetting ss = new SettlementSetting();
                    ss.setOrgUnitId(ou.getId());
                    ss.setCalcCycle("D7");
                    ss.setCalcProcType("MANUAL");
                    ss.setTransferType("MANUAL");
                    settlementSettingRepository.save(ss);
                    MerchantCommissionExtra ex = new MerchantCommissionExtra();
                    ex.setOrgUnitId(ou.getId());
                    merchantCommissionExtraRepository.save(ex);
                }
                if (pgAgencyRepository.findByPgCd("CHILLPAY").isPresent()) {
                    MerchantPgBinding binding2 = new MerchantPgBinding();
                    binding2.setOrgUnitId(e2.getId());
                    binding2.setPgCd("CHILLPAY");
                    binding2.setActivationYn("Y");
                    binding2.setOperationalYn("Y");
                    binding2.setPayMethod("WEB");
                    binding2.setMid("M035594");
                    binding2.setRootNo("4");
                    binding2.setApiKey("");
                    binding2.setIvKey("");
                    binding2.setSortOrder(1);
                    merchantPgBindingRepository.save(binding2);
                }
                if (userRepository.findByUsername("m100").isEmpty()) {
                    AppUser appUser = new AppUser();
                    appUser.setUsername("m100");
                    appUser.setPassword(passwordEncoder.encode("test123!"));
                    appUser.setName("테스트가맹점");
                    appUser.setRole("USER");
                    appUser.setEnabled(true);
                    userRepository.save(appUser);
                }
                return;
            }

            OrgUnit hq = orgUnitRepository.findAll().stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS)
                    .findFirst()
                    .orElseGet(() -> {
                        OrgUnit x = new OrgUnit();
                        x.setOrgLevel(OrgLevel.HEADQUARTERS);
                        x.setCode("0000000000");
                        x.setName("OTL HQ");
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

            OrgUnit e2 = new OrgUnit();
            e2.setOrgLevel(OrgLevel.MERCHANT);
            e2.setParentId(d.getId());
            e2.setCode("M100");
            e2.setName("테스트가맹점");
            e2.setStatus("ACTIVE");
            orgUnitRepository.save(e2);

            OrgUnit a2 = new OrgUnit();
            a2.setOrgLevel(OrgLevel.REGIONAL);
            a2.setParentId(hq.getId());
            a2.setCode("A02");
            a2.setName("샘플본사");
            a2.setStatus("ACTIVE");
            orgUnitRepository.save(a2);

            OrgUnit b2 = new OrgUnit();
            b2.setOrgLevel(OrgLevel.MASTER_DIST);
            b2.setParentId(a2.getId());
            b2.setCode("B02");
            b2.setName("샘플총판");
            b2.setStatus("ACTIVE");
            orgUnitRepository.save(b2);

            OrgUnit c2 = new OrgUnit();
            c2.setOrgLevel(OrgLevel.BRANCH);
            c2.setParentId(b2.getId());
            c2.setCode("C02");
            c2.setName("샘플지사");
            c2.setStatus("ACTIVE");
            orgUnitRepository.save(c2);

            OrgUnit d2 = new OrgUnit();
            d2.setOrgLevel(OrgLevel.AGENCY);
            d2.setParentId(c2.getId());
            d2.setCode("D02");
            d2.setName("샘플대리점");
            d2.setStatus("ACTIVE");
            orgUnitRepository.save(d2);

            for (OrgUnit ou : java.util.Arrays.asList(a, b, c, d, e, e2, a2, b2, c2, d2)) {
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
                    if (ou.getOrgLevel() == OrgLevel.MERCHANT && "M100".equals(ou.getCode())) {
                        mp.setCeoNm("홍길동");
                        mp.setCeoMobile("010-1234-5678");
                        mp.setCompTel("02-1234-5678");
                        mp.setRegNo("123-45-67890");
                        mp.setAddr("서울시 강남구");
                        mp.setAddrDetail("테헤란로 123");
                        mp.setEmail("merchant@test.com");
                        mp.setBankCd("04");
                        mp.setAccountNo("123-456-789");
                        mp.setAccountHolder("홍길동");
                    }
                    merchantProfileRepository.save(mp);
                }
                if (settlementSettingRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                    SettlementSetting ss = new SettlementSetting();
                    ss.setOrgUnitId(ou.getId());
                    ss.setCalcCycle("D7");
                    ss.setCalcProcType("MANUAL");
                    ss.setTransferType("MANUAL");
                    settlementSettingRepository.save(ss);
                }
                if (merchantCommissionExtraRepository.findByOrgUnitId(ou.getId()).isEmpty()) {
                    MerchantCommissionExtra ex = new MerchantCommissionExtra();
                    ex.setOrgUnitId(ou.getId());
                    merchantCommissionExtraRepository.save(ex);
                }
            }

            if (pgAgencyRepository.findByPgCd("CHILLPAY").isPresent()) {
                if (merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(e.getId()).stream().noneMatch(mb -> "CHILLPAY".equals(mb.getPgCd()))) {
                    MerchantPgBinding binding = new MerchantPgBinding();
                    binding.setOrgUnitId(e.getId());
                    binding.setPgCd("CHILLPAY");
                    binding.setActivationYn("Y");
                    binding.setOperationalYn("Y");
                    binding.setPayMethod("WEB");
                    binding.setMid("M035594");
                    binding.setRootNo("4");
                    binding.setApiKey("");
                    binding.setIvKey("");
                    binding.setSortOrder(1);
                    merchantPgBindingRepository.save(binding);
                }
                if (merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(e2.getId()).stream().noneMatch(mb -> "CHILLPAY".equals(mb.getPgCd()))) {
                    MerchantPgBinding binding2 = new MerchantPgBinding();
                    binding2.setOrgUnitId(e2.getId());
                    binding2.setPgCd("CHILLPAY");
                    binding2.setActivationYn("Y");
                    binding2.setOperationalYn("Y");
                    binding2.setPayMethod("WEB");
                    binding2.setMid("M035594");
                    binding2.setRootNo("4");
                    binding2.setApiKey("");
                    binding2.setIvKey("");
                    binding2.setSortOrder(1);
                    merchantPgBindingRepository.save(binding2);
                }
            }

            String defaultPwd = "test123!";
            String[][] users = new String[][] {
                {"a01", "a본사"}, {"b01", "b총판"}, {"c01", "c지사"}, {"d01", "d대리점"}, {"e01", "e가맹점"}, {"m100", "테스트가맹점"},
                {"a02", "샘플본사"}, {"b02", "샘플총판"}, {"c02", "샘플지사"}, {"d02", "샘플대리점"}
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

    /** M100 데모 가맹점 (기본 비활성). 프로퍼티 app.data.seed-legacy-demo-hierarchy 가 true 일 때만 */
    @Bean
    @Order(1000)
    @ConditionalOnProperty(prefix = "app.data", name = "seed-legacy-demo-hierarchy", havingValue = "true")
    public CommandLineRunner seedM100(OrgUnitRepository orgUnitRepository,
                                     MerchantProfileRepository merchantProfileRepository,
                                     SettlementSettingRepository settlementSettingRepository,
                                     MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                                     MerchantPgBindingRepository merchantPgBindingRepository,
                                     PgAgencyRepository pgAgencyRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (orgUnitRepository.findByCode("M100").isPresent()) return;

            OrgUnit parent = orgUnitRepository.findByCode("D01")
                    .or(() -> orgUnitRepository.findAll().stream()
                            .filter(o -> o.getOrgLevel() == OrgLevel.AGENCY)
                            .findFirst())
                    .orElseGet(() -> {
                        OrgUnit hq = orgUnitRepository.findAll().stream()
                                .filter(o -> o.getOrgLevel() == OrgLevel.HEADQUARTERS)
                                .findFirst()
                                .orElseGet(() -> {
                                    OrgUnit x = new OrgUnit();
                                    x.setOrgLevel(OrgLevel.HEADQUARTERS);
                                    x.setCode("0000000000");
                                    x.setName("OTL HQ");
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
                        return orgUnitRepository.save(d);
                    });

            OrgUnit e2 = new OrgUnit();
            e2.setOrgLevel(OrgLevel.MERCHANT);
            e2.setParentId(parent.getId());
            e2.setCode("M100");
            e2.setName("테스트가맹점");
            e2.setStatus("ACTIVE");
            orgUnitRepository.save(e2);

            MerchantProfile mp = new MerchantProfile();
            mp.setOrgUnitId(e2.getId());
            mp.setCompDiv("MERCHANT");
            mp.setUseYn("Y");
            mp.setLoginId("m100");
            mp.setRegNo("123-45-67890");
            mp.setCeoNm("홍길동");
            mp.setCeoMobile("010-1234-5678");
            mp.setCompTel("02-1234-5678");
            mp.setZipCode("00000");
            mp.setAddr("서울시 강남구");
            mp.setAddrDetail("테헤란로 123");
            mp.setEmail("merchant@test.com");
            mp.setBankCd("04");
            mp.setAccountNo("123-456-789");
            mp.setAccountHolder("홍길동");
            merchantProfileRepository.save(mp);

            SettlementSetting ss = new SettlementSetting();
            ss.setOrgUnitId(e2.getId());
            ss.setCalcCycle("D7");
            ss.setCalcProcType("MANUAL");
            ss.setTransferType("MANUAL");
            settlementSettingRepository.save(ss);

            MerchantCommissionExtra ex = new MerchantCommissionExtra();
            ex.setOrgUnitId(e2.getId());
            merchantCommissionExtraRepository.save(ex);

            if (pgAgencyRepository.findByPgCd("CHILLPAY").isPresent()) {
                MerchantPgBinding binding = new MerchantPgBinding();
                binding.setOrgUnitId(e2.getId());
                binding.setPgCd("CHILLPAY");
                binding.setActivationYn("Y");
                binding.setOperationalYn("Y");
                binding.setPayMethod("WEB");
                binding.setMid("M035594");
                binding.setRootNo("4");
                binding.setApiKey("");
                binding.setIvKey("");
                binding.setSortOrder(1);
                merchantPgBindingRepository.save(binding);
            }

            if (userRepository.findByUsername("m100").isEmpty()) {
                AppUser appUser = new AppUser();
                appUser.setUsername("m100");
                appUser.setPassword(passwordEncoder.encode("test123!"));
                appUser.setName("테스트가맹점");
                appUser.setRole("USER");
                appUser.setEnabled(true);
                userRepository.save(appUser);
            }
        };
    }

    /**
     * 가맹점(MERCHANT) 조직 기본 화면권한 — 구 SPA 하드코딩(공지·업체정보조회만 허용)과 동일.
     * 이미 tb_org_page_permission 에 MERCHANT 행이 있으면 스킵(운영에서 조직별 권한 세팅으로 변경한 경우 보존).
     */
    @Bean
    @Order(200)
    public CommandLineRunner seedMerchantOrgPagePermissions(OrgPagePermissionRepository orgPagePermissionRepository) {
        return args -> {
            if (orgPagePermissionRepository.countByOrgLevel(OrgLevel.MERCHANT.name()) > 0) {
                return;
            }
            for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
                String url = item.pageUrl();
                OrgPagePermission row = new OrgPagePermission();
                row.setOrgLevel(OrgLevel.MERCHANT.name());
                row.setPageUrl(url);
                row.setMenuId(item.menuId());
                if ("/system/noticeList".equals(url) || "/comp/myCompMng".equals(url)) {
                    row.setPermission(OrgPagePermissionService.P_OBSERVER);
                } else {
                    row.setPermission(OrgPagePermissionService.P_NONE);
                }
                orgPagePermissionRepository.save(row);
            }
        };
    }
}
