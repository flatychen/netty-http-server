package cn.flaty.http2.CompleteFutre;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FutureTest {

  public String getResult() {
    try {
      Thread.sleep(1000 * 5);
    } catch (InterruptedException e) {
    }
    log.info("getResult finish!");
    return "result";
  }

  @Test
  public void test() throws IOException {
    log.info("start !");
    CompletableFuture future = new CompletableFuture();
    future
        .orTimeout(2000, TimeUnit.MILLISECONDS)
        .whenComplete(
            (s, throwable) -> {
              if (throwable != null) {
                log.error("throwable", throwable);
              } else {
                log.info("finished:{}", s);
              }
            });
    future.complete(this.getResult());
    System.in.read();
  }
}
