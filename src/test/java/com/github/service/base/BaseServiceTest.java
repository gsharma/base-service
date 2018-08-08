package com.github.service.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.service.base.BaseService.BaseServiceBuilder;

import io.netty.handler.codec.http.HttpHeaderNames;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Happy path tests for BaseService, lots more to do.
 * 
 * @author gaurav
 */
public class BaseServiceTest {
  static {
    System.setProperty("log4j.configurationFile", "log4j.properties");
  }

  private static final Logger logger = LogManager.getLogger(BaseServiceTest.class.getSimpleName());
  // serverPort, serverThreadCount, workerThreadCount, readerIdleTimeSeconds, writerIdleTimeSeconds,
  // compressionLevel
  private static BaseServiceConfiguration config =
      new BaseServiceConfiguration(9000, 2, Runtime.getRuntime().availableProcessors(), 15, 15, 9);
  private static BaseService service = BaseServiceBuilder.newBuilder().config(config).build();
  // 10mins read timeout is for debugging
  private static OkHttpClient client =
      new OkHttpClient.Builder().connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
          .addInterceptor(new LoggingInterceptor()).readTimeout(10, TimeUnit.MINUTES).build();
  // private static final String serverUrl = "http://localhost:" + config.getServerPort();
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testServer() throws Exception {
    final Runnable work = new Runnable() {
      public void run() {
        try {
          final HttpUrl url =
              new HttpUrl.Builder().scheme("http").host("localhost").addPathSegment("service")
                  .addPathSegment("base").port(config.getServerPort()).build();
          BaseRequest baseRequest = new BaseRequest();
          baseRequest.setRequestId(Math.random());
          baseRequest.setClientTstampMillis(System.currentTimeMillis());
          String requestJson = objectMapper.writeValueAsString(baseRequest);
          RequestBody body = RequestBody.create(JSON, requestJson);
          Request request = new Request.Builder().url(url)
              .header(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json")
              // .header(HttpHeaderNames.CONNECTION.toString(), "close")
              .header(HttpHeaderNames.ORIGIN.toString(), "localhost").post(body).build();
          Response response = client.newCall(request).execute();
          assertEquals(200, response.code());

          request = new Request.Builder().url(url)
              .header(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json")
              .header("Origin", "localhost").get().build();
          response = client.newCall(request).execute();
          assertEquals(200, response.code());
          BaseResponse baseResponse =
              objectMapper.readValue(response.body().bytes(), BaseResponse.class);
          assertNotNull(baseResponse);
          assertTrue(baseResponse.getServerTstampMillis() != 0);
        } catch (Exception fooBar) {
          logger.error(fooBar);
        }
      }
    };
    int workerCount = 40;
    Thread[] workers = new Thread[workerCount];
    for (int iter = 0; iter < workerCount; iter++) {
      Thread worker = new Thread(work, "client-" + iter);
      workers[iter] = worker;
      worker.start();
    }
    for (Thread worker : workers) {
      worker.join();
    }
  }

  @BeforeClass
  public static void init() throws Exception {
    try {
      service.start();
    } catch (IOException problem) {
      logger.error(problem);
    }
  }

  @AfterClass
  public static void tini() throws Exception {
    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
    // client.cache().close();
    if (service != null) {
      service.stop();
    }
  }

  static final class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
      Request request = chain.request();
      long t1 = System.nanoTime();
      logger.info(String.format("Sending request %s on %s%n%s", request.url(), chain.connection(),
          request.headers()));
      Response response = chain.proceed(request);
      long t2 = System.nanoTime();
      logger.info(String.format("Received response for %s in %.1fms%n%s", response.request().url(),
          (t2 - t1) / 1e6d, response.headers()));
      return response;
    }
  }

}
