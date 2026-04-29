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

    void resetDeletedBatch(@Param("conversationId") Long conversationId,
                           @Param("userIds") List<Long> userIds);

    void incrementUnread(@Param("conversationId") Long conversationId,
                         @Param("userId") Long userId);

    void incrementUnreadBatch(@Param("conversationId") Long conversationId,
                              @Param("userIds") List<Long> userIds);

    void updateReadState(@Param("conversationId") Long conversationId,
                         @Param("userId") Long userId,
                         @Param("lastReadSeq") Long lastReadSeq);

    void updateTop(@Param("conversationId") Long conversationId,
                   @Param("userId") Long userId,
                   @Param("isTop") Integer isTop);

    void updateMute(@Param("conversationId") Long conversationId,
                    @Param("userId") Long userId,
                    @Param("isMute") Integer isMute);

    void updateArchive(@Param("conversationId") Long conversationId,
                       @Param("userId") Long userId,
                       @Param("isArchived") Integer isArchived);

    void updateManualUnread(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId,
                            @Param("manualUnread") Integer manualUnread);

    void hideConversation(@Param("conversationId") Long conversationId,
                          @Param("userId") Long userId);
}
