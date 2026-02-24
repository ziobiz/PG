package com.pg.api.dto;

import com.pg.entity.OrgUnit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CompListItemDto {

    public static Map<String, Object> from(OrgUnit o) {
        Map<String, Object> row = new HashMap<>();
        row.put("compId", o.getCode());
        row.put("compNm", o.getName());
        row.put("compDiv", o.getOrgLevel() != null ? o.getOrgLevel().name() : "-");
        row.put("regDt", o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE).replace("T", " ") : null);
        return row;
    }
}
