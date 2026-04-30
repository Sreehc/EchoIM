package com.echoim.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.util.UsernameRules;
import com.echoim.server.config.AuthProperties;
import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.entity.ImAuthSecurityEventEntity;
import com.echoim.server.entity.ImAuthTrustedDeviceEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.im.service.ImOnlineService;
import com.echoim.server.mapper.ImAuthSecurityEventMapper;
import com.echoim.server.mapper.ImAuthTrustedDeviceMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.auth.AuthMailService;
import com.echoim.server.service.auth.AuthService;
import com.echoim.server.service.token.TokenService;
import com.echoim.server.vo.auth.CodeDispatchVo;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.LoginUserVo;
import com.echoim.server.vo.auth.RecoveryVerifyVo;
import com.echoim.server.vo.auth.RegisterResponseVo;
import com.echoim.server.vo.auth.SecurityEventItemVo;
import com.echoim.server.vo.auth.TrustedDeviceItemVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int USER_STATUS_NORMAL = 1;
    private static final String LOGIN_STATUS_AUTHENTICATED = "authenticated";
    private static final String LOGIN_STATUS_CHALLENGE_REQUIRED = "challenge_required";
    private static final String EVENT_SUCCESS = "SUCCESS";
    private static final String EVENT_FAILURE = "FAILURE";
    private static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    private static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
    private static final String EVENT_LOGIN_CHALLENGE_SENT = "LOGIN_CHALLENGE_SENT";
    private static final String EVENT_LOGIN_CHALLENGE_VERIFIED = "LOGIN_CHALLENGE_VERIFIED";
    private static final String EVENT_LOGIN_CHALLENGE_RESENT = "LOGIN_CHALLENGE_RESENT";
    private static final String EVENT_TRUSTED_DEVICE_LOGIN = "TRUSTED_DEVICE_LOGIN";
    private static final String EVENT_TOKEN_REFRESH = "TOKEN_REFRESH";
    private static final String EVENT_LOGOUT = "LOGOUT";
    private static final String EVENT_TRUSTED_DEVICE_REVOKE = "TRUSTED_DEVICE_REVOKE";
    private static final String EVENT_TRUSTED_DEVICE_REVOKE_ALL = "TRUSTED_DEVICE_REVOKE_ALL";
    private static final String EVENT_RECOVERY_CODE_SENT = "RECOVERY_CODE_SENT";
    private static final String EVENT_RECOVERY_CODE_VERIFIED = "RECOVERY_CODE_VERIFIED";
    private static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    private static final String EVENT_EMAIL_BIND_CODE_SENT = "EMAIL_BIND_CODE_SENT";
    private static final String EVENT_EMAIL_BOUND = "EMAIL_BOUND";
    private static final String LOGIN_CHALLENGE_KEY_PREFIX = "echoim:auth:login:challenge:";
    private static final String RECOVERY_CODE_KEY_PREFIX = "echoim:auth:recovery:code:";
    private static final String RECOVERY_TOKEN_KEY_PREFIX = "echoim:auth:recovery:token:";
    private static final String EMAIL_BIND_KEY_PREFIX = "echoim:auth:email:bind:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "echoim:auth:refresh:";
    private static final String USER_REFRESH_INDEX_KEY_PREFIX = "echoim:auth:refresh:user:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ImUserMapper imUserMapper;
    private final ImAuthTrustedDeviceMapper trustedDeviceMapper;
    private final ImAuthSecurityEventMapper securityEventMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final AuthMailService authMailService;
    private final AuthProperties authProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ImOnlineService imOnlineService;

    public AuthServiceImpl(ImUserMapper imUserMapper,
                           ImAuthTrustedDeviceMapper trustedDeviceMapper,
                           ImAuthSecurityEventMapper securityEventMapper,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           AuditLogService auditLogService,
                           AuthMailService authMailService,
                           AuthProperties authProperties,
                           StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper,
                           ImOnlineService imOnlineService) {
        this.imUserMapper = imUserMapper;
        this.trustedDeviceMapper = trustedDeviceMapper;
        this.securityEventMapper = securityEventMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.authMailService = authMailService;
        this.authProperties = authProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.imOnlineService = imOnlineService;
    }

    @Override
    public RegisterResponseVo register(RegisterRequestDto requestDto) {
        String normalizedUsername = normalizeAndValidateUsername(requestDto.getUsername());
        ImUserEntity existingUser = imUserMapper.selectByUsername(normalizedUsername);
        if (existingUser != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        ImUserEntity entity = new ImUserEntity();
        entity.setUserNo("TMP_" + System.nanoTime());
        entity.setUsername(normalizedUsername);
        entity.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        entity.setNickname(requestDto.getNickname());
        entity.setStatus(USER_STATUS_NORMAL);
        imUserMapper.insert(entity);

        entity.setUserNo("E" + entity.getId());
        imUserMapper.updateById(entity);

        RegisterResponseVo responseVo = new RegisterResponseVo();
        responseVo.setUserId(entity.getId());
        responseVo.setUsername(entity.getUsername());
        responseVo.setNickname(entity.getNickname());
        auditLogService.log("AUTH_REGISTER", Map.of("userId", entity.getId(), "username", entity.getUsername()));
        return responseVo;
    }

    @Override
    public LoginResponseVo login(LoginRequestDto requestDto, String ip, String userAgent) {
        ImUserEntity userEntity = requireLoginUser(requestDto.getUsername());
        validateUserStatus(userEntity);
        if (!passwordEncoder.matches(requestDto.getPassword(), userEntity.getPasswordHash())) {
            recordSecurityEvent(userEntity.getId(), EVENT_LOGIN_FAILURE, EVENT_FAILURE, ip, userAgent,
                    Map.of("username", userEntity.getUsername(), "reason", "PASSWORD_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        boolean trustDevice = Boolean.TRUE.equals(requestDto.getTrustDevice());
        String deviceFingerprint = normalizeDeviceFingerprint(requestDto.getDeviceFingerprint());
        if (trustDevice && !StringUtils.hasText(deviceFingerprint)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "设备信息缺失，请刷新后重试");
        }

        if (trustDevice && StringUtils.hasText(userEntity.getEmail())) {
            LoginChallengeState state = new LoginChallengeState();
            state.setUserId(userEntity.getId());
            state.setEmail(userEntity.getEmail());
            state.setDeviceFingerprint(deviceFingerprint);
            state.setDeviceName(normalizeDeviceName(requestDto.getDeviceName()));
            state.setCode(generateCode());
            state.setSentAt(System.currentTimeMillis());
            state.setAttempts(0);
            String ticket = randomToken();
            storeRedisJson(loginChallengeKey(ticket), state, authProperties.getCodeExpireSeconds());
            authMailService.sendVerificationCode(userEntity.getEmail(), "登录验证", state.getCode());
            recordSecurityEvent(userEntity.getId(), EVENT_LOGIN_CHALLENGE_SENT, EVENT_SUCCESS, ip, userAgent,
                    Map.of("username", userEntity.getUsername(), "deviceName", state.getDeviceName()));
            LoginResponseVo responseVo = new LoginResponseVo();
            responseVo.setStatus(LOGIN_STATUS_CHALLENGE_REQUIRED);
            responseVo.setChallengeTicket(ticket);
            responseVo.setMaskedEmail(maskEmail(userEntity.getEmail()));
            responseVo.setResendAfterSeconds(authProperties.getResendCooldownSeconds());
            return responseVo;
        }

        recordSecurityEvent(userEntity.getId(), EVENT_LOGIN_SUCCESS, EVENT_SUCCESS, ip, userAgent,
                Map.of("username", userEntity.getUsername(), "trustedDevice", false));
        return buildAuthenticatedResponse(userEntity, null, null);
    }

    @Override
    public LoginResponseVo verifyLoginChallenge(String challengeTicket, String code, String ip, String userAgent) {
        LoginChallengeState state = loadRedisJson(loginChallengeKey(challengeTicket), LoginChallengeState.class);
        if (state == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码已失效，请重新登录");
        }

        if (state.getAttempts() >= authProperties.getMaxVerifyAttempts()) {
            stringRedisTemplate.delete(loginChallengeKey(challengeTicket));
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新登录");
        }

        String normalizedCode = normalizeCode(code);
        if (!state.getCode().equals(normalizedCode)) {
            state.setAttempts(state.getAttempts() + 1);
            long ttlSeconds = Math.max(1L, remainingSeconds(loginChallengeKey(challengeTicket)));
            storeRedisJson(loginChallengeKey(challengeTicket), state, ttlSeconds);
            recordSecurityEvent(state.getUserId(), EVENT_LOGIN_CHALLENGE_VERIFIED, EVENT_FAILURE, ip, userAgent,
                    Map.of("reason", "CODE_INVALID", "deviceName", state.getDeviceName()));
            throw new BizException(ErrorCode.UNAUTHORIZED, "验证码错误");
        }

        stringRedisTemplate.delete(loginChallengeKey(challengeTicket));
        ImUserEntity userEntity = requireUserById(state.getUserId());
        validateUserStatus(userEntity);
        TrustedDeviceGrant trustedDeviceGrant = issueTrustedDeviceGrant(userEntity.getId(), state.getDeviceFingerprint(), state.getDeviceName());
        recordSecurityEvent(userEntity.getId(), EVENT_LOGIN_CHALLENGE_VERIFIED, EVENT_SUCCESS, ip, userAgent,
                Map.of("deviceName", state.getDeviceName()));
        return buildAuthenticatedResponse(userEntity, trustedDeviceGrant.grantToken(), trustedDeviceGrant.expireAt());
    }

    @Override
    public CodeDispatchVo resendLoginChallenge(String challengeTicket, String ip, String userAgent) {
        LoginChallengeState state = loadRedisJson(loginChallengeKey(challengeTicket), LoginChallengeState.class);
        if (state == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码已失效，请重新登录");
        }
        ensureResendAllowed(state.getSentAt());
        state.setCode(generateCode());
        state.setSentAt(System.currentTimeMillis());
        long ttlSeconds = Math.max(authProperties.getCodeExpireSeconds(), remainingSeconds(loginChallengeKey(challengeTicket)));
        storeRedisJson(loginChallengeKey(challengeTicket), state, ttlSeconds);
        authMailService.sendVerificationCode(state.getEmail(), "登录验证", state.getCode());
        recordSecurityEvent(state.getUserId(), EVENT_LOGIN_CHALLENGE_RESENT, EVENT_SUCCESS, ip, userAgent,
                Map.of("deviceName", state.getDeviceName()));
        return buildCodeDispatch(maskEmail(state.getEmail()));
    }

    @Override
    public LoginResponseVo loginWithTrustedDevice(Long userId, String deviceFingerprint, String grantToken, String ip, String userAgent) {
        String normalizedFingerprint = normalizeDeviceFingerprint(deviceFingerprint);
        if (userId == null || !StringUtils.hasText(normalizedFingerprint) || !StringUtils.hasText(grantToken)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "受信设备信息不完整");
        }

        LocalDateTime now = LocalDateTime.now();
        ImAuthTrustedDeviceEntity device = trustedDeviceMapper.selectOne(new LambdaQueryWrapper<ImAuthTrustedDeviceEntity>()
                .eq(ImAuthTrustedDeviceEntity::getUserId, userId)
                .eq(ImAuthTrustedDeviceEntity::getDeviceFingerprint, normalizedFingerprint)
                .eq(ImAuthTrustedDeviceEntity::getGrantTokenHash, sha256(grantToken))
                .isNull(ImAuthTrustedDeviceEntity::getRevokedAt)
                .gt(ImAuthTrustedDeviceEntity::getExpireAt, now)
                .last("LIMIT 1"));
        if (device == null) {
            recordSecurityEvent(userId, EVENT_TRUSTED_DEVICE_LOGIN, EVENT_FAILURE, ip, userAgent,
                    Map.of("reason", "DEVICE_TOKEN_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "受信设备已失效，请重新输入密码");
        }

        ImUserEntity userEntity = requireUserById(userId);
        validateUserStatus(userEntity);
        device.setLastUsedAt(now);
        trustedDeviceMapper.updateById(device);
        recordSecurityEvent(userId, EVENT_TRUSTED_DEVICE_LOGIN, EVENT_SUCCESS, ip, userAgent,
                Map.of("deviceName", device.getDeviceName()));
        return buildAuthenticatedResponse(userEntity, grantToken, device.getExpireAt());
    }

    @Override
    public LoginResponseVo refreshSession(String refreshToken, String ip, String userAgent) {
        RefreshTokenState tokenState = requireRefreshTokenState(refreshToken);
        ImUserEntity userEntity = requireUserById(tokenState.getUserId());
        validateUserStatus(userEntity);

        revokeRefreshTokenInternal(refreshToken);
        recordSecurityEvent(userEntity.getId(), EVENT_TOKEN_REFRESH, EVENT_SUCCESS, ip, userAgent,
                Map.of("sessionRefresh", true));
        return buildAuthenticatedResponse(userEntity, null, null);
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        RefreshTokenState tokenState = loadRefreshTokenState(refreshToken);
        revokeRefreshTokenInternal(refreshToken);
        if (tokenState != null) {
            recordSecurityEvent(tokenState.getUserId(), EVENT_LOGOUT, EVENT_SUCCESS, null, null,
                    Map.of("sessionRefresh", false));
            imOnlineService.forceOffline(tokenState.getUserId(), ErrorCode.UNAUTHORIZED, "当前账号已退出登录");
        }
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        ImUserEntity userEntity = requireUserById(userId);
        if (!passwordEncoder.matches(oldPassword, userEntity.getPasswordHash())) {
            recordSecurityEvent(userId, EVENT_PASSWORD_CHANGE, EVENT_FAILURE, null, null, Map.of("reason", "OLD_PASSWORD_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "旧密码不正确");
        }

        String normalizedNewPassword = validateNewPassword(newPassword, userEntity.getPasswordHash());
        userEntity.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        imUserMapper.updateById(userEntity);
        revokeAllTrustedDevicesInternal(userId);
        revokeAllRefreshTokensInternal(userId);
        imOnlineService.forceOffline(userId, ErrorCode.UNAUTHORIZED, "密码已更新，请重新登录");
        recordSecurityEvent(userId, EVENT_PASSWORD_CHANGE, EVENT_SUCCESS, null, null, Map.of("revokedDevices", true));
    }

    @Override
    public CodeDispatchVo sendRecoveryCode(String email, String ip, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        ImUserEntity userEntity = imUserMapper.selectByEmail(normalizedEmail);
        if (userEntity == null || !StringUtils.hasText(userEntity.getEmail())) {
            recordSecurityEvent(null, EVENT_RECOVERY_CODE_SENT, EVENT_FAILURE, ip, userAgent,
                    Map.of("email", normalizedEmail, "reason", "EMAIL_NOT_FOUND"));
            throw new BizException(ErrorCode.USER_NOT_FOUND, "未找到绑定该邮箱的账号");
        }
        validateUserStatus(userEntity);

        String key = recoveryCodeKey(normalizedEmail);
        RecoveryCodeState existing = loadRedisJson(key, RecoveryCodeState.class);
        if (existing != null) {
            ensureResendAllowed(existing.getSentAt());
        }

        RecoveryCodeState state = new RecoveryCodeState();
        state.setUserId(userEntity.getId());
        state.setEmail(normalizedEmail);
        state.setCode(generateCode());
        state.setSentAt(System.currentTimeMillis());
        state.setAttempts(0);
        storeRedisJson(key, state, authProperties.getCodeExpireSeconds());
        authMailService.sendVerificationCode(normalizedEmail, "账号找回与密码重置", state.getCode());
        recordSecurityEvent(userEntity.getId(), EVENT_RECOVERY_CODE_SENT, EVENT_SUCCESS, ip, userAgent,
                Map.of("email", normalizedEmail));
        return buildCodeDispatch(maskEmail(normalizedEmail));
    }

    @Override
    public RecoveryVerifyVo verifyRecoveryCode(String email, String code, String ip, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        String key = recoveryCodeKey(normalizedEmail);
        RecoveryCodeState state = loadRedisJson(key, RecoveryCodeState.class);
        if (state == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码已失效，请重新获取");
        }
        if (state.getAttempts() >= authProperties.getMaxVerifyAttempts()) {
            stringRedisTemplate.delete(key);
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新获取");
        }
        String normalizedCode = normalizeCode(code);
        if (!state.getCode().equals(normalizedCode)) {
            state.setAttempts(state.getAttempts() + 1);
            long ttlSeconds = Math.max(1L, remainingSeconds(key));
            storeRedisJson(key, state, ttlSeconds);
            recordSecurityEvent(state.getUserId(), EVENT_RECOVERY_CODE_VERIFIED, EVENT_FAILURE, ip, userAgent,
                    Map.of("reason", "CODE_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "验证码错误");
        }

        stringRedisTemplate.delete(key);
        ImUserEntity userEntity = requireUserById(state.getUserId());
        validateUserStatus(userEntity);

        String recoveryToken = randomToken();
        RecoveryTokenState tokenState = new RecoveryTokenState();
        tokenState.setUserId(userEntity.getId());
        tokenState.setEmail(normalizedEmail);
        storeRedisJson(recoveryTokenKey(recoveryToken), tokenState, authProperties.getCodeExpireSeconds());

        RecoveryVerifyVo responseVo = new RecoveryVerifyVo();
        responseVo.setRecoveryToken(recoveryToken);
        responseVo.setAccounts(List.of(toLoginUserVo(userEntity)));
        recordSecurityEvent(userEntity.getId(), EVENT_RECOVERY_CODE_VERIFIED, EVENT_SUCCESS, ip, userAgent,
                Map.of("email", normalizedEmail));
        return responseVo;
    }

    @Override
    public void resetPasswordByRecovery(String recoveryToken, String newPassword, String ip, String userAgent) {
        RecoveryTokenState tokenState = loadRedisJson(recoveryTokenKey(recoveryToken), RecoveryTokenState.class);
        if (tokenState == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "恢复凭证已失效，请重新验证邮箱");
        }

        ImUserEntity userEntity = requireUserById(tokenState.getUserId());
        if (!tokenState.getEmail().equals(normalizeEmail(userEntity.getEmail()))) {
            stringRedisTemplate.delete(recoveryTokenKey(recoveryToken));
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱状态已变更，请重新验证");
        }

        String normalizedNewPassword = validateNewPassword(newPassword, userEntity.getPasswordHash());
        userEntity.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        imUserMapper.updateById(userEntity);
        stringRedisTemplate.delete(recoveryTokenKey(recoveryToken));
        revokeAllTrustedDevicesInternal(userEntity.getId());
        revokeAllRefreshTokensInternal(userEntity.getId());
        imOnlineService.forceOffline(userEntity.getId(), ErrorCode.UNAUTHORIZED, "密码已重置，请重新登录");
        recordSecurityEvent(userEntity.getId(), EVENT_PASSWORD_RESET, EVENT_SUCCESS, ip, userAgent,
                Map.of("email", tokenState.getEmail()));
    }

    @Override
    public CodeDispatchVo sendEmailBindCode(Long userId, String email, String currentPassword, String ip, String userAgent) {
        ImUserEntity userEntity = requireUserById(userId);
        validatePassword(currentPassword, userEntity.getPasswordHash(), "当前密码不正确");

        String normalizedEmail = normalizeEmail(email);
        ImUserEntity existing = imUserMapper.selectByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "该邮箱已被其他账号使用");
        }

        String key = emailBindKey(userId);
        EmailBindState previous = loadRedisJson(key, EmailBindState.class);
        if (previous != null) {
            ensureResendAllowed(previous.getSentAt());
        }

        EmailBindState state = new EmailBindState();
        state.setEmail(normalizedEmail);
        state.setCode(generateCode());
        state.setSentAt(System.currentTimeMillis());
        state.setAttempts(0);
        storeRedisJson(key, state, authProperties.getCodeExpireSeconds());
        authMailService.sendVerificationCode(normalizedEmail, StringUtils.hasText(userEntity.getEmail()) ? "更换邮箱" : "绑定邮箱", state.getCode());
        recordSecurityEvent(userId, EVENT_EMAIL_BIND_CODE_SENT, EVENT_SUCCESS, ip, userAgent,
                Map.of("email", normalizedEmail));
        return buildCodeDispatch(maskEmail(normalizedEmail));
    }

    @Override
    public UserProfileVo bindEmail(Long userId, String email, String code, String currentPassword, String ip, String userAgent) {
        ImUserEntity userEntity = requireUserById(userId);
        validatePassword(currentPassword, userEntity.getPasswordHash(), "当前密码不正确");
        String normalizedEmail = normalizeEmail(email);
        String key = emailBindKey(userId);
        EmailBindState state = loadRedisJson(key, EmailBindState.class);
        if (state == null || !normalizedEmail.equals(state.getEmail())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码已失效，请重新获取");
        }
        if (state.getAttempts() >= authProperties.getMaxVerifyAttempts()) {
            stringRedisTemplate.delete(key);
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新获取");
        }
        if (!state.getCode().equals(normalizeCode(code))) {
            state.setAttempts(state.getAttempts() + 1);
            long ttlSeconds = Math.max(1L, remainingSeconds(key));
            storeRedisJson(key, state, ttlSeconds);
            throw new BizException(ErrorCode.UNAUTHORIZED, "验证码错误");
        }

        ImUserEntity existing = imUserMapper.selectByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "该邮箱已被其他账号使用");
        }

        userEntity.setEmail(normalizedEmail);
        imUserMapper.updateById(userEntity);
        stringRedisTemplate.delete(key);
        recordSecurityEvent(userId, EVENT_EMAIL_BOUND, EVENT_SUCCESS, ip, userAgent, Map.of("email", normalizedEmail));
        return imUserMapper.selectProfileByUserId(userId);
    }

    @Override
    public List<TrustedDeviceItemVo> listTrustedDevices(Long userId) {
        List<ImAuthTrustedDeviceEntity> entities = trustedDeviceMapper.selectList(new LambdaQueryWrapper<ImAuthTrustedDeviceEntity>()
                .eq(ImAuthTrustedDeviceEntity::getUserId, userId)
                .isNull(ImAuthTrustedDeviceEntity::getRevokedAt)
                .orderByDesc(ImAuthTrustedDeviceEntity::getLastUsedAt)
                .orderByDesc(ImAuthTrustedDeviceEntity::getId));
        List<TrustedDeviceItemVo> result = new ArrayList<>();
        for (ImAuthTrustedDeviceEntity entity : entities) {
            TrustedDeviceItemVo itemVo = new TrustedDeviceItemVo();
            itemVo.setDeviceId(entity.getId());
            itemVo.setDeviceName(entity.getDeviceName());
            itemVo.setDeviceFingerprint(entity.getDeviceFingerprint());
            itemVo.setExpireAt(formatDateTime(entity.getExpireAt()));
            itemVo.setLastUsedAt(formatDateTime(entity.getLastUsedAt()));
            result.add(itemVo);
        }
        return result;
    }

    @Override
    public void revokeTrustedDevice(Long userId, Long deviceId) {
        ImAuthTrustedDeviceEntity entity = trustedDeviceMapper.selectOne(new LambdaQueryWrapper<ImAuthTrustedDeviceEntity>()
                .eq(ImAuthTrustedDeviceEntity::getId, deviceId)
                .eq(ImAuthTrustedDeviceEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "设备不存在");
        }
        entity.setRevokedAt(LocalDateTime.now());
        trustedDeviceMapper.updateById(entity);
        recordSecurityEvent(userId, EVENT_TRUSTED_DEVICE_REVOKE, EVENT_SUCCESS, null, null,
                Map.of("deviceName", entity.getDeviceName()));
    }

    @Override
    public void revokeAllTrustedDevices(Long userId) {
        revokeAllTrustedDevicesInternal(userId);
        recordSecurityEvent(userId, EVENT_TRUSTED_DEVICE_REVOKE_ALL, EVENT_SUCCESS, null, null, Map.of());
    }

    @Override
    public List<SecurityEventItemVo> listSecurityEvents(Long userId) {
        List<ImAuthSecurityEventEntity> entities = securityEventMapper.selectList(new LambdaQueryWrapper<ImAuthSecurityEventEntity>()
                .eq(ImAuthSecurityEventEntity::getUserId, userId)
                .orderByDesc(ImAuthSecurityEventEntity::getId)
                .last("LIMIT 30"));
        List<SecurityEventItemVo> items = new ArrayList<>();
        for (ImAuthSecurityEventEntity entity : entities) {
            SecurityEventItemVo itemVo = new SecurityEventItemVo();
            itemVo.setEventId(entity.getId());
            itemVo.setEventType(entity.getEventType());
            itemVo.setEventStatus(entity.getEventStatus());
            itemVo.setIp(entity.getIp());
            itemVo.setUserAgent(entity.getUserAgent());
            itemVo.setDetail(entity.getDetailJson());
            itemVo.setCreatedAt(formatDateTime(entity.getCreatedAt()));
            items.add(itemVo);
        }
        return items;
    }

    private ImUserEntity requireLoginUser(String username) {
        String normalizedUsername = UsernameRules.normalize(username);
        ImUserEntity userEntity = imUserMapper.selectByUsername(normalizedUsername);
        if (userEntity == null) {
            recordSecurityEvent(null, EVENT_LOGIN_FAILURE, EVENT_FAILURE, null, null,
                    Map.of("username", username, "reason", "USER_NOT_FOUND"));
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return userEntity;
    }

    private ImUserEntity requireUserById(Long userId) {
        ImUserEntity userEntity = imUserMapper.selectById(userId);
        if (userEntity == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return userEntity;
    }

    private void validateUserStatus(ImUserEntity userEntity) {
        if (!isUserStatusNormal(userEntity.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户状态不可用");
        }
    }

    private boolean isUserStatusNormal(Integer status) {
        return status != null && status == USER_STATUS_NORMAL;
    }

    private String normalizeAndValidateUsername(String username) {
        String normalizedUsername = UsernameRules.normalize(username);
        if (!UsernameRules.isValid(normalizedUsername)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名需为 3-24 位字母、数字或下划线，且不能以下划线开头或结尾");
        }
        return normalizedUsername;
    }

    private LoginResponseVo buildAuthenticatedResponse(ImUserEntity userEntity, String trustedDeviceGrantToken, LocalDateTime trustedExpireAt) {
        userEntity.setLastLoginAt(LocalDateTime.now());
        imUserMapper.updateById(userEntity);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userEntity.getId());
        loginUser.setUsername(userEntity.getUsername());
        loginUser.setTokenType("user");

        LoginResponseVo responseVo = new LoginResponseVo();
        responseVo.setStatus(LOGIN_STATUS_AUTHENTICATED);
        responseVo.setToken(tokenService.generateToken(loginUser));
        responseVo.setTokenType("Bearer");
        responseVo.setExpiresIn(tokenService.getExpireSeconds());
        responseVo.setUserInfo(toLoginUserVo(userEntity));
        responseVo.setTrustedDeviceGrantToken(trustedDeviceGrantToken);
        responseVo.setTrustedDeviceExpireAt(formatDateTime(trustedExpireAt));
        RefreshTokenGrant refreshTokenGrant = issueRefreshToken(userEntity.getId());
        responseVo.setRefreshToken(refreshTokenGrant.refreshToken());
        responseVo.setRefreshTokenExpireAt(formatDateTime(refreshTokenGrant.expireAt()));
        return responseVo;
    }

    private LoginUserVo toLoginUserVo(ImUserEntity userEntity) {
        LoginUserVo loginUserVo = new LoginUserVo();
        loginUserVo.setUserId(userEntity.getId());
        loginUserVo.setUsername(userEntity.getUsername());
        loginUserVo.setNickname(userEntity.getNickname());
        loginUserVo.setAvatarUrl(userEntity.getAvatarUrl());
        return loginUserVo;
    }

    private TrustedDeviceGrant issueTrustedDeviceGrant(Long userId, String deviceFingerprint, String deviceName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(authProperties.getTrustedDeviceExpireDays());
        String grantToken = randomToken();
        String grantTokenHash = sha256(grantToken);

        ImAuthTrustedDeviceEntity existing = trustedDeviceMapper.selectOne(new LambdaQueryWrapper<ImAuthTrustedDeviceEntity>()
                .eq(ImAuthTrustedDeviceEntity::getUserId, userId)
                .eq(ImAuthTrustedDeviceEntity::getDeviceFingerprint, deviceFingerprint)
                .last("LIMIT 1"));

        if (existing == null) {
            existing = new ImAuthTrustedDeviceEntity();
            existing.setUserId(userId);
            existing.setDeviceFingerprint(deviceFingerprint);
        }

        existing.setDeviceName(deviceName);
        existing.setGrantTokenHash(grantTokenHash);
        existing.setExpireAt(expireAt);
        existing.setLastUsedAt(now);
        existing.setRevokedAt(null);

        if (existing.getId() == null) {
            trustedDeviceMapper.insert(existing);
        } else {
            trustedDeviceMapper.updateById(existing);
        }

        return new TrustedDeviceGrant(grantToken, expireAt);
    }

    private RefreshTokenGrant issueRefreshToken(Long userId) {
        LocalDateTime expireAt = LocalDateTime.now().plusDays(authProperties.getRefreshTokenExpireDays());
        String refreshToken = randomToken();
        String tokenHash = sha256(refreshToken);

        RefreshTokenState tokenState = new RefreshTokenState();
        tokenState.setUserId(userId);
        tokenState.setIssuedAt(System.currentTimeMillis());

        long ttlSeconds = secondsUntil(expireAt);
        storeRedisJson(refreshTokenKey(tokenHash), tokenState, ttlSeconds);
        stringRedisTemplate.opsForSet().add(userRefreshIndexKey(userId), tokenHash);
        stringRedisTemplate.expire(userRefreshIndexKey(userId), Duration.ofSeconds(ttlSeconds));
        return new RefreshTokenGrant(refreshToken, expireAt);
    }

    private void revokeAllTrustedDevicesInternal(Long userId) {
        trustedDeviceMapper.update(null, new LambdaUpdateWrapper<ImAuthTrustedDeviceEntity>()
                .eq(ImAuthTrustedDeviceEntity::getUserId, userId)
                .isNull(ImAuthTrustedDeviceEntity::getRevokedAt)
                .set(ImAuthTrustedDeviceEntity::getRevokedAt, LocalDateTime.now()));
    }

    private void revokeAllRefreshTokensInternal(Long userId) {
        String indexKey = userRefreshIndexKey(userId);
        var tokenHashes = stringRedisTemplate.opsForSet().members(indexKey);
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            List<String> tokenKeys = new ArrayList<>(tokenHashes.size());
            for (String tokenHash : tokenHashes) {
                tokenKeys.add(refreshTokenKey(tokenHash));
            }
            stringRedisTemplate.delete(tokenKeys);
        }
        stringRedisTemplate.delete(indexKey);
    }

    private CodeDispatchVo buildCodeDispatch(String maskedEmail) {
        CodeDispatchVo responseVo = new CodeDispatchVo();
        responseVo.setMaskedEmail(maskedEmail);
        responseVo.setResendAfterSeconds(authProperties.getResendCooldownSeconds());
        return responseVo;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized) || !normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入有效邮箱");
        }
        return normalized;
    }

    private void validatePassword(String rawPassword, String passwordHash, String message) {
        if (!passwordEncoder.matches(rawPassword == null ? "" : rawPassword, passwordHash)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, message);
        }
    }

    private String validateNewPassword(String newPassword, String currentPasswordHash) {
        String normalizedNewPassword = newPassword == null ? "" : newPassword.trim();
        if (normalizedNewPassword.length() < 6) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码至少需要 6 位");
        }
        if (passwordEncoder.matches(normalizedNewPassword, currentPasswordHash)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }
        return normalizedNewPassword;
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.length() != 6) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入 6 位验证码");
        }
        return normalized;
    }

    private String normalizeDeviceFingerprint(String fingerprint) {
        return fingerprint == null ? "" : fingerprint.trim();
    }

    private String normalizeDeviceName(String deviceName) {
        String normalized = deviceName == null ? "" : deviceName.trim();
        if (!StringUtils.hasText(normalized)) {
            return "当前浏览器";
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private void ensureResendAllowed(long sentAtMillis) {
        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - sentAtMillis) / 1000L);
        int remaining = authProperties.getResendCooldownSeconds() - (int) elapsedSeconds;
        if (remaining > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码已发送，请 " + remaining + " 秒后再试");
        }
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "未绑定邮箱";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***@" + parts[1];
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String generateCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private <T> T loadRedisJson(String key, Class<T> clazz) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, clazz);
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private void storeRedisJson(String key, Object value, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to persist auth state", ex);
        }
    }

    private long remainingSeconds(String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return authProperties.getCodeExpireSeconds();
        }
        return ttl;
    }

    private long secondsUntil(LocalDateTime expireAt) {
        return Math.max(60L, Duration.between(LocalDateTime.now(), expireAt).getSeconds());
    }

    private void recordSecurityEvent(Long userId,
                                     String eventType,
                                     String eventStatus,
                                     String ip,
                                     String userAgent,
                                     Map<String, Object> detail) {
        ImAuthSecurityEventEntity entity = new ImAuthSecurityEventEntity();
        entity.setUserId(userId);
        entity.setEventType(eventType);
        entity.setEventStatus(eventStatus);
        entity.setIp(truncate(ip, 64));
        entity.setUserAgent(truncate(userAgent, 255));
        entity.setDetailJson(serializeDetail(detail));
        securityEventMapper.insert(entity);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        if (userId != null) {
            auditPayload.put("userId", userId);
        }
        if (StringUtils.hasText(ip)) {
            auditPayload.put("ip", ip);
        }
        if (StringUtils.hasText(userAgent)) {
            auditPayload.put("userAgent", truncate(userAgent, 255));
        }
        if (detail != null && !detail.isEmpty()) {
            auditPayload.putAll(detail);
        }
        auditLogService.log(eventType + "_" + eventStatus, auditPayload);
    }

    private String serializeDetail(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"serialize\":\"failed\"}";
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String loginChallengeKey(String ticket) {
        return LOGIN_CHALLENGE_KEY_PREFIX + ticket;
    }

    private String recoveryCodeKey(String email) {
        return RECOVERY_CODE_KEY_PREFIX + email;
    }

    private String recoveryTokenKey(String token) {
        return RECOVERY_TOKEN_KEY_PREFIX + token;
    }

    private String emailBindKey(Long userId) {
        return EMAIL_BIND_KEY_PREFIX + userId;
    }

    private String refreshTokenKey(String tokenHash) {
        return REFRESH_TOKEN_KEY_PREFIX + tokenHash;
    }

    private String userRefreshIndexKey(Long userId) {
        return USER_REFRESH_INDEX_KEY_PREFIX + userId;
    }

    private RefreshTokenState loadRefreshTokenState(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }
        return loadRedisJson(refreshTokenKey(sha256(refreshToken.trim())), RefreshTokenState.class);
    }

    private RefreshTokenState requireRefreshTokenState(String refreshToken) {
        RefreshTokenState tokenState = loadRefreshTokenState(refreshToken);
        if (tokenState == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return tokenState;
    }

    private void revokeRefreshTokenInternal(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        String normalizedToken = refreshToken.trim();
        String tokenHash = sha256(normalizedToken);
        RefreshTokenState tokenState = loadRedisJson(refreshTokenKey(tokenHash), RefreshTokenState.class);
        stringRedisTemplate.delete(refreshTokenKey(tokenHash));
        if (tokenState != null) {
            stringRedisTemplate.opsForSet().remove(userRefreshIndexKey(tokenState.getUserId()), tokenHash);
        }
    }

    private record TrustedDeviceGrant(String grantToken, LocalDateTime expireAt) {
    }

    private record RefreshTokenGrant(String refreshToken, LocalDateTime expireAt) {
    }

    public static class LoginChallengeState {
        private Long userId;
        private String email;
        private String deviceFingerprint;
        private String deviceName;
        private String code;
        private long sentAt;
        private int attempts;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDeviceFingerprint() {
            return deviceFingerprint;
        }

        public void setDeviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getSentAt() {
            return sentAt;
        }

        public void setSentAt(long sentAt) {
            this.sentAt = sentAt;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }

    public static class RecoveryCodeState {
        private Long userId;
        private String email;
        private String code;
        private long sentAt;
        private int attempts;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getSentAt() {
            return sentAt;
        }

        public void setSentAt(long sentAt) {
            this.sentAt = sentAt;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }

    public static class RecoveryTokenState {
        private Long userId;
        private String email;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class RefreshTokenState {
        private Long userId;
        private long issuedAt;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
        }
    }

    public static class EmailBindState {
        private String email;
        private String code;
        private long sentAt;
        private int attempts;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getSentAt() {
            return sentAt;
        }

        public void setSentAt(long sentAt) {
            this.sentAt = sentAt;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }
}
