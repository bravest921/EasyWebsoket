# 需求文档

## 1. 项目概述

- **项目定位**：轻量级、可集成的 WebSocket 消息管理组件
- **引入方式**：提供 Maven 依赖坐标，供其他 Spring Boot 项目引用
- **核心能力**：高性能消息推送、会话管理、模块化消息分发
- **应用场景**：股票交易实时行情推送

## 2. 技术选型

| 组件 | 技术方案 | 说明 |
|------|----------|------|
| 网络框架 | Netty 4.1.x | 高性能、NIO |
| WebSocket | netty-codec-http | 协议编解码 |
| JSON | Hutool JSON | 消息序列化 |
| 日志 | SLF4J + Logback | 日志输出 |

## 3. 功能需求

### 3.1 内置消息类型

| 类型 | 说明 |
|------|------|
| LOGIN | 登录请求 |
| LOGIN_RESPONSE | 登录响应 |
| HEARTBEAT | 心跳 |
| SUBSCRIBE | 订阅业务模块 |
| UNSUBSCRIBE | 取消订阅业务模块 |
| SYSTEM_MESSAGE | 系统消息 |

### 3.2 核心功能

- [x] Netty Server 初始化（端口可配置）
- [x] WebSocket 握手升级
- [x] Channel 会话管理（SessionManager）
- [x] 消息分发路由（MessageDispatcher）
- [x] 基于模块的订阅/取消订阅
- [x] 消息 JSON 序列化（Hutool JSON）

### 3.3 会话管理

- 基于 Channel ID 的会话管理
- 用户绑定与认证
- 模块订阅管理
- 会话存储：ConcurrentHashMap

### 3.4 消息推送 API（MsgSender）

```java
// 按用户发送
MsgSender.getInstance().sendToUser(userId, message);

// 广播
MsgSender.getInstance().broadcast(message);

// 按模块单发
MsgSender.getInstance().sendToModule(module, userId, message);

// 按模块广播
MsgSender.getInstance().broadcastToModule(module, message);

// 向多个模块广播
MsgSender.getInstance().broadcastToModules(modules, message);

// 发送日志
MsgSender.getInstance().sendLogToUser(userId, "INFO", "message");
MsgSender.getInstance().broadcastLog("WARN", "warning message");

// 发送系统消息
MsgSender.getInstance().sendSystemToUser(userId, "content");
```

## 4. 外部扩展

### 4.1 自定义消息

框架提供 `BusinessMessage` 抽象类，外部系统可继承实现自定义消息处理器。

### 4.2 自定义会话

框架提供 `WsSession` 抽象类，外部系统可继承实现自定义会话。

### 4.3 Spring Boot 集成

支持通过 Spring WebSocket 集成到现有项目，使用 `MsgSession` 接口适配不同 WebSocket 实现。

## 5. 验收标准

- [x] 可被其他 Maven 项目引入
- [x] WebSocket 连接建立成功
- [x] 消息收发正常
- [x] 按用户分发正确
- [x] 按模块订阅/分发正确
- [x] 日志推送功能正常
- [x] 断线重连后会话恢复