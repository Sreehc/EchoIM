package com.echoim.server.dto.group;

public class UpdateGroupRequestDto {

    private String groupName;
    private String notice;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
