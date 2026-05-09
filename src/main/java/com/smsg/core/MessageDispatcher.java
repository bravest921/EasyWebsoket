package com.smsg.core;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smsg.dto.LoginResponse;
import com.smsg.dto.SocketHintMessage;
import com.smsg.dto.SystemMessage;
import com.smsg.model.BusinessMessage;
import com.smsg.model.MsgSession;
import com.smsg.model.UserSession;
import com.smsg.model.WsSession;
import com.smsg.util.MessageConverter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 消息分发器
 */
public class MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);
    private static final MessageDispatcher INSTANCE = new MessageDispatcher();

    private final MessageConverter converter = MessageConverter.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * 自定义处理器映射表
     */
    private final Map<String, BiConsumer<MsgSession, JSONObject>> customHandlers = new ConcurrentHashMap<>();

    private MessageDispatcher() {
    }

    public static MessageDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * 注册自定义消息处理器
     *
     * @param type    消息类型
     * @param handler 处理器 (session, jsonData) -> {}
     */
    public void registerHandler(String type, BiConsumer<MsgSession, JSONObject> handler) {
        customHandlers.put(type, handler);
        log.info("Registered custom handler for message type: {}", type);
    }

    /**
     * 注册业务消息处理器
     *
     * @param message 业务消息实例
     */
    public void registerMessage(BusinessMessage message) {
        registerHandler(message.getType(), message::handle);
    }

    /**
     * 取消注册
     *
     * @param type 消息类型
     */
    public void unregisterHandler(String type) {
        customHandlers.remove(type);
        log.info("Unregistered handler for message type: {}", type);
    }

    // ==================== Netty 兼容方法 ====================

    /**
     * Netty 版本分发消息（保持兼容）
     */
    public void dispatch(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        dispatch(ctx.channel().id().asLongText(), frame.text());
    }

    // ==================== 核心分发方法 ====================

    /**
     * 分发消息
     *
     * @param session 会话
     * @param text    消息文本
     */
    public void dispatch(MsgSession session, String text) {
        dispatch(session.getSessionId(), text);
    }

    /**
     * 内部分发
     */
    private void dispatch(String sessionId, String text) {
        log.info("Received message: {}", text);

        try {
            JSONObject jsonObject = JSONUtil.parseObj(text);
            String type = jsonObject.getStr("type");

            if (type == null) {
                sendHintError(sessionId, "请传入消息类型type Missing message type");
                return;
            }

            // 优先查找自定义处理器
            BiConsumer<MsgSession, JSONObject> customHandler = customHandlers.get(type);
            if (customHandler != null) {
                MsgSession session = sessionManager.getSession(sessionId);
                customHandler.accept(session, jsonObject);
                return;
            }

            // 内置消息处理
            switch (type) {
                case "LOGIN":
                    handleLogin(sessionId, jsonObject);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(sessionId);
                    break;
                case "SUBSCRIBE":
                    handleSubscribe(sessionId, jsonObject);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(sessionId, jsonObject);
                    break;
                default:
                    sendHintError(sessionId, "未找到此消息类型type Unknown message type: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch message", e);
            sendHintError(sessionId, "无效的请求格式 Invalid message format ");
        }
    }

    private void handleLogin(String sessionId, JSONObject jsonObject) {
        String userId = jsonObject.getStr("userId");
        String username = jsonObject.getStr("username");
        String role = jsonObject.getStr("role");
        String module = jsonObject.getStr("module");

        if (userId == null || username == null) {
            sendError(sessionId, "登录用户id或用户名为空 Missing userId or username");
            return;
        }

        sessionManager.bindUser(sessionId, userId, username, role, module);

        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setMessage("Login successful");
        response.setUserId(userId);
        response.setSessionId(sessionId);

        sendToSession(sessionId, converter.toJson(response));
        log.info("User login: userId={}, sessionId={}", userId, sessionId);
    }

    private void handleHeartbeat(String sessionId) {
        MsgSession session = sessionManager.getSession(sessionId);
        if (session instanceof UserSession) {
            ((UserSession) session).updateHeartbeat();
        }

        SystemMessage heartbeat = new SystemMessage();
        heartbeat.setType("HEARTBEAT");
        heartbeat.setContent("pong");
        sendToSession(sessionId, converter.toJson(heartbeat));
    }

    private void handleSubscribe(String sessionId, JSONObject jsonObject) {
        String module = jsonObject.getStr("module");
        String userId = getLoggedInUserId(sessionId, module);
        if (userId == null) {
            return;
        }

        sessionManager.subscribeModule(userId, module);
        sendModuleHint(sessionId, module, "Subscribed to module: " + module);
        log.info("User {} subscribed to module {}", userId, module);
    }

    private void handleUnsubscribe(String sessionId, JSONObject jsonObject) {
        String module = jsonObject.getStr("module");
        String userId = getLoggedInUserId(sessionId, module);
        if (userId == null) {
            return;
        }

        sessionManager.unsubscribeModule(userId, module);
        sendModuleHint(sessionId, module, "Unsubscribed from module: " + module);
        log.info("User {} unsubscribed from module {}", userId, module);
    }

    private String getLoggedInUserId(String sessionId, String module) {
        if (module == null || module.trim().isEmpty()) {
            sendHintError(sessionId, "模块名不能为空 module is required");
            return null;
        }

        MsgSession session = sessionManager.getSession(sessionId);
        if (!(session instanceof UserSession)) {
            sendHintError(sessionId, "用户未登录请先登录 Not logged in");
            return null;
        }

        String userId = ((UserSession) session).getUserId();
        if (userId == null) {
            sendHintError(sessionId, "用户未登录请先登录 Not logged in");
            return null;
        }

        return userId;
    }

    // ==================== 发送方法 ====================

    private void sendToSession(String sessionId, String message) {
        MsgSession session = sessionManager.getSession(sessionId);
        if (session != null) {
            session.send(message);
        }
    }

    private void sendModuleHint(String sessionId, String module, String content) {
        SocketHintMessage hint = new SocketHintMessage();
        hint.setContent(content);
        sendToSession(sessionId, converter.toJson(hint));
    }

    private void sendError(String sessionId, String message) {
        SystemMessage error = new SystemMessage();
        error.setType("SYSTEM_MESSAGE");
        error.setContent("ERROR: " + message);
        sendToSession(sessionId, converter.toJson(error));
    }

    private void sendHintError(String sessionId, String message) {
        SocketHintMessage hint = new SocketHintMessage();
        hint.setContent("Hint: " + message);
        sendToSession(sessionId, converter.toJson(hint));
    }
}
