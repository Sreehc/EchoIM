package com.echoim.server.common.auth;

public class LoginUser {

    private Long userId;
    private String username;
    private String tokenType;
    private String roleCode;
    private Long expireAtMillis;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Long getExpireAtMillis() {
        return expireAtMillis;
    }

    public void setExpireAtMillis(Long expireAtMillis) {
        this.expireAtMillis = expireAtMillis;
    }
}
