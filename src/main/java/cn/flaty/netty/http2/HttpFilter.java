package cn.flaty.netty.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

@Slf4j
public class HttpFilter extends ChannelDuplexHandler {

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    try {
      if (msg instanceof NettyHttpRequest) {
        if (!this.beforeHandler((NettyHttpRequest) msg)) {
          ctx.fireChannelRead(msg);
        }
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof NettyHttpResponse) {
      this.afterHandler(ctx, (NettyHttpResponse) msg);
    }
    super.write(ctx, msg, promise);
  }

  protected boolean beforeHandler(NettyHttpRequest request) {
    Validate.notNull(request);
    return false;
  }

  protected void afterHandler(ChannelHandlerContext ctx, NettyHttpResponse response) {

  }



}
