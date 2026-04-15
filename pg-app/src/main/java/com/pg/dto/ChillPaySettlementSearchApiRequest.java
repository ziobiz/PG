package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ChillPay Search Settlement Transaction API 요청(Table 2.2).
 * JSON 키는 PascalCase, 문서 예시처럼 미사용 값은 {@code null}(직렬화 시 키 유지)로 둔다.
 */
@JsonPropertyOrder({
        "OrderBy", "OrderDir", "PageSize", "PageNumber", "SearchKeyword", "MerchantCode", "PaymentChannel",
        "RouteNo", "OrderNo", "Settled", "TransactionDateFrom", "TransactionDateTo", "PaymentDateFrom", "PaymentDateTo",
        "TransferDateFrom", "TransferDateTo", "CutOffTimeDateFrom", "CutOffTimeDateTo", "Checksum"
})
public class ChillPaySettlementSearchApiRequest {

    @JsonProperty("OrderBy")
    private String OrderBy;
    @JsonProperty("OrderDir")
    private String OrderDir;
    @JsonProperty("PageSize")
    private Integer PageSize;
    @JsonProperty("PageNumber")
    private Integer PageNumber;
    @JsonProperty("SearchKeyword")
    private String SearchKeyword;
    @JsonProperty("MerchantCode")
    private String MerchantCode;
    @JsonProperty("PaymentChannel")
    private String PaymentChannel;
    @JsonProperty("RouteNo")
    private Integer RouteNo;
    @JsonProperty("OrderNo")
    private String OrderNo;
    @JsonProperty("Settled")
    private String Settled;
    @JsonProperty("TransactionDateFrom")
    private String TransactionDateFrom;
    @JsonProperty("TransactionDateTo")
    private String TransactionDateTo;
    @JsonProperty("PaymentDateFrom")
    private String PaymentDateFrom;
    @JsonProperty("PaymentDateTo")
    private String PaymentDateTo;
    @JsonProperty("TransferDateFrom")
    private String TransferDateFrom;
    @JsonProperty("TransferDateTo")
    private String TransferDateTo;
    @JsonProperty("CutOffTimeDateFrom")
    private String CutOffTimeDateFrom;
    @JsonProperty("CutOffTimeDateTo")
    private String CutOffTimeDateTo;
    @JsonProperty("Checksum")
    private String Checksum;

    public String getOrderBy() {
        return OrderBy;
    }

    public void setOrderBy(String orderBy) {
        OrderBy = orderBy;
    }

    public String getOrderDir() {
        return OrderDir;
    }

    public void setOrderDir(String orderDir) {
        OrderDir = orderDir;
    }

    public Integer getPageSize() {
        return PageSize;
    }

    public void setPageSize(Integer pageSize) {
        PageSize = pageSize;
    }

    public Integer getPageNumber() {
        return PageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        PageNumber = pageNumber;
    }

    public String getSearchKeyword() {
        return SearchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        SearchKeyword = searchKeyword;
    }

    public String getMerchantCode() {
        return MerchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        MerchantCode = merchantCode;
    }

    public String getPaymentChannel() {
        return PaymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        PaymentChannel = paymentChannel;
    }

    public Integer getRouteNo() {
        return RouteNo;
    }

    public void setRouteNo(Integer routeNo) {
        RouteNo = routeNo;
    }

    public String getOrderNo() {
        return OrderNo;
    }

    public void setOrderNo(String orderNo) {
        OrderNo = orderNo;
    }

    public String getSettled() {
        return Settled;
    }

    public void setSettled(String settled) {
        Settled = settled;
    }

    public String getTransactionDateFrom() {
        return TransactionDateFrom;
    }

    public void setTransactionDateFrom(String transactionDateFrom) {
        TransactionDateFrom = transactionDateFrom;
    }

    public String getTransactionDateTo() {
        return TransactionDateTo;
    }

    public void setTransactionDateTo(String transactionDateTo) {
        TransactionDateTo = transactionDateTo;
    }

    public String getPaymentDateFrom() {
        return PaymentDateFrom;
    }

    public void setPaymentDateFrom(String paymentDateFrom) {
        PaymentDateFrom = paymentDateFrom;
    }

    public String getPaymentDateTo() {
        return PaymentDateTo;
    }

    public void setPaymentDateTo(String paymentDateTo) {
        PaymentDateTo = paymentDateTo;
    }

    public String getTransferDateFrom() {
        return TransferDateFrom;
    }

    public void setTransferDateFrom(String transferDateFrom) {
        TransferDateFrom = transferDateFrom;
    }

    public String getTransferDateTo() {
        return TransferDateTo;
    }

    public void setTransferDateTo(String transferDateTo) {
        TransferDateTo = transferDateTo;
    }

    public String getCutOffTimeDateFrom() {
        return CutOffTimeDateFrom;
    }

    public void setCutOffTimeDateFrom(String cutOffTimeDateFrom) {
        CutOffTimeDateFrom = cutOffTimeDateFrom;
    }

    public String getCutOffTimeDateTo() {
        return CutOffTimeDateTo;
    }

    public void setCutOffTimeDateTo(String cutOffTimeDateTo) {
        CutOffTimeDateTo = cutOffTimeDateTo;
    }

    public String getChecksum() {
        return Checksum;
    }

    public void setChecksum(String checksum) {
        Checksum = checksum;
    }

    public String toChecksumPlainString() {
        return nz(OrderBy) + nz(OrderDir) + nz(PageSize) + nz(PageNumber) + nz(SearchKeyword) + nz(MerchantCode)
                + nz(PaymentChannel) + nz(RouteNo) + nz(OrderNo) + nz(Settled) + nz(TransactionDateFrom)
                + nz(TransactionDateTo) + nz(PaymentDateFrom) + nz(PaymentDateTo) + nz(TransferDateFrom)
                + nz(TransferDateTo) + nz(CutOffTimeDateFrom) + nz(CutOffTimeDateTo);
    }

    /**
     * 문서 표기 순서 대안: 이체·컷오프·거래·결제일 문자열 뒤에 {@code Settled}를 두는 방식(일부 버전 호환).
     */
    public String toChecksumPlainStringSettledLast() {
        return nz(OrderBy) + nz(OrderDir) + nz(PageSize) + nz(PageNumber) + nz(SearchKeyword) + nz(MerchantCode)
                + nz(PaymentChannel) + nz(RouteNo) + nz(OrderNo)
                + nz(TransactionDateFrom) + nz(TransactionDateTo) + nz(PaymentDateFrom) + nz(PaymentDateTo)
                + nz(TransferDateFrom) + nz(TransferDateTo) + nz(CutOffTimeDateFrom) + nz(CutOffTimeDateTo)
                + nz(Settled);
    }

    /**
     * 결제 검색(Table 1.2)과 동일한 14필드 연결: {@code Settled}가 결제 API의 {@code Status} 자리.
     * 이체·컷오프가 Checksum 에 포함되지 않는 스펙 호환용.
     */
    public String toChecksumPlainStringPaymentSearchParallel() {
        return nz(OrderBy) + nz(OrderDir) + nz(PageSize) + nz(PageNumber) + nz(SearchKeyword) + nz(MerchantCode)
                + nz(PaymentChannel) + nz(RouteNo) + nz(OrderNo) + nz(Settled) + nz(TransactionDateFrom)
                + nz(TransactionDateTo) + nz(PaymentDateFrom) + nz(PaymentDateTo);
    }

    /**
     * 일부 스펙: {@code OrderNo} 가 {@code RouteNo} 앞에 오는 Checksum 순서.
     */
    public String toChecksumPlainStringOrderNoBeforeRouteNo() {
        return nz(OrderBy) + nz(OrderDir) + nz(PageSize) + nz(PageNumber) + nz(SearchKeyword) + nz(MerchantCode)
                + nz(PaymentChannel) + nz(OrderNo) + nz(RouteNo) + nz(Settled) + nz(TransactionDateFrom)
                + nz(TransactionDateTo) + nz(PaymentDateFrom) + nz(PaymentDateTo) + nz(TransferDateFrom)
                + nz(TransferDateTo) + nz(CutOffTimeDateFrom) + nz(CutOffTimeDateTo);
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String nz(Integer n) {
        return n != null ? String.valueOf(n) : "";
    }
}
