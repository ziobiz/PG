package com.pg.service;

/**
 * BOT 일평균 환율을 어느 일자 고시에 맞출지.
 * <ul>
 *   <li>{@link #PREVIOUS_DAY_CLOSE} — 방콕 기준 전일(또는 직전 고시일) 종가에 해당하는 {@code period} 행만 사용. 서버는 같은 방콕 달력일에 BOT HTTP 1회.</li>
 *   <li>{@link #LATEST_BOT_PERIOD} — 응답 내 가장 최근 {@code period}(당일 포함 가능) 사용. 서버는 같은 방콕 달력일에 BOT HTTP 1회.</li>
 * </ul>
 */
public enum BotRateAsOfMode {
    PREVIOUS_DAY_CLOSE,
    LATEST_BOT_PERIOD
}
