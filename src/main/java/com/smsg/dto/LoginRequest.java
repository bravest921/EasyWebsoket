package com.smsg.dto;

import com.smsg.model.BaseMessage;

/**
 * 登录请求
 * @author Atral
 */
public class LoginRequest extends BaseMessage {

    private String userId;
    private String username;
    private String password;
    private String role;
    private String module;

    public LoginRequest() {
        super("LOGIN");
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}