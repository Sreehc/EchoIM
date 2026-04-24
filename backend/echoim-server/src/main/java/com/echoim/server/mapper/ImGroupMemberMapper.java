package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImGroupMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupMemberMapper extends BaseMapper<ImGroupMemberEntity> {

    ImGroupMemberEntity selectByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    ImGroupMemberEntity selectActiveByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<ImGroupMemberEntity> selectActiveMembersByGroupId(@Param("groupId") Long groupId);

    List<Long> selectActiveUserIdsByGroupId(@Param("groupId") Long groupId);

    long countActiveMembers(@Param("groupId") Long groupId);
}
