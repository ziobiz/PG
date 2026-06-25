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

    private static final String[] HEADER_MARKERS = {
            "Transaction ID", "Merchant Order Number", "Gateway Access Number",
            "交易流水号", "交易号", "商户订单号", "网关接入号"
    };

    public record ParseReport(
            List<Map<String, String>> rows,
            int headerRowIndex,
            List<String> headers,
            int dataRowsScanned,
            int dataRowsAccepted,
            int sheetRowCount
    ) {}

    public List<Map<String, String>> parseFile(Path path) throws Exception {
        return parseFileReport(path).rows();
    }

    public ParseReport parseFileReport(Path path) throws Exception {
        try (InputStream is = Files.newInputStream(path)) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return parseStreamReport(is, name);
        }
    }

    public List<Map<String, String>> parseMultipart(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        try (InputStream is = file.getInputStream()) {
            return parseStreamReport(is, name).rows();
        }
    }

    private ParseReport parseStreamReport(InputStream is, String filename) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        int headerIdx = 0;
        List<String> headers = List.of();
        int scanned = 0;
        int sheetRows = 0;
        try (Workbook wb = filename.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return new ParseReport(rows, headerIdx, headers, scanned, rows.size(), sheetRows);
            }
            sheetRows = sheet.getLastRowNum() + 1;
            headerIdx = findHeaderRowIndex(sheet);
            Row headerRow = sheet.getRow(headerIdx);
            if (headerRow == null) {
                return new ParseReport(rows, headerIdx, headers, scanned, rows.size(), sheetRows);
            }
            headers = readHeaderRow(headerRow);
            if (headers.isEmpty()) {
                return new ParseReport(rows, headerIdx, headers, scanned, rows.size(), sheetRows);
            }
            for (int i = headerIdx + 1; i <= sheet.getLastRowNum(); i++) {
                scanned++;
                Map<String, String> m = readDataRow(sheet.getRow(i), headers);
                canonicalizeRowKeys(m);
                if (hasExportDataCell(m)) {
                    rows.add(m);
                }
            }
        }
        return new ParseReport(rows, headerIdx, headers, scanned, rows.size(), sheetRows);
    }

    private static List<String> readHeaderRow(Row headerRow) {
        int lastCol = Math.max(headerRow.getLastCellNum(), 0);
        List<String> headers = new ArrayList<>(lastCol);
        for (int j = 0; j < lastCol; j++) {
            headers.add(normalizeHeaderKey(getCellString(headerRow.getCell(j))));
        }
        return headers;
    }

    private static Map<String, String> readDataRow(Row row, List<String> headers) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int j = 0; j < headers.size(); j++) {
            String key = headers.get(j);
            if (key == null || key.isBlank()) {
                continue;
            }
            if (m.containsKey(key)) {
                key = key + "@" + j;
            }
            String val = row == null ? "" : getCellString(row.getCell(j));
            m.put(key, val != null ? val.trim() : "");
        }
        return m;
    }

    /** 영문·중문 헤더를 표준 키로 복제해 col() 조회가 안정적으로 동작하게 함 */
    private static void canonicalizeRowKeys(Map<String, String> row) {
        if (row == null || row.isEmpty()) {
            return;
        }
        Map<String, String> extras = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : row.entrySet()) {
            String canon = canonicalHeaderName(e.getKey());
            if (!canon.isBlank() && e.getValue() != null && !e.getValue().isBlank()) {
                extras.putIfAbsent(canon, e.getValue().trim());
            }
        }
        row.putAll(extras);
    }

    private static String canonicalHeaderName(String header) {
        if (header == null || header.isBlank()) {
            return "";
        }
        String nk = normalizeKey(header);
        if (nk.contains("transaction") && nk.contains("id")) {
            return "Transaction ID";
        }
        if (nk.contains("merchant") && nk.contains("order")) {
            return "Merchant Order Number";
        }
        if (nk.contains("gateway") && nk.contains("access")) {
            return "Gateway Access Number";
        }
        if (nk.contains("transaction") && nk.contains("date")) {
            return "Transaction Date";
        }
        if (nk.contains("transaction") && nk.contains("amount")) {
            return "Transaction Amount";
        }
        if (nk.contains("transaction") && nk.contains("currency")) {
            return "Transaction Currency";
        }
        if (nk.equals("fee") || nk.contains("fee")) {
            return "Fee";
        }
        if (nk.contains("trading") && nk.contains("status")) {
            return "Trading Status";
        }
        if (nk.contains("refund") && nk.contains("status")) {
            return "Refund Status";
        }
        if (nk.contains("chargeback")) {
            return "Is it a chargeback?";
        }
        if (nk.contains("card") && nk.contains("bin")) {
            return "Card BIN";
        }
        if (nk.contains("url") && nk.contains("source")) {
            return "URL Source";
        }
        if (nk.contains("original") && nk.contains("currency")) {
            return "Original Currency";
        }
        if (nk.contains("交易") && (nk.contains("流水") || nk.contains("号"))) {
            return "Transaction ID";
        }
        if (nk.contains("商户") && nk.contains("订单")) {
            return "Merchant Order Number";
        }
        if (nk.contains("网关") && nk.contains("接入")) {
            return "Gateway Access Number";
        }
        if (nk.contains("交易") && nk.contains("日期")) {
            return "Transaction Date";
        }
        if (nk.contains("交易") && nk.contains("金额")) {
            return "Transaction Amount";
        }
        return "";
    }

    static boolean hasExportDataCell(Map<String, String> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return !col(row,
                "Transaction ID", "transaction_id", "交易流水号", "交易号").isBlank()
                || !col(row,
                "Merchant Order Number", "pay_orderid", "orderid", "商户订单号").isBlank();
    }

    private static int findHeaderRowIndex(Sheet sheet) {
        int max = Math.min(15, sheet.getLastRowNum());
        for (int i = 0; i <= max; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            int hits = 0;
            int lastCol = Math.max(row.getLastCellNum(), 0);
            for (int j = 0; j < lastCol; j++) {
                String t = normalizeHeaderKey(getCellString(row.getCell(j)));
                if (t.isBlank()) {
                    continue;
                }
                for (String marker : HEADER_MARKERS) {
                    if (t.equalsIgnoreCase(marker) || t.contains(marker)) {
                        hits++;
                        break;
                    }
                }
            }
            if (hits >= 2) {
                return i;
            }
        }
        return 0;
    }

    private static String normalizeHeaderKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("\uFEFF", "").replaceAll("\\s+", " ").trim();
    }

    private static String normalizeKey(String key) {
        String k = normalizeHeaderKey(key);
        int at = k.lastIndexOf('@');
        if (at > 0 && at < k.length() - 1) {
            String tail = k.substring(at + 1);
            if (tail.chars().allMatch(Character::isDigit)) {
                k = k.substring(0, at);
            }
        }
        return k.toLowerCase(Locale.ROOT);
    }

    public static String col(Map<String, String> row, String... keys) {
        if (row == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null || k.isBlank()) {
                continue;
            }
            String exact = colExact(row, k);
            if (!exact.isBlank()) {
                return exact;
            }
            String fuzzy = colFuzzy(row, tokenizeKey(k));
            if (!fuzzy.isBlank()) {
                return fuzzy;
            }
        }
        return "";
    }

    private static String colExact(Map<String, String> row, String key) {
        String want = normalizeKey(key);
        for (Map.Entry<String, String> e : row.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            if (normalizeKey(e.getKey()).equals(want)) {
                String v = e.getValue();
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        }
        return "";
    }

    private static List<String> tokenizeKey(String key) {
        String norm = normalizeKey(key);
        if (norm.isBlank()) {
            return List.of();
        }
        String[] parts = norm.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    private static String colFuzzy(Map<String, String> row, List<String> tokens) {
        if (row == null || tokens == null || tokens.isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            String k = e.getKey();
            if (k == null || k.startsWith("_")) {
                continue;
            }
            String nk = normalizeKey(k);
            boolean ok = true;
            for (String t : tokens) {
                if (!nk.contains(t)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }
            String v = e.getValue();
            if (v != null && !v.isBlank()) {
                return v.trim();
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
                String plain = java.math.BigDecimal.valueOf(d).toPlainString();
                yield plain;
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> readFormulaCell(cell);
            default -> "";
        };
    }

    private static String readFormulaCell(Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
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
                default -> "";
            };
        } catch (Exception e) {
            try {
                return cell.getStringCellValue();
            } catch (Exception ignored) {
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception ignored2) {
                    return "";
                }
            }
        }
    }
}
