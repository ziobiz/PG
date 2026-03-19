package com.pg.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 그리드 등 클라이언트에서 전달하는 서식 엑셀보내기 요청
 */
public class StyledExcelExportRequest {

    private String sheetName = "목록";
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    /** 0부터 시작하는 열 인덱스 — 해당 열은 셀 서식을 텍스트(@)로 지정 */
    private List<Integer> textColumnIndexes = new ArrayList<>();

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    public List<Integer> getTextColumnIndexes() {
        return textColumnIndexes;
    }

    public void setTextColumnIndexes(List<Integer> textColumnIndexes) {
        this.textColumnIndexes = textColumnIndexes;
    }
}
