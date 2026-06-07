package cn.flaty.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class NettyHttpRequest {
  protected Map<String, List<String>> queryParameter;
  protected Map<String, Object> mapData = new HashMap<>();
  protected String uri;
  protected String path;
  protected Channel channel;
  protected Long startTime;

  public String getTraceId() {
    return traceId;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public void setTraceId(String traceId) {
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
    }
    this.traceId = traceId;
  }

  protected String traceId = "-";

  public String path() {
    return this.path;
  }

  public String uri() {
    return this.uri;
  }

  public Channel getChannel() {
    return channel;
  }

  /**
   * Gets the first value of the query parameter with the given name.
   *
   * @param name the name of the query parameter
   * @return the first value of the query parameter with the given name, or null if no value is found
   */
  public String getQueryParameter(String name) {
    List<String> values = this.queryParameter.get(name);
    if (CollectionUtils.isEmpty(values)) {
      return null;
    } else {
      return values.get(0);
    }
  }

  public Map<String, List<String>> queryParameters() {
    return this.queryParameter;
  }

  public abstract byte[] body();

  public abstract String method();

  public abstract String getHeaderValue(String key);

  public void putRequestContext(String k, Object v) {
    mapData.put(k, v);
  }

  public void cleanRequestContext() {
    mapData.clear();
  }

  public <T> T getFromRequestContext(String k) {
    return (T) mapData.get(k);
  }

  public abstract void writeHttpResponse(Channel channel, NettyHttpResponse nettyHttpResponse);
  

  public abstract void writeHttpResponse(
      ChannelHandlerContext context, NettyHttpResponse nettyHttpResponse);
}
