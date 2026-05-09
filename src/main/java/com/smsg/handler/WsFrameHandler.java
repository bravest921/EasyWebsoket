package com.smsg.handler;

import com.smsg.core.MessageDispatcher;
import com.smsg.core.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket帧处理器
 * @author Atral
 */
public class WsFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(WsFrameHandler.class);

    private final MessageDispatcher dispatcher = MessageDispatcher.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        dispatcher.dispatch(ctx, msg);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().id().asLongText();
        SessionManager.getInstance().addSession(ctx.channel());
        log.info("Client connected: {}", sessionId);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().id().asLongText();
        SessionManager.getInstance().removeSession(sessionId);
        log.info("Client disconnected: {}", sessionId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception occurred: {}", cause.getMessage());
        SessionManager.getInstance().removeSessionByChannel(ctx.channel());
        ctx.close();
    }
}