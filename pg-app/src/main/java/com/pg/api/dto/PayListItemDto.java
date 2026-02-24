package com.pg.api.dto;

import com.pg.entity.PgTrnsctn;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        row.put("payDivNm", "10".equals(t.getStatus()) ? "결제" : "20".equals(t.getStatus()) ? "취소" : t.getStatus());
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
        return row;
    }
}
