package com.echoim.server.vo.auth;

public class LoginResponseVo {

    private String token;
    private String tokenType;
    private long expiresIn;
    private LoginUserVo userInfo;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LoginUserVo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(LoginUserVo userInfo) {
        this.userInfo = userInfo;
    }
}
