package com.echoim.server.im.handler;

import com.echoim.server.im.model.WsMessage;
import org.springframework.stereotype.Component;

@Component
public class ImMessageHandler {

    public void handle(WsMessage message) {
        if (message == null || message.getType() == null) {
            return;
        }
        // WebSocket/Netty 接入后，这里负责按消息类型分发到具体处理逻辑。
    }
}
