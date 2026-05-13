package com.echoim.server.interceptor;

import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.service.admin.AdminOperationLogService;
import com.echoim.server.service.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AdminOperationLogService adminOperationLogService;

    public LoginInterceptor(TokenService tokenService,
                            AdminOperationLogService adminOperationLogService) {
        this.tokenService = tokenService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requiresAdmin = requiresAdmin(handlerMethod);
        if (!requiresLogin(handlerMethod) && !requiresAdmin) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效");
        }

        LoginUser loginUser = tokenService.parseToken(token);
        if (requiresAdmin && !"admin".equalsIgnoreCase(loginUser.getTokenType())) {
            adminOperationLogService.log(loginUser.getUserId(), "ADMIN_AUTH", "ACCESS_DENIED", "ADMIN_ENDPOINT", null, java.util.Map.of(
                    "tokenType", loginUser.getTokenType() == null ? "" : loginUser.getTokenType(),
                    "path", request.getRequestURI(),
                    "reason", "TOKEN_TYPE_INVALID"
            ));
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员身份");
        }
        LoginUserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private boolean requiresLogin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireLogin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireLogin.class);
    }

    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }
}
