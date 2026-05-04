package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImUserBanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ImUserBanMapper extends BaseMapper<ImUserBanEntity> {

    List<Map<String, Object>> selectBanPage(@Param("userId") Long userId,
                                             @Param("offset") long offset,
                                             @Param("pageSize") long pageSize);

    long countBans(@Param("userId") Long userId);
}
