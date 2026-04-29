package com.echoim.server.dto.user;

import jakarta.validation.constraints.NotBlank;

public class UsernameCheckRequestDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
