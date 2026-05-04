package com.echoim.server.service.group;

import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.dto.group.CreateInviteLinkRequestDto;
import com.echoim.server.dto.group.MuteMemberRequestDto;
import com.echoim.server.dto.group.ReviewJoinRequestDto;
import com.echoim.server.dto.group.UpdateGroupMemberRoleRequestDto;
import com.echoim.server.dto.group.UpdateGroupRequestDto;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import com.echoim.server.vo.group.GroupInviteItemVo;
import com.echoim.server.vo.group.GroupInviteLinkVo;
import com.echoim.server.vo.group.GroupJoinRequestItemVo;
import com.echoim.server.vo.group.GroupMemberItemVo;
import com.echoim.server.vo.group.InvitePreviewVo;

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

    // 7.3 邀请链接
    GroupInviteLinkVo createInviteLink(Long currentUserId, Long groupId, CreateInviteLinkRequestDto requestDto);

    List<GroupInviteItemVo> listInviteLinks(Long currentUserId, Long groupId);

    void revokeInviteLink(Long currentUserId, Long groupId, Long inviteId);

    InvitePreviewVo getInvitePreview(String token);

    void joinByInvite(Long currentUserId, String token);

    // 7.4 禁言
    void muteMember(Long currentUserId, Long groupId, Long userId, MuteMemberRequestDto requestDto);

    void unmuteMember(Long currentUserId, Long groupId, Long userId);

    // 7.4 入群审批
    void submitJoinRequest(Long currentUserId, Long groupId, String applyMsg);

    void reviewJoinRequest(Long currentUserId, Long groupId, Long requestId, ReviewJoinRequestDto requestDto);

    List<GroupJoinRequestItemVo> listPendingJoinRequests(Long currentUserId, Long groupId);
}
