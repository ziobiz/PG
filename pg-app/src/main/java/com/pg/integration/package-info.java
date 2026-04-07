/**
 * 결제대행사(PG) 연동 확장을 위한 패키지.
 * <p>
 * <b>역할</b>
 * <ul>
 *   <li>{@link com.pg.integration.pg.PgVendor} — DB·화면에 쓰는 PG 벤더 코드 상수와 계열 판별(ChillPay 등).</li>
 *   <li>{@link com.pg.integration.pg.notify.PgNotifyInboundTxnHandler} — 노티 수신 후 {@code pg_trnsctn} 적재 등
 *       PG별 후처리. 신규 PG는 구현체를 추가하고 스프링 빈으로 등록합니다.</li>
 *   <li>{@link com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher} — 등록된 핸들러를 순서대로 호출합니다.</li>
 * </ul>
 * <p>
 * <b>신규 PG 추가 절차(요약)</b>
 * <ol>
 *   <li>{@code PgVendor}에 공개 코드 상수·필요 시 {@code pg_cd} 접두 판별 메서드 추가.</li>
 *   <li>결제·노티 전용 서비스는 {@code com.pg.service} 등 기존 패키지에 두되, 노티→거래 공통 진입은
 *       {@link com.pg.integration.pg.notify.PgNotifyInboundTxnHandler} 구현으로 연결.</li>
 *   <li>본사 배포 레지스트리·매핑 UI({@code HqNotifyMappingService} 등)에 벤더 메타 추가.</li>
 * </ol>
 */
package com.pg.integration;
