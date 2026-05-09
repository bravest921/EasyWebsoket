package com.smsg.util;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

/**
 * 消息转换器
 * @author Atral
 */
public class MessageConverter {

    private static final MessageConverter INSTANCE = new MessageConverter();

    private MessageConverter() {
    }

    public static MessageConverter getInstance() {
        return INSTANCE;
    }

    public String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    public JSONObject parseObject(String json) {
        return JSONUtil.parseObj(json);
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }
}