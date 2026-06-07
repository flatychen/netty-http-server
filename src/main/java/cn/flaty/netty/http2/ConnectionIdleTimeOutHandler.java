package cn.flaty.netty.http2;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ConnectionIdleTimeOutHandler extends IdleStateHandler {


  public ConnectionIdleTimeOutHandler( int allIdleTimeout) {
    super(0, 0, allIdleTimeout, TimeUnit.MILLISECONDS);
  }


  @Override
  protected final void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    if (IdleState.ALL_IDLE.equals(evt.state())) {
      log.warn("the connection idle, close channel directly!");
      ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
    }
  }

}
