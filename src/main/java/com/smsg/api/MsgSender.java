package com.smsg.api;

import com.smsg.core.SessionManager;
import com.smsg.dto.SystemMessage;
import com.smsg.model.BaseMessage;
import com.smsg.util.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 消息发送器，对外提供的消息推送API
 */
public class MsgSender {

    private static final Logger log = LoggerFactory.getLogger(MsgSender.class);

    private static final MsgSender INSTANCE = new MsgSender();

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final MessageConverter converter = MessageConverter.getInstance();

    private MsgSender() {
    }

    public static MsgSender getInstance() {
        return INSTANCE;
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(String userId, String message) {
        sessionManager.sendToUser(userId, message);
    }

    /**
     * 发送任意业务消息给指定用户
     *
     * @param userId  用户ID
     * @param message 业务消息对象（继承 BaseMessage）
     */
    public <T extends BaseMessage> void sendToUser(String userId, T message) {
        sessionManager.sendToUser(userId, converter.toJson(message));
        log.debug("Sent message to user {}: {}", userId, message.getType());
    }

    /**
     * 向指定模块的指定用户发送消息
     */
    public void sendToModule(String module, String userId, String message) {
        sessionManager.sendToModule(module, userId, message);
    }

    /**
     * 向指定模块的指定用户发送业务消息
     */
    public <T extends BaseMessage> void sendToModule(String module, String userId, T message) {
        sessionManager.sendToModule(module, userId, converter.toJson(message));
    }

    /**
     * 广播消息给所有会话
     */
    public void broadcast(String message) {
        sessionManager.broadcast(message);
    }

    /**
     * 广播任意业务消息
     */
    public <T extends BaseMessage> void broadcast(T message) {
        sessionManager.broadcast(converter.toJson(message));
        log.debug("Broadcast message: {}", message.getType());
    }

    /**
     * 向指定模块的所有用户广播消息
     */
    public void broadcastToModule(String module, String message) {
        sessionManager.broadcastToModule(module, message);
    }

    /**
     * 向指定模块的所有用户广播业务消息
     */
    public <T extends BaseMessage> void broadcastToModule(String module, T message) {
        sessionManager.broadcastToModule(module, converter.toJson(message));
        log.debug("Broadcast to module {}: {}", module, message.getType());
    }

    /**
     * 向多个模块广播消息
     */
    public <T extends BaseMessage> void broadcastToModules(Set<String> modules, T message) {
        modules.forEach(module -> broadcastToModule(module, message));
    }

    /**
     * 发送日志消息给指定用户
     */
    public void sendLogToUser(String userId, String level, String content) {
        SystemMessage msg = new SystemMessage();
        msg.setType("LOG_" + level.toUpperCase());
        msg.setContent(content);
        sendToUser(userId, msg);
    }

    /**
     * 广播日志消息
     */
    public void broadcastLog(String level, String content) {
        SystemMessage msg = new SystemMessage();
        msg.setType("LOG_" + level.toUpperCase());
        msg.setContent(content);
        broadcast(msg);
    }

    /**
     * 发送系统消息给指定用户
     */
    public void sendSystemToUser(String userId, String content) {
        SystemMessage msg = new SystemMessage();
        msg.setContent(content);
        sendToUser(userId, msg);
    }
}
