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
    /** Y/N — 로그인 페이지 접속팝업 */
    private String showAsPopup;
    /** Y/N — 로그인 완료 후 팝업 */
    private String showPostLoginPopup;
    /** Y/N — 메인 대시보드 공지 */
    private String showOnMain;
    private String writerNm;
    private String deployTarget;
    private String deployTargetLabel;
    /** Y — 전체 중 최신 1건(기간 밖이어도 목록 상단 고정) */
    private String recentPinned;

    public static NoticeListDto from(com.pg.entity.Notice n) {
        return from(n, null, null, false);
    }

    public static NoticeListDto from(com.pg.entity.Notice n, com.pg.entity.OrgUnit ou) {
        return from(n, ou, null, false);
    }

    public static NoticeListDto from(com.pg.entity.Notice n, com.pg.entity.OrgUnit ou, String deployTargetLabel) {
        return from(n, ou, deployTargetLabel, false);
    }

    public static NoticeListDto from(com.pg.entity.Notice n, com.pg.entity.OrgUnit ou, String deployTargetLabel,
                                     boolean recentPinned) {
        NoticeListDto d = new NoticeListDto();
        d.setId(n.getId());
        d.setTitle(n.getTitle());
        d.setRegDt(n.getRegDt() != null ? n.getRegDt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ") : null);
        d.setHitCnt(n.getHitCnt());
        d.setShowOnLogin(n.getShowOnLogin());
        d.setShowAsPopup(n.getShowAsPopup());
        d.setShowPostLoginPopup(n.getShowPostLoginPopup());
        d.setShowOnMain(n.getShowOnMain());
        d.setWriterNm(n.getWriterNm());
        d.setDeployTarget(n.getDeployTarget());
        d.setDeployTargetLabel(deployTargetLabel);
        d.setRecentPinned(recentPinned ? "Y" : "N");
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
    public String getShowPostLoginPopup() { return showPostLoginPopup; }
    public void setShowPostLoginPopup(String showPostLoginPopup) { this.showPostLoginPopup = showPostLoginPopup; }
    public String getShowOnMain() { return showOnMain; }
    public void setShowOnMain(String showOnMain) { this.showOnMain = showOnMain; }
    public String getWriterNm() { return writerNm; }
    public void setWriterNm(String writerNm) { this.writerNm = writerNm; }
    public String getDeployTarget() { return deployTarget; }
    public void setDeployTarget(String deployTarget) { this.deployTarget = deployTarget; }
    public String getDeployTargetLabel() { return deployTargetLabel; }
    public void setDeployTargetLabel(String deployTargetLabel) { this.deployTargetLabel = deployTargetLabel; }
    public String getRecentPinned() { return recentPinned; }
    public void setRecentPinned(String recentPinned) { this.recentPinned = recentPinned; }
}
