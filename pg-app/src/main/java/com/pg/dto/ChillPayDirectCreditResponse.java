package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ChillPay DirectCredit Payment API 응답.
 * status 200 + paymentStatus WaitAuthorize → paymentUrl로 리다이렉트
 * status 200 + paymentStatus Paid → 즉시 결제 완료
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChillPayDirectCreditResponse {

    private int status;
    private String message;
    private Data data;

    public static class Data {
        private String paymentStatus;  // WaitAuthorize, Paid, ...
        private Long amount;
        private String orderNo;
        private String customerId;
        private String returnUrl;
        private String paymentUrl;     // OTP 확인 페이지 URL
        private String ipAddress;
        private String token;
        private Long transactionId;
        private String channelCode;
        private String createdDate;
        private String expiredDate;

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
        public String getPaymentUrl() { return paymentUrl; }
        public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public String getChannelCode() { return channelCode; }
        public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
        public String getCreatedDate() { return createdDate; }
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
        public String getExpiredDate() { return expiredDate; }
        public void setExpiredDate(String expiredDate) { this.expiredDate = expiredDate; }
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}
