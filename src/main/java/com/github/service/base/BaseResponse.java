package com.github.service.base;

public class BaseResponse {
  private long serverTstampMillis;

  public long getServerTstampMillis() {
    return serverTstampMillis;
  }

  public void setServerTstampMillis(long serverTstampMillis) {
    this.serverTstampMillis = serverTstampMillis;
  }

  @Override
  public String toString() {
    return "BaseResponse [serverTstampMillis=" + serverTstampMillis + "]";
  }
}
