package com.echoim.server.vo.offline;

import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;

import java.util.List;

public class OfflineSyncConversationVo {

    private ConversationItemVo conversation;
    private Long fromSeq;
    private Long toSeq;
    private Boolean hasMore;
    private List<MessageItemVo> messages;

    public ConversationItemVo getConversation() {
        return conversation;
    }

    public void setConversation(ConversationItemVo conversation) {
        this.conversation = conversation;
    }

    public Long getFromSeq() {
        return fromSeq;
    }

    public void setFromSeq(Long fromSeq) {
        this.fromSeq = fromSeq;
    }

    public Long getToSeq() {
        return toSeq;
    }

    public void setToSeq(Long toSeq) {
        this.toSeq = toSeq;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<MessageItemVo> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageItemVo> messages) {
        this.messages = messages;
    }
}
