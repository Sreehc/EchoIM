package com.echoim.server.service.auth;

public interface AuthMailService {

    void sendVerificationCode(String email, String sceneLabel, String code);
}
