package com.echoim.server.common.util;

import java.util.regex.Pattern;

public final class UsernameRules {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9_]{1,22}[A-Za-z0-9])?$");

    private UsernameRules() {
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim();
    }

    public static boolean isValid(String username) {
        return USERNAME_PATTERN.matcher(normalize(username)).matches();
    }
}
