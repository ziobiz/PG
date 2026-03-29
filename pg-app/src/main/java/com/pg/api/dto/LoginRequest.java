package com.pg.api.dto;

public class LoginRequest {
    private String username;
    private String password;
    /** 브라우저 location.host — 조직에 관리자(웹) URL이 있으면 호스트 일치 시에만 로그인 허용 */
    private String clientHost;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getClientHost() { return clientHost; }
    public void setClientHost(String clientHost) { this.clientHost = clientHost; }
}
