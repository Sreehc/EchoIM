package com.echoim.server.vo.auth;

public class LoginResponseVo {

    private String status;
    private String token;
    private String tokenType;
    private long expiresIn;
    private LoginUserVo userInfo;
    private String challengeTicket;
    private String maskedEmail;
    private Integer resendAfterSeconds;
    private String trustedDeviceGrantToken;
    private String trustedDeviceExpireAt;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public String getChallengeTicket() {
        return challengeTicket;
    }

    public void setChallengeTicket(String challengeTicket) {
        this.challengeTicket = challengeTicket;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public Integer getResendAfterSeconds() {
        return resendAfterSeconds;
    }

    public void setResendAfterSeconds(Integer resendAfterSeconds) {
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public String getTrustedDeviceGrantToken() {
        return trustedDeviceGrantToken;
    }

    public void setTrustedDeviceGrantToken(String trustedDeviceGrantToken) {
        this.trustedDeviceGrantToken = trustedDeviceGrantToken;
    }

    public String getTrustedDeviceExpireAt() {
        return trustedDeviceExpireAt;
    }

    public void setTrustedDeviceExpireAt(String trustedDeviceExpireAt) {
        this.trustedDeviceExpireAt = trustedDeviceExpireAt;
    }
}
