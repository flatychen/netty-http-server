package cn.flaty.netty.http2.channelInitializer;

import cn.flaty.netty.http2.HttpReadStartHandler;
import cn.flaty.netty.http2.NettyHttpServer;
import cn.flaty.netty.http2.http1.Http1ToRequestHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class Http1Initializer extends ChannelInitializer<SocketChannel> {

  protected NettyHttpServer nettyHttpServer;

  public Http1Initializer(NettyHttpServer nettyHttpServer) {
    this.nettyHttpServer = nettyHttpServer;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ch.pipeline()
        .addLast(
            new HttpReadStartHandler(),
            new HttpServerCodec(),
            new HttpObjectAggregator(nettyHttpServer.getMaxLength()),
            new Http1ToRequestHandler());
    nettyHttpServer.addPipelineFilter(ch.pipeline());
  }
}
