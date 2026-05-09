package com.smsg.dto;

import com.smsg.model.BaseMessage;

/**
 * 系统消息
 * @author Atral
 */
public class SystemMessage extends BaseMessage {

    private String content;

    public SystemMessage() {
        super("SYSTEM_MESSAGE");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}