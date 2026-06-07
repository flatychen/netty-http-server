package cn.flaty.http2.client;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.VertxBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyHttpClient {

  private static Logger logger = LoggerFactory.getLogger(MyHttpClient.class);
  HttpClient client;

  Vertx vertx;

  public MyHttpClient() {
    vertx = new VertxBuilder().init().vertx();
    HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setKeepAlive(true);
    client = vertx.createHttpClient(httpClientOptions);
  }

  @Test
  public void testReadTimeOut() throws InterruptedException {
    client.request(
        HttpMethod.GET,
        6667,
        "127.0.0.1",
        "/",
        ar -> {
          if (ar.succeeded()) {
            logger.info("connect success");
            HttpClientRequest request = ar.result();
            request
                .send()
                .onSuccess(
                    r -> {
                      logger.info("send onResponse code {}", r.statusCode());
                      r.bodyHandler(
                          buffer -> {
                            if (buffer != null) {
                              String body = new String(ByteBufUtil.getBytes(buffer.getByteBuf()));
                              logger.info("send onResponse code {}", body);
                            }
                              try {
                                  Thread.sleep(6000);
                              } catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                              }
                          });
                    });
          }
        });
    Thread.sleep(1000 * 15);
  }
  @Test
  public void testWriteTimeOut() throws InterruptedException {
    client.request(
        HttpMethod.GET,
        6667,
        "127.0.0.1",
        "/",
        ar -> {
          if (ar.succeeded()) {
            logger.info("connect success");
            HttpClientRequest request = ar.result();
            request
                .send()
                .onSuccess(
                    r -> {
                      logger.info("send onResponse code {}", r.statusCode());
                      r.bodyHandler(
                          buffer -> {
                            if (buffer != null) {
                              String body = new String(ByteBufUtil.getBytes(buffer.getByteBuf()));
                              logger.info("send onResponse code {}", body);
                            }
                              try {
                                  Thread.sleep(6000);
                              } catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                              }
                          });
                    });
          }
        });
    Thread.sleep(1000 * 15);
  }

  @Test
  public void testIdleTimeOUt() throws InterruptedException {
    client.request(
        HttpMethod.GET,
        6667,
        "127.0.0.1",
        "/",
        ar -> {
          if (ar.succeeded()) {
            logger.info("connect success");
          }
        });
    Thread.sleep(1000 * 100);
  }
}
