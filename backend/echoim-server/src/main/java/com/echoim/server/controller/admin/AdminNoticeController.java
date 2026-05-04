package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.entity.ImSystemNoticeEntity;
import com.echoim.server.mapper.ImSystemNoticeMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final ImSystemNoticeMapper noticeMapper;

    public AdminNoticeController(ImSystemNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listNotices(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        long offset = (pageNo - 1) * pageSize;
        var list = noticeMapper.selectNoticePage(status, offset, pageSize);
        long total = noticeMapper.countNotices(status);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createNotice(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        Integer noticeType = (Integer) request.getOrDefault("noticeType", 1);
        String targetUserIds = (String) request.get("targetUserIds");

        ImSystemNoticeEntity notice = new ImSystemNoticeEntity();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(noticeType);
        notice.setTargetUserIds(targetUserIds);
        notice.setStatus(1);
        notice.setPublishedBy(LoginUserContext.requireUserId());
        notice.setPublishedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return ApiResponse.success(Map.of("noticeId", notice.getId(), "success", true));
    }

    @PutMapping("/{id}/withdraw")
    public ApiResponse<Void> withdrawNotice(@PathVariable Long id) {
        ImSystemNoticeEntity notice = noticeMapper.selectById(id);
        if (notice == null) {
            return ApiResponse.success();
        }
        notice.setStatus(2);
        noticeMapper.updateById(notice);
        return ApiResponse.success();
    }
}
