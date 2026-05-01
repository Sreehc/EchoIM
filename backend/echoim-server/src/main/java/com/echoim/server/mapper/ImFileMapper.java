package com.echoim.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echoim.server.entity.ImFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImFileMapper extends BaseMapper<ImFileEntity> {

    long countAccessibleByConversation(@Param("fileId") Long fileId,
                                       @Param("userId") Long userId);

    @Select("""
            SELECT f.* FROM im_file f
            INNER JOIN im_message m ON m.file_id = f.id
            WHERE m.conversation_id = #{conversationId}
              AND f.status = 1
            ORDER BY f.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ImFileEntity> selectByConversationId(@Param("conversationId") Long conversationId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*) FROM im_file f
            INNER JOIN im_message m ON m.file_id = f.id
            WHERE m.conversation_id = #{conversationId}
              AND f.status = 1
            """)
    long countByConversationId(@Param("conversationId") Long conversationId);
}
