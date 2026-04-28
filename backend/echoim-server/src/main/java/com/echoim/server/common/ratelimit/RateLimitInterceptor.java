package com.echoim.server.common.ratelimit;

import com.echoim.server.common.auth.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LocalRateLimitService localRateLimitService;

    public RateLimitInterceptor(LocalRateLimitService localRateLimitService) {
        this.localRateLimitService = localRateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }
        String dimensionKey = switch (rateLimit.keyType()) {
            case IP -> resolveIp(request);
            case USER -> String.valueOf(LoginUserContext.requireUserId());
        };
        String key = rateLimit.name() + ":" + dimensionKey;
        localRateLimitService.check(key, rateLimit.permits(), rateLimit.windowSeconds(), rateLimit.message());
        return true;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
