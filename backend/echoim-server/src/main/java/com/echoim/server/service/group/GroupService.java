package com.echoim.server.service.group;

import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.dto.group.UpdateGroupMemberRoleRequestDto;
import com.echoim.server.dto.group.UpdateGroupRequestDto;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import com.echoim.server.vo.group.GroupMemberItemVo;

import java.util.List;

public interface GroupService {

    GroupCreateVo createGroup(Long currentUserId, CreateGroupRequestDto requestDto);

    GroupDetailVo getGroupDetail(Long currentUserId, Long groupId);

    List<GroupMemberItemVo> listMembers(Long currentUserId, Long groupId);

    GroupDetailVo updateGroup(Long currentUserId, Long groupId, UpdateGroupRequestDto requestDto);

    void updateMemberRole(Long currentUserId, Long groupId, Long userId, UpdateGroupMemberRoleRequestDto requestDto);

    GroupDetailVo addMembers(Long currentUserId, Long groupId, AddGroupMembersRequestDto requestDto);

    void removeMember(Long currentUserId, Long groupId, Long userId);

    void leaveGroup(Long currentUserId, Long groupId, boolean keepConversation);

    void dissolveGroup(Long currentUserId, Long groupId);
}
