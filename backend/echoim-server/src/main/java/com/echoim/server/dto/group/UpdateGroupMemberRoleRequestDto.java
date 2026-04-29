package com.echoim.server.dto.group;

import jakarta.validation.constraints.NotNull;

public class UpdateGroupMemberRoleRequestDto {

    @NotNull(message = "角色不能为空")
    private Integer role;

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }
}
