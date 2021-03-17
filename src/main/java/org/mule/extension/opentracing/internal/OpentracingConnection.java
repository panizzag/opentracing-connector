package org.mule.extension.opentracing.internal;


import org.mule.extension.opentracing.api.OpentracingConfiguration;
import org.mule.extension.opentracing.api.enums.HttpMethod;
import org.mule.extension.opentracing.api.enums.OpenTraceLogType;
import org.mule.extension.opentracing.api.enums.TagType;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents an extension connection just as example (there is no real connection with anything here c:).
 */
public final class OpentracingConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpentracingConnection.class);

  private OpentracingConfiguration genConfig;
  private HttpClient httpClient;
  private HttpRequestBuilder httpRequestBuilder;

  AllInOneTracing openTrace = null;
  String traceName = "mule-open-trace";

  public OpentracingConnection(HttpService httpService, OpentracingConfiguration gConfig) {
    genConfig = gConfig;
    initHttpClient(httpService);
  }


  public void invalidate() {
    httpClient.stop();
  }

  public boolean isConnected() throws Exception{
    return true;
  }

  public void initHttpClient(HttpService httpService){
    HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder();
    builder.setName("opentracing");
    httpClient = httpService.getClientFactory().create(builder.build());
    httpRequestBuilder = HttpRequest.builder();
    httpClient.start();
  }

  public void beginSpan(String traceName, String spanName, TagType tagName, String tagValue, OpenTraceLogType logType, String logMessage){
    try {
      this.traceName = traceName;
      openTrace = new AllInOneTracing(traceName,
                                      genConfig.getAgentHost(),
                                      new Integer(genConfig.getAgentPort()).intValue(),
                                      new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                      new Integer(genConfig.getMaxQueueSize()).intValue());
      openTrace.beginSpan(spanName, tagName.name(), tagValue, logType.name(), logMessage);
    } catch (Exception e) {
      LOGGER.error("Failed while Begining the span and trace: " + e.getMessage());
    }
  }

  public void endSpan(String spanName, OpenTraceLogType logType, String logMessage) {
    try {
      if (openTrace == null) {
        LOGGER.warn("endSpan operation. openTrace instance is null, creating a new one");
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
      }
      openTrace.endTrace(spanName, logType.name(), logMessage);
    } catch (Exception e) {
      LOGGER.error("Failed while Ending span:  " + e.getMessage());
    }
  }

  public Map<String, String> injectTrace(String remoteHost,
                                         String remotePort,
                                         String remotePath,
                                         String scheme,
                                         HttpMethod remoteServiceHttpMethod) {
    Map<String, String> headers = new HashMap<String, String>();
    try {
      if (openTrace == null) {
        LOGGER.warn("injectTrace operation. openTrace instance is null, creating a new one");
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
      }

      Map map = openTrace.injectTrace(remoteHost, remotePort, remotePath, remoteServiceHttpMethod.name(), scheme);
      headers.putAll(map);
    } catch (Exception e) {
      LOGGER.error("Failed while Injecting the trace: " + e.getMessage());
      e.printStackTrace();
    }
    return headers;
  }

  public void extractTrace(String traceID, String headerName, String resourceName, TagType tagName, String tagValue, OpenTraceLogType logType, String logMessage) {
    try {
      this.traceName = resourceName + "-tracer";
      LOGGER.debug("Tracename is "
                      + traceName
                      + " TraceID :"
                      + traceID
                      + " Agent Host: "
                      + genConfig.getAgentHost()
                      + " Agent port : "
                      + genConfig.getAgentPort());
      openTrace = new AllInOneTracing(traceName,
                                      genConfig.getAgentHost(),
                                      new Integer(genConfig.getAgentPort()).intValue(),
                                      new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                      new Integer(genConfig.getMaxQueueSize()).intValue());
      if (traceID != null)
        openTrace.extractTrace(traceID,
                               headerName,
                               resourceName,
                               logType.name(),
                               logMessage);
      else
        openTrace.beginSpan(resourceName,
                            tagName.name(),
                            tagValue,
                            logType.name(),
                            "TraceID not found. Starting a new Span");
    } catch (Exception e) {
      LOGGER.error("Failed while Extracting trace" + e.getMessage());
      e.printStackTrace();
    }
  }

  public void writeTraceLog(String spanName, OpenTraceLogType logType, String logMessage) {
    try {
      if (openTrace == null) {
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
      }
      LOGGER.debug("Writing logType {} to spanName {} ", logType.name(), spanName);
      openTrace.writeTraceLog(spanName, logType.name(), logMessage);
    } catch (Exception e) {
      LOGGER.error("Failed while writing to log " + e.getMessage());
    }
  }

  public void addTag(String spanName, TagType tagType, String tagvalue) {
    try {
      if (openTrace == null) {
        LOGGER.warn("addTag operation. openTrace instance is null, creating a new one");
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
      }
      openTrace.addTag(spanName, tagType.name(), tagvalue);
    } catch (Exception e) {
     LOGGER.error("Failed while Adding a tag: " + e.getMessage());
    }
  }
}
