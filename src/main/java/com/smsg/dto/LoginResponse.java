package com.smsg.dto;

import com.smsg.model.BaseMessage;

/**
 * 登录响应
 * @author Atral
 */
public class LoginResponse extends BaseMessage {

    private boolean success;
    private String message;
    private String userId;
    private String sessionId;

    public LoginResponse() {
        super("LOGIN_RESPONSE");
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}