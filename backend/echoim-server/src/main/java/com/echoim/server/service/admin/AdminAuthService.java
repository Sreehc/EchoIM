package com.echoim.server.service.admin;

import java.util.Map;

public interface AdminAuthService {

    Map<String, Object> login(String username, String password);

    void logout(Long adminUserId, String username);
}
