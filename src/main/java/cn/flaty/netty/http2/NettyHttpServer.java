package cn.flaty.netty.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class NettyHttpServer {
  public static final int MAX_BACKLOG = 1024;
  public static final int MAX_CONTENT_LENGTH = 1024 * 64;
  public static final int MAX_CONCURRENT_STREAM_SIZE = 5000;

  private int connectionIdleTimeout = 0;
  private int port;
  private int maxLength;
  private boolean http2Support = false;
  private int workThreadSize;
  private int maxConcurrentStreamSize = 0;
  private int bossThreadSize;
  private HttpHandlerFactory httpHandlerFactory;
  private List<HttpFilterFactory> httpFilterFactories;

  public int getMaxLength() {
    return maxLength;
  }

  public boolean isHttp2Support() {
    return http2Support;
  }

  public int getMaxConcurrentStreamSize() {
    return maxConcurrentStreamSize;
  }

  public interface HttpHandlerFactory {
    HttpHandler newHttpHandler();
  }

  public int getConnectionIdleTimeout() {
    return connectionIdleTimeout;
  }

  public interface HttpFilterFactory {
    HttpFilter newHttpFilter();
  }

  public static final class Builder {
    private int port = 8080;
    private int bossThreadSize = 1;
    private int maxLength = 1024 * 10;
    private int connectionIdleTimeout = 0;
    private int maxConcurrentStreamSize = MAX_CONCURRENT_STREAM_SIZE;
    private boolean http2Support = false;
    private int workThreadSize = Runtime.getRuntime().availableProcessors();
    private HttpHandlerFactory applicationFilterFactory;
    private List<HttpFilterFactory> filterFactories = new ArrayList<>();

    private Builder() {}

    public Builder maxConcurrentStreamSize(int maxConcurrentStreamSize) {
      this.maxConcurrentStreamSize = maxConcurrentStreamSize;
      return this;
    }


    public Builder bossThreadSize(int bossThreadSize) {
      this.bossThreadSize = bossThreadSize;
      return this;
    }

    public Builder maxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder connectionIdleTimeout(int connectionIdleTimeout) {
      this.connectionIdleTimeout = connectionIdleTimeout;
      return this;
    }


    public Builder http2Support(boolean http2Support) {
      this.http2Support = http2Support;
      return this;
    }

    public Builder addHttpFilterFactory(HttpFilterFactory httpFilterFactory) {
      this.filterFactories.add(httpFilterFactory);
      return this;
    }

    public Builder httpHandlerFactory(HttpHandlerFactory filterFactory) {
      this.applicationFilterFactory = filterFactory;
      return this;
    }

    public Builder workThreadSize(int workThreadSize) {
      this.workThreadSize = workThreadSize;
      return this;
    }

    public NettyHttpServer build() {
      Validate.notNull(this.applicationFilterFactory);
      NettyHttpServer nettyHttpServer = new NettyHttpServer();
      nettyHttpServer.workThreadSize = this.workThreadSize;
      nettyHttpServer.http2Support = this.http2Support;
      nettyHttpServer.port = this.port;
      nettyHttpServer.connectionIdleTimeout = this.connectionIdleTimeout;
      nettyHttpServer.maxLength = this.maxLength;
      nettyHttpServer.httpHandlerFactory = this.applicationFilterFactory;
      nettyHttpServer.httpFilterFactories = this.filterFactories;
      nettyHttpServer.bossThreadSize = this.bossThreadSize;
      nettyHttpServer.maxConcurrentStreamSize = this.maxConcurrentStreamSize;
      return nettyHttpServer;
    }
  }

  public static Builder createBuilder() {
    return new Builder();
  }

  public void startServer() {
    final EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadSize);
    final EventLoopGroup workerGroup = new NioEventLoopGroup(workThreadSize);
    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, MAX_BACKLOG);
    if (bossThreadSize > 1) {
      b.option(ChannelOption.SO_REUSEADDR, true);
    }

    b.childOption(ChannelOption.AUTO_READ, true);
    b.childOption(ChannelOption.TCP_NODELAY, true);
    b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    b.group(bossGroup, workerGroup)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .channel(NioServerSocketChannel.class)
        .childHandler(new NettyHandlerInitializer(this));
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
    try {
      log.info("Netty server start at port:{}", port);
      for (int i = 0; i < bossThreadSize; i++) {
        b.bind(port + i * 100).sync();
      }
    } catch (InterruptedException e) {
      log.error("NettyHttpServer was interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  public void addPipelineFilter(ChannelPipeline pipeline) {
    pipeline.addFirst(new ConnectionIdleTimeOutHandler(this.connectionIdleTimeout));
    if (this.httpFilterFactories != null) {
      List<ChannelHandler> channelHandlers =
          httpFilterFactories.stream()
              .map(HttpFilterFactory::newHttpFilter)
              .collect(Collectors.toList());
      pipeline.addLast(channelHandlers.toArray(new ChannelHandler[channelHandlers.size()]));
    }
    pipeline.addLast(this.httpHandlerFactory.newHttpHandler());
  }
}
