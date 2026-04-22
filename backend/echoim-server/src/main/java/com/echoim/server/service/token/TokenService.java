package com.echoim.server.service.token;

import com.echoim.server.common.auth.LoginUser;

public interface TokenService {

    String generateToken(LoginUser loginUser);

    LoginUser parseToken(String token);

    long getExpireSeconds();
}
