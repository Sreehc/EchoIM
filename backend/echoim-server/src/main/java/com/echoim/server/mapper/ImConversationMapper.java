package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.vo.conversation.ConversationItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImConversationMapper extends BaseMapper<ImConversationEntity> {

    List<ConversationItemVo> selectConversationPageByUserId(@Param("userId") Long userId,
                                                            @Param("offset") long offset,
                                                            @Param("pageSize") long pageSize);

    List<ConversationItemVo> selectAllConversationsByUserId(@Param("userId") Long userId);

    ConversationItemVo selectConversationItemByUserId(@Param("conversationId") Long conversationId,
                                                      @Param("userId") Long userId);

    long countConversationByUserId(@Param("userId") Long userId);

    ImConversationEntity selectSingleConversationByBizKey(@Param("bizKey") String bizKey);

    ImConversationEntity selectGroupConversationByGroupId(@Param("groupId") Long groupId);

    ImConversationEntity selectChannelConversationByGroupId(@Param("groupId") Long groupId);

    ImConversationEntity selectByIdForUpdate(@Param("conversationId") Long conversationId);

    void updateLastMessage(@Param("conversationId") Long conversationId,
                           @Param("lastMessageId") Long lastMessageId,
                           @Param("lastMessagePreview") String lastMessagePreview);

    void updateLastMessageState(@Param("conversationId") Long conversationId,
                                @Param("lastMessageId") Long lastMessageId,
                                @Param("lastMessagePreview") String lastMessagePreview,
                                @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);
}
