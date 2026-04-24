package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImGroupEntity;
import com.echoim.server.vo.group.GroupDetailVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupEntity> {

    GroupDetailVo selectGroupDetail(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
