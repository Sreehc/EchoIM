package com.echoim.server.im.service;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ImWsPushService {

    private final ImSessionManager imSessionManager;
    private final ObjectMapper objectMapper;

    public ImWsPushService(ImSessionManager imSessionManager, ObjectMapper objectMapper) {
        this.imSessionManager = imSessionManager;
        this.objectMapper = objectMapper;
    }

    public void pushToUser(Long userId, WsMessageType type, String traceId, String clientMsgId, Object data) {
        Optional<Channel> channelOptional = imSessionManager.getChannel(userId);
        if (channelOptional.isEmpty() || !channelOptional.get().isActive()) {
            return;
        }
        pushToChannel(channelOptional.get(), type, traceId, clientMsgId, data);
    }

    public void pushToChannel(Channel channel, WsMessageType type, String traceId, String clientMsgId, Object data) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        WsMessage response = new WsMessage();
        response.setType(type);
        response.setTraceId(traceId);
        response.setClientMsgId(clientMsgId);
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        try {
            channel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "WebSocket 消息编码失败");
        }
    }

    public void pushForceOffline(Channel channel, int code, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        pushToChannel(channel, WsMessageType.FORCE_OFFLINE, null, null, data);
    }

    public void pushForceOffline(Long userId, int code, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        pushToUser(userId, WsMessageType.FORCE_OFFLINE, null, null, data);
    }

    public void pushConversationChange(Long userId,
                                       String changeType,
                                       ConversationItemVo conversation,
                                       Object message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("changeType", changeType);
        data.put("conversation", conversation);
        if (message != null) {
            data.put("message", message);
        }
        pushToUser(userId, WsMessageType.CONVERSATION_CHANGE, null, null, data);
    }
}
