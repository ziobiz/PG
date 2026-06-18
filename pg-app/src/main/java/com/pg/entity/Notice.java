package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 공지사항 (PG_*_NOTICE 스타일)
 */
@Entity
@Table(name = "pg_notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "hit_cnt")
    private int hitCnt = 0;

    /** 작성 시점 로그인 조직(tb_org_unit.id). 없으면 구 데이터 */
    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Y: 로그인 첫 화면에 공지 노출(동시에 하나만 Y 권장 — 저장 시 다른 행은 N으로 정리) */
    @Column(name = "show_on_login", nullable = false, length = 1)
    private String showOnLogin = "N";

    /** {"titles":{"KO":"…","EN":"…",…},"bodies":{"KO":"…",…}} — LLM 번역 캐시 */
    @Column(name = "login_i18n_json", columnDefinition = "TEXT")
    private String loginI18nJson;

    /** Y: 로그인 페이지 접속팝업(동시에 하나만 Y, 총본사 전용) */
    @Column(name = "show_as_popup", nullable = false, length = 1)
    private String showAsPopup = "N";

    /** Y: 로그인 완료 후 메인 팝업(동시에 하나만 Y) */
    @Column(name = "show_post_login_popup", nullable = false, length = 1)
    private String showPostLoginPopup = "N";

    /** Y: 메인 대시보드 공지(동시에 하나만 Y) */
    @Column(name = "show_on_main", nullable = false, length = 1)
    private String showOnMain = "N";

    /** 작성자 표시명(로그인 사용자 이름·없으면 아이디) */
    @Column(name = "writer_nm", length = 100)
    private String writerNm;

    /** 배포 대상 코드 — {@link com.pg.notice.NoticeDeployTarget} */
    @Column(name = "deploy_target", length = 30)
    private String deployTarget;

    /** NOTI 배포 시 대상 tb_org_unit.id JSON 배열 */
    @Column(name = "target_org_unit_ids_json", columnDefinition = "TEXT")
    private String targetOrgUnitIdsJson;

    @PrePersist
    protected void onCreate() {
        if (regDt == null) regDt = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getHitCnt() { return hitCnt; }
    public void setHitCnt(int hitCnt) { this.hitCnt = hitCnt; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getShowOnLogin() { return showOnLogin; }
    public void setShowOnLogin(String showOnLogin) { this.showOnLogin = showOnLogin; }
    public String getLoginI18nJson() { return loginI18nJson; }
    public void setLoginI18nJson(String loginI18nJson) { this.loginI18nJson = loginI18nJson; }
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
    public String getTargetOrgUnitIdsJson() { return targetOrgUnitIdsJson; }
    public void setTargetOrgUnitIdsJson(String targetOrgUnitIdsJson) { this.targetOrgUnitIdsJson = targetOrgUnitIdsJson; }
}
