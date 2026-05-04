package com.echoim.server.service.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getOverview();

    List<Map<String, Object>> getMessageTrend(int days);

    List<Map<String, Object>> getUserTrend(int days);

    List<Map<String, Object>> getMessageTypeBreakdown();

    Map<String, Object> getOnlineStats();
}
