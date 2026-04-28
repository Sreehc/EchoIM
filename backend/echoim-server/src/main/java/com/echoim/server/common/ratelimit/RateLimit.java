package com.echoim.server.common.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    KeyType keyType();

    String name();

    int permits();

    int windowSeconds();

    String message() default "请求过于频繁";

    enum KeyType {
        IP,
        USER
    }
}
