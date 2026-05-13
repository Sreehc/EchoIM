package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.SysAdminUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAdminUserMapper extends BaseMapper<SysAdminUserEntity> {

    SysAdminUserEntity selectByUsername(String username);
}
