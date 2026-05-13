package com.echoim.server.service.notice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImSystemNoticeEntity;
import com.echoim.server.entity.ImSystemNoticeReadEntity;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.ImSystemNoticeMapper;
import com.echoim.server.mapper.ImSystemNoticeReadMapper;
import com.echoim.server.service.admin.AdminOperationLogService;
import com.echoim.server.service.notice.SystemNoticeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SystemNoticeServiceImpl implements SystemNoticeService {

    private static final int NOTICE_STATUS_PUBLISHED = 1;
    private static final int NOTICE_STATUS_WITHDRAWN = 2;
    private static final int NOTICE_TYPE_ALL = 1;
    private static final int NOTICE_TYPE_TARGETED = 2;

    private final ImSystemNoticeMapper imSystemNoticeMapper;
    private final ImSystemNoticeReadMapper imSystemNoticeReadMapper;
    private final ImSessionManager imSessionManager;
    private final ImWsPushService imWsPushService;
    private final AdminOperationLogService adminOperationLogService;
    private final ObjectMapper objectMapper;

    public SystemNoticeServiceImpl(ImSystemNoticeMapper imSystemNoticeMapper,
                                   ImSystemNoticeReadMapper imSystemNoticeReadMapper,
                                   ImSessionManager imSessionManager,
                                   ImWsPushService imWsPushService,
                                   AdminOperationLogService adminOperationLogService,
                                   ObjectMapper objectMapper) {
        this.imSystemNoticeMapper = imSystemNoticeMapper;
        this.imSystemNoticeReadMapper = imSystemNoticeReadMapper;
        this.imSessionManager = imSessionManager;
        this.imWsPushService = imWsPushService;
        this.adminOperationLogService = adminOperationLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> pageAdminNotices(Integer status, long pageNo, long pageSize) {
        long offset = Math.max(0L, (Math.max(1L, pageNo) - 1L) * Math.max(1L, pageSize));
        var list = imSystemNoticeMapper.selectNoticePage(status, offset, Math.max(1L, pageSize));
        long total = imSystemNoticeMapper.countNotices(status);
        return Map.of(
                "list", list,
                "pageNo", Math.max(1L, pageNo),
                "pageSize", Math.max(1L, pageSize),
                "total", total
        );
    }

    @Override
    public Map<String, Object> createNotice(Map<String, Object> request) {
        Long adminUserId = LoginUserContext.requireAdmin().getUserId();
        String title = request.get("title") instanceof String value ? value.trim() : "";
        String content = request.get("content") instanceof String value ? value.trim() : "";
        Integer noticeType = request.get("noticeType") instanceof Number value ? value.intValue() : NOTICE_TYPE_ALL;
        String rawTargetUserIds = request.get("targetUserIds") instanceof String value ? value : null;

        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "公告标题和内容不能为空");
        }
        if (!Objects.equals(noticeType, NOTICE_TYPE_ALL) && !Objects.equals(noticeType, NOTICE_TYPE_TARGETED)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "公告类型错误");
        }

        List<Long> targetUserIds = normalizeTargetUserIds(rawTargetUserIds);
        if (Objects.equals(noticeType, NOTICE_TYPE_TARGETED) && targetUserIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "指定用户公告至少需要一个目标用户");
        }

        ImSystemNoticeEntity notice = new ImSystemNoticeEntity();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(noticeType);
        notice.setTargetUserIds(targetUserIds.isEmpty() ? null : toJson(targetUserIds));
        notice.setStatus(NOTICE_STATUS_PUBLISHED);
        notice.setPublishedBy(adminUserId);
        notice.setPublishedAt(LocalDateTime.now());
        imSystemNoticeMapper.insert(notice);

        pushNotice(notice, targetUserIds);
        adminOperationLogService.log(adminUserId, "SYSTEM_NOTICE", "CREATE", "NOTICE", notice.getId(), Map.of(
                "title", title,
                "noticeType", noticeType,
                "targetUserCount", targetUserIds.size()
        ));
        return Map.of("noticeId", notice.getId(), "success", true);
    }

    @Override
    public void withdrawNotice(Long noticeId) {
        Long adminUserId = LoginUserContext.requireAdmin().getUserId();
        ImSystemNoticeEntity notice = imSystemNoticeMapper.selectById(noticeId);
        if (notice == null) {
            return;
        }
        notice.setStatus(NOTICE_STATUS_WITHDRAWN);
        imSystemNoticeMapper.updateById(notice);
        adminOperationLogService.log(adminUserId, "SYSTEM_NOTICE", "WITHDRAW", "NOTICE", noticeId, Map.of(
                "title", notice.getTitle()
        ));
    }

    @Override
    public Map<String, Object> pageUserNotices(Long userId, long pageNo, long pageSize) {
        long normalizedPageNo = Math.max(1L, pageNo);
        long normalizedPageSize = Math.max(1L, pageSize);
        List<ImSystemNoticeEntity> visibleNotices = loadVisibleNotices(userId);
        long total = visibleNotices.size();
        List<Long> allVisibleNoticeIds = visibleNotices.stream()
                .map(ImSystemNoticeEntity::getId)
                .toList();
        Set<Long> allReadIds = loadReadNoticeIds(userId, allVisibleNoticeIds);
        int fromIndex = (int) Math.min((normalizedPageNo - 1) * normalizedPageSize, total);
        int toIndex = (int) Math.min(fromIndex + normalizedPageSize, total);

        List<Long> pageNoticeIds = visibleNotices.subList(fromIndex, toIndex).stream()
                .map(ImSystemNoticeEntity::getId)
                .toList();
        Set<Long> readIds = loadReadNoticeIds(userId, pageNoticeIds);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ImSystemNoticeEntity notice : visibleNotices.subList(fromIndex, toIndex)) {
            list.add(toUserNoticeItem(notice, readIds.contains(notice.getId())));
        }

        long unreadCount = visibleNotices.stream()
                .map(ImSystemNoticeEntity::getId)
                .filter(id -> !allReadIds.contains(id))
                .count();

        return Map.of(
                "list", list,
                "pageNo", normalizedPageNo,
                "pageSize", normalizedPageSize,
                "total", total,
                "unreadCount", unreadCount
        );
    }

    @Override
    public Map<String, Object> markNoticeRead(Long userId, Long noticeId) {
        ImSystemNoticeEntity notice = imSystemNoticeMapper.selectById(noticeId);
        if (notice == null || !Objects.equals(notice.getStatus(), NOTICE_STATUS_PUBLISHED) || !isVisibleToUser(notice, userId)) {
            throw new BizException(ErrorCode.MESSAGE_NOT_FOUND, "公告不存在");
        }

        ImSystemNoticeReadEntity existing = imSystemNoticeReadMapper.selectOne(new LambdaQueryWrapper<ImSystemNoticeReadEntity>()
                .eq(ImSystemNoticeReadEntity::getNoticeId, noticeId)
                .eq(ImSystemNoticeReadEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (existing == null) {
            ImSystemNoticeReadEntity entity = new ImSystemNoticeReadEntity();
            entity.setNoticeId(noticeId);
            entity.setUserId(userId);
            entity.setReadAt(LocalDateTime.now());
            imSystemNoticeReadMapper.insert(entity);
        } else if (existing.getReadAt() == null) {
            existing.setReadAt(LocalDateTime.now());
            imSystemNoticeReadMapper.updateById(existing);
        }

        return Map.of("noticeId", noticeId, "read", true);
    }

    private List<ImSystemNoticeEntity> loadVisibleNotices(Long userId) {
        return imSystemNoticeMapper.selectList(new LambdaQueryWrapper<ImSystemNoticeEntity>()
                        .eq(ImSystemNoticeEntity::getStatus, NOTICE_STATUS_PUBLISHED)
                        .orderByDesc(ImSystemNoticeEntity::getPublishedAt))
                .stream()
                .filter(notice -> isVisibleToUser(notice, userId))
                .toList();
    }

    private boolean isVisibleToUser(ImSystemNoticeEntity notice, Long userId) {
        if (Objects.equals(notice.getNoticeType(), NOTICE_TYPE_ALL)) {
            return true;
        }
        return parseTargetUserIds(notice.getTargetUserIds()).contains(userId);
    }

    private Set<Long> loadReadNoticeIds(Long userId, List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return Set.of();
        }
        return imSystemNoticeReadMapper.selectList(new LambdaQueryWrapper<ImSystemNoticeReadEntity>()
                        .eq(ImSystemNoticeReadEntity::getUserId, userId)
                        .in(ImSystemNoticeReadEntity::getNoticeId, noticeIds))
                .stream()
                .map(ImSystemNoticeReadEntity::getNoticeId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Map<String, Object> toUserNoticeItem(ImSystemNoticeEntity notice, boolean read) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("noticeId", notice.getId());
        item.put("title", notice.getTitle());
        item.put("content", notice.getContent());
        item.put("noticeType", notice.getNoticeType());
        item.put("publishedAt", notice.getPublishedAt());
        item.put("read", read);
        return item;
    }

    private void pushNotice(ImSystemNoticeEntity notice, List<Long> targetUserIds) {
        Map<String, Object> payload = Map.of(
                "noticeId", notice.getId(),
                "title", notice.getTitle(),
                "noticeType", notice.getNoticeType(),
                "publishedAt", notice.getPublishedAt(),
                "requiresDetail", true
        );

        if (Objects.equals(notice.getNoticeType(), NOTICE_TYPE_ALL)) {
            Collection<com.echoim.server.im.session.ImSessionContext> sessions = imSessionManager.allSessions();
            for (var session : sessions) {
                imWsPushService.pushToUser(session.getUserId(), WsMessageType.SYSTEM_NOTICE, null, null, payload);
            }
            return;
        }

        for (Long targetUserId : targetUserIds) {
            imWsPushService.pushToUser(targetUserId, WsMessageType.SYSTEM_NOTICE, null, null, payload);
        }
    }

    private List<Long> normalizeTargetUserIds(String rawValue) {
        List<Long> values = parseTargetUserIds(rawValue);
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null && value > 0) {
                unique.add(value);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<Long> parseTargetUserIds(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }
        try {
            if (rawValue.trim().startsWith("[")) {
                return objectMapper.readValue(rawValue, new TypeReference<List<Long>>() {});
            }
        } catch (JsonProcessingException ignored) {
            // fall back to comma separated parsing below
        }

        List<Long> result = new ArrayList<>();
        for (String part : rawValue.split("[,\\n\\r\\s]+")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                result.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ex) {
                throw new BizException(ErrorCode.PARAM_ERROR, "目标用户 ID 格式错误");
            }
        }
        return result;
    }

    private String toJson(List<Long> targetUserIds) {
        try {
            return objectMapper.writeValueAsString(targetUserIds);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "公告目标用户序列化失败");
        }
    }
}
