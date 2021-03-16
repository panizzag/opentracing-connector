package org.mule.extension.opentracing.api;

import io.opentracing.Tracer;
import org.mule.extension.opentracing.internal.OpentracingConnectionProvider;
import org.mule.extension.opentracing.internal.OpentracingOperations;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(OpentracingOperations.class)
@ConnectionProviders(OpentracingConnectionProvider.class)
public class OpentracingConfiguration {

  @Parameter
  @Optional(defaultValue = "localhost")
  private String agentHost;

  @Parameter
  @Optional(defaultValue = "6831")
  String agentPort;

  @Parameter
  @Optional(defaultValue = "100")
  String flushIntervalInMillis;

  @Parameter
  @Optional(defaultValue = "10")
  String maxQueueSize;

  public String getFlushIntervalInMillis() {
    return flushIntervalInMillis;
  }

  public void setFlushIntervalInMillis(String flushIntervalInMillis) {
    this.flushIntervalInMillis = flushIntervalInMillis;
  }

  public String getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(String maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public String getAgentHost() {
    return agentHost;
  }

  public void setAgentHost(String agentHost) {
    this.agentHost = agentHost;
  }

  public String getAgentPort() {
    return agentPort;
  }

  public void setAgentPort(String agentPort) {
    this.agentPort = agentPort;
  }

}
