package com.echoim.server.im.netty;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.im.model.WsAuthData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.monitor.WsMetrics;
import com.echoim.server.im.service.ImOnlineService;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.service.call.CallService;
import com.echoim.server.service.token.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@ChannelHandler.Sharable
@Component
public class ImTextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final ImOnlineService imOnlineService;
    private final ImSingleChatService imSingleChatService;
    private final ImGroupChatService imGroupChatService;
    private final CallService callService;
    private final com.echoim.server.config.ImProperties imProperties;
    private final ImWsPushService imWsPushService;
    private final ImConversationUserMapper imConversationUserMapper;
    private final WsMetrics wsMetrics;

    public ImTextFrameHandler(ObjectMapper objectMapper,
                              TokenService tokenService,
                              ImOnlineService imOnlineService,
                              ImSingleChatService imSingleChatService,
                              ImGroupChatService imGroupChatService,
                              CallService callService,
                              com.echoim.server.config.ImProperties imProperties,
                              ImWsPushService imWsPushService,
                              ImConversationUserMapper imConversationUserMapper,
                              WsMetrics wsMetrics) {
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.imOnlineService = imOnlineService;
        this.imSingleChatService = imSingleChatService;
        this.imGroupChatService = imGroupChatService;
        this.callService = callService;
        this.imProperties = imProperties;
        this.imWsPushService = imWsPushService;
        this.imConversationUserMapper = imConversationUserMapper;
        this.wsMetrics = wsMetrics;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        wsMetrics.recordMessageReceived();
        WsMessage message = objectMapper.readValue(frame.text(), WsMessage.class);
        if (message == null || message.getType() == null) {
            ctx.close();
            return;
        }

        LoginUser loginUser = ctx.channel().attr(ImChannelAttributes.LOGIN_USER).get();
        if (loginUser == null && message.getType() != WsMessageType.AUTH) {
            ctx.close();
            return;
        }
        if (loginUser != null && message.getType() != WsMessageType.AUTH && isSessionExpired(loginUser)) {
            forceOfflineAndClose(ctx, message, ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
            return;
        }

        try {
            switch (message.getType()) {
                case AUTH -> handleAuth(ctx, message);
                case PING -> handlePing(ctx, loginUser);
                case CHAT_SINGLE -> writeWsMessage(ctx, WsMessageType.ACK, message, imSingleChatService.sendSingle(loginUser, message));
                case CHAT_GROUP -> writeWsMessage(ctx, WsMessageType.ACK, message, imGroupChatService.sendGroup(loginUser, message));
                case ACK -> writeWsMessage(ctx, WsMessageType.ACK, message, imSingleChatService.deliveredAck(loginUser, message));
                case READ -> writeWsMessage(ctx, WsMessageType.READ, message, imSingleChatService.read(loginUser, message));
                case CALL_OFFER, CALL_ANSWER, CALL_ICE_CANDIDATE -> callService.relaySignal(loginUser, message);
                case TYPING -> handleTyping(loginUser, message);
                default -> writeNotice(ctx, message, ErrorCode.PARAM_ERROR, "不支持的消息类型");
            }
        } catch (BizException ex) {
            if (message.getType() == WsMessageType.AUTH) {
                writeAuthFailureAndClose(ctx, message, ex);
            } else if (message.getType() == WsMessageType.CHAT_SINGLE || message.getType() == WsMessageType.CHAT_GROUP) {
                writeWsMessage(ctx, WsMessageType.ACK, message, failedSendAck(ex));
            } else {
                writeNotice(ctx, message, ex.getCode(), ex.getMessage());
            }
        } catch (Exception ex) {
            if (message.getType() == WsMessageType.AUTH) {
                writeAuthFailureAndClose(ctx, message, new BizException(ErrorCode.SYSTEM_ERROR, ex.getMessage()));
            } else if (message.getType() == WsMessageType.CHAT_SINGLE || message.getType() == WsMessageType.CHAT_GROUP) {
                writeWsMessage(ctx, WsMessageType.ACK, message, failedSendAck(new BizException(ErrorCode.SYSTEM_ERROR, ex.getMessage())));
            } else {
                writeNotice(ctx, message, ErrorCode.SYSTEM_ERROR, ex.getMessage());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cleanupChannel(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent && idleStateEvent.state() == IdleState.READER_IDLE) {
            cleanupChannel(ctx);
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cleanupChannel(ctx);
        ctx.close();
    }

    private void handleAuth(ChannelHandlerContext ctx, WsMessage message) throws JsonProcessingException {
        WsAuthData authData = objectMapper.convertValue(message.getData(), WsAuthData.class);
        LoginUser loginUser = tokenService.parseToken(authData.getToken());
        ctx.channel().attr(ImChannelAttributes.LOGIN_USER).set(loginUser);
        imOnlineService.markOnline(loginUser, ctx.channel(), currentNodeId(), imProperties.getHeartbeatTimeoutSeconds());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "SUCCESS");
        data.put("userId", loginUser.getUserId());
        writeWsMessage(ctx, WsMessageType.AUTH, message, data);
    }

    private void handlePing(ChannelHandlerContext ctx, LoginUser loginUser) throws JsonProcessingException {
        if (loginUser == null || isSessionExpired(loginUser)) {
            forceOfflineAndClose(ctx, null, ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
            return;
        }
        imOnlineService.refreshHeartbeat(loginUser.getUserId(), currentNodeId(), imProperties.getHeartbeatTimeoutSeconds());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ALIVE");
        writeWsMessage(ctx, WsMessageType.PONG, null, data);
    }

    @SuppressWarnings("unchecked")
    private void handleTyping(LoginUser loginUser, WsMessage message) {
        Object rawData = message.getData();
        if (!(rawData instanceof Map<?, ?> raw)) {
            return;
        }
        Object convIdObj = raw.get("conversationId");
        if (convIdObj == null) {
            return;
        }
        Long conversationId;
        try {
            conversationId = convIdObj instanceof Number n ? n.longValue() : Long.parseLong(convIdObj.toString());
        } catch (NumberFormatException e) {
            return;
        }

        // Forward typing indicator to the other member in the single conversation
        imConversationUserMapper.selectByConversationId(conversationId).stream()
                .map(u -> u.getUserId())
                .filter(uid -> !uid.equals(loginUser.getUserId()))
                .forEach(peerId -> imWsPushService.pushToUser(peerId, WsMessageType.TYPING,
                        message.getTraceId(), message.getClientMsgId(), rawData));
    }

    private void writeNotice(ChannelHandlerContext ctx, WsMessage request, int code, String message) throws JsonProcessingException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message == null ? "系统异常" : message);
        writeWsMessage(ctx, WsMessageType.NOTICE, request, data);
    }

    private Map<String, Object> failedSendAck(BizException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ackType", "SEND");
        data.put("status", "FAILED");
        data.put("duplicate", false);
        data.put("retryable", ex.getCode() == ErrorCode.SYSTEM_ERROR || ex.getCode() == ErrorCode.TOO_MANY_REQUESTS);
        data.put("code", ex.getCode());
        data.put("message", ex.getMessage());
        return data;
    }

    private void writeWsMessage(ChannelHandlerContext ctx, WsMessageType type, WsMessage request, Object data) throws JsonProcessingException {
        WsMessage response = new WsMessage();
        response.setType(type);
        if (request != null) {
            response.setTraceId(request.getTraceId());
            response.setClientMsgId(request.getClientMsgId());
        }
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
    }

    private void writeAuthFailureAndClose(ChannelHandlerContext ctx, WsMessage request, BizException ex) throws JsonProcessingException {
        wsMetrics.recordAuthFailure();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "FAILED");
        data.put("code", ex.getCode());
        data.put("message", ex.getMessage());
        writeWsMessage(ctx, WsMessageType.AUTH, request, data);
        cleanupChannel(ctx);
        ctx.close();
    }

    private void forceOfflineAndClose(ChannelHandlerContext ctx, WsMessage request, int code, String message) throws JsonProcessingException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        writeWsMessage(ctx, WsMessageType.FORCE_OFFLINE, request, data);
        cleanupChannel(ctx);
        ctx.close();
    }

    private boolean isSessionExpired(LoginUser loginUser) {
        Long expireAtMillis = loginUser.getExpireAtMillis();
        return expireAtMillis != null && expireAtMillis <= System.currentTimeMillis();
    }

    private String currentNodeId() {
        return "echoim-server:" + imProperties.getPort();
    }

    private void cleanupChannel(ChannelHandlerContext ctx) {
        LoginUser loginUser = ctx.channel().attr(ImChannelAttributes.LOGIN_USER).getAndSet(null);
        if (loginUser != null) {
            imOnlineService.markOffline(loginUser.getUserId(), ctx.channel());
        }
    }
}
