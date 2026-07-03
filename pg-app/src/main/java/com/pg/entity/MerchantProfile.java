package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 가맹점/영업조직 상세 정보 (업체관리 등록 화면의 상세 필드 보관)
 */
@Entity
@Table(name = "tb_merchant_profile")
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OrgUnit.id (총본사/지사/대리점/가맹점 공통) */
    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_div", length = 20)
    private String compDiv;

    @Column(name = "tel", length = 50)
    private String compTel;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "addr", length = 255)
    private String addr;

    @Column(name = "addr_detail", length = 255)
    private String addrDetail;

    @Column(name = "addr_etc", length = 255)
    private String addrEtc;

    @Column(name = "ceo_nm", length = 100)
    private String ceoNm;

    @Column(name = "ceo_mobile", length = 50)
    private String ceoMobile;

    @Column(name = "use_yn", length = 1)
    /** Y=사용, N=미사용(로그인 가능·서비스 중단), S=영구정지(로그인 불가) */
    private String useYn;

    /**
     * 상위 조직(총판·본사 등) 미사용/정지에 따라 함께 미사용 처리된 하위 조직 표시.
     * Y=상위연쇄로 미사용됨(상위가 다시 사용되면 자동 복원 대상).
     * N=직접(개별) 설정 — 상위 복원과 무관하게 현재 상태 유지.
     */
    @Column(name = "parent_cascade_disabled_yn", length = 1, nullable = false)
    private String parentCascadeDisabledYn = "N";

    @Column(name = "login_id", length = 50)
    private String loginId;

    @Column(name = "reg_no", length = 50)
    private String regNo;

    /** 업태 (사업자등록증) */
    @Column(name = "biz_type", length = 100)
    private String bizType;

    /** 종목 (사업자등록증) */
    @Column(name = "industry", length = 100)
    private String industry;

    /** 사업자형태 (가맹점 전용) */
    @Column(name = "biz_nature", length = 100)
    private String bizNature;

    /** 취급물품 (가맹점 전용) */
    @Column(name = "product", length = 100)
    private String product;

    /** 대표사이트 (가맹점 전용) */
    @Column(name = "homepage", length = 255)
    private String homepage;

    /** 사이트 주소 */
    @Column(name = "site_url", length = 255)
    private String siteUrl;

    /** 사이트개요 */
    @Column(name = "site_summary", length = 500)
    private String siteSummary;

    /** 정산담당자명 (가맹점 전용) */
    @Column(name = "settle_name", length = 100)
    private String settleName;

    /** 정산담당자연락처 (가맹점 전용) */
    @Column(name = "settle_tel_no", length = 50)
    private String settleTelNo;

    /** 정산형태 (총판/지사/대리점): M=가맹점별정산, G=총판정산 */
    @Column(name = "settle_type", length = 5)
    private String settleType;

    /** 요율 (총판/지사/대리점) */
    @Column(name = "commission_rate", precision = 10, scale = 4)
    private java.math.BigDecimal commissionRate;

    /** 사용한도 (총판/지사/대리점) */
    @Column(name = "limit_amt", precision = 18, scale = 0)
    private java.math.BigDecimal limitAmt;

    @Column(name = "fax", length = 50)
    private String fax;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "pwd", length = 200)
    private String pwd;

    @Column(name = "bank_cd", length = 20)
    private String bankCd;

    @Column(name = "transfer_fee", length = 50)
    private String transferFee;

    @Column(name = "crypto_transfer_fee", length = 50)
    private String cryptoTransferFee;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "country_cd", length = 10)
    private String countryCd;

    /** 주소 국가 (기본정보) JP/KR/TH 또는 기타 시 국가명 */
    @Column(name = "addr_country_cd", length = 20)
    private String addrCountryCd;

    @Column(name = "swift", length = 50)
    private String swift;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "branch_addr", length = 255)
    private String branchAddr;

    @Column(name = "contact_tel", length = 50)
    private String contactTel;

    @Column(name = "wallet_address", length = 255)
    private String walletAddress;

    @Column(name = "network_name", length = 50)
    private String networkName;

    @Column(name = "remark", length = 500)
    private String remark;

    /** 수수료 설정 권한 (총본사가 본사/총판에 부여) Y/N */
    @Column(name = "commission_config_allowed", length = 1)
    private String commissionConfigAllowed = "N";

    /** 웹결제 사용여부 (가맹점) - 미사용 시 WEB 결제 시스템 중지 */
    @Column(name = "web_payment_use_yn", length = 1)
    private String webPaymentUseYn = "Y";

    /**
     * 웹결제(URL·JPAY) 결제창 상단 로고 — {@link com.pg.urlpay.WebPaymentHeaderLogoModeUtil}.
     */
    @Column(name = "web_payment_header_logo_mode", nullable = false, length = 16)
    private String webPaymentHeaderLogoMode = "DEFAULT";

    /** 웹결제 상단 로고 URL — mode=ACTIVE 일 때 가맹 업로드 */
    @Column(name = "web_payment_header_logo_url", length = 500)
    private String webPaymentHeaderLogoUrl;

    /** 웹결제 HTML 로고 모드 상단 표시명 — mode=HTML 일 때 결제창 상단 문구(최대 20자) */
    @Column(name = "web_payment_header_html_title", length = 20)
    private String webPaymentHeaderHtmlTitle;

    /**
     * 웹결제 결제창 로고 아래 경고/안내 문구 — {@link com.pg.urlpay.WebPaymentHeaderLogoModeUtil}.
     */
    @Column(name = "web_payment_header_subtitle_mode", nullable = false, length = 16)
    private String webPaymentHeaderSubtitleMode = "DEFAULT";

    /** 웹결제 상단 경고문구 — mode=ACTIVE 일 때 가맹 입력 */
    @Column(name = "web_payment_header_subtitle_text", length = 200)
    private String webPaymentHeaderSubtitleText;

    /**
     * 공개 URL 결제 방식 — {@link com.pg.urlpay.UrlPayCheckoutModeUtil}.
     * STANDARD=일반 URL, REPAY=저장 카드 재결제 URL.
     */
    @Column(name = "url_pay_checkout_mode", nullable = false, length = 16)
    private String urlPayCheckoutMode = "STANDARD";

    /** URL 결제창 상품명 입력·표시 — Y=사용, N=미사용(결제창에서 숨김) */
    @Column(name = "url_pay_product_name_use_yn", nullable = false, length = 1)
    private String urlPayProductNameUseYn = "Y";

    /** URL·공개 결제창(jpay-pay.html 등) — 가맹점명(Merchant) 행 표시 여부. N=비활성(화면만 숨김, 결제 전문은 유지) */
    @Column(name = "url_pay_company_name_show_yn", nullable = false, length = 1)
    private String urlPayCompanyNameShowYn = "Y";

    /** URL 결제창 다국어 변경 메뉴 — Y=표시, N=브라우저 언어 자동(메뉴 숨김) */
    @Column(name = "url_pay_lang_menu_use_yn", nullable = false, length = 1)
    private String urlPayLangMenuUseYn = "Y";

    /** 결제창 연락처 자동기억 — FOLLOW_HQ | Y | N */
    @Column(name = "checkout_contact_remember_mode", nullable = false, length = 16)
    private String checkoutContactRememberMode = "FOLLOW_HQ";

    /** URL·JPAY 공개 결제창 배송 주소 입력 — Y=표시·필수(FULL·1형), N=미표시(기본) */
    @Column(name = "url_pay_shipping_address_use_yn", nullable = false, length = 1)
    private String urlPayShippingAddressUseYn = "N";

    /** URL 공개 결제창 입력방식 — {@link com.pg.urlpay.UrlPayInputModeUtil} */
    @Column(name = "url_pay_input_mode", nullable = false, length = 16)
    private String urlPayInputMode = "GENERAL";

    /** JPAY URL 결제창 카드 유효기간 입력 — {@link com.pg.urlpay.UrlPayCardExpiryModeUtil} */
    @Column(name = "url_pay_card_expiry_mode", nullable = false, length = 16)
    private String urlPayCardExpiryMode = "FOLLOW_HQ";

    /**
     * API URL 인라인 중계 결제 방식 — {@link com.pg.urlpay.UrlPayCheckoutModeUtil}.
     */
    @Column(name = "api_url_pay_checkout_mode", nullable = false, length = 16)
    private String apiUrlPayCheckoutMode = "STANDARD";

    /** JPAY 가맹 API 구독(정기) 인라인 사용 — 본사 jpaySubscriptionEnabledYn 과 함께 */
    @Column(name = "api_jpay_subscription_use_yn", nullable = false, length = 1)
    private String apiJpaySubscriptionUseYn = "N";

    /** 가맹 API 인라인(embed) 연동 — 본사 apiBrokerInlineEnabledYn·urlPayInlineEnabledYn 과 함께 */
    @Column(name = "api_broker_inline_use_yn", nullable = false, length = 1)
    private String apiBrokerInlineUseYn = "Y";

    /** 가맹 API 리다이렉트 연동 — 본사 apiBrokerRedirectEnabledYn·urlPayRedirectEnabledYn 과 함께 */
    @Column(name = "api_broker_redirect_use_yn", nullable = false, length = 1)
    private String apiBrokerRedirectUseYn = "N";

    /** WordPress/WooCommerce 플러그인 연동 — 본사 apiWordpressPluginEnabledYn 과 함께 */
    @Column(name = "api_wordpress_use_yn", nullable = false, length = 1)
    private String apiWordpressUseYn = "N";

    /** 모바일·embed 결제창 오버라이드 — NULL=본사 mobileCheckoutModeDefault */
    @Column(name = "mobile_checkout_mode", length = 32)
    private String mobileCheckoutMode;

    /**
     * JPAY URL 결제창(jpay-pay.html) 입력 필드 가맹 오버라이드 —
     * {@link com.pg.urlpay.JpayCheckoutFieldModeUtil}. NULL/빈값이면 본사 기본값을 따름.
     */
    @Column(name = "jpay_checkout_field_mode", length = 20)
    private String jpayCheckoutFieldMode;

    /** JPAY 결제창 전화 국가번호 드롭다운 가맹 오버라이드 — NULL=본사 기본. */
    @Column(name = "jpay_phone_dial_code_yn", length = 1)
    private String jpayPhoneDialCodeYn;

    /**
     * 챗봇 결제 URL 방식 — {@link com.pg.urlpay.UrlPayCheckoutModeUtil}.
     * 웹결제(공개 URL)·API 중계와 별도 선택.
     */
    @Column(name = "chatbot_url_pay_checkout_mode", nullable = false, length = 16)
    private String chatbotUrlPayCheckoutMode = "STANDARD";

    /** 챗봇결제 사용여부 (가맹점) - 미사용 시 관리자 챗봇 메뉴 비표시 */
    @Column(name = "chatbot_payment_use_yn", length = 1)
    private String chatbotPaymentUseYn = "N";

    /**
     * 상위 조직(본사·총판 등) 운영 보류. 챗봇결제=Y 이어도 공개 고객 챗봇에서 상품·주문·예약·결제는 막고 일반 문의만 허용.
     */
    @Column(name = "chatbot_commerce_hold_yn", length = 1, nullable = false)
    private String chatbotCommerceHoldYn = "N";

    /** 챗봇 카탈로그 등록 가능 상품 수(10·20·50·80·100·150·200). 월 이용료·등록 건수 제한 */
    @Column(name = "chatbot_product_slot_limit")
    private Integer chatbotProductSlotLimit;

    /**
     * 다음 플랜(예약): 상향·하향 모두 익월(서울 달력) 적용 시 본 값을 {@link #chatbotProductSlotLimit} 로 이관.
     * 당월에는 {@link #chatbotProductSlotLimit} 이 유효하며, {@link #chatbotProductSlotPendingApplyYm} 에 도달하면 이관.
     */
    @Column(name = "chatbot_product_slot_limit_pending")
    private Integer chatbotProductSlotLimitPending;

    /** {@code chatbot_product_slot_limit_pending} 적용 시작 월(YYYY-MM, Asia/Seoul) */
    @Column(name = "chatbot_product_slot_pending_apply_ym", length = 7)
    private String chatbotProductSlotPendingApplyYm;

    /** 고객 대면 챗봇 안내 — 회사명(비우면 조직명) */
    @Column(name = "chatbot_kb_company_nm", length = 200)
    private String chatbotKbCompanyNm;

    /** 고객 대면 챗봇 안내 — 주소 */
    @Column(name = "chatbot_kb_addr", length = 600)
    private String chatbotKbAddr;

    @Column(name = "chatbot_kb_tel", length = 100)
    private String chatbotKbTel;

    @Column(name = "chatbot_kb_email", length = 120)
    private String chatbotKbEmail;

    @Column(name = "chatbot_kb_contact_nm", length = 100)
    private String chatbotKbContactNm;

    @Column(name = "chatbot_kb_intro", columnDefinition = "TEXT")
    private String chatbotKbIntro;

    @Column(name = "chatbot_kb_product_desc", columnDefinition = "TEXT")
    private String chatbotKbProductDesc;

    /** 공개 챗봇 첫 진입 상단 안내 문구. 비우면 서버·클라이언트 기본 문구 사용 */
    @Column(name = "chatbot_kb_welcome_hint", columnDefinition = "TEXT")
    private String chatbotKbWelcomeHint;

    /**
     * 챗봇관리 기본설정 — 운영방식 코드.
     * {@link com.pg.chatbot.ChatbotOperationMode} 참조. 미설정 시 응답·LLM 에서는 SALE_PREPAID 로 간주.
     */
    @Column(name = "chatbot_operation_mode", length = 40)
    private String chatbotOperationMode;

    /**
     * 챗봇관리 기본설정 — 가맹점 업체성격(운영방식과 별개: 주문·예약 질문 흐름).
     * {@link com.pg.chatbot.ChatbotMerchantVertical} 코드.
     */
    @Column(name = "chatbot_merchant_vertical", nullable = false, length = 40)
    private String chatbotMerchantVertical = "GENERAL_SALE";

    /** 본사·총판: 업체성격 보조 메모(필수 질문 키워드 등). LLM 반영 */
    @Column(name = "chatbot_merchant_vertical_notes", columnDefinition = "TEXT")
    private String chatbotMerchantVerticalNotes;

    /**
     * 챗봇 고객 주문·예약 시트(필드 표시·라벨·숨김 등) 가맹별 JSON.
     * 비우면 {@link com.pg.chatbot.ChatbotOrderSheetUiResolver} 가 업체성격 기본만 적용합니다.
     */
    @Column(name = "chatbot_order_sheet_ui_json", columnDefinition = "TEXT")
    private String chatbotOrderSheetUiJson;

    /** 예약 상품 시 겹침 방지용 기본 슬롯 길이(분). 기본 60 */
    @Column(name = "chatbot_reservation_slot_minutes", nullable = false)
    private Integer chatbotReservationSlotMinutes = 60;

    /** 예약 시작 시각 해석용 타임존(예: Asia/Seoul) */
    @Column(name = "chatbot_reservation_zone_id", nullable = false, length = 64)
    private String chatbotReservationZoneId = "Asia/Seoul";

    /** 공개 챗봇 결제 페이지 상단 로고(URL). 미설정 시 상위 본사·총판 브랜딩 로고 사용 */
    @Column(name = "chatbot_header_logo_url", length = 500)
    private String chatbotHeaderLogoUrl;

    /** 챗봇에서 상품 등록 허용 관리자(tb_user.id), 가맹당 1명 */
    @Column(name = "chatbot_admin_user_id")
    private Long chatbotAdminUserId;

    /** 산하 가맹 허용 카탈로그 유형(SALE,RESERVATION_TIME,RESERVATION_PLACE) 교집합. 비우면 해당 조직 단계에서 추가 제한 없음 */
    @Column(name = "chatbot_catalog_listing_grant", length = 160)
    private String chatbotCatalogListingGrant;

    /** 가맹: 실제 사용할 유형. 비우면 상위 교집합 전부 */
    @Column(name = "chatbot_catalog_listing_enabled", length = 160)
    private String chatbotCatalogListingEnabled;

    /** 산하 상품 이미지 최대 장수(1~4) 상한. 실효=체인 최소, 미설정은 최종 1 */
    @Column(name = "chatbot_max_product_images_grant")
    private Integer chatbotMaxProductImagesGrant;

    /**
     * 챗봇-pay 상단 프로모션 인텔리전트 모드.
     * {@link com.pg.chatbot.ChatbotPromotionShelfMode} 코드 저장.
     */
    @Column(name = "chatbot_promotion_shelf_mode", nullable = false, length = 16)
    private String chatbotPromotionShelfMode = "PROMOTION";

    /** DYNAMIC·HYBRID 순환 주기(초). 30초 단위, 기본 30 */
    @Column(name = "chatbot_promotion_rotate_seconds", nullable = false)
    private Integer chatbotPromotionRotateSeconds = 30;

    /** URL·챗봇 인라인(DirectCredit) 승인 시 가맹점 대표 이메일로 알림 */
    @Column(name = "url_pay_alert_email_yn", length = 1)
    private String urlPayAlertEmailYn = "N";

    /** LINE Notify(https://notify-bot.line.me/) 발급 토큰. 비면 미사용 */
    @Column(name = "url_pay_line_notify_token", length = 256)
    private String urlPayLineNotifyToken;

    /** URL 분할결제 사용 Y/N */
    @Column(name = "split_pay_enabled_yn", length = 1, nullable = false)
    private String splitPayEnabledYn = "N";

    /** 분할결제 — 월 단위 간격 허용 */
    @Column(name = "split_pay_interval_month_yn", length = 1, nullable = false)
    private String splitPayIntervalMonthYn = "Y";

    /** 분할결제 — 일 단위 간격 허용 */
    @Column(name = "split_pay_interval_day_yn", length = 1, nullable = false)
    private String splitPayIntervalDayYn = "N";

    /** 일 단위 간격 기본 일수 (예: 10일 간격) */
    @Column(name = "split_pay_day_interval_days", nullable = false)
    private Integer splitPayDayIntervalDays = 10;

    /** 월 단위 간격 기본 개월 수 (예: 1=매월, 2=2개월마다) */
    @Column(name = "split_pay_month_interval_months", nullable = false)
    private Integer splitPayMonthIntervalMonths = 1;

    /** 1회차: IMMEDIATE=즉시결제, LINK=링크만 */
    @Column(name = "split_pay_first_pay_mode", length = 16, nullable = false)
    private String splitPayFirstPayMode = "IMMEDIATE";

    /** 분할결제 — 멀티(고객 선택 개월) 모드 */
    @Column(name = "split_pay_interval_multi_yn", length = 1, nullable = false)
    private String splitPayIntervalMultiYn = "N";

    /** 멀티 모드 최대 선택 개월 (3·5·6·12) */
    @Column(name = "split_pay_multi_max_months", nullable = false)
    private Integer splitPayMultiMaxMonths = 6;

    /** URL 분할결제 결제창 상단 로고 — {@link com.pg.urlpay.WebPaymentHeaderLogoModeUtil} */
    @Column(name = "split_pay_header_logo_mode", nullable = false, length = 16)
    private String splitPayHeaderLogoMode = "HTML";

    /** URL 분할결제 상단 로고 URL — mode=ACTIVE 일 때 가맹 업로드 */
    @Column(name = "split_pay_header_logo_url", length = 500)
    private String splitPayHeaderLogoUrl;

    /** URL 분할결제 HTML 로고 모드 상단 표시명 — mode=HTML 일 때 결제창 상단 문구(최대 20자) */
    @Column(name = "split_pay_header_html_title", length = 20)
    private String splitPayHeaderHtmlTitle;

    /** URL 분할결제 결제창 로고 아래 안내문구 — {@link com.pg.urlpay.WebPaymentHeaderLogoModeUtil} */
    @Column(name = "split_pay_header_subtitle_mode", nullable = false, length = 16)
    private String splitPayHeaderSubtitleMode = "DEFAULT";

    /** URL 분할결제 상단 안내문구 — mode=ACTIVE 일 때 가맹 입력 */
    @Column(name = "split_pay_header_subtitle_text", length = 200)
    private String splitPayHeaderSubtitleText;

    /** URL 분할결제 결제창 다국어 변경 메뉴 — Y=표시, N=숨김 */
    @Column(name = "split_pay_lang_menu_use_yn", nullable = false, length = 1)
    private String splitPayLangMenuUseYn = "Y";

    /** 기준 화폐. 본사: 최대 3종 comma구분 (KRW,USD,JPY). 총판: 1종만 */
    @Column(name = "base_currency", length = 30)
    private String baseCurrency;

    /** 터미널[단말] 개수 */
    @Column(name = "terminal_count_terminal")
    private Integer terminalCountTerminal;

    /** 터미널[웹] 개수 */
    @Column(name = "terminal_count_web")
    private Integer terminalCountWeb;

    /** 본사(REGIONAL) 전용 JSON 설정 */
    @Column(name = "regional_settings", columnDefinition = "TEXT")
    private String regionalSettings;

    /** 가맹점 관리자 결제 후속조치 사용 (NULL=기존 호환 허용) */
    @Column(name = "pay_follow_merchant_use_yn", length = 1)
    private String payFollowMerchantUseYn;

    @Column(name = "pay_follow_auto_void_yn", length = 1)
    private String payFollowAutoVoidYn;

    @Column(name = "pay_follow_email_void_yn", length = 1)
    private String payFollowEmailVoidYn;

    @Column(name = "pay_follow_auto_refund_yn", length = 1)
    private String payFollowAutoRefundYn;

    @Column(name = "pay_follow_force_refund_yn", length = 1)
    private String payFollowForceRefundYn;

    /** FOLLOW_HQ | CUSTOM | DISABLED */
    @Column(name = "card_risk_policy_mode", length = 16)
    private String cardRiskPolicyMode = "DISABLED";

    @Column(name = "card_risk_tier1_hours")
    private Integer cardRiskTier1Hours;

    @Column(name = "card_risk_tier1_min")
    private Integer cardRiskTier1Min;

    @Column(name = "card_risk_tier2_hours")
    private Integer cardRiskTier2Hours;

    @Column(name = "card_risk_tier2_min")
    private Integer cardRiskTier2Min;

    @Column(name = "card_risk_tier3_hours")
    private Integer cardRiskTier3Hours;

    @Column(name = "card_risk_tier3_min")
    private Integer cardRiskTier3Min;

    @Column(name = "card_risk_tier4_hours")
    private Integer cardRiskTier4Hours;

    @Column(name = "card_risk_tier4_min")
    private Integer cardRiskTier4Min;

    @Column(name = "card_risk_auto_blacklist_tier")
    private Integer cardRiskAutoBlacklistTier;

    /** NONE | FOLLOW_HQ | CUSTOM — 추적기간 기간정책(가맹점) */
    @Column(name = "card_risk_track_period_policy", length = 16)
    private String cardRiskTrackPeriodPolicy;

    @Column(name = "card_risk_track_period_mode", length = 8)
    private String cardRiskTrackPeriodMode;

    @Column(name = "card_risk_track_period_value")
    private Integer cardRiskTrackPeriodValue;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompDiv() { return compDiv; }
    public void setCompDiv(String compDiv) { this.compDiv = compDiv; }
    public String getCompTel() { return compTel; }
    public void setCompTel(String compTel) { this.compTel = compTel; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public String getAddrDetail() { return addrDetail; }
    public void setAddrDetail(String addrDetail) { this.addrDetail = addrDetail; }
    public String getAddrEtc() { return addrEtc; }
    public void setAddrEtc(String addrEtc) { this.addrEtc = addrEtc; }
    public String getCeoNm() { return ceoNm; }
    public void setCeoNm(String ceoNm) { this.ceoNm = ceoNm; }
    public String getCeoMobile() { return ceoMobile; }
    public void setCeoMobile(String ceoMobile) { this.ceoMobile = ceoMobile; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getParentCascadeDisabledYn() { return parentCascadeDisabledYn; }
    public void setParentCascadeDisabledYn(String parentCascadeDisabledYn) {
        this.parentCascadeDisabledYn = parentCascadeDisabledYn != null
                && "Y".equalsIgnoreCase(parentCascadeDisabledYn.trim()) ? "Y" : "N";
    }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getBizNature() { return bizNature; }
    public void setBizNature(String bizNature) { this.bizNature = bizNature; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getSiteSummary() { return siteSummary; }
    public void setSiteSummary(String siteSummary) { this.siteSummary = siteSummary; }
    public String getSettleName() { return settleName; }
    public void setSettleName(String settleName) { this.settleName = settleName; }
    public String getSettleTelNo() { return settleTelNo; }
    public void setSettleTelNo(String settleTelNo) { this.settleTelNo = settleTelNo; }
    public String getSettleType() { return settleType; }
    public void setSettleType(String settleType) { this.settleType = settleType; }
    public java.math.BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(java.math.BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public java.math.BigDecimal getLimitAmt() { return limitAmt; }
    public void setLimitAmt(java.math.BigDecimal limitAmt) { this.limitAmt = limitAmt; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public String getBankCd() { return bankCd; }
    public void setBankCd(String bankCd) { this.bankCd = bankCd; }
    public String getTransferFee() { return transferFee; }
    public void setTransferFee(String transferFee) { this.transferFee = transferFee; }
    public String getCryptoTransferFee() { return cryptoTransferFee; }
    public void setCryptoTransferFee(String cryptoTransferFee) { this.cryptoTransferFee = cryptoTransferFee; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getCountryCd() { return countryCd; }
    public void setCountryCd(String countryCd) { this.countryCd = countryCd; }
    public String getAddrCountryCd() { return addrCountryCd; }
    public void setAddrCountryCd(String addrCountryCd) { this.addrCountryCd = addrCountryCd; }
    public String getSwift() { return swift; }
    public void setSwift(String swift) { this.swift = swift; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getBranchAddr() { return branchAddr; }
    public void setBranchAddr(String branchAddr) { this.branchAddr = branchAddr; }
    public String getContactTel() { return contactTel; }
    public void setContactTel(String contactTel) { this.contactTel = contactTel; }
    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
    public String getNetworkName() { return networkName; }
    public void setNetworkName(String networkName) { this.networkName = networkName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCommissionConfigAllowed() { return commissionConfigAllowed; }
    public void setCommissionConfigAllowed(String commissionConfigAllowed) { this.commissionConfigAllowed = commissionConfigAllowed; }
    public String getWebPaymentUseYn() { return webPaymentUseYn; }
    public void setWebPaymentUseYn(String webPaymentUseYn) { this.webPaymentUseYn = webPaymentUseYn; }
    public String getWebPaymentHeaderLogoMode() { return webPaymentHeaderLogoMode; }
    public void setWebPaymentHeaderLogoMode(String webPaymentHeaderLogoMode) { this.webPaymentHeaderLogoMode = webPaymentHeaderLogoMode; }
    public String getWebPaymentHeaderLogoUrl() { return webPaymentHeaderLogoUrl; }
    public void setWebPaymentHeaderLogoUrl(String webPaymentHeaderLogoUrl) { this.webPaymentHeaderLogoUrl = webPaymentHeaderLogoUrl; }
    public String getWebPaymentHeaderHtmlTitle() { return webPaymentHeaderHtmlTitle; }
    public void setWebPaymentHeaderHtmlTitle(String webPaymentHeaderHtmlTitle) { this.webPaymentHeaderHtmlTitle = webPaymentHeaderHtmlTitle; }
    public String getWebPaymentHeaderSubtitleMode() { return webPaymentHeaderSubtitleMode; }
    public void setWebPaymentHeaderSubtitleMode(String webPaymentHeaderSubtitleMode) { this.webPaymentHeaderSubtitleMode = webPaymentHeaderSubtitleMode; }
    public String getWebPaymentHeaderSubtitleText() { return webPaymentHeaderSubtitleText; }
    public void setWebPaymentHeaderSubtitleText(String webPaymentHeaderSubtitleText) { this.webPaymentHeaderSubtitleText = webPaymentHeaderSubtitleText; }
    public String getUrlPayCheckoutMode() { return urlPayCheckoutMode; }
    public void setUrlPayCheckoutMode(String urlPayCheckoutMode) {
        this.urlPayCheckoutMode = urlPayCheckoutMode != null && !urlPayCheckoutMode.isBlank()
                ? urlPayCheckoutMode.trim().toUpperCase(java.util.Locale.ROOT) : "STANDARD";
    }

    public String getUrlPayProductNameUseYn() { return urlPayProductNameUseYn; }
    public void setUrlPayProductNameUseYn(String urlPayProductNameUseYn) {
        this.urlPayProductNameUseYn = urlPayProductNameUseYn != null && "Y".equalsIgnoreCase(urlPayProductNameUseYn.trim()) ? "Y" : "N";
    }

    public String getUrlPayCompanyNameShowYn() { return urlPayCompanyNameShowYn; }
    public void setUrlPayCompanyNameShowYn(String urlPayCompanyNameShowYn) {
        this.urlPayCompanyNameShowYn = urlPayCompanyNameShowYn != null && "Y".equalsIgnoreCase(urlPayCompanyNameShowYn.trim()) ? "Y" : "N";
    }

    public String getUrlPayLangMenuUseYn() { return urlPayLangMenuUseYn; }
    public void setUrlPayLangMenuUseYn(String urlPayLangMenuUseYn) {
        this.urlPayLangMenuUseYn = urlPayLangMenuUseYn != null && "Y".equalsIgnoreCase(urlPayLangMenuUseYn.trim()) ? "Y" : "N";
    }
    public String getCheckoutContactRememberMode() { return checkoutContactRememberMode; }
    public void setCheckoutContactRememberMode(String checkoutContactRememberMode) {
        if (checkoutContactRememberMode == null || checkoutContactRememberMode.isBlank()) {
            this.checkoutContactRememberMode = "FOLLOW_HQ";
            return;
        }
        String u = checkoutContactRememberMode.trim().toUpperCase(java.util.Locale.ROOT);
        this.checkoutContactRememberMode = ("Y".equals(u) || "N".equals(u)) ? u : "FOLLOW_HQ";
    }

    public String getUrlPayShippingAddressUseYn() { return urlPayShippingAddressUseYn; }
    public void setUrlPayShippingAddressUseYn(String urlPayShippingAddressUseYn) {
        this.urlPayShippingAddressUseYn = urlPayShippingAddressUseYn != null
                && "Y".equalsIgnoreCase(urlPayShippingAddressUseYn.trim()) ? "Y" : "N";
    }

    public String getUrlPayInputMode() { return urlPayInputMode; }
    public void setUrlPayInputMode(String urlPayInputMode) {
        this.urlPayInputMode = com.pg.urlpay.UrlPayInputModeUtil.normalizeMerchantStored(urlPayInputMode);
    }
    public String getUrlPayCardExpiryMode() { return urlPayCardExpiryMode; }
    public void setUrlPayCardExpiryMode(String urlPayCardExpiryMode) {
        this.urlPayCardExpiryMode = com.pg.urlpay.UrlPayCardExpiryModeUtil.normalizeMerchantStored(urlPayCardExpiryMode);
    }

    public String getApiUrlPayCheckoutMode() { return apiUrlPayCheckoutMode; }
    public void setApiUrlPayCheckoutMode(String apiUrlPayCheckoutMode) {
        this.apiUrlPayCheckoutMode = com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(apiUrlPayCheckoutMode);
    }
    public String getApiJpaySubscriptionUseYn() { return apiJpaySubscriptionUseYn; }
    public void setApiJpaySubscriptionUseYn(String apiJpaySubscriptionUseYn) {
        this.apiJpaySubscriptionUseYn = apiJpaySubscriptionUseYn != null && "Y".equalsIgnoreCase(apiJpaySubscriptionUseYn.trim()) ? "Y" : "N";
    }

    public String getApiBrokerInlineUseYn() { return apiBrokerInlineUseYn; }
    public void setApiBrokerInlineUseYn(String apiBrokerInlineUseYn) {
        this.apiBrokerInlineUseYn = apiBrokerInlineUseYn != null && "Y".equalsIgnoreCase(apiBrokerInlineUseYn.trim()) ? "Y" : "N";
    }

    public String getApiBrokerRedirectUseYn() { return apiBrokerRedirectUseYn; }
    public void setApiBrokerRedirectUseYn(String apiBrokerRedirectUseYn) {
        this.apiBrokerRedirectUseYn = apiBrokerRedirectUseYn != null && "Y".equalsIgnoreCase(apiBrokerRedirectUseYn.trim()) ? "Y" : "N";
    }

    public String getApiWordpressUseYn() { return apiWordpressUseYn; }
    public void setApiWordpressUseYn(String apiWordpressUseYn) {
        this.apiWordpressUseYn = apiWordpressUseYn != null && "Y".equalsIgnoreCase(apiWordpressUseYn.trim()) ? "Y" : "N";
    }

    public String getMobileCheckoutMode() { return mobileCheckoutMode; }
    public void setMobileCheckoutMode(String mobileCheckoutMode) {
        if (mobileCheckoutMode == null || mobileCheckoutMode.isBlank()) {
            this.mobileCheckoutMode = null;
            return;
        }
        this.mobileCheckoutMode = com.pg.urlpay.MobileCheckoutModeUtil.normalizeMerchantOverride(mobileCheckoutMode);
    }

    public String getJpayCheckoutFieldMode() { return jpayCheckoutFieldMode; }
    public void setJpayCheckoutFieldMode(String jpayCheckoutFieldMode) {
        this.jpayCheckoutFieldMode = com.pg.urlpay.JpayCheckoutFieldModeUtil.normalizeMerchantOverride(jpayCheckoutFieldMode);
    }
    public String getJpayPhoneDialCodeYn() { return jpayPhoneDialCodeYn; }
    public void setJpayPhoneDialCodeYn(String jpayPhoneDialCodeYn) {
        this.jpayPhoneDialCodeYn = com.pg.urlpay.JpayPhoneDialCodeUtil.normalizeMerchantOverride(jpayPhoneDialCodeYn);
    }

    public String getChatbotUrlPayCheckoutMode() { return chatbotUrlPayCheckoutMode; }
    public void setChatbotUrlPayCheckoutMode(String chatbotUrlPayCheckoutMode) {
        this.chatbotUrlPayCheckoutMode = com.pg.urlpay.UrlPayCheckoutModeUtil.normalize(chatbotUrlPayCheckoutMode);
    }
    public String getChatbotPaymentUseYn() { return chatbotPaymentUseYn; }
    public void setChatbotPaymentUseYn(String chatbotPaymentUseYn) { this.chatbotPaymentUseYn = chatbotPaymentUseYn; }

    public String getChatbotCommerceHoldYn() {
        return chatbotCommerceHoldYn;
    }

    public void setChatbotCommerceHoldYn(String chatbotCommerceHoldYn) {
        this.chatbotCommerceHoldYn = chatbotCommerceHoldYn != null ? chatbotCommerceHoldYn : "N";
    }

    public Integer getChatbotProductSlotLimit() { return chatbotProductSlotLimit; }
    public void setChatbotProductSlotLimit(Integer chatbotProductSlotLimit) {
        this.chatbotProductSlotLimit = chatbotProductSlotLimit;
    }

    public Integer getChatbotProductSlotLimitPending() {
        return chatbotProductSlotLimitPending;
    }

    public void setChatbotProductSlotLimitPending(Integer chatbotProductSlotLimitPending) {
        this.chatbotProductSlotLimitPending = chatbotProductSlotLimitPending;
    }

    public String getChatbotProductSlotPendingApplyYm() {
        return chatbotProductSlotPendingApplyYm;
    }

    public void setChatbotProductSlotPendingApplyYm(String chatbotProductSlotPendingApplyYm) {
        this.chatbotProductSlotPendingApplyYm = chatbotProductSlotPendingApplyYm;
    }

    public String getChatbotKbCompanyNm() { return chatbotKbCompanyNm; }
    public void setChatbotKbCompanyNm(String chatbotKbCompanyNm) { this.chatbotKbCompanyNm = chatbotKbCompanyNm; }
    public String getChatbotKbAddr() { return chatbotKbAddr; }
    public void setChatbotKbAddr(String chatbotKbAddr) { this.chatbotKbAddr = chatbotKbAddr; }
    public String getChatbotKbTel() { return chatbotKbTel; }
    public void setChatbotKbTel(String chatbotKbTel) { this.chatbotKbTel = chatbotKbTel; }
    public String getChatbotKbEmail() { return chatbotKbEmail; }
    public void setChatbotKbEmail(String chatbotKbEmail) { this.chatbotKbEmail = chatbotKbEmail; }
    public String getChatbotKbContactNm() { return chatbotKbContactNm; }
    public void setChatbotKbContactNm(String chatbotKbContactNm) { this.chatbotKbContactNm = chatbotKbContactNm; }
    public String getChatbotKbIntro() { return chatbotKbIntro; }
    public void setChatbotKbIntro(String chatbotKbIntro) { this.chatbotKbIntro = chatbotKbIntro; }
    public String getChatbotKbProductDesc() { return chatbotKbProductDesc; }
    public void setChatbotKbProductDesc(String chatbotKbProductDesc) { this.chatbotKbProductDesc = chatbotKbProductDesc; }
    public String getChatbotKbWelcomeHint() { return chatbotKbWelcomeHint; }
    public void setChatbotKbWelcomeHint(String chatbotKbWelcomeHint) { this.chatbotKbWelcomeHint = chatbotKbWelcomeHint; }
    public String getChatbotOperationMode() { return chatbotOperationMode; }
    public void setChatbotOperationMode(String chatbotOperationMode) { this.chatbotOperationMode = chatbotOperationMode; }

    public String getChatbotMerchantVertical() {
        return chatbotMerchantVertical;
    }

    public void setChatbotMerchantVertical(String chatbotMerchantVertical) {
        this.chatbotMerchantVertical = chatbotMerchantVertical != null ? chatbotMerchantVertical : "GENERAL_SALE";
    }

    public String getChatbotMerchantVerticalNotes() {
        return chatbotMerchantVerticalNotes;
    }

    public void setChatbotMerchantVerticalNotes(String chatbotMerchantVerticalNotes) {
        this.chatbotMerchantVerticalNotes = chatbotMerchantVerticalNotes;
    }

    public String getChatbotOrderSheetUiJson() {
        return chatbotOrderSheetUiJson;
    }

    public void setChatbotOrderSheetUiJson(String chatbotOrderSheetUiJson) {
        this.chatbotOrderSheetUiJson = chatbotOrderSheetUiJson;
    }

    public Integer getChatbotReservationSlotMinutes() {
        return chatbotReservationSlotMinutes;
    }

    public void setChatbotReservationSlotMinutes(Integer chatbotReservationSlotMinutes) {
        this.chatbotReservationSlotMinutes = chatbotReservationSlotMinutes;
    }

    public String getChatbotReservationZoneId() {
        return chatbotReservationZoneId;
    }

    public void setChatbotReservationZoneId(String chatbotReservationZoneId) {
        this.chatbotReservationZoneId = chatbotReservationZoneId;
    }

    public String getChatbotHeaderLogoUrl() { return chatbotHeaderLogoUrl; }
    public void setChatbotHeaderLogoUrl(String chatbotHeaderLogoUrl) { this.chatbotHeaderLogoUrl = chatbotHeaderLogoUrl; }
    public Long getChatbotAdminUserId() { return chatbotAdminUserId; }
    public void setChatbotAdminUserId(Long chatbotAdminUserId) { this.chatbotAdminUserId = chatbotAdminUserId; }

    public String getChatbotCatalogListingGrant() {
        return chatbotCatalogListingGrant;
    }

    public void setChatbotCatalogListingGrant(String chatbotCatalogListingGrant) {
        this.chatbotCatalogListingGrant = chatbotCatalogListingGrant;
    }

    public String getChatbotCatalogListingEnabled() {
        return chatbotCatalogListingEnabled;
    }

    public void setChatbotCatalogListingEnabled(String chatbotCatalogListingEnabled) {
        this.chatbotCatalogListingEnabled = chatbotCatalogListingEnabled;
    }

    public Integer getChatbotMaxProductImagesGrant() {
        return chatbotMaxProductImagesGrant;
    }

    public void setChatbotMaxProductImagesGrant(Integer chatbotMaxProductImagesGrant) {
        this.chatbotMaxProductImagesGrant = chatbotMaxProductImagesGrant;
    }

    public String getChatbotPromotionShelfMode() {
        return chatbotPromotionShelfMode;
    }

    public void setChatbotPromotionShelfMode(String chatbotPromotionShelfMode) {
        this.chatbotPromotionShelfMode = chatbotPromotionShelfMode != null ? chatbotPromotionShelfMode : "PROMOTION";
    }

    public Integer getChatbotPromotionRotateSeconds() {
        return chatbotPromotionRotateSeconds;
    }

    public void setChatbotPromotionRotateSeconds(Integer chatbotPromotionRotateSeconds) {
        this.chatbotPromotionRotateSeconds = chatbotPromotionRotateSeconds != null ? chatbotPromotionRotateSeconds : 30;
    }

    public String getUrlPayAlertEmailYn() { return urlPayAlertEmailYn; }
    public void setUrlPayAlertEmailYn(String urlPayAlertEmailYn) { this.urlPayAlertEmailYn = urlPayAlertEmailYn; }
    public String getUrlPayLineNotifyToken() { return urlPayLineNotifyToken; }
    public void setUrlPayLineNotifyToken(String urlPayLineNotifyToken) { this.urlPayLineNotifyToken = urlPayLineNotifyToken; }
    public String getSplitPayEnabledYn() { return splitPayEnabledYn; }
    public void setSplitPayEnabledYn(String splitPayEnabledYn) { this.splitPayEnabledYn = splitPayEnabledYn != null && "Y".equalsIgnoreCase(splitPayEnabledYn.trim()) ? "Y" : "N"; }
    public String getSplitPayIntervalMonthYn() { return splitPayIntervalMonthYn; }
    public void setSplitPayIntervalMonthYn(String splitPayIntervalMonthYn) { this.splitPayIntervalMonthYn = splitPayIntervalMonthYn != null && "Y".equalsIgnoreCase(splitPayIntervalMonthYn.trim()) ? "Y" : "N"; }
    public String getSplitPayIntervalDayYn() { return splitPayIntervalDayYn; }
    public void setSplitPayIntervalDayYn(String splitPayIntervalDayYn) { this.splitPayIntervalDayYn = splitPayIntervalDayYn != null && "Y".equalsIgnoreCase(splitPayIntervalDayYn.trim()) ? "Y" : "N"; }
    public Integer getSplitPayDayIntervalDays() { return splitPayDayIntervalDays; }
    public void setSplitPayDayIntervalDays(Integer splitPayDayIntervalDays) { this.splitPayDayIntervalDays = splitPayDayIntervalDays != null && splitPayDayIntervalDays > 0 ? splitPayDayIntervalDays : 10; }
    public Integer getSplitPayMonthIntervalMonths() { return splitPayMonthIntervalMonths; }
    public void setSplitPayMonthIntervalMonths(Integer splitPayMonthIntervalMonths) {
        this.splitPayMonthIntervalMonths = splitPayMonthIntervalMonths != null && splitPayMonthIntervalMonths > 0
                ? splitPayMonthIntervalMonths : 1;
    }
    public String getSplitPayFirstPayMode() { return splitPayFirstPayMode; }
    public void setSplitPayFirstPayMode(String splitPayFirstPayMode) {
        this.splitPayFirstPayMode = splitPayFirstPayMode != null && "LINK".equalsIgnoreCase(splitPayFirstPayMode.trim()) ? "LINK" : "IMMEDIATE";
    }
    public String getSplitPayIntervalMultiYn() { return splitPayIntervalMultiYn; }
    public void setSplitPayIntervalMultiYn(String splitPayIntervalMultiYn) {
        this.splitPayIntervalMultiYn = splitPayIntervalMultiYn != null && "Y".equalsIgnoreCase(splitPayIntervalMultiYn.trim()) ? "Y" : "N";
    }
    public Integer getSplitPayMultiMaxMonths() { return splitPayMultiMaxMonths; }
    public void setSplitPayMultiMaxMonths(Integer splitPayMultiMaxMonths) {
        int v = splitPayMultiMaxMonths != null ? splitPayMultiMaxMonths : 6;
        this.splitPayMultiMaxMonths = (v == 3 || v == 5 || v == 6 || v == 12) ? v : 6;
    }
    public String getSplitPayHeaderLogoMode() { return splitPayHeaderLogoMode; }
    public void setSplitPayHeaderLogoMode(String splitPayHeaderLogoMode) {
        this.splitPayHeaderLogoMode = splitPayHeaderLogoMode;
    }
    public String getSplitPayHeaderLogoUrl() { return splitPayHeaderLogoUrl; }
    public void setSplitPayHeaderLogoUrl(String splitPayHeaderLogoUrl) { this.splitPayHeaderLogoUrl = splitPayHeaderLogoUrl; }
    public String getSplitPayHeaderHtmlTitle() { return splitPayHeaderHtmlTitle; }
    public void setSplitPayHeaderHtmlTitle(String splitPayHeaderHtmlTitle) { this.splitPayHeaderHtmlTitle = splitPayHeaderHtmlTitle; }
    public String getSplitPayHeaderSubtitleMode() { return splitPayHeaderSubtitleMode; }
    public void setSplitPayHeaderSubtitleMode(String splitPayHeaderSubtitleMode) {
        this.splitPayHeaderSubtitleMode = splitPayHeaderSubtitleMode;
    }
    public String getSplitPayHeaderSubtitleText() { return splitPayHeaderSubtitleText; }
    public void setSplitPayHeaderSubtitleText(String splitPayHeaderSubtitleText) {
        this.splitPayHeaderSubtitleText = splitPayHeaderSubtitleText;
    }
    public String getSplitPayLangMenuUseYn() { return splitPayLangMenuUseYn; }
    public void setSplitPayLangMenuUseYn(String splitPayLangMenuUseYn) {
        this.splitPayLangMenuUseYn = splitPayLangMenuUseYn != null && "Y".equalsIgnoreCase(splitPayLangMenuUseYn.trim()) ? "Y" : "N";
    }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public Integer getTerminalCountTerminal() { return terminalCountTerminal; }
    public void setTerminalCountTerminal(Integer terminalCountTerminal) { this.terminalCountTerminal = terminalCountTerminal; }
    public Integer getTerminalCountWeb() { return terminalCountWeb; }
    public void setTerminalCountWeb(Integer terminalCountWeb) { this.terminalCountWeb = terminalCountWeb; }
    public String getRegionalSettings() { return regionalSettings; }
    public void setRegionalSettings(String regionalSettings) { this.regionalSettings = regionalSettings; }
    public String getPayFollowMerchantUseYn() { return payFollowMerchantUseYn; }
    public void setPayFollowMerchantUseYn(String payFollowMerchantUseYn) { this.payFollowMerchantUseYn = payFollowMerchantUseYn; }
    public String getPayFollowAutoVoidYn() { return payFollowAutoVoidYn; }
    public void setPayFollowAutoVoidYn(String payFollowAutoVoidYn) { this.payFollowAutoVoidYn = payFollowAutoVoidYn; }
    public String getPayFollowEmailVoidYn() { return payFollowEmailVoidYn; }
    public void setPayFollowEmailVoidYn(String payFollowEmailVoidYn) { this.payFollowEmailVoidYn = payFollowEmailVoidYn; }
    public String getPayFollowAutoRefundYn() { return payFollowAutoRefundYn; }
    public void setPayFollowAutoRefundYn(String payFollowAutoRefundYn) { this.payFollowAutoRefundYn = payFollowAutoRefundYn; }
    public String getPayFollowForceRefundYn() { return payFollowForceRefundYn; }
    public void setPayFollowForceRefundYn(String payFollowForceRefundYn) { this.payFollowForceRefundYn = payFollowForceRefundYn; }
    public String getCardRiskPolicyMode() { return cardRiskPolicyMode; }
    public void setCardRiskPolicyMode(String cardRiskPolicyMode) { this.cardRiskPolicyMode = cardRiskPolicyMode; }
    public Integer getCardRiskTier1Hours() { return cardRiskTier1Hours; }
    public void setCardRiskTier1Hours(Integer cardRiskTier1Hours) { this.cardRiskTier1Hours = cardRiskTier1Hours; }
    public Integer getCardRiskTier1Min() { return cardRiskTier1Min; }
    public void setCardRiskTier1Min(Integer cardRiskTier1Min) { this.cardRiskTier1Min = cardRiskTier1Min; }
    public Integer getCardRiskTier2Hours() { return cardRiskTier2Hours; }
    public void setCardRiskTier2Hours(Integer cardRiskTier2Hours) { this.cardRiskTier2Hours = cardRiskTier2Hours; }
    public Integer getCardRiskTier2Min() { return cardRiskTier2Min; }
    public void setCardRiskTier2Min(Integer cardRiskTier2Min) { this.cardRiskTier2Min = cardRiskTier2Min; }
    public Integer getCardRiskTier3Hours() { return cardRiskTier3Hours; }
    public void setCardRiskTier3Hours(Integer cardRiskTier3Hours) { this.cardRiskTier3Hours = cardRiskTier3Hours; }
    public Integer getCardRiskTier3Min() { return cardRiskTier3Min; }
    public void setCardRiskTier3Min(Integer cardRiskTier3Min) { this.cardRiskTier3Min = cardRiskTier3Min; }
    public Integer getCardRiskTier4Hours() { return cardRiskTier4Hours; }
    public void setCardRiskTier4Hours(Integer cardRiskTier4Hours) { this.cardRiskTier4Hours = cardRiskTier4Hours; }
    public Integer getCardRiskTier4Min() { return cardRiskTier4Min; }
    public void setCardRiskTier4Min(Integer cardRiskTier4Min) { this.cardRiskTier4Min = cardRiskTier4Min; }
    public Integer getCardRiskAutoBlacklistTier() { return cardRiskAutoBlacklistTier; }
    public void setCardRiskAutoBlacklistTier(Integer cardRiskAutoBlacklistTier) {
        this.cardRiskAutoBlacklistTier = cardRiskAutoBlacklistTier;
    }
    public String getCardRiskTrackPeriodPolicy() { return cardRiskTrackPeriodPolicy; }
    public void setCardRiskTrackPeriodPolicy(String cardRiskTrackPeriodPolicy) {
        this.cardRiskTrackPeriodPolicy = cardRiskTrackPeriodPolicy;
    }
    public String getCardRiskTrackPeriodMode() { return cardRiskTrackPeriodMode; }
    public void setCardRiskTrackPeriodMode(String cardRiskTrackPeriodMode) {
        this.cardRiskTrackPeriodMode = cardRiskTrackPeriodMode;
    }
    public Integer getCardRiskTrackPeriodValue() { return cardRiskTrackPeriodValue; }
    public void setCardRiskTrackPeriodValue(Integer cardRiskTrackPeriodValue) {
        this.cardRiskTrackPeriodValue = cardRiskTrackPeriodValue;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

