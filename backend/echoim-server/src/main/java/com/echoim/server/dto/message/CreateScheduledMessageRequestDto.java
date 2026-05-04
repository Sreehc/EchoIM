package com.echoim.server.dto.message;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class CreateScheduledMessageRequestDto {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @NotNull(message = "消息类型不能为空")
    private Integer msgType;

    @Size(max = 500, message = "消息内容不能超过 500 字")
    private String content;

    private Long fileId;

    private Object extraJson;

    @NotNull(message = "计划发送时间不能为空")
    @Future(message = "计划发送时间必须是未来时间")
    private LocalDateTime scheduledAt;

    private List<MentionDto> mentions;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Object getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(Object extraJson) {
        this.extraJson = extraJson;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public List<MentionDto> getMentions() {
        return mentions;
    }

    public void setMentions(List<MentionDto> mentions) {
        this.mentions = mentions;
    }

    public static class MentionDto {
        private Long userId;
        private String displayName;
        private Integer startIndex;
        private Integer length;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Integer getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(Integer startIndex) {
            this.startIndex = startIndex;
        }

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }
    }
}
