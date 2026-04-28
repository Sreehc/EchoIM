package com.echoim.server.service;

import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.message.EditMessageRequestDto;
import com.echoim.server.dto.message.ForwardMessageRequestDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.config.SystemConfigService;
import com.echoim.server.service.impl.MessageCommandServiceImpl;
import com.echoim.server.service.message.MessageViewService;
import com.echoim.server.common.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class MessageCommandServiceImplTest {

    private ImMessageMapper imMessageMapper;
    private ImConversationMapper imConversationMapper;
    private ImConversationUserMapper imConversationUserMapper;
    private ImSingleChatService imSingleChatService;
    private ImGroupChatService imGroupChatService;
    private ImWsPushService imWsPushService;
    private MessageViewService messageViewService;
    private SystemConfigService systemConfigService;
    private AuditLogService auditLogService;
    private MessageCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        imMessageMapper = mock(ImMessageMapper.class);
        imConversationMapper = mock(ImConversationMapper.class);
        imConversationUserMapper = mock(ImConversationUserMapper.class);
        imSingleChatService = mock(ImSingleChatService.class);
        imGroupChatService = mock(ImGroupChatService.class);
        imWsPushService = mock(ImWsPushService.class);
        messageViewService = mock(MessageViewService.class);
        systemConfigService = mock(SystemConfigService.class);
        auditLogService = mock(AuditLogService.class);
        service = new MessageCommandServiceImpl(
                imMessageMapper, imConversationMapper, imConversationUserMapper,
                imSingleChatService, imGroupChatService, imWsPushService,
                messageViewService, systemConfigService, auditLogService, new ObjectMapper()
        );
    }

    @Test
    void recallShouldFailWhenExceedRecallWindow() {
        ImMessageEntity message = singleTextMessage();
        message.setSentAt(LocalDateTime.now().minusSeconds(121));
        when(imMessageMapper.selectById(1L)).thenReturn(message);
        when(imConversationMapper.selectById(anyLong())).thenReturn(normalConversation());
        when(imConversationUserMapper.selectByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(normalConversationUser());
        when(systemConfigService.getIntValue("message.recall-seconds", 120)).thenReturn(120);

        assertThrows(BizException.class, () -> service.recall(10001L, 1L));
    }

    @Test
    void editShouldFailForNonTextMessage() {
        ImMessageEntity message = singleTextMessage();
        message.setMsgType(5);
        when(imMessageMapper.selectById(1L)).thenReturn(message);
        when(imConversationMapper.selectById(anyLong())).thenReturn(normalConversation());
        when(imConversationUserMapper.selectByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(normalConversationUser());

        EditMessageRequestDto requestDto = new EditMessageRequestDto();
        requestDto.setContent("updated");
        assertThrows(BizException.class, () -> service.edit(10001L, 1L, requestDto));
    }

    @Test
    void forwardShouldFailWhenSourceMessageInvisible() {
        ForwardMessageRequestDto requestDto = new ForwardMessageRequestDto();
        requestDto.setMessageIds(List.of(1L));
        requestDto.setTargetConversationIds(List.of(30001L));
        when(imMessageMapper.selectAccessibleEntityByIdAndUserId(1L, 10001L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.forward(10001L, requestDto));
    }

    private ImMessageEntity singleTextMessage() {
        ImMessageEntity entity = new ImMessageEntity();
        entity.setId(1L);
        entity.setConversationId(30001L);
        entity.setConversationType(1);
        entity.setSeqNo(1L);
        entity.setClientMsgId("c1");
        entity.setFromUserId(10001L);
        entity.setToUserId(10002L);
        entity.setMsgType(1);
        entity.setContent("hello");
        entity.setSendStatus(1);
        entity.setSentAt(LocalDateTime.now());
        return entity;
    }

    private ImConversationEntity normalConversation() {
        ImConversationEntity entity = new ImConversationEntity();
        entity.setId(30001L);
        entity.setConversationType(1);
        entity.setStatus(1);
        return entity;
    }

    private ImConversationUserEntity normalConversationUser() {
        ImConversationUserEntity entity = new ImConversationUserEntity();
        entity.setConversationId(30001L);
        entity.setUserId(10001L);
        entity.setDeleted(0);
        return entity;
    }
}
