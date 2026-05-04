package com.echoim.server.dto.group;

import jakarta.validation.constraints.NotNull;

public class ReviewJoinRequestDto {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
