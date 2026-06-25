package com.pg.api.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 통합 결제내역(/api/calc/payList) 검색 파라미터.
 */
public class PayListSearchRequest {

    /** 통합 검색: 수수료내역 스타일의 검색구분 */
    private String searchFieldType;
    private String searchTranFactor;
    private String searchTranValue;
    private LocalDate searchFromDate;
    private LocalDate searchToDate;
    /** NM, CODE, 빈값(전체) */
    private String searchCompField;
    private String searchCompQ;
    private String searchMid;
    /** 하위 호환: 예전 파라미터명 */
    private String searchTmnId;
    private String searchPayDivCd;
    private String searchPayProcCd;
    private String searchKeyword;
    private String searchPgCd;
    private String searchCycle;
    private String searchRegNo;
    private String searchCardAprvNo;
    private String searchChillTxnId;
    /** NOTI 수신 경로: CALLBACK(기본)·RESULT·BOTH·ALL(전체). 통합/노티 결제내역만 서버에서 적용 */
    private String searchNotifyChannel;
    /** 목록 정렬 방향: ASC / 그 외 DESC(기본). 정렬 키는 {@link #searchOrderBy} 없으면 createdAt */
    private String searchOrderDir;
    /** 정렬 속성(화면 확장용): createdAt·paidAt·trnId·amtKrw·merchantId·orderNo·status 등 엔티티 필드명 */
    private String searchOrderBy;
    private String payListVariant;
    /** true — 상태바·금액 집계(meta) 생략(모두다운로드·연속 페이지 조회) */
    private boolean skipMeta;
    /** true — 페이지당 최대 15_000건까지 조회(모두다운로드) */
    private boolean listExport;
    /** 관리자 UI 언어(KO/EN/JP/CH/TH) — 처리사유 번역용. API 파라미터가 아닌 컨트롤러에서 설정 */
    private String adminUiLocale;
    private int page = 1;
    private int size = 20;

    public static PayListSearchRequest fromParams(Map<String, String> raw) {
        PayListSearchRequest r = new PayListSearchRequest();
        if (raw == null) return r;
        r.searchFieldType = raw.get("searchFieldType");
        r.searchTranFactor = raw.get("searchTranFactor");
        r.searchTranValue = raw.get("searchTranValue");
        r.searchCompField = raw.get("searchCompField");
        r.searchCompQ = raw.get("searchCompQ");
        r.searchMid = firstNonBlank(raw.get("searchMid"), raw.get("searchTmnId"));
        r.searchTmnId = raw.get("searchTmnId");
        r.searchPayDivCd = raw.get("searchPayDivCd");
        r.searchPayProcCd = raw.get("searchPayProcCd");
        r.searchKeyword = raw.get("searchKeyword");
        r.searchPgCd = firstNonBlank(raw.get("searchPgCd"), raw.get("searchPg"));
        r.searchCycle = raw.get("searchCycle");
        r.searchRegNo = raw.get("searchRegNo");
        r.searchCardAprvNo = raw.get("searchCardAprvNo");
        r.searchChillTxnId = raw.get("searchChillTxnId");
        r.searchNotifyChannel = raw.get("searchNotifyChannel");
        r.searchOrderDir = raw.get("searchOrderDir");
        r.searchOrderBy = raw.get("searchOrderBy");
        r.payListVariant = raw.get("payListVariant");
        r.searchFromDate = parseDate(raw.get("searchFromDate"));
        r.searchToDate = parseDate(raw.get("searchToDate"));
        r.page = parseInt(raw.get("page"), 1);
        r.size = parseInt(raw.get("size"), 20);
        r.skipMeta = isTruthy(raw.get("skipMeta"));
        r.listExport = isTruthy(raw.get("listExport"));
        return r;
    }

    public Map<String, String> toQueryParamMap() {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "searchFieldType", searchFieldType);
        put(m, "searchTranFactor", searchTranFactor);
        put(m, "searchTranValue", searchTranValue);
        put(m, "searchFromDate", searchFromDate != null ? searchFromDate.toString() : null);
        put(m, "searchToDate", searchToDate != null ? searchToDate.toString() : null);
        put(m, "searchCompField", searchCompField);
        put(m, "searchCompQ", searchCompQ);
        put(m, "searchMid", searchMid);
        put(m, "searchPayDivCd", searchPayDivCd);
        put(m, "searchPayProcCd", searchPayProcCd);
        put(m, "searchKeyword", searchKeyword);
        put(m, "searchPgCd", searchPgCd);
        put(m, "searchCycle", searchCycle);
        put(m, "searchRegNo", searchRegNo);
        put(m, "searchCardAprvNo", searchCardAprvNo);
        put(m, "searchChillTxnId", searchChillTxnId);
        put(m, "searchNotifyChannel", searchNotifyChannel);
        put(m, "searchOrderDir", searchOrderDir);
        put(m, "searchOrderBy", searchOrderBy);
        put(m, "payListVariant", payListVariant);
        m.put("page", String.valueOf(page));
        m.put("size", String.valueOf(size));
        return m;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v.trim());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean isTruthy(String s) {
        if (s == null || s.isBlank()) return false;
        String v = s.trim();
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "Y".equalsIgnoreCase(v);
    }

    public String getSearchTranFactor() { return searchTranFactor; }
    public void setSearchTranFactor(String searchTranFactor) { this.searchTranFactor = searchTranFactor; }
    public String getSearchFieldType() { return searchFieldType; }
    public void setSearchFieldType(String searchFieldType) { this.searchFieldType = searchFieldType; }
    public String getSearchTranValue() { return searchTranValue; }
    public void setSearchTranValue(String searchTranValue) { this.searchTranValue = searchTranValue; }
    public LocalDate getSearchFromDate() { return searchFromDate; }
    public void setSearchFromDate(LocalDate searchFromDate) { this.searchFromDate = searchFromDate; }
    public LocalDate getSearchToDate() { return searchToDate; }
    public void setSearchToDate(LocalDate searchToDate) { this.searchToDate = searchToDate; }
    public String getSearchCompField() { return searchCompField; }
    public void setSearchCompField(String searchCompField) { this.searchCompField = searchCompField; }
    public String getSearchCompQ() { return searchCompQ; }
    public void setSearchCompQ(String searchCompQ) { this.searchCompQ = searchCompQ; }
    public String getSearchMid() { return searchMid; }
    public void setSearchMid(String searchMid) { this.searchMid = searchMid; }
    public String getSearchTmnId() { return searchTmnId; }
    public void setSearchTmnId(String searchTmnId) { this.searchTmnId = searchTmnId; }
    public String getSearchPayDivCd() { return searchPayDivCd; }
    public void setSearchPayDivCd(String searchPayDivCd) { this.searchPayDivCd = searchPayDivCd; }
    public String getSearchPayProcCd() { return searchPayProcCd; }
    public void setSearchPayProcCd(String searchPayProcCd) { this.searchPayProcCd = searchPayProcCd; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }
    public String getSearchPgCd() { return searchPgCd; }
    public void setSearchPgCd(String searchPgCd) { this.searchPgCd = searchPgCd; }
    public String getSearchCycle() { return searchCycle; }
    public void setSearchCycle(String searchCycle) { this.searchCycle = searchCycle; }
    public String getSearchRegNo() { return searchRegNo; }
    public void setSearchRegNo(String searchRegNo) { this.searchRegNo = searchRegNo; }
    public String getSearchCardAprvNo() { return searchCardAprvNo; }
    public void setSearchCardAprvNo(String searchCardAprvNo) { this.searchCardAprvNo = searchCardAprvNo; }
    public String getSearchChillTxnId() { return searchChillTxnId; }
    public void setSearchChillTxnId(String searchChillTxnId) { this.searchChillTxnId = searchChillTxnId; }
    public String getSearchNotifyChannel() { return searchNotifyChannel; }
    public void setSearchNotifyChannel(String searchNotifyChannel) { this.searchNotifyChannel = searchNotifyChannel; }
    public String getSearchOrderDir() { return searchOrderDir; }
    public void setSearchOrderDir(String searchOrderDir) { this.searchOrderDir = searchOrderDir; }
    public String getSearchOrderBy() { return searchOrderBy; }
    public void setSearchOrderBy(String searchOrderBy) { this.searchOrderBy = searchOrderBy; }
    public String getPayListVariant() { return payListVariant; }
    public void setPayListVariant(String payListVariant) { this.payListVariant = payListVariant; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public boolean isSkipMeta() { return skipMeta; }
    public void setSkipMeta(boolean skipMeta) { this.skipMeta = skipMeta; }
    public boolean isListExport() { return listExport; }
    public void setListExport(boolean listExport) { this.listExport = listExport; }
    public String getAdminUiLocale() { return adminUiLocale; }
    public void setAdminUiLocale(String adminUiLocale) { this.adminUiLocale = adminUiLocale; }

    /** 일별 집계 등: 검색 조건만 복제(날짜·페이지는 호출부에서 덮어씀). */
    public static PayListSearchRequest shallowCopy(PayListSearchRequest src) {
        PayListSearchRequest r = new PayListSearchRequest();
        if (src == null) {
            return r;
        }
        r.searchFieldType = src.searchFieldType;
        r.searchTranFactor = src.searchTranFactor;
        r.searchTranValue = src.searchTranValue;
        r.searchFromDate = src.searchFromDate;
        r.searchToDate = src.searchToDate;
        r.searchCompField = src.searchCompField;
        r.searchCompQ = src.searchCompQ;
        r.searchMid = src.searchMid;
        r.searchTmnId = src.searchTmnId;
        r.searchPayDivCd = src.searchPayDivCd;
        r.searchPayProcCd = src.searchPayProcCd;
        r.searchKeyword = src.searchKeyword;
        r.searchPgCd = src.searchPgCd;
        r.searchCycle = src.searchCycle;
        r.searchRegNo = src.searchRegNo;
        r.searchCardAprvNo = src.searchCardAprvNo;
        r.searchChillTxnId = src.searchChillTxnId;
        r.searchNotifyChannel = src.searchNotifyChannel;
        r.searchOrderDir = src.searchOrderDir;
        r.searchOrderBy = src.searchOrderBy;
        r.payListVariant = src.payListVariant;
        r.page = src.page;
        r.size = src.size;
        r.skipMeta = src.skipMeta;
        r.listExport = src.listExport;
        r.adminUiLocale = src.adminUiLocale;
        return r;
    }
}
