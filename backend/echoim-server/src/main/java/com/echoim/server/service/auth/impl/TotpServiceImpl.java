package com.echoim.server.service.auth.impl;

import com.echoim.server.service.auth.TotpService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TotpServiceImpl implements TotpService {

    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final String RECOVERY_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluding confusing chars: 0,O,1,I

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String generateUri(String secret, String accountName, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, accountName, secret, issuer);
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) {
            return false;
        }
        try {
            return codeVerifier.isValidCode(secret, code.trim());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> generateRecoveryCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateSingleRecoveryCode());
        }
        return codes;
    }

    @Override
    public boolean verifyRecoveryCode(String storedCodesJson, String code) {
        if (storedCodesJson == null || code == null) {
            return false;
        }
        List<String> storedCodes = parseRecoveryCodes(storedCodesJson);
        return storedCodes.contains(code.trim().toUpperCase());
    }

    @Override
    public String consumeRecoveryCode(String storedCodesJson, String code) {
        if (storedCodesJson == null || code == null) {
            return null;
        }
        List<String> storedCodes = parseRecoveryCodes(storedCodesJson);
        String normalizedCode = code.trim().toUpperCase();
        if (!storedCodes.remove(normalizedCode)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(storedCodes);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String generateSingleRecoveryCode() {
        StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            sb.append(RECOVERY_CODE_CHARS.charAt(secureRandom.nextInt(RECOVERY_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private List<String> parseRecoveryCodes(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
