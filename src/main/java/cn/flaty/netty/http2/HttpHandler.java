package cn.flaty.netty.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class HttpHandler extends SimpleChannelInboundHandler<NettyHttpRequest> {

  private static long DEFAULT_TIMEOUT = 1000L * 5;
  private static NettyHttpResponse DEFAULT_TIMEOUT_RESPONSE =
      NettyHttpResponse.builder()
          .body(HttpResponseStatus.GATEWAY_TIMEOUT.reasonPhrase().getBytes())
          .status(HttpResponseStatus.GATEWAY_TIMEOUT)
          .build();
  private static NettyHttpResponse DEFAULT_EXCEPTION_RESPONSE =
      NettyHttpResponse.builder()
          .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          .body(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase().getBytes())
          .build();

  private NettyHttpRequest nettyHttpRequest = null;

  protected ExecutorService getHandlerThread() {
    return ForkJoinPool.commonPool();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, NettyHttpRequest request)
      throws Exception {
    ExecutorService thread = getHandlerThread();
    if (thread == null) {
      thread = ForkJoinPool.commonPool();
    }
    CompletableFuture<NettyHttpResponse> withTimeoutResponse = new CompletableFuture();
    withTimeoutResponse.completeOnTimeout(
        this.timeoutResponse(), this.timeout(), TimeUnit.MILLISECONDS);
    thread.execute(
        () -> {
          try {
            this.handler(request)
                .whenComplete(
                    (nettyHttpResponse, e) -> {
                      if (e != null) {
                        withTimeoutResponse.completeExceptionally(e);
                      } else {
                        withTimeoutResponse.complete(nettyHttpResponse);
                      }
                    });
          } catch (Throwable e) {
            withTimeoutResponse.completeExceptionally(e);
          }
        });

    withTimeoutResponse.whenComplete(
        (nettyHttpResponse, throwable) -> {
          log.info("anyOf throwable:{}", throwable);
          if (!ctx.channel().isActive()) {
            log.warn("The connection is closed before response.");
          } else {
            if (throwable != null) {
              ctx.writeAndFlush(this.exception(request, throwable));
            } else {
              ctx.writeAndFlush(nettyHttpResponse);
            }
          }
        });
  }

  public long timeout() {
    return DEFAULT_TIMEOUT;
  }

  public NettyHttpResponse timeoutResponse() {
    return DEFAULT_TIMEOUT_RESPONSE;
  }

  protected abstract CompletableFuture<NettyHttpResponse> handler(NettyHttpRequest request);

  public NettyHttpResponse exception(NettyHttpRequest request, Throwable throwable) {
    Validate.notNull(request, "NettyHttpRequest is null ! maybe es-proxy-netty init fail!");
    Validate.notNull(throwable);
    return DEFAULT_EXCEPTION_RESPONSE;
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    ctx.writeAndFlush(this.exception(nettyHttpRequest, throwable));
  }
}
