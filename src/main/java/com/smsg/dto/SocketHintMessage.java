package com.smsg.dto;

import com.smsg.model.BaseMessage;

/**
 * @description:
 * @author: Atral
 * @time: 2026/5/9 9:27
 */
public class SocketHintMessage extends BaseMessage {
    private String content;

    public SocketHintMessage() {
        super("SOCKET_HINT_MESSAGE");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
