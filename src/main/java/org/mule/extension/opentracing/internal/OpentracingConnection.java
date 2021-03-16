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
      openTrace.beginSpan(
              traceName, spanName, tagName.name(), tagValue, logType.name(), logMessage);
    } catch (Exception e) {
      LOGGER.error("Failed while Begining the span and trace: " + e.getMessage());
    }
  }

  public void endSpan(String spanName, OpenTraceLogType logType, String logMessage) {
    try {
      if (openTrace == null) {
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

  public void injectTrace(String remoteHost, String remotePort, String remotePath, HttpMethod remoteServiceHttpMethod, Map<String, Object> outboundHeaders) {
    try {
      if (openTrace == null) {
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
      }

      Map map = openTrace.injectTrace(
                      remoteHost, remotePort, remotePath, remoteServiceHttpMethod.name());
      outboundHeaders.putAll(map);
    } catch (Exception e) {
      LOGGER.error("Failed while Injecting the trace: " + e.getMessage());
    }
  }

  public void extractTrace(String traceID, String resourceName, TagType tagName, String tagValue, OpenTraceLogType logType, String logMessage) {
    try {
      this.traceName = resourceName + "-tracer";
      LOGGER.debug("INSIDE extractTrace - Tracename is "
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
        openTrace.extractTrace(
                traceID,
                resourceName,
                tagName.name(),
                tagValue,
                logType.name(),
                logMessage);
      else
        openTrace.beginSpan(
                traceName,
                resourceName,
                tagName.name(),
                tagValue,
                logType.name(),
                "Begning a new Span");
    } catch (Exception e) {
      LOGGER.error("Failed while Extracting trace" + e.getMessage());
    }
  }

  public void writeTraceLog(String spanName, OpenTraceLogType logType, String logMessage) {
    try {
      LOGGER.debug(logMessage);
      if (openTrace == null) {
        openTrace = new AllInOneTracing(traceName,
                                        genConfig.getAgentHost(),
                                        new Integer(genConfig.getAgentPort()).intValue(),
                                        new Integer(genConfig.getFlushIntervalInMillis()).intValue(),
                                        new Integer(genConfig.getMaxQueueSize()).intValue());
          }
      openTrace.writeTraceLog(spanName, logType.name(), logMessage);
    } catch (Exception e) {
      LOGGER.error("Failed while writing to log " + e.getMessage());
    }
  }

  public void addTag(String spanName, TagType tagType, String tagvalue) {
    try {
      if (openTrace == null) {
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
