package com.pg.service;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * 업체관리 엑셀 업로드 파싱 및 등록
 * 엑셀 1행: 헤더 (업체코드, 업체명, 업체구분, 상위코드, 사업자번호, 대표자명, 연락처 등)
 * 2행~: 데이터
 */
@Service
public class CompExcelImportService {

    private static final Map<String, String> COMP_DIV_MAP = Map.ofEntries(
            Map.entry("REGIONAL", "REGIONAL"),
            Map.entry("본사", "REGIONAL"),
            Map.entry("MASTER_DIST", "MASTER_DIST"),
            Map.entry("총판", "MASTER_DIST"),
            Map.entry("BRANCH", "BRANCH"),
            Map.entry("지사", "BRANCH"),
            Map.entry("AGENCY", "AGENCY"),
            Map.entry("대리점", "AGENCY"),
            Map.entry("SALES_OFFICE", "SALES_OFFICE"),
            Map.entry("영업점", "SALES_OFFICE"),
            Map.entry("MERCHANT", "MERCHANT"),
            Map.entry("가맹점", "MERCHANT")
    );

    /** 엑셀 시트에서 업체 행 목록 추출. 헤더는 1행, 데이터는 2행부터 */
    public List<Map<String, String>> parseExcel(MultipartFile file) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try (InputStream is = file.getInputStream();
             Workbook wb = name.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return rows;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return rows;

            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) {
                headers.add(getCellString(c).trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> m = new LinkedHashMap<>();
                boolean hasAny = false;
                for (int j = 0; j < headers.size(); j++) {
                    String val = j < row.getLastCellNum() ? getCellString(row.getCell(j)) : "";
                    if (val != null && !val.trim().isEmpty()) hasAny = true;
                    m.put(headers.get(j), val != null ? val.trim() : "");
                }
                if (hasAny) rows.add(m);
            }
        }
        return rows;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == (long) d) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.toString();
                }
            }
            default -> "";
        };
    }

    /** 한글/영문 헤더명 → 표준 키 매핑 */
    public static String mapHeaderToKey(String header) {
        if (header == null || header.isEmpty()) return null;
        String h = header.trim();
        return switch (h) {
            case "업체코드", "compId", "코드" -> "compId";
            case "업체명", "compNm", "comp_name" -> "compNm";
            case "업체구분", "compDiv", "구분" -> "compDiv";
            case "상위코드", "상위업체코드", "parentComp", "parentCode", "parent" -> "parentComp";
            case "사업자번호", "regNo", "reg_no" -> "regNo";
            case "대표자명", "ceoNm", "ceo_nm" -> "ceoNm";
            case "연락처", "휴대폰", "ceoMobile", "contact", "ceo_mobile" -> "ceoMobile";
            case "업체전화", "compTel", "comp_tel", "전화" -> "compTel";
            case "이메일", "email" -> "email";
            case "주소", "addr", "address" -> "addr";
            case "상세주소", "addrDetail", "addr_detail" -> "addrDetail";
            case "우편번호", "zipCode", "zip_code" -> "zipCode";
            case "은행", "bankCd", "bank_cd", "bankNm" -> "bankCd";
            case "계좌번호", "accountNo", "account_no" -> "accountNo";
            case "예금주", "accountHolder", "account_holder" -> "accountHolder";
            case "이체수수료", "transferFee", "transfer_fee" -> "transferFee";
            case "정산주기", "calcCycle", "calc_cycle" -> "calcCycle";
            case "이체구분", "transferType", "transfer_type" -> "transferType";
            case "사용여부", "useYn", "use_yn" -> "useYn";
            case "로그인ID", "loginId", "login_id" -> "loginId";
            case "비밀번호", "pwd", "password" -> "pwd";
            case "비고", "remark" -> "remark";
            default -> null;
        };
    }

    /** compDiv 문자열 정규화 (한글→영문) */
    public static String normalizeCompDiv(String compDiv) {
        if (compDiv == null || compDiv.isEmpty()) return null;
        String key = compDiv.trim();
        return COMP_DIV_MAP.getOrDefault(key.toUpperCase(), COMP_DIV_MAP.get(key));
    }

    /** 행 Map을 표준 키로 변환 */
    public Map<String, String> toStandardRow(Map<String, String> raw) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            String key = mapHeaderToKey(e.getKey());
            if (key != null && e.getValue() != null && !e.getValue().isEmpty()) {
                out.put(key, e.getValue());
            }
        }
        if (out.containsKey("compDiv")) {
            String normalized = normalizeCompDiv(out.get("compDiv"));
            if (normalized != null) out.put("compDiv", normalized);
        }
        return out;
    }
}
