package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(Map.of(
                "token", "todo-admin-token",
                "tokenType", "Bearer",
                "adminInfo", Map.of(
                        "adminUserId", 1L,
                        "username", request.username(),
                        "nickname", "系统管理员",
                        "roleCode", "super_admin"
                )
        ));
    }

    public record AdminLoginRequest(
            @NotBlank(message = "管理员账号不能为空") String username,
            @NotBlank(message = "管理员密码不能为空") String password
    ) {
    }
}
