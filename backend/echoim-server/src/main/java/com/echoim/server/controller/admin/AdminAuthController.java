package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.admin.AdminAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/auth", "/admin/auth"})
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    @RequireAdmin
    public ApiResponse<Void> logout() {
        var admin = LoginUserContext.requireAdmin();
        adminAuthService.logout(admin.getUserId(), admin.getUsername());
        return ApiResponse.success();
    }

    public record AdminLoginRequest(
            @NotBlank(message = "管理员账号不能为空") String username,
            @NotBlank(message = "管理员密码不能为空") String password
    ) {
    }
}
