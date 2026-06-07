package cn.flaty.netty.http2.http2;

import cn.flaty.netty.http2.NettyHttpRequest;
import cn.flaty.netty.http2.NettyHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class NettyHttp2Request extends NettyHttpRequest {

  private Http2Headers http2Headers;
  private List<Http2DataFrame> http2DataFrames = new ArrayList<>();
  private byte[] body = null;
  private int streamId;

  public int getStreamId() {
    return streamId;
  }

  public NettyHttp2Request(Channel channel) {
    this.channel = channel;
  }

  public void setHttp2Headers(Http2Headers http2Headers,int streamId) {
    Validate.notNull(http2Headers);
    this.http2Headers = http2Headers;
    QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(this.http2Headers.path().toString());
    this.path = queryStringDecoder.path();
    this.uri = this.http2Headers.path().toString();
    this.queryParameter = queryStringDecoder.parameters();
    this.streamId = streamId;
    ReferenceCountUtil.release(http2Headers);
  }

  public void addHttp2DataFrame(Http2DataFrame http2DataFrame) {
    Validate.notNull(http2DataFrame);
    http2DataFrames.add(http2DataFrame);
  }

  public void parseHttpRequestBody() {
    if (CollectionUtils.isNotEmpty(this.http2DataFrames)) {
      try {
      List<ByteBuf> buffs = http2DataFrames.stream().map(Http2DataFrame::content).collect(
          Collectors.toList());
      ByteBuf content = Unpooled.wrappedBuffer(buffs.toArray(new ByteBuf[0]));
      body = ByteBufUtil.getBytes(content);

      for (Http2DataFrame http2DataFrame : http2DataFrames) {
        ReferenceCountUtil.release(http2DataFrame);
      }
      } catch (Exception exception) {
        body = "{}".getBytes();
        log.error("msg:{},stream id is {}",exception.getMessage(),streamId);
      }

    }
  }

  @Override
  public byte[] body() {
    return this.body;
  }

  @Override
  public String method() {
    return http2Headers.method().toString().toUpperCase();
  }

  @Override
  public String getHeaderValue(String key) {
    return Optional.ofNullable(http2Headers.get(key)).map(CharSequence::toString).orElse("");
  }

  @Override
  public void writeHttpResponse(Channel channel, NettyHttpResponse nettyHttpResponse) {
    channel.writeAndFlush(this.prepareHttp2HeadersFrame(nettyHttpResponse));
    if (nettyHttpResponse.isBodyExist()) {
      channel.writeAndFlush(this.prepareHttp2DataFrame(nettyHttpResponse));
    }
  }

  @Override
  public void writeHttpResponse(
      ChannelHandlerContext context, NettyHttpResponse nettyHttpResponse) {
    context.writeAndFlush(this.prepareHttp2HeadersFrame(nettyHttpResponse));
    if (nettyHttpResponse.isBodyExist()) {
      context.writeAndFlush(this.prepareHttp2DataFrame(nettyHttpResponse));
    }
  }

  private DefaultHttp2DataFrame prepareHttp2DataFrame(NettyHttpResponse nettyHttpResponse) {
    ByteBuf byteBuf = Unpooled.copiedBuffer(nettyHttpResponse.getBody());
    return new DefaultHttp2DataFrame(byteBuf, true);
  }

  private DefaultHttp2HeadersFrame prepareHttp2HeadersFrame(NettyHttpResponse nettyHttpResponse) {
    Http2Headers headers =
        new DefaultHttp2Headers().status(nettyHttpResponse.getStatus().codeAsText());
    nettyHttpResponse.getHeaders().forEach(headers::add);
    return new DefaultHttp2HeadersFrame(headers, !nettyHttpResponse.isBodyExist());
  }
}
