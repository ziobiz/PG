package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 노티미들웨어 → PG 노티 서버로 보내는 <strong>관리자 무효·취소·환불 중계</strong> 요청 본문.
 * 칠페이 원문 노티가 아닐 때도 동일 URL·HMAC 정책으로 수신한 뒤, 내부에서 ChillPay 호환 JSON 으로 변환합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotiMiddlewareRelayRequest {

    /**
     * {@code VOID} | {@code CANCEL} | {@code REFUND} (대소문자 무시)
     */
    private String eventType;

    @JsonAlias({ "transactionId", "TransactionId", "transId" })
    private String transactionId;

    @JsonAlias({ "merchantCode", "MerchantCode", "mid", "MID" })
    private String merchantCode;

    @JsonAlias({ "routeNo", "RouteNo", "rootNo", "RootNo" })
    private String routeNo;

    @JsonAlias({ "orderNo", "OrderNo" })
    private String orderNo;

    /** 업체코드 — {@code PaymentDescription} 에 {@code icopayCompId=} 로 넣어 가맹점 매칭 보강 */
    @JsonAlias({ "compId", "comp_id", "icopayCompId" })
    private String compId;

    private String reason;

    /** 선택: 직접 ICOPAY 내부 코드(21·20·30 등). 없으면 eventType 에서 결정 */
    @JsonAlias({ "internalStatusCode", "statusCode" })
    private String internalStatusCode;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public String getRouteNo() {
        return routeNo;
    }

    public void setRouteNo(String routeNo) {
        this.routeNo = routeNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getCompId() {
        return compId;
    }

    public void setCompId(String compId) {
        this.compId = compId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInternalStatusCode() {
        return internalStatusCode;
    }

    public void setInternalStatusCode(String internalStatusCode) {
        this.internalStatusCode = internalStatusCode;
    }
}
