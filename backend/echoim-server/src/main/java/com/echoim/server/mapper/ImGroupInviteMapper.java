package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImGroupInviteEntity;
import com.echoim.server.vo.group.GroupInviteItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupInviteMapper extends BaseMapper<ImGroupInviteEntity> {

    ImGroupInviteEntity selectValidByToken(@Param("token") String token);

    List<GroupInviteItemVo> selectActiveInvitesByGroupId(@Param("groupId") Long groupId);
}
