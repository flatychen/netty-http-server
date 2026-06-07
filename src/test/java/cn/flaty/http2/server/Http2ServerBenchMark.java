package cn.flaty.http2.server;

import cn.flaty.netty.http2.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Http2ServerBenchMark {

  public static void main(String[] args) throws IOException {
    NettyHttpServer.createBuilder()
        .port(6667)
        .connectionIdleTimeout(3000)
        .httpHandlerFactory(() -> new AppHandler())
        .http2Support(true)
        .build()
        .startServer();
    System.in.read();
  }

  public static class AppHandler extends HttpHandler {
    @Override
    protected CompletableFuture<NettyHttpResponse> handler(NettyHttpRequest request) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      log.info("AppHandler start response");
      return CompletableFuture.completedFuture(
          NettyHttpResponse.builder().body("hello".getBytes(StandardCharsets.UTF_8)).build());
    }
  }
}
