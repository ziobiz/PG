package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * ChillPay DirectCredit (Inline) Payment API 요청 파라미터.
 * 문서: ChillCredit-Merchant-Integration-Manual Table 1.3
 * <p>
 * 금액·CheckSum 문자열은 ziobiz/NOTI {@code /admin/test-pay/submit} 과 동일하게
 * (JavaScript {@code String(amount)} 에 맞춤) {@link #amountForChecksum()} 로 직렬화합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChillPayDirectCreditRequest {

    private String OrderNo;
    private String CustomerId;
    private BigDecimal Amount;
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
    /**
     * ChillPay 호스티드(OTP/3DS) 완료 후 브라우저 복귀 URL. 매뉴얼에 따라 JSON 필드명은 ReturnUrl.
     * CheckSum 연결 문자열(Table 1.3)에는 포함하지 않습니다(옵션 필드).
     */
    @JsonProperty("ReturnUrl")
    private String returnUrl;

    public String getOrderNo() { return OrderNo; }
    public void setOrderNo(String orderNo) { OrderNo = orderNo; }
    public String getCustomerId() { return CustomerId; }
    public void setCustomerId(String customerId) { CustomerId = customerId; }
    public BigDecimal getAmount() { return Amount; }
    public void setAmount(BigDecimal amount) { Amount = amount; }
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
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    /** CheckSum 계산용 문자열 (파라미터 1~17 순서 concatenation, null은 빈문자열) */
    public String toConcatString() {
        return nullToEmpty(OrderNo) + nullToEmpty(CustomerId) + amountForChecksum()
            + nullToEmpty(PhoneNumber) + nullToEmpty(Description) + nullToEmpty(ChannelCode)
            + nullToEmpty(Currency) + nullToEmpty(LangCode) + (RouteNo != null ? RouteNo : "")
            + nullToEmpty(IPAddress) + nullToEmpty(TokenType) + nullToEmpty(CreditToken)
            + nullToEmpty(DirectCreditToken) + (CreditMonth != null ? CreditMonth : "")
            + nullToEmpty(ShopID) + nullToEmpty(CustEmail) + nullToEmpty(SaveCard);
    }

    /** NOTI {@code parseVal(payload.Amount)} 와 동일: 정수 금액은 소수점 없이, 소수는 plain 문자열 */
    private String amountForChecksum() {
        if (Amount == null) {
            return "";
        }
        return Amount.stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
