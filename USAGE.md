# WebSocket 消息管理框架使用文档

## 1. 项目概述

基于 Netty 的 WebSocket 消息推送框架，支持会话管理、模块化消息路由，适用于股票交易实时行情等场景。

---

## 2. 快速开始

### 2.1 引入依赖

```xml

<dependency>
    <groupId>com.smsg</groupId>
    <artifactId>socketMsg</artifactId>
    <version>1.0-0</version>
</dependency>
```

### 2.2 打包安装

```bash
mvn clean install
```

---

## 3. 独立运行

```java
import com.smsg.core.WsServer;

public class Main {
    public static void main(String[] args) {
        WsServer server = new WsServer(8080);
        server.start();
    }
}
```

---

## 4. 消息发送 API

```java
MsgSender sender = MsgSender.getInstance();

// 发送给指定用户
sender.

sendToUser(userId, message);

// 广播给所有用户
sender.

broadcast(message);

// 按模块单发（仅发送给指定模块的指定用户）
sender.

sendToModule("stock",userId, priceMessage);

// 按模块广播（仅发送给订阅了该模块的所有用户）
sender.

broadcastToModule("stock",priceMessage);

// 向多个模块广播
Set<String> modules = new HashSet<>(Arrays.asList("stock", "futures"));
sender.

broadcastToModules(modules, alertMessage);

// 发送日志
sender.

sendLogToUser(userId, "INFO","Market update");
sender.

broadcastLog("WARN","High volatility detected");

// 发送系统消息
sender.

sendSystemToUser(userId, "Order filled");
```

---

## 5. 前端连接

```javascript
const ws = new WebSocket("ws://localhost:8080/ws");

ws.onopen = function () {
    console.log("Connected to server");
    ws.send(JSON.stringify({
        type: "LOGIN",
        userId: "user001",
        username: "张三",
        password: "123456",
        role: "trader",
        module: "stock"
    }));
};

ws.onmessage = function (event) {
    const data = JSON.parse(event.data);
    switch (data.type) {
        case "LOGIN_RESPONSE":
            console.log("Login result:", data.success);
            break;
        case "SYSTEM_MESSAGE":
            console.log("System:", data.content);
            break;
        case "HEARTBEAT":
            console.log("Heartbeat:", data.content);
            break;
        default:
            console.log("Business message:", data);
    }
};

ws.onclose = function () {
    console.log("Disconnected");
};
```

---

## 6. 消息格式

**登录请求：**

```json
{
  "type": "LOGIN",
  "userId": "user001",
  "username": "张三",
  "password": "123456",
  "role": "trader",
  "module": "stock"
}
```

**登录响应：**

```json
{
  "type": "LOGIN_RESPONSE",
  "success": true,
  "message": "Login successful",
  "userId": "user001",
  "sessionId": "abc123...",
  "timestamp": 1715174400000
}
```

**心跳：**

```json
{
  "type": "HEARTBEAT",
  "content": "pong",
  "timestamp": 1715174400000
}
```

**系统消息：**

```json
{
  "type": "SYSTEM_MESSAGE",
  "content": "Order filled",
  "timestamp": 1715174400000
}
```

**订阅/取消订阅：**

```json
{
  "type": "SUBSCRIBE",
  "module": "stock"
}
{
  "type": "UNSUBSCRIBE",
  "module": "stock"
}
```

> 其他业务消息格式由外部系统自定义实现

---

## 7. 技术栈

- Netty 4.1.x（网络框架）
- Hutool JSON（JSON序列化）
- SLF4J + Logback（日志）

---

## 8. Spring Boot 集成（RuoYi）

### 8.1 安装到本地 Maven

```bash
cd D:\Atral\ideaProject\SocketMsgManagerFrame\socketMsg
mvn clean install
```

### 8.2 RuoYi pom.xml 添加依赖

```xml

<dependency>
    <groupId>com.smsg</groupId>
    <artifactId>socketMsg</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 8.3 创建 SpringSession 适配器

`ruoyi-admin/src/main/java/com/ruoyi/web/websocket/SpringSession.java`

```java
package com.ruoyi.web.websocket;

import com.smsg.model.MsgSession;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

public class SpringSession implements MsgSession {

    private final WebSocketSession session;

    public SpringSession(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    @Override
    public void send(String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public boolean isActive() {
        return session.isOpen();
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
```

### 8.4 创建 SpringWebSocketHandler

`ruoyi-admin/src/main/java/com/ruoyi/web/websocket/SpringWebSocketHandler.java`

```java
package com.ruoyi.web.websocket;

import com.smsg.core.MessageDispatcher;
import com.smsg.core.SessionManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SpringWebSocketHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final MessageDispatcher dispatcher = MessageDispatcher.getInstance();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SpringSession msgSession = new SpringSession(session);
        sessionManager.addSession(msgSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SpringSession msgSession = new SpringSession(session);
        dispatcher.dispatch(msgSession, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionManager.removeSession(session.getId());
    }
}
```

### 8.5 创建 WebSocket 配置类

`ruoyi-admin/src/main/java/com/ruoyi/web/config/WebSocketConfig.java`

```java
package com.ruoyi.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public SpringWebSocketHandler webSocketHandler() {
        return new SpringWebSocketHandler();
    }
}
```

### 8.6 业务中使用 MsgSender

```java

@Service
public class StockServiceImpl {

    public void pushToUser(String userId, Object message) {
        MsgSender.getInstance().sendToUser(userId, message);
    }

    public void broadcastToModule(String module, Object message) {
        MsgSender.getInstance().broadcastToModule(module, message);
    }

    public void broadcast(Object message) {
        MsgSender.getInstance().broadcast(message);
    }
}
```

### 8.7 前端连接

```javascript
const ws = new WebSocket("ws://localhost:8080/ws");

ws.onopen = function () {
    ws.send(JSON.stringify({
        type: "LOGIN",
        userId: "user001",
        username: "张三",
        role: "trader",
        module: "stock"
    }));
};

ws.onmessage = function (event) {
    console.log("Received:", JSON.parse(event.data));
};

// 订阅模块
ws.send(JSON.stringify({type: "SUBSCRIBE", module: "stock"}));

// 定时心跳
setInterval(() => {
    ws.send(JSON.stringify({type: "HEARTBEAT"}));
}, 25000);
```

---

## 9. 前端 Vue 集成

### 9.1 获取 SDK

WebSocketSDK.js 位于 `src/main/resources/static/WebSocketSDK.js`。

**方式一：通过 HTTP 引入**

```html

<script src="http://localhost:8080/WebSocketSDK.js"></script>
```

**方式二：复制到项目中**
将 `WebSocketSDK.js` 复制到 Vue 项目的 `public/` 或 `src/assets/` 目录下。

### 9.2 创建 WebSocket 管理模块

`src/utils/websocket.js`

```javascript
import WebSocketSDK from '@/assets/WebSocketSDK.js';

const wsUrl = process.env.VUE_APP_WS_URL || 'ws://localhost:8080/ws';
const sdk = new WebSocketSDK();

export function connect(options = {}) {
    return sdk.connect(wsUrl, {
        heartbeatInterval: 25000,
        heartbeatTimeout: 5000,
        reconnectInterval: 3000,
        reconnectMaxAttempts: 10,
        debug: process.env.NODE_ENV === 'development',
        ...options
    });
}

export const login = (info) => sdk.login(info);
export const subscribe = (module) => sdk.subscribe(module);
export const unsubscribe = (module) => sdk.unsubscribe(module);
export const sendMessage = (msg) => sdk.sendMessage(msg);
export const logout = () => sdk.logout();
export const close = () => sdk.close();
export const getSdk = () => sdk;
```

### 9.3 Vue 组件中使用

```javascript
import {connect, login, subscribe, getSdk} from '@/utils/websocket';

export default {
    data() {
        return {wsConnected: false};
    },
    mounted() {
        this.initWebSocket();
    },
    beforeDestroy() {
        getSdk().close();
    },
    methods: {
        initWebSocket() {
            connect({
                onOpen: () => {
                    this.wsConnected = true;
                    console.log('WebSocket connected');
                },
                onClose: () => {
                    this.wsConnected = false;
                },
                onLoginSuccess: (data) => {
                    console.log('Login success:', data);
                    subscribe('stock');
                },
                onLoginError: (data) => {
                    console.error('Login failed:', data.message);
                },
                onMessage: (data) => {
                    console.log('Received:', data);
                    this.handleMessage(data);
                }
            });
        },
        handleMessage(data) {
            // 业务消息处理
        },
        doLogin() {
            login({
                userId: 'user001',
                username: '张三',
                role: 'trader',
                module: 'stock'
            });
        },
        switchModule(newModule, oldModule) {
            getSdk().switchModule(newModule, oldModule);
        }
    }
};
```

### 9.4 Vue3 Composition API

```javascript
import {onMounted, onBeforeUnmount, ref} from 'vue';
import {connect, login, getSdk} from '@/utils/websocket';

export default {
    setup() {
        const wsConnected = ref(false);

        onMounted(() => {
            connect({
                onOpen: () => {
                    wsConnected.value = true;
                },
                onClose: () => {
                    wsConnected.value = false;
                }
            });
        });

        onBeforeUnmount(() => {
            getSdk().close();
        });

        const doLogin = () => {
            login({
                userId: 'user001',
                username: '张三',
                role: 'trader',
                module: 'stock'
            });
        };

        return {wsConnected, doLogin};
    }
};
```

组件中使用：

```javascript
export default {
    created() {
        this.$store.dispatch('subscribe', {
            module: 'stockAlert',
            callback: this.handleStockAlert
        })
    },

    beforeDestroy() {
        this.$store.dispatch('unsubscribe', {
            module: 'stockAlert',
            callback: this.handleStockAlert
        })
    },

    methods: {
        handleStockAlert(data) {
            console.log('收到消息:', data)
        }
    }
}

```

### 9.5 SDK 配置参数

| 参数                     | 默认值   | 说明         |
|------------------------|-------|------------|
| `reconnectInterval`    | 3000  | 重连间隔(ms)   |
| `reconnectMaxAttempts` | 10    | 最大重连次数     |
| `heartbeatInterval`    | 25000 | 心跳间隔(ms)   |
| `heartbeatTimeout`     | 5000  | 心跳超时时间(ms) |
| `debug`                | false | 是否输出调试日志   |

### 9.6 回调函数

| 回调               | 说明    |
|------------------|-------|
| `onOpen`         | 连接打开时 |
| `onClose`        | 连接关闭时 |
| `onError`        | 连接错误时 |
| `onMessage`      | 收到消息时 |
| `onReconnect`    | 触发重连时 |
| `onLoginSuccess` | 登录成功时 |
| `onLoginError`   | 登录失败时 |

### 9.7 注意事项

1. **跨域问题**：确保后端配置了 CORS 或同域访问
2. **重连机制**：浏览器端会自动重连，无需手动处理
3. **心跳检测**：服务器在 `heartbeatTimeout` 时间内未响应会自动断开重连
4. **会话恢复**：断线重连后会自动恢复登录状态和模块订阅
5. **组件销毁**：务必在 `beforeDestroy` 或 `onBeforeUnmount` 中调用 `close()`

---

## 10. 外部扩展

### 10.1 定义自定义消息

继承 `BusinessMessage`：

```java
public class CustomAlertMessage extends BusinessMessage {

    public CustomAlertMessage() {
        super("CUSTOM_ALERT");
    }

    @Override
    public void handle(MsgSession session, JSONObject data) {
        // 处理逻辑
    }
}
```

注册处理器：

```java
MessageDispatcher dispatcher = MessageDispatcher.getInstance();
dispatcher.

registerMessage(new CustomAlertMessage());

// 或使用 lambda
        dispatcher.

registerHandler("CUSTOM_TYPE",(session, data) ->{
        // 处理逻辑
        });
```

### 10.2 发送自定义消息

```java
MsgSender sender = MsgSender.getInstance();
CustomAlertMessage alert = new CustomAlertMessage();
alert.

setContent("Price alert!");
sender.

sendToUser("user1",alert);
sender.

broadcast(alert);
```

### 10.3 扩展会话

继承 `WsSession`：

```java
public class CustomSession extends WsSession {

    public CustomSession(Channel channel) {
        super(channel);
    }

    public void setAccountId(String accountId) {
        // ...
    }
}
```

注册会话：

```java
SessionManager manager = SessionManager.getInstance();
CustomSession session = new CustomSession(channel);
manager.

addSession(session);
```