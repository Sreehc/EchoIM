package com.echoim.server.common.log;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every HTTP request with method, path, status, duration, userId, and traceId.
 * Runs after TraceIdFilter so MDC traceId is already set.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LoginUser loginUser = LoginUserContext.get();
            Long userId = loginUser != null ? loginUser.getUserId() : null;

            log.info("method={} path={} status={} duration={}ms userId={} traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    userId,
                    TraceContext.get());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip health check and static resources to reduce noise
        return path.startsWith("/api/health")
                || path.startsWith("/actuator")
                || path.endsWith(".ico");
    }
}
