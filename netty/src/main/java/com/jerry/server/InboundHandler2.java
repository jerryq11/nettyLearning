package com.jerry.server;

/**
 * Created by jerryqq on 2017/12/26.
 */
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class InboundHandler2 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InboundHandler2.channelRead: msg :" +  ((ByteBuf)msg).toString(CharsetUtil.UTF_8));
        ctx.write(msg);
    }

//    @Override
//    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("InboundHandler2.channelReadComplete");
//        ctx.flush();
//    }

}
