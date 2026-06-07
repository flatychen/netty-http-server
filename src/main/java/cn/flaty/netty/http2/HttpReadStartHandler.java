package cn.flaty.netty.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author eenchxi
 * @date 2021/8/13
 */
@Slf4j
public  class HttpReadStartHandler extends ChannelInboundHandlerAdapter {

  private long readStartTime;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    this.readStartTime = System.currentTimeMillis();
    ctx.fireChannelRead(msg);
  }

  public long getReadStartTime() {
    return readStartTime;
  }
}
