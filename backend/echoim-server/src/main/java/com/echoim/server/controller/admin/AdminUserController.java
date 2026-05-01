package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword
    ) {
        List<Map<String, Object>> list = List.of(
                Map.of(
                        "userId", 10001L,
                        "username", "echo_demo_01",
                        "nickname", "Echo用户01",
                        "status", 1,
                        "keyword", keyword == null ? "" : keyword
                )
        );
        return ApiResponse.success(new PageResponse<>(list, pageNo, pageSize, list.size()));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.success(Map.of("userId", id, "status", request.status()));
    }

    @PutMapping("/{id}/offline")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        return ApiResponse.success();
    }

    public record AdminUserStatusRequest(
            @NotNull(message = "用户状态不能为空") Integer status
    ) {
    }
}
