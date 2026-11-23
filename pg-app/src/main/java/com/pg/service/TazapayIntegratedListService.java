package com.pg.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tazapay 통합내역 — GET payin list API 연동 예정(TODO).
 * 현재는 운영자 UI 옵션·체크리스트용 스텁.
 */
@Service
public class TazapayIntegratedListService {

    public Map<String, Object> stubNotReady() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", "TAZAPAY_LIST_TODO");
        out.put("message", "Tazapay 통합내역 목록 조회는 다음 릴리스에 제공됩니다. 결제내역은 ICOPAY 거래목록을 사용하세요.");
        return out;
    }
}