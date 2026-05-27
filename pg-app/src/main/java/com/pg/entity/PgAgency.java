package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 결제대행사 - 총본사에서 여러 국가 PG사 API로 구축한 결제 모듈.
 * 가맹점이 이 결제대행사를 선택하여 결제 서비스를 이용.
 * <p>
 * 연동 용도(노티 / URL결제 / 웹챗봇 / API)는 <strong>행당 하나</strong>만 Y로 두고, PG코드는 용도별로 분리 등록하는 것을 권장한다.
 * {@code api_endpoint}는 구버전 호환·ChillPay URL 병합용으로 유지한다.
 */
@Entity
@Table(name = "tb_pg_agency")
public class PgAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_cd", nullable = false, unique = true, length = 20)
    private String pgCd;

    @Column(name = "pg_nm", nullable = false, length = 100)
    private String pgNm;

    /** 구버전 통합 URL (신규는 용도별 엔드포인트 우선) */
    @Column(name = "api_endpoint", length = 255)
    private String apiEndpoint;

    /** 노티 미들웨어·노티 처리 연동에 사용하는 기준 URL(참고·식별) */
    @Column(name = "endpoint_noti", length = 512)
    private String endpointNoti;

    /** URL 결제(피지사 URL 플로우) 연동용 엔드포인트 */
    @Column(name = "endpoint_url_pay", length = 512)
    private String endpointUrlPay;

    /** API 연동·웹챗봇 결제(피지사 API 직연동)용 엔드포인트 */
    @Column(name = "endpoint_api", length = 512)
    private String endpointApi;

    /** 연동 용도: 노티 미들웨어에서 MID·Route·Key·IV 매칭 후 가맹점 적재 */
    @Column(name = "integ_noti_yn", nullable = false, length = 1)
    private String integNotiYn = "N";

    @Column(name = "integ_url_pay_yn", nullable = false, length = 1)
    private String integUrlPayYn = "N";

    @Column(name = "integ_web_chatbot_yn", nullable = false, length = 1)
    private String integWebChatbotYn = "N";

    @Column(name = "integ_api_yn", nullable = false, length = 1)
    private String integApiYn = "N";

    /** JPAY 가맹 API 구독(정기) 연동 — integ_api_yn 과 별도 */
    @Column(name = "integ_api_subscription_yn", nullable = false, length = 1)
    private String integApiSubscriptionYn = "N";

    @Column(name = "endpoint_api_subscription", length = 512)
    private String endpointApiSubscription;

    /** URL 재결제(저장 카드·CreditToken) 연동 */
    @Column(name = "integ_url_pay_repay_yn", nullable = false, length = 1)
    private String integUrlPayRepayYn = "N";

    @Column(name = "endpoint_url_pay_repay", length = 512)
    private String endpointUrlPayRepay;

    @Column(name = "use_yn", length = 1)
    private String useYn = "Y";

    /** 결제 운영 대행사(Y): 가맹점 PG 선택·연동에 노출. 사용(use_yn)이 Y인 항목 중 운영으로 지정된 것만 */
    @Column(name = "operational_yn", length = 1)
    private String operationalYn = "N";

    /** PG별 MID / Merchant Code */
    @Column(name = "merchant_mid", length = 100)
    private String merchantMid;

    @Column(name = "api_key", length = 512)
    private String apiKey;

    /** CheckSum·서명용 시크릿 (예 ChillPay MD5 Key). 가맹점 연동 IV와 별개 */
    @Column(name = "md5_secret_key", length = 255)
    private String md5SecretKey;

    @Column(name = "route_no")
    private Integer routeNo;

    @Column(name = "sandbox_yn", length = 1)
    private String sandboxYn = "Y";

    /** PG별 추가 파라미터 JSON */
    @Column(name = "credentials_extra_json", columnDefinition = "TEXT")
    private String credentialsExtraJson;

    /**
     * PG↔ICOPAY 통합정산 예정일 규칙: {@code OFF}(미사용), {@code T}(T+N 영업일·결제와 동일 시각), {@code D}(D+N 달력일·일괄 시각).
     */
    @Column(name = "ext_settle_mode", nullable = false, length = 8)
    private String extSettleMode = "OFF";

    /** N (1~10). {@code OFF}이면 무시 */
    @Column(name = "ext_settle_lag")
    private Integer extSettleLag;

    /** {@code D} 모드: 정산일 당일의 정산 시각. {@code T} 모드에서는 무시 */
    @Column(name = "ext_settle_batch_time")
    private LocalTime extSettleBatchTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        normalizeIntegrationFlags();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeIntegrationFlags();
    }

    private void normalizeIntegrationFlags() {
        if (integNotiYn == null || integNotiYn.isBlank()) {
            integNotiYn = "N";
        }
        if (integUrlPayYn == null || integUrlPayYn.isBlank()) {
            integUrlPayYn = "N";
        }
        if (integWebChatbotYn == null || integWebChatbotYn.isBlank()) {
            integWebChatbotYn = "N";
        }
        if (integApiYn == null || integApiYn.isBlank()) {
            integApiYn = "N";
        }
        if (integApiSubscriptionYn == null || integApiSubscriptionYn.isBlank()) {
            integApiSubscriptionYn = "N";
        }
        if (integUrlPayRepayYn == null || integUrlPayRepayYn.isBlank()) {
            integUrlPayRepayYn = "N";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getPgNm() { return pgNm; }
    public void setPgNm(String pgNm) { this.pgNm = pgNm; }
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public String getEndpointNoti() { return endpointNoti; }
    public void setEndpointNoti(String endpointNoti) { this.endpointNoti = endpointNoti; }
    public String getEndpointUrlPay() { return endpointUrlPay; }
    public void setEndpointUrlPay(String endpointUrlPay) { this.endpointUrlPay = endpointUrlPay; }
    public String getEndpointApi() { return endpointApi; }
    public void setEndpointApi(String endpointApi) { this.endpointApi = endpointApi; }
    public String getIntegNotiYn() { return integNotiYn; }
    public void setIntegNotiYn(String integNotiYn) { this.integNotiYn = integNotiYn; }
    public String getIntegUrlPayYn() { return integUrlPayYn; }
    public void setIntegUrlPayYn(String integUrlPayYn) { this.integUrlPayYn = integUrlPayYn; }
    public String getIntegWebChatbotYn() { return integWebChatbotYn; }
    public void setIntegWebChatbotYn(String integWebChatbotYn) { this.integWebChatbotYn = integWebChatbotYn; }
    public String getIntegApiYn() { return integApiYn; }
    public void setIntegApiYn(String integApiYn) { this.integApiYn = integApiYn; }
    public String getIntegApiSubscriptionYn() { return integApiSubscriptionYn; }
    public void setIntegApiSubscriptionYn(String integApiSubscriptionYn) { this.integApiSubscriptionYn = integApiSubscriptionYn; }
    public String getEndpointApiSubscription() { return endpointApiSubscription; }
    public void setEndpointApiSubscription(String endpointApiSubscription) { this.endpointApiSubscription = endpointApiSubscription; }
    public String getIntegUrlPayRepayYn() { return integUrlPayRepayYn; }
    public void setIntegUrlPayRepayYn(String integUrlPayRepayYn) { this.integUrlPayRepayYn = integUrlPayRepayYn; }
    public String getEndpointUrlPayRepay() { return endpointUrlPayRepay; }
    public void setEndpointUrlPayRepay(String endpointUrlPayRepay) { this.endpointUrlPayRepay = endpointUrlPayRepay; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getOperationalYn() { return operationalYn; }
    public void setOperationalYn(String operationalYn) { this.operationalYn = operationalYn; }
    public String getMerchantMid() { return merchantMid; }
    public void setMerchantMid(String merchantMid) { this.merchantMid = merchantMid; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getMd5SecretKey() { return md5SecretKey; }
    public void setMd5SecretKey(String md5SecretKey) { this.md5SecretKey = md5SecretKey; }
    public Integer getRouteNo() { return routeNo; }
    public void setRouteNo(Integer routeNo) { this.routeNo = routeNo; }
    public String getSandboxYn() { return sandboxYn; }
    public void setSandboxYn(String sandboxYn) { this.sandboxYn = sandboxYn; }
    public String getCredentialsExtraJson() { return credentialsExtraJson; }
    public void setCredentialsExtraJson(String credentialsExtraJson) { this.credentialsExtraJson = credentialsExtraJson; }
    public String getExtSettleMode() { return extSettleMode; }
    public void setExtSettleMode(String extSettleMode) { this.extSettleMode = extSettleMode; }
    public Integer getExtSettleLag() { return extSettleLag; }
    public void setExtSettleLag(Integer extSettleLag) { this.extSettleLag = extSettleLag; }
    public LocalTime getExtSettleBatchTime() { return extSettleBatchTime; }
    public void setExtSettleBatchTime(LocalTime extSettleBatchTime) { this.extSettleBatchTime = extSettleBatchTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
