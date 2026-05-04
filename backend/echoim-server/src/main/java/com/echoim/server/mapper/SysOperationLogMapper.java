package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<com.echoim.server.entity.SysOperationLogEntity> {

    List<Map<String, Object>> selectLogPage(@Param("adminUserId") Long adminUserId,
                                             @Param("moduleName") String moduleName,
                                             @Param("offset") long offset,
                                             @Param("pageSize") long pageSize);

    long countLogs(@Param("adminUserId") Long adminUserId,
                   @Param("moduleName") String moduleName);
}
