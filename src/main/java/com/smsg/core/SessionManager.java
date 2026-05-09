package com.smsg.core;

import com.smsg.model.MsgSession;
import com.smsg.model.UserSession;
import com.smsg.model.WsSession;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 *
 * @author Atral
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    /**
     * 所有会话
     */
    private final Map<String, MsgSession> sessions = new ConcurrentHashMap<>();

    /**
     * 用户ID到会话的映射（仅限已认证用户）
     */
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 模块 -> 用户ID集合
     */
    private final Map<String, Set<String>> moduleUsers = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 添加会话
     */
    public void addSession(MsgSession session) {
        sessions.put(session.getSessionId(), session);
    }

    public void addSession(WsSession session) {
        sessions.put(session.getSessionId(), session);
    }

    /**
     * 添加 Netty Channel 会话（自动创建 UserSession）
     */
    public void addSession(io.netty.channel.Channel channel) {
        UserSession session = new UserSession(channel);
        sessions.put(session.getSessionId(), session);
    }

    public MsgSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public MsgSession getSessionByChannel(Object channel) {
        // 兼容 Netty Channel
        if (channel instanceof io.netty.channel.Channel) {
            return sessions.get(((io.netty.channel.Channel) channel).id().asLongText());
        }
        return null;
    }

    /**
     * 获取用户会话
     */
    public UserSession getUserSession(String userId) {
        return userSessions.get(userId);
    }

    public void removeSession(String sessionId) {
        MsgSession session = sessions.remove(sessionId);
        if (session instanceof UserSession) {
            UserSession userSession = (UserSession) session;
            if (userSession.getUserId() != null) {
                userSessions.remove(userSession.getUserId());
                // 从所有模块中移除
                moduleUsers.values().forEach(users -> users.remove(userSession.getUserId()));
            }
        }
    }

    public void removeSessionByChannel(Object channel) {
        if (channel instanceof io.netty.channel.Channel) {
            removeSession(((io.netty.channel.Channel) channel).id().asLongText());
        }
    }

    /**
     * 绑定用户
     */
    public void bindUser(String sessionId, String userId, String username, String role, String module) {
        MsgSession session = sessions.get(sessionId);
        if (session instanceof UserSession) {
            UserSession userSession = (UserSession) session;
            userSession.bindUser(userId, username, role, module);
            userSessions.put(userId, userSession);
            // 注册到模块
            if (module != null) {
                moduleUsers.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet()).add(userId);
            }
        }
    }

    /**
     * 用户订阅模块
     */
    public void subscribeModule(String userId, String module) {
        if (module != null && userSessions.containsKey(userId)) {
            moduleUsers.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }
    }

    /**
     * 用户取消订阅模块
     */
    public void unsubscribeModule(String userId, String module) {
        if (module != null) {
            Set<String> users = moduleUsers.get(module);
            if (users != null) {
                users.remove(userId);
            }
        }
    }

    /**
     * 获取用户订阅的所有模块
     */
    public Set<String> getUserModules(String userId) {
        Set<String> result = new HashSet<>();
        moduleUsers.forEach((module, users) -> {
            if (users.contains(userId)) {
                result.add(module);
            }
        });
        return result;
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(String userId, String message) {
        UserSession session = userSessions.get(userId);
        if (session != null) {
            session.send(message);
        }
    }

    /**
     * 向指定模块的指定用户发送消息
     */
    public void sendToModule(String module, String userId, String message) {
        Set<String> users = moduleUsers.get(module);
        if (users != null && users.contains(userId)) {
            sendToUser(userId, message);
        }
    }

    /**
     * 向指定模块的所有用户广播消息
     */
    public void broadcastToModule(String module, String message) {
        Set<String> users = moduleUsers.get(module);
        if (users != null) {
            users.forEach(uid -> sendToUser(uid, message));
        }
    }

    /**
     * 向多个模块广播消息
     */
    public void broadcastToModules(Set<String> modules, String message) {
        modules.forEach(module -> broadcastToModule(module, message));
    }

    /**
     * 广播消息
     */
    public void broadcast(String message) {
        sessions.values().forEach(session -> session.send(message));
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public void clear() {
        sessions.clear();
        userSessions.clear();
        moduleUsers.clear();
    }
}
