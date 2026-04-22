package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.vo.user.UserProfileVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImUserMapper extends BaseMapper<ImUserEntity> {

    @Select("""
            SELECT id, user_no, username, password_hash, nickname, avatar_url, gender, phone, email, signature,
                   status, last_login_at, created_at, updated_at
            FROM im_user
            WHERE username = #{username}
            LIMIT 1
            """)
    ImUserEntity selectByUsername(@Param("username") String username);

    UserProfileVo selectProfileByUserId(@Param("userId") Long userId);
}
