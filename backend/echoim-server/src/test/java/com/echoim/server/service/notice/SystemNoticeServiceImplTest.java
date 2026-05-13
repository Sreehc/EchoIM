package com.echoim.server.service.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.entity.ImSystemNoticeEntity;
import com.echoim.server.entity.ImSystemNoticeReadEntity;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.im.session.ImSessionContext;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.ImSystemNoticeMapper;
import com.echoim.server.mapper.ImSystemNoticeReadMapper;
import com.echoim.server.service.admin.AdminOperationLogService;
import com.echoim.server.service.notice.impl.SystemNoticeServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SystemNoticeServiceImplTest {

    private ImSystemNoticeMapper imSystemNoticeMapper;
    private ImSystemNoticeReadMapper imSystemNoticeReadMapper;
    private ImSessionManager imSessionManager;
    private ImWsPushService imWsPushService;
    private AdminOperationLogService adminOperationLogService;
    private SystemNoticeServiceImpl systemNoticeService;

    @BeforeEach
    void setUp() {
        imSystemNoticeMapper = mock(ImSystemNoticeMapper.class);
        imSystemNoticeReadMapper = mock(ImSystemNoticeReadMapper.class);
        imSessionManager = mock(ImSessionManager.class);
        imWsPushService = mock(ImWsPushService.class);
        adminOperationLogService = mock(AdminOperationLogService.class);
        systemNoticeService = new SystemNoticeServiceImpl(
                imSystemNoticeMapper,
                imSystemNoticeReadMapper,
                imSessionManager,
                imWsPushService,
                adminOperationLogService,
                new ObjectMapper()
        );

        LoginUser admin = new LoginUser();
        admin.setUserId(1L);
        admin.setUsername("admin");
        admin.setTokenType("admin");
        admin.setRoleCode("super_admin");
        LoginUserContext.set(admin);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void createNoticeShouldPushOnlyTargetUsersForTargetedNotice() {
        when(imSessionManager.allSessions()).thenReturn(List.of(
                session(10001L),
                session(10002L),
                session(10003L)
        ));
        doAnswer(invocation -> {
            ImSystemNoticeEntity entity = invocation.getArgument(0);
            entity.setId(90001L);
            return 1;
        }).when(imSystemNoticeMapper).insert(any(ImSystemNoticeEntity.class));

        Map<String, Object> result = systemNoticeService.createNotice(Map.of(
                "title", "系统维护通知",
                "content", "今晚 23:00 进行维护",
                "noticeType", 2,
                "targetUserIds", "10001,10003"
        ));

        assertEquals(90001L, result.get("noticeId"));
        verify(imWsPushService).pushToUser(eq(10001L), eq(WsMessageType.SYSTEM_NOTICE), isNull(), isNull(), any());
        verify(imWsPushService).pushToUser(eq(10003L), eq(WsMessageType.SYSTEM_NOTICE), isNull(), isNull(), any());
        verify(imWsPushService, never()).pushToUser(eq(10002L), eq(WsMessageType.SYSTEM_NOTICE), isNull(), isNull(), any());
        verify(adminOperationLogService).log(eq(1L), eq("SYSTEM_NOTICE"), eq("CREATE"), eq("NOTICE"), eq(90001L), any());
    }

    @Test
    void pageAndReadNoticesShouldTrackUnreadCount() {
        ImSystemNoticeEntity noticeAll = notice(1L, 1, null, "全员公告");
        ImSystemNoticeEntity noticeTargeted = notice(2L, 2, "[10001]", "指定用户公告");
        ImSystemNoticeEntity hiddenNotice = notice(3L, 2, "[10002]", "其他用户公告");
        when(imSystemNoticeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(noticeTargeted, hiddenNotice, noticeAll));
        when(imSystemNoticeReadMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(), List.of(), List.of(readRecord(2L, 10001L)), List.of(readRecord(2L, 10001L)));
        when(imSystemNoticeMapper.selectById(2L)).thenReturn(noticeTargeted);
        when(imSystemNoticeReadMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Map<String, Object> firstPage = systemNoticeService.pageUserNotices(10001L, 1, 20);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) firstPage.get("list");
        assertEquals(2, items.size());
        assertEquals(2L, items.get(0).get("noticeId"));
        assertEquals(2L, firstPage.get("unreadCount"));

        Map<String, Object> readResult = systemNoticeService.markNoticeRead(10001L, 2L);
        assertEquals(true, readResult.get("read"));
        verify(imSystemNoticeReadMapper).insert(org.mockito.ArgumentMatchers.<ImSystemNoticeReadEntity>argThat(entity ->
                entity.getNoticeId().equals(2L) && entity.getUserId().equals(10001L) && entity.getReadAt() != null
        ));

        Map<String, Object> secondPage = systemNoticeService.pageUserNotices(10001L, 1, 20);
        assertEquals(1L, secondPage.get("unreadCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedItems = (List<Map<String, Object>>) secondPage.get("list");
        assertTrue((Boolean) updatedItems.get(0).get("read"));
    }

    private ImSessionContext session(Long userId) {
        return new ImSessionContext(userId, "session-" + userId, LocalDateTime.now());
    }

    private ImSystemNoticeEntity notice(Long id, Integer noticeType, String targetUserIds, String title) {
        ImSystemNoticeEntity entity = new ImSystemNoticeEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setContent(title + " 内容");
        entity.setNoticeType(noticeType);
        entity.setTargetUserIds(targetUserIds);
        entity.setStatus(1);
        entity.setPublishedAt(LocalDateTime.now());
        return entity;
    }

    private ImSystemNoticeReadEntity readRecord(Long noticeId, Long userId) {
        ImSystemNoticeReadEntity entity = new ImSystemNoticeReadEntity();
        entity.setNoticeId(noticeId);
        entity.setUserId(userId);
        entity.setReadAt(LocalDateTime.now());
        return entity;
    }
}
