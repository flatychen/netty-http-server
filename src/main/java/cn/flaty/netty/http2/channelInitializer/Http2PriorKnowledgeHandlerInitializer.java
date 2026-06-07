
package cn.flaty.netty.http2.channelInitializer;

import cn.flaty.netty.http2.NettyHttpServer;
import cn.flaty.netty.http2.http2.Http2StreamChannelInitializer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Http2PriorKnowledgeHandlerInitializer extends ChannelInitializer<SocketChannel> {
  protected NettyHttpServer nettyHttpServer;

  public Http2PriorKnowledgeHandlerInitializer(NettyHttpServer nettyHttpServer) {
    this.nettyHttpServer = nettyHttpServer;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    Http2Settings http2Settings =
            Http2Settings.defaultSettings()
                    .maxConcurrentStreams(nettyHttpServer.getMaxConcurrentStreamSize());
    ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().initialSettings(http2Settings).build(),
    new Http2MultiplexHandler(new Http2StreamChannelInitializer(nettyHttpServer)),
    new Http2ExceptionHandler());
  }

  public class Http2ExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      if (cause instanceof IOException) {
        log.debug("connection maybe reset by remote! cause:{}",cause.getMessage());
      }else{
        log.error("Http2ExceptionHandler exception:", cause);
      }
      ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
    }
  }

}
