package cn.flaty.netty.http2.http1;

import cn.flaty.netty.http2.NettyHttpResponse;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Http1ToRequestHandler extends ChannelDuplexHandler {

  private NettyHttp1Request nettyHttp1Request = null;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    try {
      if (msg instanceof FullHttpRequest) {
        FullHttpRequest request = (FullHttpRequest) msg;
        nettyHttp1Request = new NettyHttp1Request(ctx.channel(), request);
        nettyHttp1Request.parseHttpRequestBody();
        ctx.fireChannelRead(nettyHttp1Request);
      } else {
        super.channelRead(ctx, msg);
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    nettyHttp1Request.cleanRequestContext();
    if (msg instanceof NettyHttpResponse) {
      NettyHttpResponse nettyHttpResponse = (NettyHttpResponse) msg;
      nettyHttp1Request.writeHttpResponse(ctx, nettyHttpResponse);
    } else {
      super.write(ctx, msg, promise);
    }
  }
}
