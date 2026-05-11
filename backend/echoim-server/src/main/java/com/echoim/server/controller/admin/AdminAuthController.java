package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.service.token.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/auth", "/admin/auth"})
public class AdminAuthController {

    private final TokenService tokenService;

    public AdminAuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        // TODO: replace stub with real admin credential verification
        LoginUser adminUser = new LoginUser();
        adminUser.setUserId(0L);
        adminUser.setUsername(request.username());
        adminUser.setTokenType("admin");

        String token = tokenService.generateToken(adminUser);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("adminInfo", Map.of(
                "adminUserId", 0L,
                "username", request.username(),
                "nickname", "系统管理员",
                "roleCode", "super_admin"
        ));
        return ApiResponse.success(result);
    }

    public record AdminLoginRequest(
            @NotBlank(message = "管理员账号不能为空") String username,
            @NotBlank(message = "管理员密码不能为空") String password
    ) {
    }
}
