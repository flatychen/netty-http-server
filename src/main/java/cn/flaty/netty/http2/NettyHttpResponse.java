package cn.flaty.netty.http2;

import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;

/**
 * @author eenchxi
 * @date 2021/8/13
 */
public class NettyHttpResponse {
  private byte[] body;
  private HttpResponseStatus status;
  private Map<String, String> headers;

  public HttpResponseStatus getStatus() {
    return status;
  }

  public byte[] getBody() {
    return body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }


  public static NettyHttpResponseBuilder builder() {
    return new NettyHttpResponseBuilder();
  }

  public boolean isBodyExist() {
    return body != null && body.length > 0;
  }

  public static final class NettyHttpResponseBuilder {
    private byte[] body = null;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private Map<String, String> headers = Maps.newHashMap();
    private boolean needCloseChannel = false;
    private NettyHttpResponseBuilder() {}

    public NettyHttpResponseBuilder body(byte[] body) {
      this.body = body;
      return this;
    }

    public NettyHttpResponseBuilder status(HttpResponseStatus status) {
      this.status = status;
      return this;
    }
    public NettyHttpResponseBuilder needCloseChannel(boolean needClose) {
      this.needCloseChannel = needClose;
      return this;
    }

    public NettyHttpResponseBuilder addHeader(String name, String value) {
      headers.put(name, value);
      return this;
    }

    public NettyHttpResponse build() {
      NettyHttpResponse nettyHttpResponse = new NettyHttpResponse();
      nettyHttpResponse.body = this.body;
      nettyHttpResponse.status = this.status;
      nettyHttpResponse.headers = this.headers;
      return nettyHttpResponse;
    }
  }
}
