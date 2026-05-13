package com.echoim.server.service.admin.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.SysAdminUserEntity;
import com.echoim.server.mapper.SysAdminUserMapper;
import com.echoim.server.service.admin.AdminAuthService;
import com.echoim.server.service.admin.AdminOperationLogService;
import com.echoim.server.service.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String TOKEN_TYPE_ADMIN = "admin";
    private static final String ROLE_SUPER_ADMIN = "super_admin";

    private final SysAdminUserMapper sysAdminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AdminOperationLogService adminOperationLogService;

    public AdminAuthServiceImpl(SysAdminUserMapper sysAdminUserMapper,
                                PasswordEncoder passwordEncoder,
                                TokenService tokenService,
                                AdminOperationLogService adminOperationLogService) {
        this.sysAdminUserMapper = sysAdminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        SysAdminUserEntity adminUser = sysAdminUserMapper.selectByUsername(normalizedUsername);
        if (adminUser == null || !passwordEncoder.matches(password, adminUser.getPasswordHash())) {
            adminOperationLogService.log(0L, "ADMIN_AUTH", "LOGIN_FAILURE", "ADMIN_USER", null, Map.of(
                    "username", normalizedUsername,
                    "reason", "INVALID_CREDENTIALS"
            ));
            throw new BizException(ErrorCode.UNAUTHORIZED, "管理员账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(adminUser.getStatus())) {
            adminOperationLogService.log(adminUser.getId(), "ADMIN_AUTH", "LOGIN_FAILURE", "ADMIN_USER", adminUser.getId(), Map.of(
                    "username", adminUser.getUsername(),
                    "reason", "ACCOUNT_DISABLED"
            ));
            throw new BizException(ErrorCode.FORBIDDEN, "管理员账号已停用");
        }
        if (!ROLE_SUPER_ADMIN.equals(adminUser.getRoleCode())) {
            adminOperationLogService.log(adminUser.getId(), "ADMIN_AUTH", "LOGIN_FAILURE", "ADMIN_USER", adminUser.getId(), Map.of(
                    "username", adminUser.getUsername(),
                    "roleCode", adminUser.getRoleCode(),
                    "reason", "ROLE_UNSUPPORTED"
            ));
            throw new BizException(ErrorCode.FORBIDDEN, "当前管理员角色未开放");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(adminUser.getId());
        loginUser.setUsername(adminUser.getUsername());
        loginUser.setTokenType(TOKEN_TYPE_ADMIN);
        loginUser.setRoleCode(adminUser.getRoleCode());

        String token = tokenService.generateToken(loginUser);
        adminUser.setLastLoginAt(LocalDateTime.now());
        sysAdminUserMapper.updateById(adminUser);

        adminOperationLogService.log(adminUser.getId(), "ADMIN_AUTH", "LOGIN_SUCCESS", "ADMIN_USER", adminUser.getId(), Map.of(
                "username", adminUser.getUsername(),
                "roleCode", adminUser.getRoleCode()
        ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("adminInfo", Map.of(
                "adminUserId", adminUser.getId(),
                "username", adminUser.getUsername(),
                "nickname", adminUser.getNickname(),
                "roleCode", adminUser.getRoleCode()
        ));
        return result;
    }

    @Override
    public void logout(Long adminUserId, String username) {
        adminOperationLogService.log(adminUserId, "ADMIN_AUTH", "LOGOUT", "ADMIN_USER", adminUserId, Map.of(
                "username", username == null ? "" : username
        ));
    }
}
