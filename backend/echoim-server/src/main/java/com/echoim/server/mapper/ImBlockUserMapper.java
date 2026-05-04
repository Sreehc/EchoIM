package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImBlockUserEntity;
import com.echoim.server.vo.block.BlockedUserItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImBlockUserMapper extends BaseMapper<ImBlockUserEntity> {

    ImBlockUserEntity selectByUserAndBlocked(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    List<BlockedUserItemVo> selectBlockedItemsByUserId(@Param("userId") Long userId);

    List<Long> selectBlockedUserIdsByUserId(@Param("userId") Long userId);
}
