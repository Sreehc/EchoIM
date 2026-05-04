package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.mapper.SysOperationLogMapper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    private final SysOperationLogMapper logMapper;

    public AdminOperationLogController(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listLogs(
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false) String moduleName,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        long offset = (pageNo - 1) * pageSize;
        var list = logMapper.selectLogPage(adminUserId, moduleName, offset, pageSize);
        long total = logMapper.countLogs(adminUserId, moduleName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("total", total);
        return ApiResponse.success(result);
    }
}
