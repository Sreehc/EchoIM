package com.echoim.server.vo.search;

import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.user.UserSearchItemVo;

import java.util.List;

public class GlobalSearchResponseVo {

    private List<ConversationItemVo> conversations;
    private List<UserSearchItemVo> users;
    private List<GlobalSearchMessageItemVo> messages;

    public List<ConversationItemVo> getConversations() {
        return conversations;
    }

    public void setConversations(List<ConversationItemVo> conversations) {
        this.conversations = conversations;
    }

    public List<UserSearchItemVo> getUsers() {
        return users;
    }

    public void setUsers(List<UserSearchItemVo> users) {
        this.users = users;
    }

    public List<GlobalSearchMessageItemVo> getMessages() {
        return messages;
    }

    public void setMessages(List<GlobalSearchMessageItemVo> messages) {
        this.messages = messages;
    }
}
