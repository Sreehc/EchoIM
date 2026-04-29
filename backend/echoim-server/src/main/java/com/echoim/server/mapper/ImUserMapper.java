package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.vo.user.UserPublicProfileVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.echoim.server.vo.user.UserSearchItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    ImUserEntity selectByUsernameExcludingUserId(@Param("username") String username,
                                                 @Param("excludeUserId") Long excludeUserId);

    UserProfileVo selectProfileByUserId(@Param("userId") Long userId);

    List<UserSearchItemVo> selectSearchPage(@Param("currentUserId") Long currentUserId,
                                            @Param("keyword") String keyword,
                                            @Param("offset") long offset,
                                            @Param("pageSize") long pageSize);

    long countSearchUsers(@Param("currentUserId") Long currentUserId,
                          @Param("keyword") String keyword);

    UserPublicProfileVo selectPublicProfileByUserId(@Param("currentUserId") Long currentUserId,
                                                    @Param("targetUserId") Long targetUserId);

    UserPublicProfileVo selectPublicProfileByUsername(@Param("currentUserId") Long currentUserId,
                                                      @Param("username") String username);
}
