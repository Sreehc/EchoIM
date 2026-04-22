package com.echoim.server.dto.friend;

import jakarta.validation.constraints.NotNull;

public class FriendRequestActionDto {

    @NotNull(message = "好友申请ID不能为空")
    private Long requestId;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}
