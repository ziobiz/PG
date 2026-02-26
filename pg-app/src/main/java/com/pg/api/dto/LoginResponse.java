package com.pg.api.dto;

public class LoginResponse {
    private String token;
    private String userId;
    private String userNm;
    private Long orgUnitId;
    private String compId;
    private String orgLevel;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserNm() { return userNm; }
    public void setUserNm(String userNm) { this.userNm = userNm; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getOrgLevel() { return orgLevel; }
    public void setOrgLevel(String orgLevel) { this.orgLevel = orgLevel; }
}
