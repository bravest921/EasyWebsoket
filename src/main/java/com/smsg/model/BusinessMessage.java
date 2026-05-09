package com.smsg.model;

import cn.hutool.json.JSONObject;

/**
 * 业务消息抽象基类
 * 外部系统可继承此类实现自定义消息
 *
 * @author Atral
 */
public abstract class BusinessMessage extends BaseMessage {

    protected BusinessMessage(String type) {
        super(type);
    }

    /**
     * 处理接收到的消息
     *
     * @param session 会话
     * @param rawData 原始 JSON 数据
     */
    public abstract void handle(MsgSession session, JSONObject rawData);

    /**
     * 验证消息是否合法，可重写
     */
    public boolean validate(JSONObject data) {
        return true;
    }
}
