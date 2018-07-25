package com.github.service.base;

import com.github.service.base.BaseServiceConfiguration;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

/**
 * This is the entry point to bootstrapping and configuring the base service.
 * 
 * @author gaurav
 */
final class BaseServiceImpl implements BaseService {
  private static final Logger logger = LogManager.getLogger(BaseServiceImpl.class.getSimpleName());

  private final BaseServiceConfiguration config;
  private Channel httpChannel;
  private EventLoopGroup serverThreads;
  private EventLoopGroup workerThreads;

  private static final AtomicInteger currentActiveConnectionCount = new AtomicInteger();
  private static final AtomicLong allAcceptedConnectionCount = new AtomicLong();

  BaseServiceImpl(final BaseServiceConfiguration config) {
    this.config = config;
  }

  @Override
  public void start() throws Exception {
    // TODO: handle args
    int port = config.getServerPort();
    int serverThreadCount = config.getServerThreadCount();
    int workerThreadCount = config.getWorkerThreadCount();

    logger.info(
        String.format("Firing up BaseService at port %d port with %d server & %d worker threads",
            port, serverThreadCount, workerThreadCount));

    // Configure the server:worker system
    // TODO: try and use EpollEventLoopGroup
    serverThreads = new NioEventLoopGroup(serverThreadCount, new ThreadFactory() {
      final AtomicInteger threadCounter = new AtomicInteger();

      @Override
      public Thread newThread(final Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("server-" + threadCounter.getAndIncrement());
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable error) {
            logger.error("Logging unhandled exception.", error);
          }
        });
        return thread;
      }
    });
    workerThreads = new NioEventLoopGroup(workerThreadCount, new ThreadFactory() {
      final AtomicInteger threadCounter = new AtomicInteger();

      @Override
      public Thread newThread(final Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("worker-" + threadCounter.getAndIncrement());
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable error) {
            logger.error("Logging unhandled exception.", error);
          }
        });
        return thread;
      }
    });

    // InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);

    // TODO: get read/write timeout values from BaseServiceConfiguration
    final ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(serverThreads, workerThreads).channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new BaseServiceInitializer(config));
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    // TODO get this from BaseServiceConfiguration
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000);

    httpChannel = bootstrap.bind(port).sync().channel();
    logger.info("Successfully fired up BaseService");
  }

  @Override
  public void stop() throws Exception {
    logger.info(String.format("Current Active Connections:%d, All Accepted Connections:%d",
        currentActiveConnectionCount.get(), allAcceptedConnectionCount.get()));

    logger.info("Shutting down BaseService");
    if (httpChannel != null) {
      httpChannel.close();
    }
    if (serverThreads != null) {
      serverThreads.shutdownGracefully();
    }
    if (workerThreads != null) {
      workerThreads.shutdownGracefully();
    }
    if (httpChannel != null) {
      httpChannel.closeFuture().await();
    }
    logger.info("Successfully shut down BaseService");
  }

  /**
   * Convenience initializer to quickly wire downstream handler pipeline.
   * 
   * TODO: this could be rule-based depending on the environment of this server.
   */
  private static class BaseServiceInitializer extends ChannelInitializer<Channel> {
    private final BaseServiceConfiguration config;

    private BaseServiceInitializer(final BaseServiceConfiguration config) {
      this.config = config;
    }

    @Override
    public void initChannel(final Channel channel) {
      final ChannelPipeline pipeline = channel.pipeline();

      /**
       * Important notes for understanding & modifying the pipeline:<br/>
       * 1. An inbound event is handled by the inbound handlers in the bottom-up direction. An
       * inbound handler usually handles the inbound data generated by the I/O thread. The inbound
       * data is often read from a remote peer via the actual input operation such as
       * SocketChannel.read(ByteBuffer). If an inbound event goes beyond the top inbound handler, it
       * is discarded silently, or logged if it needs attention.
       * 
       * 2. An outbound event is handled by the outbound handler in the top-down direction. An
       * outbound handler usually generates or transforms the outbound traffic such as write
       * requests. If an outbound event goes beyond the bottom outbound handler, it is handled by an
       * I/O thread associated with the Channel. The I/O thread often performs the actual output
       * operation such as SocketChannel.write(ByteBuffer).
       * 
       * Inbound eval order: 0->1->2->3->4->5 <br/>
       * Outbound eval order: 6->5->3->1->0 <br/>
       * 
       * 3. Our chosen handler contract leverages FullHttpRequest flowing through the entire
       * pipeline.
       */
      final ConnectionMetricHandler connectionMetricHandler =
          new ConnectionMetricHandler(currentActiveConnectionCount, allAcceptedConnectionCount);
      pipeline.addLast("0", new IdleStateHandler(config.getReaderIdleTimeSeconds(),
          config.getWriterIdleTimeSeconds(), 30));
      pipeline.addLast("1", connectionMetricHandler);
      pipeline.addLast("2", new HttpServerCodec(4096, 8192, 8192));
      pipeline.addLast("3", new HttpObjectAggregator(65535));
      pipeline.addLast("4", new HttpContentCompressor(config.getCompressionLevel()));

      pipeline.addLast("5", new ReadTimeoutHandler(15000L, TimeUnit.MILLISECONDS));
      pipeline.addLast("6", new WriteTimeoutHandler(15000L, TimeUnit.MILLISECONDS));

      // Add service handler(s) here
      pipeline.addLast("7", new ServiceHandler());

      pipeline.addLast("8", new PipelineExceptionHandler());

      final CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
          .allowedRequestMethods(new HttpMethod[] {HttpMethod.GET, HttpMethod.POST}).build();
      pipeline.addLast("9", new CorsHandler(corsConfig));
    }
  }

  public static void main(String[] args) {
    Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
      }
    });
  }

}
