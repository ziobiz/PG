package com.pg.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * JPAY 가맹 포털 Export — {@code Merchant Orders List_*.xlsx} 파싱.
 */
@Service
public class JpayOrderExcelParseService {

    public List<Map<String, String>> parseFile(Path path) throws Exception {
        try (InputStream is = Files.newInputStream(path)) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return parseStream(is, name);
        }
    }

    public List<Map<String, String>> parseMultipart(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        try (InputStream is = file.getInputStream()) {
            return parseStream(is, name);
        }
    }

    private List<Map<String, String>> parseStream(InputStream is, String filename) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook wb = filename.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return rows;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return rows;
            }
            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) {
                headers.add(getCellString(c).trim());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> m = new LinkedHashMap<>();
                boolean hasAny = false;
                for (int j = 0; j < headers.size(); j++) {
                    String key = headers.get(j);
                    if (key == null || key.isBlank()) {
                        continue;
                    }
                    String val = j < row.getLastCellNum() ? getCellString(row.getCell(j)) : "";
                    if (val != null && !val.trim().isEmpty()) {
                        hasAny = true;
                    }
                    m.put(key, val != null ? val.trim() : "");
                }
                if (hasAny) {
                    rows.add(m);
                }
            }
        }
        return rows;
    }

    public static String col(Map<String, String> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            for (Map.Entry<String, String> e : row.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(k)) {
                    String v = e.getValue();
                    if (v != null && !v.isBlank()) {
                        return v.trim();
                    }
                }
            }
        }
        return "";
    }

    private static String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString().replace('T', ' ');
                }
                double d = cell.getNumericCellValue();
                if (d == (long) d) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }
}
