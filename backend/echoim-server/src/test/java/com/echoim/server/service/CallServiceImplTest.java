package com.echoim.server.service;

import com.echoim.server.config.CallProperties;
import com.echoim.server.entity.ImCallSessionEntity;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.ImCallSessionMapper;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.call.CallService;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.service.impl.CallServiceImpl;
import com.echoim.server.vo.conversation.ConversationItemVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CallServiceImplTest {

    private ImCallSessionMapper imCallSessionMapper;
    private ImConversationMapper imConversationMapper;
    private ImConversationUserMapper imConversationUserMapper;
    private ImMessageMapper imMessageMapper;
    private FriendService friendService;
    private ImWsPushService imWsPushService;
    private ImSessionManager imSessionManager;
    private CallServiceImpl callService;

    @BeforeEach
    void setUp() {
        imCallSessionMapper = mock(ImCallSessionMapper.class);
        imConversationMapper = mock(ImConversationMapper.class);
        imConversationUserMapper = mock(ImConversationUserMapper.class);
        imMessageMapper = mock(ImMessageMapper.class);
        friendService = mock(FriendService.class);
        imWsPushService = mock(ImWsPushService.class);
        imSessionManager = mock(ImSessionManager.class);
        CallProperties callProperties = new CallProperties();
        callProperties.setRingTimeoutSeconds(30);
        callService = new CallServiceImpl(
                imCallSessionMapper,
                imConversationMapper,
                imConversationUserMapper,
                imMessageMapper,
                friendService,
                imWsPushService,
                imSessionManager,
                callProperties
        );
    }

    @AfterEach
    void tearDown() {
        callService.shutdownTimeoutExecutor();
    }

    @Test
    void createCallShouldPersistSessionAndPushInvite() {
        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setId(30001L);
        conversation.setConversationType(1);
        conversation.setStatus(1);
        conversation.setBizKey("10001_10002");

        ImConversationUserEntity member = new ImConversationUserEntity();
        member.setConversationId(30001L);
        member.setUserId(10001L);

        ConversationItemVo callerConversation = new ConversationItemVo();
        callerConversation.setConversationId(30001L);
        callerConversation.setConversationName("对端");
        callerConversation.setAvatarUrl("caller-avatar");
        ConversationItemVo calleeConversation = new ConversationItemVo();
        calleeConversation.setConversationId(30001L);
        calleeConversation.setConversationName("发起者");
        calleeConversation.setAvatarUrl("callee-avatar");

        when(imConversationMapper.selectByIdForUpdate(30001L)).thenReturn(conversation);
        when(imConversationUserMapper.selectByConversationIdAndUserId(30001L, 10001L)).thenReturn(member);
        when(imConversationUserMapper.selectByConversationId(30001L)).thenReturn(List.of(
                buildConversationUser(30001L, 10001L),
                buildConversationUser(30001L, 10002L)
        ));
        when(imSessionManager.isOnline(10002L)).thenReturn(true);
        when(imCallSessionMapper.selectCount(any())).thenReturn(0L);
        when(imMessageMapper.selectMaxSeqNoByConversationId(30001L)).thenReturn(0L);
        when(imConversationMapper.selectConversationItemByUserId(30001L, 10001L)).thenReturn(callerConversation);
        when(imConversationMapper.selectConversationItemByUserId(30001L, 10002L)).thenReturn(calleeConversation);
        doAnswer(invocation -> {
            ImCallSessionEntity entity = invocation.getArgument(0);
            entity.setId(90001L);
            return 1;
        }).when(imCallSessionMapper).insert(any(ImCallSessionEntity.class));

        CallService service = callService;
        var summary = service.createCall(10001L, 30001L, "audio");

        assertNotNull(summary);
        assertEquals(90001L, summary.getCallId());
        assertEquals("ringing", summary.getStatus());
        verify(friendService).validateSingleChatAllowed(10001L, 10002L);
        verify(imWsPushService).pushToUser(eq(10002L), eq(WsMessageType.CALL_INVITE), any(), any(), any());
        verify(imWsPushService).pushToUser(eq(10001L), eq(WsMessageType.CALL_STATE), any(), any(), any());
        verify(imWsPushService).pushToUser(eq(10002L), eq(WsMessageType.CALL_STATE), any(), any(), any());
    }

    @Test
    void acceptCallShouldTransitionToAcceptedAndNotifyCaller() {
        ImCallSessionEntity session = new ImCallSessionEntity();
        session.setId(90001L);
        session.setConversationId(30001L);
        session.setCallType("audio");
        session.setCallerUserId(10001L);
        session.setCalleeUserId(10002L);
        session.setStatus("ringing");
        session.setStartedAt(LocalDateTime.now());

        ConversationItemVo callerConversation = new ConversationItemVo();
        callerConversation.setConversationId(30001L);
        callerConversation.setConversationName("对端");
        ConversationItemVo calleeConversation = new ConversationItemVo();
        calleeConversation.setConversationId(30001L);
        calleeConversation.setConversationName("发起者");

        when(imCallSessionMapper.selectOne(any())).thenReturn(session);
        when(imConversationMapper.selectConversationItemByUserId(30001L, 10001L)).thenReturn(callerConversation);
        when(imConversationMapper.selectConversationItemByUserId(30001L, 10002L)).thenReturn(calleeConversation);

        var summary = callService.acceptCall(10002L, 90001L);

        assertEquals("accepted", summary.getStatus());
        verify(imCallSessionMapper).updateById((ImCallSessionEntity) argThat((ImCallSessionEntity entity) ->
                "accepted".equals(entity.getStatus()) && entity.getAnsweredAt() != null
        ));
        verify(imWsPushService).pushToUser(eq(10001L), eq(WsMessageType.CALL_ACCEPT), any(), any(), any());
    }

    private ImConversationUserEntity buildConversationUser(Long conversationId, Long userId) {
        ImConversationUserEntity entity = new ImConversationUserEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(userId);
        return entity;
    }
}
