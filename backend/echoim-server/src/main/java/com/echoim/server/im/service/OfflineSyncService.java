package com.echoim.server.im.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OfflineSyncService {

    public List<Map<String, Object>> sync(Long userId, Long conversationId, Long lastReadSeq) {
        return List.of(
                Map.of(
                        "userId", userId,
                        "conversationId", conversationId,
                        "lastReadSeq", lastReadSeq == null ? 0L : lastReadSeq,
                        "status", "TODO"
                )
        );
    }
}
