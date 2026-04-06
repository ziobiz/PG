package com.pg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ChillPay (칠리페이) Sandbox 연동 설정.
 * application.yml의 app.chillpay 로 설정.
 */
@Component
@ConfigurationProperties(prefix = "app.chillpay")
public class ChillPayProperties {

    /** ChillPay Merchant Code (예: M035594) */
    private String merchantCode = "M035594";
    /** ChillPay API Key */
    private String apiKey;
    /** ChillPay MD5 Secret Key (CheckSum 생성용) */
    private String md5Key;
    /** Route No — DB에 URL결제 ChillPay 행·본사 API설정 route가 없을 때만 쓰는 최후 폴백 */
    private int routeNo = 4;
    /** Sandbox 사용 여부 */
    private boolean sandbox = true;

    /** DirectCredit(Inlie) API Sandbox Base */
    private static final String DIRECT_CREDIT_SANDBOX = "https://sandbox-api-directcredit.chillpay.co";
    /** DirectCredit(Inlie) API Production Base */
    private static final String DIRECT_CREDIT_PROD = "https://api-directcredit.chillpay.co";
    /** CCD Payment Script Sandbox */
    public static final String CCD_SCRIPT_SANDBOX = "https://sandbox-bankdemo3.chillpay.co/js/ccdpayment.js";
    /** CCD Payment Script Production */
    public static final String CCD_SCRIPT_PROD = "https://cdn.chill.credit/js/ccdpayment.js";

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getMd5Key() { return md5Key; }
    public void setMd5Key(String md5Key) { this.md5Key = md5Key; }
    public int getRouteNo() { return routeNo; }
    public void setRouteNo(int routeNo) { this.routeNo = routeNo; }
    public boolean isSandbox() { return sandbox; }
    public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }

    public String getDirectCreditBaseUrl() {
        return sandbox ? DIRECT_CREDIT_SANDBOX : DIRECT_CREDIT_PROD;
    }
    public String getPaymentApiUrl() {
        return getDirectCreditBaseUrl() + "/api/v1/payment";
    }
    public String getCcdScriptUrl() {
        return sandbox ? CCD_SCRIPT_SANDBOX : CCD_SCRIPT_PROD;
    }
}
