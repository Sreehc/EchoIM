package com.echoim.server.service.admin;

import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.SysAdminUserEntity;
import com.echoim.server.mapper.SysAdminUserMapper;
import com.echoim.server.service.admin.impl.AdminAuthServiceImpl;
import com.echoim.server.service.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuthServiceImplTest {

    private SysAdminUserMapper sysAdminUserMapper;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private AdminOperationLogService adminOperationLogService;
    private AdminAuthServiceImpl adminAuthService;

    @BeforeEach
    void setUp() {
        sysAdminUserMapper = mock(SysAdminUserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        tokenService = mock(TokenService.class);
        adminOperationLogService = mock(AdminOperationLogService.class);
        adminAuthService = new AdminAuthServiceImpl(
                sysAdminUserMapper,
                passwordEncoder,
                tokenService,
                adminOperationLogService
        );
    }

    @Test
    void loginShouldIssueAdminTokenForSuperAdmin() {
        SysAdminUserEntity adminUser = new SysAdminUserEntity();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setNickname("系统管理员");
        adminUser.setRoleCode("super_admin");
        adminUser.setStatus(1);
        adminUser.setPasswordHash(passwordEncoder.encode("EchoIM@Admin2026!"));
        when(sysAdminUserMapper.selectByUsername("admin")).thenReturn(adminUser);
        when(tokenService.generateToken(any())).thenReturn("admin-token");

        Map<String, Object> result = adminAuthService.login("admin", "EchoIM@Admin2026!");

        assertEquals("admin-token", result.get("token"));
        assertEquals("Bearer", result.get("tokenType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> adminInfo = (Map<String, Object>) result.get("adminInfo");
        assertNotNull(adminInfo);
        assertEquals(1L, adminInfo.get("adminUserId"));
        assertEquals("super_admin", adminInfo.get("roleCode"));
        verify(sysAdminUserMapper).updateById(org.mockito.ArgumentMatchers.<SysAdminUserEntity>argThat(entity ->
                entity.getLastLoginAt() != null && entity.getId().equals(1L)
        ));
        verify(adminOperationLogService).log(eq(1L), eq("ADMIN_AUTH"), eq("LOGIN_SUCCESS"), eq("ADMIN_USER"), eq(1L), any());
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        when(sysAdminUserMapper.selectByUsername("admin")).thenReturn(null);

        BizException error = assertThrows(BizException.class, () -> adminAuthService.login("admin", "bad-password"));

        assertEquals("管理员账号或密码错误", error.getMessage());
        verify(adminOperationLogService).log(eq(0L), eq("ADMIN_AUTH"), eq("LOGIN_FAILURE"), eq("ADMIN_USER"), isNull(), any());
        verify(tokenService, never()).generateToken(any());
    }
}
