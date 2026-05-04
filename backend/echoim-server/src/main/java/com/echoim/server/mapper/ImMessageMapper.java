package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.search.GlobalSearchMessageItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessageEntity> {

    List<MessageItemVo> selectMessagePageByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                                   @Param("userId") Long userId,
                                                                   @Param("offset") long offset,
                                                                   @Param("pageSize") long pageSize);

    long countMessageByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                               @Param("userId") Long userId);

    List<MessageItemVo> selectMessageCursorByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                                     @Param("userId") Long userId,
                                                                     @Param("maxSeqNo") Long maxSeqNo,
                                                                     @Param("pageSize") long pageSize);

    List<MessageItemVo> selectMessageAfterSeqByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                                       @Param("userId") Long userId,
                                                                       @Param("afterSeq") Long afterSeq,
                                                                       @Param("pageSize") long pageSize);

    long countMessageAfterSeqByConversationIdAndUserId(@Param("conversationId") Long conversationId,
                                                       @Param("userId") Long userId,
                                                       @Param("afterSeq") Long afterSeq);

    Long selectMaxSeqNoByConversationId(@Param("conversationId") Long conversationId);

    ImMessageEntity selectByFromUserIdAndClientMsgId(@Param("fromUserId") Long fromUserId,
                                                     @Param("clientMsgId") String clientMsgId);

    ImMessageEntity selectAccessibleEntityByIdAndUserId(@Param("messageId") Long messageId,
                                                        @Param("userId") Long userId);

    List<GlobalSearchMessageItemVo> selectGlobalSearchMessages(@Param("userId") Long userId,
                                                               @Param("keyword") String keyword,
                                                               @Param("limit") long limit);

    ImMessageEntity selectLatestByConversationId(@Param("conversationId") Long conversationId);
}
