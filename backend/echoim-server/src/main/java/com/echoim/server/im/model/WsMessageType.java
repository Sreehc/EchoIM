package com.echoim.server.im.model;

public enum WsMessageType {
    AUTH,
    PING,
    PONG,
    CHAT_SINGLE,
    CHAT_GROUP,
    ACK,
    READ,
    MESSAGE_RECALL,
    MESSAGE_EDIT,
    NOTICE,
    CONVERSATION_CHANGE,
    FORCE_OFFLINE,
    OFFLINE_SYNC
}
