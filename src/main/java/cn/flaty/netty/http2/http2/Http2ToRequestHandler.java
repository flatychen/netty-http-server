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

import cn.flaty.netty.http2.NettyHttpResponse;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Http2ToRequestHandler extends ChannelDuplexHandler {

  private NettyHttp2Request nettyHttp2Request = null;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    nettyHttp2Request = new NettyHttp2Request(ctx.channel());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof Http2HeadersFrame) {
        onHeadersRead(ctx, (Http2HeadersFrame) msg);
      } else if (msg instanceof Http2DataFrame) {
        onDataRead(ctx, (Http2DataFrame) msg);
      } else {
        super.channelRead(ctx, msg);
      }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    nettyHttp2Request.cleanRequestContext();
    if (msg instanceof NettyHttpResponse) {
      NettyHttpResponse nettyHttpResponse = (NettyHttpResponse) msg;
      nettyHttp2Request.writeHttpResponse(ctx, nettyHttpResponse);
    } else {
      super.write(ctx, msg, promise);
    }
  }

  /** If receive a frame with end-of-stream set, send a pre-canned response. */
  private void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) {
    nettyHttp2Request.addHttp2DataFrame(data);
    if (data.isEndStream()) {
      nettyHttp2Request.parseHttpRequestBody();
      ctx.fireChannelRead(nettyHttp2Request);
    }
  }

  private void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headers) {
    nettyHttp2Request.setHttp2Headers(headers.headers(),headers.stream().id());
    if (headers.isEndStream()) {
      nettyHttp2Request.parseHttpRequestBody();
      ctx.fireChannelRead(nettyHttp2Request);
    }
  }


}
