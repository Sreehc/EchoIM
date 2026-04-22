package com.echoim.server.im.service;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.im.constant.ImRedisKey;
import com.echoim.server.im.session.ImSessionContext;
import com.echoim.server.im.session.ImSessionManager;
import io.netty.channel.Channel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class ImOnlineService {

    private final ImSessionManager imSessionManager;
    private final StringRedisTemplate stringRedisTemplate;

    public ImOnlineService(ImSessionManager imSessionManager, StringRedisTemplate stringRedisTemplate) {
        this.imSessionManager = imSessionManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void markOnline(LoginUser loginUser, Channel channel, String nodeId, int heartbeatTimeoutSeconds) {
        ImSessionContext context = new ImSessionContext(
                loginUser.getUserId(),
                channel.id().asLongText(),
                LocalDateTime.now()
        );
        Channel previousChannel = imSessionManager.register(context, channel);
        if (previousChannel != null && previousChannel != channel) {
            previousChannel.close();
        }

        stringRedisTemplate.opsForValue().set(ImRedisKey.onlineUser(loginUser.getUserId()), "1", heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.heartbeatUser(loginUser.getUserId()), String.valueOf(System.currentTimeMillis()), heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.routeUser(loginUser.getUserId()), nodeId, heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
    }

    public void refreshHeartbeat(Long userId, String nodeId, int heartbeatTimeoutSeconds) {
        stringRedisTemplate.opsForValue().set(ImRedisKey.onlineUser(userId), "1", heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.heartbeatUser(userId), String.valueOf(System.currentTimeMillis()), heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.routeUser(userId), nodeId, heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
    }

    public void markOffline(Long userId, Channel channel) {
        if (userId == null) {
            return;
        }
        if (!imSessionManager.removeIfMatch(userId, channel)) {
            return;
        }
        stringRedisTemplate.delete(ImRedisKey.onlineUser(userId));
        stringRedisTemplate.delete(ImRedisKey.heartbeatUser(userId));
        stringRedisTemplate.delete(ImRedisKey.routeUser(userId));
    }
}
