package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.service.group.GroupService;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
