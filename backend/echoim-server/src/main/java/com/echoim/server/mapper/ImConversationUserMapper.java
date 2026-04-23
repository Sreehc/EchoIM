package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImConversationUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImConversationUserMapper extends BaseMapper<ImConversationUserEntity> {

    ImConversationUserEntity selectByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                             @Param("userId") Long userId);

    List<ImConversationUserEntity> selectByConversationId(@Param("conversationId") Long conversationId);

    void resetDeleted(@Param("conversationId") Long conversationId,
                      @Param("userId") Long userId);

    void incrementUnread(@Param("conversationId") Long conversationId,
                         @Param("userId") Long userId);

    void updateReadState(@Param("conversationId") Long conversationId,
                         @Param("userId") Long userId,
                         @Param("lastReadSeq") Long lastReadSeq);
}
