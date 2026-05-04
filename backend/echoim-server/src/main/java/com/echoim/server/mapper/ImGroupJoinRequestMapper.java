package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImGroupJoinRequestEntity;
import com.echoim.server.vo.group.GroupJoinRequestItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupJoinRequestMapper extends BaseMapper<ImGroupJoinRequestEntity> {

    ImGroupJoinRequestEntity selectPendingByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<GroupJoinRequestItemVo> selectPendingItemsByGroupId(@Param("groupId") Long groupId);
}
