package com.echoim.server.im.service;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.im.model.WsMessage;

import java.util.Map;

public interface ImSingleChatService {

    Map<String, Object> sendSingle(LoginUser loginUser, WsMessage message);

    Map<String, Object> deliveredAck(LoginUser loginUser, WsMessage message);

    Map<String, Object> read(LoginUser loginUser, WsMessage message);

    Map<String, Object> read(Long userId, Long conversationId, Long lastReadSeq, String traceId, String clientMsgId);
}
