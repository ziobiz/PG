/**
 * ICOPAY <b>URL 결제 공통 플랫폼</b> — PG사·결제대행사와 무관한 본사 설정·공개 결제 UX.
 *
 * <h2>원칙</h2>
 * <ul>
 *   <li><b>플랫폼 공통(모든 PG)</b>: 본사 URL결제설정(폼 FULL/SIMPLE, INLINE/REDIRECT), 표시통화→실결제(FX),
 *       결제통화 로직(×100 등), 결제문구(다국어), 공개 결제 셸 JS({@code site/js/url-pay-public-shell.js}),
 *       금액 해석({@link com.pg.service.UrlPayChargeResolutionService}).</li>
 *   <li><b>PG 어댑터(벤더별)</b>: 인라인 카드 UI 종류, 결제 페이지 경로, 승인 API, 재결제 URL 가능 여부.</li>
 * </ul>
 *
 * <h2>신규 PG 추가 체크리스트</h2>
 * <ol>
 *   <li>{@link com.pg.integration.pg.PgVendor} 에 계열 상수·{@code isXxxFamily(pgCd)} 추가.</li>
 *   <li>{@link UrlPayVendorCapabilityRegistry} 에 {@code pg_cd} 분기 — {@code inlineWidgetKind},
 *       {@link UrlPaySaleChannel}, {@code checkoutPagePath}, {@code repayUrlEnabled}(PG API에 재결제가 있을 때만).</li>
 *   <li>필요 시 {@link UrlPayCheckoutContextEnricher} 구현(벤더 전용 checkout 필드).</li>
 *   <li>승인 API: {@link UrlPaySaleDispatcher} 에 {@link UrlPaySaleChannel} case 추가 또는 ChillPay형 CCD 연동.</li>
 *   <li>정적 결제 페이지: {@code site/} 또는 기존 셸 재사용 + {@link com.pg.controller.PayPageController} 경로.</li>
 *   <li>{@code tb_pg_agency}: 연동용도 URL결제·URL재결제 플래그.</li>
 * </ol>
 *
 * <p>상세: {@code docs/URL결제_공통플랫폼_가이드.md}
 */
package com.pg.urlpay;
