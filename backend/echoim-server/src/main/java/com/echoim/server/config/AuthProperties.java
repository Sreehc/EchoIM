package com.echoim.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "echoim.auth")
public class AuthProperties {

    private String mailFrom;
    private int codeExpireSeconds = 600;
    private int resendCooldownSeconds = 60;
    private int maxVerifyAttempts = 5;
    private int trustedDeviceExpireDays = 30;

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public int getCodeExpireSeconds() {
        return codeExpireSeconds;
    }

    public void setCodeExpireSeconds(int codeExpireSeconds) {
        this.codeExpireSeconds = codeExpireSeconds;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getTrustedDeviceExpireDays() {
        return trustedDeviceExpireDays;
    }

    public void setTrustedDeviceExpireDays(int trustedDeviceExpireDays) {
        this.trustedDeviceExpireDays = trustedDeviceExpireDays;
    }
}
