package com.echoim.server.service.message;

import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.vo.conversation.MessageItemVo;

import java.util.List;

public interface MessageViewService {

    void enrichMessages(Long viewerUserId, List<MessageItemVo> messages);

    void enrichWsMessage(Long viewerUserId, WsMessageItem item, ImMessageEntity entity);
}
