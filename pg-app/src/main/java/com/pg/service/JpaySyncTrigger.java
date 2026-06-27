package com.pg.service;

/**
 * JPAY 포털 Export 동기화 트리거.
 * <ul>
 *   <li>{@link #BASIC_MIDNIGHT} — 매일 00:00 기본 동기화(스케줄과 별도), 어제·오늘 2일</li>
 *   <li>{@link #SCHEDULED} — 전산설정 주기 자동(당일 1회=2일, 2회~=당일)</li>
 *   <li>{@link #MANUAL} — [JPAY 동기화] 수동(항상 당일만)</li>
 *   <li>{@link #FULL_RESYNC} — [전체 재동기화] 수동(초기화 개월 전체 구간)</li>
 *   <li>{@link #EXPLICIT_RANGE} — 수동·조회 시 기간 지정</li>
 *   <li>{@link #INITIAL_BOOTSTRAP} — 캐시 비어 있을 때 초기 적재</li>
 * </ul>
 */
public enum JpaySyncTrigger {
    BASIC_MIDNIGHT,
    SCHEDULED,
    MANUAL,
    FULL_RESYNC,
    EXPLICIT_RANGE,
    INITIAL_BOOTSTRAP
}
