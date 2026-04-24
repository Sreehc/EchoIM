package com.echoim.server.im.service;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.im.model.WsMessage;

import java.util.Map;

public interface ImGroupChatService {

    Map<String, Object> sendGroup(LoginUser loginUser, WsMessage message);
}
