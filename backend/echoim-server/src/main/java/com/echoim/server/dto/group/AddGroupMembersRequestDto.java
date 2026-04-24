package com.echoim.server.dto.group;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AddGroupMembersRequestDto {

    @NotEmpty(message = "待添加成员不能为空")
    private List<Long> memberIds;

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}
