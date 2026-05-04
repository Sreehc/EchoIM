package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImScheduledMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ImScheduledMessageMapper extends BaseMapper<ImScheduledMessageEntity> {

    List<ImScheduledMessageEntity> selectPendingByUserId(@Param("userId") Long userId);

    List<ImScheduledMessageEntity> selectPendingByConversationId(@Param("conversationId") Long conversationId);

    List<ImScheduledMessageEntity> selectPendingBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
}
