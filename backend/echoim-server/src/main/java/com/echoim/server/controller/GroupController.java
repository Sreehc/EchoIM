package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.dto.group.CreateInviteLinkRequestDto;
import com.echoim.server.dto.group.MuteMemberRequestDto;
import com.echoim.server.dto.group.ReviewJoinRequestDto;
import com.echoim.server.dto.group.UpdateGroupMemberRoleRequestDto;
import com.echoim.server.dto.group.UpdateGroupRequestDto;
import com.echoim.server.service.group.GroupService;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import com.echoim.server.vo.group.GroupInviteItemVo;
import com.echoim.server.vo.group.GroupInviteLinkVo;
import com.echoim.server.vo.group.GroupJoinRequestItemVo;
import com.echoim.server.vo.group.GroupMemberItemVo;
import com.echoim.server.vo.group.InvitePreviewVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @RequireLogin
    @PostMapping
    public ApiResponse<GroupCreateVo> create(@Valid @RequestBody CreateGroupRequestDto request) {
        return ApiResponse.success(groupService.createGroup(LoginUserContext.requireUserId(), request));
    }

    @RequireLogin
    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailVo> detail(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroupDetail(LoginUserContext.requireUserId(), groupId));
    }

    @RequireLogin
    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberItemVo>> members(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.listMembers(LoginUserContext.requireUserId(), groupId));
    }

    @RequireLogin
    @PutMapping("/{groupId}")
    public ApiResponse<GroupDetailVo> update(@PathVariable Long groupId,
                                             @Valid @RequestBody UpdateGroupRequestDto request) {
        return ApiResponse.success(groupService.updateGroup(LoginUserContext.requireUserId(), groupId, request));
    }

    @RequireLogin
    @PutMapping("/{groupId}/members/{userId}/role")
    public ApiResponse<Void> updateRole(@PathVariable Long groupId,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody UpdateGroupMemberRoleRequestDto request) {
        groupService.updateMemberRole(LoginUserContext.requireUserId(), groupId, userId, request);
        return ApiResponse.success();
    }

    @RequireLogin
    @PostMapping("/{groupId}/members")
    public ApiResponse<GroupDetailVo> addMembers(@PathVariable Long groupId,
                                                 @Valid @RequestBody AddGroupMembersRequestDto request) {
        return ApiResponse.success(groupService.addMembers(LoginUserContext.requireUserId(), groupId, request));
    }

    @RequireLogin
    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(LoginUserContext.requireUserId(), groupId, userId);
        return ApiResponse.success();
    }

    @RequireLogin
    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leave(@PathVariable Long groupId,
                                   @RequestParam(defaultValue = "true") boolean keepConversation) {
        groupService.leaveGroup(LoginUserContext.requireUserId(), groupId, keepConversation);
        return ApiResponse.success();
    }

    @RequireLogin
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolve(@PathVariable Long groupId) {
        groupService.dissolveGroup(LoginUserContext.requireUserId(), groupId);
        return ApiResponse.success();
    }

    // ==================== 7.3 邀请链接 ====================

    @RequireLogin
    @PostMapping("/{groupId}/invites")
    public ApiResponse<GroupInviteLinkVo> createInviteLink(@PathVariable Long groupId,
                                                           @Valid @RequestBody CreateInviteLinkRequestDto request) {
        return ApiResponse.success(groupService.createInviteLink(LoginUserContext.requireUserId(), groupId, request));
    }

    @RequireLogin
    @GetMapping("/{groupId}/invites")
    public ApiResponse<List<GroupInviteItemVo>> listInviteLinks(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.listInviteLinks(LoginUserContext.requireUserId(), groupId));
    }

    @RequireLogin
    @DeleteMapping("/{groupId}/invites/{inviteId}")
    public ApiResponse<Void> revokeInviteLink(@PathVariable Long groupId, @PathVariable Long inviteId) {
        groupService.revokeInviteLink(LoginUserContext.requireUserId(), groupId, inviteId);
        return ApiResponse.success();
    }

    // Public endpoints for invite preview and join
    @GetMapping("/invite/{token}/preview")
    public ApiResponse<InvitePreviewVo> invitePreview(@PathVariable String token) {
        return ApiResponse.success(groupService.getInvitePreview(token));
    }

    @RequireLogin
    @PostMapping("/invite/{token}/join")
    public ApiResponse<Void> joinByInvite(@PathVariable String token) {
        groupService.joinByInvite(LoginUserContext.requireUserId(), token);
        return ApiResponse.success();
    }

    // ==================== 7.4 禁言 ====================

    @RequireLogin
    @PutMapping("/{groupId}/members/{userId}/mute")
    public ApiResponse<Void> muteMember(@PathVariable Long groupId,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody MuteMemberRequestDto request) {
        groupService.muteMember(LoginUserContext.requireUserId(), groupId, userId, request);
        return ApiResponse.success();
    }

    @RequireLogin
    @DeleteMapping("/{groupId}/members/{userId}/mute")
    public ApiResponse<Void> unmuteMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.unmuteMember(LoginUserContext.requireUserId(), groupId, userId);
        return ApiResponse.success();
    }

    // ==================== 7.4 入群审批 ====================

    @RequireLogin
    @PostMapping("/{groupId}/join-requests")
    public ApiResponse<Void> submitJoinRequest(@PathVariable Long groupId,
                                               @RequestParam(required = false) String applyMsg) {
        groupService.submitJoinRequest(LoginUserContext.requireUserId(), groupId, applyMsg);
        return ApiResponse.success();
    }

    @RequireLogin
    @GetMapping("/{groupId}/join-requests")
    public ApiResponse<List<GroupJoinRequestItemVo>> listJoinRequests(@PathVariable Long groupId) {
        return ApiResponse.success(groupService.listPendingJoinRequests(LoginUserContext.requireUserId(), groupId));
    }

    @RequireLogin
    @PutMapping("/{groupId}/join-requests/{requestId}")
    public ApiResponse<Void> reviewJoinRequest(@PathVariable Long groupId,
                                               @PathVariable Long requestId,
                                               @Valid @RequestBody ReviewJoinRequestDto request) {
        groupService.reviewJoinRequest(LoginUserContext.requireUserId(), groupId, requestId, request);
        return ApiResponse.success();
    }
}
