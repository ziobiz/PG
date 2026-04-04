package com.pg.dto;

/**
 * ChillPay Search Payment Transaction API 요청 본문.
 * 문서: ChillPay-API-Transaction-Services-Document-EN_v1.0.6 Table 1.2
 * <p>
 * Checksum: Table 1.2 No.1~14 값을 순서대로 이어 붙인 뒤 MD5 Secret Key를 붙이고 MD5 해시.
 */
public class ChillPayPaymentSearchApiRequest {

    private String OrderBy;
    private String OrderDir;
    private Integer PageSize;
    private Integer PageNumber;
    private String SearchKeyword;
    private String MerchantCode;
    private String PaymentChannel;
    private Integer RouteNo;
    private String OrderNo;
    private String Status;
    private String TransactionDateFrom;
    private String TransactionDateTo;
    private String PaymentDateFrom;
    private String PaymentDateTo;
    private String Checksum;

    public String getOrderBy() { return OrderBy; }
    public void setOrderBy(String orderBy) { OrderBy = orderBy; }
    public String getOrderDir() { return OrderDir; }
    public void setOrderDir(String orderDir) { OrderDir = orderDir; }
    public Integer getPageSize() { return PageSize; }
    public void setPageSize(Integer pageSize) { PageSize = pageSize; }
    public Integer getPageNumber() { return PageNumber; }
    public void setPageNumber(Integer pageNumber) { PageNumber = pageNumber; }
    public String getSearchKeyword() { return SearchKeyword; }
    public void setSearchKeyword(String searchKeyword) { SearchKeyword = searchKeyword; }
    public String getMerchantCode() { return MerchantCode; }
    public void setMerchantCode(String merchantCode) { MerchantCode = merchantCode; }
    public String getPaymentChannel() { return PaymentChannel; }
    public void setPaymentChannel(String paymentChannel) { PaymentChannel = paymentChannel; }
    public Integer getRouteNo() { return RouteNo; }
    public void setRouteNo(Integer routeNo) { RouteNo = routeNo; }
    public String getOrderNo() { return OrderNo; }
    public void setOrderNo(String orderNo) { OrderNo = orderNo; }
    public String getStatus() { return Status; }
    public void setStatus(String status) { Status = status; }
    public String getTransactionDateFrom() { return TransactionDateFrom; }
    public void setTransactionDateFrom(String transactionDateFrom) { TransactionDateFrom = transactionDateFrom; }
    public String getTransactionDateTo() { return TransactionDateTo; }
    public void setTransactionDateTo(String transactionDateTo) { TransactionDateTo = transactionDateTo; }
    public String getPaymentDateFrom() { return PaymentDateFrom; }
    public void setPaymentDateFrom(String paymentDateFrom) { PaymentDateFrom = paymentDateFrom; }
    public String getPaymentDateTo() { return PaymentDateTo; }
    public void setPaymentDateTo(String paymentDateTo) { PaymentDateTo = paymentDateTo; }
    public String getChecksum() { return Checksum; }
    public void setChecksum(String checksum) { Checksum = checksum; }

    /**
     * Checksum 계산용 연결 문자열 (No.1~14, null/미사용은 빈 문자열).
     */
    public String toChecksumPlainString() {
        return nz(OrderBy) + nz(OrderDir) + nz(PageSize) + nz(PageNumber) + nz(SearchKeyword) + nz(MerchantCode)
                + nz(PaymentChannel) + nz(RouteNo) + nz(OrderNo) + nz(Status) + nz(TransactionDateFrom)
                + nz(TransactionDateTo) + nz(PaymentDateFrom) + nz(PaymentDateTo);
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String nz(Integer n) {
        return n != null ? String.valueOf(n) : "";
    }
}
