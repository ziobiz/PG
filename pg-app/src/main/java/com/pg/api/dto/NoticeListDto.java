package com.pg.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NoticeListDto {
    private Long id;
    private String title;
    private String regDt;
    private int hitCnt;

    public static NoticeListDto from(com.pg.entity.Notice n) {
        NoticeListDto d = new NoticeListDto();
        d.setId(n.getId());
        d.setTitle(n.getTitle());
        d.setRegDt(n.getRegDt() != null ? n.getRegDt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ") : null);
        d.setHitCnt(n.getHitCnt());
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRegDt() { return regDt; }
    public void setRegDt(String regDt) { this.regDt = regDt; }
    public int getHitCnt() { return hitCnt; }
    public void setHitCnt(int hitCnt) { this.hitCnt = hitCnt; }
}
