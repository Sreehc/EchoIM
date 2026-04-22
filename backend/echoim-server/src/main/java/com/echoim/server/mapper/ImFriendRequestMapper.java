package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImFriendRequestEntity;
import com.echoim.server.vo.friend.FriendRequestItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImFriendRequestMapper extends BaseMapper<ImFriendRequestEntity> {

    @Select("""
            SELECT COUNT(1)
            FROM im_friend_request
            WHERE status = 0
              AND ((from_user_id = #{userAId} AND to_user_id = #{userBId})
                OR (from_user_id = #{userBId} AND to_user_id = #{userAId}))
            """)
    long countPendingRequest(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    @Select("""
            SELECT COUNT(1)
            FROM im_friend
            WHERE status = 1
              AND ((user_id = #{userAId} AND friend_user_id = #{userBId})
                OR (user_id = #{userBId} AND friend_user_id = #{userAId}))
            """)
    long countExistingFriendRelation(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    List<FriendRequestItemVo> selectRelatedRequests(@Param("userId") Long userId);
}
