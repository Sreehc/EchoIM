package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImMessagePinEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImMessagePinMapper extends BaseMapper<ImMessagePinEntity> {

    List<ImMessagePinEntity> selectPinsByConversationId(@Param("conversationId") Long conversationId);

    List<Long> selectPinnedMessageIdsByConversationId(@Param("conversationId") Long conversationId);
}
