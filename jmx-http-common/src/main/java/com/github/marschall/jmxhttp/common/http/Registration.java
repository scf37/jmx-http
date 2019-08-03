package com.github.marschall.jmxhttp.common.http;

import java.io.Serializable;

/**
 * Represents virtual jmx connection over stateless HTTP
 */
public final class Registration implements Serializable {

  private final long correlationId;

  public Registration(long correlationId) {
    this.correlationId = correlationId;
  }

  public long getCorrelationId() {
    return correlationId;
  }

}
