/**
 * WebSocket 消息管理框架 SDK
 * 引入后直接使用 WebSocketSDK 类即可
 *
 * 使用方式：
 * 1. script 标签引入：<script src="WebSocketSDK.js"></script>
 * 2. ES Module 导入：import WebSocketSDK from './WebSocketSDK.js'
 *
 * @author Atral
 */
(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
        typeof define === 'function' && define.amd ? define(factory) :
            (global = typeof globalThis !== 'undefined' ? globalThis : global || self, global.WebSocketSDK = factory());
})(this, function () {
    'use strict';

    /**
     * WebSocket SDK
     */
    class WebSocketSDK {
        constructor() {
            this.ws = null;
            this.url = '';
            this.options = {
                reconnectInterval: 3000,    // 重连间隔(ms)
                reconnectMaxAttempts: 10,   // 最大重连次数
                heartbeatInterval: 25000,   // 心跳间隔(ms)
                heartbeatTimeout: 5000,     // 心跳超时时间(ms)
                debug: false
            };
            this.reconnectAttempts = 0;
            this.isManualClose = false;
            this.isLoggedIn = false;        // 登录状态

            // 定时器
            this.heartbeatTimer = null;
            this.heartbeatTimeoutTimer = null;
            this.reconnectTimer = null;

            // 登录信息
            this.loginData = null;

            // 已订阅的模块（用于重连后自动恢复）
            this.subscribedModules = new Set();

            // 回调函数
            this.callbacks = {
                onOpen: null,
                onClose: null,
                onError: null,
                onMessage: null,
                onReconnect: null,
                onLoginSuccess: null,
                onLoginError: null
            };
        }

        /**
         * 连接到服务器
         * @param {string} url - WebSocket 地址，如 ws://localhost:8080/ws
         * @param {Object} options - 配置选项
         * @param {Function} options.onOpen - 连接打开回调
         * @param {Function} options.onClose - 连接关闭回调
         * @param {Function} options.onError - 错误回调
         * @param {Function} options.onMessage - 消息回调
         * @param {Function} options.onReconnect - 重连回调
         * @param {Function} options.onLoginSuccess - 登录成功回调
         * @param {Function} options.onLoginError - 登录失败回调
         */
        connect(url, options = {}) {
            this.url = url;
            this.isManualClose = false;

            // 合并配置
            this.options = {...this.options, ...options};

            // 保存回调
            if (options.onOpen) this.callbacks.onOpen = options.onOpen;
            if (options.onClose) this.callbacks.onClose = options.onClose;
            if (options.onError) this.callbacks.onError = options.onError;
            if (options.onMessage) this.callbacks.onMessage = options.onMessage;
            if (options.onReconnect) this.callbacks.onReconnect = options.onReconnect;
            if (options.onLoginSuccess) this.callbacks.onLoginSuccess = options.onLoginSuccess;
            if (options.onLoginError) this.callbacks.onLoginError = options.onLoginError;

            // 保存登录信息用于重连
            if (options.loginData) {
                this.loginData = options.loginData;
            }

            this._createConnection();
        }

        /**
         * 创建连接
         */
        _createConnection() {
            this._log('Connecting to:', this.url);

            try {
                this.ws = new WebSocket(this.url);

                this.ws.onopen = (event) => {
                    this._log('WebSocket connected');
                    this.reconnectAttempts = 0;

                    // 停止重连定时器
                    if (this.reconnectTimer) {
                        clearTimeout(this.reconnectTimer);
                        this.reconnectTimer = null;
                    }

                    // 启动心跳
                    this._startHeartbeat();

                    // 触发回调
                    if (this.callbacks.onOpen) {
                        this.callbacks.onOpen(event);
                    }

                    // 重连后自动恢复登录和订阅
                    if (this.loginData) {
                        if (this.isLoggedIn) {
                            this._log('Re-logging in after reconnect...');
                            this.login(this.loginData);
                        }
                    }
                };

                this.ws.onclose = (event) => {
                    this._log('WebSocket closed, code:', event.code, 'reason:', event.reason);

                    // 停止心跳
                    this._stopHeartbeat();

                    // 触发回调
                    if (this.callbacks.onClose) {
                        this.callbacks.onClose(event);
                    }

                    // 非手动关闭，尝试重连
                    if (!this.isManualClose) {
                        this._reconnect();
                    }
                };

                this.ws.onerror = (error) => {
                    this._log('WebSocket error:', error);
                    if (this.callbacks.onError) {
                        this.callbacks.onError(error);
                    }
                };

                this.ws.onmessage = (event) => {
                    this._log('Received:', event.data);
                    try {
                        const data = JSON.parse(event.data);

                        // 处理心跳响应
                        if (data.type === 'HEARTBEAT') {
                            this._resetHeartbeatTimeout();
                            return;
                        }

                        // 处理登录响应
                        if (data.type === 'LOGIN_RESPONSE') {
                            if (data.success) {
                                this._log('Login success');
                                this.isLoggedIn = true;
                                // 恢复订阅的模块
                                this._restoreSubscriptions();
                                if (this.callbacks.onLoginSuccess) {
                                    this.callbacks.onLoginSuccess(data);
                                }
                            } else {
                                this._log('Login failed:', data.message);
                                this.isLoggedIn = false;
                                if (this.callbacks.onLoginError) {
                                    this.callbacks.onLoginError(data);
                                }
                            }
                            return;
                        }

                        // 触发消息回调
                        if (this.callbacks.onMessage) {
                            this.callbacks.onMessage(data);
                        }
                    } catch (e) {
                        this._log('Parse message error:', e);
                    }
                };

            } catch (error) {
                this._log('Create connection error:', error);
                this._reconnect();
            }
        }

        /**
         * 尝试重连
         */
        _reconnect() {
            if (this.isManualClose) return;
            if (this.reconnectAttempts >= this.options.reconnectMaxAttempts) {
                this._log('Max reconnect attempts reached, stop trying');
                return;
            }

            this.reconnectAttempts++;
            this._log(`Reconnecting... attempt ${this.reconnectAttempts}/${this.options.reconnectMaxAttempts}`);

            if (this.callbacks.onReconnect) {
                this.callbacks.onReconnect(this.reconnectAttempts);
            }

            this.reconnectTimer = setTimeout(() => {
                this._createConnection();
            }, this.options.reconnectInterval);
        }

        /**
         * 启动心跳
         */
        _startHeartbeat() {
            this._stopHeartbeat();
            this.heartbeatTimer = setInterval(() => {
                if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                    this.send({type: 'HEARTBEAT'});
                    // 启动心跳超时检测
                    this._startHeartbeatTimeout();
                }
            }, this.options.heartbeatInterval);
        }

        /**
         * 启动心跳超时检测
         */
        _startHeartbeatTimeout() {
            this._clearHeartbeatTimeout();
            this.heartbeatTimeoutTimer = setTimeout(() => {
                this._log('Heartbeat timeout, connection may be dead');
                this._closeAndReconnect();
            }, this.options.heartbeatTimeout);
        }

        /**
         * 清除心跳超时定时器
         */
        _clearHeartbeatTimeout() {
            if (this.heartbeatTimeoutTimer) {
                clearTimeout(this.heartbeatTimeoutTimer);
                this.heartbeatTimeoutTimer = null;
            }
        }

        /**
         * 停止心跳
         */
        _stopHeartbeat() {
            if (this.heartbeatTimer) {
                clearInterval(this.heartbeatTimer);
                this.heartbeatTimer = null;
            }
            this._clearHeartbeatTimeout();
        }

        /**
         * 收到心跳响应时调用，重置超时检测
         */
        _resetHeartbeatTimeout() {
            this._clearHeartbeatTimeout();
        }

        /**
         * 关闭并重连
         */
        _closeAndReconnect() {
            this.isLoggedIn = false;
            if (this.ws) {
                this.ws.close();
            }
            this._reconnect();
        }

        /**
         * 发送消息
         * @param {Object|string} message - 消息对象或字符串
         */
        send(message) {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                const data = typeof message === 'string' ? message : JSON.stringify(message);
                this.ws.send(data);
                this._log('Sent:', data);
            } else {
                this._log('WebSocket not connected, cannot send');
            }
        }

        /**
         * 登录
         * @param {Object} loginInfo - 登录信息 {userId, username, password, role, module}
         */
        login(loginInfo) {
            this.loginData = loginInfo; // 保存用于重连
            this.send({
                type: 'LOGIN',
                userId: loginInfo.userId,
                username: loginInfo.username,
                password: loginInfo.password,
                role: loginInfo.role || '',
                module: loginInfo.module || ''
            });
        }

        /**
         * 登出
         */
        logout() {
            this.send({
                type: 'LOGOUT',
                userId: this.loginData?.userId
            });
            this.isLoggedIn = false;
            this.subscribedModules.clear();
        }

        /**
         * 恢复订阅（重连后自动订阅之前订阅过的模块）
         */
        _restoreSubscriptions() {
            if (this.subscribedModules.size > 0) {
                this._log('Restoring subscriptions:', [...this.subscribedModules]);
                this.subscribedModules.forEach(module => {
                    this.send({
                        type: 'SUBSCRIBE',
                        module: module
                    });
                });
            }
        }

        /**
         * 订阅模块
         * @param {string} module - 模块名
         */
        subscribe(module) {
            this.subscribedModules.add(module);
            this.send({
                type: 'SUBSCRIBE',
                module: module
            });
        }

        /**
         * 取消订阅模块
         * @param {string} module - 模块名
         */
        unsubscribe(module) {
            this.subscribedModules.delete(module);
            this.send({
                type: 'UNSUBSCRIBE',
                module: module
            });
        }

        /**
         * 切换模块（取消旧模块，订阅新模块）
         * @param {string} newModule - 新模块名
         * @param {string} oldModule - 旧模块名
         */
        switchModule(newModule, oldModule) {
            if (oldModule) {
                this.unsubscribe(oldModule);
            }
            this.subscribe(newModule);
        }

        /**
         * 发送自定义消息
         * @param {Object} message - 消息对象，需包含 type 字段
         */
        sendMessage(message) {
            this.send(message);
        }

        /**
         * 关闭连接
         */
        close() {
            this.isManualClose = true;
            this._stopHeartbeat();
            if (this.reconnectTimer) {
                clearTimeout(this.reconnectTimer);
                this.reconnectTimer = null;
            }
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
        }

        /**
         * 获取连接状态
         * @returns {string} CONNECTING|OPEN|CLOSING|CLOSED
         */
        getState() {
            if (!this.ws) return 'CLOSED';
            const states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED'];
            return states[this.ws.readyState] || 'CLOSED';
        }

        /**
         * 是否已连接
         */
        isConnected() {
            return this.ws && this.ws.readyState === WebSocket.OPEN;
        }

        /**
         * 日志
         */
        _log(...args) {
            if (this.options.debug) {
                console.log('[WebSocketSDK]', ...args);
            }
        }
    }

    return WebSocketSDK;
});
