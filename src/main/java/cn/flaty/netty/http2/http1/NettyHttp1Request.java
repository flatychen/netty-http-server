package cn.flaty.netty.http2.http1;

import cn.flaty.netty.http2.NettyHttpRequest;
import cn.flaty.netty.http2.NettyHttpResponse;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

@Slf4j
public class NettyHttp1Request extends NettyHttpRequest {

  private byte[] body = null;

  private FullHttpRequest fullHttpRequest;

  public NettyHttp1Request(Channel channel, FullHttpRequest fullHttpRequest) {
    Validate.notNull(fullHttpRequest);
    Validate.notNull(channel);
    super.channel = channel;
    this.fullHttpRequest = fullHttpRequest;
    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(fullHttpRequest.uri());
    this.path = queryStringDecoder.path();
    this.uri = fullHttpRequest.uri();
    this.queryParameter = queryStringDecoder.parameters();
  }

  public void parseHttpRequestBody() {
    if (this.fullHttpRequest != null) {
      this.body = ByteBufUtil.getBytes(this.fullHttpRequest.content());
    }
  }

  @Override
  public byte[] body() {
    return this.body;
  }

  @Override
  public String method() {
    return fullHttpRequest.method().toString().toUpperCase();
  }

  @Override
  public String getHeaderValue(String key) {
    return fullHttpRequest.headers().get(key);
  }

  @Override
  public void writeHttpResponse(Channel channel, NettyHttpResponse nettyHttpResponse) {
    FullHttpResponse response = prepareHttpResponse(nettyHttpResponse);
    channel.writeAndFlush(response);
  }

  @Override
  public void writeHttpResponse(
      ChannelHandlerContext context, NettyHttpResponse nettyHttpResponse) {
    FullHttpResponse response = prepareHttpResponse(nettyHttpResponse);
    context.writeAndFlush(response);
  }

  private FullHttpResponse prepareHttpResponse(NettyHttpResponse nettyHttpResponse) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, nettyHttpResponse.getStatus());
    if (nettyHttpResponse.isBodyExist()) {
      byte[] b = nettyHttpResponse.getBody();
      response.content().writeBytes(b);
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, b.length);
    }
    nettyHttpResponse.getHeaders().forEach((name, value) -> response.headers().set(name, value));
    return response;
  }
}
