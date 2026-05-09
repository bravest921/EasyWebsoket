package com.smsg.core;

import com.smsg.handler.WsFrameHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket服务器
 */
public class WsServer {

    private static final Logger log = LoggerFactory.getLogger(WsServer.class);

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public WsServer(int port) {
        this.port = port;
    }

    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP协议编解码
                            pipeline.addLast(new HttpServerCodec());

                            // 块写处理器
                            pipeline.addLast(new ChunkedWriteHandler());

                            // 聚合HTTP请求
                            pipeline.addLast(new HttpObjectAggregator(8192));

                            // WebSocket协议处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true));

                            // 自定义帧处理器
                            pipeline.addLast(new WsFrameHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("WebSocket server started on port {}", port);
            log.info("Connect address: ws://localhost:{}/ws", port);

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket server shut down");
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.warn("Invalid port, using default 8080");
            }
        }
        new WsServer(port).start();
    }
}