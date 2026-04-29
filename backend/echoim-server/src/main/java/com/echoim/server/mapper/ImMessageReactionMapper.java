package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImMessageReactionEntity;
import com.echoim.server.vo.message.MessageReactionStatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImMessageReactionMapper extends BaseMapper<ImMessageReactionEntity> {

    List<MessageReactionStatVo> selectReactionStatsByMessageIds(@Param("messageIds") List<Long> messageIds,
                                                                @Param("viewerUserId") Long viewerUserId);
}
