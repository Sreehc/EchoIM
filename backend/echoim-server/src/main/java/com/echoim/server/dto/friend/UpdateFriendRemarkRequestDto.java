package com.echoim.server.dto.friend;

import jakarta.validation.constraints.Size;

public class UpdateFriendRemarkRequestDto {

    @Size(max = 100, message = "备注长度不能超过 100")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
