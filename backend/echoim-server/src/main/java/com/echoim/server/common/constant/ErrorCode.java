package com.echoim.server.common.constant;

public final class ErrorCode {

    public static final int SUCCESS = 0;
    public static final int PARAM_ERROR = 40000;
    public static final int BUSINESS_CONFLICT = 40900;
    public static final int UNAUTHORIZED = 40100;
    public static final int TOKEN_INVALID = 40101;
    public static final int TOKEN_EXPIRED = 40102;
    public static final int USER_NOT_FOUND = 40401;
    public static final int CONVERSATION_NOT_FOUND = 40402;
    public static final int FRIEND_REQUEST_NOT_FOUND = 40403;
    public static final int USERNAME_EXISTS = 40901;
    public static final int FRIEND_REQUEST_DUPLICATE = 40902;
    public static final int ALREADY_FRIEND = 40903;
    public static final int SYSTEM_ERROR = 50000;

    private ErrorCode() {
    }
}
