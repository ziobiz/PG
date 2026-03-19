package com.pg.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 헤더 배경색·가운데 정렬·테두리·텍스트 서식(@)이 적용된 xlsx 생성.
 * 계좌번호·사업자번호 등 긴 숫자가 과학적 표기법으로 깨지지 않도록 텍스트 열만 지정한다.
 */
@Service
public class ExcelStyledExportService {

    private static final int MAX_EXPORT_ROWS = 15_000;
    private static final int MIN_COL_WIDTH = 3200;
    private static final int MAX_COL_WIDTH = 18000;

    public byte[] buildStyledTable(String sheetName, List<String> headers, List<List<String>> rows,
                                   Set<Integer> textColumnIndices) throws IOException {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("headers가 비어 있습니다.");
        }
        List<List<String>> safeRows = rows != null ? rows : Collections.emptyList();
        if (safeRows.size() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("행 수는 최대 " + MAX_EXPORT_ROWS + "건까지입니다.");
        }
        Set<Integer> textIdx = textColumnIndices != null ? textColumnIndices : Collections.emptySet();
        String safeSheet = sanitizeSheetName(sheetName);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(safeSheet);
            DataFormat df = wb.createDataFormat();

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(headerStyle);
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(hFont);
            headerStyle.setWrapText(true);

            CellStyle dataCenterStyle = wb.createCellStyle();
            dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            dataCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(dataCenterStyle);
            dataCenterStyle.setWrapText(true);

            CellStyle dataTextStyle = wb.createCellStyle();
            dataTextStyle.cloneStyleFrom(dataCenterStyle);
            dataTextStyle.setDataFormat(df.getFormat("@"));

            int colCount = headers.size();
            Row hRow = sheet.createRow(0);
            hRow.setHeightInPoints(24);
            for (int i = 0; i < colCount; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers.get(i) != null ? headers.get(i) : "");
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            for (List<String> line : safeRows) {
                Row dataRow = sheet.createRow(r++);
                dataRow.setHeightInPoints(18);
                for (int i = 0; i < colCount; i++) {
                    Cell cell = dataRow.createCell(i);
                    String val = "";
                    if (line != null && i < line.size() && line.get(i) != null) {
                        val = line.get(i);
                    }
                    boolean asText = textIdx.contains(i);
                    cell.setCellValue(val);
                    cell.setCellStyle(asText ? dataTextStyle : dataCenterStyle);
                }
            }

            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                if (w < MIN_COL_WIDTH) {
                    sheet.setColumnWidth(i, MIN_COL_WIDTH);
                } else if (w > MAX_COL_WIDTH) {
                    sheet.setColumnWidth(i, MAX_COL_WIDTH);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /** 업체 엑셀등록용 SAMPLE (CompExcelImportService 헤더와 동일) */
    public byte[] buildCompRegisterSample() throws IOException {
        List<String> headers = List.of(
                "업체명", "업체구분", "상위코드", "사업자번호", "대표자명", "연락처", "업체전화", "이메일",
                "주소", "상세주소", "우편번호", "은행", "계좌번호", "예금주", "이체수수료", "정산주기", "이체구분",
                "사용여부", "로그인ID", "비밀번호", "비고");
        List<String> sample = List.of(
                "샘플가맹점_SAMPLE", "가맹점", "R0001", "123-45-67890", "홍길동", "010-0000-0000", "02-0000-0000",
                "sample@example.com", "서울시 강남구", "테헤란로 1", "06234", "04", "11012345678901", "홍길동", "0",
                "D1", "일반이체", "Y", "sample_login", "변경필수123", "SAMPLE행_삭제후입력");
        // 코드·숫자처럼 보이는 열은 텍스트(@)로 저장 (과학적 표기법 방지)
        Set<Integer> textCols = new HashSet<>();
        textCols.add(2);  // 상위코드
        textCols.add(3);  // 사업자번호
        textCols.add(5);  // 연락처
        textCols.add(6);  // 업체전화
        textCols.add(10); // 우편번호
        textCols.add(11); // 은행
        textCols.add(12); // 계좌번호
        textCols.add(18); // 로그인ID
        textCols.add(19); // 비밀번호
        return buildStyledTable("업체등록_SAMPLE", headers, List.of(sample), textCols);
    }

    private static void setThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) {
            return "Sheet1";
        }
        String n = name.replaceAll("[\\\\/*?\\[\\]:]", "_").trim();
        if (n.length() > 31) {
            n = n.substring(0, 31);
        }
        return n.isEmpty() ? "Sheet1" : n;
    }
}
