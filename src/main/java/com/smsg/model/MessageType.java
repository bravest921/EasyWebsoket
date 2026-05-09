package com.smsg.model;

/**
 * 消息类型接口
 * 外部系统可实现此接口定义自己的消息类型
 * @author Atral
 */
public interface MessageType {

    /**
     * 获取消息类型字符串
     */
    String getType();

    /**
     * 内置消息类型枚举
     */
    enum BuiltIn implements MessageType {
        LOGIN,
        LOGIN_RESPONSE,
        HEARTBEAT,
        SYSTEM_MESSAGE,
        UNKNOWN;

        @Override
        public String getType() {
            return this.name();
        }
    }
}
