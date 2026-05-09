package com.smsg.model;

import io.netty.channel.Channel;

/**
 * 用户会话
 * 继承 WsSession，添加用户相关字段
 *
 * @author Atral
 */
public class UserSession extends WsSession {

    private String userId;
    private String username;
    private String role;
    private String module;
    private long lastHeartbeat;
    private boolean authenticated;

    public UserSession(Channel channel) {
        super(channel);
        this.lastHeartbeat = System.currentTimeMillis();
        this.authenticated = false;
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

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * 绑定用户信息
     */
    public void bindUser(String userId, String username, String role, String module) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.module = module;
        this.authenticated = true;
    }
}
