package com.pg.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NoticeListDto {
    private Long id;
    private String title;
    private String regDt;
    private int hitCnt;
    private String compNm;
    private String compId;
    /** Y/N — 로그인 첫 화면 노출 */
    private String showOnLogin;
    /** Y/N — 로그인 팝업 */
    private String showAsPopup;
    private String writerNm;

    public static NoticeListDto from(com.pg.entity.Notice n) {
        return from(n, null);
    }

    public static NoticeListDto from(com.pg.entity.Notice n, com.pg.entity.OrgUnit ou) {
        NoticeListDto d = new NoticeListDto();
        d.setId(n.getId());
        d.setTitle(n.getTitle());
        d.setRegDt(n.getRegDt() != null ? n.getRegDt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ") : null);
        d.setHitCnt(n.getHitCnt());
        d.setShowOnLogin(n.getShowOnLogin());
        d.setShowAsPopup(n.getShowAsPopup());
        d.setWriterNm(n.getWriterNm());
        if (ou != null) {
            d.setCompNm(ou.getName());
            d.setCompId(ou.getCode());
        }
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
    public String getCompNm() { return compNm; }
    public void setCompNm(String compNm) { this.compNm = compNm; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getShowOnLogin() { return showOnLogin; }
    public void setShowOnLogin(String showOnLogin) { this.showOnLogin = showOnLogin; }
    public String getShowAsPopup() { return showAsPopup; }
    public void setShowAsPopup(String showAsPopup) { this.showAsPopup = showAsPopup; }
    public String getWriterNm() { return writerNm; }
    public void setWriterNm(String writerNm) { this.writerNm = writerNm; }
}
