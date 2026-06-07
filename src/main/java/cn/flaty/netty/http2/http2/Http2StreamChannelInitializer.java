/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.flaty.netty.http2.http2;

import cn.flaty.netty.http2.HttpReadStartHandler;
import cn.flaty.netty.http2.NettyHttpServer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public final class Http2StreamChannelInitializer extends ChannelDuplexHandler {

  private NettyHttpServer nettyHttpServer;

  public Http2StreamChannelInitializer(NettyHttpServer nettyHttpServer) {
    this.nettyHttpServer = nettyHttpServer;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().addLast(new HttpReadStartHandler(),new Http2ToRequestHandler());
    nettyHttpServer.addPipelineFilter(ctx.pipeline());
  }


}
