package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImFriendEntity;
import com.echoim.server.vo.friend.FriendItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImFriendMapper extends BaseMapper<ImFriendEntity> {

    List<FriendItemVo> selectFriendListByUserId(@Param("userId") Long userId);
}
