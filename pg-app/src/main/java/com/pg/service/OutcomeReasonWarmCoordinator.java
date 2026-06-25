package com.pg.service;

import org.springframework.stereotype.Service;

import java.util.Optional;

/** 처리사유 저장 직후 다국어 캐시 예열 — 호출부에서 {@link Optional} 반환값을 넘깁니다. */
@Service
public class OutcomeReasonWarmCoordinator {

    private final OutcomeReasonTranslateService outcomeReasonTranslateService;

    public OutcomeReasonWarmCoordinator(OutcomeReasonTranslateService outcomeReasonTranslateService) {
        this.outcomeReasonTranslateService = outcomeReasonTranslateService;
    }

    public void onRecorded(Optional<String> recordedReason) {
        if (recordedReason == null || recordedReason.isEmpty()) {
            return;
        }
        outcomeReasonTranslateService.scheduleWarmAllLocales(recordedReason.get());
    }
}
