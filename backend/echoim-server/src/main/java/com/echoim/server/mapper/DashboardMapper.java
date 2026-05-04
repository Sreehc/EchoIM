package com.echoim.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    long countTotalUsers();

    long countNewUsersToday();

    long countTotalMessages();

    long countMessagesToday();

    List<Map<String, Object>> selectMessageTrend(@Param("days") int days);

    List<Map<String, Object>> selectUserTrend(@Param("days") int days);

    List<Map<String, Object>> selectMessageTypeBreakdown();
}
