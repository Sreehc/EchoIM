package com.echoim.server.common.auth;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;

public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long requireUserId() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return loginUser.getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
