package com.echoim.server.dto.group;

public class CreateInviteLinkRequestDto {

    private Integer maxUses;

    private Integer expireHours;

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(Integer expireHours) {
        this.expireHours = expireHours;
    }
}
