package com.smsg.model;

/**
 * 消息会话接口
 * 抽象 WebSocket 会话操作，兼容不同实现（Netty、Spring WebSocket 等）
 *
 * @author Atral
 */
public interface MsgSession {

    /**
     * 获取会话ID
     */
    String getSessionId();

    /**
     * 发送消息
     */
    void send(String message);

    /**
     * 是否活跃
     */
    boolean isActive();

    /**
     * 关闭会话
     */
    void close();
}
