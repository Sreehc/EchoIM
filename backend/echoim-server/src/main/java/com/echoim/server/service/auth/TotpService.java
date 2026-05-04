package com.echoim.server.service.auth;

import java.util.List;

public interface TotpService {

    /**
     * Generate a new TOTP secret for a user
     * @return Base32-encoded secret
     */
    String generateSecret();

    /**
     * Generate a TOTP URI for QR code generation
     * @param secret the Base32-encoded secret
     * @param accountName the user's account name (email or username)
     * @param issuer the application name
     * @return the otpauth:// URI
     */
    String generateUri(String secret, String accountName, String issuer);

    /**
     * Verify a TOTP code against a secret
     * @param secret the Base32-encoded secret
     * @param code the 6-digit code to verify
     * @return true if the code is valid
     */
    boolean verifyCode(String secret, String code);

    /**
     * Generate recovery codes
     * @param count number of codes to generate
     * @return list of recovery codes
     */
    List<String> generateRecoveryCodes(int count);

    /**
     * Verify a recovery code against stored codes
     * @param storedCodesJson JSON array of stored recovery codes
     * @param code the code to verify
     * @return true if the code is valid, and the code should be consumed
     */
    boolean verifyRecoveryCode(String storedCodesJson, String code);

    /**
     * Consume a recovery code (remove it from stored codes)
     * @param storedCodesJson JSON array of stored recovery codes
     * @param code the code to consume
     * @return updated JSON array, or null if code was not found
     */
    String consumeRecoveryCode(String storedCodesJson, String code);
}
