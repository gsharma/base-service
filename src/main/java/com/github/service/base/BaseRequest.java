package com.github.service.base;

public class BaseRequest {
  private long clientTstampMillis;
  private double requestId;

  public long getClientTstampMillis() {
    return clientTstampMillis;
  }

  public void setClientTstampMillis(long clientTstampMillis) {
    this.clientTstampMillis = clientTstampMillis;
  }

  public void setRequestId(double requestId) {
    this.requestId = requestId;
  }

  public double getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return "BaseRequest [clientTstampMillis=" + clientTstampMillis + ", requestId=" + requestId
        + "]";
  }

}
