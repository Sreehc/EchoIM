package com.echoim.server.service.notice;

import java.util.Map;

public interface SystemNoticeService {

    Map<String, Object> pageAdminNotices(Integer status, long pageNo, long pageSize);

    Map<String, Object> createNotice(Map<String, Object> request);

    void withdrawNotice(Long noticeId);

    Map<String, Object> pageUserNotices(Long userId, long pageNo, long pageSize);

    Map<String, Object> markNoticeRead(Long userId, Long noticeId);
}
