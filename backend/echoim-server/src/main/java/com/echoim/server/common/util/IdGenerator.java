package com.echoim.server.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 统一对外展示编号生成器
 * <p>
 * 用户:  U  + 10位随机数字  (例: U3829104756)
 * 群聊:  G  + 10位随机数字  (例: G7102845639)
 * 频道:  CH + 10位随机数字  (例: CH4910382756)
 * 会话:  UUID v4            (例: f47ac10b-58cc-4372-a567-0e02b2c3d479)
 */
public final class IdGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final long LOWER_BOUND = 1_000_000_000L;   // 10^9
    private static final long UPPER_BOUND = 10_000_000_000L;  // 10^10
    private static final long RANGE = UPPER_BOUND - LOWER_BOUND;

    private IdGenerator() {}

    public static String userNo() {
        return "U" + randomDigits();
    }

    public static String groupNo() {
        return "G" + randomDigits();
    }

    public static String channelNo() {
        return "CH" + randomDigits();
    }

    public static String conversationNo() {
        return UUID.randomUUID().toString();
    }

    private static String randomDigits() {
        long num = LOWER_BOUND + Math.abs(RNG.nextLong()) % RANGE;
        return Long.toString(num);
    }
}
