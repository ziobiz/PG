package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HolidayPresetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/holiday", produces = "application/json")
public class ApiHolidayController {

    private final HolidayPresetService holidayPresetService;

    public ApiHolidayController(HolidayPresetService holidayPresetService) {
        this.holidayPresetService = holidayPresetService;
    }

    /**
     * 연도·국가별 공휴일 프리셋 (KR/US/JP/TH). countries=KR,US 형식, 생략 시 4국.
     */
    @GetMapping("/presets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> presets(
            @RequestParam int year,
            @RequestParam(required = false) String countries) {
        List<String> cc = HolidayPresetService.parseCountryList(countries);
        return ResponseEntity.ok(ApiResponse.ok(holidayPresetService.getPresets(year, cc)));
    }
}
