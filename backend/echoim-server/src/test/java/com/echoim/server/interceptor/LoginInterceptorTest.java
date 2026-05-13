package com.echoim.server.interceptor;

import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.service.admin.AdminOperationLogService;
import com.echoim.server.service.token.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LoginInterceptorTest {

    private TokenService tokenService;
    private AdminOperationLogService adminOperationLogService;
    private LoginInterceptor loginInterceptor;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        adminOperationLogService = mock(AdminOperationLogService.class);
        loginInterceptor = new LoginInterceptor(tokenService, adminOperationLogService);
    }

    @AfterEach
    void tearDown() {
        com.echoim.server.common.auth.LoginUserContext.clear();
    }

    @Test
    void shouldRejectNonAdminTokenForAdminEndpoint() throws Exception {
        LoginUser user = new LoginUser();
        user.setUserId(10001L);
        user.setUsername("echo_demo_01");
        user.setTokenType("user");
        when(tokenService.parseToken("user-token")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token");
        request.setRequestURI("/api/admin/notices");

        BizException error = assertThrows(BizException.class, () ->
                loginInterceptor.preHandle(request, new MockHttpServletResponse(), adminHandlerMethod())
        );

        verify(adminOperationLogService).log(eq(10001L), eq("ADMIN_AUTH"), eq("ACCESS_DENIED"), eq("ADMIN_ENDPOINT"), isNull(), any());
        org.junit.jupiter.api.Assertions.assertEquals("需要管理员身份", error.getMessage());
    }

    @Test
    void shouldAllowAdminTokenForAdminEndpoint() throws Exception {
        LoginUser admin = new LoginUser();
        admin.setUserId(1L);
        admin.setUsername("admin");
        admin.setTokenType("admin");
        when(tokenService.parseToken("admin-token")).thenReturn(admin);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token");
        request.setRequestURI("/api/admin/notices");

        assertDoesNotThrow(() ->
                loginInterceptor.preHandle(request, new MockHttpServletResponse(), adminHandlerMethod())
        );
    }

    private HandlerMethod adminHandlerMethod() throws NoSuchMethodException {
        Method method = DummyAdminController.class.getMethod("list");
        return new HandlerMethod(new DummyAdminController(), method);
    }

    @RequireAdmin
    private static class DummyAdminController {
        public void list() {
        }
    }
}
