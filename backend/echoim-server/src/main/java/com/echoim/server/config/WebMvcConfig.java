package com.echoim.server.config;

import com.echoim.server.interceptor.LoginInterceptor;
import com.echoim.server.common.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor,
                        RateLimitInterceptor rateLimitInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
    }
}
