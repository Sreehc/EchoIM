package com.echoim.server.im.netty;

import com.echoim.server.common.auth.LoginUser;
import io.netty.util.AttributeKey;

public final class ImChannelAttributes {

    private ImChannelAttributes() {
    }

    public static final AttributeKey<LoginUser> LOGIN_USER = AttributeKey.valueOf("echoim.loginUser");
}
