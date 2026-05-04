package com.echoim.server.service.admin;

import java.util.Map;

public interface ReportService {

    void submitReport(Long reporterUserId, Integer targetType, Long targetId, String reason, String description);

    Map<String, Object> listReports(Integer status, long pageNo, long pageSize);

    void handleReport(Long reportId, Long adminUserId, Integer action, String remark);
}
