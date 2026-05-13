package com.echoim.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "echoim.admin")
public class AdminProperties {

    private String superUsername;
    private String superPassword;
    private String superNickname = "系统管理员";

    public String getSuperUsername() {
        return superUsername;
    }

    public void setSuperUsername(String superUsername) {
        this.superUsername = superUsername;
    }

    public String getSuperPassword() {
        return superPassword;
    }

    public void setSuperPassword(String superPassword) {
        this.superPassword = superPassword;
    }

    public String getSuperNickname() {
        return superNickname;
    }

    public void setSuperNickname(String superNickname) {
        this.superNickname = superNickname;
    }
}
