package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ChillPay DirectCredit (Inline) Payment API 요청 파라미터.
 * 문서: ChillCredit-Merchant-Integration-Manual Table 1.3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChillPayDirectCreditRequest {

    private String OrderNo;
    private String CustomerId;
    private Long Amount;         // 소수점 2자리 포함 정수 (예: 550.25 → 55025)
    private String PhoneNumber;
    private String Description;
    private String ChannelCode = "creditcard";
    private String Currency = "392";   // 392=JPY (소수점 없음)
    private String LangCode = "EN";
    private Integer RouteNo;
    private String IPAddress;
    private String TokenType = "DT";  // DT: PaymentCreditToken, CT: PaymentCreditToken+CreditToken
    private String CreditToken;
    @JsonProperty("DirectCreditToken")
    private String DirectCreditToken;
    private Integer CreditMonth;
    private String ShopID;
    private String CustEmail;
    private String SaveCard = "N";
    private String CheckSum;

    public String getOrderNo() { return OrderNo; }
    public void setOrderNo(String orderNo) { OrderNo = orderNo; }
    public String getCustomerId() { return CustomerId; }
    public void setCustomerId(String customerId) { CustomerId = customerId; }
    public Long getAmount() { return Amount; }
    public void setAmount(Long amount) { Amount = amount; }
    public String getPhoneNumber() { return PhoneNumber; }
    public void setPhoneNumber(String phoneNumber) { PhoneNumber = phoneNumber; }
    public String getDescription() { return Description; }
    public void setDescription(String description) { Description = description; }
    public String getChannelCode() { return ChannelCode; }
    public void setChannelCode(String channelCode) { ChannelCode = channelCode; }
    public String getCurrency() { return Currency; }
    public void setCurrency(String currency) { Currency = currency; }
    public String getLangCode() { return LangCode; }
    public void setLangCode(String langCode) { LangCode = langCode; }
    public Integer getRouteNo() { return RouteNo; }
    public void setRouteNo(Integer routeNo) { RouteNo = routeNo; }
    public String getIPAddress() { return IPAddress; }
    public void setIPAddress(String IPAddress) { this.IPAddress = IPAddress; }
    public String getTokenType() { return TokenType; }
    public void setTokenType(String tokenType) { TokenType = tokenType; }
    public String getCreditToken() { return CreditToken; }
    public void setCreditToken(String creditToken) { CreditToken = creditToken; }
    public String getDirectCreditToken() { return DirectCreditToken; }
    public void setDirectCreditToken(String directCreditToken) { DirectCreditToken = directCreditToken; }
    public Integer getCreditMonth() { return CreditMonth; }
    public void setCreditMonth(Integer creditMonth) { CreditMonth = creditMonth; }
    public String getShopID() { return ShopID; }
    public void setShopID(String shopID) { ShopID = shopID; }
    public String getCustEmail() { return CustEmail; }
    public void setCustEmail(String custEmail) { CustEmail = custEmail; }
    public String getSaveCard() { return SaveCard; }
    public void setSaveCard(String saveCard) { SaveCard = saveCard; }
    public String getCheckSum() { return CheckSum; }
    public void setCheckSum(String checkSum) { CheckSum = checkSum; }

    /** CheckSum 계산용 문자열 (파라미터 1~17 순서 concatenation, null은 빈문자열) */
    public String toConcatString() {
        return nullToEmpty(OrderNo) + nullToEmpty(CustomerId) + (Amount != null ? Amount : "")
            + nullToEmpty(PhoneNumber) + nullToEmpty(Description) + nullToEmpty(ChannelCode)
            + nullToEmpty(Currency) + nullToEmpty(LangCode) + (RouteNo != null ? RouteNo : "")
            + nullToEmpty(IPAddress) + nullToEmpty(TokenType) + nullToEmpty(CreditToken)
            + nullToEmpty(DirectCreditToken) + (CreditMonth != null ? CreditMonth : "")
            + nullToEmpty(ShopID) + nullToEmpty(CustEmail) + nullToEmpty(SaveCard);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
