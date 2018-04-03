package com.github.service.base;

public interface BaseService {

  void start() throws Exception;

  void stop() throws Exception;

  /**
   * A simple builder to let users use fluent APIs to build BaseService.
   * 
   * @author gaurav
   */
  public final static class BaseServiceBuilder {
    private BaseServiceConfiguration config;

    public static BaseServiceBuilder newBuilder() {
      return new BaseServiceBuilder();
    }

    public BaseServiceBuilder config(final BaseServiceConfiguration config) {
      this.config = config;
      return this;
    }

    public BaseService build() {
      return new BaseServiceImpl(config);
    }

    private BaseServiceBuilder() {}
  }
}
