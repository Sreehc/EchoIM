package com.echoim.server.dto.user;

import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequestDto {

    @NotBlank(message = "昵称不能为空")
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String signature;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
