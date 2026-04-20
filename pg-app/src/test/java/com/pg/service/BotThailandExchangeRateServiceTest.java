package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotThailandExchangeRateServiceTest {

    @Mock
    HqApiConfigRepository hqApiConfigRepository;

    @Mock
    RestTemplate restTemplate;

    private BotThailandExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new BotThailandExchangeRateService(
                hqApiConfigRepository,
                restTemplate,
                "https://iapi.bot.or.th",
                "/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/",
                "",
                "api-key");
    }

    @Test
    void isConfigured_false_when_no_key() {
        when(hqApiConfigRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.isConfigured()).isFalse();
    }

    @Test
    void isConfigured_true_when_yml_key_only() {
        service = new BotThailandExchangeRateService(
                hqApiConfigRepository,
                restTemplate,
                "https://iapi.bot.or.th",
                "/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/",
                "secret-from-yml",
                "api-key");
        when(hqApiConfigRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void isConfigured_true_when_db_key_overrides_empty_yml() {
        HqApiConfig c = new HqApiConfig();
        c.setBotThailandApiKey("db-token");
        when(hqApiConfigRepository.findAll()).thenReturn(List.of(c));
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void fetch_uses_db_base_path_header_and_parses_rates() {
        HqApiConfig c = new HqApiConfig();
        c.setBotThailandApiKey("portal-token");
        c.setBotThailandBaseUrl("https://gateway.api.bot.or.th/Stat-ExchangeRate/v2");
        c.setBotThailandDailyAvgPath("/DAILY_AVG_EXG_RATE/");
        c.setBotThailandApiKeyHeader("Authorization");
        when(hqApiConfigRepository.findAll()).thenReturn(List.of(c));

        String json = """
                {"result":{"data":{"data_detail":[
                  {"period":"2026-04-10","currency_name_eng":"#JAPAN (JPY100) -E-","mid_rate":"21.0"},
                  {"period":"2026-04-18","currency_name_eng":"#JAPAN (JPY100) -E-","mid_rate":"22.5"},
                  {"period":"2026-04-18","currency_name_eng":"US DOLLAR 1 USD","mid_rate":"36.7"}
                ]}}}
                """;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        Optional<BotThailandExchangeRateService.BotDailyRates> out = service.fetchLatestThbPerUnitRates();
        assertThat(out).isPresent();
        assertThat(out.get().period()).isEqualTo("2026-04-18");
        assertThat(out.get().thbPerJpy()).isEqualByComparingTo("0.225");
        assertThat(out.get().thbPerUsd()).isEqualByComparingTo("36.7");

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> entCap = (ArgumentCaptor) ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCap.capture(), eq(HttpMethod.GET), entCap.capture(), eq(String.class));
        assertThat(urlCap.getValue()).startsWith("https://gateway.api.bot.or.th/Stat-ExchangeRate/v2/DAILY_AVG_EXG_RATE/");
        assertThat(urlCap.getValue()).contains("start_period=").contains("end_period=");
        assertThat(entCap.getValue().getHeaders().getFirst("Authorization")).isEqualTo("portal-token");
    }

    @Test
    void fetch_second_call_within_cache_does_not_call_rest_again() {
        HqApiConfig c = new HqApiConfig();
        c.setBotThailandApiKey("k");
        when(hqApiConfigRepository.findAll()).thenReturn(List.of(c));
        String json = """
                {"result":{"data":{"data_detail":[
                  {"period":"2026-04-18","currency_name_eng":"JPY","mid_rate":"1"}
                ]}}}
                """;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        assertThat(service.fetchLatestThbPerUnitRates()).isPresent();
        assertThat(service.fetchLatestThbPerUnitRates()).isPresent();
        verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void fetch_different_bot_modes_same_bangkok_day_calls_rest_twice() {
        HqApiConfig c = new HqApiConfig();
        c.setBotThailandApiKey("k");
        when(hqApiConfigRepository.findAll()).thenReturn(List.of(c));
        String json = """
                {"result":{"data":{"data_detail":[
                  {"period":"2026-04-18","currency_name_eng":"JPY","mid_rate":"1"}
                ]}}}
                """;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        assertThat(service.fetchThbPerUnitRates(BotRateAsOfMode.LATEST_BOT_PERIOD)).isPresent();
        assertThat(service.fetchThbPerUnitRates(BotRateAsOfMode.PREVIOUS_DAY_CLOSE)).isPresent();
        verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void fetch_returns_empty_when_key_missing() {
        when(hqApiConfigRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.fetchLatestThbPerUnitRates()).isEmpty();
        verify(restTemplate, times(0)).exchange(any(), any(), any(), any(Class.class));
    }

    @Test
    void fetch_parses_krw_when_label_varies_and_mid_is_numeric() {
        HqApiConfig c = new HqApiConfig();
        c.setBotThailandApiKey("k");
        when(hqApiConfigRepository.findAll()).thenReturn(List.of(c));
        String json = """
                {"result":{"data":{"data_detail":[
                  {"period":"2026-04-18","Currency_Name_ENG":"#REP. OF KOREA (1000) -E-","mid_rate":0.0234},
                  {"period":"2026-04-18","currency_name_eng":"US DOLLAR 1 USD","mid_rate":"36.7"}
                ]}}}
                """;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        Optional<BotThailandExchangeRateService.BotDailyRates> out = service.fetchLatestThbPerUnitRates();
        assertThat(out).isPresent();
        assertThat(out.get().thbPerKrw()).isEqualByComparingTo("0.0000234");
        assertThat(out.get().thbPerUsd()).isEqualByComparingTo("36.7");
    }
}
