package com.echoim.server.service;

import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImFileMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.service.conversation.ConversationService;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.service.impl.ConversationServiceImpl;
import com.echoim.server.service.message.MessageViewService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ConversationServiceImplTest {

    private ImConversationMapper imConversationMapper;
    private ImConversationUserMapper imConversationUserMapper;
    private ImMessageMapper imMessageMapper;
    private ImFileMapper imFileMapper;
    private ImSingleChatService imSingleChatService;
    private FileService fileService;
    private MessageViewService messageViewService;
    private FriendService friendService;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        imConversationMapper = mock(ImConversationMapper.class);
        imConversationUserMapper = mock(ImConversationUserMapper.class);
        imMessageMapper = mock(ImMessageMapper.class);
        imFileMapper = mock(ImFileMapper.class);
        imSingleChatService = mock(ImSingleChatService.class);
        fileService = mock(FileService.class);
        messageViewService = mock(MessageViewService.class);
        friendService = mock(FriendService.class);
        service = new ConversationServiceImpl(
                imConversationMapper,
                imConversationUserMapper,
                imMessageMapper,
                imFileMapper,
                imSingleChatService,
                fileService,
                messageViewService,
                friendService
        );
    }

    @Test
    void pageCurrentUserConversationsShouldRespectArchivedFlag() {
        ConversationPageQueryDto queryDto = new ConversationPageQueryDto();
        queryDto.setArchived(1);
        queryDto.setPageNo(1L);
        queryDto.setPageSize(20L);

        ConversationItemVo archivedConversation = new ConversationItemVo();
        archivedConversation.setConversationId(30001L);
        archivedConversation.setArchived(true);
        archivedConversation.setConversationType(1);
        when(imConversationMapper.selectAllConversationsByUserId(10001L)).thenReturn(List.of(archivedConversation));

        var response = service.pageCurrentUserConversations(10001L, queryDto);

        assertEquals(1, response.getTotal());
        verify(imConversationMapper).selectAllConversationsByUserId(10001L);
    }

    @Test
    void createSingleConversationShouldReuseExistingConversationAndClearCreatorArchive() {
        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setId(30001L);
        conversation.setConversationType(1);
        conversation.setStatus(1);

        ImConversationUserEntity creatorRelation = new ImConversationUserEntity();
        creatorRelation.setConversationId(30001L);
        creatorRelation.setUserId(10001L);
        creatorRelation.setDeleted(1);
        creatorRelation.setIsArchived(1);
        creatorRelation.setManualUnread(1);

        ConversationItemVo itemVo = new ConversationItemVo();
        itemVo.setConversationId(30001L);
        itemVo.setConversationType(1);

        when(imConversationMapper.selectSingleConversationByBizKey("10001_10002")).thenReturn(conversation);
        when(imConversationUserMapper.selectByConversationIdAndUserId(30001L, 10001L)).thenReturn(creatorRelation);
        when(imConversationUserMapper.selectByConversationIdAndUserId(30001L, 10002L)).thenReturn(null);
        when(imConversationMapper.selectConversationItemByUserId(30001L, 10001L)).thenReturn(itemVo);

        ConversationItemVo result = service.createSingleConversation(10001L, 10002L);

        assertNotNull(result);
        assertEquals(30001L, result.getConversationId());
        verify(friendService).validateSingleChatAllowed(10001L, 10002L);
        verify(imConversationUserMapper).updateById(argThat((ImConversationUserEntity entity) ->
                entity.getConversationId().equals(30001L)
                        && entity.getUserId().equals(10001L)
                        && entity.getDeleted() == 0
                        && entity.getIsArchived() == 0
                        && entity.getManualUnread() == 0
        ));
        verify(imConversationUserMapper).insert(argThat((ImConversationUserEntity entity) ->
                entity.getConversationId().equals(30001L)
                        && entity.getUserId().equals(10002L)
                        && entity.getDeleted() == 0
        ));
    }

    @Test
    void markConversationUnreadShouldPersistManualUnreadFlag() {
        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setId(30001L);
        conversation.setStatus(1);
        ImConversationUserEntity relation = new ImConversationUserEntity();
        relation.setConversationId(30001L);
        relation.setUserId(10001L);

        when(imConversationMapper.selectById(30001L)).thenReturn(conversation);
        when(imConversationUserMapper.selectByConversationIdAndUserId(30001L, 10001L)).thenReturn(relation);

        service.markConversationUnread(10001L, 30001L, true);

        verify(imConversationUserMapper).updateManualUnread(30001L, 10001L, 1);
    }
}
