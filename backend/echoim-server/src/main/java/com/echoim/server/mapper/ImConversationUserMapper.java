package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImConversationUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImConversationUserMapper extends BaseMapper<ImConversationUserEntity> {

    ImConversationUserEntity selectByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                             @Param("userId") Long userId);
}
