package com.echoim.server.dto.friend;

import jakarta.validation.constraints.NotNull;

public class CreateFriendRequestDto {

    @NotNull(message = "目标用户不能为空")
    private Long toUserId;

    private String applyMsg;

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getApplyMsg() {
        return applyMsg;
    }

    public void setApplyMsg(String applyMsg) {
        this.applyMsg = applyMsg;
    }
}
