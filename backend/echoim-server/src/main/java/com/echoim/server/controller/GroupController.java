package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(Map.of(
                "groupId", 20001L,
                "groupName", request.groupName(),
                "memberCount", request.memberIds().size() + 1
        ));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long groupId) {
        return ApiResponse.success(Map.of(
                "groupId", groupId,
                "groupName", "Echo 项目讨论群",
                "ownerUserId", 10001L,
                "notice", "欢迎加入 EchoIM 项目讨论群"
        ));
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<Map<String, Object>> addMembers(@PathVariable Long groupId,
                                                       @Valid @RequestBody AddGroupMembersRequest request) {
        return ApiResponse.success(Map.of(
                "groupId", groupId,
                "memberIds", request.memberIds()
        ));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        return ApiResponse.success();
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolve(@PathVariable Long groupId) {
        return ApiResponse.success();
    }

    public record CreateGroupRequest(
            @NotBlank(message = "群名称不能为空") String groupName,
            @NotEmpty(message = "群成员不能为空") List<Long> memberIds
    ) {
    }

    public record AddGroupMembersRequest(
            @NotEmpty(message = "待添加成员不能为空") List<Long> memberIds
    ) {
    }
}
