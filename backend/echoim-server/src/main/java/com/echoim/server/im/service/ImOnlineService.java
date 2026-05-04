package com.echoim.server.im.service;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.im.constant.ImRedisKey;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.monitor.WsMetrics;
import com.echoim.server.im.session.ImSessionContext;
import com.echoim.server.im.session.ImSessionManager;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.service.block.BlockService;
import io.netty.channel.Channel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ImOnlineService {

    private final ImSessionManager imSessionManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ImWsPushService imWsPushService;
    private final ImConversationUserMapper imConversationUserMapper;
    private final BlockService blockService;
    private final WsMetrics wsMetrics;

    public ImOnlineService(ImSessionManager imSessionManager,
                           StringRedisTemplate stringRedisTemplate,
                           ImWsPushService imWsPushService,
                           ImConversationUserMapper imConversationUserMapper,
                           BlockService blockService,
                           WsMetrics wsMetrics) {
        this.imSessionManager = imSessionManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.imWsPushService = imWsPushService;
        this.imConversationUserMapper = imConversationUserMapper;
        this.blockService = blockService;
        this.wsMetrics = wsMetrics;
    }

    public void markOnline(LoginUser loginUser, Channel channel, String nodeId, int heartbeatTimeoutSeconds) {
        ImSessionContext context = new ImSessionContext(
                loginUser.getUserId(),
                channel.id().asLongText(),
                LocalDateTime.now()
        );
        Channel previousChannel = imSessionManager.register(context, channel);
        if (previousChannel != null && previousChannel != channel) {
            imWsPushService.pushForceOffline(previousChannel, ErrorCode.UNAUTHORIZED, "账号已在其他连接登录");
            previousChannel.close();
        }

        stringRedisTemplate.opsForValue().set(ImRedisKey.onlineUser(loginUser.getUserId()), "1", heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.heartbeatUser(loginUser.getUserId()), String.valueOf(System.currentTimeMillis()), heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(ImRedisKey.routeUser(loginUser.getUserId()), nodeId, heartbeatTimeoutSeconds * 2L, TimeUnit.SECONDS);

        notifyPresenceToPeers(loginUser.getUserId(), WsMessageType.USER_ONLINE);
        wsMetrics.recordConnectionOpened();
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
        clearPresence(userId);
        notifyPresenceToPeers(userId, WsMessageType.USER_OFFLINE);
        wsMetrics.recordConnectionClosed();
    }

    public void forceOffline(Long userId, int code, String message) {
        if (userId == null) {
            return;
        }
        imSessionManager.getChannel(userId).ifPresent(channel -> {
            if (channel.isActive()) {
                imWsPushService.pushForceOffline(channel, code, message);
                channel.close();
            }
        });
        imSessionManager.remove(userId);
        clearPresence(userId);
    }

    private void clearPresence(Long userId) {
        stringRedisTemplate.delete(ImRedisKey.onlineUser(userId));
        stringRedisTemplate.delete(ImRedisKey.heartbeatUser(userId));
        stringRedisTemplate.delete(ImRedisKey.routeUser(userId));
    }

    public boolean isOnline(Long userId) {
        return imSessionManager.isOnline(userId);
    }

    private void notifyPresenceToPeers(Long userId, WsMessageType presenceType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("online", presenceType == WsMessageType.USER_ONLINE);
        List<Long> blockedByMe = blockService.getBlockedUserIds(userId);
        imConversationUserMapper.selectDistinctPeerUserIds(userId).stream()
                .filter(peerId -> !peerId.equals(userId))
                .filter(peerId -> !blockedByMe.contains(peerId))
                .filter(peerId -> !blockService.isBlocked(peerId, userId))
                .forEach(peerId -> imWsPushService.pushToUser(peerId, presenceType, null, null, data));
    }
}
