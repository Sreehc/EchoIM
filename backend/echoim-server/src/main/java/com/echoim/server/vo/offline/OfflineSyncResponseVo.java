package com.echoim.server.vo.offline;

import java.util.List;

public class OfflineSyncResponseVo {

    private List<OfflineSyncConversationVo> conversations;
    private Boolean hasMore;

    public OfflineSyncResponseVo(List<OfflineSyncConversationVo> conversations, Boolean hasMore) {
        this.conversations = conversations;
        this.hasMore = hasMore;
    }

    public List<OfflineSyncConversationVo> getConversations() {
        return conversations;
    }

    public void setConversations(List<OfflineSyncConversationVo> conversations) {
        this.conversations = conversations;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}
