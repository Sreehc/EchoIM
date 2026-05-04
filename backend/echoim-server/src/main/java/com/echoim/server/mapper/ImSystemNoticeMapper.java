package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImSystemNoticeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImSystemNoticeMapper extends BaseMapper<ImSystemNoticeEntity> {

    List<ImSystemNoticeEntity> selectNoticePage(@Param("status") Integer status,
                                                 @Param("offset") long offset,
                                                 @Param("pageSize") long pageSize);

    long countNotices(@Param("status") Integer status);
}
