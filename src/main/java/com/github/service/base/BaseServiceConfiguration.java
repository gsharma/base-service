package com.github.service.base;

/**
 * An immutable config holder for the server to bootstrap.
 * 
 * TODO: provide a way to populate via properties, as well
 * 
 * @author gaurav
 */
public final class BaseServiceConfiguration {
  private final int serverPort;
  private final int serverThreadCount;
  private final int workerThreadCount;
  private final int readerIdleTimeSeconds;
  private final int writerIdleTimeSeconds;
  private final int compressionLevel;

  public BaseServiceConfiguration(final int serverPort, final int serverThreadCount,
      final int workerThreadCount, final int readerIdleTimeSeconds, final int writerIdleTimeSeconds,
      final int compressionLevel) {
    this.serverPort = serverPort;
    this.serverThreadCount = serverThreadCount;
    this.workerThreadCount = workerThreadCount;
    this.readerIdleTimeSeconds = readerIdleTimeSeconds;
    this.writerIdleTimeSeconds = writerIdleTimeSeconds;
    this.compressionLevel = compressionLevel;
  }

  int getServerPort() {
    return serverPort;
  }

  int getServerThreadCount() {
    return serverThreadCount;
  }

  int getWorkerThreadCount() {
    return workerThreadCount;
  }

  int getReaderIdleTimeSeconds() {
    return readerIdleTimeSeconds;
  }

  int getWriterIdleTimeSeconds() {
    return writerIdleTimeSeconds;
  }

  int getCompressionLevel() {
    return compressionLevel;
  }
}
