package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImSensitiveWordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImSensitiveWordMapper extends BaseMapper<ImSensitiveWordEntity> {

    List<ImSensitiveWordEntity> selectAllEnabled();
}
