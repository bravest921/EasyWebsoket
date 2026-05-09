package com.smsg.model;

import io.netty.channel.Channel;

/**
 * WebSocket会话基类
 * 外部系统可继承此类实现自定义会话
 *
 * @author Atral
 */
public abstract class WsSession implements MsgSession {

    protected final String sessionId;
    protected final transient Channel channel;

    protected WsSession(Channel channel) {
        this.sessionId = channel.id().asLongText();
        this.channel = channel;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void send(String message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }
}
