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

    long countConversationByUserId(@Param("userId") Long userId);
}
