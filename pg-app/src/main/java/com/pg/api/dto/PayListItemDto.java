package com.pg.api.dto;

import com.pg.entity.PgTrnsctn;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 결제내역 그리드 한 행 (프론트 columns 키와 매핑)
 */
public class PayListItemDto {

    public static Map<String, Object> from(PgTrnsctn t, String compNm) {
        Map<String, Object> row = new HashMap<>();
        row.put("compDivCode9", compNm != null ? compNm : t.getMerchantId());
        row.put("compId", t.getMerchantId());
        row.put("compRegDivNm", "-");
        row.put("compRegNo", "-");
        row.put("payDivNm", payDivLabel(t.getStatus()));
        row.put("payProcNm", "정산대기");
        row.put("payCard", "-");
        row.put("cardAprvNo", t.getApprovalNo());
        row.put("payCardNo", "-");
        row.put("instalMonth", "0");
        row.put("payMethod", "카드");
        row.put("corpNm", compNm);
        row.put("pgNm", t.getVan() != null ? t.getVan() : "-");
        row.put("calcCycle", "-");
        String payDtStr = t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ") : null;
        row.put("payAprv", payDtStr);
        row.put("payDttm", payDtStr);
        row.put("paySeq", t.getTrnId() != null ? t.getTrnId() : (t.getPayNo() != null ? t.getPayNo() : "-"));
        row.put("payAmount", t.getAmtKrw() != null ? t.getAmtKrw().longValue() : 0);
        row.put("origin", t.getOrigin() != null ? t.getOrigin() : "CHILL");
        return row;
    }

    private static String payDivLabel(String status) {
        if (status == null) {
            return "-";
        }
        switch (status) {
            case "10":
                return "결제";
            case "20":
                return "취소";
            case "30":
                return "환불";
            case "31":
                return "강제환불";
            case "40":
                return "자동무효";
            case "41":
                return "이메일무효";
            case "42":
                return "자동환불";
            case "F0":
            case "99":
                return "실패";
            default:
                return status;
        }
    }
}
