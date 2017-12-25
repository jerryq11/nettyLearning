package com.kg.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

import com.kg.netty.msg.KeepAliveMessage;
import com.kg.utils.Constants;
import com.kg.utils.Utils;

public class KeepAliveServer {

    private int port;

    public KeepAliveServer(int port) {
        this.port = port;
    }

    ChannelFuture f;

    ServerBootstrap b;

    // 设置6秒检测chanel是否接受过心跳数据
    private static final int READ_WAIT_SECONDS = 6;

    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PING_TIMES = 3;

    public void startServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new KeepAliveServerInitializer());
            // 服务器绑定端口监听
            f = b.bind(port).sync();
            // 监听服务器关闭监听，此方法会阻塞
            f.channel().closeFuture().sync();
            /* b.bind(portNumber).sync().channel().closeFuture().sync(); */
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class KeepAliveServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
			
			/*
			 * 使用ObjectDecoder和ObjectEncoder
			 * 因为双向都有写数据和读数据，所以这里需要两个都设置
			 * 如果只读，那么只需要ObjectDecoder即可
			 */
            pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
            pipeline.addLast("encoder", new ObjectEncoder());
			
			/*
			 * 这里只监听读操作
			 * 可以根据需求，监听写操作和总得操作
			 */
            pipeline.addLast("pong", new IdleStateHandler(READ_WAIT_SECONDS, 0, 0, TimeUnit.SECONDS));
            pipeline.addLast("handler", new Heartbeat());
        }
    }

    private class Heartbeat extends SimpleChannelInboundHandler<KeepAliveMessage> {

        // 失败计数器：未收到client端发送的ping请求
        private int unRecPingTimes = 0;

        // 每个chanel对应一个线程，此处用来存储对应于每个线程的一些基础数据
        ThreadLocal<KeepAliveMessage> localMsgInfo = new ThreadLocal<KeepAliveMessage>();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, KeepAliveMessage msg) throws Exception {
            System.out.println("thread:" + Thread.currentThread().getName() + "," + ctx.channel().remoteAddress() + " Say : sn=" + msg.getSn() + ",reqcode=" + msg.getReqCode());
            // 收到ping消息后，回复
            if (Utils.notEmpty(msg.getSn()) && msg.getReqCode() == Constants.REQ_CODE) {
                msg.setReqCode(Constants.RET_CODE);
                if (msg.getSn().equals("sn_2")) {
                    Thread.sleep(10000);
                    System.out.println("thread:" + Thread.currentThread().getName() + "interrupt");
                }
                ctx.channel().writeAndFlush(msg);
                // 失败计数器清零
                unRecPingTimes = 0;
                if (localMsgInfo.get() == null) {
                    KeepAliveMessage localMsg = new KeepAliveMessage();
                    localMsg.setSn(msg.getSn());
                    localMsgInfo.set(localMsg);
                }
            } else {
                ctx.channel().close();
            }
        }

        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
	                /*读超时*/
                    System.out.println("===服务端===(READER_IDLE 读超时)");
                    // 失败计数器次数大于等于3次的时候，关闭链接，等待client重连
                    if (unRecPingTimes >= MAX_UN_REC_PING_TIMES) {
                        System.out.println("===服务端===(读超时，关闭chanel)");
                        // 连续超过N次未收到client的ping消息，那么关闭该通道，等待client重连
                        ctx.channel().close();
                    } else {
                        // 失败计数器加1
                        unRecPingTimes++;
                    }
                } else if (event.state() == IdleState.WRITER_IDLE) {
                    System.out.println("===服务端===(WRITER_IDLE 写超时)");
                } else if (event.state() == IdleState.ALL_IDLE) {
                    System.out.println("===服务端===(ALL_IDLE 总超时)");
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            if (localMsgInfo.get() != null) {
                System.out.println(localMsgInfo.get().getSn() + "异常:" + cause.getMessage());
            } else {
                System.out.println("错误原因：" + cause.getMessage());
            }
            ctx.channel().close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("Client active ");
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // 关闭，等待重连
            ctx.close();
            if (localMsgInfo.get() != null) {
                System.out.println(localMsgInfo.get().getSn() + "连接断开");
            }
//		    System.out.println("===服务端===(客户端失效)");
        }
    }

    public void stopServer() {
        if (f != null) {
            f.channel().close();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        KeepAliveServer keepAliveServer = new KeepAliveServer(1666);
        keepAliveServer.startServer();
    }
}
