package com.pg.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * ChillPay Search Payment Transaction API 응답 (Table 1.3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChillPayPaymentSearchApiResponse {

    @JsonAlias({"totalRecord", "TotalRecord"})
    private Long totalRecord;
    @JsonAlias({"pageSize", "PageSize"})
    private Integer pageSize;
    @JsonAlias({"pageNumber", "PageNumber"})
    private Integer pageNumber;
    @JsonAlias({"filteredRecord", "FilteredRecord"})
    private Integer filteredRecord;
    /** ChillPay 본문 상태 코드 (Appendix C) */
    @JsonAlias({"status", "Status"})
    private Integer status;
    @JsonAlias({"message", "Message"})
    private String message;
    @JsonAlias({"data", "Data"})
    private List<Map<String, Object>> data;

    public Long getTotalRecord() { return totalRecord; }
    public void setTotalRecord(Long totalRecord) { this.totalRecord = totalRecord; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public Integer getFilteredRecord() { return filteredRecord; }
    public void setFilteredRecord(Integer filteredRecord) { this.filteredRecord = filteredRecord; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
}
