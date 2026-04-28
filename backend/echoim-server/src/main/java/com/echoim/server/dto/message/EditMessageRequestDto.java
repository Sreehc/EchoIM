package com.echoim.server.dto.message;

import jakarta.validation.constraints.NotBlank;

public class EditMessageRequestDto {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
