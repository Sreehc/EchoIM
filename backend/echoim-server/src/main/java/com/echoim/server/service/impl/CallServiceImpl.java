package com.echoim.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.CallProperties;
import com.echoim.server.entity.ImCallSessionEntity;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsCallSignalData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.ImCallSessionMapper;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.call.CallService;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.vo.call.CallIceServerVo;
import com.echoim.server.vo.call.CallSessionSummaryVo;
import com.echoim.server.vo.conversation.ConversationItemVo;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CallServiceImpl implements CallService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int MESSAGE_TYPE_SYSTEM = 6;
    private static final int MESSAGE_STATUS_SENT = 1;
    private static final String CALL_TYPE_AUDIO = "audio";
    private static final String CALL_TYPE_VIDEO = "video";
    private static final String STATUS_RINGING = "ringing";
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_ENDED = "ended";
    private static final String STATUS_MISSED = "missed";
    private static final String END_REASON_HANGUP = "hangup";
    private static final String END_REASON_REJECT = "reject";
    private static final String END_REASON_TIMEOUT = "timeout";
    private static final String END_REASON_BUSY = "busy";
    private static final String END_REASON_OFFLINE = "offline";
    private static final String SAVED_BIZ_KEY_PREFIX = "saved_";
    private static final List<String> ACTIVE_STATUSES = List.of(STATUS_RINGING, STATUS_ACCEPTED);

    private final ImCallSessionMapper imCallSessionMapper;
    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final FriendService friendService;
    private final ImWsPushService imWsPushService;
    private final ImSessionManager imSessionManager;
    private final CallProperties callProperties;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "echoim-call-timeout");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, ScheduledFuture<?>> timeoutFutures = new ConcurrentHashMap<>();

    public CallServiceImpl(ImCallSessionMapper imCallSessionMapper,
                           ImConversationMapper imConversationMapper,
                           ImConversationUserMapper imConversationUserMapper,
                           ImMessageMapper imMessageMapper,
                           FriendService friendService,
                           ImWsPushService imWsPushService,
                           ImSessionManager imSessionManager,
                           CallProperties callProperties) {
        this.imCallSessionMapper = imCallSessionMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.friendService = friendService;
        this.imWsPushService = imWsPushService;
        this.imSessionManager = imSessionManager;
        this.callProperties = callProperties;
    }

    @Override
    @Transactional
    public CallSessionSummaryVo createCall(Long userId, Long conversationId, String callType) {
        String normalizedType = normalizeCallType(callType);
        ImConversationEntity conversation = requireCallableSingleConversation(userId, conversationId);
        Long peerUserId = resolvePeerUserId(conversation.getId(), userId);
        friendService.validateSingleChatAllowed(userId, peerUserId);
        if (!imSessionManager.isOnline(peerUserId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "对方当前不在线");
        }
        ensureNotBusy(userId, null, END_REASON_BUSY, "你当前正在通话中");
        ensureNotBusy(peerUserId, null, END_REASON_BUSY, "对方当前忙线中");

        ImCallSessionEntity session = new ImCallSessionEntity();
        session.setConversationId(conversation.getId());
        session.setCallType(normalizedType);
        session.setCallerUserId(userId);
        session.setCalleeUserId(peerUserId);
        session.setStatus(STATUS_RINGING);
        session.setStartedAt(LocalDateTime.now());
        imCallSessionMapper.insert(session);

        ensureConversationVisible(conversation.getId(), userId, peerUserId);
        String startText = CALL_TYPE_VIDEO.equals(normalizedType) ? "发起了视频通话" : "发起了语音通话";
        pushSystemMessage(conversation, userId, peerUserId, startText, "call-start-" + session.getId(), session.getStartedAt());
        scheduleTimeout(session.getId());

        CallSessionSummaryVo calleeView = buildSummary(session, peerUserId);
        CallSessionSummaryVo callerView = buildSummary(session, userId);
        imWsPushService.pushToUser(peerUserId, WsMessageType.CALL_INVITE, null, null, calleeView);
        pushState(session);
        return callerView;
    }

    @Override
    @Transactional
    public CallSessionSummaryVo acceptCall(Long userId, Long callId) {
        ImCallSessionEntity session = requireLockedCall(callId, userId);
        if (!Objects.equals(session.getCalleeUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有被叫方可以接听");
        }
        if (!STATUS_RINGING.equals(session.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "当前通话不可接听");
        }

        session.setStatus(STATUS_ACCEPTED);
        session.setAnsweredAt(LocalDateTime.now());
        imCallSessionMapper.updateById(session);
        cancelTimeout(callId);

        CallSessionSummaryVo callerView = buildSummary(session, session.getCallerUserId());
        imWsPushService.pushToUser(session.getCallerUserId(), WsMessageType.CALL_ACCEPT, null, null, callerView);
        pushState(session);
        return buildSummary(session, userId);
    }

    @Override
    @Transactional
    public CallSessionSummaryVo rejectCall(Long userId, Long callId) {
        ImCallSessionEntity session = requireLockedCall(callId, userId);
        if (!Objects.equals(session.getCalleeUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有被叫方可以拒绝");
        }
        if (!STATUS_RINGING.equals(session.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "当前通话不可拒绝");
        }

        finishCall(session, STATUS_REJECTED, END_REASON_REJECT, LocalDateTime.now());
        imWsPushService.pushToUser(session.getCallerUserId(), WsMessageType.CALL_REJECT, null, null, buildSummary(session, session.getCallerUserId()));
        pushState(session);
        return buildSummary(session, userId);
    }

    @Override
    @Transactional
    public CallSessionSummaryVo cancelCall(Long userId, Long callId) {
        ImCallSessionEntity session = requireLockedCall(callId, userId);
        if (!Objects.equals(session.getCallerUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有主叫方可以取消");
        }
        if (!STATUS_RINGING.equals(session.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "当前通话不可取消");
        }

        finishCall(session, STATUS_CANCELLED, null, LocalDateTime.now());
        imWsPushService.pushToUser(session.getCalleeUserId(), WsMessageType.CALL_CANCEL, null, null, buildSummary(session, session.getCalleeUserId()));
        pushState(session);
        return buildSummary(session, userId);
    }

    @Override
    @Transactional
    public CallSessionSummaryVo endCall(Long userId, Long callId) {
        ImCallSessionEntity session = requireLockedCall(callId, userId);
        if (!isCallParticipant(session, userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权结束该通话");
        }
        if (!STATUS_ACCEPTED.equals(session.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "当前通话尚未建立");
        }

        LocalDateTime endedAt = LocalDateTime.now();
        finishCall(session, STATUS_ENDED, END_REASON_HANGUP, endedAt);
        pushDurationMessage(session, endedAt);
        Long targetUserId = otherUserId(session, userId);
        imWsPushService.pushToUser(targetUserId, WsMessageType.CALL_END, null, null, buildSummary(session, targetUserId));
        pushState(session);
        return buildSummary(session, userId);
    }

    @Override
    public CallSessionSummaryVo getCall(Long userId, Long callId) {
        ImCallSessionEntity session = requireCall(callId, userId);
        return buildSummary(session, userId);
    }

    @Override
    public void relaySignal(LoginUser loginUser, WsMessage wsMessage) {
        WsCallSignalData data = wsMessage == null ? null : convertSignalData(wsMessage.getData());
        if (data == null || data.getCallId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "通话信令参数错误");
        }
        ImCallSessionEntity session = requireCall(data.getCallId(), loginUser.getUserId());
        if (!STATUS_ACCEPTED.equals(session.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "当前通话未建立");
        }
        Long targetUserId = otherUserId(session, loginUser.getUserId());
        data.setConversationId(session.getConversationId());
        imWsPushService.pushToUser(targetUserId, wsMessage.getType(), wsMessage.getTraceId(), wsMessage.getClientMsgId(), data);
    }

    @PreDestroy
    public void shutdownTimeoutExecutor() {
        timeoutExecutor.shutdownNow();
    }

    private WsCallSignalData convertSignalData(Object payload) {
        if (payload instanceof WsCallSignalData data) {
            return data;
        }
        if (!(payload instanceof Map<?, ?> raw)) {
            return null;
        }
        WsCallSignalData data = new WsCallSignalData();
        data.setCallId(toLong(raw.get("callId")));
        data.setConversationId(toLong(raw.get("conversationId")));
        data.setSdp(toStringValue(raw.get("sdp")));
        data.setCandidate(toStringValue(raw.get("candidate")));
        data.setSdpMid(toStringValue(raw.get("sdpMid")));
        data.setSdpMLineIndex(toInteger(raw.get("sdpMLineIndex")));
        return data;
    }

    private void scheduleTimeout(Long callId) {
        cancelTimeout(callId);
        ScheduledFuture<?> future = timeoutExecutor.schedule(() -> timeoutCall(callId), callProperties.getRingTimeoutSeconds(), TimeUnit.SECONDS);
        timeoutFutures.put(callId, future);
    }

    private void cancelTimeout(Long callId) {
        ScheduledFuture<?> future = timeoutFutures.remove(callId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Transactional
    protected void timeoutCall(Long callId) {
        ImCallSessionEntity session = requireLockedCallInternal(callId);
        if (session == null || !STATUS_RINGING.equals(session.getStatus())) {
            cancelTimeout(callId);
            return;
        }
        LocalDateTime endedAt = LocalDateTime.now();
        finishCall(session, STATUS_MISSED, END_REASON_TIMEOUT, endedAt);
        pushMissedCallMessage(session, endedAt);
        imWsPushService.pushToUser(session.getCallerUserId(), WsMessageType.CALL_END, null, null, buildSummary(session, session.getCallerUserId()));
        imWsPushService.pushToUser(session.getCalleeUserId(), WsMessageType.CALL_END, null, null, buildSummary(session, session.getCalleeUserId()));
        pushState(session);
    }

    private void finishCall(ImCallSessionEntity session, String status, String endReason, LocalDateTime endedAt) {
        session.setStatus(status);
        session.setEndReason(endReason);
        session.setEndedAt(endedAt);
        imCallSessionMapper.updateById(session);
        cancelTimeout(session.getId());
    }

    private void pushState(ImCallSessionEntity session) {
        imWsPushService.pushToUser(session.getCallerUserId(), WsMessageType.CALL_STATE, null, null, buildSummary(session, session.getCallerUserId()));
        imWsPushService.pushToUser(session.getCalleeUserId(), WsMessageType.CALL_STATE, null, null, buildSummary(session, session.getCalleeUserId()));
    }

    private void pushMissedCallMessage(ImCallSessionEntity session, LocalDateTime occurredAt) {
        ImConversationEntity conversation = imConversationMapper.selectById(session.getConversationId());
        if (conversation == null) {
            return;
        }
        String missedText = CALL_TYPE_VIDEO.equals(session.getCallType()) ? "未接视频通话" : "未接来电";
        pushSystemMessage(conversation, session.getCallerUserId(), session.getCalleeUserId(), missedText, "call-missed-" + session.getId(), occurredAt);
    }

    private void pushDurationMessage(ImCallSessionEntity session, LocalDateTime occurredAt) {
        if (session.getAnsweredAt() == null) {
            return;
        }
        ImConversationEntity conversation = imConversationMapper.selectById(session.getConversationId());
        if (conversation == null) {
            return;
        }
        long durationSeconds = Math.max(0, Duration.between(session.getAnsweredAt(), occurredAt).getSeconds());
        String callLabel = CALL_TYPE_VIDEO.equals(session.getCallType()) ? "视频通话时长" : "通话时长";
        pushSystemMessage(
                conversation,
                session.getCallerUserId(),
                session.getCalleeUserId(),
                callLabel + " " + formatDuration(durationSeconds),
                "call-ended-" + session.getId(),
                occurredAt
        );
    }

    private void pushSystemMessage(ImConversationEntity conversation,
                                   Long callerUserId,
                                   Long calleeUserId,
                                   String content,
                                   String clientMsgId,
                                   LocalDateTime sentAt) {
        ensureConversationVisible(conversation.getId(), callerUserId, calleeUserId);
        ImMessageEntity message = new ImMessageEntity();
        message.setConversationId(conversation.getId());
        message.setConversationType(CONVERSATION_TYPE_SINGLE);
        message.setSeqNo(nextSeqNo(conversation.getId()));
        message.setClientMsgId(clientMsgId);
        message.setFromUserId(callerUserId);
        message.setToUserId(calleeUserId);
        message.setMsgType(MESSAGE_TYPE_SYSTEM);
        message.setContent(content);
        message.setSendStatus(MESSAGE_STATUS_SENT);
        message.setSentAt(sentAt);
        imMessageMapper.insert(message);
        imConversationMapper.updateLastMessageState(conversation.getId(), message.getId(), content, sentAt);

        pushConversationMessage(callerUserId, conversation.getId(), message);
        pushConversationMessage(calleeUserId, conversation.getId(), message);
    }

    private void pushConversationMessage(Long userId, Long conversationId, ImMessageEntity message) {
        ConversationItemVo conversation = imConversationMapper.selectConversationItemByUserId(conversationId, userId);
        if (conversation == null) {
            return;
        }
        imWsPushService.pushConversationChange(userId, "MESSAGE_NEW", conversation, toSystemMessageItem(message));
    }

    private WsMessageItem toSystemMessageItem(ImMessageEntity message) {
        WsMessageItem item = new WsMessageItem();
        item.setMessageId(message.getId());
        item.setConversationId(message.getConversationId());
        item.setConversationType(message.getConversationType());
        item.setSeqNo(message.getSeqNo());
        item.setClientMsgId(message.getClientMsgId());
        item.setFromUserId(message.getFromUserId());
        item.setToUserId(message.getToUserId());
        item.setMsgType("SYSTEM");
        item.setContent(message.getContent());
        item.setSendStatus(message.getSendStatus());
        item.setSentAt(message.getSentAt());
        return item;
    }

    private ImConversationEntity requireCallableSingleConversation(Long userId, Long conversationId) {
        ImConversationEntity conversation = imConversationMapper.selectByIdForUpdate(conversationId);
        if (conversation == null
                || !Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(conversation.getStatus())
                || !Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(conversation.getConversationType())
                || isSavedMessagesConversation(conversation)) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "当前会话不支持发起通话");
        }
        ImConversationUserEntity member = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private ImCallSessionEntity requireLockedCall(Long callId, Long userId) {
        ImCallSessionEntity session = requireLockedCallInternal(callId);
        if (session == null || !isCallParticipant(session, userId)) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "通话不存在");
        }
        return session;
    }

    private ImCallSessionEntity requireLockedCallInternal(Long callId) {
        return imCallSessionMapper.selectOne(new LambdaQueryWrapper<ImCallSessionEntity>()
                .eq(ImCallSessionEntity::getId, callId)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private ImCallSessionEntity requireCall(Long callId, Long userId) {
        ImCallSessionEntity session = imCallSessionMapper.selectById(callId);
        if (session == null || !isCallParticipant(session, userId)) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "通话不存在");
        }
        return session;
    }

    private boolean isCallParticipant(ImCallSessionEntity session, Long userId) {
        return Objects.equals(session.getCallerUserId(), userId) || Objects.equals(session.getCalleeUserId(), userId);
    }

    private void ensureConversationVisible(Long conversationId, Long callerUserId, Long calleeUserId) {
        imConversationUserMapper.resetDeleted(conversationId, callerUserId);
        imConversationUserMapper.resetDeleted(conversationId, calleeUserId);
    }

    private void ensureNotBusy(Long userId, Long excludeCallId, String endReason, String message) {
        LambdaQueryWrapper<ImCallSessionEntity> query = new LambdaQueryWrapper<ImCallSessionEntity>()
                .in(ImCallSessionEntity::getStatus, ACTIVE_STATUSES)
                .and(wrapper -> wrapper.eq(ImCallSessionEntity::getCallerUserId, userId)
                        .or()
                        .eq(ImCallSessionEntity::getCalleeUserId, userId));
        if (excludeCallId != null) {
            query.ne(ImCallSessionEntity::getId, excludeCallId);
        }
        if (imCallSessionMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, END_REASON_BUSY.equals(endReason) ? message : "当前用户忙线中");
        }
    }

    private Long resolvePeerUserId(Long conversationId, Long currentUserId) {
        return imConversationUserMapper.selectByConversationId(conversationId)
                .stream()
                .map(ImConversationUserEntity::getUserId)
                .filter(userId -> !Objects.equals(userId, currentUserId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "单聊对端不存在"));
    }

    private Long otherUserId(ImCallSessionEntity session, Long currentUserId) {
        return Objects.equals(session.getCallerUserId(), currentUserId) ? session.getCalleeUserId() : session.getCallerUserId();
    }

    private boolean isSavedMessagesConversation(ImConversationEntity conversation) {
        return conversation.getBizKey() != null && conversation.getBizKey().startsWith(SAVED_BIZ_KEY_PREFIX);
    }

    private String normalizeCallType(String callType) {
        if (!StringUtils.hasText(callType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "通话类型不能为空");
        }
        String normalized = callType.trim().toLowerCase();
        if (!CALL_TYPE_AUDIO.equals(normalized) && !CALL_TYPE_VIDEO.equals(normalized)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的通话类型：" + normalized);
        }
        return normalized;
    }

    private CallSessionSummaryVo buildSummary(ImCallSessionEntity session, Long viewerUserId) {
        ConversationItemVo conversation = imConversationMapper.selectConversationItemByUserId(session.getConversationId(), viewerUserId);
        CallSessionSummaryVo summary = new CallSessionSummaryVo();
        summary.setCallId(session.getId());
        summary.setConversationId(session.getConversationId());
        summary.setCallType(session.getCallType());
        summary.setStatus(session.getStatus());
        summary.setEndReason(session.getEndReason());
        summary.setCallerUserId(session.getCallerUserId());
        summary.setCalleeUserId(session.getCalleeUserId());
        summary.setPeerUserId(otherUserId(session, viewerUserId));
        summary.setPeerDisplayName(conversation == null ? "通话对象" : conversation.getConversationName());
        summary.setPeerAvatarUrl(conversation == null ? null : conversation.getAvatarUrl());
        summary.setStartedAt(session.getStartedAt());
        summary.setAnsweredAt(session.getAnsweredAt());
        summary.setEndedAt(session.getEndedAt());
        summary.setDurationSeconds(resolveDurationSeconds(session));
        summary.setIceServers(callProperties.getIceServers().stream().map(this::toIceServerVo).toList());
        return summary;
    }

    private Long resolveDurationSeconds(ImCallSessionEntity session) {
        if (session.getAnsweredAt() == null) {
            return 0L;
        }
        LocalDateTime endTime = session.getEndedAt() == null ? LocalDateTime.now() : session.getEndedAt();
        return Math.max(0, Duration.between(session.getAnsweredAt(), endTime).getSeconds());
    }

    private CallIceServerVo toIceServerVo(CallProperties.IceServerProperties properties) {
        CallIceServerVo server = new CallIceServerVo();
        server.setUrls(properties.getUrls());
        server.setUsername(properties.getUsername());
        server.setCredential(properties.getCredential());
        return server;
    }

    private long nextSeqNo(Long conversationId) {
        Long current = imMessageMapper.selectMaxSeqNoByConversationId(conversationId);
        return current == null ? 1L : current + 1L;
    }

    private String formatDuration(long durationSeconds) {
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return "%02d:%02d".formatted(minutes, seconds);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
