package com.echoim.server.im.constant;

public final class ImRedisKey {

    private ImRedisKey() {
    }

    public static String onlineUser(Long userId) {
        return "echoim:online:user:" + userId;
    }

    public static String heartbeatUser(Long userId) {
        return "echoim:heartbeat:user:" + userId;
    }

    public static String routeUser(Long userId) {
        return "echoim:route:user:" + userId;
    }
}
