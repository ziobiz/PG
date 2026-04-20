package com.pg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.MerchantProfile;
import com.pg.entity.SettlementSetting;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.UserRepository;
import com.pg.util.TotpRfc6238;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수수료 인라인 저장과 동일한 폼 POST → 배분율 반영 및 변경 이력 적재 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CommissionSaveAndHistoryApiTest {

    private static final String TEST_MERCHANT_CODE = "TSTC01";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrgUnitRepository orgUnitRepository;
    @Autowired
    MerchantProfileRepository merchantProfileRepository;
    @Autowired
    SettlementSettingRepository settlementSettingRepository;
    @Autowired
    MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    @Autowired
    UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void ensureTestMerchant() {
        OrgUnit hq = orgUnitRepository.findByCode("0000000000").orElseGet(() -> {
            OrgUnit h = new OrgUnit();
            h.setOrgLevel(OrgLevel.HEADQUARTERS);
            h.setCode("0000000000");
            h.setName("Test HQ");
            h.setStatus("ACTIVE");
            return orgUnitRepository.save(h);
        });
        if (orgUnitRepository.findByCode(TEST_MERCHANT_CODE).isPresent()) {
            return;
        }
        OrgUnit m = new OrgUnit();
        m.setOrgLevel(OrgLevel.MERCHANT);
        m.setParentId(hq.getId());
        m.setCode(TEST_MERCHANT_CODE);
        m.setName("Commission API Test Merchant");
        m.setStatus("ACTIVE");
        orgUnitRepository.save(m);
        OrgUnit saved = orgUnitRepository.findByCode(TEST_MERCHANT_CODE).orElseThrow();

        MerchantProfile mp = new MerchantProfile();
        mp.setOrgUnitId(saved.getId());
        mp.setCompDiv("MERCHANT");
        mp.setUseYn("Y");
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(saved.getId());
        ss.setCalcCycle("D7");
        ss.setCalcProcType("MANUAL");
        ss.setTransferType("MANUAL");
        ss.setPayHoldYn("N");
        settlementSettingRepository.save(ss);

        MerchantCommissionExtra ex = new MerchantCommissionExtra();
        ex.setOrgUnitId(saved.getId());
        merchantCommissionExtraRepository.save(ex);
    }

    private String loginToken() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin1!\"}";
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(login.getResponse().getContentAsString());
        if (!root.path("success").asBoolean() && "OTP_REQUIRED".equals(root.path("errorCode").asText())) {
            var admin = userRepository.findByUsername("admin").orElseThrow();
            if (admin.getOtpSecret() != null && !admin.getOtpSecret().isBlank()) {
                String code = TotpRfc6238.currentTotpCode(admin.getOtpSecret());
                body = "{\"username\":\"admin\",\"password\":\"admin1!\",\"totpCode\":\"" + code + "\"}";
                login = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isOk())
                        .andReturn();
                root = objectMapper.readTree(login.getResponse().getContentAsString());
            }
        }
        assertThat(root.path("success").asBoolean()).isTrue();
        String token = root.path("data").path("token").asText(null);
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    void commissionSave_persistsDistribution_andAppendsHistory() throws Exception {
        String token = loginToken();
        String auth = "Bearer " + token;

        mockMvc.perform(post("/api/commission/save")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("compId", "tstc01")
                        .param("hqRate", "4.4")
                        .param("regionalRate", "0")
                        .param("masterRate", "0")
                        .param("branchRate", "0")
                        .param("agencyRate", "0")
                        .param("salesOfficeRate", "0")
                        .param("hqPerTxFee", "11")
                        .param("regionalPerTxFee", "0")
                        .param("masterPerTxFee", "0")
                        .param("branchPerTxFee", "0")
                        .param("agencyPerTxFee", "0")
                        .param("salesOfficePerTxFee", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult listRes = mockMvc.perform(get("/api/commission/list")
                        .header("Authorization", auth)
                        .param("searchCompId", TEST_MERCHANT_CODE)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode listRoot = objectMapper.readTree(listRes.getResponse().getContentAsString());
        JsonNode first = listRoot.path("data").path("list").path(0);
        assertThat(first.path("compId").asText()).isEqualTo(TEST_MERCHANT_CODE);
        assertThat(first.path("hqRate").asDouble()).isEqualTo(4.4);
        assertThat(first.path("hqPerTxFee").asDouble()).isEqualTo(11.0);

        mockMvc.perform(get("/api/commission/history")
                        .header("Authorization", auth)
                        .param("compId", TEST_MERCHANT_CODE)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list[0].endDttm").value(org.hamcrest.Matchers.containsString("9999")))
                .andExpect(jsonPath("$.data.list[0].hqRate").value(org.hamcrest.Matchers.closeTo(4.4, 0.05)));

        mockMvc.perform(post("/api/commission/save")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("compId", TEST_MERCHANT_CODE)
                        .param("hqRate", "5.0")
                        .param("regionalRate", "0")
                        .param("masterRate", "0")
                        .param("branchRate", "0")
                        .param("agencyRate", "0")
                        .param("salesOfficeRate", "0")
                        .param("hqPerTxFee", "12")
                        .param("regionalPerTxFee", "0")
                        .param("masterPerTxFee", "0")
                        .param("branchPerTxFee", "0")
                        .param("agencyPerTxFee", "0")
                        .param("salesOfficePerTxFee", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/commission/history")
                        .header("Authorization", auth)
                        .param("compId", TEST_MERCHANT_CODE)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.list[0].hqRate").value(org.hamcrest.Matchers.closeTo(5.0, 0.05)))
                .andExpect(jsonPath("$.data.list[1].hqRate").value(org.hamcrest.Matchers.closeTo(4.4, 0.05)));
    }
}
