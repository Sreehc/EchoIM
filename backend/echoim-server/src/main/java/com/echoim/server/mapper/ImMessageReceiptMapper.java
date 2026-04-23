package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImMessageReceiptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
