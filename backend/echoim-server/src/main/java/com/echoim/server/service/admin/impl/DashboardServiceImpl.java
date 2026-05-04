package com.echoim.server.service.admin.impl;

import com.echoim.server.im.monitor.WsMetrics;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.DashboardMapper;
import com.echoim.server.service.admin.DashboardService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final ImSessionManager imSessionManager;
    private final WsMetrics wsMetrics;

    public DashboardServiceImpl(DashboardMapper dashboardMapper,
                                ImSessionManager imSessionManager,
                                WsMetrics wsMetrics) {
        this.dashboardMapper = dashboardMapper;
        this.imSessionManager = imSessionManager;
        this.wsMetrics = wsMetrics;
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", dashboardMapper.countTotalUsers());
        overview.put("newUsersToday", dashboardMapper.countNewUsersToday());
        overview.put("totalMessages", dashboardMapper.countTotalMessages());
        overview.put("messagesToday", dashboardMapper.countMessagesToday());
        overview.put("onlineUsers", imSessionManager.allSessions().size());
        return overview;
    }

    @Override
    public List<Map<String, Object>> getMessageTrend(int days) {
        return dashboardMapper.selectMessageTrend(days);
    }

    @Override
    public List<Map<String, Object>> getUserTrend(int days) {
        return dashboardMapper.selectUserTrend(days);
    }

    @Override
    public List<Map<String, Object>> getMessageTypeBreakdown() {
        return dashboardMapper.selectMessageTypeBreakdown();
    }

    @Override
    public Map<String, Object> getOnlineStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("currentOnline", imSessionManager.allSessions().size());
        stats.putAll(wsMetrics.snapshot());
        return stats;
    }
}
