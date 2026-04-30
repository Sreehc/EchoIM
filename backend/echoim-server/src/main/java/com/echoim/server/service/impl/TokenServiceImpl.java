package com.echoim.server.service.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.JwtProperties;
import com.echoim.server.service.token.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenServiceImpl implements TokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public TokenServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret 未配置，请设置 ECHOIM_JWT_SECRET 或在 application-local.yml 中配置 echoim.jwt.secret");
        }
        try {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException ex) {
            throw new IllegalStateException("JWT secret 长度不足，至少需要 32 个字符", ex);
        }
    }

    @Override
    public String generateToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim("userId", loginUser.getUserId())
                .claim("username", loginUser.getUsername())
                .claim("tokenType", loginUser.getTokenType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            LoginUser loginUser = new LoginUser();
            Number userId = claims.get("userId", Number.class);
            loginUser.setUserId(userId == null ? null : userId.longValue());
            loginUser.setUsername(claims.get("username", String.class));
            loginUser.setTokenType(claims.get("tokenType", String.class));
            Date expiration = claims.getExpiration();
            loginUser.setExpireAtMillis(expiration == null ? null : expiration.getTime());
            return loginUser;
        } catch (ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "token 已过期");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效");
        }
    }

    @Override
    public long getExpireSeconds() {
        return jwtProperties.getExpireSeconds();
    }
}
