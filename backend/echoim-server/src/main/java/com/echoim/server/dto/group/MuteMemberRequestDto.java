package com.echoim.server.dto.group;

public class MuteMemberRequestDto {

    private Integer durationMinutes;

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
