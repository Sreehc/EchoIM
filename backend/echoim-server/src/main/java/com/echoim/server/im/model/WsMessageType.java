package com.echoim.server.im.model;

public enum WsMessageType {
    AUTH,
    PING,
    PONG,
    CHAT_SINGLE,
    CHAT_GROUP,
    ACK,
    READ,
    NOTICE,
    FORCE_OFFLINE,
    OFFLINE_SYNC
}
