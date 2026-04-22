package com.echoim.server.im.netty;

import com.echoim.server.config.ImProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

@Component
public class ImServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ImProperties imProperties;
    private final ImTextFrameHandler imTextFrameHandler;

    public ImServerChannelInitializer(ImProperties imProperties, ImTextFrameHandler imTextFrameHandler) {
        this.imProperties = imProperties;
        this.imTextFrameHandler = imTextFrameHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536))
                .addLast(new WebSocketServerProtocolHandler(imProperties.getWsPath(), null, true))
                .addLast(new IdleStateHandler(imProperties.getHeartbeatTimeoutSeconds(), 0, 0))
                .addLast(imTextFrameHandler);
    }
}
