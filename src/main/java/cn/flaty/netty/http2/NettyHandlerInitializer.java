/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package cn.flaty.netty.http2;

import cn.flaty.netty.http2.channelInitializer.Http1Initializer;
import cn.flaty.netty.http2.channelInitializer.Http2PriorKnowledgeHandlerInitializer;
import cn.flaty.netty.http2.http2.Http2ToRequestHandler;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.*;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHandlerInitializer extends ChannelInitializer<SocketChannel> {

  private static final int MAX_CONTENT_LENGTH = 1024 * 20;
  private final NettyHttpServer nettyHttpServer;

  public NettyHandlerInitializer(NettyHttpServer nettyHttpServer) {
    this.nettyHttpServer = nettyHttpServer;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    if (this.nettyHttpServer.isHttp2Support()) {
      initHttp2(ch);
    } else {
      initHttp1(ch);
    }
  }

  private void initHttp1(SocketChannel ch) {
    final ChannelPipeline p = ch.pipeline();
    Http1Initializer http1Initializer = new Http1Initializer(nettyHttpServer);
    p.addLast(http1Initializer);
  }

  private void initHttp2(SocketChannel ch) {
    final ChannelPipeline p = ch.pipeline();
    final HttpServerCodec sourceCodec = new HttpServerCodec();
    final HttpServerUpgradeHandler upgradeHandler =
        new HttpServerUpgradeHandler(
            sourceCodec, new Http2UpgradeCodecFactory(), MAX_CONTENT_LENGTH);
    final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
        new CleartextHttp2ServerUpgradeHandler(
            sourceCodec,
            upgradeHandler,
            new Http2PriorKnowledgeHandlerInitializer(nettyHttpServer));
    p.addLast(cleartextHttp2ServerUpgradeHandler);
    p.addLast(
        new SimpleChannelInboundHandler<HttpMessage>() {
          @Override
          protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
            log.error("server not support http1 , protocolVersion:{}", msg.protocolVersion());
            ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
          }
        });
  }

  public static class Http2UpgradeCodecFactory implements UpgradeCodecFactory {

    @Override
    public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
      if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return new Http2ServerUpgradeCodec(
            Http2FrameCodecBuilder.forServer().build(),
            new Http2MultiplexHandler(new Http2ToRequestHandler()));
      } else {
        return null;
      }
    }
  }
}
