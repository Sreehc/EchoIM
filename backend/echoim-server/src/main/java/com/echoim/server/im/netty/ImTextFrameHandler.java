package com.echoim.server.im.netty;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.im.model.WsAuthData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImOnlineService;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImSingleChatService;
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
    private final com.echoim.server.config.ImProperties imProperties;

    public ImTextFrameHandler(ObjectMapper objectMapper,
                              TokenService tokenService,
                              ImOnlineService imOnlineService,
                              ImSingleChatService imSingleChatService,
                              ImGroupChatService imGroupChatService,
                              com.echoim.server.config.ImProperties imProperties) {
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.imOnlineService = imOnlineService;
        this.imSingleChatService = imSingleChatService;
        this.imGroupChatService = imGroupChatService;
        this.imProperties = imProperties;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
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

        try {
            switch (message.getType()) {
                case AUTH -> handleAuth(ctx, message);
                case PING -> handlePing(ctx, loginUser);
                case CHAT_SINGLE -> writeWsMessage(ctx, WsMessageType.ACK, message, imSingleChatService.sendSingle(loginUser, message));
                case CHAT_GROUP -> writeWsMessage(ctx, WsMessageType.ACK, message, imGroupChatService.sendGroup(loginUser, message));
                case ACK -> writeWsMessage(ctx, WsMessageType.ACK, message, imSingleChatService.deliveredAck(loginUser, message));
                case READ -> writeWsMessage(ctx, WsMessageType.READ, message, imSingleChatService.read(loginUser, message));
                default -> writeNotice(ctx, message, ErrorCode.PARAM_ERROR, "不支持的消息类型");
            }
        } catch (BizException ex) {
            if (message.getType() == WsMessageType.CHAT_SINGLE || message.getType() == WsMessageType.CHAT_GROUP) {
                writeWsMessage(ctx, WsMessageType.ACK, message, failedSendAck(ex));
            } else {
                writeNotice(ctx, message, ex.getCode(), ex.getMessage());
            }
        } catch (Exception ex) {
            if (message.getType() == WsMessageType.CHAT_SINGLE || message.getType() == WsMessageType.CHAT_GROUP) {
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
        imOnlineService.refreshHeartbeat(loginUser.getUserId(), currentNodeId(), imProperties.getHeartbeatTimeoutSeconds());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ALIVE");
        writeWsMessage(ctx, WsMessageType.PONG, null, data);
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
