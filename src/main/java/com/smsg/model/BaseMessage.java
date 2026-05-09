package com.smsg.model;

import java.io.Serializable;

/**
 * 消息基类
 * @author Atral
 */
public abstract class BaseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;

    private String type;

    protected BaseMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    protected BaseMessage(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}