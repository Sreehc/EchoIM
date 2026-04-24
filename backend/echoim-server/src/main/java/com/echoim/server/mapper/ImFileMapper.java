package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImFileMapper extends BaseMapper<ImFileEntity> {

    long countAccessibleByConversation(@Param("fileId") Long fileId,
                                       @Param("userId") Long userId);
}
