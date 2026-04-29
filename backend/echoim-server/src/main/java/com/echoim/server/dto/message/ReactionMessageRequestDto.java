package com.echoim.server.dto.message;

import jakarta.validation.constraints.NotBlank;

public class ReactionMessageRequestDto {

    @NotBlank(message = "表情不能为空")
    private String emoji;

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
