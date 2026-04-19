package com.pg.api.dto;

import java.time.ZoneId;

/**
 * 결제·수수료 그리드 거래시각 2줄 표시: 1줄(총판 설정 프리셋) + 2줄(총판 정산 크론 Zone).
 */
public record TxnDualLineSpec(String tag1, ZoneId displayZone1, String tag2, ZoneId displayZone2) {
}
