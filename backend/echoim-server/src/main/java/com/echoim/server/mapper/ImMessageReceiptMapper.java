package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImMessageReceiptEntity;
import com.echoim.server.vo.message.MessageReceiptStatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImMessageReceiptMapper extends BaseMapper<ImMessageReceiptEntity> {

    void insertIgnore(@Param("messageId") Long messageId,
                      @Param("conversationId") Long conversationId,
                      @Param("userId") Long userId,
                      @Param("receiptType") Integer receiptType);

    void insertReadReceiptsUpToSeq(@Param("conversationId") Long conversationId,
                                   @Param("userId") Long userId,
                                   @Param("lastReadSeq") Long lastReadSeq,
                                   @Param("receiptType") Integer receiptType);

    List<MessageReceiptStatVo> selectReceiptStatsByMessageIds(@Param("messageIds") List<Long> messageIds);

    List<MessageReceiptStatVo> selectChannelViewStatsByMessageIds(@Param("messageIds") List<Long> messageIds);
}
