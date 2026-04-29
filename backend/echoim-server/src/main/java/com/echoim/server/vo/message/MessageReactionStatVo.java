package com.echoim.server.vo.message;

public class MessageReactionStatVo {

    private Long messageId;
    private String emoji;
    private Integer count;
    private Boolean reacted;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getReacted() {
        return reacted;
    }

    public void setReacted(Boolean reacted) {
        this.reacted = reacted;
    }
}
