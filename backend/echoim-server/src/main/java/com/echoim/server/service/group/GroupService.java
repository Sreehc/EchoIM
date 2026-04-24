package com.echoim.server.service.group;

import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;

public interface GroupService {

    GroupCreateVo createGroup(Long currentUserId, CreateGroupRequestDto requestDto);

    GroupDetailVo getGroupDetail(Long currentUserId, Long groupId);

    GroupDetailVo addMembers(Long currentUserId, Long groupId, AddGroupMembersRequestDto requestDto);

    void removeMember(Long currentUserId, Long groupId, Long userId);

    void leaveGroup(Long currentUserId, Long groupId, boolean keepConversation);

    void dissolveGroup(Long currentUserId, Long groupId);
}
