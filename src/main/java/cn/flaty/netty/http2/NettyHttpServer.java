package cn.flaty.netty.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class NettyHttpServer {
  public static final int MAX_BACKLOG = 1024;
  public static final int MAX_CONCURRENT_STREAM_SIZE = 5000;
  public static final int DEFAULT_PORT = 8080;
  public static final int DEFAULT_BOSS_THREAD_SIZE = 1;
  public static final int DEFAULT_WORK_THREAD_SIZE = 0; // Will use available processors
  public static final int DEFAULT_MAX_LENGTH = 1024 * 10;
  public static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 0;
  public static final boolean DEFAULT_HTTP2_SUPPORT = false;
  public static final int PORT_INCREMENT = 1; // Changed from 100 to 1 to avoid conflicts

  private volatile boolean isRunning = false;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private List<Channel> boundChannels = new CopyOnWriteArrayList<>();

  private int connectionIdleTimeout = DEFAULT_CONNECTION_IDLE_TIMEOUT;
  private int port = DEFAULT_PORT;
  private int maxLength = DEFAULT_MAX_LENGTH;
  private boolean http2Support = DEFAULT_HTTP2_SUPPORT;
  private int workThreadSize = DEFAULT_WORK_THREAD_SIZE;
  private int maxConcurrentStreamSize = MAX_CONCURRENT_STREAM_SIZE;
  private int bossThreadSize = DEFAULT_BOSS_THREAD_SIZE;
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
    private int port = DEFAULT_PORT;
    private int bossThreadSize = DEFAULT_BOSS_THREAD_SIZE;
    private int maxLength = DEFAULT_MAX_LENGTH;
    private int connectionIdleTimeout = DEFAULT_CONNECTION_IDLE_TIMEOUT;
    private int maxConcurrentStreamSize = MAX_CONCURRENT_STREAM_SIZE;
    private boolean http2Support = DEFAULT_HTTP2_SUPPORT;
    private int workThreadSize = DEFAULT_WORK_THREAD_SIZE;
    private HttpHandlerFactory applicationFilterFactory;
    private List<HttpFilterFactory> filterFactories = new ArrayList<>();

    private Builder() {
      // Initialize work thread size if not set
      if (workThreadSize == 0) {
        workThreadSize = Runtime.getRuntime().availableProcessors();
      }
    }

    public Builder maxConcurrentStreamSize(int maxConcurrentStreamSize) {
      this.maxConcurrentStreamSize = maxConcurrentStreamSize;
      return this;
    }


    public Builder bossThreadSize(int bossThreadSize) {
      Validate.isTrue(bossThreadSize > 0, "Boss thread size must be positive");
      this.bossThreadSize = bossThreadSize;
      return this;
    }

    public Builder maxLength(int maxLength) {
      Validate.isTrue(maxLength > 0, "Max length must be positive");
      this.maxLength = maxLength;
      return this;
    }

    public Builder port(int port) {
      Validate.isTrue(port > 0 && port <= 65535, "Port must be between 1 and 65535");
      this.port = port;
      return this;
    }

    public Builder connectionIdleTimeout(int connectionIdleTimeout) {
      Validate.isTrue(connectionIdleTimeout >= 0, "Connection idle timeout must be non-negative");
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
      Validate.isTrue(workThreadSize > 0, "Work thread size must be positive");
      this.workThreadSize = workThreadSize;
      return this;
    }

    public NettyHttpServer build() {
      Validate.notNull(this.applicationFilterFactory, "HttpHandlerFactory must not be null");
      Validate.notNull(this.filterFactories, "Filter factories list must not be null");

      NettyHttpServer nettyHttpServer = new NettyHttpServer();
      nettyHttpServer.workThreadSize = this.workThreadSize;
      nettyHttpServer.http2Support = this.http2Support;
      nettyHttpServer.port = this.port;
      nettyHttpServer.connectionIdleTimeout = this.connectionIdleTimeout;
      nettyHttpServer.maxLength = this.maxLength;
      nettyHttpServer.httpHandlerFactory = this.applicationFilterFactory;
      nettyHttpServer.httpFilterFactories = new CopyOnWriteArrayList<>(this.filterFactories);
      nettyHttpServer.bossThreadSize = this.bossThreadSize;
      nettyHttpServer.maxConcurrentStreamSize = this.maxConcurrentStreamSize;
      return nettyHttpServer;
    }
  }

  public static Builder createBuilder() {
    return new Builder();
  }

  public void startServer() {
    if (isRunning) {
      log.warn("Server is already running on port: {}", port);
      return;
    }

    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

    bossGroup = new NioEventLoopGroup(bossThreadSize);
    workerGroup = new NioEventLoopGroup(workThreadSize);

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
      log.info("Starting Netty server at port: {}", port);

      // Bind all channels
      for (int i = 0; i < bossThreadSize; i++) {
        int actualPort = port + i * PORT_INCREMENT;
        ChannelFuture channelFuture = b.bind(actualPort).sync();
        Channel channel = channelFuture.channel();
        boundChannels.add(channel);
        log.info("Server bound to port: {}", actualPort);
      }

      isRunning = true;
      log.info("Netty server started successfully on {} port(s)", boundChannels.size());

    } catch (InterruptedException e) {
      log.error("NettyHttpServer was interrupted during start", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Failed to start Netty server", e);
      throw new RuntimeException("Failed to start server", e);
    }
  }

  public void addPipelineFilter(ChannelPipeline pipeline) {
    Objects.requireNonNull(pipeline, "ChannelPipeline must not be null");

    pipeline.addFirst(new ConnectionIdleTimeOutHandler(this.connectionIdleTimeout));
    if (this.httpFilterFactories != null && !this.httpFilterFactories.isEmpty()) {
      List<ChannelHandler> channelHandlers =
          httpFilterFactories.stream()
              .map(HttpFilterFactory::newHttpFilter)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!channelHandlers.isEmpty()) {
        pipeline.addLast(channelHandlers.toArray(new ChannelHandler[0]));
      }
    }
    pipeline.addLast(this.httpHandlerFactory.newHttpHandler());
  }
}
