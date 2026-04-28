package com.echoim.server.common.ratelimit;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalRateLimitService {

    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();

    public void check(String key, int permits, int windowSeconds, String message) {
        long now = Instant.now().getEpochSecond();
        Counter counter = counterMap.compute(key, (ignored, current) -> {
            if (current == null || current.windowStart + windowSeconds <= now) {
                Counter fresh = new Counter();
                fresh.windowStart = now;
                fresh.count = 1;
                return fresh;
            }
            current.count += 1;
            return current;
        });
        if (counter.count > permits) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }

    private static class Counter {
        private long windowStart;
        private int count;
    }
}
