package com.jerry.server;

/**
 * Created by jerryqq on 2017/12/26.
 */
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;


public class InboundHandler1 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InboundHandler1.channelRead: msg :" + ((ByteBuf)msg).toString(CharsetUtil.UTF_8));

        // 通知执行下一个InboundHandler
        ctx.fireChannelRead(msg);
    }

//    @Override
//    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("InboundHandler1.channelReadComplete");
//        ctx.fireChannelReadComplete();
//    }
}